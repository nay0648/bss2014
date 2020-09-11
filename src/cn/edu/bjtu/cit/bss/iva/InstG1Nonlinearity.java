package cn.edu.bjtu.cit.bss.iva;
import org.apache.commons.math.complex.*;
import pp.util.BLAS;

/**
 * <h1>Description</h1>
 * G1=sqrt(a1+y) used in instantaneous fast ica.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Jul 25, 2012 9:44:57 AM, revision:
 */
public class InstG1Nonlinearity implements NonlinearityTable
{
private static final long serialVersionUID=-4802118515225262000L;
private double a1=0.1;
private Complex[][][] ydata;

	public void setYData(Complex[][][] ydata)
	{
		this.ydata=ydata;
	}
	
	public double g(int binidx,int sourcei,int tau)
	{
		return Math.sqrt(a1+BLAS.absSquare(ydata[binidx][sourcei][tau]));
	}
	
	public double dg(int binidx,int sourcei,int tau)
	{
		return 1.0/(2.0*Math.sqrt(a1+BLAS.absSquare(ydata[binidx][sourcei][tau])));	
	}

	public double ddg(int binidx,int sourcei,int tau)
	{
		return -1.0/(4.0*Math.pow(a1+BLAS.absSquare(ydata[binidx][sourcei][tau]),1.5));
	}
}
