#include <avr/io.h>
#include <avr/interrupt.h>
#include <avr/signal.h>
#include <stdlib.h>
#include <avr/boot.h>

#define LEDPORT (1<<PB1); //PB1 pin 6 Attiny85
#define INITLED {DDRB|=LEDPORT;}

#define LEDON {PORTB|=LEDPORT;}
#define LEDOFF {PORTB&=~LEDPORT;}
#define TOGGLELED {PORTB^=LEDPORT;}

#define INPUTAUDIOPIN (1<<PB0) //PB0 pin 5 attiny85
#define PINVALUE (PINB&INPUTAUDIOPIN)

#define INITPORT {PORTB|=INPUTAUDIOPIN;} //turn on pull up 

#define PINLOW (PINVALUE==0)
#define PINHIGH (!PINLOW)

#define WAITBLINKTIME 500


#define true (1==1)
#define false !true


//***************************************************************************************
// main loop
//***************************************************************************************

#define TIMER TCNT1 // we use timer1 for measuring time

// frame format definition
#define COMMAND         0
#define PAGEINDEXLOW 	1  // page address lower part
#define PAGEINDEXHIGH 	2  // page address higher part
#define CRCLOW          3  // checksum lower part 
#define CRCHIGH 		4  // checksum higher part 
#define DATAPAGESTART   5  // start of data
#define PAGESIZE 		128
#define FRAMESIZE       (PAGESIZE+DATAPAGESTART)// size of the data block to be received

// bootloader commands
#define NOCOMMAND       0
#define TESTCOMMAND     1
#define PROGCOMMAND     2
#define RUNCOMMAND      3

uint8_t FrameData[FRAMESIZE];

//***************************************************************************************
// receiveFrame()
//
// This routine receives a differential manchester coded signal at the input pin.
// The routine waits for a toggling voltage level.
// It automatically detects the transmission speed.
//
// output: 		uint8_t flag: true: checksum ok
//				Data // global variable
//
//***************************************************************************************
uint8_t receiveFrame()
{
  uint16_t store[16];

  uint16_t counter = 0;
  volatile uint16_t time = 0;
  volatile uint16_t delayTime;
  uint8_t p, t;
  uint8_t k = 8;
  uint8_t dataPointer = 0;
  uint16_t n;

  //*** synchronisation and bit rate estimation **************************
  time = 0;
  // wait for edge
  p = PINVALUE;
  while (p == PINVALUE);

  p = PINVALUE;

  TIMER = 0; // reset timer
  for (n = 0; n < 16; n++)
  {
    // wait for edge
    while (p == PINVALUE);
    t = TIMER;
    TIMER = 0; // reset timer
    p = PINVALUE;

    store[counter++] = t;

    if (n >= 8)time += t; // time accumulator for mean period calculation only the last 8 times are used
  }

  delayTime = time * 3 / 4 / 8;
  // delay 3/4 bit
  while (TIMER < delayTime);

  //p=1;

  //****************** wait for start bit ***************************
  while (p == PINVALUE) // while not startbit ( no change of pinValue means 0 bit )
  {
    // wait for edge
    while (p == PINVALUE);
    p = PINVALUE;
    TIMER = 0;

    // delay 3/4 bit
    while (TIMER < delayTime);
    TIMER = 0;

    counter++;
  }
  p = PINVALUE;
  //****************************************************************
  //receive data bits
  k = 8;
  for (n = 0; n < (FRAMESIZE * 8); n++)
  {
    // wait for edge
    while (p == PINVALUE);
    TIMER = 0;
    p = PINVALUE;

    // delay 3/4 bit
    while (TIMER < delayTime);

    t = PINVALUE;

    counter++;

    FrameData[dataPointer] = FrameData[dataPointer] << 1;
    if (p != t) FrameData[dataPointer] |= 1;
    p = t;
    k--;
    if (k == 0) {
      dataPointer++;
      k = 8;
    };
  }
  uint16_t crc = (uint16_t)FrameData[CRCLOW] + FrameData[CRCHIGH] * 256;


  if (crc == 0x55AA) return true;
  else return false;
}

