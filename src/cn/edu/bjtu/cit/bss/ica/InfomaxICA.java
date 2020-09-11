package cn.edu.bjtu.cit.bss.ica;
import java.io.*;
import java.util.*;
import org.apache.commons.math.complex.*;
import cn.edu.bjtu.cit.bss.*;
import cn.edu.bjtu.cit.bss.preprocess.*;
import cn.edu.bjtu.cit.bss.util.*;
import pp.util.BLAS;

/**
 * <h1>Description</h1>
 * See the paper: Vince Calhoun, Complex Infomax: Convergence and Approximation of 
 * Infomax with Complex Nonlinearities, Journal of VLSI Signal Processing, vol. 44, 
 * pp. 173-190, 2006.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Mar 6, 2012 2:58:22 PM, revision:
 */
public class InfomaxICA extends ICA implements Serializable
{
private static final long serialVersionUID=5619052344980807251L;

	/**
	 * <h1>Description</h1>
	 * the split tanh nonlinearity function.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Mar 6, 2012 3:35:53 PM, revision:
	 */
	public static class STanh implements CNonlinearity
	{
	private static final long serialVersionUID=-4076175300571900249L;

		public Complex g(Complex u)
		{
			return new Complex(Math.tanh(u.getReal()),Math.tanh(u.getImaginary()));
		}
	
		public Complex dg(Complex u)
		{
			throw new UnsupportedOperationException();
		}

		public Complex ddg(Complex u)
		{
			throw new UnsupportedOperationException();
		}
	}
	
	/**
	 * <h1>Description</h1>
	 * The full tanh nonlinearity function.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Mar 6, 2012 4:15:08 PM, revision:
	 */
	public static class FTanh implements CNonlinearity
	{
	private static final long serialVersionUID=-5207792009633357555L;

		public Complex g(Complex u)
		{
			return u.tanh().multiply(-2);
		}
	
		public Complex dg(Complex u)
		{
			throw new UnsupportedOperationException();
		}

		public Complex ddg(Complex u)
		{
			throw new UnsupportedOperationException();
		}
	}

/*
 * the nonlinearity funciton used
 */
private static Map<String,CNonlinearity> NONLINEARITY=new HashMap<String,CNonlinearity>();

	static
	{
		NONLINEARITY.put("stanh",new STanh());
		NONLINEARITY.put("ftanh",new FTanh());
	}

/*
 * the full tanh not work, I don't know why...
 */
private CNonlinearity nonlinearity=NONLINEARITY.get("stanh");//the nonlinearity function
private double pcath=1e-6;//threshold used in PCA
private double epsilon=1e-10;//convergence threshold of the ica algorithm
private double eta=0.01;//step size for gradient method
private int maxiteration=5000;//max iteration times allowed

	/**
	 * apply nonlinearity on each entry of a vector
	 * @param y
	 * a complex vector
	 * @param result
	 * destination for results
	 * @return
	 */
	private Complex[] g(Complex[] y,Complex[] result)
	{
		if(result==null) result=new Complex[y.length];
		else BLAS.checkDestinationSize(result,y.length);
		
		for(int i=0;i<result.length;i++) result[i]=nonlinearity.g(y[i]);
		
		return result;
	}
	
	/**
	 * calculate the norm of two complex matrices
	 * @param w
	 * a complex matrix
	 * @param w1
	 * another complex matrix
	 * @return
	 */
	private double norm(Complex[][] w,Complex[][] w1)
	{
	double val,minval=Double.MAX_VALUE;	

		BLAS.checkSize(w,w1);
		
		for(int i=0;i<w.length;i++) 
			for(int j=0;j<w[i].length;j++) 
			{
				val=Math.abs(w[i][j].abs()-w1[i][j].abs());
				if(val<minval) minval=val;
			}
		
		return minval;
	}

