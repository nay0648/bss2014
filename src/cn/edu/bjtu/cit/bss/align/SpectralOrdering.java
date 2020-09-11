package cn.edu.bjtu.cit.bss.align;
import java.io.*;
import java.util.*;
import java.awt.image.*;
import org.apache.commons.math.linear.*;
import org.apache.commons.math.util.*;
import cn.edu.bjtu.cit.bss.*;
import pp.util.BLAS;

/**
 * <h1>Description</h1>
 * Align by spectral ordering: Chris Ding, Xiaofeng He, "Linearized Cluster Assignment 
 * via Spectral Ordering", ICML, 2004.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Oct 4, 2011 3:33:34 PM, revision:
 */
public class SpectralOrdering extends AlignPolicy
{
private static final long serialVersionUID=7952464139458320836L;
private int neighborhood=20;//neighbor bins in affinity matrix construction
private AffinityMatrixBuilder builder=null;//used to build affinity matrix
private int numband=10;//number of frequency bands

	public void align(DemixingModel demixm)
	{
	int total,deltalen,remains,len,poffset=0,plen=0;
	AffinityMatrix am=null;
	AffinityPreprocessor ampreprocessor;
	int[][] indicator;
		
		total=this.fftSize()/2+1;//total number of frequency bins
		deltalen=(int)Math.ceil(total/numband);//expected length of each alignment
		ampreprocessor=new SingleLinkagePreprocessor(this.numSources());
			
		for(int offset=0;offset<total;offset+=len)
		{				
			remains=total-(offset+deltalen);//number of frequency bins not algined
			if(remains<=deltalen/2) len=deltalen+remains;
			else len=deltalen;

			//build affinity matrix for a frequency band
			am=affinityMatrixBuilder().buildAffinityMatrix(demixm,offset,len);
			//apply single linkage preprocessor
			am=ampreprocessor.preprocess(am);
			//invocate spectral ordering
			indicator=this.spectralOrdering(am);
				

				
//			System.out.println("offset: "+offset);
//			System.out.println(BLAS.toString(this.toIndicator2D(indicator)));
				
				
				
			//align current frequency band
			this.align(demixm,offset,len,indicator);

			/*
			 * merge with the revious subband
			 */
			if(plen!=0) this.merge(demixm,poffset,plen,offset,len);
			poffset=offset;
			plen=len;
		}
	}
	
	/**
	 * get affinity matrix builder
	 * @return
	 */
	public AffinityMatrixBuilder affinityMatrixBuilder()
	{
		if(builder==null) builder=new DefaultAffinityMatrixBuilder((FDBSS)this.getFDBSSAlgorithm(),neighborhood);
		return builder;
	}
	
	/**
	 * perform spectral ordering on specified affinity matrix
	 * @param am
	 * an affinity matrix
	 * @return
	 * the 2D indicator
	 */
	public int[][] spectralOrdering(AffinityMatrix am)
	{
	double[] d;//the degree
	AffinityMatrix laplacian;
		
		//build the normalized Laplacian matrix
		{
		double temp;

			/*
			 * calculate the diagonal matrix D
			 */
			d=new double[am.size()];
			for(AffinityMatrix.Entry entry:am) d[entry.rowIndex()]+=entry.value();
				
			//calculate D^(-1/2)	
			for(int i=0;i<d.length;i++) d[i]=1.0/Math.sqrt(d[i]);

			/*
			 * calculate L=D^(-1/2)*S*D^(-1/2) into similarity matrix
			 */
			laplacian=new DenseAffinityMatrix(am.size());
			for(AffinityMatrix.Entry entry:am)
			{
				if(entry.rowIndex()>=entry.columnIndex()) continue;//symmetric, diagonal is 0
				temp=d[entry.rowIndex()]*entry.value()*d[entry.columnIndex()];
				laplacian.setAffinity(entry.rowIndex(),entry.columnIndex(),temp);
				laplacian.setAffinity(entry.columnIndex(),entry.rowIndex(),temp);
			}
			
			//make the normalized Laplacian matrix L positive semidefinite
			for(int i=0;i<am.size();i++) laplacian.setAffinity(i,i,1);
		}
		
		//get the order
		{
		EigenDecompositionImpl eigen;
		double[] q1;
		int[][] indicator;
		PEntry[] pentry;
		int idx;
		
			//calculate the eigen decomposition
			eigen=new EigenDecompositionImpl(laplacian.toCommonosMatrix(),MathUtils.SAFE_MIN);

			q1=eigen.getEigenvector(1).getData();
			BLAS.entryMultiply(d,q1,q1);

			indicator=new int[this.numSources()][am.size()/this.numSources()];
			pentry=new PEntry[indicator.length];
			for(int binidx=0;binidx<indicator[0].length;binidx++)
			{
				//get eigenvector entries for a frequency bin
				for(int chidx=0;chidx<indicator.length;chidx++) 
				{
					idx=chidx*indicator[0].length+binidx;
					pentry[chidx]=new PEntry(chidx,q1[idx]);
				}
				
				/*
				 * sort them to get the permutation
				 */
				Arrays.sort(pentry);
				for(int chidx=0;chidx<indicator.length;chidx++) indicator[chidx][binidx]=pentry[chidx].index;
			}

			return indicator;
		}
	}

	/**
	 * <h1>Description</h1>
	 * Used to sort eigenvector entries.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Oct 4, 2011 4:08:59 PM, revision:
	 */
	private static class PEntry implements Serializable, Comparable<PEntry>
	{
	private static final long serialVersionUID=-1722493253959998493L;
	int index;
	double value;
	
		public PEntry(int index,double value)
		{
			this.index=index;
			this.value=value;
		}

		public int compareTo(PEntry o)
		{
			if(value<o.value) return -1;
			else if(value>o.value) return 1;
			else return 0;
		}
		
		public String toString()
		{
			return index+": "+value;
		}
	}
	
	public static void main(String[] args) throws IOException
	{
	int offset=0,len=100;	
		
	SpectralOrdering foo;
	DemixingModel demixm;
	AffinityMatrixBuilder builder;
	AffinityMatrix am,amp;
	int[][] indicator;
	BufferedImage imgam,imgamp;
			
		foo=new SpectralOrdering();
		foo.setFDBSSAlgorithm(new FDBSS(new File("temp")));
		demixm=((FDBSS)foo.getFDBSSAlgorithm()).loadDemixingModelNotAligned();
		builder=foo.affinityMatrixBuilder();

		/*
		 * affinity matrix visualization before align
		 */
		am=builder.buildAffinityMatrix(demixm,offset,len);
		imgam=am.toImage(false);

		/*
		 * affinity matrix visualization after align
		 */
		indicator=foo.spectralOrdering(am);
		amp=builder.buildAffinityMatrix(demixm,offset,len,indicator);
		imgamp=amp.toImage(false);
			
		pp.util.Util.showImage(pp.util.Util.drawResult(1,2,5,imgam,imgamp));
	}
}
