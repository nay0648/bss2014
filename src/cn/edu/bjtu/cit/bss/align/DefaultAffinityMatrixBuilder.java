package cn.edu.bjtu.cit.bss.align;
import java.io.*;
import java.util.*;
import cn.edu.bjtu.cit.bss.*;

/**
 * <h1>Description</h1>
 * Default procedure to constructure affinity matrix.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Nov 17, 2011 7:44:33 PM, revision:
 */
public class DefaultAffinityMatrixBuilder extends AffinityMatrixBuilder
{
private static final long serialVersionUID=8888350320942579130L;
private double eps=1e-10;//absolute value smaller than this threshold will be regarded as zero
private int neighborhood;//neighbor bins in affinity matrix construction

	/**
	 * @param fdbss
	 * bss algorithm reference
	 * @param neighborhood
	 * neighborhood threshold
	 */
	public DefaultAffinityMatrixBuilder(FDBSS fdbss,int neighborhood)
	{
		super(fdbss);
		this.neighborhood=neighborhood;
	}
		
	/**
	 * Calculate the similarity (affinity) of two sequences. The correlation 
	 * coefficient is used as the similarity because feature dimension is high.
	 * @param v1
	 * a vector
	 * @param v2
	 * another vector
	 * @return
	 */
	public double similarity(double[] v1,double[] v2)
	{
	int len;
	double r12=0,mu1=0,mu2=0,sigma1=0,sigma2=0;
	double cor,denominator;

		len=Math.min(v1.length,v2.length);
		
		for(int i=0;i<len;i++)
		{
			r12+=v1[i]*v2[i];
			
			mu1+=v1[i];
			mu2+=v2[i];
			
			sigma1+=v1[i]*v1[i];
			sigma2+=v2[i]*v2[i];
		}
		
		r12/=len;
		
		mu1/=len;
		mu2/=len;
		
		sigma1/=len;
		sigma2/=len;
		sigma1=Math.sqrt(sigma1-mu1*mu1);
		sigma2=Math.sqrt(sigma2-mu2*mu2);

		denominator=sigma1*sigma2;
		if(Math.abs(denominator)<eps) cor=1-eps;
		else cor=(r12-mu1*mu2)/denominator;
		
		if(cor<0) return 0;
		else if(cor>1) return 1;
		else return cor;
	}

	public AffinityMatrix buildAffinityMatrix(DemixingModel demixm,int offset,int len)
	{
	AlignPolicy policy;
	CommonFeature feature;
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
		
		feature=new CommonFeature(this.fdbssAlgorithm());
		am=new DenseAffinityMatrix(policy.numSources()*len);
		fn=new LinkedList<double[][]>();

		//initialize
		for(loadbin=offset;loadbin<offset+neighborhood&&loadbin<offset+len;loadbin++)
			try
			{
				fn.add(feature.powerRatio(demixm,loadbin));
			}
			catch(IOException e)
			{
				throw new RuntimeException("failed to calculate power ratio",e);
			}

		for(int binidx1=offset;;binidx1++)
		{
			if(fn.isEmpty()) break;//finished
			if(loadbin<offset+len) 
				try
				{
					//add a new bin to neighbors
					fn.add(feature.powerRatio(demixm,loadbin++));
				}
				catch(IOException e)
				{
					throw new RuntimeException("failed to calculate power ratio",e);
				}

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
	
	public static void main(String[] args) throws IOException
	{
	DefaultAffinityMatrixBuilder app;
	DemixingModel demixm;
	AffinityMatrix am;
	
		app=new DefaultAffinityMatrixBuilder(new FDBSS(new File("temp")),20);
		demixm=app.fdbssAlgorithm().loadDemixingModel();

		am=app.buildAffinityMatrix(demixm,0,150);
		am.visualize(false);
	}
}
