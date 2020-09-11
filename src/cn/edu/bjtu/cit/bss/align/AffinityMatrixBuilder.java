package cn.edu.bjtu.cit.bss.align;
import java.io.*;
import cn.edu.bjtu.cit.bss.*;
import pp.util.BLAS;

/**
 * <h1>Description</h1>
 * Build affinity matrix from different kinds of features and different 
 * structure of affinity matrix.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Nov 18, 2011 3:32:11 AM, revision:
 */
public abstract class AffinityMatrixBuilder implements Serializable
{
private static final long serialVersionUID=7304387386488236468L;
private FDBSS fdbss;//bss algorithm reference

	/**
	 * @param fdbss
	 * bss algorithm reference
	 */
	public AffinityMatrixBuilder(FDBSS fdbss)
	{
		this.fdbss=fdbss;
	}
	
	/**
	 * get bss algorithm reference
	 * @return
	 */
	public FDBSS fdbssAlgorithm()
	{
		return fdbss;
	}
	
	/**
	 * construct affinity matrix for estimated data in a specified frequency subbands
	 * @param demixm
	 * demixing model
	 * @param offset
	 * frequency subbands offset
	 * @param len
	 * frequency subbands length
	 * @return
	 */
	public abstract AffinityMatrix buildAffinityMatrix(DemixingModel demixm,int offset,int len);
	
	/**
	 * build permutation matrix according to clustering indicator
	 * @param indicator
	 * @return
	 */
	public double[][] buildPermutationMatrix(int[][] indicator)
	{
	double[][] p;
	int amidx1,amidx2;
	
		p=new double[indicator.length*indicator[0].length][indicator.length*indicator[0].length];
		
		for(int binidx=0;binidx<indicator[0].length;binidx++)
			for(int chidx=0;chidx<indicator.length;chidx++)
			{
				amidx1=chidx*indicator[0].length+binidx;//sample index
				amidx2=indicator[chidx][binidx]*indicator[0].length+binidx;//cluster index
				
				/*
				 * inverse permutation is needed!!!
				 */
				p[amidx2][amidx1]=1;
			}

		return p;
	}
	
	/**
	 * build affinity matrix according to clustering results, used for experiments
	 * @param demixm
	 * demixing model
	 * @param offset
	 * frequency band offset
	 * @param len
	 * frequency band length
	 * @param indicator
	 * clustering results
	 * @return
	 */
	public AffinityMatrix buildAffinityMatrixByPermutation(DemixingModel demixm,int offset,int len,int[][] indicator)
	{
	AlignPolicy policy;
	double[][] amd,p;
	
		policy=fdbss.alignPolicy();
		policy.checkSubbandSize(offset,len);
		if(indicator.length!=policy.numSources()||indicator[0].length!=len) 
			throw new IllegalArgumentException(
					"indicator size not match: "+indicator.length+" x "+indicator[0].length+
					", required: "+policy.numSources()+" x "+len);

		amd=buildAffinityMatrix(demixm,offset,len).toMatrix();
		
		/*
		 * P*W*P'
		 */
		p=buildPermutationMatrix(indicator);
		BLAS.multiply(p,amd,amd);
		BLAS.multiply(amd,BLAS.transpose(p,null),amd);
		
		return new DenseAffinityMatrix(amd);
	}
	
	/**
	 * build affinity matrix according to clustering results, used for experiments
	 * @param demixm
	 * demixing model
	 * @param offset
	 * frequency band offset
	 * @param len
	 * frequency band length
	 * @param indicator
	 * clustering results
	 * @return
	 */
	public AffinityMatrix buildAffinityMatrix(DemixingModel demixm,int offset,int len,int[][] indicator)
	{
	AlignPolicy policy;
	DemixingModel demix2;
	
		policy=fdbss.alignPolicy();
		policy.checkSubbandSize(offset,len);
		if(indicator.length!=policy.numSources()||indicator[0].length!=len) 
			throw new IllegalArgumentException(
					"indicator size not match: "+indicator.length+" x "+indicator[0].length+
					", required: "+policy.numSources()+" x "+len);

		demix2=new DemixingModel(demixm);//make a copy
		
		/*
		 * build affinity matrix according to aligned demixing matrices
		 */
		policy.align(demix2,offset,len,indicator);
		return buildAffinityMatrix(demix2,offset,len);
	}
}
