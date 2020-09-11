package cn.edu.bjtu.cit.bss.iva;
import java.io.*;
import java.util.logging.*;
import javax.sound.sampled.*;
import org.apache.commons.math.complex.*;
import pp.util.BLAS;
import cn.edu.bjtu.cit.bss.*;
import cn.edu.bjtu.cit.bss.FDBSS.Parameter;
import cn.edu.bjtu.cit.bss.preprocess.*;
import cn.edu.bjtu.cit.bss.signalio.*;

/**
 * <h1>Description</h1>
 * Gradient descent method for independ vector analysis. Please see: Taesu Kim, 
 * Hagai T. Attias, Soo-Young Lee, Blind Source Separation Exploiting Higher-
 * Order Frequency Dependencies, IEEE Trans. Audio, Speech, and Language Processing, 
 * vol. 15, no. 1, pp. 10-79, 2007. Matlab code is available: 
 * http://inc.ucsd.edu/~taesu/code/ivabss.zip
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: May 4, 2012 8:14:48 AM, revision:
 */
public class IVABSS extends IVA
{
private static final long serialVersionUID=8141674565495810313L;
private static final String LOGGER_NAME="cn.edu.bjtu.cit.bss";
private int maxiteration=1000;//max iteration time
private double epsi=1e-6;//prevent zero denominator
private double eta=0.1;//the learning step size
private double tol=1e-6;//tolerance for algorithm termination

	public IVABSS()
	{}
	
	/**
	 * apply IVA on a frequency band
	 * @param offset
	 * frequency band offset
	 * @param len
	 * frequency band length
	 * @param model
	 * the demixing matrices will returned into the model
	 */
	private void iva(int offset,int len,DemixingModel model)
	{
	FDBSSAlgorithm fdbss;
	Complex[][][] xdata;//observed signals
	Preprocessor preprocessor;
	Complex[][][] prew;//preprocessing matrix [bin index][][]
	Complex[][][] wp;//demixing matrix for preprocessed data
	
		fdbss=this.getFDBSSAlgorithm();
		
		/*
		 * check subband size
		 */
		if(offset<0||offset>=fdbss.fftSize()/2) throw new IndexOutOfBoundsException(
				"subband offset out of bounds: "+offset+", "+fdbss.fftSize()/2+1);
		if(offset+len>fdbss.fftSize()/2+1) throw new IndexOutOfBoundsException(
				"subband length out of bounds: "+(offset+len)+", "+(fdbss.fftSize()/2+1));
		
		/*
		 * load data
		 */
		xdata=new Complex[len][][];
		for(int binidx=offset;binidx<offset+len;binidx++) 
			xdata[binidx-offset]=fdbss.binData(binidx,xdata[binidx-offset]);
		
		/*
		 * preprocess
		 */
		preprocessor=this.preprocessor();
		prew=new Complex[len][][];
		for(int binidx=0;binidx<xdata.length;binidx++) 
		{
			xdata[binidx]=preprocessor.preprocess(xdata[binidx],fdbss.numSources());
			prew[binidx]=preprocessor.transferMatrix();
		}
		
		/*
		 * initialize demixing matrix
		 */
		wp=new Complex[len][][];
		for(int binidx=0;binidx<wp.length;binidx++) 
			wp[binidx]=BLAS.eyeComplex(fdbss.numSources(),fdbss.numSources());

		/*
		 * perform iva
		 */
		{
		Logger logger;
		Complex[][][] ydata;//estimated signals
		Complex[][] dwp=null;//delta in each iteration
		double[][] ssq;//used to calculate score function [N][tau]
		Complex[] phiy;//nonlinear transformed vector
		Complex[][] tempg;//store the expectation
		double dlw,obj,pobj=Double.MAX_VALUE,dobj,sumssq;//for objective function	
		
			logger=Logger.getLogger(LOGGER_NAME);
		
			ydata=new Complex[xdata.length][][];
			ssq=new double[xdata[0].length][xdata[0][0].length];
			phiy=new Complex[xdata[0].length];
			tempg=new Complex[xdata[0].length][xdata[0].length];
		
			for(int it=0;it<maxiteration;it++)
			{
				//separate signals
				for(int binidx=0;binidx<ydata.length;binidx++) 
					ydata[binidx]=BLAS.multiply(wp[binidx],xdata[binidx],ydata[binidx]);

				/*
				 * prepare score function
				 */
				BLAS.fill(ssq,0);
				for(int binidx=0;binidx<ydata.length;binidx++) 
					for(int sourcei=0;sourcei<ydata[binidx].length;sourcei++) 
						for(int tau=0;tau<ydata[binidx][sourcei].length;tau++) 
							ssq[sourcei][tau]+=BLAS.absSquare(ydata[binidx][sourcei][tau]);
				
				sumssq=0;
				for(int sourcei=0;sourcei<ssq.length;sourcei++) 
					for(int tau=0;tau<ssq[sourcei].length;tau++) 
					{
						ssq[sourcei][tau]=Math.sqrt(ssq[sourcei][tau]);
						sumssq+=ssq[sourcei][tau];//used in objective function
						ssq[sourcei][tau]=1.0/(ssq[sourcei][tau]+epsi);
					}
			
				/*
				 * update layer by layer according to frequency bin
				 */
				dlw=0;
				for(int binidx=0;binidx<wp.length;binidx++) 
				{
					BLAS.fill(tempg,Complex.ZERO);
				
					//traverse all data in a frequency bin
					for(int tau=0;tau<ydata[0][0].length;tau++) 
					{
						//after apply score function
						for(int sourcei=0;sourcei<phiy.length;sourcei++) 
							phiy[sourcei]=ydata[binidx][sourcei][tau].multiply(ssq[sourcei][tau]);
					
						//accumulate outer product
						for(int i=0;i<tempg.length;i++) 
							for(int j=0;j<tempg[i].length;j++) 
								tempg[i][j]=tempg[i][j].add(phiy[i].multiply(ydata[binidx][j][tau].conjugate()));
					}

					//I-E{G}
					for(int i=0;i<tempg.length;i++) 
						for(int j=0;j<tempg[i].length;j++) 
						{
							tempg[i][j]=tempg[i][j].multiply(1.0/ydata[0][0].length);
				
							if(i==j) tempg[i][j]=Complex.ONE.subtract(tempg[i][j]);
							else tempg[i][j]=tempg[i][j].negate();
						}
				
					//delta demixing matrix
					dwp=BLAS.multiply(tempg,wp[binidx],dwp);
				
					/*
					 * update demixing matrix
					 */
					dlw+=Math.log(BLAS.det(wp[binidx]).abs()+epsi);//for objective function
					
					dwp=BLAS.scalarMultiply(eta,dwp,dwp);
					wp[binidx]=BLAS.add(wp[binidx],dwp,wp[binidx]);
				}
			
				/*
				 * see if the algorithm is converged
				 */
				obj=(sumssq/xdata[0][0].length-dlw)/(xdata[0].length*xdata.length);
				dobj=pobj-obj;
				pobj=obj;
		    
				logger.info("iteration "+it+", obj="+obj+", dobj="+dobj);
		    
				if(Math.abs(dobj)/Math.abs(obj)<tol) break;
			}
		}
		
		//return results
		for(int binidx=offset;binidx<offset+len;binidx++) 
			model.setDemixingMatrix(
					binidx,
					BLAS.multiply(wp[binidx-offset],prew[binidx-offset],prew[binidx-offset]));
	}
	
