/*---------------------------------------------------------------------------------------------------------------------------------------------------
 * equinox-boot.c - bootloader for equinox
 *
 * ATTINY85 @ 8 MHz
 * lfuse: 0xE2
 * hfuse: 0xDC
 * efuse: 0xFE
 * 
 * Copyright (c) 2012 Frank Meyer, Robert Meyer
 *
 * Should be linked with following options on ATTiny85:
 *    -Wl,--section-start=.text=0x1800                      // section in flash memory begins at 6144 of 8192(last 2 KB)
 *    -Wl,--section-start=.data=0x800100                    // sram begins at 256 of 512
 *    -Wl,--section-start=.bootreset=0x0000                 // reset vector is at address 0
 *
 * Should be linked with following options on ATTiny85:
 *    -Wl,--section-start=.text=0x0800                      // section in flash memory begins at 2048 (last 2 KB)
 *    -Wl,--section-start=.data=0x800080                    // sram begins at 128 of 256
 *    -Wl,--section-start=.bootreset=0x0000                 // reset vector is at address 0
 *---------------------------------------------------------------------------------------------------------------------------------------------------
 */

#define BOOTLOADER_STARTADDRESS 0x1800                  // start address: 0x1800 = 6144
#define BOOTLOADER_ENDADDRESS   0x2000                  // end address:   0x2000 = 8192

#define BOOTLOADER_FUNC_ADDRESS (BOOTLOADER_ENDADDRESS - sizeof (BOOTLOADER_FUNCTIONS))

#define LED_PORT                PORTB
#define LED_DDR                 DDRB

#define LED_RED                 1                       // use PB1 for red LED
#define LED_GREEN               0                       // use PB0 for green LED
#define LED_BLUE                4                       // use PB4 for blue LED

#define LED_R_ON                LED_PORT &= ~(1<<LED_RED)
#define LED_R_OFF               LED_PORT |=  (1<<LED_RED)
#define LED_G_ON                LED_PORT &= ~(1<<LED_GREEN)
#define LED_G_OFF               LED_PORT |=  (1<<LED_GREEN)
#define LED_B_ON                LED_PORT &= ~(1<<LED_BLUE)
#define LED_B_OFF               LED_PORT |=  (1<<LED_BLUE)

extern uint8_t                  serial_poll (uint8_t *);
extern uint8_t                  serial_getc (void);
extern void                     start_bootloader (void);

typedef struct
{
    uint8_t                     (*serial_poll) (uint8_t *);
    uint8_t                     (*serial_getc) (void);
    void                        (*start_bootloader) (void);
    void                        (*start_appl_main) (void);
} BOOTLOADER_FUNCTIONS;

#include <inttypes.h>
#include <avr/io.h>
#include <util/delay.h>
#include <avr/eeprom.h>
#include <avr/pgmspace.h>
#include <avr/interrupt.h>
#include <string.h>

#include <avr/boot.h>
#ifndef RWWSRE                                                                  // bug in AVR libc:
#define RWWSRE CTPB                                                             // RWWSRE is not defined on ATTinys, use CTBP instead
#endif

#include "equinox-boot.h"

#define TRUE                                    1
#define FALSE                                   0

#define RJMP    (0xC000U - 1)
uint16_t        boot_reset __attribute__((section(".bootreset"))) = RJMP + BOOTLOADER_STARTADDRESS / 2;
    
  // if (currentAddress.w == RESET_VECTOR_OFFSET * 2) {
  //   data = 0xC000 + (BOOTLOADER_ADDRESS/2) - 1;
  // }

static BOOTLOADER_FUNCTIONS bootloader_functions =
{
    serial_poll,
    serial_getc,
    start_bootloader,
    (void (*)) NULL
};

#define FLASH_RESET_ADDR                        0x0000                          // address of reset vector (in bytes)
#define FLASH_INT0_ADDR                         0x0002                          // address of int0 vector (in bytes)
#define FLASH_PCINT0_ADDR                       0x0004                          // address of pcint0 vector (in bytes)


/*-------------------------------------------------------------------------------------------------------------------------------------------
 * SW serial routines for ATtiny
 *-------------------------------------------------------------------------------------------------------------------------------------------
 */
#define SERIAL_DATA_PIN                         PINB                            // data pin
#define SERIAL_DATA_DDR                         DDRB                            // data ddr
#define SERIAL_DATA_BIT                         PINB3                           // data bit

#define SERIAL_CLK_PIN                          PINB                            // clk pin
#define SERIAL_CLK_DDR                          DDRB                            // clk ddr
#define SERIAL_CLK_BIT                          PINB2                           // clk bit
#define SERIAL_CLK_PCINT                        PCINT2

#define SERIAL_IN_BUF_SIZE                      32                              // input buffer size
#define DATA_PIN_STATUS()                       (SERIAL_DATA_PIN  & (1<<SERIAL_DATA_BIT))
#define CLK_PIN_STATUS()                        (SERIAL_CLK_PIN  & (1<<SERIAL_CLK_BIT))

static volatile uint8_t                         inbuf[SERIAL_IN_BUF_SIZE];
static volatile uint8_t                         qin;

