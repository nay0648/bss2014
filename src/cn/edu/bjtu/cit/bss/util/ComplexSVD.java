package cn.edu.bjtu.cit.bss.util;
import java.io.*;
import java.util.*;
import org.apache.commons.math.complex.*;
import org.apache.commons.math3.linear.*;
import pp.util.BLAS;

/**
 * <h1>Description</h1>
 * Perform complex singular value decomposition: X=U*S*V^H, where V^H is 
 * V's conjugate transpose.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Jul 6, 2012 8:54:37 AM, revision:
 */
public class ComplexSVD implements Serializable
{
private static final long serialVersionUID=-7813690360912146681L;
private double[] s;//singular values
private Complex[][] u,v;

	public ComplexSVD(Complex[][] x)
	{
	RealMatrix xaug;
	List<double[]> uaug2,vaug2;
	
		//build [real(X), imag(X); -imag(X), real(X)]
		{
			xaug=new Array2DRowRealMatrix(x.length*2,x[0].length*2);
			for(int i=0;i<x.length;i++)
				for(int j=0;j<x[i].length;j++)
				{
					xaug.setEntry(i,j,x[i][j].getReal());
					xaug.setEntry(i+x.length,j+x[i].length,x[i][j].getReal());
				
					xaug.setEntry(i,j+x[i].length,x[i][j].getImaginary());
					xaug.setEntry(i+x.length,j,-x[i][j].getImaginary());
				}
		}
		
		//perform svd
		{
		SingularValueDecomposition svd;
		double[] saug;
		RealMatrix uaug,vaug;
	
			svd=new SingularValueDecomposition(xaug);

			/*
			 * get singular values
			 */
			saug=svd.getSingularValues();
			s=new double[saug.length/2];
			for(int i=0;i<s.length;i++) s[i]=(saug[2*i]+saug[2*i+1])/2;

			/*
			 * get U, V and rearrange them
			 */
			uaug=svd.getU();
			uaug2=new ArrayList<double[]>(uaug.getColumnDimension());
			for(int j=0;j<uaug.getColumnDimension();j+=2) uaug2.add(uaug.getColumn(j));
			for(int j=1;j<uaug.getColumnDimension();j+=2) uaug2.add(uaug.getColumn(j));

			vaug=svd.getV();
			vaug2=new ArrayList<double[]>(vaug.getColumnDimension());
			for(int j=0;j<vaug.getColumnDimension();j+=2) vaug2.add(vaug.getColumn(j));
			for(int j=1;j<vaug.getColumnDimension();j+=2) vaug2.add(vaug.getColumn(j));
			
			/*
			 * adjust sign
			 */
			adjustSign(uaug2);
			adjustSign(vaug2);
		}
		
		//extract results
		{
		int delta;
		double[] evr,evi;
		
			delta=uaug2.size()/2;
			u=new Complex[uaug2.get(0).length/2][uaug2.size()/2];
			for(int j=0;j<u[0].length;j++) 
			{
				evr=uaug2.get(j);
				evi=uaug2.get(j+delta);
				for(int i=0;i<u.length;i++) u[i][j]=new Complex(evr[i],evi[i]);
			}
			
			delta=vaug2.size()/2;
			v=new Complex[vaug2.get(0).length/2][vaug2.size()/2];
			for(int j=0;j<v[0].length;j++) 
			{
				evr=vaug2.get(j);
				evi=vaug2.get(j+delta);
				for(int i=0;i<v.length;i++) v[i][j]=new Complex(evr[i],evi[i]);
			}
		}
	}
	
	/**
	 * adjust eigenvector's sign to keep the format: [real(M), imag(M); -imag(M), real(M)]
	 * @param evectors
	 * each element is a column eigenvector
	 */
	private void adjustSign(List<double[]> evectors)
	{
	int deltar,deltac;
	double[] ev1,ev2;
	double value1=0,value2=0;
		
		deltar=evectors.get(0).length/2;
		deltac=evectors.size()/2;
		
nextev:	for(int column=0;column<deltac;column++)
		{
			/*
			 * get two eigenvectors belong to the same eigenvalue
			 */
			ev1=evectors.get(column);
			ev2=evectors.get(column+deltac);

			//adjust direction by real part
			for(int row=0;row<deltar;row++)
			{
				value1=ev1[row];
				value2=ev2[row+deltar];
				if(Math.abs(value1)>=1e-10)
				{
					//real parts must have same sign
					if((value1>0&&value2<0)||(value1<0&&value2>0)) BLAS.scalarMultiply(-1,ev2,ev2);
					continue nextev;
				}
			}
				
			//adjust direction by imaginary part
			for(int row=deltar;row<ev1.length;row++)
			{
				value1=ev1[row];
				value2=ev2[row-deltar];
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
	
	/**
	 * get the matrix U
	 * @return
	 */
	public Complex[][] matrixU()
	{
		return u;
	}
	
	/**
	 * get the matrix V
	 * @return
	 */
	public Complex[][] matrixV()
	{
		return v;
	}
	
	/**
	 * get the matrix V^H
	 * @return
	 */
	public Complex[][] matrixVH()
	{
		return BLAS.transpose(v,null);
	}
	
	/**
	 * get the matrix S
	 * @return
	 */
	public Complex[][] matrixS()
	{
	Complex[][] ss;
	
		ss=new Complex[u[0].length][v[0].length];
		BLAS.fill(ss,Complex.ZERO);
		for(int i=0;i<s.length;i++) ss[i][i]=new Complex(s[i],0);
		
		return ss;
	}
	
	/**
	 * get all singular values
	 * @return
	 */
	public double[] singularValues()
	{
		return s;
	}
	
	/**
	 * get number of singular values
	 * @return
	 */
	public int numSingularValues()
	{
		return s.length;
	}
	
	/**
	 * get a singular value
	 * @param idx
	 * singular value index
	 * @return
	 */
	public double singularValue(int idx)
	{
		return s[idx];
	}
	
	public static void main(String[] args) throws IOException
	{
	Complex[][] x;
//	double[][][] dx;
	ComplexSVD svd;
	Complex[][] u,s,vh,x2;
	double e,error=0;
	
		x=BLAS.randComplexMatrix(3,5);
//		dx=BLAS.splitComplexMatrix(x);
//		BLAS.save(dx[0],new File("/home/nay0648/xr.txt"));
//		BLAS.save(dx[1],new File("/home/nay0648/xi.txt"));
		
//		dx=new double[2][][];
//		dx[0]=BLAS.loadDoubleMatrix(new File("/home/nay0648/xr.txt"));
//		dx[1]=BLAS.loadDoubleMatrix(new File("/home/nay0648/xi.txt"));
//		x=BLAS.buildComplexMatrix(dx[0],dx[1]);
	
		svd=new ComplexSVD(x);
		u=svd.matrixU();
		s=svd.matrixS();
		vh=svd.matrixVH();
		
		x2=BLAS.multiply(u,s,null);
		x2=BLAS.multiply(x2,vh,null);
		
		System.out.println(BLAS.toString(x));
		System.out.println(BLAS.toString(x2));
		
		for(int i=0;i<x.length;i++) 
			for(int j=0;j<x[i].length;j++) 
			{
				e=x2[i][j].subtract(x[i][j]).abs();
				if(e>error) error=e;
			}
		System.out.println("error: "+error);		
	}
}
