package cn.edu.bjtu.cit.bss.preprocess;
import java.util.*;
import org.apache.commons.math.complex.*;
import cn.edu.bjtu.cit.bss.util.*;
import pp.util.BLAS;

/**
 * <h1>Description</h1>
 * Perform dimensionality reduction by PCA.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Oct 28, 2011 9:34:28 AM, revision:
 */
public class PCA extends Preprocessor
{
private static final long serialVersionUID=-2875404070536455426L;
private HermitianEigensolver eigensolver=new CommonsEigensolver();
	
	/**
	 * build transfer matrix for PCA from covariance eigenvectors
	 * @param container
	 * selected eigencontainers of the covariance matrix used to build new the feature space
	 * @return
	 */
	public Complex[][] calculateTransferMatrix(HermitianEigensolver.EigenContainer... container)
	{
	Complex[][] fm;
	
		fm=new Complex[container.length][];
		for(int i=0;i<fm.length;i++) 
			/*
			 * The returned eigenvector is a column vector, here is 
			 * used as a row vector in dimensionality reduction matrix.
			 */
			fm[i]=BLAS.copy(container[i].eigenvector(),null);
		fm=BLAS.conjugate(fm,fm);
		return fm;
	}
	
	/**
	 * calculate the transfer matrix for PCA
	 * @param csigs
	 * already centered signals, each row is a channel
	 * @param pcath
	 * Threshold for PCA, eigenvalues smaller than this threshold will be discarded.
	 * @return
	 */
	public Complex[][] calculateTransferMatrix(Complex[][] csigs,double pcath)
	{
	double[][][] cov;//the real and imaginary part of the covariance matrix
	HermitianEigensolver.EigenDecomposition coveigen;
	List<HermitianEigensolver.EigenContainer> cl;
	HermitianEigensolver.EigenContainer[] container;
		
		cov=covarianceMatrix(csigs);//calculate the covariance matrix
		coveigen=eigensolver.eig(cov[0],cov[1]);//perform eigendecomposition

		cl=new LinkedList<HermitianEigensolver.EigenContainer>();
		for(HermitianEigensolver.EigenContainer c:coveigen)
			//use eigenvalues larger than the PCA threshold
			if(c.eigenvalue().getReal()>=pcath) cl.add(c);

		container=new HermitianEigensolver.EigenContainer[cl.size()];
		cl.toArray(container);
		return calculateTransferMatrix(container);	
	}
	
	/**
	 * calculate the transfer matrix for PCA
	 * @param csigs
	 * already centered signals, each row is a channel
	 * @param numchout
	 * number of output channels
	 * @return
	 */
	public Complex[][] calculateTransferMatrix(Complex[][] csigs,int numchout)
	{
	double[][][] cov;//the real and imaginary part of the covariance matrix
	HermitianEigensolver.EigenDecomposition coveigen;
	HermitianEigensolver.EigenContainer[] container;
		
		if(numchout>csigs[0].length) throw new IllegalArgumentException(
				"too many output channels: "+numchout+", should no more than: "+csigs[0].length);
		
		cov=covarianceMatrix(csigs);//calculate the covariance matrix
		coveigen=eigensolver.eig(cov[0],cov[1]);//perform eigendecomposition

		container=new HermitianEigensolver.EigenContainer[numchout];
		//select the largest eigenvectors
		for(int i=0;i<container.length;i++) container[i]=coveigen.eigenContainer(i);
		return calculateTransferMatrix(container);		
	}
	
	/**
	 * perform preprocessing
	 * @param sigs
	 * signals, each row for a channel
	 * @param pcath
	 * threshold for PCA
	 * @return
	 */
	public Complex[][] preprocess(Complex[][] sigs,double pcath)
	{
		signalmeans=centering(sigs);
		transform=calculateTransferMatrix(sigs,pcath);
		return BLAS.multiply(transform,sigs,null);
	}
}
