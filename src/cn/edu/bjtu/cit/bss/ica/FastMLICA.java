package cn.edu.bjtu.cit.bss.ica;
import java.io.*;
import java.util.*;
import org.apache.commons.math.complex.*;
import pp.util.BLAS;
//import cn.edu.bjtu.cit.bss.*;
import cn.edu.bjtu.cit.bss.preprocess.*;
import cn.edu.bjtu.cit.bss.util.*;

/**
 * <h1>Description</h1>
 * Fast maximul likelihood ICA, see: Aapo Hyvarinen, Juha Karhunen, Erkki Oja, 
 * Independent Component Analysis, chapter 9.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Jun 26, 2012 4:20:43 PM, revision:
 */
public class FastMLICA extends ICA
{
private static final long serialVersionUID=-7231852201303169714L;
private static final double EPS=2.220446049250313e-16;//a small positive number

	/**
	 * <h1>Description</h1>
	 * The tanh nonlinearity.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Jun 26, 2012 4:28:14 PM, revision:
	 */
	private static class STanh implements CNonlinearity
	{
	private static final long serialVersionUID=-6758996516595042527L;

		public Complex g(Complex u)
		{
			throw new UnsupportedOperationException();
		}

		public Complex dg(Complex u)
		{
			return new Complex(Math.tanh(u.getReal()),Math.tanh(u.getImaginary()));
		}

		public Complex ddg(Complex u)
		{
		double real,imag,tanh;
			
			tanh=Math.tanh(u.getReal());
			real=1-tanh*tanh;
			
			tanh=Math.tanh(u.getImaginary());
			imag=1-tanh*tanh;
			
			return new Complex(real,imag);			
		}
	}
	
	/**
	 * <h1>Description</h1>
	 * The spherical symmetric Laplacian source prior.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Jun 27, 2012 4:21:09 PM, revision:
	 */
	private static class SSL implements CNonlinearity
	{
	private static final long serialVersionUID=-8104267609318902051L;

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
			return u.multiply(-1.0/Math.sqrt(BLAS.absSquare(u)+EPS));
		}
		
		public Complex ddg(Complex u)
		{
		double abs2;
		Complex temp1,temp2;
		
			abs2=BLAS.absSquare(u)+EPS;
			
			temp1=u.multiply(u).multiply(Math.pow(abs2,-1.5));
			temp2=new Complex(Math.pow(abs2,-0.5),0);

			return temp1.subtract(temp2);
		}
	}

private static final Map<String,CNonlinearity> NONLINEARITY_MAP=new HashMap<String,CNonlinearity>();

	static
	{
		NONLINEARITY_MAP.put("stanh",new STanh());
		NONLINEARITY_MAP.put("ssl",new SSL());
	}
	
