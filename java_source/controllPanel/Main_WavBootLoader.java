/*
 * 
	wave generator for audio bootloader
	
	main startup / GUI-interface 
	
	(c) -C-H-R-I-S-T-O-P-H-   -H-A-B-E-R-E-R- 2011

	This program is free software; you can redistribute it and/or modify
 	it under the terms of the GNU General Public License as published by
 	the Free Software Foundation; either version 2 of the License, or
 	(at your option) any later version.
*/

package controllPanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.filechooser.FileFilter;

import wavCreator.WavCodeGenerator;
import waveFile.AePlayWave;

public class Main_WavBootLoader extends JPanel{
		
	static final long serialVersionUID = 1L;
	public JFrame frame;
	public JTextArea messageText;
	
	//%Variable_Section%//
	public JButton   button_InputHexFile;
	public JButton   button_ouputWavFile;
	public JButton   button_writeWav;
	public JCheckBox speedCheckBox;
	public JTextArea testText;
	public Model_ProgrammParameters setupData;
	
	public void showMainWindow()
	{
		speedCheckBox = new JCheckBox("slow");

		frame= new JFrame(); // create main window
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setTitle("WavBootWriter");
		frame.getContentPane().setLayout(null); // kein Layout Manager für freie Größenwahl
		
		JPanel panel        = new JPanel();
		panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
		
		messageText         = new JTextArea(5,20); // Lauftext Fenster
	
		messageText.setBounds(100,200,200,250);	

		button_InputHexFile = new JButton("select hexfile");
		button_ouputWavFile = new JButton("wav filename");
		button_writeWav     = new JButton("play wav file");
		button_writeWav.setBackground(Color.GREEN);
		//button_writeWav.setForeground(Color.GRAY);
		
		inputHexFile_ButtonListener inputHexFileButtonListener = new inputHexFile_ButtonListener();
		button_InputHexFile.addActionListener(inputHexFileButtonListener);

		writeWav_ButtonListener writeWavButtonListener = new writeWav_ButtonListener();
		button_writeWav.addActionListener(writeWavButtonListener);
		testText=new JTextArea("bootloader for audio line");
		testText.append("\n\n");
		testText.setLineWrap(true);
		JScrollPane scrollText=new JScrollPane(testText);
		scrollText.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		//scrollText.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		
		// alle Panels in Hauptframe einbauen
	  	frame.setLayout(new BorderLayout(5,5));
		panel.add(button_InputHexFile);

		panel.add(button_writeWav);
		frame.add(panel,BorderLayout.LINE_END);
		frame.add(scrollText,BorderLayout.CENTER);

        panel.add(speedCheckBox);
        
		frame.setSize(640,480);
		frame.setVisible(true);
		
		testText.append("selected hex-file:\n");
		testText.append(setupData.getInputHexFile().getAbsolutePath());
		testText.append("\n\n");
		testText.append("output wav-file:\n");
		testText.append(setupData.getOutputWavFile().getAbsolutePath());
		testText.append("\n");
	}
	
	public Main_WavBootLoader()
	{
		setupData=new Model_ProgrammParameters(); 
	}

	public static String getBaseName(String fileName) {
	    int index = fileName.lastIndexOf('.');
	    if (index == -1) {
	        return fileName;
	    } else {
	        return fileName.substring(0, index);
	    }
	}
	class inputHexFile_ButtonListener implements ActionListener
	{
	
		public void actionPerformed(ActionEvent e) {
			System.out.println(e.getActionCommand());

        	System.out.println("File Button pressed");
        	// get current file path
        	File ff=new File(".");
        	try {
				ff.getCanonicalPath();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        	JFileChooser fc = new JFileChooser(ff);
        
        	fc.setFileFilter(new FileFilter()
        	{
        		@Override public boolean accept ( File f)
        		{
        			return f.isDirectory() || f.getName().toLowerCase().endsWith(".hex");
        		}
        		@Override public String getDescription()
        		{
        			return "Hex-Files";
        		}	        		
        	});
        	
        	int state=fc.showOpenDialog(null);
        	if(state==JFileChooser.APPROVE_OPTION)
        	{
        		File file=fc.getSelectedFile();
    			
        		testText.append("selected hexfile:\n");
        		testText.append(file.getAbsolutePath());
        		setupData.setInputHexFile(file);
        		
        		String outputFileName=getBaseName( file.getName() )+".wav";
        		System.out.println("output file name:"+outputFileName);
        		setupData.setOutputWavFile( new File( outputFileName ) ); 
        	};	
		}	
	}
	
	class writeWav_ButtonListener implements ActionListener
	{		
		public void actionPerformed(ActionEvent e) 
		{
			System.out.println(e.getActionCommand());
			testText.append("\nconverting hex to wav\n");
			try 
			{
				WavCodeGenerator wg=new WavCodeGenerator();
				wg.setSignalSpeed(!speedCheckBox.isSelected());
				wg.convertHex2Wav(setupData.getInputHexFile() ,setupData.getOutputWavFile());
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			testText.append("done\n");
			
			testText.append("playing wav-file\n");
        	new AePlayWave(setupData.getOutputWavFile().toString()).start();
			testText.append("done\n");

		}
	}

	public void convertAndPlayWav()
	{
		System.out.println("\nconverting hex to wav\n");


		try {
			WavCodeGenerator wg=new WavCodeGenerator();

			wg.setSignalSpeed(true);
			wg.convertHex2Wav(setupData.getInputHexFile() ,setupData.getOutputWavFile());
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		System.out.println("done\n");
		
		System.out.println("playing wav-file\n");
    	new AePlayWave(setupData.getOutputWavFile().toString()).start();
	}
	
	public static void main(String[] args) 
	{
    	Main_WavBootLoader w=new Main_WavBootLoader();
    	
    	String property = "java.io.tmpdir";
    	String tempDir = System.getProperty(property);

    	System.out.println("convert hex-file to speaker sound : java -jar AudioBoot.jar testFile.hex");
		
    	if(args.length>0) // command line arguments: run in shell, do not show window
        {
        	System.out.println("there are "+args.length+"command-line arguments.");
        	for(int i=0;i<args.length;i++) System.out.println("args["+i+"]:"+args[i]);
    		File file=new File(args[0]);
    		String outputFileName=getBaseName( file.getName() )+".wav";

    	    String absolutePath = file.getAbsolutePath();
    	    String filePath = absolutePath.substring(0,absolutePath.lastIndexOf(File.separator));

        	w.setupData.setInputHexFile(file); 

        	w.setupData.setOutputWavFile(new File(filePath + File.separator + outputFileName));   	
  	
        	w.convertAndPlayWav();
        }
        else // no command line available arguments, run GUI
        {
        	w.showMainWindow();
        }
    	
		System.out.println("End");

	}

}
