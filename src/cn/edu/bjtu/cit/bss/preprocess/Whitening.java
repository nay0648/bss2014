package cn.edu.bjtu.cit.bss.preprocess;
import org.apache.commons.math.complex.*;
import cn.edu.bjtu.cit.bss.util.*;

/**
 * <h1>Description</h1>
 * Perform dimensionality reduction by PCA and whitening signals.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Oct 28, 2011 10:03:23 AM, revision:
 */
public class Whitening extends PCA
{
private static final long serialVersionUID=-2875404070536455426L;
		
	/**
	 * Construct whitening matrix with designated eigenvalue and eigenvectors of 
	 * the signal covariance matrix. Here the whitening operation is to make 
	 * E{xx^H}=I but not make sure E{xx^T}=O.
	 * @param container
	 * specified eigenvalue and eigenvectors
	 * @return
	 */
	public Complex[][] calculateTransferMatrix(HermitianEigensolver.EigenContainer... container)
	{
	Complex[][] whitening;// the whitening matrix
	double temp;
	Complex[] evt;
		
		whitening=new Complex[container.length][];
		/*
		 * the whitening is: (V*D^(-1/2))', where V is the eigenvectors, 
		 * D is the eigenvalues
		 */
		for(int edi=0;edi<container.length;edi++)
		{
			//Hermitian matrix only has real eigenvalues
			temp=1.0/Math.sqrt(container[edi].eigenvalue().getReal());
			evt=container[edi].eigenvector();
			for(int i=0;i<evt.length;i++) 
				//Hermitian to product a row vector
				evt[i]=evt[i].multiply(temp).conjugate();
			whitening[edi]=evt;
		}
		return whitening;	
	}
}
