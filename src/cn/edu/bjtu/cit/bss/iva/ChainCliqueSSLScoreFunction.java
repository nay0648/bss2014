package cn.edu.bjtu.cit.bss.iva;
import java.io.*;
import java.util.*;
import org.apache.commons.math.complex.*;
import pp.util.BLAS;

/**
 * <h1>Description</h1>
 * Implementation of: Gil-Jin Jang, Intae Lee, and Te-Won Lee, INDEPENDENT 
 * VECTOR ANALYSIS USING NON-SPHERICAL JOINT DENSITIES FOR THE SEPARATION OF 
 * SPEECH SIGNALS, ICASSP, 2007.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Aug 13, 2012 6:55:30 PM, revision:
 */
public class ChainCliqueSSLScoreFunction implements ScoreFunction
{
private static final long serialVersionUID=-6509872934459211301L;

	/**
	 * <h1>Description</h1>
	 * Partial sqrt of a clique.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Aug 13, 2012 6:58:38 PM, revision:
	 */
	private class PartialSqrt implements Serializable
	{
	private static final long serialVersionUID=1906804723119247125L;
	int offset;//clique offset
	int len;//clique length
	double[][] sqrt;//partial sqrt
	
		/**
		 * @param offset
		 * subband offset
		 * @param len
		 * subband length
		 */
		public PartialSqrt(int offset,int len)
		{
			this.offset=offset;
			this.len=len;
			
			sqrt=new double[ydata[0].length][ydata[0][0].length];
			for(int n=0;n<ydata[0].length;n++) 
				for(int tau=0;tau<ydata[0][0].length;tau++) 
				{
					for(int f=offset;f<offset+len;f++) 
						sqrt[n][tau]+=BLAS.absSquare(ydata[f][n][tau]);
					
					sqrt[n][tau]=Math.sqrt(sqrt[n][tau]);
				}
		}
	}

private Complex[][][] ydata;//estimated source data [binidx][source index][stft frame index]
private List<PartialSqrt> cliquelist=new LinkedList<PartialSqrt>();
private int cliquesize=100;//clique size
private int cliqueshift=cliquesize*1/2;//clique shift

	public void setYData(Complex[][][] ydata)
	{
	int len;
	boolean cliqueend=false;
	
		this.ydata=ydata;
		
		/*
		 * build cliques
		 */
		cliquelist.clear();//clear old cliques
		for(int offset=0;!cliqueend;)
		{
			if(offset+cliquesize<=ydata.length) len=cliquesize;
			//the last clique
			else 
			{	
				len=ydata.length-offset;
				cliqueend=true;
			}
			
			cliquelist.add(new PartialSqrt(offset,len));
			
			offset+=cliqueshift;
		}
	}

	public Complex phi(int binidx,int sourcei,int tau)
	{
	Complex val=Complex.ZERO;
	
		for(PartialSqrt clique:cliquelist) 
			//find cliques this frequency belongs to
			if(binidx>=clique.offset&&binidx<clique.offset+clique.len) 
				val=val.add(ydata[binidx][sourcei][tau].multiply(1.0/clique.sqrt[sourcei][tau]));
		
		return val;
	}
}