/*---------------------------------------------------------------------------------------------------------------------------------------------------
 * LED: init
 *---------------------------------------------------------------------------------------------------------------------------------------------------
 */
static
void led_init (void)
{
    LED_PORT |= 1<<LED_RED | 1<<LED_GREEN | 1<<LED_BLUE;    // set led pins to 1: off
    LED_DDR  |= 1<<LED_RED | 1<<LED_GREEN | 1<<LED_BLUE;    // set led pins to output
}

/*-------------------------------------------------------------------------------------------------------------------------------------------
 *  SW serial: Initialize
 *-------------------------------------------------------------------------------------------------------------------------------------------
 */
static
void serial_init (void)
{
    SERIAL_DATA_DDR &= ~(1 << SERIAL_DATA_BIT);             // DATA pin as input
    SERIAL_CLK_DDR &= ~(1 << SERIAL_CLK_BIT);               // CLK pin as input
    MCUCR |= (1<<ISC01) | (0<<ISC00);                       // falling edge of INT0 generates an interrupt request
    GIMSK |= (1<<INT0);
}

/*-------------------------------------------------------------------------------------------------------------------------------------------
 * SW serial: receive character without wait
 *-------------------------------------------------------------------------------------------------------------------------------------------
 */
uint8_t
serial_poll (unsigned char * chp)
{
    static uint8_t  static_qout = 0;
    uint8_t         qout;
    uint8_t         tmp_qin;
    uint8_t         rtc;

    cli ();
    tmp_qin = qin;
    sei ();

    qout = static_qout;

    if (tmp_qin != qout)
    {
        *chp = inbuf[qout++];

        if (qout >= SERIAL_IN_BUF_SIZE)
        {
            qout = 0;
        }

        static_qout = qout;
        rtc = 1;
    }
    else
    {
        rtc = 0;
    }

    return rtc;
}

/*-------------------------------------------------------------------------------------------------------------------------------------------
 * SW serial: receive character with wait
 *-------------------------------------------------------------------------------------------------------------------------------------------
 */
uint8_t
serial_getc (void)
{
    uint8_t ch;

    while (! serial_poll (&ch))
    {
        ;
    }
    return ch;
}

/*-----------------------------------------------------------------------------------------------------------------------
 * Flash: fill page word by word
 *-----------------------------------------------------------------------------------------------------------------------
 */
#define boot_program_page_fill(byteaddr, word)      \
{                                                   \
    uint8_t sreg;                                   \
    sreg = SREG;                                    \
    cli ();                                         \
    boot_page_fill ((uint32_t) (byteaddr), word);   \
    SREG = sreg;                                    \
}

/*-----------------------------------------------------------------------------------------------------------------------
 * Flash: erase and write page
 *-----------------------------------------------------------------------------------------------------------------------
 */
#define boot_program_page_erase_write(pageaddr)     \
{                                                   \
    uint8_t sreg;                                   \
    eeprom_busy_wait ();                            \
    sreg = SREG;                                    \
    cli ();                                         \
    boot_page_erase ((uint32_t) (pageaddr));        \
    boot_spm_busy_wait ();                          \
    boot_page_write ((uint32_t) (pageaddr));        \
    boot_spm_busy_wait ();                          \
    boot_rww_enable ();                             \
    SREG = sreg;                                    \
}

/*-----------------------------------------------------------------------------------------------------------------------
 * write a block into flash
 *-----------------------------------------------------------------------------------------------------------------------
 */
static void
pgm_write_block (uint16_t flash_addr, uint16_t * block, size_t size)
{
    uint16_t        start_addr;
    uint16_t        addr;
    uint16_t        w;
    uint8_t         idx = 0;

    start_addr = (flash_addr / SPM_PAGESIZE) * SPM_PAGESIZE;        // round down (granularity is SPM_PAGESIZE)

    for (idx = 0; idx < SPM_PAGESIZE / 2; idx++)
    {
        addr = start_addr + 2 * idx;

        if (addr >= flash_addr && size > 0)
        {
            w = *block++;
            size -= sizeof (uint16_t);
        }
        else
        {
            w = pgm_read_word (addr);
        }

        boot_program_page_fill (addr, w);
    }

    boot_program_page_erase_write(start_addr);                      // erase and write the page
}

/*-----------------------------------------------------------------------------------------------------------------------
 * update flash
 *-----------------------------------------------------------------------------------------------------------------------
 */
