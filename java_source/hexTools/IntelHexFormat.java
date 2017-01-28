package hexTools;

/*
 * Created on Apr 29, 2005
 *
 * Copyright (c) 2005 by ETH Zurich
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY ETH ZURICH AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL ETH ZURICH
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 */

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;

/**
 * Class for handling Intel Hex Format Files.
 * Supports building of greater blocks.
 *
 * @author <a href=mailto:yuecelm{at}ee{dot}ethz{dot}ch>Mustafa Yuecel</a>
 */
public class IntelHexFormat
{
    /** enable debugMode */
    private final static boolean debugMode = true;

    /** default maximum length of a block */
    private final static int maxblocklen = Integer.MAX_VALUE;

    //--- Methods (public)
    
    /**
     * Convert IntelHex file to a formatted byte array
     *
     * @param fp a <code>File</code> instance
     * @return formatted byte array (ch? 0 length_high length_low 0 startaddress_high startaddress_low ) 
     * @throws Exception
     */
    public static byte[] IntelHexFormatToByteArray(File fp) throws Exception
    {
        return IntelHexFormatToByteArray(fp, maxblocklen);
    }
    public static int toUnsignedInt(byte value)	
    {
    	return (value & 0x7F) + (value < 0 ? 128 : 0);
    }
    public static int[] toUnsignedIntArray(byte value[])	
    {
    	int[] erg=new int[value.length];

    	for(int n=0;n<erg.length;n++) erg[n]=(int) (value[n] & 0x7F) + (value[n] < 0 ? 128 : 0);

    	return erg;
    }
	public static void anzeigen(byte[] daten)
	{
		int n,z;
		
		int length_high=toUnsignedInt(daten[1]);
		int length_low=toUnsignedInt(daten[2]);
		
		int laenge=(length_high<<8)+length_low;

		if(debugMode)System.out.printf("%x ;",laenge);
		if(debugMode)System.out.println(laenge);
		
		if (laenge>10000) laenge=10000;
		int spalten=16;
		int anfang=6;
		
		String s="";
		
		z=0;
		for(n=0;n< laenge;n++)
		{			

			if(z==spalten) 
			{
				if(debugMode)System.out.printf("  %s",s);
				s="";
				if(debugMode)System.out.println();
				z=0;
			}
			if(z==0)
			{
				
				if(debugMode)System.out.printf("%04x: ",n);
			}
			if(debugMode)System.out.printf("%02x ",daten[anfang+n]);
			Character c=(char) daten[anfang+n];
			
			if((c>32)&&(c<255)) s=s+c;
			else s=s+".";
			z++;
		}

	}	
    /**
     * Convert IntelHex file to a formatted byte array
     *
     * @param fp a <code>File</code> instance
     * @param maxblocklen maximum length of a block
     * @return formatted byte array
     * @throws Exception
     */
    public static byte[] IntelHexFormatToByteArray(File fp, int maxblocklen)
        throws Exception
    {
        if (debugMode)
            System.out.println("Parse the Intel Hex File...");

        // reserve enough space
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteArrayOutputStream datablock = new ByteArrayOutputStream();

        BufferedReader in = new BufferedReader(new FileReader(fp));

        String stringline = null;
        byte[] byteline;
        int data_length;
        int data_address;
        int block_length = 0;
        int block_address = -1;
        int block_address_offset = 0;

        for (;;)
        {
            stringline = in.readLine();

            // EOF reached
            if ( stringline == null )
                throw new Exception("No End Of File record in file");

            // start code exists?
            if ( stringline.length() == 0 || stringline.charAt(0) != ':' )
                continue;

            // is Data record?
            if ( !stringline.substring(7,9).equals("00") )
            {
                if ( stringline.substring(7,9).equals("01") )
                {
                    // End Of File record

                    // write datablock if necessary
                    if ( block_length > 0)
                    {
                        // write header and datablock into baos
                        writeHeader(baos, block_length, block_address);
                        // write datablock into baos
                        datablock.writeTo(baos);
                    }

                    break;
                }
                else if ( stringline.substring(7,9).equals("02") )
                {
                    // Extended Segment Address Records (HEX86)

                    int len = new BigInteger(stringline.substring(1,3),16
                            ).intValue();
                    block_address_offset =
                        new BigInteger(stringline.substring(9,9+2*len),16
                                ).intValue();
                    block_address_offset *= 16;  // byte_shift
                    if (debugMode)
                        System.out.println("Extend address record. Offset: 0x"
                                + Integer.toHexString(block_address_offset
                                        ).toUpperCase());

                    continue;
                }
                else
                    // jump to the next line
                    continue;
            }

            // convert hex-string w/o start code to byte array
            byteline = new BigInteger(stringline.substring(1),16
                    ).toByteArray();

            // checksum is correct?
            checkSum(byteline);

            data_length = new BigInteger(stringline.substring(1,3),16
                    ).intValue();
            data_address = new BigInteger(stringline.substring(3,7),16
                    ).intValue();

            if ( ( block_address + block_length != data_address + 
                    block_address_offset )
                    || ( block_length+data_length > maxblocklen ) )
            {
                // write last block header?
                if (block_address != -1)
                {
                    // write header into baos
                    writeHeader(baos, block_length, block_address);
                    // write datablock into baos
                    datablock.writeTo(baos);
                }

                datablock.reset();
                block_length = 0;
                block_address = data_address + block_address_offset;
            }

            // write data
            datablock.write(byteline,4,data_length);
            // update length
            block_length += data_length;
        }

        return baos.toByteArray();
    }
    public static byte[] discardHeaderBytes(byte[] data)
    {
    	int headerOffset=6;
    	byte[] newData= new byte[data.length-headerOffset];
    	for(int n=0;n<data.length-headerOffset;n++)newData[n]=data[n+headerOffset];
    	return newData;
    }

