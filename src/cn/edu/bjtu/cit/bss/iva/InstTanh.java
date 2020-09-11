package cn.edu.bjtu.cit.bss.iva;
import org.apache.commons.math.complex.*;

/**
 * <h1>Description</h1>
 * Instantaneous tanh score function, phi(y)=2*tanh(y).
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Jul 20, 2012 9:59:50 AM, revision:
 */
public class InstTanh implements ScoreFunction
{
private static final long serialVersionUID=7238737340991761166L;
private Complex[][][] ydata;

	public void setYData(Complex[][][] ydata)
	{
		this.ydata=ydata;
	}

	public Complex phi(int binidx,int sourcei,int tau)
	{
		return ydata[binidx][sourcei][tau].tanh().multiply(2);
	}

	public Complex dphi(int binidx,int sourcei,int tau)
	{
	Complex tanh;
	
		tanh=ydata[binidx][sourcei][tau].tanh();
		//2*(1-tanh^2(x))
		return Complex.ONE.subtract(tanh.multiply(tanh)).multiply(2);
	}
}
