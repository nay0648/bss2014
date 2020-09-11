package cn.edu.bjtu.cit.bss.iva;
import java.io.*;
import java.util.*;
import java.util.logging.*;
import javax.sound.sampled.*;
import org.apache.commons.math.complex.*;
import pp.util.BLAS;
import cn.edu.bjtu.cit.bss.*;
import cn.edu.bjtu.cit.bss.FDBSS.Operation;
import cn.edu.bjtu.cit.bss.FDBSS.Parameter;
import cn.edu.bjtu.cit.bss.preprocess.*;
import cn.edu.bjtu.cit.bss.signalio.*;
import cn.edu.bjtu.cit.bss.util.*;

/**
 * <h1>Description</h1>
 * Fast fixed-point IVA algorithm. Please see: Intae Lee, Taesu Kim, Te-Won Lee, 
 * Fast fixed-point independent vector analysis algorithms for convolutive blind 
 * source separation, Signal Processing, vol. 87, no. 8, pp. 1859-1871, 2007. 
 * Matlab code is available: http://inc.ucsd.edu/~taesu/code/fivabss.zip
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Jun 29, 2012 10:21:24 AM, revision:
 */
public class FastIVA extends IVA
{
private static final long serialVersionUID=6077091303714944009L;
private static final String LOGGER_NAME="cn.edu.bjtu.cit.bss";
	
/*
 * supported nonlinearity calculation methods
 */
private static final Map<String,NonlinearityTable> 
	NONLINEARITY_TABLE_MAP=new HashMap<String,NonlinearityTable>();

	static
	{
		//G1 nonlinearity used in instantaneous CFastICA
		NONLINEARITY_TABLE_MAP.put("instg1",new InstG1Nonlinearity());
		//G2 nonlinearity used in instantaneous CFastICA
		NONLINEARITY_TABLE_MAP.put("instg2",new InstG2Nonlinearity());
		
		//spherically symmetric Laplacian
		NONLINEARITY_TABLE_MAP.put("ssl",new SSLNonlinearity());
		//spherically symmetric log
		NONLINEARITY_TABLE_MAP.put("sslog",new SSLogNonlinearity());

		//nonlinearity on subspace
		NONLINEARITY_TABLE_MAP.put("subspace",new SubspaceNonlinearity());
		
		NONLINEARITY_TABLE_MAP.put("cov",new CovNonlinearity());
		
		NONLINEARITY_TABLE_MAP.put("cov1",new Cov1Nonlinearity());
	}

private NonlinearityTable nonlinearity=NONLINEARITY_TABLE_MAP.get("subspace");//the nonlinearity mapping
private int maxiteration=500;//max iteration times
private double tol=1e-10;//threshold controls when the algorithm converge
/*
 * iva will performed on subbands from high frequency to low frequency
 */
private int subbandsize=100;//subband size in taps, 0 for full frequency band
private int subbandshift=subbandsize/8;//subband shift taps
	
	/**
	 * <h1>Description</h1>
	 * IVA intermediate status.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Aug 5, 2012 10:00:47 AM, revision:
	 */
	private class IVAStat implements Serializable
	{
	private static final long serialVersionUID=2405055568733254664L;
	private Complex[][][] xdata;//already preprocessed data [binidx][sourcei][tau]
	private Complex[][][] ydata;//estimated source data [binidx][sourcei][tau]
	private Complex[][][] wp;//demixing matrices [binidx][][]
	private Set<Integer> ivabins;//unconverged frequency bins
	private double[] cosine;//cosine for each demixing matrix
	
		/**
		 * @param xdata
		 * already preprocessed data [binidx][sourcei][tau]
		 */
		public IVAStat(Complex[][][] xdata)
		{
			this.xdata=xdata;
			ydata=new Complex[numBins()][][];
			cosine=new double[numBins()];
			
			/*
			 * initialize demixing matrices
			 */
			wp=new Complex[numBins()][][];
			for(int f=0;f<wp.length;f++) 
				wp[f]=BLAS.eyeComplex(numSources(),numSources());
			
			/*
			 * initialize not converged bins
			 */
			ivabins=new HashSet<Integer>();
			for(int f=0;f<numBins();f++) ivabins.add(f);
		}
	
