package cn.edu.bjtu.cit.bss.align;
import java.io.*;
import java.util.*;
import org.apache.commons.math.complex.*;
import cn.edu.bjtu.cit.bss.*;
import pp.util.BLAS;

/**
 * <h1>Description</h1>
 *
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Nov 24, 2011 8:43:45 AM, revision:
 */
public class DemixAffinityMatrixBuilder extends AffinityMatrixBuilder
{
private static final long serialVersionUID=8254596557054174424L;
private double eps=1e-10;//absolute value smaller than this threshold will be regarded as zero
private int neighborhood;//neighbor bins in affinity matrix construction

	/**
	 * @param fdbss
	 * bss algorithm reference
	 * @param neighborhood
	 * neighborhood threshold
	 */
	public DemixAffinityMatrixBuilder(FDBSS fdbss,int neighborhood)
	{
		super(fdbss);
		this.neighborhood=neighborhood;
	}
	
	/**
	 * calculate the magnitude ratio of demixing matrix as feature
	 * @param demix
	 * demixing matrix for a frequency bin
	 * @param result
	 * space for result, null to allocate new space
	 * @return
	 * each row is a feature for a channel
	 */
	public double[][] demixRatio(Complex[][] demix,double[][] result)
	{
	double sum;
	
		if(result==null) result=new double[demix.length][demix[0].length];
		else BLAS.checkDestinationSize(result,demix.length,demix[0].length);
		
		for(int sourcei=0;sourcei<result.length;sourcei++) 
		{
			sum=0;
			for(int sensorj=0;sensorj<result.length;sensorj++) 
			{
				result[sourcei][sensorj]=demix[sourcei][sensorj].abs();
				sum+=result[sourcei][sensorj];
			}
			
			//calculate the ratio
			for(int sensorj=0;sensorj<result.length;sensorj++) result[sourcei][sensorj]/=sum;
		}
		
		return result;
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
	double abscos;
	
		abscos=Math.abs(BLAS.innerProduct(v1,v2)/(BLAS.norm2(v1)*BLAS.norm2(v2)));
		
		if(abscos>1) abscos=1;else if(abscos<eps) abscos=0;
		return abscos;
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
				fn.add(demixRatio(demixm.getDemixingMatrix(loadbin),null));

		for(int binidx1=offset;;binidx1++)
		{
			if(fn.isEmpty()) break;//finished
			//add a new bin to neighbors
			if(loadbin<offset+len) fn.add(demixRatio(demixm.getDemixingMatrix(loadbin++),null));

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
	int offset=0,len=100,neighborhood=3;
	
	DemixAffinityMatrixBuilder foo;
	DemixingModel demixm;
	AffinityMatrix am;
	
		foo=new DemixAffinityMatrixBuilder(new FDBSS(new File("temp")),neighborhood);
		demixm=foo.fdbssAlgorithm().loadDemixingModel();

		am=foo.buildAffinityMatrix(demixm,offset,len);
		am=(new SingleLinkagePreprocessor(foo.fdbssAlgorithm().numSources())).preprocess(am);
		am.visualize(false);
	}
}
