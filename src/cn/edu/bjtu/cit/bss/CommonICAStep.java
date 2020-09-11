package cn.edu.bjtu.cit.bss;
import java.io.*;
import java.util.logging.*;
import org.apache.commons.math.complex.*;
import cn.edu.bjtu.cit.bss.ica.*;
import cn.edu.bjtu.cit.bss.preprocess.*;
import pp.util.BLAS;

/**
 * <h1>Description</h1>
 * Common ICA steps used in frequency domain BSS.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Oct 28, 2011 10:48:41 AM, revision:
 */
public class CommonICAStep extends ICAStep
{
private static final long serialVersionUID=-5542138787760354660L;
private static final String LOGGER_NAME="cn.edu.bjtu.cit.bss";
private ICA ica;//the ICA algorithm
private int icaretry=10;//number of retry times when ica not converge
private double seeddelta=2;//perturbation add to the initial seed when retry ica

	public CommonICAStep()
	{}
	
	/**
	 * get ICA algorithm
	 * @return
	 */
	public ICA icaAlgorithm()
	{
		return ica;
	}
	
	/**
	 * set ICA algorithm
	 * @param name
	 * ICA class name
	 */
	public void setICA(String name)
	{
		try
		{
			ica=(ICA)Class.forName(name).newInstance();
		}
		catch(InstantiationException e)
		{
			throw new RuntimeException("failed to instantiate ICA: "+name,e);
		}
		catch(IllegalAccessException e)
		{
			throw new RuntimeException("failed to instantiate ICA: "+name,e);
		}
		catch(ClassNotFoundException e)
		{
			throw new RuntimeException("failed to instantiate ICA: "+name,e);
		}
	}
	
	/**
	 * add a small perturbation to the seed used in ICA
	 * @param seed
	 * initial seed
	 * @param seed2
	 * new seed, null to allocate new space
	 * @return
	 */
	private Complex[][] perturbSeed(Complex[][] seed,Complex[][] seed2)
	{
	double real,imag;
	
		if(seed2==null) seed2=new Complex[seed.length][seed[0].length];
		else if(seed.length!=seed2.length||seed[0].length!=seed2[0].length) 
			throw new IllegalArgumentException("seed size not match: "+seed2.length+" x "+seed2[0].length+
					", required: "+seed.length+" x "+seed[0].length);
		
		for(int i=0;i<seed2.length;i++)
			for(int j=0;j<seed2[i].length;j++)
			{
				real=seed[i][j].getReal()+Math.random()*seeddelta-seeddelta/2;
				imag=seed[i][j].getImaginary()+Math.random()*seeddelta-seeddelta/2;
				seed2[i][j]=new Complex(real,imag);
			}
		
		BLAS.orthogonalize(seed2);
		return seed2;
	}
	
	/**
	 * <h1>Description</h1>
	 * Store preprocessed demixing matrix, demixing matrix, seed for next bin.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Oct 29, 2011 9:00:32 AM, revision:
	 */
	private static class ICAStepResults implements Serializable
	{
	private static final long serialVersionUID=3311211512043220633L;
	boolean converged;//true if the ICA algorithm is converged
	Complex[][] demix;//demixing matrix for frequency bin data
	Complex[][] nextseed;//seed for next bin

		/**
		 * @param converged
		 * true if the ICA algorithm is converged
		 * @param demix
		 * demixing matrix for frequency bin data
		 * @param nextseed
		 * seed for next bin
		 */
		public ICAStepResults(boolean converged,Complex[][] demix,Complex[][] nextseed)
		{
			this.converged=converged;
			this.demix=demix;
			this.nextseed=nextseed;
		}
	}
	
