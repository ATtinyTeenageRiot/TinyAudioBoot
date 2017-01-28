/*
 * 
	wave generator for audio bootloader
	
	frame format
	
	(c) -C-H-R-I-S-T-O-P-H-   -H-A-B-E-R-E-R- 2011

	This program is free software; you can redistribute it and/or modify
 	it under the terms of the GNU General Public License as published by
 	the Free Software Foundation; either version 2 of the License, or
 	(at your option) any later version.
*/
package wavCreator;

public class BootFrame {

	/*
	 * 	#define COMMAND         0
		#define PAGEINDEXLOW 	1  // page address lower part
		#define PAGEINDEXHIGH 	2  // page address higher part
		#define CRCLOW          3  // checksum lower part
		#define CRCHIGH 		4  // checksum higher part
		#define DATAPAGESTART   5  // start of data
		#define FRAMESIZE       (DATAPAGESTART+128) // size of the data block to be received
	 */
	
	private int command;
	private int pageIndex;
	private int crc;
	private int pageStart=5;
	private int pageSize=64;
	private int frameSize=pageStart+pageSize;
	
	//private double silenceBetweenPages=2; // 2 seconds for debugging purposes silence in seconds
	private double silenceBetweenPages=0.02; // silence in seconds
	
	public BootFrame()
	{
		command=0;
		pageIndex=4;
		crc=0x55AA;
	}
	public void setProgCommand()
	{
		command=2;
	}
	public void setRunCommand()
	{
		command=3;
	}
	public int[] addFrameParameters(int data[])
	{
		data[0]=command;
		data[1]=pageIndex&0xFF;
		data[2]=(pageIndex>>8)&0xFF;
		data[3]=crc&0xFF;
		data[4]=(crc>>8)&0xFF;
		return data;
	}
	public void setFrameSize(int frameSize) {
		this.frameSize = frameSize;
	}
	public int getFrameSize() {
		return frameSize;
	}
	public void setCommand(int command) {
		this.command = command;
	}
	public int getCommand() {
		return command;
	}
	public void setPageIndex(int pageIndex) {
		this.pageIndex = pageIndex;
	}
	public int getPageIndex() {
		return pageIndex;
	}
	public void setCrc(int crc) {
		this.crc = crc;
	}
	public int getCrc() {
		return crc;
	}
	public void setPageStart(int pageStart) {
		this.pageStart = pageStart;
	}
	public int getPageStart() {
		return pageStart;
	}
	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}
	public int getPageSize() {
		return pageSize;
	}
	public void setSilenceBetweenPages(double silenceBetweenPages) {
		this.silenceBetweenPages = silenceBetweenPages;
	}
	public double getSilenceBetweenPages() {
		return silenceBetweenPages;
	}
}
