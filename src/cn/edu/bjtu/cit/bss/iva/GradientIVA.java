package cn.edu.bjtu.cit.bss.iva;
import java.awt.Point;
import java.io.*;
import java.util.*;
import java.util.logging.*;
import javax.sound.sampled.*;
import org.apache.commons.math.complex.*;
import pp.util.BLAS;
import cn.edu.bjtu.cit.bss.*;
import cn.edu.bjtu.cit.bss.FDBSS.Operation;
import cn.edu.bjtu.cit.bss.FDBSS.Parameter;
import cn.edu.bjtu.cit.bss.align.*;
import cn.edu.bjtu.cit.bss.preprocess.*;
import cn.edu.bjtu.cit.bss.signalio.*;

/**
 * <h1>Description</h1>
 * General gradient descent algorithm.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Jul 10, 2012 8:45:19 AM, revision:
 */
public class GradientIVA extends IVA
{
private static final long serialVersionUID=-442082604962831135L;
private static final String LOGGER_NAME="cn.edu.bjtu.cit.bss";
/*
 * supported score functions
 */
private static final Map<String,ScoreFunction> PHI_MAP=new HashMap<String,ScoreFunction>();

	static
	{
		PHI_MAP.put("inststanh",new InstSTanh());
		PHI_MAP.put("insttanh",new InstSTanh());
		PHI_MAP.put("ssl",new SSLScoreFunction());
		
		//chain of local cliques approach
		PHI_MAP.put("chaincliquessl",new ChainCliqueSSLScoreFunction());
	}

private int maxiteration=1000;//max iteration time
private double eta=0.1;//the learning step size
private double tol=1e-10;//tolerance for algorithm termination
private ScoreFunction nonlinearity=PHI_MAP.get("chaincliquessl");//used to calculate nonlinearity mapping

	public GradientIVA()
	{}

	/**
	 * iva algorithm
	 * @param xdata
	 * already preprocessed data [bin index][source index][frame index]
	 * @return
	 * demixing matrices for preprocessed data [bin index][][]
	 */
	private Complex[][][] iva(Complex[][][] xdata)
	{
	Complex[][][] ydata;//estimated signals
	Complex[][][] wp;//demixing matrix for preprocessed data
	Complex[][] dwp;//delta in each iteration
	Complex[] phiy;//nonlinear transformed vector
	double[] normg;//norm for each bin
	Set<Integer> bins;//frequency bin indices need to be updated
	Set<Integer> convergedbins;//converged bins in each iteration	
	Logger logger;

		/*
		 * initialize
		 */
		wp=new Complex[xdata.length][][];
		for(int binidx=0;binidx<wp.length;binidx++) 
			wp[binidx]=BLAS.eyeComplex(xdata[0].length,xdata[0].length);

		ydata=new Complex[xdata.length][][];
		dwp=new Complex[xdata[0].length][xdata[0].length];
		phiy=new Complex[xdata[0].length];
		logger=Logger.getLogger(LOGGER_NAME);
		
		normg=new double[xdata.length];
		bins=new LinkedHashSet<Integer>();
		for(int binidx=0;binidx<xdata.length;binidx++) bins.add(binidx);
		convergedbins=new HashSet<Integer>();
		
		//iva iteration
		for(int itcount=0;itcount<maxiteration;itcount++)
		{
			//separate signals
			for(int binidx:bins) ydata[binidx]=BLAS.multiply(wp[binidx],xdata[binidx],ydata[binidx]);

			//prepare score function
			nonlinearity.setYData(ydata);
				
			//update layer by layer according to frequency bin
			for(int binidx:bins)
			{
				BLAS.fill(dwp,Complex.ZERO);
				
				//traverse all data in a frequency bin
				for(int tau=0;tau<ydata[0][0].length;tau++) 
				{
					//after apply score function
					for(int sourcei=0;sourcei<phiy.length;sourcei++) 
						phiy[sourcei]=nonlinearity.phi(binidx,sourcei,tau);
			
					//accumulate outer product
					for(int ii=0;ii<dwp.length;ii++) 
						for(int jj=0;jj<dwp[ii].length;jj++) 
							dwp[ii][jj]=dwp[ii][jj].add(phiy[ii].multiply(ydata[binidx][jj][tau].conjugate()));
				}

				/*
				 * I-E{G}
				 */
				for(int i=0;i<dwp.length;i++) 
					for(int j=0;j<dwp[i].length;j++) 
					{
						dwp[i][j]=dwp[i][j].multiply(1.0/ydata[0][0].length);
						
						if(i==j) dwp[i][j]=Complex.ONE.subtract(dwp[i][j]);
						else dwp[i][j]=dwp[i][j].negate();
					}
				
				/*
				 * to see if this bin is converged
				 */
				normg[binidx]=0;
				for(int i=0;i<dwp.length;i++) 
					for(int j=0;j<dwp[i].length;j++) 
						normg[binidx]+=dwp[i][j].abs();
				normg[binidx]/=dwp.length*dwp[0].length;
				
				if(Math.abs(normg[binidx])<=tol) convergedbins.add(binidx);
				
				/*
				 * update demixing matrix
				 */
				//demixing matrix derivative
				dwp=BLAS.multiply(dwp,wp[binidx],dwp);

				dwp=BLAS.scalarMultiply(eta,dwp,dwp);
				wp[binidx]=BLAS.add(wp[binidx],dwp,wp[binidx]);
			}

			/*
			 * see if the algorithm converge
			 */
			bins.removeAll(convergedbins);//no longer need to update converged bins
			logger.info("iteration "+itcount+", obj (->0)="+BLAS.sum(normg)/normg.length
					+", "+bins.size()+" frequency bins left");
			if(bins.isEmpty()) return wp;
		}
		
//		throw new AlgorithmNotConvergeException("maximum iteration times exceeded: "+maxiteration);
		return wp;
	}

