package cn.edu.bjtu.cit.bss.align;
import java.io.*;
import java.util.*;
import cn.edu.bjtu.cit.bss.*;

/**
 * <h1>Description</h1>
 * Use phase feature to calculate affinity.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Nov 29, 2011 4:55:22 PM, revision:
 */
public class PhaseAffinityMatrixBuilder extends AffinityMatrixBuilder
{
private static final long serialVersionUID=-6652124736315889988L;
private double eps=1e-10;//absolute value smaller than this threshold will be regarded as zero
private int neighborhood;//neighbor threshold in affinity matrix construction
private int refidxj;//reference sensor index
private CommonFeature feature;//used to calculate features

	/**
	 * @param fdbss
	 * bss algorithm reference
	 * @param neighborhood
	 * neighbor threshold in affinity matrix construction
	 * @param refidxj
	 * reference sensor index
	 */
	public PhaseAffinityMatrixBuilder(FDBSS fdbss,int neighborhood,int refidxj)
	{
		super(fdbss);
		this.neighborhood=neighborhood;
		this.refidxj=refidxj;
		feature=new CommonFeature(fdbss);
	}
	
	public AffinityMatrix buildAffinityMatrix(DemixingModel demixm,int offset,int len)
	{
	AlignPolicy policy;
	AffinityMatrix am;
	List<double[][]> fn;//feature for neighbors
	int loadbin;//frequency bin index for loaded data
	double[][] f1;//features for current frequency bin
	double[][] f2;//features for the other bin
	int binidx2;//frequency bin index for the other bin
	int idx1,idx2;//index in affinity matrix
	double sim;//the affinity
			
		policy=this.fdbssAlgorithm().alignPolicy();
		policy.checkDemixingModel(demixm);
		policy.checkSubbandSize(offset,len);
			
		am=new DenseAffinityMatrix(policy.numSources()*len);
		fn=new LinkedList<double[][]>();

		//initialize
		for(loadbin=offset;loadbin<offset+neighborhood&&loadbin<offset+len;loadbin++)
				fn.add(feature.mixPhase(demixm,loadbin,refidxj));
	
		for(int binidx1=offset;;binidx1++)
		{
			if(fn.isEmpty()) break;//finished
			//add a new bin to neighbors
			if(loadbin<offset+len) fn.add(feature.mixPhase(demixm,loadbin++,refidxj));
			
			/*
			 * calculate the affinity from current bin to its neighbors
			 */
			f1=fn.remove(0);//current bin
			for(int delta=0;delta<fn.size();delta++) 
			{
				f2=fn.get(delta);//the other frequency bin
				binidx2=binidx1+1+delta;//its frequency bin index
					
				for(int i=0;i<f1.length;i++) 
					for(int j=0;j<f2.length;j++) 
					{
						idx1=policy.b2i(i,binidx1,offset,len);
						idx2=policy.b2i(j,binidx2,offset,len);
						
						sim=similarity(f1[i],f2[j]);
						am.setAffinity(idx1,idx2,sim);
						am.setAffinity(idx2,idx1,sim);
					}
			}
		}
		return am;
	}
	
	/**
	 * calculate the similarity between two vectors
	 * @param v1
	 * a vector
	 * @param v2
	 * another vector
	 * @return
	 */
	private double similarity(double[] v1,double[] v2)
	{
	double cosval,sim=Double.MAX_VALUE;

		for(int i=0;i<Math.min(v1.length,v2.length);i++)
		{
			cosval=Math.cos(Math.abs(v1[i]-v2[i]));	
			if(cosval>1) cosval=1;else if(cosval<eps) cosval=0;
			
			if(cosval<sim) sim=cosval;
		}

		return sim;
	}
	
	public static void main(String[] args) throws IOException
	{
	int offset=0,len=300,neighborhood=5,refidxj=0;
	
	PhaseAffinityMatrixBuilder foo;
	DemixingModel demixm;
	AffinityMatrix am;
	
		foo=new PhaseAffinityMatrixBuilder(new FDBSS(new File("temp")),neighborhood,refidxj);
		demixm=foo.fdbssAlgorithm().loadDemixingModel();

		am=foo.buildAffinityMatrix(demixm,offset,len);
		am=(new SingleLinkagePreprocessor(foo.fdbssAlgorithm().numSources())).preprocess(am);
		am.visualize(false);
	}
}
