# TinyAudioBoot
Audio Bootloader for Attiny85 Microcontrollers

## What?

You can simply program an Attiny85 via the audio output of a PC, Smartphone or audio-player.

<p align="left">
  <img src="/doc/AudioBootLoaderFlyer.png" width="480"/>
</p>

Only a low-part count circuit is needed.

The bootloader presented here has the following features:

- low memory footprint: ~1KB
- [full Arduino IDE integration] (https://github.com/8BitMixtape/8Bit-Mixtape-NEO/wiki/3_3-IDE-integration)
- automatic Baudrate detection and callibration
- very simple circuit: two 10 k resistors and a 100 nF capacitor are needed to connect the microcontroller to the audio output
  of the PC or audio player
- one pin for data transmission is needed
- an optional led indicator, needs an extra pin, for the state of the boot loader
- as hex/binary to wav converter, programmed in java, is also available. It works on win and linux

## Why?

In some cases it is simpler to play an audio file to program a microcontroller than to install an IDE and dig up
a programming dongle !

A good starting point for using this bootloader can be found here: [8bitmixedtape synthesizer](https://8bitmixtape.github.io/). A collection of ready-to-play files. 

## How?

### installing the bootloader on the Attiny85

Firtly, the bootloader must be flashed onto the microcontroller with an ISP programmer.
You can use an [Arduino-Uno as ISP-pogrammer] (https://www.frag-duino.de/index.php/maker-faq/37-atmel-attiny-85-mit-arduino-arduinoisp-flashen-und-programmieren)

There are precompiled HEX files (one file per configuration) e.g.:

**AudioBootAttiny85_AudioPB4_LedPB1_V3_1.hex**

AudioPB4 means: PB4 is the audio input pin
LEDPB1 means: The LED signal is on PB1

Some fuses have to be programmed too (for the ATTiny85):

	Extended: 0xFE
	HIGH:     0xDD
	LOW:      0xE1

With these settings the ATTiny will run at 16Mhz

If you are using avrdude this is the commandl ine to set the fuses (for a serial com called ttyACM0):
> avrdude -P /dev/ttyACM0 -b 19200 -c avrisp -p t85 -U efuse:w:0xfe:m -U
hfuse:w:0xdd:m -U lfuse:w:0xe1:m

This is the command line to program the bootloader with audio input at PB3 and Led at PB1 (again for ttyACM0):
> avrdude -v -pattiny85 -c avrisp -P/dev/ttyACM0 -b19200
-Uflash:w:/home/dusjagr/Arduino/AttinySound-master/AudioBoot/AudioBootAttiny85_InputPB3_LEDPB1.hex:i


### bootloader operation

1. After reset the bootloader waits for about 5 seconds for a signal from the audio input. 
   During this period the LED blinks at 1/2 Hz
   
2. If there was no signal, the bootloader starts the main program from flash 

3. If there was a signal, the bootloader starts receiving the new program data an flashes it

The sound volume has to be adjusted to a suitable value (some trial and error needed here).
On most PCs the AudioBootloader should work with a **volume setting of 70%** .


## creating the WAV file

### Arduino IDE integration

You could directly integrate the wav-file generator into your Arduino IDE to program your sketches:

[full Arduino IDE integration] (https://github.com/8BitMixtape/8Bit-Mixtape-NEO/wiki/3_3-IDE-integration)

### HEX to WAV java Progam

There is a java program in this repository to convert the hex files to wav files.

**AudioBootAttiny85.jar**

You can start by just clicking on it, if supported by your operating system or from the command line (see below).
The wav-file is created and stored in the same directory where you started the java program. 

You can also use AudioBootAttiny85.jar directly from the command line without starting the GUI with the following command:

> java -jar AudioBootAttiny85.jar someExampleFile.hex

This might be useful if you want to integrate it in your own applications.

## interfacing the Attiny85 with the audio line

You need two resistors and a capacitor as shown in the schematic below.
You could also add a LED as status indicator for the bootloader. It allows to see if the bootloader has started and to quickly check if programming is underway due to the different blinking pattern.

<p align="left">
  <img src="/doc/AudioBootLoaderMinimumBreadBoard.PNG" width="480"/>
</p>




