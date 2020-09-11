package cn.edu.bjtu.cit.bss.iva;
import org.apache.commons.math.complex.*;

/**
 * <h1>Description</h1>
 * SSL score function used in ICA.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Jul 18, 2012 4:03:01 PM, revision:
 */
public class InstSSL implements ScoreFunction
{
private static final long serialVersionUID=4100992175874242585L;
private Complex[][][] ydata;//estimated source data [binidx][sourcei][tau]

	public void setYData(Complex[][][] ydata)
	{
		this.ydata=ydata;
	}
	
	public Complex phi(int binidx,int sourcei,int tau)
	{
	Complex y;
	double abs;
		
		y=ydata[binidx][sourcei][tau];
		abs=y.abs();
		
//		return new Complex(y.getReal()/(2.0*abs),-y.getImaginary()/(2.0*abs));
		return new Complex(y.getReal()/(2.0*abs),y.getImaginary()/(2.0*abs));
	}
	
	public Complex dphi(int binidx,int sourcei,int tau)
	{
	Complex y;
	double abs,abs3,a,b,val;
		
		y=ydata[binidx][sourcei][tau];
		abs=y.abs();
		abs3=abs*abs*abs;
		a=y.getReal();
		b=y.getImaginary();

		val=2.0/abs-a*a/abs3-b*b/abs3;
		return new Complex(val/4.0,0);
	}
}
