package cn.edu.bjtu.cit.bss.align;
import java.io.*;
import java.util.*;
import org.apache.commons.math.complex.*;
import cn.edu.bjtu.cit.bss.*;
import cn.edu.bjtu.cit.bss.preprocess.*;
import pp.util.BLAS;

/**
 * <h1>Description</h1>
 * The implementation of: Radoslaw Mazur, Alfred Mertins, "A SPARSITY BASED CRITERION FOR 
 * SOLVING THE PERMUTATION AMBIGUITY IN CONVOLUTIVE BLIND SOURCE SEPARATION", ICASSP 2011.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Sep 16, 2011 9:04:16 AM, revision:
 */
public class SparsityAlign extends AlignPolicy
{
private static final long serialVersionUID=5092035016568207831L;
private List<Complex[][]> stftyl;//estimated stft data

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
	private static final long serialVersionUID=-2189529251730165871L;
	private DemixingModel demixm;//the demixing matrices
	private int offset;
	private int len;
	
		/**
		 * @param demixm
		 * demixing matrices
		 * @param offset
		 * subband offset
		 * @param len
		 * subband length
		 */
		public Subband(DemixingModel demixm,int offset,int len)
		{
			SparsityAlign.this.checkSubbandSize(offset,len);
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
		int[] bestp=null;//best permutation
		Complex[][] stfty;
		int numbins;
		Complex[][] stft;
		double[][] istftlow,istfthigh;
		double cor;
			
			numbins=SparsityAlign.this.fftSize();

			/*
			 * calculate istft for lower subband
			 */
			stft=new Complex[numbins][stftyl.get(0)[0].length];
			for(Complex[] cb:stft) Arrays.fill(cb,new Complex(0,0));
			istftlow=new double[stftyl.size()][];
			
			for(int sourcei=0;sourcei<stftyl.size();sourcei++)
			{
				stfty=stftyl.get(sourcei);
				
				//arrange frequency bins for lower subband
				for(int binidx=offset;binidx<offset+len;binidx++) 
				{
					stft[binidx]=stfty[binidx];
					
					if(binidx>0&&binidx<numbins/2) 
						stft[numbins-binidx]=stfty[numbins-binidx];
				}
				
				istftlow[sourcei]=SparsityAlign.this.getFDBSSAlgorithm().stfTransformer().istft(stft);
			}
			
			/*
			 * calculate istft for higher subband
			 */
			stft=new Complex[numbins][stftyl.get(0)[0].length];
			for(Complex[] cb:stft) Arrays.fill(cb,new Complex(0,0));
			istfthigh=new double[stftyl.size()][];
			
			for(int i=0;i<stftyl.size();i++)
			{
				stfty=stftyl.get(i);
				
				//arrange frequency bins for higher subband
				for(int binidx=another.offset;binidx<another.offset+another.len;binidx++) 
				{
					stft[binidx]=stfty[binidx];
				
					if(binidx>0&&binidx<numbins/2) 
						stft[numbins-binidx]=stfty[numbins-binidx];
				}
				
				istfthigh[i]=SparsityAlign.this.getFDBSSAlgorithm().stfTransformer().istft(stft);
			}
			
			cor=(lpPseudoNorm(istftlow[0],istfthigh[0],0.1)+lpPseudoNorm(istftlow[1],istfthigh[1],0.1))/
				(lpPseudoNorm(istftlow[0],istfthigh[1],0.1)+lpPseudoNorm(istftlow[1],istfthigh[0],0.1));
			
			/*
			 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			 * Here is not the same as paper! The lp pseudo norm is smaller the signal is more sparse, 
			 * so when the signals are correctly aligned, their lp pseudo should be small.
			 */
			if(cor<1) bestp=new int[]{0,1};
			else bestp=new int[]{1,0};
			
			
			System.out.println(Arrays.toString(bestp)+": "+cor);
			
			
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
			SparsityAlign.this.checkSubbandSize(offset,len+higher.len);

			/*
			 * rearrange the higher subband
			 */
			p=permutationForAnother(higher);
			SparsityAlign.this.rearrange(demixm,higher.offset,higher.len,p);
			len+=higher.len;
		}
		
