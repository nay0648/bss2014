package cn.edu.bjtu.cit.bss.align;
import java.io.*;
import java.util.*;
import java.awt.image.*;
import cn.edu.bjtu.cit.bss.*;
import cn.edu.bjtu.cit.bss.align.AffinityMatrix.Entry;
import pp.util.BLAS;

/**
 * <h1>Description</h1>
 * Constrained weighted kernel k-means. See: Inderjit S. Dhillon, et al. Kernel 
 * k-means, Spectral Clustering and Normalized Cuts, KDD, 2004, for more information.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Nov 21, 2011 8:21:11 AM, revision:
 */
public class CWKKMeans extends AlignPolicy
{
private static final long serialVersionUID=4527792858030480686L;
private int maxit=200;//max number of iterations
private int neighborhood=100;//neighborhood of affinity
private AffinityMatrixBuilder ambuilder;//used to construct affinity matrix
private int numband=10;//number of frequency bands
private List<int[]> idxp=null;//all index permutations
//private AlignPolicy initializer=new RegionGrow();//used to initialize cluster centers if not null
private AlignPolicy initializer=null;//used to initialize cluster centers if not null

	public void align(DemixingModel demixm)
	{
	int total,deltalen,remains,len,poffset=0,plen=0;
	AffinityMatrix am=null;
	AffinityPreprocessor ampreprocessor;
	int[][] indicator;
		
		//apply initializer if available
		if(initializer!=null) 
		{
			initializer.setFDBSSAlgorithm(this.getFDBSSAlgorithm());
			initializer.align(demixm);
		}
		
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
			//invocate constrained weighted kernel k-means
			indicator=this.cwkkmeans(am);
				

				
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
		if(ambuilder==null) 
			ambuilder=new DefaultAffinityMatrixBuilder((FDBSS)this.getFDBSSAlgorithm(),neighborhood);
//			ambuilder=new PhaseAffinityMatrixBuilder((FDBSS)this.getFDBSSAlgorithm(),neighborhood,0);
		
		return ambuilder;
	}
	
	/**
	 * constrained weighted kernel k-means for permutation problem
	 * @param am
	 * affinity matrix
	 * @return
	 * cluster indicator
	 */
	private int[][] cwkkmeans(AffinityMatrix am)
	{
	double[] weight;//weight of each sample
	AffinityMatrix kernel;//kernel matrix
	int[][] indicator,indicator2,temp;//cluster result
	List<List<Integer>> cl;//clusters
	double[][] costtable;//channel-cluster cost table
	double[] term3;//the third term in cost function
	double[] sumw;//sum of weights in each cluster
	boolean converge;//true if the algorithm is converged
	double[] distortion;//distrotion for each cluster

		/*
		 * calculate kernel matrix and weight
		 */
		kernel=buildKernel(am);

		weight=new double[kernel.size()];
		for(Entry entry:kernel) weight[entry.rowIndex()]+=entry.value();
	
		/*
		 * initial cluster indicator, use input permutation as initialization
		 */
		indicator=new int[this.numSources()][kernel.size()/this.numSources()];
		for(int chidx=0;chidx<indicator.length;chidx++) 
			for(int binidx=0;binidx<indicator[chidx].length;binidx++) 
				indicator[chidx][binidx]=chidx;

		indicator2=new int[indicator.length][indicator[0].length];
		
		/*
		 * for cluster assignment
		 */
		cl=new ArrayList<List<Integer>>(indicator.length);
		for(int chidx=0;chidx<indicator.length;chidx++) 
			cl.add(new ArrayList<Integer>(indicator[chidx].length));

		/*
		 * k-means iteration
		 */
		//store costs of assign elements in each frequency bin to each cluster
		costtable=new double[indicator.length][indicator.length];
		term3=new double[indicator.length];//the third term in the cost function for each cluster
		sumw=new double[indicator.length];//sum of weights of each cluster
		distortion=new double[indicator.length];//distortion
		
		for(int numit=0;numit<maxit;numit++)
		{
			/*
			 * elements belong to each cluster
			 */
			//clear results of last turn
			for(List<Integer> tempc:cl) tempc.clear();
			
			for(int chidx=0;chidx<indicator.length;chidx++) 
				for(int binidx=0;binidx<indicator[chidx].length;binidx++) 
					cl.get(indicator[chidx][binidx]).add(b2i(chidx,binidx,indicator[chidx].length));

			/*
			 * calculate the sum of weights in each cluster
			 */
			Arrays.fill(sumw,0);
			for(int clidx=0;clidx<cl.size();clidx++) 
				for(int eleidx:cl.get(clidx)) sumw[clidx]+=weight[eleidx];

			/*
			 * calculate the third term in the cost function, this term will not 
			 * change in a single iteration
			 */
			Arrays.fill(term3,0);
			for(int clidx=0;clidx<cl.size();clidx++) 
			{
			List<Integer> tempcl;
			int bidx,cidx;
			
				tempcl=cl.get(clidx);

				for(int ii=0;ii<tempcl.size();ii++) 
					//symmetric
					for(int jj=ii;jj<tempcl.size();jj++) 
					{
						bidx=tempcl.get(ii);
						cidx=tempcl.get(jj);
						
						//diagonal
						if(ii==jj) term3[clidx]+=weight[bidx]*weight[cidx]*kernel.getAffinity(bidx,cidx);
						else term3[clidx]+=2*weight[bidx]*weight[cidx]*kernel.getAffinity(bidx,cidx);
					}
				
				term3[clidx]/=sumw[clidx]*sumw[clidx];
			}

			/*
			 * assign elements to clusters
			 */
			{
			double term2;
			int aidx;
			int[] optassign;
				
				converge=true;
				Arrays.fill(distortion,0);//distortion changes in every iteration
			
				//traverse all elements
				for(int binidx=0;binidx<indicator[0].length;binidx++) 
				{
					//elements in the same frequency bin
					for(int chidx=0;chidx<indicator.length;chidx++) 
					{
						aidx=b2i(chidx,binidx,indicator[0].length);//index of current element

						//try to assign current element to all clusters
						for(int clidx=0;clidx<indicator.length;clidx++)
						{
							term2=0;

							for(int bidx:cl.get(clidx)) term2+=weight[bidx]*kernel.getAffinity(aidx,bidx);
							term2=2*term2/sumw[clidx];

							costtable[chidx][clidx]=kernel.getAffinity(aidx,aidx)-term2+term3[clidx];
						}
					}
				
					/*
					 * assign new clusters according to cost table
					 */
					optassign=optAssignment(costtable);//calculate optimum assignment
					for(int chidx=0;chidx<indicator2.length;chidx++) 
					{
						indicator2[chidx][binidx]=optassign[chidx];
						//assignment changed
						if(indicator2[chidx][binidx]!=indicator[chidx][binidx]) converge=false;
					}
					
					//accumulate distortion
					for(int chidx=0;chidx<distortion.length;chidx++) 
					{
						aidx=b2i(chidx,binidx,indicator[0].length);//index of current element
						distortion[optassign[chidx]]+=weight[aidx]*costtable[chidx][optassign[chidx]];
					}
				}
			}

//			System.out.println("distortion: "+BLAS.sum(distortion));

			//to see if algorithm converges
			if(converge) return indicator;
			else
			{
				temp=indicator;
				indicator=indicator2;
				indicator2=temp;
			}
		}
		
		throw new AlgorithmNotConvergeException("max number of iterations exceeded: "+maxit);		
	}
	
