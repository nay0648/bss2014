package cn.edu.bjtu.cit.bss.iva;
import org.apache.commons.math.complex.*;
import pp.util.BLAS;

/**
 * <h1>Description</h1>
 * Spherically symmetric Laplacian nonlinearity.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Jul 5, 2012 10:14:26 AM, revision:
 */
public class SSLNonlinearity implements NonlinearityTable
{
private static final long serialVersionUID=-1792755365130061681L;
private double[][] g2;

	public void setYData(Complex[][][] ydata)
	{
		g2=new double[ydata[0].length][ydata[0][0].length];
		
		for(int binidx=0;binidx<ydata.length;binidx++) 
			for(int sourcei=0;sourcei<ydata[0].length;sourcei++) 
				for(int tau=0;tau<ydata[0][0].length;tau++) 
					g2[sourcei][tau]+=BLAS.absSquare(ydata[binidx][sourcei][tau]);
	}
	
	public double g(int binidx,int sourcei,int tau)
	{
		return Math.sqrt(g2[sourcei][tau]);
	}
	
	public double dg(int binidx,int sourcei,int tau)
	{
		return 1.0/(2.0*Math.sqrt(g2[sourcei][tau]));
	}

	public double ddg(int binidx,int sourcei,int tau)
	{
		return -1.0/(4.0*Math.pow(g2[sourcei][tau],1.5));
	}
}
