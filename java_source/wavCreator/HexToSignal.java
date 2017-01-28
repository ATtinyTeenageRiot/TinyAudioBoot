
package wavCreator;

public class HexToSignal {

	private int lowNumberOfPulses=2;
	private int highNumberOfPulses=3;
	private int startSequencePulses=40;
	private int numStartBits=1;
	private int numStopBits=1;
	private boolean invertSignal=true; // correction of an inverted audio signal line
	private double manchesterPhase=1; // current phase for differential manchester coding

	
	private int manchesterNumberOfSamplesPerBit=4; // this value must be even
	/* flag=true: rising edge
	 * flag=false: falling edge
	 */
	private double[] manchesterEdge(boolean flag, int pointerIntoSignal,double signal[] )
	{
		double sigpart[]=new double[manchesterNumberOfSamplesPerBit];
		int n;
		double value;

		if(false) // manchester code
		{
			if(flag) value=1;
			else value=-1;
			if(invertSignal)value=value*-1;  // correction of an inverted audio signal line
			for(n=0;n<manchesterNumberOfSamplesPerBit;n++)
			{
				if(n<manchesterNumberOfSamplesPerBit/2)signal[pointerIntoSignal]=-value;
				else signal[pointerIntoSignal]=value;
				pointerIntoSignal++;
			}
		}
		else // differential manchester code ( inverted )
		{
			if(flag) manchesterPhase=-manchesterPhase; // toggle phase
			for(n=0;n<manchesterNumberOfSamplesPerBit;n++)
			{
				if(n==(manchesterNumberOfSamplesPerBit/2))manchesterPhase=-manchesterPhase; // toggle phase
				signal[pointerIntoSignal]=manchesterPhase;
				pointerIntoSignal++;
			}		
		}
		return sigpart;
	}

	public double[] manchesterCoding(int hexdata[])
	{
		int laenge=hexdata.length;
		double[] signal=new double[(1+startSequencePulses+laenge*8)*manchesterNumberOfSamplesPerBit];
		
		int counter=0;
		/** generate synchronisation start sequence **/
		for (int n=0; n<startSequencePulses; n++)
		{
			manchesterEdge(false,counter,signal); // 0 bits: generate falling edges 
			counter+=manchesterNumberOfSamplesPerBit;
		}
		
		/** start bit **/
		manchesterEdge(true,counter,signal); //  1 bit:  rising edge 
		counter+=manchesterNumberOfSamplesPerBit;
		
		/** create data signal **/
		int count=0;
		for(count=0;count<hexdata.length;count++)
		{
			int dat=hexdata[count];
			//System.out.println(dat);
			/** create one byte **/			
			for( int n=0;n<8;n++) // first bit to send: MSB
			{
				if((dat&0x80)==0) 	manchesterEdge(false,counter,signal); // generate falling edges ( 0 bits )
				else 				manchesterEdge(true,counter,signal); // rising edge ( 1 bit )
				counter+=manchesterNumberOfSamplesPerBit;	
				dat=dat<<1; // shift to next bit
			}
		}
		return signal;	
	}
	public double[] flankensignal(int hexdata[])
	{
		int intro=startSequencePulses*lowNumberOfPulses+numStartBits*highNumberOfPulses+numStopBits*lowNumberOfPulses;
		int laenge=hexdata.length;
		double[] signal=new double[intro+laenge*8*highNumberOfPulses];
	
		double sigState=-1;
		int counter=0;

		/** generate start sequence **/
		int numOfPulses=lowNumberOfPulses;
		for (int n=0; n<startSequencePulses; n++)
		{
			for(int k=0;k<numOfPulses;k++)
			{
				signal[counter++]=sigState;
			}
			sigState*=-1;
		}
		/** start: create 2 high-Bits **/
		numOfPulses=highNumberOfPulses;
		for (int n=0; n<numStartBits; n++)
		{
			for(int k=0;k<numOfPulses;k++)
			{
				signal[counter++]=sigState;
			}
			sigState*=-1;
		}
		/** create data signal **/
		int count=0;
		for(count=0;count<hexdata.length;count++)
		{
			int dat=hexdata[count];
			/** create one byte **/			
			for( int n=0;n<8;n++)
			{
				if((dat&0x80)==0)numOfPulses=lowNumberOfPulses;
				else numOfPulses=highNumberOfPulses;
				dat=dat<<1; // shift to next bit
				
				for(int k=0;k<numOfPulses;k++)
				{
					signal[counter++]=sigState;
				}
				sigState*=-1;
			}
		}
		/** stop: create 1 low-Bit **/
		numOfPulses=lowNumberOfPulses;
		for (int n=0; n<numStopBits; n++)
		{
			for(int k=0;k<numOfPulses;k++)
			{
				signal[counter++]=sigState;
			}
			sigState*=-1;
		}
		/** cut to long signal */
		double[] sig2=new double[counter];
		for(int n=0;n<sig2.length;n++) sig2[n]=signal[n];
		return sig2;
	}
}
