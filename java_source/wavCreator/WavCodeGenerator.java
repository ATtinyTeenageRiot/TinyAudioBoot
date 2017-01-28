/*
 * 
	wave generator for audio bootloader
	
	waveform generation: building the audio signal
	
	(c) -C-H-R-I-S-T-O-P-H-   -H-A-B-E-R-E-R- 2011

	This program is free software; you can redistribute it and/or modify
 	it under the terms of the GNU General Public License as published by
 	the Free Software Foundation; either version 2 of the License, or
 	(at your option) any later version.
*/
package wavCreator;

import hexTools.IntelHexFormat;

import java.io.*;
import java.util.ArrayList;
import java.util.List;


import waveFile.WavFile;

public class WavCodeGenerator {
	private int sampleRate = 44100;		// Samples per second
	private BootFrame frameSetup;
	
	public WavCodeGenerator()
	{
		frameSetup=new BootFrame();	
	}
	private double[] appendSignal(double[] sig1, double[] sig2)
	{
		int l1=sig1.length;
		int l2=sig2.length;
		double[] d=new double[l1+l2];
		for(int n=0;n<l1;n++) d[n]=sig1[n];
		for(int n=0;n<l2;n++) d[n+l1]=sig2[n];		
		return d;
	}
	public double[] generatePageSignal(int data[])
	{
		HexToSignal h2s=new HexToSignal();

		int[] frameData=new int[frameSetup.getFrameSize()];

		// copy data into frame data
		for(int n=0;n<frameSetup.getPageSize();n++)
		{
			if(n<data.length) frameData[n+frameSetup.getPageStart()]=data[n];
			else frameData[n+frameSetup.getPageStart()]=0xFF;
		}
		frameSetup.addFrameParameters(frameData);
		double[] signal=h2s.manchesterCoding(frameData);
		return signal;
	}
	// duration in seconds
	public double[] silence(double duration)
	{
		double[] signal=new double[(int)(duration * sampleRate)];
		return signal;
	}
	public double[] makeRunCommand()
	{
		HexToSignal h2s=new HexToSignal();
		int[] frameData=new int[frameSetup.getFrameSize()];
		frameSetup.setRunCommand();
		frameSetup.addFrameParameters(frameData);
		double[] signal=h2s.manchesterCoding(frameData);
		return signal;
	}
	public double[] generateSignal(int data[])
	{
		double[] signal=new double[1];
		frameSetup.setProgCommand(); // we want to programm the mc
		int pl=frameSetup.getPageSize();
		int total=data.length;
		int sigPointer=0;
		int pagePointer=0;
		while(total>0)
		{
			frameSetup.setPageIndex(pagePointer++);
			int[] partSig=new int[pl];
			for(int n=0;n<pl;n++)
			{
				if(n+sigPointer>data.length-1) partSig[n]=0xFF;
				else partSig[n]=data[n+sigPointer];
			}
			sigPointer+=pl;
			double[] sig=generatePageSignal(partSig);
			signal=appendSignal(signal,sig);
			signal=appendSignal(signal,silence(frameSetup.getSilenceBetweenPages()));
			total-=pl;
		}
		signal=appendSignal(signal,makeRunCommand()); // send mc "start the application"
		return signal;
	}
	
	public boolean saveWav(double[] signal, File fileName)
	{		
		try
		{
			// Calculate the number of frames required for specified duration
			//long numFrames = (long)(duration * sampleRate);
			long numFrames=signal.length;
			// Create a wav file with the name specified as the first argument
			WavFile wavFile = WavFile.newWavFile(fileName, 2, numFrames, 16, sampleRate);

			// Create a buffer of 100 frames
			double[][] buffer = new double[2][100];

			// Initialise a local frame counter
			long frameCounter = 0;

			// Loop until all frames written
			while (frameCounter < numFrames)
			{
				// Determine how many frames to write, up to a maximum of the buffer size
				long remaining = wavFile.getFramesRemaining();
				int toWrite = (remaining > 100) ? 100 : (int) remaining;

				// Fill the buffer, one tone per channel
				for (int s=0 ; s<toWrite ; s++, frameCounter++)
				{
					if(frameCounter<signal.length)
					{
						buffer[0][s] = signal[(int)frameCounter];
						buffer[1][s] = signal[(int)frameCounter];						
					}else
					{
						buffer[0][s] = Math.sin(2.0 * Math.PI * 400 * frameCounter / sampleRate);
						buffer[1][s] = Math.sin(2.0 * Math.PI * 500 * frameCounter / sampleRate);
					}
				}
				// Write the buffer
				wavFile.writeFrames(buffer, toWrite);
			}

			// Close the wavFile
			wavFile.close();
		}
		catch (Exception e)
		{
			System.err.println(e);
			return false;
		}
		return true;
	}
	public static boolean convertHex2Wav(File hexFile, File wavFile) throws Exception
	{
		//IntelHexFormat ih=new IntelHexFormat();
		byte[] erg = IntelHexFormat.IntelHexFormatToByteArray(hexFile);
		IntelHexFormat.anzeigen(erg);
		WavCodeGenerator w=new WavCodeGenerator();
		double[] signal=w.generateSignal(IntelHexFormat.toUnsignedIntArray(IntelHexFormat.discardHeaderBytes(erg)));
		w.saveWav(signal,new File("test.wav"));
		return true;
	}
	public static void main(String[] args) throws Exception
	{
   	    File f1 = new File("C:\\Dokumente und Einstellungen\\chris\\Eigene Dateien\\Entwicklung\\java\\EclipseWorkspace2\\wavBootLoader\\test.hex");
   	    File f2 = new File("C:\\Dokumente und Einstellungen\\chris\\Eigene Dateien\\Entwicklung\\java\\EclipseWorkspace2\\wavBootLoader\\test.wav");
   	    convertHex2Wav(f1,f2);
	}
}
