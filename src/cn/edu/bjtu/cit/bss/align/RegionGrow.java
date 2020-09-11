package cn.edu.bjtu.cit.bss.align;
import java.io.*;
import java.util.*;
import java.awt.Point;
import cn.edu.bjtu.cit.bss.*;
import pp.util.BLAS;

/**
 * <h1>Description</h1>
 * The implementation of: Lin Wang, Heping Ding, Fuliang Yin, "A Region-Growing Permutation 
 * Alignment Approach in Frequency-Domain Blind Source Separation of Speech Mixtures", IEEE 
 * Trans. Audio, Speech, and Language Processing, vol. 19, no. 3, pp. 549-557, 2011.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Sep 16, 2011 8:53:10 PM, revision:
 */
public class RegionGrow extends AlignPolicy
{
private static final long serialVersionUID=5566700800583583690L;
private double fs=8000;//sampling rate of the signal

	/**
	 * <h1>Description</h1>
	 * Represents a frequency bin.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Sep 16, 2011 9:07:20 AM, revision:
	 */
	private class FrequencyBin implements Serializable
	{
	private static final long serialVersionUID=351112840947696096L;
	private DemixingModel model;//the demixing model
	private int binidx;//subband offset
	private double[][] features;//feature for the frequency bin
	private double bestcor=0;//the best correlation between this bin and previous bin
	
		/**
		 * @param model
		 * demixing model
		 * @param binidx
		 * frequency bin index
		 * @param features
		 * features in this frequency bin
		 */
		public FrequencyBin(DemixingModel model,int binidx,double[][] features)
		{
			checkSubbandSize(binidx,1);
			this.model=model;
			this.binidx=binidx;
			this.features=features;
		}

		/**
		 * rearrange this frequency bin according to the specified permutation
		 * @param p
		 * a permutation
		 */
		public void rearrange(int[] p)
		{
		double[][] temp;
		
			RegionGrow.this.rearrange(model,binidx,1,p);
			
			temp=new double[features.length][];
			for(int sourcei=0;sourcei<features.length;sourcei++) 
				temp[sourcei]=features[p[sourcei]];
			
			for(int sourcei=0;sourcei<features.length;sourcei++) 
				features[sourcei]=temp[sourcei];
		}
	}
	
	/**
	 * <h1>Description</h1>
	 * Represents a region.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Sep 19, 2011 3:17:25 PM, revision:
	 */
	private class Region implements Serializable
	{
	private static final long serialVersionUID=4529150036919744986L;
	private List<FrequencyBin> bins=new LinkedList<FrequencyBin>();//frequency bins belong to this region
	private double[][] centersum;//centers sum
	
		/**
		 * get region size
		 * @return
		 */
		public int size()
		{
			return bins.size();
		}
		
		/**
		 * get the frequency band range of this region
		 * @return
		 */
		public Point bandRange()
		{
			if(bins.isEmpty()) return null;
			else return new Point(bins.get(0).binidx,bins.get(bins.size()-1).binidx);
		}
	
		/**
		 * add a frequency bin into this region
		 * @param bin
		 */
		public void addFrequencyBin(FrequencyBin bin)
		{
		FrequencyBin b0,b1;
		
			if(bins.isEmpty()) 
			{	
				bins.add(bin);
				
				centersum=BLAS.copy(bin.features,null);
			}
			else
			{
				b0=bins.get(0);
				b1=bins.get(bins.size()-1);
				
				if(bin.binidx==b0.binidx-1) bins.add(0,bin);
				else if(bin.binidx==b1.binidx+1) bins.add(bin);
				else throw new IllegalArgumentException(
						"only consecutive bins are allowed: "+bin.binidx+", ["+b0.binidx+", "+b1.binidx+"]");
				
				BLAS.add(centersum,bin.features,centersum);
			}
		}
		
		/**
		 * get the centers of this region
		 * @return
		 */
		public double[][] centers()
		{
			if(bins.isEmpty()) return null;
			else return BLAS.scalarMultiply(1.0/bins.size(),centersum,null);
		}
		
		/**
		 * merge another region with this one
		 * @param another
		 */
		public void merge(Region another)
		{
		Point range1,range2;
		int[] bestp;
		double[][] c1,c2;
		FrequencyBin bin;
		
			range1=bandRange();
			range2=another.bandRange();
			bestp=new int[RegionGrow.this.numSources()];
			
			//the other region is its higher consecutive region
			if(range1.y+1==range2.x)
			{
				c1=centers();
				c2=another.centers();
				
				RegionGrow.this.bestPermutation(c1,c2,bestp);
				
				for(Iterator<FrequencyBin> it=another.bins.iterator();it.hasNext();)
				{
					bin=it.next();
					bin.rearrange(bestp);
					addFrequencyBin(bin);
				}
			}
			//the other region is its lower consecutive region
			else if(range1.x-1==range2.y)
			{
			ListIterator<FrequencyBin> it;
			
				c1=centers();
				c2=another.centers();
				
				RegionGrow.this.bestPermutation(c1,c2,bestp);
				
				it=((LinkedList<FrequencyBin>)another.bins).listIterator(another.bins.size());
				for(;it.hasPrevious();)
				{
					bin=it.previous();
					bin.rearrange(bestp);
					addFrequencyBin(bin);
				}				
			}
			else throw new IllegalArgumentException(
					"only consecutive regions are allowed to merge: "+range1+", "+range2);
		}
	}
	
