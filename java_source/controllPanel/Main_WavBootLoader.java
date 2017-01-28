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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.JButton;
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
	public JButton button_InputHexFile;
	public JButton button_ouputWavFile;
	public JButton button_writeWav;
	public JTextArea testText;
	public Model_ProgrammParameters setupData;

	public Main_WavBootLoader()
	{
		frame= new JFrame(); // Hautpfenster erzeugen
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setTitle("WavBootWriter");
		frame.getContentPane().setLayout(null); // kein Layout Manager für freie Größenwahl
		
		JPanel panel=new JPanel();
		panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
		
		messageText=new JTextArea(5,20); // Lauftext Fenster
	
		messageText.setBounds(100,200,200,250);	

		button_InputHexFile=new JButton("select hexfile");
		button_ouputWavFile=new JButton("wav filename");
		button_writeWav=new JButton("play wav file");
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
		//panel.add(button_ouputWavFile);
		panel.add(button_writeWav);
		frame.add(panel,BorderLayout.LINE_END);
		frame.add(scrollText,BorderLayout.CENTER);

		frame.setSize(640,480);
		frame.setVisible(true);
		
		setupData=new Model_ProgrammParameters(); 
		testText.append("selected hex-file:\n");
		testText.append(setupData.getInputHexFile() .getAbsolutePath());
		testText.append("\n\n");
		testText.append("output wav-file:\n");
		testText.append(setupData.getOutputWavFile().getAbsolutePath());
		testText.append("\n");
	}

	class inputHexFile_ButtonListener implements ActionListener{
		public boolean Knopf1=false;
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
    			testText.append("new hexfile name:\n");
        		testText.append(file.getAbsolutePath());
        		setupData.setInputHexFile(file);

        	};	
		}	
	}

	class writeWav_ButtonListener implements ActionListener{
		public boolean Knopf1=false;
		public void actionPerformed(ActionEvent e) {
			System.out.println(e.getActionCommand());
			testText.append("\nconverting hex to wav\n");
			try {
				WavCodeGenerator.convertHex2Wav(setupData.getInputHexFile() ,setupData.getOutputWavFile());
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

	public static void main(String[] args) {

		Main_WavBootLoader w=new Main_WavBootLoader();
		
		System.out.println("Ende");

	}

}
