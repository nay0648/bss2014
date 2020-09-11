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
 * Fast ICA for noncircular complex data, see: Mike Novey, Tulay Adah, 
 * Complex ICA by Negentropy Maximization, IEEE Trans. Neural Networks, 
 * vol. 19, no. 4, 2008.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Feb 22, 2012 10:39:57 AM, revision:
 */
public class NCFastICA2 extends ICA implements Serializable
{
private static final long serialVersionUID=-3690770278157928949L;

	/**
	 * <h1>Description</h1>
	 * The asinh nonlinearity function.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Feb 22, 2012 10:41:31 AM, revision:
	 */
	public static class Asinh implements CNonlinearity
	{
	private static final long serialVersionUID=-4909770899493920345L;

		public Complex g(Complex u)
		{
			return u.multiply(u).add(Complex.ONE).sqrt().add(u).log();
		}
		
		public Complex dg(Complex u)
		{
			return Complex.ONE.divide(u.multiply(u).add(Complex.ONE).sqrt());	
		}

		public Complex ddg(Complex u)
		{
			return u.negate().multiply(u.multiply(u).add(Complex.ONE).pow(new Complex(-3.0/2.0,0)));
		}
	}
	
	/**
	 * <h1>Description</h1>
	 * The cosh nonlinearity function.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Feb 22, 2012 11:27:16 AM, revision:
	 */
	public static class Cosh implements CNonlinearity
	{
	private static final long serialVersionUID=-5404000147883797808L;

		public Complex g(Complex u)
		{
			return u.exp().add(u.negate().exp()).divide(new Complex(2,0));
		}
		
		public Complex dg(Complex u)
		{
			return u.exp().subtract(u.negate().exp()).divide(new Complex(2,0));
		}

		public Complex ddg(Complex u)
		{
			return g(u);
		}
	}
	
	/**
	 * <h1>Description</h1>
	 * The z1.25 nonlinearity function.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Feb 22, 2012 11:28:11 AM, revision:
	 */
	public static class Z125 implements CNonlinearity
	{
	private static final long serialVersionUID=-1824539672239936519L;

		public Complex g(Complex u)
		{
			return u.pow(new Complex(1.25,0));
		}
		
		public Complex dg(Complex u)
		{
			return new Complex(1.25,0).multiply(u.pow(new Complex(0.25,0)));
		}

		public Complex ddg(Complex u)
		{
			return new Complex(1.25*0.25,0).multiply(u.pow(new Complex(-0.75,0)));
		}
	}

/*
 * the nonlinearity funciton used
 */
private static Map<String,CNonlinearity> NONLINEARITY=new HashMap<String,CNonlinearity>();

	static
	{
		NONLINEARITY.put("asinh",new Asinh());
		NONLINEARITY.put("cosh",new Cosh());
		NONLINEARITY.put("z1.25",new Z125());
	}

private CNonlinearity nonlinearity=NONLINEARITY.get("asinh");//the nonlinearity function
private double pcath=1e-6;//threshold used in PCA
private double epsilon=1e-15;//convergence threshold of the fast ica algorithm
private int maxiteration=1000;//max iteration times allowed
private Decorrelation decorrelation=Decorrelation.deflation;//decorrelation policy

	/**
	 * <h1>Description</h1>
	 * Decorrelation policy used in ICA.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Mar 31, 2011 9:22:14 AM, revision:
	 */
	public enum Decorrelation
	{
		/**
		 * Estimate demixing vectors one by one.
		 */
		deflation,
		/**
		 * Estimate demixing vectors simultaneously.
		 */
		symmetric
	}
	
	/**
	 * the objective function of this algorithm
	 * @param sigs
	 * already preprocessed signals
	 * @param w
	 * a demixing vector
	 * @return
	 */
	public double objectiveFunction(Complex[][] sigs,Complex[] w)
	{
	Complex u;
	double e=0;
	
		for(int j=0;j<sigs[0].length;j++)
		{
			u=new Complex(0,0);
			for(int i=0;i<w.length;i++) u=u.add(w[i].conjugate().multiply(sigs[i][j]));
			
			e+=BLAS.absSquare(nonlinearity.g(u));	
		}

		return e/sigs[0].length;
	}
	
