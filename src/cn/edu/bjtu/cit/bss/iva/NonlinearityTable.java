package cn.edu.bjtu.cit.bss.iva;
import java.io.*;
import org.apache.commons.math.complex.*;

/**
 * <h1>Description</h1>
 * Used to perform nonlinearity mapping in fast iva algorithms.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Jul 2, 2012 2:56:18 PM, revision:
 */
public interface NonlinearityTable extends Serializable
{	
	/**
	 * set estimated source signals to prepare nonlinearity table
	 * @param ydata
	 * [frequency bin index][source index][stft frame index]
	 */
	public void setYData(Complex[][][] ydata);
	
	/**
	 * the nonlinearity transform G=-log(p(|y|^2))
	 * @param binidx
	 * frequency bin index
	 * @param sourcei
	 * source index
	 * @param tau
	 * stft frame index
	 * @return
	 */
	public double g(int binidx,int sourcei,int tau);
	
	/**
	 * G'
	 * @param binidx
	 * frequency bin index
	 * @param sourcei
	 * source index
	 * @param tau
	 * stft frame index
	 * @return
	 */
	public double dg(int binidx,int sourcei,int tau);
	
	/**
	 * G''
	 * @param binidx
	 * frequency bin index
	 * @param sourcei
	 * source index
	 * @param tau
	 * stft frame index
	 * @return
	 */
	public double ddg(int binidx,int sourcei,int tau);
}