	/**
	 * calculate the demixing matrix for centered and whitened signals with specified 
	 * initial seeds
	 * @param sigs
	 * already centered and whitened signals
	 * @param seed
	 * initial seeds
	 * @return
	 */
	public Complex[][] demixingMatrixPreprocessed(Complex[][] sigs,Complex[][] seed)
	{
	Complex[][] w,w1=null,tempg,temp1=null,temp2=null,swap;
	Complex[] y,fy=null;
	
		if(seed.length!=sigs.length||seed[0].length!=sigs.length) throw new IllegalArgumentException(
				"illegal seeds size: "+seed.length+" x "+seed[0].length+", required: "+sigs.length+" x "+sigs.length);

		w=seed;
		w1=new Complex[w.length][w[0].length];
		tempg=new Complex[w.length][w[0].length];
		y=new Complex[sigs.length];
		
		for(int it=1;it<=maxiteration;it++)
		{
			/*
			 * calculate G
			 */
			BLAS.fill(tempg,Complex.ZERO);
			for(int j=0;j<sigs[0].length;j++)
			{
				/*
				 * the estimated signals
				 */
				Arrays.fill(y,Complex.ZERO);
				for(int i=0;i<w.length;i++) 
					for(int k=0;k<sigs.length;k++) y[i]=y[i].add(w[i][k].multiply(sigs[k][j]));
				
				//perform nonlinearity transform
				fy=g(y,fy);
				
				//accumulate the outer product
				for(int ii=0;ii<tempg.length;ii++) 
					for(int jj=0;jj<tempg[ii].length;jj++) 
						tempg[ii][jj]=tempg[ii][jj].add(fy[ii].multiply(y[jj].conjugate()));
			}
			BLAS.scalarMultiply(1.0/sigs[0].length,tempg,tempg);

			/*
			 * the new demixing matrix
			 */
			temp1=BLAS.scalarMultiply(1+eta,w,temp1);
			temp2=BLAS.multiply(tempg,w,temp2);
			temp2=BLAS.scalarMultiply(eta,temp2,temp2);
			w1=BLAS.substract(temp1,temp2,w1);
			
			/*
			 * check convergence
			 */
//			System.out.println("iteration "+it+", norm="+norm(w,w1));
			
			if(norm(w,w1)<=epsilon) return w;
			else
			{
				swap=w;
				w=w1;
				w1=swap;
			}
		}

		/*
		 * sometimes a good result still can be given even if max iteration times reached
		 */
		throw new AlgorithmNotConvergeException("max iteration times exceeded: "+maxiteration);
//		return w;
	}
	
	/**
	 * calculate the demixing matrix for centered and whitened signals with 
	 * random initial vectors
	 * @param sigs
	 * already centered and whitened signals
	 * @return
	 */
	public Complex[][] demixingMatrixPreprocessed(Complex[][] sigs)
	{
	Complex[][] seed;
	
		seed=BLAS.randComplexMatrix(sigs.length,sigs.length);
		seed=BLAS.scalarMultiply(0.01,seed,seed);
		BLAS.orthogonalize(seed);
		return demixingMatrixPreprocessed(sigs,seed);
	}
	
	/**
	 * rescale a demixing matrix to overcome the scaling ambiguity
	 * @param demix
	 * a demixing matrix for original signals
	 */
	public void rescale(Complex[][] demix)
	{
	Complex[][] pseudoinv=null;
				
		pseudoinv=BLAS.pinv(demix,pseudoinv);//calculate pseudo inverse
		for(int i=0;i<demix.length;i++)
			for(int j=0;j<demix[i].length;j++) 
				//apply pseudo inverse's diagonal elements as scale
				demix[i][j]=demix[i][j].multiply(pseudoinv[i][i]);
	}
	
	/**
	 * calculate the demixing matrix for origin input signals
	 * @param sigs
	 * Input signals, each row is a channel, signal values will be 
	 * modified because of the centering operation.
	 * @param pcath
	 * threshold for PAC
	 * @return
	 * Demixing matrix for input signals, the scale problem also considered.
	 */
	public Complex[][] demixingMatrix(Complex[][] sigs,double pcath)
	{
	Whitening preprocessor;
	Complex[][] whitening;//whitening matrix
	Complex[][] sigsp;//whitened signals
	Complex[][] demixp;//demixing matrix for preprocessed signals
	Complex[][] demix;//demixing matrix for original signals
	
		/*
		 * preprocessing
		 */
		preprocessor=new Whitening();
		sigsp=preprocessor.preprocess(sigs,pcath);
		whitening=preprocessor.transferMatrix();
		
		//calculate the demixing matrix for preprocessed signals
		demixp=demixingMatrixPreprocessed(sigsp);
		
		demix=BLAS.multiply(demixp,whitening,null);//demixing matrix for the observed signal
		rescale(demix);//solve the scale ambiguity
		return demix;
	}
	
