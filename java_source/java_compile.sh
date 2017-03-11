rm -fr */*.class
javac -classpath . */*.java
jar cvfm hex2wav.jar MANIFEST.MF */*.class
#java -cp . controllPanel/Main_WavBootLoader
java -jar hex2wav.jar /Users/xcorex/Documents/Projects/AVR/8BitMixtapeNeo/TinyAudioBoot/build/AudioBootAttiny85_AudioPB3_LEDPB1.hex
