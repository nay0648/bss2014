package cn.edu.bjtu.cit.bss.iva;
import org.apache.commons.math.complex.*;
import pp.util.BLAS;

/**
 * <h1>Description</h1>
 * Spherical symmetric Laplacian nonlinearity table used in gradient iva.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Jul 10, 2012 9:11:55 AM, revision:
 */
public class SSLScoreFunction implements ScoreFunction
{
private static final long serialVersionUID=758133529300471380L;
private Complex[][][] ydata;//estimated source data [binidx][sourcei][tau]
private double[][] ssq;//sqrt(|yi|^2)

	public void setYData(Complex[][][] ydata)
	{
		this.ydata=ydata;
		
		ssq=new double[ydata[0].length][ydata[0][0].length];
		for(int sourcei=0;sourcei<ydata[0].length;sourcei++) 
			for(int tau=0;tau<ydata[0][0].length;tau++) 
			{
				for(int binidx=0;binidx<ydata.length;binidx++) 
					ssq[sourcei][tau]+=BLAS.absSquare(ydata[binidx][sourcei][tau]);
				
				ssq[sourcei][tau]=Math.sqrt(ssq[sourcei][tau]);
			}
	}

	public Complex phi(int binidx,int sourcei,int tau)
	{
		return ydata[binidx][sourcei][tau].multiply(1.0/(ssq[sourcei][tau]));
	}
}