	/**
	 * perform one step iteration for demixing vector calculation
	 * @param sigs
	 * already centered and whitened signals
	 * @param w
	 * old demixing vector
	 * @param w1
	 * space for new demixing vector
	 */
	private void oneStepIteration(Complex[][] sigs,Complex[] w,Complex[] w1)
	{
	Complex e2,e3,y,temp;
	Complex[][] pcov;//the pseudo covariance matrix
		
		/*
		 * calculate the expectation
		 */
		Arrays.fill(w1,Complex.ZERO);
		e2=Complex.ZERO;
		e3=Complex.ZERO;
		pcov=new Complex[sigs.length][sigs.length];
		for(int i=0;i<pcov.length;i++) Arrays.fill(pcov[i],Complex.ZERO);

		for(int j=0;j<sigs[0].length;j++)
		{
			/*
			 * calculate the inner product: y=w^Hx
			 */
			y=Complex.ZERO;
			for(int i=0;i<w.length;i++) y=y.add(w[i].conjugate().multiply(sigs[i][j]));
			
			/*
			 * the 1st E
			 */
			temp=nonlinearity.g(y).conjugate().multiply(nonlinearity.dg(y));
			for(int i=0;i<w1.length;i++) w1[i]=w1[i].add(temp.multiply(sigs[i][j]));
			
			//the 2nd E
			e2=e2.add(nonlinearity.dg(y).multiply(nonlinearity.dg(y).conjugate()));
			
			//the 3rd E
			e3=e3.add(nonlinearity.g(y).conjugate().multiply(nonlinearity.ddg(y)));
			
			//the pseudo covariance matrix
			for(int ii=0;ii<pcov.length;ii++) 
				for(int jj=0;jj<pcov[ii].length;jj++) 
					pcov[ii][jj]=pcov[ii][jj].add(sigs[ii][j].multiply(sigs[jj][j]));
		}
		
		BLAS.scalarMultiply(-1.0/sigs[0].length,w1,w1);
		e2=e2.multiply(1.0/sigs[0].length);
		e3=e3.multiply(1.0/sigs[0].length);
		BLAS.scalarMultiply(1.0/sigs[0].length,pcov,pcov);
		
		/*
		 * the new direction
		 */
		for(int i=0;i<w1.length;i++) w1[i]=w1[i].add(w[i].multiply(e2));
		
		for(int i=0;i<w1.length;i++) 
		{
			temp=Complex.ZERO;
			for(int k=0;k<w.length;k++) 
				temp=temp.add(pcov[i][k].multiply(e3.multiply(w[k].conjugate())));
			
			w1[i]=w1[i].add(temp);
		}
	}

	/**
	 * calculate the demixing matrix for centered and whitened signals, deflation 
	 * policy is used to calculate demixing vectors one by one
	 * @param sigs
	 * already centered and whitened signals
	 * @param seed
	 * Initial seeds for demixing vectors, each row is a seed.
	 * @return
	 */
	private Complex[][] demixingMatrixDeflation(Complex[][] sigs,Complex[][] seed)
	{
	Complex[][] demix;
	Complex[] w,w1=null,temp=null;//the estimated direction

		demix=new Complex[sigs.length][];
		w1=new Complex[sigs.length];
		
		//calculate demixing vectors one by one
		for(int found=0;found<demix.length;found++)
		{	
			/*
			 * get a seed as initial vector
			 */
			w=new Complex[sigs.length];
			for(int j=0;j<w.length;j++) w[j]=seed[found][j];
			BLAS.normalize(w);
			
			//search for the direction iteratively
			for(int numit=1;;numit++)
			{
				if(numit>maxiteration) throw new AlgorithmNotConvergeException(
						"max iteration times reached: "+maxiteration);
				
				oneStepIteration(sigs,w,w1);//perform one step iteration
				
				/*
				 * substract previous directions
				 */
				for(int i=0;i<found;i++)
				{
					//<a,b>=<b,a>* in complex domain
					temp=BLAS.scalarMultiply(BLAS.innerProduct(demix[i],w1),demix[i],temp);
					w1=BLAS.substract(w1,temp,w1);
				}
				BLAS.normalize(w1);

				//to see if the direction converges
				if(Math.abs(1-BLAS.innerProduct(w,w1).abs())<=epsilon)
				{
					demix[found]=w;
					break;
				}
				
				//not converge, continue the iteration
				for(int i=0;i<w.length;i++) w[i]=w1[i];
			}			
		}
		
		//row vectors will be used, so complex conjugate is required
		return BLAS.conjugate(demix,demix);
	}
	
