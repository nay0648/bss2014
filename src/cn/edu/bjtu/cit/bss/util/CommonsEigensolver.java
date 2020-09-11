package cn.edu.bjtu.cit.bss.util;
import java.util.*;
import org.apache.commons.math.complex.*;
import org.apache.commons.math.linear.*;
import org.apache.commons.math.util.*;
import pp.util.BLAS;

/**
 * <h1>Description</h1>
 * Calculate eigenvalues and eigenvectors of a Hermitian matrix by apache commons API.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Mar 22, 2011 9:09:00 AM, revision:
 */
public class CommonsEigensolver extends HermitianEigensolver
{
private static final long serialVersionUID=411443363707927999L;
	
	public EigenDecomposition eig(double[][] real,double[][] imag)
	{
	Array2DRowRealMatrix aug;
	List<double[]> evectors;//eigenvectors, each array is a column vector
	double[] evalues;

		//construct [real(M) imag(M); -imag(M) real(M)]
		{
		double avg;
		
			if(real.length!=imag.length||real[0].length!=imag[0].length) throw new IllegalArgumentException(
					"real part and imaginary part size not match: "+
					real.length+" x "+real[0].length+", "+imag.length+" x "+imag[0].length);
		
			aug=new Array2DRowRealMatrix(real.length*2,real[0].length*2);
			for(int i=0;i<real.length;i++)
				for(int j=0;j<real[i].length;j++)
				{
					/*
					 * symmetric
					 */
					if(i<=j)
					{
						avg=(real[i][j]+real[j][i])/2.0;
						aug.setEntry(i,j,avg);
						aug.setEntry(j,i,avg);
					}
					if(i+real.length<=j+real[i].length)
					{
						avg=(real[i][j]+real[j][i])/2.0;
						aug.setEntry(i+real.length,j+real[i].length,avg);
						aug.setEntry(j+real[i].length,i+real.length,avg);
					}
					if(i<=j+imag[i].length)
					{
						avg=(imag[i][j]-imag[j][i])/2.0;
						aug.setEntry(i,j+imag[i].length,avg);
						aug.setEntry(j+imag[i].length,i,avg);
					}
				}
		}
		
		/*
		 * Perform eigendecomposition by apache commons api, the resulting 
		 * eigenvalues has the multiplicity of 2, and already sorted in 
		 * decreasing order. Hermitian matrix only has real eigenvalues.
		 */
		{
		EigenDecompositionImpl eig;
		
			eig=new EigenDecompositionImpl(aug,MathUtils.SAFE_MIN);
			evalues=new double[aug.getColumnDimension()/2];
			for(int i=0;i<evalues.length*2;i+=2) 
				evalues[i/2]=(eig.getRealEigenvalue(i)+eig.getRealEigenvalue(i+1))/2.0;

			/*
			 * get eigenvectors as column vectors
			 */
			evectors=new ArrayList<double[]>(aug.getColumnDimension());
			for(int i=0;i<aug.getColumnDimension();i++) evectors.add(eig.getEigenvector(i).getData());
		}
		
		/*
		 * rearrange the order of eigenvectors to overcome multiplicity effect
		 */
		{
		List<double[]> temp;
		
			temp=new ArrayList<double[]>(evectors.size());
			for(int i=0;i<evectors.size();i+=2) temp.add(evectors.get(i));
			for(int i=1;i<evectors.size();i+=2) temp.add(evectors.get(i));
			evectors=temp;
		}
		
		/*
		 * Adjust eigenvector sign to make sure eigenvalues belong to the same 
		 * multiple eigenvalue point to the same direction. The resulting matrix 
		 * is: [real(EV) imag(EV); -imag(EV) real(EV)], where EV is the eigenvector 
		 * matrix of the input Hermitian matrix.
		 */
		{
		int delta;
		double[] ev1,ev2;
		double value1=0,value2=0;
		
			delta=evectors.size()/2;
nextev:		for(int column=0;column<delta;column++)
			{
				/*
				 * get two eigenvectors belong to the same eigenvalue
				 */
				ev1=evectors.get(column);
				ev2=evectors.get(column+delta);

				//adjust direction by real part
				for(int row=0;row<delta;row++)
				{
					value1=ev1[row];
					value2=ev2[row+delta];
					if(Math.abs(value1)>=1e-10)
					{
						//real parts must have same sign
						if((value1>0&&value2<0)||(value1<0&&value2>0)) BLAS.scalarMultiply(-1,ev2,ev2);
						continue nextev;
					}
				}
				
				//adjust direction by imaginary part
				for(int row=delta;row<ev1.length;row++)
				{
					value1=ev1[row];
					value2=ev2[row-delta];
					if(Math.abs(value1)>=1e-10)
					{
						//imaginary parts must have different sign
						if((value1>0&&value2>0)||(value1<0&&value2<0)) BLAS.scalarMultiply(-1,ev2,ev2);
						continue nextev;
					}
				}

				throw new ArithmeticException("zero eigenvector: "+Arrays.toString(ev1));
			}
		}
		
		/*
		 * extract results
		 */
		{
		EigenDecomposition result;
		Complex[] ev;
		int delta;
		double avgr,avgi;
		
			delta=evectors.size()/2;
			result=new EigenDecomposition(evalues.length);
			//get eigenvectors
			for(int column=0;column<evalues.length;column++)
			{
				ev=new Complex[evalues.length];//an eigenvector as a column vector
				//get eitries of a eigenvector
				for(int row=0;row<ev.length;row++)
				{
					avgr=(evectors.get(column)[row]+evectors.get(column+delta)[row+delta])/2.0;
					avgi=(evectors.get(column+delta)[row]-evectors.get(column)[row+delta])/2.0;
					ev[row]=new Complex(avgr,avgi);
				}
				result.add(new Complex(evalues[column],0),ev);
			}
			return result;
		}
	}

	public static void main(String[] args)
	{
	int order=5;
	double[][] real,imag;
	Complex[][] cov;
	CommonsEigensolver solver;
	EigenDecomposition result;
		
		/*
		 * generate a Hermitian matrix
		 */
		real=BLAS.randMatrix(order,order);
		imag=BLAS.randMatrix(order,order);

		cov=BLAS.buildComplexMatrix(real,imag);
		BLAS.multiply(cov,BLAS.transpose(cov,null),cov);
		System.out.println(BLAS.toString(cov));
		
		/*
		 * solve the eigenproblem
		 */
		solver=new CommonsEigensolver();
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