	/**
	 * build kernel from affinity matrix
	 * @param am
	 * affinity matrix
	 * @return
	 */
	private AffinityMatrix buildKernel(AffinityMatrix am)
	{
	AffinityMatrix kernel;
	
//		return (new ConnectivityMatrixBuilder(this.numOutputChannels())).preprocess(am);
		
		kernel=new DenseAffinityMatrix(am);
		//positive semidefinite
		for(int i=0;i<kernel.size();i++) kernel.setAffinity(i,i,1);
		return kernel;
	}
	
	/**
	 * convert 2D channel-frequency bin index to 1D affinity matrix index
	 * @param chidx
	 * channel index
	 * @param binidx
	 * frequency bin index
	 * @param len
	 * frequency band length
	 * @return
	 */
	private int b2i(int chidx,int binidx,int len)
	{
		return chidx*len+binidx;
	}
	
	/**
	 * find optimum assignment according to cost table
	 * @param costtable
	 * costtable[channel index][cluster index]=cost
	 * @return
	 * its elements are cluster labels
	 */
	private int[] optAssignment(double[][] costtable)
	{
	int[] optassign=null;
	double cost,mincost=Double.MAX_VALUE;
		
		if(idxp==null) idxp=AlignPolicy.indexPermutation(costtable.length);
		
		for(int[] p:idxp)
		{
			cost=0;
			for(int chidx=0;chidx<costtable.length;chidx++) cost+=costtable[chidx][p[chidx]];
			
			if(cost<mincost)
			{
				mincost=cost;
				optassign=p;
			}
		}

		return optassign;
	}
	
	public static void main(String[] args) throws IOException
	{
	int offset=0,len=200;
	
	CWKKMeans foo;
	AffinityMatrix am,am2;
	DemixingModel demixm;
	int[][] indicator;
	BufferedImage img,img2;
	
		foo=new CWKKMeans();
		foo.setFDBSSAlgorithm(new FDBSS(new File("temp")));
		demixm=((FDBSS)foo.getFDBSSAlgorithm()).loadDemixingModelNotAligned();
		
		am=foo.affinityMatrixBuilder().buildAffinityMatrix(demixm,offset,len);
		img=am.toImage(false);

		am=(new SingleLinkagePreprocessor(foo.numSources())).preprocess(am);
		indicator=foo.cwkkmeans(am);
		System.out.println(BLAS.toString(indicator));
		
		am2=foo.affinityMatrixBuilder().buildAffinityMatrix(demixm,offset,len,indicator);
		img2=am2.toImage(false);
	
		pp.util.Util.showImage(pp.util.Util.drawResult(1,2,5,img,img2));
	}
}
