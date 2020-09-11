package cn.edu.bjtu.cit.bss.align;
import java.io.*;
import java.util.*;
import org.apache.commons.math.complex.*;
import pp.util.BLAS;
import cn.edu.bjtu.cit.bss.*;

/**
 * <h1>Description</h1>
 * Sequential align by signal envelop.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Jun 9, 2011 3:57:37 PM, revision:
 */
public class SequentialAlign extends AlignPolicy
{
private static final long serialVersionUID=-999139197203179375L;

	public SequentialAlign()
	{}
		
	/**
	 * align consecutive feature matrix
	 * @param f1
	 * the first feature matrix, each row is a feature
	 * @param f2
	 * the second feature matrix, each row is a feature
	 * @param permutation
	 * column permutations
	 * @return
	 * the best permutation
	 */
	private int[] align(double[][] f1,double[][] f2,List<int[]> permutation)
	{
	double temp,dist,mindist=Double.MAX_VALUE;
	int[] optp=null;//the optimum permutation
		
		BLAS.checkSize(f1,f2);
			
		//for all column permutations
		for(int[] colidx:permutation)
		{
			dist=0;
			for(int i=0;i<f1.length;i++)
				for(int j=0;j<f1[i].length;j++)
				{
					temp=f1[i][j]-f2[colidx[i]][j];
					dist+=temp*temp;
				}

			if(dist<mindist)
			{
				mindist=dist;
				optp=colidx;
			}
		}
		return optp;
	}
	
	/**
	 * rearrange demixing matrix rows according to the given order
	 * @param demix
	 * demixing matrix
	 * @param optp
	 * the optimum permutation
	 */
	private void rearrange(Complex[][] demix,int[] optp)
	{
	Complex[][] temp;
	
		temp=new Complex[demix.length][];
		for(int i=0;i<temp.length;i++) temp[i]=demix[optp[i]];
		System.arraycopy(temp,0,demix,0,demix.length);
	}
	
	/**
	 * used to rearrange the feature
	 * @param feature
	 * bin-wise feature
	 * @param optp
	 * the permutation
	 */
	private void rearrange(double[][] feature,int[] optp)
	{
	double[][] temp;
		
		temp=new double[feature.length][];
		for(int i=0;i<temp.length;i++) temp[i]=feature[optp[i]];
		System.arraycopy(temp,0,feature,0,feature.length);
	}
	
	public void align(DemixingModel demixm)
	{
	List<int[]> permutation;
	CommonFeature feature;
	int[] optp;
	double[][] envelop1=null,envelop2;

		//generate permutation according to number of sources
		permutation=indexPermutation(demixm.numSources());
		feature=new CommonFeature(this.getFDBSSAlgorithm());
		
		for(int binidx=0;binidx<demixm.fftSize()/2+1;binidx++)
		{
			try
			{
				envelop2=feature.envelop(demixm,binidx);
			}
			catch(IOException e)
			{
				throw new RuntimeException("failed to calculate signal envelop",e);
			}
	
			if(envelop1!=null)
			{
				optp=align(envelop1,envelop2,permutation);//get the optimum permutation

				//rearrange demixing matrices
				rearrange(demixm.getDemixingMatrix(binidx),optp);
				//bin-wise features are also need to be rearranged
				rearrange(envelop2,optp);
			}
			
			envelop1=envelop2;
		}
	}
}
