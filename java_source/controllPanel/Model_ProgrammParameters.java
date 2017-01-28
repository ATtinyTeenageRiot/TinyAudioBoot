/*
 * 
	wave generator for audio bootloader
	
	standard setup 
	
	(c) -C-H-R-I-S-T-O-P-H-   -H-A-B-E-R-E-R- 2011

	This program is free software; you can redistribute it and/or modify
 	it under the terms of the GNU General Public License as published by
 	the Free Software Foundation; either version 2 of the License, or
 	(at your option) any later version.
*/
package controllPanel;

import java.io.File;

public class Model_ProgrammParameters {
	private File inputHexFile;
	private File outputWavFile;
	private int data[];
	
	public Model_ProgrammParameters()
	{
		inputHexFile=new File("test.hex");
		outputWavFile=new File("test.wav");
		
		data=new int[200];
		// test
		for(int n=0;n<200;n++)data[n]=n;
	}
	public void setOutputWavFile(File outputWavFile) {
		this.outputWavFile = outputWavFile;
	}
	public File getOutputWavFile() {
		return outputWavFile;
	}
	public void setInputHexFile(File inputHexFile) {
		this.inputHexFile = inputHexFile;
	}
	public File getInputHexFile() {
		return inputHexFile;
	}
	public void setData(int data[]) {
		this.data = data;
	}
	public int[] getData() {
		return data;
	}
}