		/**
		 * get the number of frequency bins
		 * @return
		 */
		public int numBins()
		{
			return xdata.length;
		}
		
		/**
		 * get the number of sources
		 * @return
		 */
		public int numSources()
		{
			return xdata[0].length;
		}
		
		/**
		 * get the number of stft frames
		 * @return
		 */
		public int numSTFTFrames()
		{
			return xdata[0][0].length;
		}
		
		/**
		 * get estimated source data for a specified frequency bin
		 * @param binidx
		 * frequency bin index
		 * @return
		 */
		public Complex[][] estimatedSourceData(int binidx)
		{
			//separate data with new demixing matrix
			if(!isConverged(binidx)) 
				ydata[binidx]=BLAS.multiply(wp[binidx],xdata[binidx],ydata[binidx]);
			
			return ydata[binidx];
		}
		
		/**
		 * get sensor data for a frequency bin
		 * @param binidx
		 * frequency bin index
		 * @return
		 */
		public Complex[][] sensorData(int binidx)
		{
			return xdata[binidx];
		}
		
		/**
		 * get a demixing matrix
		 * @param binidx
		 * frequency bin index
		 * @return
		 */
		public Complex[][] demixingMatrix(int binidx)
		{
			return wp[binidx];
		}
		
		/**
		 * Update a demixing matrix, this method will do nothing if 
		 * corresponding frequency bin is converged.
		 * @param binidx
		 * frequency bin index
		 * @param wp1
		 * new demixing matrix
		 * @return
		 * true if the demixing matrix is converged
		 */
		public boolean updateDemixingMatrix(int binidx,Complex[][] wp1)
		{
		boolean converged=false;
		
			//already converged
			if(isConverged(binidx)) return true;
			
			/*
			 * see if this bin is converged or not
			 */
			cosine[binidx]=0;
			for(int sourcei=0;sourcei<wp1.length;sourcei++) 
				cosine[binidx]+=BLAS.innerProduct(wp[binidx][sourcei],wp1[sourcei]).abs();
			cosine[binidx]/=wp1.length;
			
			//converged
			if(Math.abs(1-cosine[binidx])<=tol) 
			{	
				ivabins.remove(binidx);
				converged=true;
			}
			
			//copy demixing matrix
			wp[binidx]=BLAS.copy(wp1,wp[binidx]);
			
			return converged;
		}
		
		/**
		 * see if a frequency bin is converged
		 * @param binidx
		 * frequency bin index
		 * @return
		 */
		public boolean isConverged(int binidx)
		{
			return !ivabins.contains(binidx);
		}
		
		/**
		 * get the number of unconverged frequency bins
		 * @return
		 */
		public int numBinsLeft()
		{
			return ivabins.size();
		}
		
		/**
		 * get the average cosine for all demixing matrices, 
		 * can be used as objective function
		 * @return
		 */
		public double cosine()
		{
			return BLAS.mean(cosine);
		}
	}

	public FastIVA()
	{}
	
