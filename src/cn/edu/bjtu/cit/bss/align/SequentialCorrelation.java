package cn.edu.bjtu.cit.bss.align;
import java.io.*;
import java.util.*;
import org.apache.commons.math.complex.*;
import cn.edu.bjtu.cit.bss.*;
import cn.edu.bjtu.cit.bss.preprocess.*;
import pp.util.BLAS;

/**
 * <h1>Description</h1>
 * Sequential align by signal envelop correlation.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Jun 8, 2011 2:31:25 PM, revision:
 */
public class SequentialCorrelation extends AlignPolicy
{
private static final long serialVersionUID=3300149995135146318L;

	/**
	 * <h1>Description</h1>
	 * Used to sort according to correlation coeffieients.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Jun 8, 2011 3:12:54 PM, revision:
	 */
	private static class PermutationEntry implements Serializable, Comparable<PermutationEntry>
	{
	private static final long serialVersionUID=-6168458900787206583L;
	int ch0,ch1;//channel index for previous bin and current bin
	double cor;//correlation coefficient

		/**
		 * @param ch0
		 * channel index for previous bin
		 * @param ch1
		 * channel index for current bin
		 * @param cor
		 * correlation coefficient
		 */
		public PermutationEntry(int ch0,int ch1,double cor)
		{
			this.ch0=ch0;
			this.ch1=ch1;
			this.cor=cor;
		}
	
		public int compareTo(PermutationEntry o)
		{
			//sort according to decreasing order
			if(cor>o.cor) return -1;
			else if(cor<o.cor) return 1;
			else return 0;
		}
		
		public String toString()
		{
			return "["+ch0+", "+ch1+"]: "+cor;
		}
	}

	public void align(DemixingModel demix)
	{
	FDBSSAlgorithm fdbss;
	Complex[][] sigs=null;//for bin data
	Complex[][] ests=null;//for estimated signals
	double[][] envelop0=null,envelop1=null;//envelop for previous bin and current bin
	double[][] temp;//used to swap two envelops
	PermutationEntry[] entries;
	int idx;
	
		this.checkDemixingModel(demix);
		
		fdbss=this.getFDBSSAlgorithm();
		entries=new PermutationEntry[demix.numSources()*demix.numSources()];

		/*
		 * envelop for the first bin
		 */
		sigs=fdbss.binData(0,sigs);
		Preprocessor.centering(sigs);
		//use two different space because the number of channels may change
		ests=BLAS.multiply(demix.getDemixingMatrix(0),sigs,ests);
		envelop0=BLAS.abs(ests,envelop0);
			
		//envelop for other bins
		for(int index=1;index<demix.fftSize()/2+1;index++)
		{
			/*
			 * envelop for current bin
			 */
			sigs=fdbss.binData(index,sigs);
			Preprocessor.centering(sigs);
			ests=BLAS.multiply(demix.getDemixingMatrix(index),sigs,ests);
			envelop1=BLAS.abs(ests,envelop1);
			
			/*
			 * sort according to correlation coeffieients decreasing order
			 */
			idx=0;
			for(int ch0=0;ch0<envelop0.length;ch0++) 
				for(int ch1=0;ch1<envelop1.length;ch1++) 
					entries[idx++]=new PermutationEntry(
							ch0,
							ch1,
							CommonFeature.correlationCoefficient(envelop0[ch0],envelop1[ch1]));
			Arrays.sort(entries);

			/*
			 * rearrange demixing matrix
			 */
			rearrange(demix.getDemixingMatrix(index),entries);//rearrange the demixing matrix for current bin
			rearrange(envelop1,entries);//current envelop also need to be rearranged
				
			/*
			 * swap buffer
			 */
			temp=envelop0;
			envelop0=envelop1;
			envelop1=temp;
		}
	}
	
	/**
	 * rearrange rows of the matrix according to the given order
	 * @param m
	 * a matrix
	 * @param swap
	 * new row orders
	 */
	private void rearrange(Complex[][] m,PermutationEntry... swap)
	{
	Complex[][] temp;
	
		temp=new Complex[m.length][];
		for(PermutationEntry s:swap)
		{
			//new position is taken or old row is used
			if(temp[s.ch0]!=null||m[s.ch1]==null) continue;
			
			temp[s.ch0]=m[s.ch1];
			m[s.ch1]=null;
		}
		
		//copy rows back
		System.arraycopy(temp,0,m,0,m.length);
	}
	
	/**
	 * rearrange rows of the matrix according to the given order
	 * @param m
	 * a matrix
	 * @param swap
	 * new row orders
	 */
	private void rearrange(double[][] m,PermutationEntry... swap)
	{
	double[][] temp;
	
		temp=new double[m.length][];
		for(PermutationEntry s:swap)
		{
			//new position is taken or old row is used
			if(temp[s.ch0]!=null||m[s.ch1]==null) continue;
			
			temp[s.ch0]=m[s.ch1];
			m[s.ch1]=null;
		}
		
		//copy rows back
		System.arraycopy(temp,0,m,0,m.length);
	}
}
