# TinyAudioBoot

Audio Bootloader for Attiny85 Microcontrollers

It is possible to load a programm into an Attiny85-microcontroller over the audio interface of a PC or laptop.

The bootloader presented here has the following features:

- automatic Baudrate detection and callibration
- very simply hardware circuit: 2 resistors and a 100nF capacitor to connect to the audio line
- only one pin for data transmission needed and an additional pin for a status led
- low memory footprint: ~1KB
- java program to generate the sound, works on win and linux
- a led indicator for the state of the boot loader

<p align="left">
  <img src="/doc/audioInputSchematic.PNG" width="480"/>
</p>

## bootloader operation

1. After reset the bootloader waits for about 5 seconds for a signal from the audio input. 
   During this period the LED blinks in a 1 second cycle
   
2. If there was no signal, the bootloader starts the main program in the flash 

3. If there was a signal, the bootloader starts receiving the new program data an flashes it

## installing the bootloader on the Attiny85

To use the bootloader it has to be flashed into the microcontroller with an ISP programmer.

There are precompiled versions e.g.:

AudioBootAttiny85_AudioPB4_LedPB1_V3_1.hex

AudioPB4 means: PB4 is the audio input pin
LEDPB1 means: The LED signal is on PB1

To work correct it is also necessary to program the Attin85 fuses as follows:

	Extended: 0xFE
	HIGH:     0xDD
	LOW:      0xE1

With this setting the Attiny is running at 16Mhz
	
## creating the WAV sound

There is a Java Program

**AudioBootAttiny85.jar**

which on the most operating systems you can start by just clicking on it. 
The wav-file is created and stored in the same directory where you started the java program. 

You can also use AudioBoot.jar directly from the command line without starting the GUI with the following command:

> java -jar AudioBoot.jar someExampleFile.hex

This might be usefull if you want to integrate it in your own applications.





