package cn.edu.bjtu.cit.bss.util;
import org.apache.commons.math.complex.*;
import com.mathworks.toolbox.javabuilder.*;
import pp.util.BLAS;
import icam.*;

/**
 * <h1>Description</h1>
 * Calculate eigenvalues and eigenvectors of a Hermitian matrix by matlab.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Mar 21, 2011 3:57:00 PM, revision:
 */
public class MatlabEigensolver extends HermitianEigensolver
{
private static final long serialVersionUID=-8120852919736319236L;
private Eigen eigen=null;//matlab eigensolver

	public MatlabEigensolver()
	{
		try
		{
			eigen=new Eigen();
		}
		catch(MWException e)
		{
			throw new RuntimeException("failed to construct matlab eigensolver",e);
		}
	}
	
	public void finalize()
	{
		if(eigen!=null) eigen.dispose();
	}

	public EigenDecomposition eig(double[][] real,double[][] imag)
	{
	Object[] lhs;
	MWNumericArray rv,iv,rd,id;
	int[] coordinate;
	Complex ed;
	Complex[] ev;
	EigenDecomposition result;

		try
		{
			lhs=eigen.complexEig(4,real,imag);
			rv=(MWNumericArray)lhs[0];//eigenvector's real part
			iv=(MWNumericArray)lhs[1];//eigenvector's imaginary part
			rd=(MWNumericArray)lhs[2];//eigenvalue's real part, its imaginary part is 0
			id=(MWNumericArray)lhs[3];//eigenvalue's imaginary part
		}
		catch(MWException e)
		{
			throw new RuntimeException("failed to perform eigendecomposition",e);
		}

		/*
		 * extract eigenvalue and eigenvector and construct it as 
		 * commons Complex form
		 */
		result=new EigenDecomposition(rd.getDimensions()[1]);
		coordinate=new int[2];//used to access matlab matrix
		//traverse each eigenvalue
		for(int j=0;j<rd.getDimensions()[1];j++)
		{
			/*
			 * eigenvalue
			 */
			coordinate[0]=1;//matlab index starts from 1
			coordinate[1]=j+1;
			ed=new Complex(rd.getDouble(coordinate),id.getDouble(coordinate));
				
			/*
			 * eigenvector
			 */
			coordinate[1]=j+1;
			ev=new Complex[rv.getDimensions()[0]];
			for(int i=0;i<ev.length;i++)
			{
				coordinate[0]=i+1;
				ev[i]=new Complex(rv.getDouble(coordinate),iv.getDouble(coordinate));
			}
			result.add(ed,ev);
		}
		return result;
	}
	
	public static void main(String[] args) throws MWException
	{
	int order=4;
	double[][] real,imag;
	Complex[][] cov;
	MatlabEigensolver solver;
	EigenDecomposition result;
	
		/*
		 * generate a Hermitian matrix
		 */
		real=BLAS.randMatrix(order,order);
		imag=BLAS.randMatrix(order,order);
		cov=BLAS.buildComplexMatrix(real,imag);
		BLAS.multiply(cov,BLAS.transpose(cov,(Complex[][])null),cov);
		System.out.println(BLAS.toString(cov));
		
		/*
		 * solve the eigenproblem
		 */
		solver=new MatlabEigensolver();
		result=solver.eig(cov);
		System.out.println(BLAS.toString(result.eigenvalues()));
		
		/*
		 * calculate EV'*Cov*EV
		 */
		System.out.println();
		BLAS.multiply(BLAS.transpose(result.eigenvectors(),null),cov,cov);
		System.out.println(BLAS.toString(BLAS.multiply(cov,result.eigenvectors(),null)));
		
		/*
		 * calculate EV*EV'
		 */
		System.out.println();
		System.out.println(BLAS.toString(
				BLAS.multiply(result.eigenvectors(),BLAS.transpose(result.eigenvectors(),null),null)));
	}
}
