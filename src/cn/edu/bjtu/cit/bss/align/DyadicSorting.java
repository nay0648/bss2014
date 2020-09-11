package cn.edu.bjtu.cit.bss.align;
import java.io.*;
import java.util.*;
import cn.edu.bjtu.cit.bss.*;

/**
 * <h1>Description</h1>
 * Binary agglomeration by power ratio correlation. See: Kamran Rahbar, James P. Reilly, 
 * "A Frequency Domain Method for Blind Source Separation of Convolutive Audio Mixtures", 
 * IEEE Trans. Speech and Audio Processing, vol. 13, no. 5, 2005.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Sep 16, 2011 9:04:16 AM, revision:
 */
public class DyadicSorting extends AlignPolicy
{
private static final long serialVersionUID=5092035016568207831L;

	/**
	 * <h1>Description</h1>
	 * Represents a subband.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Sep 16, 2011 9:07:20 AM, revision:
	 */
	private class Subband implements Serializable
	{
	private static final long serialVersionUID=-7529558823313084225L;
	private DemixingModel demixm;//the demixing model
	private int offset;
	private int len;
	
		/**
		 * @param demixm
		 * demixing model
		 * @param offset
		 * subband offset
		 * @param len
		 * subband length
		 */
		public Subband(DemixingModel demixm,int offset,int len)
		{
			DyadicSorting.this.checkSubbandSize(offset,len);
			this.demixm=demixm;
			this.offset=offset;
			this.len=len;
		}
	
		/**
		 * find best permutation for another subband meets this one
		 * @param another
		 * another subband
		 * @return
		 */
		public int[] permutationForAnother(Subband another)
		{
		CommonFeature feature;
		double[][] powr1,powr2;
		int[] bestp=null;
		double cor,maxcor=Double.MIN_VALUE;
		
			try
			{
				feature=new CommonFeature(DyadicSorting.this.getFDBSSAlgorithm());				
				powr1=feature.powerRatio(demixm,offset+len-1);
				powr2=feature.powerRatio(demixm,another.offset);
			}
			catch(IOException e)
			{
				throw new RuntimeException("failed to calculate power ratio",e);
			}
			
			for(int[] p:AlignPolicy.indexPermutation(DyadicSorting.this.numSources()))
			{
				cor=0;
				
				for(int chidx=0;chidx<p.length;chidx++) 
					cor+=CommonFeature.correlationCoefficient(powr1[chidx],powr2[p[chidx]]);
				
				if(cor>maxcor) 
				{
					maxcor=cor;
					bestp=p;
				}
			}

			return bestp;
		}
	
		/**
		 * merge a higher consecutive subband into this one
		 * @param higher
		 */
		public void merge(Subband higher)
		{
		int[] p;
		
			/*
			 * check subbands size
			 */
			if(offset+len!=higher.offset) throw new IllegalArgumentException(
					"they are not consecutive subbands: "+offset+", "+len+", "+higher.offset+", "+higher.len);
			DyadicSorting.this.checkSubbandSize(offset,len+higher.len);

			/*
			 * rearrange the higher subband
			 */
			p=permutationForAnother(higher);
			DyadicSorting.this.rearrange(demixm,higher.offset,higher.len,p);
			len+=higher.len;
		}
		
		public String toString()
		{
			return "["+offset+", "+len+")";
		}
	}

	public void align(DemixingModel demixm)
	{
	List<Subband> layer1,layer2;
	Subband sub1,sub2;

		layer1=new LinkedList<Subband>();
		layer2=new LinkedList<Subband>();
	
		//prepare
		for(int binidx=0;binidx<this.fftSize()/2+1;binidx++)
			layer1.add(new Subband(demixm,binidx,1));

		//merge
		for(int turn=0;;turn++)
		{
			if(layer1.size()==1) break;//finished

			//merge a layer
			for(;!layer1.isEmpty();)
			{
				sub1=null;
				sub2=null;

				sub1=layer1.remove(0);//the lower subband

				//no higher subband exists
				if(layer1.isEmpty()) 
				{
					layer2.add(sub1);
					break;
				}
			
				sub2=layer1.remove(0);//the higher subband

				/*
				 * merge
				 */
				sub1.merge(sub2);
				layer2.add(sub1);
			}

			/*
			 * reset layer lists
			 */
			layer1.clear();
			layer1.addAll(layer2);
			layer2.clear();
		}
	}
}