//***************************************************************************************
//	void boot_program_page (uint32_t page, uint8_t *buf)
//
//  Erase and flash one page.
//
//  inputt: 		page address and data to be programmed
//
//***************************************************************************************
void boot_program_page (uint32_t page, uint8_t *buf)
{
  uint16_t i;

  cli(); // disable interrupts

  boot_page_erase (page);
  boot_spm_busy_wait ();      // Wait until the memory is erased.

  for (i = 0; i < SPM_PAGESIZE; i += 2)
  {
    uint16_t w = *buf++;
    w += (*buf++) << 8;

    boot_page_fill (page + i, w);
    boot_spm_busy_wait();       // Wait until the memory is written.
  }

  boot_page_write (page);     // Store buffer in flash page.
  boot_spm_busy_wait();       // Wait until the memory is written.

  //boot_rww_enable ();
}
//***************************************************************************************
void initstart()
{
  // Timer 2 normal mode, clk/8, count up from 0 to 255
  // ==> frequency @16MHz= 16MHz/8/256=7812.5Hz
  TCCR1 = _BV(CS12);
}
//***************************************************************************************
void runProgramm(void)
{
  // reintialize registers to default
  DDRB = 0;
  cli();
  TCCR1 = 0; // turn off timer1
  // start user programm
  asm volatile(
    "clr r30	\n\t"
    "clr r31	\n\t"	// z Register mit Adresse laden
    "ijmp		\n\t"	// z Register mit Adresse laden
  );
}

//***************************************************************************************
// main loop
//***************************************************************************************
void a_main()
{
  initstart();
  uint8_t p;
  uint16_t time = WAITBLINKTIME;
  uint8_t timeout = 6;

  //*************** wait for toggling input pin or timeout ******************************
  uint8_t exitcounter = 3;
  while (1)
  {

    if (TIMER > 100) // timedelay ==> frequency @16MHz= 16MHz/8/100=20kHz
    {
      TIMER = 0;
      time--;
      if (time == 0)
      {
        TOGGLELED;
        time = WAITBLINKTIME;
        timeout--;
        if (timeout == 0)
        {
          LEDOFF; // timeout,
          // leave bootloader and run program
          runProgramm();
        }
      }
    }
    if (p != PINVALUE)
    {
      p = PINVALUE;
      exitcounter--;
    }
    if (exitcounter == 0) break; // signal received, leave this loop and go on
  }
  //*************** start command interpreter *************************************
  LEDON;
  while (1)
  {
    if (!receiveFrame())
    {
      //*****  error: blink fast, press reset to restart *******************
      while (1)
      {
        if (TIMER > 100) // timerstop ==> frequency @16MHz= 16MHz/8/100=20kHz
        {
          TIMER = 0;
          time--;
          if (time == 0)
          {
            TOGGLELED;
            time = 1000;
          }
        }
      }
    }
    else // succeed
    {
      TOGGLELED;
      switch (FrameData[COMMAND])
      {
        case TESTCOMMAND: // not used yet
          {

          }
          break;
        case RUNCOMMAND:
          {
            // leave bootloader and run program
            runProgramm();
          }
          break;
        case PROGCOMMAND:
          {
            //todo attiny85
            // Atmega168 Pagesize=64 Worte=128 Byte
            uint16_t k;
            k = (((uint16_t)FrameData[PAGEINDEXHIGH]) << 8) + FrameData[PAGEINDEXLOW];
            boot_program_page (SPM_PAGESIZE * k, FrameData + DATAPAGESTART);	// erase and programm page
          }
          break;
      }
      FrameData[COMMAND] = NOCOMMAND; // delete command
    }
  }
}



int main()
{
  INITLED;
  INITPORT;
  a_main(); // start the main function
}