    /**
     * Convert integer to a fixed sized byte array.
     * Empty bytes will be filled with 0.
     * Oversized array will be cutted to fit into the byte array.
     *
     * @param integer number to convert
     * @param bytelen the length of the returned byte array
     * @return formatted byte array
     * @throws Exception
     */
    public static byte[] IntToFixedByteArray(int integer, int bytelen)
        throws Exception
    {
        // not the best implementation...
        // but it should works

        ByteArrayOutputStream baos = new ByteArrayOutputStream(bytelen);
        byte[] b = new BigInteger(new Integer(integer).toString()
                ).toByteArray();

        if ( b.length == bytelen )
            baos.write(b);
        else if ( b.length > bytelen )
        {
            // cut array
            baos.write(b,b.length-bytelen,bytelen);
        }
        else
        {
            // fill up with '\0'
            for (int i=0;i<bytelen-b.length;i++)
                baos.write(0);
            baos.write(b);
        }

        return baos.toByteArray();
    }

    /**
     * Check if file is in IntelHex format
     *
     * @param fp a <code>File</code> instance
     * @return true, if file is in IntelHex format
     */
    public static boolean isIntelHexFormat(File fp)
    {
        BufferedReader in;
        String stringline;
        byte[] byteline;

        try
        {
            in = new BufferedReader(new FileReader(fp));
        }
        catch (FileNotFoundException e)
        {
            return false;
        }

        for (;;)
        {
            try
            {
                stringline = in.readLine();
            }
            catch (IOException e)
            {
                return false;
            }

            // EOF reached
            if ( stringline == null )
                return false;
            // start code exists?
            if ( stringline.length() == 0 || stringline.charAt(0) != ':' )
                continue;
            // convert hex-string w/o start code to byte array
            byteline = new BigInteger(stringline.substring(1),16
                    ).toByteArray();

            try
            {
                // checksum is correct?
                checkSum(byteline);
            }
            catch (Exception e)
            {
                return false;
            }

            if ( stringline.substring(7,9).equals("01") )
                break;

        }

        return true;
    }
        
    //--- Methods (private)

    /**
     * Calculate checksum (modulo 256)
     * 
     * @param b byte array
     * @throws Exception
     */
    private static void checkSum(byte[] b) throws Exception
    {
        int checksum = 0;

        for (int i=0;i<b.length;i++)
        {
            checksum += b[i];
            checksum %= 256;   // 2^8
        }

        if ( checksum != 0 )
            throw new Exception("Checksum of file not correct");
    }

    /**
     * Write header into <code>baos</code>
     * 
     * @param baos write header into ByteArrayOutputStream
     * @param length length information to write into header
     * @param address address information to write into header
     * @throws Exception
     */
    private static void writeHeader(ByteArrayOutputStream baos, int length,
            int address) throws Exception
    {
        // write header and datablock into baos
        if (debugMode)
            System.out.println("Write Header: Length: " + length +
                    " (0x" + Integer.toHexString(length).toUpperCase() +
                    ") Address: 0x" + 
                    Integer.toHexString(address).toUpperCase());

        // write length
        baos.write(IntToFixedByteArray(length,3));
        // write address
        baos.write(IntToFixedByteArray(address,3));
    }
    public static void main(String[] args) {
    	
    	try {
    		
    	    File f = new File("C:\\Dokumente und Einstellungen\\chris\\Eigene Dateien\\Entwicklung\\java\\EclipseWorkspace2\\wavBootLoader\\test.hex");
    	    //File f = new File("C:\\Dokumente und Einstellungen\\chris\\Desktop\\AnalyserAsuroBoot\\small.hex");

    		byte[] erg = IntelHexFormatToByteArray(f);
    		if(debugMode)System.out.println(erg);
/*    		
    	    int ende=erg.length;

    		for(int n=0;n<ende;n++)
    		{
    			//System.out.println(erg[n]);
    			System.out.printf("%x ",erg[n]);
    		}
*/
    		anzeigen(erg);
    		//File f = new File("datafile");
    		
    	    //HexFileParser hfp = new HexFileParser(new File("C:\\Dokumente und Einstellungen\\chris\\Desktop\\AnalyserAsuroBoot\\test.hex"));
    	    //hfp.parseFile();
    	} catch (Exception e) {
    	    e.printStackTrace();
    	}
        }


}