	/**
	 * perform IVA on a clique
	 * @param ivastat
	 * iva status
	 * @param clique
	 * clique indices
	 */
	private void iva(IVAStat ivastat,ArrayList<Integer> clique)
	{
	Complex[][][] ydata;//estimated signals
	Complex[][] xdata;//sensor data
	Complex[][] wp,wp1;//the demixing matrix
	double e1;//the first expectation
	Complex[] e2;//the second expectation
	int binsleft=0;//number of still not converged bins
	int realbinidx;//real binidx in the entire frequency band
	Logger logger;
	
		/*
		 * initialize
		 */
		ydata=new Complex[clique.size()][][];
		wp1=new Complex[ivastat.numSources()][ivastat.numSources()];
		e2=new Complex[ivastat.numSources()];
		logger=Logger.getLogger(LOGGER_NAME);
		
		/*
		 * number of unconverged frequency bins
		 */
		for(int rbinidx:clique) if(!ivastat.isConverged(rbinidx)) binsleft++;
		if(binsleft<=0) return;
		
		//perform iteration
		for(int itcount=0;itcount<maxiteration;itcount++)
		{
			//get separated signals
			for(int binidx=0;binidx<ydata.length;binidx++) 
				ydata[binidx]=ivastat.estimatedSourceData(clique.get(binidx));
			
			//prepare for nonlinearity
			nonlinearity.setYData(ydata);
			
			//update layer by layer
			for(int binidx=0;binidx<ydata.length;binidx++) 
			{
				//get the actual bin index
				realbinidx=clique.get(binidx);
				//this frequency bin is converged
				if(ivastat.isConverged(realbinidx)) continue;
				
				//sensor data for current frequency bin
				xdata=ivastat.sensorData(realbinidx);
				//old demixing matrix for current frequency bin
				wp=ivastat.demixingMatrix(realbinidx);

				//update source by source
				for(int sourcei=0;sourcei<ivastat.numSources();sourcei++)
				{
					/*
					 * calculate expectations
					 */
					e1=0;
					Arrays.fill(e2,Complex.ZERO);
						
					for(int tau=0;tau<ivastat.numSTFTFrames();tau++) 
					{
					double dg,ddg;
					Complex temp;
					
						dg=nonlinearity.dg(binidx,sourcei,tau);
						ddg=nonlinearity.ddg(binidx,sourcei,tau);
						
						//G'+|y|^2*G''
						e1+=dg+BLAS.absSquare(ydata[binidx][sourcei][tau])*ddg;
						
						temp=ydata[binidx][sourcei][tau].conjugate().multiply(dg);
						for(int ii=0;ii<e2.length;ii++) 
							e2[ii]=e2[ii].add(xdata[ii][tau].multiply(temp));
					}
					
					e1/=ivastat.numSTFTFrames();
					e2=BLAS.scalarMultiply(1.0/ivastat.numSTFTFrames(),e2,e2);
					
					/*
					 * update demixing vector for current sourcei
					 */
					for(int jj=0;jj<wp1[sourcei].length;jj++) 
						wp1[sourcei][jj]=
							wp[sourcei][jj].conjugate().multiply(e1).subtract(e2[jj]).conjugate();
				}

				//perform symmetric decorrelation
				symmetricDecorrelation(wp1);
				
				//update demixing matrix for current bin
				if(ivastat.updateDemixingMatrix(realbinidx,wp1)) binsleft--;
			}
	
			logger.info("iteration "+itcount+", obj(->1)="+ivastat.cosine()+
					", "+ivastat.numBinsLeft()+" frequency bins left");
			//all bins are converged
			if(binsleft<=0) return;
		}
		
//		throw new AlgorithmNotConvergeException("maximum iteration times exceeded: "+maxiteration);
	}
	
	/**
	 * perform symmetric decorrelation
	 * @param wp1
	 */
	private void symmetricDecorrelation(Complex[][] wp1)
	{
	HermitianEigensolver eigensolver;//used for decorrelation
	HermitianEigensolver.EigenDecomposition decomp;	
	Complex[][] wh=null,edm,ev,evh=null;//used for decorrelation
	
		eigensolver=new CommonsEigensolver();
		edm=BLAS.eyeComplex(wp1.length,wp1.length);
	
		/*
		 * W*W'
		 */
		wh=BLAS.transpose(wp1,wh);
		wh=BLAS.multiply(wp1,wh,wh);
		
		/*
		 * (W*W')^-0.5
		 */
		decomp=eigensolver.eig(wh);

		//construct D^(-0.5)
		for(int i=0;i<edm.length;i++) 
			//Hermitian matrix has only real eigenvalues
			edm[i][i]=new Complex(1.0/Math.sqrt(decomp.eigenvalue(i).getReal()),0);
			
		ev=decomp.eigenvectors();
		wh=BLAS.multiply(ev,edm,wh);
		evh=BLAS.transpose(ev,evh);
		wh=BLAS.multiply(wh,evh,wh);

		//(W*W')^-0.5*W
		wp1=BLAS.multiply(wh,wp1,wp1);
	}

