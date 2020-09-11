package cn.edu.bjtu.cit.bss.ica;
import java.io.*;
import java.util.*;
import org.apache.commons.math.complex.*;
//import cn.edu.bjtu.cit.bss.*;
import cn.edu.bjtu.cit.bss.preprocess.*;
import cn.edu.bjtu.cit.bss.util.*;
import pp.util.BLAS;

/**
 * <h1>Description</h1>
 * Maximul likelihood ICA, see: Aapo Hyvarinen, Juha Karhunen, Erkki Oja, 
 * Independent Component Analysis, chapter 9.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Jun 21, 2012 9:24:35 AM, revision:
 */
public class MLICA extends ICA
{
private static final long serialVersionUID=-3909065307775363998L;

	/**
	 * <h1>Description</h1>
	 * The spherically symmetric Laplacian nonlinearity.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Jun 21, 2012 9:28:33 AM, revision:
	 */
	private static class SSL implements CNonlinearity
	{
	private static final long serialVersionUID=-6038285862367919375L;

		/**
		 * log(p(y))
		 */
		public Complex g(Complex u)
		{
			return new Complex(-Math.sqrt(BLAS.absSquare(u)),0);
		}
		
		/**
		 * dlog(p(y))/dy
		 */
		public Complex dg(Complex u)
		{
			return u.multiply(-1.0/Math.sqrt(BLAS.absSquare(u)));
		}
		
		public Complex ddg(Complex u)
		{
			throw new UnsupportedOperationException();
		}
	}
	
	/**
	 * <h1>Description</h1>
	 * Nonlinearity for super Gaussian pdf.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Jun 21, 2012 3:07:23 PM, revision:
	 */
	private static class LogPP implements CNonlinearity
	{
	private static final long serialVersionUID=-540649225760356606L;
	private double alpha1=1;

		public Complex g(Complex u)
		{
		double real,imag;
		
			real=alpha1-2*Math.log(Math.cosh(u.getReal()));
			imag=-2*Math.log(Math.cosh(u.getImaginary()));
			
			return new Complex(real,imag);
		}
		
		public Complex dg(Complex u)
		{
		double real,imag;
		
			real=-2*Math.tanh(u.getReal());
			imag=-2*Math.tanh(u.getImaginary());

			return new Complex(real,imag);
		}
	
		public Complex ddg(Complex u)
		{
		double tanh,real,imag;
		
			tanh=Math.tanh(u.getReal());
			real=2*(tanh*tanh-1);
			
			tanh=Math.tanh(u.getImaginary());
			imag=2*(tanh*tanh-1);
			
			return new Complex(real,imag);
		}
	}
	
	/**
	 * <h1>Description</h1>
	 * Nonlinearity for sub Gaussian pdf.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Jun 21, 2012 6:40:30 PM, revision:
	 */
	private static class LogPN implements CNonlinearity
	{
	private static final long serialVersionUID=5260724486203850453L;
	private double alpha2=1;

		public Complex g(Complex u)
		{
		double real,imag;
		
			real=alpha2-(u.getReal()*u.getReal()/2-Math.log(Math.cosh(u.getReal())));
			imag=-(u.getImaginary()*u.getImaginary()/2-Math.log(Math.cosh(u.getImaginary())));
			
			return new Complex(real,imag);
		}
		
		public Complex dg(Complex u)
		{
		double real,imag;
		
			real=Math.tanh(u.getReal())-u.getReal();
			imag=Math.tanh(u.getImaginary())-u.getImaginary();
			
			return new Complex(real,imag);
		}	
	
		public Complex ddg(Complex u)
		{
		double tanh,real,imag;
		
			tanh=Math.tanh(u.getReal());
			real=-tanh*tanh;
			
			tanh=Math.tanh(u.getImaginary());
			imag=-tanh*tanh;
			
			return new Complex(real,imag);
		}
	}

private static final Map<String,CNonlinearity> NONLINEARITY_MAP=new HashMap<String,CNonlinearity>();

	static
	{
		NONLINEARITY_MAP.put("ssl",new SSL());
		NONLINEARITY_MAP.put("logpp",new LogPP());
		NONLINEARITY_MAP.put("logpn",new LogPN());
	}

private int maxiteration=5000;//maximum iterations
private CNonlinearity phi=NONLINEARITY_MAP.get("ssl");//the nonlinearity
private double eta=0.01;//the learning rate
private double pcath=1e-6;//threshold for pca
private double stopthres=1e-8;//stop threshold

