# TinyAudioBoot
Audio Bootloader for Attiny85 Microcontrollers

## What?

You can simply connect your Attiny85 to the audio jack of your PC, Smartphone or wav-player to program it.

The bootloader presented here has the following features:

- java program to generate the sound, works on win and linux
- [full Arduino IDE integration] (https://github.com/8BitMixtape/8Bit-Mixtape-NEO/wiki/IDE-integration)
- automatic Baudrate detection and callibration
- very simply hardware circuit: 2 resistors and a 100nF capacitor to connect to the audio line
- only one pin for data transmission needed and an additional pin for a status led
- low memory footprint: ~1KB
- a led indicator for the state of the boot loader

## Why?

It is much simplier to play wav files on any device than to install some programming IDEs.
If you can play a wav-file on a device you can use it as programming tool for your Attiny85.
Compared to any other method this is unbeatable easy.
A good example is the [8bitmixedtape synthesizer](https://8bitmixtape.github.io/) where you can directly play the code-wav-files for programming.

## How?

### installing the bootloader on the Attiny85

To use the bootloader it has to be flashed into the microcontroller with an ISP programmer.
You can use an [Arduino-Uno as ISP-pogrammer] (https://www.frag-duino.de/index.php/maker-faq/37-atmel-attiny-85-mit-arduino-arduinoisp-flashen-und-programmieren)

There are precompiled HEX files e.g.:

**AudioBootAttiny85_AudioPB4_LedPB1_V3_1.hex**

AudioPB4 means: PB4 is the audio input pin
LEDPB1 means: The LED signal is on PB1

To work correct it is also necessary to program the Attin85 fuses as follows:

	Extended: 0xFE
	HIGH:     0xDD
	LOW:      0xE1

With this setting the Attiny is running at 16Mhz

If you are using avrdude this is the line to set the fuses:
> avrdude -P /dev/ttyACM0 -b 19200 -c avrisp -p t85 -U efuse:w:0xfe:m -U
hfuse:w:0xdd:m -U lfuse:w:0xe1:m

This is the line to program the bootloader with audio input at PB3 and Led at PB1:
> avrdude -v -pattiny85 -c avrisp -P/dev/ttyACM0 -b19200
-Uflash:w:/home/dusjagr/Arduino/AttinySound-master/AudioBoot/AudioBootAttiny85_InputPB3_LEDPB1.hex:i


### bootloader operation

1. After reset the bootloader waits for about 5 seconds for a signal from the audio input. 
   During this period the LED blinks in a 1 second cycle
   
2. If there was no signal, the bootloader starts the main program in the flash 

3. If there was a signal, the bootloader starts receiving the new program data an flashes it

The sound volume has to be adjusted to a suitable value. 
On most PCs the AudioBootloader should work with a **volume setting of 70%** .

	
## creating the WAV sound

### Arduino IDE integration

You could directly integrate the wav-file generator into your Arduino IDE to program your sketches:

[full Arduino IDE integration] (https://github.com/8BitMixtape/8Bit-Mixtape-NEO/wiki/IDE-integration)

### HEX to WAV java Progam

There is a java program here in this repository

**AudioBootAttiny85.jar**

which on the most operating systems you can start by just clicking on it. 
The wav-file is created and stored in the same directory where you started the java program. 

You can also use AudioBoot.jar directly from the command line without starting the GUI with the following command:

> java -jar AudioBoot.jar someExampleFile.hex

This might be usefull if you want to integrate it in your own applications.

## interfacing the Attiny85 with the audio line

It is quite easy to connect the Attiny to the PC. You need only two resistors and a capacitor:

<p align="left">
  <img src="/doc/audioInputSchematic.PNG" width="480"/>
</p>