	public RegionGrow()
	{}

	/**
	 * @param fdbss
	 * the algorithm reference
	 * @param fs
	 * sampling rate of the signals
	 */
	public RegionGrow(FDBSSAlgorithm fdbss,double fs)
	{
		this.setFDBSSAlgorithm(fdbss);
		this.fs=fs;
	}
	
	/**
	 * calculate the best permutation for feature 2 to align to feature 1 with 
	 * max correlation coefficients
	 * @param f1
	 * feature 1
	 * @param f2
	 * feature 2
	 * @param bestp
	 * space for the best permutation
	 * @return
	 * average correlation coefficient for the best permutation
	 */
	private double bestPermutation(double[][] f1,double[][] f2,int[] bestp)
	{
	double cor,maxcor=-10;
		
		if(bestp.length!=this.numSources()) throw new IllegalArgumentException(
				"permutation size not match: "+bestp.length+", "+this.numSources());
		
		//try all permutations
		for(int[] p:AlignPolicy.indexPermutation(this.numSources()))
		{
			cor=0;
				
			for(int chidx=0;chidx<p.length;chidx++) 
				cor+=CommonFeature.correlationCoefficient(f1[chidx],f2[p[chidx]]);
				
			if(cor>maxcor) 
			{
				maxcor=cor;
				System.arraycopy(p,0,bestp,0,p.length);
			}
		}

		return maxcor/bestp.length;
	}
	
	public void align(DemixingModel model)
	{
	CommonFeature feature;
	List<FrequencyBin> bins;
	double uth;
	Region rlow,rhigh;
	
		this.checkDemixingModel(model);
		feature=new CommonFeature(this.getFDBSSAlgorithm());
		
		/*
		 * step 1:
		 * initialize
		 */
		{
			bins=new ArrayList<FrequencyBin>(this.fftSize()/2+1);
			try
			{
				for(int binidx=0;binidx<this.fftSize()/2+1;binidx++) 
					bins.add(new FrequencyBin(model,binidx,feature.powerRatio(model,binidx)));
			}
			catch(IOException e)
			{
				throw new RuntimeException("failed to calculate power ratio",e);
			}
		}
		
		/*
		 * step 2:
		 * one way sequential rearrange
		 */
		{
		List<Double> bestcorl;//best correlations
		FrequencyBin b0,b1;
		int[] bestp;
		
			bestcorl=new ArrayList<Double>(bins.size());
			bestp=new int[this.numSources()];
			
			for(int binidx=1;binidx<bins.size();binidx++)
			{			
				b0=bins.get(binidx-1);//its previous bin
				b1=bins.get(binidx);//current bin
				
				b1.bestcor=bestPermutation(b0.features,b1.features,bestp);
				bestcorl.add(b1.bestcor);
				
				b1.rearrange(bestp);
			}
			
			/*
			 * the threshold for region
			 */
			Collections.sort(bestcorl);
			uth=Math.min(0.7,bestcorl.get((int)Math.round(0.2*bestcorl.size())));
		}
		
		/*
		 * step 3:
		 * divide the full frequency band into low and high bands
		 */
		{
		int lowhighth;
		List<FrequencyBin> lowband,highband;
		
			lowhighth=(int)(600/(fs/this.fftSize()));
			
			lowband=new ArrayList<FrequencyBin>(lowhighth+1);
			highband=new ArrayList<FrequencyBin>(bins.size()-lowhighth-1);

			for(int binidx=0;binidx<bins.size();binidx++) 
				if(binidx<=lowhighth) lowband.add(bins.get(binidx));
				else highband.add(bins.get(binidx));
			
			rlow=regionGrow(lowband,uth);
			rhigh=regionGrow(highband,uth);
		}
		
		/*
		 * step 6:
		 * merge the lower band and higher band together
		 */
		rlow.merge(rhigh);
	}
	
	/**
	 * perform region grow
	 * @param band
	 * frequency bins
	 * @param uth
	 * region threshold
	 * @return
	 */
	private Region regionGrow(List<FrequencyBin> band,double uth)
	{
	List<Region> regions;

		/*
		 * step 4:
		 * generate regions
		 */
		{
		Region region;
		FrequencyBin bin;
		
			regions=new LinkedList<Region>();
			
			region=new Region();
			region.addFrequencyBin(band.get(0));
			
			for(int binidx=1;binidx<band.size();binidx++) 
			{
				bin=band.get(binidx);

				if(bin.bestcor>uth) region.addFrequencyBin(bin);//add to the same region
				//generate a new region
				else
				{
					regions.add(region);
					
					region=new Region();
					region.addFrequencyBin(bin);
				}
			}
			
			regions.add(region);//the last region
		}

		/*
		 * step 5:
		 * region grow from the biggest one
		 */
		{
		Region seed=null;
		int idx=0,maxvol=0,seedidx=0;
		ListIterator<Region> it;
		
			//find the biggest region
			for(Region r:regions) 
			{
				if(r.size()>maxvol)
				{
					maxvol=r.size();
					seedidx=idx;
					seed=r;
				}
				
				idx++;
			}

			/*
			 * merge its higher bands
			 */
			it=regions.listIterator(seedidx+1);
			for(;it.hasNext();) seed.merge(it.next());
			
			/*
			 * merge its lower bands
			 */
			it=regions.listIterator(seedidx);
			for(;it.hasPrevious();) seed.merge(it.previous());

			return seed;
		}
	}
}
