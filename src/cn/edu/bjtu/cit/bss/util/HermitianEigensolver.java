package cn.edu.bjtu.cit.bss.util;
import java.io.*;
import java.util.*;
import org.apache.commons.math.complex.*;
import pp.util.BLAS;

/**
 * <h1>Description</h1>
 * Used to calculate eigenvalues and eigenvectors of a complex Hermitian matrix.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Mar 21, 2011 3:49:55 PM, revision:
 */
public abstract class HermitianEigensolver implements Serializable
{
private static final long serialVersionUID=-1986289673415670818L;

	/**
	 * <h1>Description</h1>
	 * Eigenvalue and corresponding eigenvector, enable sorted according 
	 * to eigenvalue's magnitude decreasing order.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Mar 17, 2011 9:01:40 AM, revision:
	 */
	public static class EigenContainer implements Serializable, Comparable<EigenContainer>
	{
	private static final long serialVersionUID=6308960949291700751L;
	private Complex ed;
	private Complex[] ev;//eigenvector

		/**
		 * @param ed
		 * eigenvalue
		 * @param ev
		 * corresponding eigenvector, it's a column vector
		 */
		public EigenContainer(Complex ed,Complex[] ev)
		{
			this.ed=ed;
			this.ev=ev;
		}
		
		/**
		 * get eigenvalue
		 * @return
		 */
		public Complex eigenvalue()
		{
			return ed;
		}
		
		/**
		 * get eigenvector as a column vector
		 * @return
		 */
		public Complex[] eigenvector()
		{
			return ev;
		}

		public int compareTo(EigenContainer o)
		{
		double abs1,abs2;

			abs1=ed.abs();
			abs2=o.ed.abs();
			if(abs1>abs2) return -1;
			else if(abs1<abs2) return 1;
			else return 0;
		}

		public String toString()
		{
			return BLAS.toString(ed)+", "+BLAS.toString(ev);
		}
	}

	/**
	 * <h1>Description</h1>
	 * Holds eigendecomposition results.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Mar 29, 2011 9:22:05 AM, revision:
	 */
	public class EigenDecomposition implements Serializable, Iterable<EigenContainer>
	{
	private static final long serialVersionUID=-6323933549544857309L;
	private List<EigenContainer> container;//decomposition results
	private boolean sorted=false;//true means results are sorted according to eigenvalue decreasing order
			
		/**
		 * @param size
		 * number of eigenvalues
		 */
		EigenDecomposition(int size)
		{
			container=new ArrayList<EigenContainer>(size);
		}
		
		/**
		 * add eigenvalue and corresponding eigenvector into results set
		 * @param ed
		 * eigenvalue
		 * @param ev
		 * corresponding eigenvector
		 */
		void add(Complex ed,Complex[] ev)
		{
			container.add(new EigenContainer(ed,ev));
			sorted=false;
		}
		
		/**
		 * get number of eigenvalues
		 * @return
		 */
		public int size()
		{
			return container.size();
		}
		
		/**
		 * get eigenvalue and corresponding eigenvector
		 * @param index
		 * eigenvalue index
		 * @return
		 */
		public EigenContainer eigenContainer(int index)
		{
			if(!sorted)
			{
				Collections.sort(container);
				sorted=true;
			}
			return container.get(index);
		}
		
		/**
		 * get an eigenvalue
		 * @param index
		 * eigenvalue index
		 * @return
		 */
		public Complex eigenvalue(int index)
		{
			if(!sorted)
			{
				Collections.sort(container);
				sorted=true;
			}
			return container.get(index).eigenvalue();
		}
		
		/**
		 * get all eigenvalues sorted according to magnitude decreasing order
		 * @return
		 */
		public Complex[] eigenvalues()
		{
		Complex[] eds;
		int idx=0;
			
			if(!sorted)
			{
				Collections.sort(container);
				sorted=true;
			}
			eds=new Complex[size()];
			for(EigenContainer c:container) eds[idx++]=c.eigenvalue();
			return eds;
		}
		
		/**
		 * get all eigenvalues as a diagonal matrix
		 * @return
		 */
		public Complex[][] eigenvalueMatrix()
		{
		Complex[][] edm;
		int idx=0;
		
			if(!sorted)
			{
				Collections.sort(container);
				sorted=true;
			}
			
			edm=new Complex[size()][size()];
			for(int i=0;i<edm.length;i++) Arrays.fill(edm[i],Complex.ZERO);

			for(EigenContainer c:container) 
			{	
				edm[idx][idx]=c.eigenvalue();
				idx++;
			}
			return edm;
		}
		
		/**
		 * get an eigenvector, as a column vector
		 * @param index
		 * eigenvector index
		 * @return
		 */
		public Complex[] eigenvector(int index)
		{
			if(!sorted)
			{
				Collections.sort(container);
				sorted=true;
			}
			return container.get(index).eigenvector();
		}
		
		/**
		 * get all eigenvectors as a matrix, each eigenvector is a 
		 * column of the result matrix
		 * @return
		 */
		public Complex[][] eigenvectors()
		{
		Complex[][] evm;
		Complex[] v;
		int col=0;
		
			if(!sorted)
			{
				Collections.sort(container);
				sorted=true;
			}
			evm=new Complex[size()][size()];
			for(EigenContainer c:container)
			{
				v=c.eigenvector();
				for(int row=0;row<evm.length;row++) evm[row][col]=v[row];
				col++;
			}
			return evm;
		}

		public Iterator<EigenContainer> iterator()
		{
			if(!sorted)
			{
				Collections.sort(container);
				sorted=true;
			}
			return container.iterator();
		}
	}
	
	/**
	 * calculate eigenvalues and eigenvectors for a Hermitian matrix
	 * @param real
	 * real part of the Hermitian matrix
	 * @param imag
	 * imaginary part of the Hermitian matrix
	 * @return
	 */
	public abstract EigenDecomposition eig(double[][] real,double[][] imag);
		
	/**
	 * calculate eigenvalues and eigenvectors for a Hermitian matrix
	 * @param matrix
	 * a Hermitian matrix
	 * @return
	 */
	public EigenDecomposition eig(Complex[][] matrix)
	{
	double[][][] rm;
	
		rm=BLAS.splitComplexMatrix(matrix);
		return eig(rm[0],rm[1]);
	}

	/**
	 * calculate eigenvalues and eigenvectors for a real symmetric matrix
	 * @param matrix
	 * a real symmetric matrix
	 * @return
	 */
	public EigenDecomposition eig(double[][] matrix)
	{
	double[][] imag;
	
		imag=new double[matrix.length][matrix[0].length];
		return eig(matrix,imag);
	}
}