private int maxiteration=1000;//maximum iterations
private CNonlinearity nonlinearity=NONLINEARITY_MAP.get("ssl");//the nonlinearity
private double pcath=1e-6;//threshold for pca
private double stopthres=1e-10;//stop threshold

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
	Complex[][] wp,wp1,swap;
	Complex[] y;//estimated signals
	Complex[] phiy;//after nonlinearity transform
	Complex[] alpha,beta;
	double obj;
	
		if(seeds.length!=xdata.length||seeds[0].length!=xdata.length) throw new IllegalArgumentException(
				"seeds size not match: "+seeds.length+" x "+seeds[0].length+", "+xdata.length+" x "+xdata.length);
	
		wp=BLAS.copy(seeds,null);
		y=new Complex[xdata.length];
		phiy=new Complex[xdata.length];
		alpha=new Complex[xdata.length];
		beta=new Complex[xdata.length];
		wp1=new Complex[xdata.length][xdata.length];
		
		for(int itcount=0;itcount<maxiteration;itcount++) 
		{
			Arrays.fill(alpha,Complex.ZERO);
			Arrays.fill(beta,Complex.ZERO);
			BLAS.fill(wp1,Complex.ZERO);
			
			//traverse the dataset
			for(int tau=0;tau<xdata[0].length;tau++) 
			{
				/*
				 * get separated signals
				 */
				Arrays.fill(y,Complex.ZERO);
				for(int sourcei=0;sourcei<y.length;sourcei++) 
					for(int j=0;j<y.length;j++) 
						y[sourcei]=y[sourcei].add(wp[sourcei][j].multiply(xdata[j][tau]));

				//perform nonlinearity transform
				for(int sourcei=0;sourcei<y.length;sourcei++) phiy[sourcei]=nonlinearity.dg(y[sourcei]);
								
				for(int sourcei=0;sourcei<xdata.length;sourcei++) 
				{
					beta[sourcei]=beta[sourcei].add(y[sourcei].multiply(phiy[sourcei]));
					alpha[sourcei]=alpha[sourcei].add(nonlinearity.ddg(y[sourcei]));
				}

				//accumulate outer product
				for(int ii=0;ii<y.length;ii++) 
					for(int jj=0;jj<y.length;jj++) 
						wp1[ii][jj]=wp1[ii][jj].add(phiy[ii].multiply(y[jj].conjugate()));
			}
			
			/*
			 * the expectations
			 */
			BLAS.scalarMultiply(-1.0/xdata[0].length,beta,beta);
			
			BLAS.scalarMultiply(1.0/xdata[0].length,alpha,alpha);
			for(int sourcei=0;sourcei<alpha.length;sourcei++) 
			{
			double real,imag,abs2;
			
				real=beta[sourcei].getReal()+alpha[sourcei].getReal();
				imag=beta[sourcei].getImaginary()+alpha[sourcei].getImaginary();
				abs2=real*real+imag*imag;
				
				alpha[sourcei]=new Complex(-real/abs2,imag/abs2);
			}
			
			BLAS.scalarMultiply(1.0/xdata[0].length,wp1,wp1);
			
			/*
			 * the new demixing matrix
			 */
			for(int sourcei=0;sourcei<wp1.length;sourcei++) 
				wp1[sourcei][sourcei]=wp1[sourcei][sourcei].add(beta[sourcei]);
			
			for(int sourcei=0;sourcei<wp1.length;sourcei++) 
				for(int sensorj=0;sensorj<wp1[sourcei].length;sensorj++) 
					wp1[sourcei][sensorj]=wp1[sourcei][sensorj].multiply(alpha[sourcei]);
			
			BLAS.multiply(wp1,wp,wp1);
			BLAS.add(wp,wp1,wp1);
			
			symmDecorrelation(wp1);//perform symmetric decorrelation
			
			/*
			 * see if it converges
			 */
			obj=0;
			for(int sourcei=0;sourcei<wp.length;sourcei++) 
				obj+=BLAS.innerProduct(wp[sourcei],wp1[sourcei]).abs();
			obj=Math.abs(1-obj/wp.length);

//			System.out.println("iteration "+itcount+", obj="+obj);
			
			if(obj<=stopthres) return wp;
			
			swap=wp;
			wp=wp1;
			wp1=swap;
		}

//		throw new AlgorithmNotConvergeException("max iteration times exceeded: "+maxiteration);
		return wp;
	}

	/**
	 * symmetric decorrelation
	 * @param wp
	 * demixing matrix
	 */
	private void symmDecorrelation(Complex[][] wp)
	{
	Complex[][] wh=null,edm,ev,evh=null;
	HermitianEigensolver eigensolver;
	HermitianEigensolver.EigenDecomposition decomp;
	
		/*
		 * W*W'
		 */
		wh=BLAS.transpose(wp,wh);
		wh=BLAS.multiply(wp,wh,wh);
			
		/*
		 * (W*W')^-0.5
		 */
		eigensolver=new CommonsEigensolver();
		decomp=eigensolver.eig(wh);
			
		//construct D^(-0.5)
		edm=BLAS.eyeComplex(wp.length,wp[0].length);
		for(int i=0;i<edm.length;i++) 
			//Hermitian matrix has only real eigenvalues
			edm[i][i]=new Complex(1.0/Math.sqrt(decomp.eigenvalue(i).getReal()),0);
			
		ev=decomp.eigenvectors();
		wh=BLAS.multiply(ev,edm,wh);
		evh=BLAS.transpose(ev,evh);
		wh=BLAS.multiply(wh,evh,wh);

		//(W*W')^-0.5*W
		wp=BLAS.multiply(wh,wp,wp);		
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
	FastMLICA foo;
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
		foo=new FastMLICA();
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