static uint8_t
boot_update_flash (void)
{
    uint16_t            start_addr;
    uint16_t            n_pages;
    uint16_t            pgcnt = 0;
    uint8_t             idx;
    uint8_t             chksum;
    uint8_t             ch_l;
    uint8_t             ch_h;
    uint16_t            w;
    uint16_t            reset_vector = pgm_read_word (FLASH_RESET_ADDR);
    uint16_t            int0_vector = pgm_read_word (FLASH_INT0_ADDR);

    start_addr          = (uint16_t) serial_getc () << 8;
    start_addr          |= serial_getc ();

    n_pages             = (uint16_t) serial_getc () << 8;
    n_pages             |= serial_getc ();

    // avoid overwriting the bootloader itself:
    if ((void *) (start_addr + n_pages * SPM_PAGESIZE) >= (void *) BOOTLOADER_STARTADDRESS)
    {
        return FALSE;
    }

    while (pgcnt < n_pages)
    {
        chksum = 0;

        for (idx = 0; idx < SPM_PAGESIZE / 2; idx++)
        {
            ch_l = serial_getc ();
            chksum ^= ch_l;

            ch_h = serial_getc ();
            chksum ^= ch_h;

            w = ch_l | ((uint16_t) ch_h << 8);

            if (start_addr == 0)
            {
                switch (idx)
                {
                    case FLASH_RESET_ADDR / 2:
                    {
                        bootloader_functions.start_appl_main = (void *) (w - RJMP);
                        w = reset_vector;
                        break;
                    }
                    case FLASH_INT0_ADDR / 2:
                    {
                        w = int0_vector;
                        break;
                    }
                }
            }
            boot_program_page_fill (start_addr + 2 * idx, w);
        }

        if (serial_getc () != chksum)
        {
            return FALSE;
        }

        pgcnt++;

        LED_G_ON;
        boot_program_page_erase_write (start_addr);        
        LED_G_OFF;

        start_addr += SPM_PAGESIZE;
    }

    LED_G_ON;
    pgm_write_block (BOOTLOADER_FUNC_ADDRESS, (uint16_t *) &bootloader_functions, sizeof (bootloader_functions));
    LED_G_OFF;

    return TRUE;
}

/*-----------------------------------------------------------------------------------------------------------------------
 * Start Bootloader
 *-----------------------------------------------------------------------------------------------------------------------
 */
void
start_bootloader (void)
{
    uint8_t     ch = 0;
    uint16_t    i;
    uint8_t     success = TRUE;
    void        (*start) (void) = FLASH_RESET_ADDR;

    LED_B_ON;

    for (i = 0; i < 600; i++)
    {
        if (serial_poll (&ch))
        {
            break;
        }
        _delay_ms(5);
    }

    LED_B_OFF;

    if (ch == '$')                                                      // '$' flashes, any other character returns
    {
        success = boot_update_flash ();
        LED_G_OFF;

        if (! success)                                                  // boot_update_flash() failed...
        {
            LED_R_ON;
            _delay_ms(2000);
        }

        (*start) ();
    }
}

/*-----------------------------------------------------------------------------------------------------------------------
 * INT0 Interrupt on CLK, edge falling
 * needs 5µs@8MHz
 *-----------------------------------------------------------------------------------------------------------------------
 */
ISR(INT0_vect)
{
    static uint8_t  static_data_bits = 0;
    static uint8_t  static_data = 0;
    uint8_t         data_bits;          // force register use of sdata, saves 2 bytes
    uint8_t         data;               // force register use of sdata, saves 6 bytes

    data_bits = static_data_bits;
    data = static_data >> 1;

    if (DATA_PIN_STATUS())
    {
        data |= 0x80;
    }

    data_bits++;

    if (data_bits == 8)
    {
        uint8_t     tmp_qin = qin;
        uint8_t *   p       = (uint8_t *) (inbuf + tmp_qin++);

        *p = data;

        if (tmp_qin >= SERIAL_IN_BUF_SIZE)
        {
            tmp_qin = 0;                                                // overflow - reset inbuf-index
        }

        qin = tmp_qin;
        data = 0;                                                       // necessary?
        data_bits = 0;
    }

    static_data = data;
    static_data_bits = data_bits;
}

/*-----------------------------------------------------------------------------------------------------------------------
 * main
 *-----------------------------------------------------------------------------------------------------------------------
 */
int
main ()
{
    uint16_t    rjmp;
    uint8_t     idx;

    rjmp = pgm_read_word (BOOTLOADER_STARTADDRESS) + BOOTLOADER_STARTADDRESS / 2;

    if (rjmp != pgm_read_word (FLASH_RESET_ADDR))
    {
        for (idx = 0; idx < _VECTORS_SIZE; idx += 2)
        {
            rjmp = pgm_read_word (BOOTLOADER_STARTADDRESS + idx) + BOOTLOADER_STARTADDRESS / 2;
            boot_program_page_fill (FLASH_RESET_ADDR + idx, rjmp);
        }
        while (idx < SPM_PAGESIZE)
        {
            boot_program_page_fill (FLASH_RESET_ADDR + idx, 0x0000);
            idx += 2;
        }
        boot_program_page_erase_write (FLASH_RESET_ADDR);
        pgm_write_block (BOOTLOADER_FUNC_ADDRESS, (uint16_t *) &bootloader_functions, sizeof (bootloader_functions));
    }

    led_init ();
    serial_init ();
    _delay_ms (1000);
    sei ();

    for (;;)
    {
        start_bootloader ();

        memcpy_P (&bootloader_functions, (PGM_P) BOOTLOADER_FUNC_ADDRESS, sizeof (bootloader_functions));

        if (bootloader_functions.start_appl_main)
        {
            cli ();
            (*bootloader_functions.start_appl_main) ();
        }
        else
        {
            _delay_ms (25);
        }
    }
}