	/**
	 * apply ICA on already preprocessed bin data
	 * @param sigsp
	 * already preprocessed data
	 * @param seed
	 * initial seeds
	 * @return
	 * demixing matrix and seed for next bin
	 * @throws AlgorithmNotConvergeException
	 * ICA not converge after retries
	 */
	public Complex[][] applyICAPreprocessed(Complex[][] sigsp,Complex[][] seed)
	{
	Complex[][] seed2;//seed bakup
	Complex[][] demixp=null;//demixing matrix for preprocessed signals
	int numretry;
	Logger logger;
		
		logger=Logger.getLogger(LOGGER_NAME);
			
		/*
		 * calculate the demixing matrix for preprocessed signals
		 */
		seed2=BLAS.copy(seed,null);
		for(numretry=0;numretry<icaretry;numretry++)
		{
			try
			{
				demixp=ica.icaPreprocessed(sigsp,seed2).getDemixingMatrixPreprocessed();
				break;//converged
			}
			catch(AlgorithmNotConvergeException e)
			{
				/*
				 * the log level is info, because the final result may converge 
				 * after some retries.
				 */
				logger.info("ICA not converge, retry "+(numretry+1));
					
				//add a small perturbation to the initial seed and retry
				seed2=perturbSeed(seed,seed2);
			}
		}
		
		if(numretry<icaretry) return demixp;
		//algorithm not converge
		else throw new AlgorithmNotConvergeException("ICA not converge after "+numretry+" retries");
	}
	
	/**
	 * apply ica for a frequency bin
	 * @param sigs
	 * signals of a frequency bin
	 * @param seed
	 * initial seeds
	 * @return
	 */
	public ICAStepResults applyICA(Complex[][] sigs,Complex[][] seed)
	{
	Preprocessor preprocessor;
	Complex[][] sigsp;//preprocessed signals
	Complex[][] demixp=null;//demixing matrix for preprocessed signals
	Complex[][] demix;//demixing matrix for original signals
	Complex[][] nextseed;//seed for next frequency bin
	boolean converged;
	
		/*
		 * preprocessing, number of seeds implies number of output channels
		 */
		preprocessor=this.preprocessor();
		sigsp=preprocessor.preprocess(sigs,seed.length);

		//calculate the demixing matrix for preprocessed signals
		try
		{
			demixp=applyICAPreprocessed(sigsp,seed);
			converged=true;
			/*
			 * Algorithm converged, use demixing matrix as the seed for the next bin. 
			 * Seed is used as column vector, while demix matrix is used as row vector, 
			 * thus conjugate is required.
			 */
			nextseed=BLAS.conjugate(demixp,null);
		}
		catch(AlgorithmNotConvergeException e)
		{
			//use identity demixing matrix instead
			demixp=BLAS.eyeComplex(sigsp.length,sigsp.length);
			converged=false;
			nextseed=seed;
		}
		
		//demixing matrix for original input
		demix=BLAS.multiply(demixp,preprocessor.transferMatrix(),null);
		
		return new ICAStepResults(converged,demix,nextseed);
	}
	
	public DemixingModel applyICA()
	{
	Logger logger;
	FDBSSAlgorithm fdbss;
	DemixingModel model;
	ICAStepResults icares;
	Complex[][] buffer=null;//bin data buffer
	Complex[][] seed;//seed for ica
					
		logger=Logger.getLogger(LOGGER_NAME);
		fdbss=this.getFDBSSAlgorithm();
		model=new DemixingModel(fdbss.numSources(),fdbss.numSensors(),fdbss.fftSize());
		
		/*
		 * generate fixed initial seeds
		 */
		seed=BLAS.eyeComplex(fdbss.numSources(),fdbss.numSources());
//		seed=BLAS.randComplexMatrix(fdbss.numSources(),fdbss.numSources());//generate random initial seeds
//		BLAS.orthogonalize(seed);
			
		/*
		 * Apply ica on frequency bins. Only half of demixing matrices 
		 * will be calculated, the other half can be generated by utilizing 
		 * the complex conjugate property.
		 */
		for(int binidx=0;binidx<fdbss.fftSize()/2+1;binidx++)
		{
			/*
			 * load data
			 */
			logger.info("apply ICA on frequency bin "+binidx);
			buffer=fdbss.binData(binidx,buffer);

			/*
			 * apply ica
			 */
			icares=applyICA(buffer,seed);
			seed=icares.nextseed;
			if(!icares.converged) logger.warning(
					"ICA not converge after "+icaretry+" retries for bin "+binidx);
			
			//get the demixing matrix for current frequency bin
			model.setDemixingMatrix(binidx,icares.demix);
		}

		return model;
	}
}
