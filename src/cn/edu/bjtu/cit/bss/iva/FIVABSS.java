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
import cn.edu.bjtu.cit.bss.ica.*;
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
 * @version created on: May 14, 2012 9:23:18 AM, revision:
 */
public class FIVABSS extends IVA
{
private static final long serialVersionUID=6020136875032731448L;
private static final String LOGGER_NAME="cn.edu.bjtu.cit.bss";

	/**
	 * <h1>Description</h1>
	 * The spherically symmetric Laplacian distribution nonlinearity (SSL).
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: May 14, 2012 2:36:00 PM, revision:
	 */
	private static class SSL implements Nonlinearity
	{
	private static final long serialVersionUID=-7290219055947470780L;
	
		public double g(double u)
		{
			return Math.sqrt(u);
		}
		
		public double dg(double u)
		{
			return 1.0/(2.0*Math.sqrt(u));
		}

		public double ddg(double u)
		{
			return -1.0/(4.0*Math.sqrt(u*u*u));
		}		
	}
	
	/**
	 * <h1>Description</h1>
	 * The SNP nonlinearity.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: May 14, 2012 2:43:52 PM, revision:
	 */
	private static class SNP implements Nonlinearity
	{
	private static final long serialVersionUID=5070341602013528002L;
	
		public double g(double u)
		{
			return Math.log(u);
		}
		
		public double dg(double u)
		{
			return 1.0/u;
		}

		public double ddg(double u)
		{
			return -1.0/(u*u);
		}
	}
	
/*
 * supported nonlinearity function
 */
private static final Map<String,Nonlinearity> NONLINEARITY_MAP=new HashMap<String,Nonlinearity>();

	static
	{
		NONLINEARITY_MAP.put("SSL",new SSL());
		NONLINEARITY_MAP.put("SNP",new SNP());
	}	
	
private Nonlinearity nonlinearity=NONLINEARITY_MAP.get("SNP");//the nonlinearity mapping
private int maxiteration=1000;//max iteration times
private double tol=1e-6;//threshold controls when the algorithm converge

	public FIVABSS()
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
	FDBSSAlgorithm fdbss;//bss algorithm reference
	Complex[][][] xdata;//observed signals
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
	
		//prepare observed data
		{
		Preprocessor preprocessor;
			
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
		}
		
		//perform iva
		{
		int itcount;//iteration count
		Complex[][][] ydata;//estimated signals
		double[][] sumabsy2;//used to calculate nonlinearity mapping [N][tau]	
		double e1;//the first expectation
		Complex[] e2;//the second expectation
		double obj,pobj=Double.MAX_VALUE,dobj;//objective function
		HermitianEigensolver eigensolver;//used for decorrelation
		Complex[][] wh=null,edm,ev,evh=null;//used for decorrelation
		Logger logger;
		
			/*
			 * initialize demixing matrix
			 */
			wp=new Complex[xdata.length][][];
			for(int binidx=0;binidx<wp.length;binidx++) 
				wp[binidx]=BLAS.eyeComplex(fdbss.numSources(),fdbss.numSources());
			
			/*
			 * allocate space
			 */
			ydata=new Complex[xdata.length][][];
			sumabsy2=new double[xdata[0].length][xdata[0][0].length];
			e2=new Complex[xdata[0].length];
			
			eigensolver=new CommonsEigensolver();
			edm=BLAS.eyeComplex(fdbss.numSources(),fdbss.numSources());
			
			logger=Logger.getLogger(LOGGER_NAME);
			
			//perform iteration
			for(itcount=0;itcount<maxiteration;itcount++)
			{
				//separate signals
				for(int binidx=0;binidx<ydata.length;binidx++) 
					ydata[binidx]=BLAS.multiply(wp[binidx],xdata[binidx],ydata[binidx]);	
				
				/*
				 * prepare nonlinearity
				 */
				BLAS.fill(sumabsy2,0);
				for(int binidx=0;binidx<ydata.length;binidx++) 
					for(int sourcei=0;sourcei<ydata[binidx].length;sourcei++) 
						for(int tau=0;tau<ydata[binidx][sourcei].length;tau++) 
							sumabsy2[sourcei][tau]+=BLAS.absSquare(ydata[binidx][sourcei][tau]);
				
				//see if the algorithm converge
				{
					obj=0;
					for(int sourcei=0;sourcei<sumabsy2.length;sourcei++) 
						for(int tau=0;tau<sumabsy2[sourcei].length;tau++) 
							obj+=nonlinearity.g(sumabsy2[sourcei][tau]);
					obj/=(ydata.length*ydata[0].length*ydata[0][0].length);

					dobj=pobj-obj;
					pobj=obj;
				
					logger.info("iteration "+itcount+", obj="+obj+", dobj="+dobj);
				
					if(Math.abs(dobj)/Math.abs(obj)<tol) break;
				}
					
				//update layer by layer
				for(int binidx=0;binidx<ydata.length;binidx++)
				{
					//update source by source
					for(int sourcei=0;sourcei<ydata[0].length;sourcei++)
					{
						/*
						 * calculate expectations
						 */
						e1=0;
						Arrays.fill(e2,Complex.ZERO);
						
						for(int tau=0;tau<sumabsy2[0].length;tau++) 
						{
						double dg,ddg;
						Complex temp;
							
							dg=nonlinearity.dg(sumabsy2[sourcei][tau]);
							ddg=nonlinearity.ddg(sumabsy2[sourcei][tau]);
						
							e1+=dg+BLAS.absSquare(ydata[binidx][sourcei][tau])*ddg;
							
							temp=ydata[binidx][sourcei][tau].conjugate().multiply(dg);
							for(int ii=0;ii<e2.length;ii++) 
								e2[ii]=e2[ii].add(xdata[binidx][ii][tau].multiply(temp));							
						}
					
						e1/=sumabsy2[0].length;
						e2=BLAS.scalarMultiply(1.0/sumabsy2[0].length,e2,e2);
					
						//update demixing vector for current sourcei
						for(int jj=0;jj<wp[binidx][sourcei].length;jj++) 
							wp[binidx][sourcei][jj]=
								wp[binidx][sourcei][jj].conjugate().multiply(e1).subtract(e2[jj]).conjugate();
					}
					
					//symmetric decorrelation
					{
					HermitianEigensolver.EigenDecomposition decomp;
					
						/*
						 * W*W'
						 */
						wh=BLAS.transpose(wp[binidx],wh);
						wh=BLAS.multiply(wp[binidx],wh,wh);
						
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
						wp[binidx]=BLAS.multiply(wh,wp[binidx],wp[binidx]);
					}
				}
			}
			
//			if(itcount>=maxiteration) throw new AlgorithmNotConvergeException(
//					"maximum iteration times exceeded: "+maxiteration);
			
			//return results
			for(int binidx=offset;binidx<offset+len;binidx++) 
				model.setDemixingMatrix(
						binidx,
						BLAS.multiply(wp[binidx-offset],prew[binidx-offset],prew[binidx-offset]));
		}
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
		fdbss.setParameter(Parameter.ica_algorithm,"cn.edu.bjtu.cit.bss.iva.FIVABSS");
		fdbss.setParameter(Parameter.align_policy,"cn.edu.bjtu.cit.bss.align.IdentityAlign");//without permutation

		fdbss.separate(x,Operation.stft);
		fdbss.separate(x,Operation.ica);
		fdbss.separate(x,Operation.align,Operation.demix);
		x.close();

		fdbss.visualizeSourceSpectrograms(1,fdbss.numSources());		
	}
}
