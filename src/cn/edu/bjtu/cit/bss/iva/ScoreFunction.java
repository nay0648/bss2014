package cn.edu.bjtu.cit.bss.iva;
import java.io.*;
import org.apache.commons.math.complex.*;

/**
 * <h1>Description</h1>
 * Score function (nonlinearity transform) used in the maximum likelihood 
 * type of iva algorithms: phi(y)=-d(log(p(y)))/dy.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Jul 18, 2012 9:38:47 AM, revision:
 */
public interface ScoreFunction extends Serializable
{
	/**
	 * set estimated source signals to prepare nonlinearity table
	 * @param ydata
	 * [frequency bin index][source index][stft frame index]
	 */
	public void setYData(Complex[][][] ydata);

	/**
	 * get the value y for nonlinearity mapping phi(y)
	 * @param binidx
	 * frequency bin index
	 * @param sourcei
	 * source index
	 * @param tau
	 * stft frame index
	 * @return
	 */
	public Complex phi(int binidx,int sourcei,int tau);
}