	/**
	 * calculate demixing matrix for already preprocessed
	 * @param xdata
	 * each row is a channel
	 * @param seeds
	 * initial seeds
	 * @return
	 */
	private Complex[][] demixingMatrixPreprocessed(Complex[][] xdata,Complex[][] seeds)
	{
	Complex[][] wp=null;
	Complex[][] dwp;//the gradient
	Complex[] y;//estimated signals
	Complex[] phiy;//after nonlinearity transform
	double obj,pobj=Double.MIN_VALUE,dobj;
		
		if(seeds.length!=xdata.length||seeds[0].length!=xdata.length) throw new IllegalArgumentException(
				"seeds size not match: "+seeds.length+" x "+seeds[0].length+", "+xdata.length+" x "+xdata.length);

		wp=BLAS.copy(seeds,wp);
		y=new Complex[xdata.length];
		phiy=new Complex[xdata.length];
		dwp=new Complex[xdata.length][xdata.length];

		for(int itcount=0;itcount<maxiteration;itcount++) 
		{
			/*
			 * traverse the dataset
			 */
			obj=0;
			BLAS.fill(dwp,Complex.ZERO);
			
			for(int tau=0;tau<xdata[0].length;tau++) 
			{
				/*
				 * get separated signals
				 */
				Arrays.fill(y,Complex.ZERO);
				for(int sourcei=0;sourcei<y.length;sourcei++) 
					for(int j=0;j<y.length;j++) 
						y[sourcei]=y[sourcei].add(wp[sourcei][j].multiply(xdata[j][tau]));
						
				//accumulate objective function
				for(int sourcei=0;sourcei<y.length;sourcei++) 
				{
				Complex l;
				
					l=phi.g(y[sourcei]);
					obj+=l.getReal()+l.getImaginary();
				}
				
				//perform nonlinearity transform
				for(int sourcei=0;sourcei<y.length;sourcei++) phiy[sourcei]=phi.dg(y[sourcei]);
				
				//accumulate outer product
				for(int ii=0;ii<y.length;ii++) 
					for(int jj=0;jj<y.length;jj++) 
						dwp[ii][jj]=dwp[ii][jj].add(phiy[ii].multiply(y[jj].conjugate()));
			}
			
			/*
			 * calculate the gradient
			 */
			for(int ii=0;ii<dwp.length;ii++) 
				for(int jj=0;jj<dwp[ii].length;jj++) 
				{
					dwp[ii][jj]=dwp[ii][jj].multiply(1.0/xdata[0].length);
					if(ii==jj) dwp[ii][jj]=dwp[ii][jj].add(Complex.ONE);
				}
			BLAS.multiply(dwp,wp,dwp);
			
			/*
			 * calculate the objective function
			 */
			obj/=xdata[0].length;
			obj+=Math.log(BLAS.det(wp).abs());
			
			/*
			 * update demixing matrix
			 */
			BLAS.scalarMultiply(eta,dwp,dwp);
			BLAS.add(wp,dwp,wp);

			/*
			 * see if the algorithm converges
			 */
			dobj=pobj-obj;
			pobj=obj;
			
//			System.out.println("iteration "+itcount+", obj (asc)="+obj+", dobj="+dobj);
				
			if(Math.abs(dobj)/Math.abs(obj)<=stopthres) return wp;
		}
		
//		throw new AlgorithmNotConvergeException("max iteration times exceeded: "+maxiteration);
		return wp;
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
	Complex[][] seeds;
		
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
		seeds=BLAS.eyeComplex(sigsp.length,sigsp.length);
		icares.setDemixingMatrixPreprocessed(demixingMatrixPreprocessed(sigsp,seeds));
			
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
	double[][] rs,ry;
	Complex[][]	cs,cx,cy;
	MLICA foo;
	ICAResult icares;
	
		/*
		 * generate input signals
		 */
		rs=Util.loadSignals(new File("data/demosig.txt"),Util.Dimension.COLUMN);
		cs=new Complex[rs.length][];
		for(int i=0;i<cs.length;i++) cs[i]=SpectralAnalyzer.fft(rs[i]);
		
		cx=BLAS.multiply(BLAS.randComplexMatrix(cs.length,cs.length),cs,null);
		
		/*
		 * apply ica
		 */	
		foo=new MLICA();
		icares=foo.ica(cx);
		cy=icares.getEstimatedSignals();
		
		/*
		 * transform back to real signal
		 */
		ry=new double[cy.length][];
		for(int i=0;i<ry.length;i++) ry[i]=SpectralAnalyzer.ifftReal(cy[i]);
		
		Util.plotSignals(ry);
	}
}