	public DemixingModel applyICA()
	{
	Complex[][][] xdata;//observed signals [bin index][source index][frame index]
	Complex[][][] prew;//preprocessing matrix [bin index][][]
	IVAStat ivastat;
	
		//load and preprocess data
		{
		FDBSSAlgorithm fdbss;
		Preprocessor preprocessor;
	
			fdbss=this.getFDBSSAlgorithm();
		
			/*
			 * load data
			 */
			xdata=new Complex[fdbss.fftSize()/2+1][][];
			for(int binidx=0;binidx<xdata.length;binidx++) 
				xdata[binidx]=fdbss.binData(binidx,xdata[binidx]);
		
			/*
			 * preprocess
			 */
			preprocessor=this.preprocessor();
			prew=new Complex[xdata.length][][];
			for(int binidx=0;binidx<xdata.length;binidx++) 
			{
				xdata[binidx]=preprocessor.preprocess(xdata[binidx],fdbss.numSources());
				prew[binidx]=preprocessor.transferMatrix();
			}
		}
		
		//apply iva
		{
		ArrayList<Integer> clique;
		Logger logger;
		int realsubbandsize;
		
			//intermediate status for iva
			ivastat=new IVAStat(xdata);
			logger=Logger.getLogger(LOGGER_NAME);
			
			if(subbandsize<=0) realsubbandsize=ivastat.numBins();
			else realsubbandsize=subbandsize;
			for(int offset=ivastat.numBins()-realsubbandsize;;) 
			{
				logger.info("perform IVA on subband: ["+offset+", "+(offset+realsubbandsize-1)+"]");
			
				clique=new ArrayList<Integer>(realsubbandsize);
				for(int f=offset;f<offset+realsubbandsize;f++) clique.add(f);
				
				iva(ivastat,clique);
				
				//finished
				if(offset==0) break;
				
				offset-=subbandshift;
				if(offset<0) offset=0;
			}
		}

		//return result
		{
		FDBSSAlgorithm fdbss;
		DemixingModel model;
		
			fdbss=this.getFDBSSAlgorithm();
			model=new DemixingModel(fdbss.numSources(),fdbss.numSensors(),fdbss.fftSize());
			
			for(int binidx=0;binidx<model.fftSize()/2+1;binidx++) 
				model.setDemixingMatrix(
						binidx,
						BLAS.multiply(ivastat.demixingMatrix(binidx),prew[binidx],prew[binidx]));

			return model;
		}
	}
	
	public static void main(String[] args) throws IOException, UnsupportedAudioFileException
	{
	SignalSource x0,x1,x;
	FDBSS fdbss;
	long t;
			
		x0=new WaveSource(new File("data/rsm2_mA.wav"),true);
		x1=new WaveSource(new File("data/rsm2_mB.wav"),true);
		x=new SignalMixer(x0,x1);
				
		fdbss=new FDBSS(new File("temp"));
		fdbss.setParameter(Parameter.stft_size,"512");//stft block size
		fdbss.setParameter(Parameter.stft_overlap,Integer.toString((int)(512*1/2)));//stft overlap
		fdbss.setParameter(Parameter.fft_size,"1024");//fft size, must be powers of 2
		fdbss.setParameter(Parameter.ica_algorithm,FastIVA.class.getName());
		fdbss.setParameter(Parameter.align_policy,"cn.edu.bjtu.cit.bss.align.IdentityAlign");//without permutation

		t=System.currentTimeMillis();
		fdbss.separate(x,Operation.stft);
		fdbss.separate(x,Operation.ica);
		fdbss.separate(x,Operation.align,Operation.demix);
		t=System.currentTimeMillis()-t;
		x.close();

		System.out.println("time spent: "+t);
		fdbss.visualizeSourceSpectrograms(1,fdbss.numSources());
	}
}