	/**
	 * calculate the demixing matrix for centered and whitened signals with 
	 * symmetric decorrelation policy
	 * @param sigs
	 * already centered and whitened signals
	 * @param seed
	 * Initial seeds for demixing vectors, each row is a seed.
	 * @return
	 */
	private Complex[][] demixingMatrixSymmetric(Complex[][] sigs,Complex[][] seed)
	{
	HermitianEigensolver eigensolver;
	Complex[][] demix;//demixing matrix for already preprocessed signals
	Complex[][] demix1;//for new direction
	Complex[][] demix1h=null;//transpose
	HermitianEigensolver.EigenDecomposition decomp;
	Complex[][] edm;//the diagonal matrix for eigenvalues
	Complex[][] ev;//eigenvectors
	Complex[][] evh=null;//transpose of the eigenvectors
	double cos,mincos;
	Complex[][] swap;
	
		eigensolver=new CommonsEigensolver();
	
		/*
		 * the initial demixing matrix
		 */
		demix=BLAS.copy(seed,null);
		BLAS.orthogonalize(demix);
		
		demix1=new Complex[demix.length][demix[0].length];
		edm=new Complex[demix.length][demix[0].length];
		BLAS.fill(edm,new Complex(0,0));
		
		//search for the direction iteratively
		for(int numit=1;;numit++)
		{
			if(numit>maxiteration) throw new AlgorithmNotConvergeException(
					"max iteration times reached: "+maxiteration);
			
			//perform one step iteration for all demixing vectors
			for(int i=0;i<demix.length;i++) oneStepIteration(sigs,demix[i],demix1[i]);
			
			/*
			 * Decorrelation with symmetric policy. Here is not the same as the paper, 
			 * because row vectors are used in the program. The paper says: W=(W*W')^(-0.5)*W, 
			 * here we use: X=X*(X'*X)^(-0.5), where X=W'.
			 */
			demix1h=BLAS.transpose(demix1,demix1h);
			//X'*X, is a Hermitian matrix
			demix1h=BLAS.multiply(demix1h,demix1,demix1h);
			/*
			 * (X'*X)^(-0.5)
			 */
			decomp=eigensolver.eig(demix1h);
			//construct D^(-0.5)
			for(int i=0;i<edm.length;i++) 
				//Hermitian matrix has only real eigenvalues
				edm[i][i]=new Complex(1.0/Math.sqrt(decomp.eigenvalue(i).getReal()),0);
			
			ev=decomp.eigenvectors();
			demix1h=BLAS.multiply(ev,edm,demix1h);
			evh=BLAS.transpose(ev,evh);
			demix1h=BLAS.multiply(demix1h,evh,demix1h);

			//X*(X'*X)^(-0.5);
			demix1=BLAS.multiply(demix1,demix1h,demix1);
			
			/*
			 * to see if the direction converges
			 */
			mincos=1;
			for(int i=0;i<demix.length;i++)
			{
				cos=BLAS.innerProduct(demix[i],demix1[i]).abs();
				if(cos<mincos) mincos=cos;
			}
			if(Math.abs(1-mincos)<=epsilon) break;
			
			/*
			 * not converge, continue the iteration
			 */
			swap=demix1;
			demix1=demix;
			demix=swap;
		}
		
		//row vectors will be used, so complex conjugate is required
		return BLAS.conjugate(demix,demix);
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
		if(seed.length!=sigs.length||seed[0].length!=sigs.length) throw new IllegalArgumentException(
				"illegal seeds size: "+seed.length+" x "+seed[0].length+", required: "+sigs.length+" x "+sigs.length);
		switch(decorrelation)
		{
			case deflation:
				return demixingMatrixDeflation(sigs,seed);
			case symmetric:
				return demixingMatrixSymmetric(sigs,seed);
			default: throw new IllegalArgumentException(
					"unknown decorrelation policy: "+decorrelation);
		}
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
	NCFastICA2 app;
	ICAResult icares;
	
		/*
		 * generate input signals
		 */
		rs=Util.loadSignals(new File("data/demosig.txt"),Util.Dimension.COLUMN);
		mix=BLAS.randMatrix(4,4);
		rx=BLAS.multiply(mix,rs,null);
	
		cx=new Complex[rx.length][];
		for(int i=0;i<cx.length;i++) cx[i]=SpectralAnalyzer.fft(rx[i]);
		
		/*
		 * apply ica
		 */	
		app=new NCFastICA2();
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