	public DemixingModel applyICA()
	{
	Complex[][][] xdata;//observed signals [bin index][source index][frame index]
	Complex[][][] prew;//preprocessing matrix [bin index][][]
	Complex[][][] wp;//demixing matrix for preprocessed data
	List<Point> subbandl=new LinkedList<Point>();//record subband
		
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
		int subbandsize=xdata.length,len;
		Complex[][][] xdata0=null,wp0;
			
			wp=new Complex[xdata.length][][];
				
				
				
				
				
	
			for(int binidx=0;binidx<wp.length;binidx++) 
				wp[binidx]=BLAS.eyeComplex(xdata[0].length,xdata[0].length);

				
				
				
				
				
			int offset=0;
				
//			for(int offset=0;offset<xdata.length;) 
//			{
				if(offset+subbandsize<=xdata.length) len=subbandsize;
				else len=xdata.length-offset;
				subbandl.add(new Point(offset,len));
					
					
				System.out.println(offset+", "+(offset+len-1));
					
					

				if(xdata0==null||xdata0.length!=len) xdata0=new Complex[len][][];
				for(int f=offset;f<offset+len;f++) xdata0[f-offset]=xdata[f];
					
				wp0=iva(xdata0);
				for(int f=offset;f<offset+len;f++) wp[f]=wp0[f-offset];
					
				offset+=len;
//			}
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
						BLAS.multiply(wp[binidx],prew[binidx],prew[binidx]));
				
				
				
				
				
				
				
				
				
				
			AlignPolicy align=((FDBSS)this.getFDBSSAlgorithm()).alignPolicy();
			Point p0=subbandl.remove(0);
			for(Point p1:subbandl) 
			{
				align.merge(model,p0.x,p0.y,p1.x,p1.y);
				p0=p1;
			}
				
				
				
				
				
				
				
				
				
				
			return model;
		}
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
		fdbss.setParameter(Parameter.ica_algorithm,GradientIVA.class.getName());
		fdbss.setParameter(Parameter.align_policy,"cn.edu.bjtu.cit.bss.align.IdentityAlign");//without permutation

		fdbss.separate(x,Operation.stft);
		fdbss.separate(x,Operation.ica);
		fdbss.separate(x,Operation.align,Operation.demix);
		x.close();

		fdbss.visualizeSourceSpectrograms(1,fdbss.numSources());
	}
}