	/**
	 * calculate the demixing matrix for origin input signals
	 * @param sigs
	 * Input signals, each row is a channel, signal values will be 
	 * modified because of the centering operation.
	 * @param seed
	 * Initial seeds for demixing vectors, each row is a seed. Number of 
	 * output channels will be decided by the number of seeds.
	 * @return
	 * Demixing matrix for input signals, the scale problem also considered.
	 */
	public Complex[][] demixingMatrix(Complex[][] sigs,Complex[][] seed)
	{
	Whitening preprocessor;
	Complex[][] whitening;//whitening matrix
	Complex[][] sigsp;//whitened signals
	Complex[][] demixp;//demixing matrix for preprocessed signals
	Complex[][] demix;//demixing matrix for input signals
	
		/*
		 * preprocessing
		 */
		preprocessor=new Whitening();
		sigsp=preprocessor.preprocess(sigs,seed.length);
		whitening=preprocessor.transferMatrix();
		
		//calculate the demixing matrix for preprocessed signals
		demixp=demixingMatrixPreprocessed(sigsp,seed);
		
		demix=BLAS.multiply(demixp,whitening,null);//demixing matrix for the observed signal
		rescale(demix);//solve the scale ambiguity
		return demix;
	}
	
	public ICAResult icaPreprocessed(Complex[][] sigsp,Complex[][] seeds)
	{
	ICAResult icares;
	
		icares=new ICAResult();
		icares.setDemixingMatrixPreprocessed(
				demixingMatrixPreprocessed(sigsp,seeds));
		return icares;
	}
	
	public ICAResult ica(Complex[][] sigs)
	{
	ICAResult icares;
	Whitening preprocessor;
	Complex[][] sigsp;
	
		icares=new ICAResult();
		
		/*
		 * preprocessing
		 */
		preprocessor=new Whitening();
		sigsp=preprocessor.preprocess(sigs,pcath);
		icares.setSignalMeans(preprocessor.signalMeans());
		icares.setWhiteningMatrix(preprocessor.transferMatrix());

		/*
		 * perform ica
		 */
		icares.setDemixingMatrixPreprocessed(demixingMatrixPreprocessed(sigsp));
		
		//demixing matrix for original input
		icares.setDemixingMatrix(BLAS.multiply(
				icares.getDemixingMatrixPreprocessed(),
				icares.getWhiteningMatrix(),
				null));
		
		//estimated sources
		icares.setEstimatedSignals(BLAS.multiply(icares.getDemixingMatrix(),sigs,null));
		
		return icares;
	}

	public static void main(String[] args) throws IOException
	{
	double[][] rs,rx,mix,ry;
	Complex[][]	cx,cy;
	ICA app;
	ICAResult icares;
	
		/*
		 * generate input signals
		 */
		rs=Util.loadSignals(new File("data/demosig.txt"),Util.Dimension.COLUMN);
		
		mix=BLAS.randMatrix(rs.length,rs.length);
		rx=BLAS.multiply(mix,rs,null);
	
		cx=new Complex[rx.length][];
		for(int i=0;i<cx.length;i++) cx[i]=SpectralAnalyzer.fft(rx[i]);
		
		/*
		 * apply ica
		 */	
		app=new InfomaxICA();
		icares=app.ica(cx);
		cy=icares.getEstimatedSignals();
		
		/*
		 * transform back to real signal
		 */
		ry=new double[cy.length][];
		for(int i=0;i<ry.length;i++) ry[i]=SpectralAnalyzer.ifftReal(cy[i]);
		
		Util.plotSignals(ry);
	}
}