	public DemixingModel applyICA()
	{
	FDBSSAlgorithm fdbss;
	DemixingModel model;
	
		fdbss=this.getFDBSSAlgorithm();
		model=new DemixingModel(fdbss.numSources(),fdbss.numSensors(),fdbss.fftSize());
	
		iva(0,fdbss.fftSize()/2+1,model);
	
		return model;
	}
	
	public static void main(String[] args) throws IOException, UnsupportedAudioFileException
	{
	SignalSource x0,x1,x;
	FDBSS fdbss;
	
		x0=new WaveSource(new File("data/rsm2_mA.wav"),true);
		x1=new WaveSource(new File("data/rsm2_mB.wav"),true);
		x=new SignalMixer(x0,x1);
		
		fdbss=new FDBSS(new File("temp"));
		fdbss.setParameter(Parameter.stft_size,"512");//stft block size
		fdbss.setParameter(Parameter.stft_overlap,Integer.toString((int)(512*1/2)));//stft overlap
		fdbss.setParameter(Parameter.fft_size,"1024");//fft size, must be powers of 2
		fdbss.setParameter(Parameter.ica_algorithm,"cn.edu.bjtu.cit.bss.iva.IVABSS");
		fdbss.setParameter(Parameter.align_policy,"cn.edu.bjtu.cit.bss.align.IdentityAlign");//without permutation

		fdbss.separate(x);
		x.close();
		
		fdbss.visualizeSourceSpectrograms(1,fdbss.numSources());
	}
}
