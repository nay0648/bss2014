package cn.edu.bjtu.cit.bss.ica;
import java.io.*;
import org.apache.commons.math.complex.*;
import pp.util.BLAS;

/**
 * <h1>Description</h1>
 * Calculate different kinds of expectations.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: May 25, 2011 10:51:31 AM, revision:
 */
public class Expectation implements Serializable
{
private static final long serialVersionUID=5014071739616563995L;

	/**
	 * calculate E{a}
	 * @param a
	 * samples
	 * @return
	 */
	public static double e(double[] a)
	{
	double e=0;
	
		for(double s:a) e+=s;
		return e/a.length;
	}

	/**
	 * calculate E{a}
	 * @param a
	 * samples
	 * @return
	 */
	public static Complex e(Complex[] a)
	{
	double er=0,ei=0;
	
		for(Complex s:a) 
		{
			er+=s.getReal();
			ei+=s.getImaginary();
		}
		return new Complex(er/a.length,ei/a.length);
	}
	
	/**
	 * calculate E{a^2}
	 * @param a
	 * samples
	 * @return
	 */
	public static double eSquare(double[] a)
	{
	double e=0;
	
		for(double s:a) e+=s*s;
		return e/a.length;
	}
	
	/**
	 * calculate E{a^2}
	 * @param a
	 * samples
	 * @return
	 */
	public static Complex eSquare(Complex[] a)
	{
	double er=0,ei=0;
		
		for(Complex s:a) 
		{
			er+=s.getReal()*s.getReal()-s.getImaginary()*s.getImaginary();
			ei+=2*s.getReal()*s.getImaginary();
		}
		return new Complex(er/a.length,ei/a.length);
	}
	
	/**
	 * calculate E{|a|}
	 * @param a
	 * samples
	 * @return
	 */
	public static double eAbs(Complex[] a)
	{
	double e=0;
	
		for(Complex s:a) e+=s.abs();
		return e/a.length;
	}
	
	/**
	 * calculate E{|a|^2}
	 * @param a
	 * samples
	 */
	public static double eAbsSquare(Complex[] a)
	{
	double e=0;
	
		for(Complex s:a) e+=s.getReal()*s.getReal()+s.getImaginary()*s.getImaginary();
		return e/a.length;
	}
	
	public static void main(String[] args) throws IOException
	{
	double[][] car,cai;
	Complex[][] ca;
	
		/*
		 * generate test data
		 */
//		car=BLAS.randMatrix(1,100);
//		cai=BLAS.randMatrix(1,100);
//		BLAS.scalarAdd(-0.5,car,car);
//		BLAS.scalarAdd(-0.5,cai,cai);
//		BLAS.scalarMultiply(2,car,car);
//		BLAS.scalarMultiply(2,cai,cai);
//		BLAS.save(car,new File("/home/nay0648/real.txt"));
//		BLAS.save(cai,new File("/home/nay0648/imag.txt"));
		
		car=BLAS.loadDoubleMatrix(new File("/home/nay0648/real.txt"));
		cai=BLAS.loadDoubleMatrix(new File("/home/nay0648/imag.txt"));
		ca=BLAS.buildComplexMatrix(car,cai);

		System.out.println(Expectation.e(car[0]));
		System.out.println(BLAS.toString(Expectation.e(ca[0])));
		System.out.println(Expectation.eSquare(car[0]));
		System.out.println(BLAS.toString(Expectation.eSquare(ca[0])));
		System.out.println(Expectation.eAbs(ca[0]));
		System.out.println(Expectation.eAbsSquare(ca[0]));
	}
}
