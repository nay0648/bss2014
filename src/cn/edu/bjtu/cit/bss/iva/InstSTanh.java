package cn.edu.bjtu.cit.bss.iva;
import org.apache.commons.math.complex.*;

/**
 * <h1>Description</h1>
 * Instantaneous split tanh score function: phi(y)=tanh(real(y))+tanh(imag(y))*i.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Jul 20, 2012 10:12:12 AM, revision:
 */
public class InstSTanh implements ScoreFunction
{
private static final long serialVersionUID=6730095840066079905L;
private Complex[][][] ydata;

	public void setYData(Complex[][][] ydata)
	{
		this.ydata=ydata;
	}
	
	public Complex phi(int binidx,int sourcei,int tau)
	{
	double real,imag;
		
		real=ydata[binidx][sourcei][tau].getReal();
		imag=ydata[binidx][sourcei][tau].getImaginary();
		
		return new Complex(Math.tanh(real),Math.tanh(imag));
	}

	public Complex dphi(int binidx,int sourcei,int tau)
	{
	double real,imag;
		
		real=ydata[binidx][sourcei][tau].getReal();
		imag=ydata[binidx][sourcei][tau].getImaginary();
		
		real=Math.tanh(real);
		imag=Math.tanh(imag);
		
		return new Complex(1-real*real,1-imag*imag);
	}
}
