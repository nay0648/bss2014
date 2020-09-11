package cn.edu.bjtu.cit.bss.preprocess;
import java.util.*;
import org.apache.commons.math.complex.*;
import pp.util.BLAS;
import cn.edu.bjtu.cit.bss.util.*;

/**
 * <h1>Description</h1>
 * Forth Order Blind Identification, see: Aapo Hyvarinen, Juha Karhunen, 
 * Erkki Oja, Independent Component Analysis, section 11.5.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Mar 9, 2012 9:51:46 AM, revision:
 */
public class FOBI extends Whitening
{
private static final long serialVersionUID=-799455622299747639L;

	public Complex[][] calculateTransferMatrix(Complex[][] csigs,int numchout)
	{
	Complex[][] whitening;//the whitening matrix
	Complex[][] omega;//contains 4th order correlation information
	Complex[] z;
	double normsquare;
	HermitianEigensolver solver;
	Complex[][] ev;
	
		//calculate the whitening matrix first
		whitening=super.calculateTransferMatrix(csigs,numchout);
	
		/*
		 * calculate the 4th-order correlation
		 */
		omega=new Complex[whitening.length][whitening.length];
		for(int i=0;i<omega.length;i++) Arrays.fill(omega[i],Complex.ZERO);
		z=new Complex[whitening.length];
		
		for(int j=0;j<csigs[0].length;j++)
		{
			/*
			 * the centered and whitened signals
			 */
			Arrays.fill(z,Complex.ZERO);		
			for(int i=0;i<whitening.length;i++) 
				for(int k=0;k<csigs.length;k++) 
					z[i]=z[i].add(whitening[i][k].multiply(csigs[k][j]));
			
			/*
			 * calculate zz^H||z||^2
			 */
			normsquare=BLAS.norm2(z);
			normsquare*=normsquare;
			
			for(int ii=0;ii<omega.length;ii++) 
				for(int jj=0;jj<omega[ii].length;jj++) 
					omega[ii][jj]=omega[ii][jj].add(z[ii].multiply(z[jj].conjugate()).multiply(normsquare));
		}
		BLAS.scalarMultiply(1.0/csigs[0].length,omega,omega);

		/*
		 * perform eigen decomposition
		 */
		solver=new CommonsEigensolver();
		
		/*
		 * In the FOBI algorithm, sometimes the eigenvector matrix is 
		 * the demixing matrix of the whitened signals.
		 */
		ev=solver.eig(omega).eigenvectors();

		return BLAS.multiply(ev,whitening,whitening);
	}
}