		public String toString()
		{
			return "["+offset+", "+len+")";
		}
	}
	
	public SparsityAlign()
	{
//		if(this.numOutputChannels()>2) throw new RuntimeException("only 2 sources supported");
	}
	
	/**
	 * load all estimated stft data
	 * @param demixm
	 * demixing matrices
	 * @return
	 * @throws IOException
	 */
	private List<Complex[][]> loadEstimatedSTFT(DemixingModel demixm) throws IOException
	{
	FDBSSAlgorithm bss;
	Complex[][] bindata=null,estbindata=null,estc=null;
	List<Complex[][]> stftyl;
	
		/*
		 * prepare
		 */
		bss=this.getFDBSSAlgorithm();
		stftyl=new ArrayList<Complex[][]>(this.numSources());
		for(int sourcei=0;sourcei<this.numSources();sourcei++) 
			stftyl.add(new Complex[this.fftSize()][]);
		
		//get estimated stft
		for(int binidx=0;binidx<this.fftSize()/2+1;binidx++)
		{
			/*
			 * get estimated bin data
			 */
			bindata=bss.binData(binidx,bindata);
			Preprocessor.centering(bindata);
			estbindata=BLAS.multiply(demixm.getDemixingMatrix(binidx),bindata,null);
			
			//dispatch
			for(int sourcei=0;sourcei<stftyl.size();sourcei++) 
				stftyl.get(sourcei)[binidx]=estbindata[sourcei];
			
			//its complex conjugate part
			if(binidx>0&&binidx<this.fftSize()/2)
			{
				estc=BLAS.conjugate(estbindata,null);
				for(int sourcei=0;sourcei<stftyl.size();sourcei++) 
					stftyl.get(sourcei)[this.fftSize()-binidx]=estc[sourcei];
			}
		}

		return stftyl;
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
			
			
			
			
			try
			{
				stftyl=SparsityAlign.this.loadEstimatedSTFT(demixm);
			}
			catch(IOException e)
			{
				throw new RuntimeException("failed to load estimated stft data: "+e);
			}
			System.out.println("turn: "+turn);
			
			
			
			
			
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
	
	/**
	 * calculate the correlation of two vectors
	 * @param v1, v2
	 * two vectors
	 * @return
	 */
	public double correlation(double[] v1,double[] v2)
	{
	double e12=0;
	double var1=0,var2=0;
	
		for(int i=0;i<Math.min(v1.length,v2.length);i++)
		{
			e12+=v1[i]*v2[i];
			
			var1+=v1[i]*v1[i];
			var2+=v2[i]*v2[i];
		}
	
		return e12/(var1*var2);
	}
	
	/**
	 * calculate the correlation ratio of two vectors
	 * @param v1, v2
	 * two vectors
	 * @return
	 */
	public double correlationRatio(double[] v1,double[] v2)
	{
		return (correlation(v1,v1)+correlation(v2,v2))/(2*correlation(v1,v2));
	}
	
	/**
	 * calculate the lp pseudo norm of two vectors
	 * @param v1, v2
	 * two vectors
	 * @param p
	 * the norm power
	 * @return
	 */
	public double lpPseudoNorm(double[] v1,double[] v2,double p)
	{
	double sum=0;
	
		for(int i=0;i<Math.min(v1.length,v2.length);i++)
			sum+=Math.pow(Math.abs(v2[i]+v1[i]),p);

		return Math.pow(sum,1/p);
	}
	
	/**
	 * calculate the sparsity ratio of two vectors
	 * @param v1, v2
	 * two vectors
	 * @param p
	 * the norm power
	 * @return
	 */
	public double sparsityRatio(double[] v1,double[] v2,double p)
	{
		return (lpPseudoNorm(v1,v1,p)+lpPseudoNorm(v2,v2,p))/(2*lpPseudoNorm(v1,v2,p));
	}

	public static void main(String[] args) throws IOException
	{
	SparsityAlign app;
	DemixingModel demixm;
		
		app=new SparsityAlign();
		app.setFDBSSAlgorithm(new FDBSS(new File("temp")));
		demixm=((FDBSS)app.getFDBSSAlgorithm()).loadDemixingModel();

		app.align(demixm);
	}
}
