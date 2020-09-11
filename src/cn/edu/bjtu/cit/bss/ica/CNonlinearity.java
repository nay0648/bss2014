package cn.edu.bjtu.cit.bss.ica;
import java.io.*;
import org.apache.commons.math.complex.*;

/**
 * <h1>Description</h1>
 * Complex valued nonlinearity function used in ICA algorithms.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Feb 22, 2012 10:36:07 AM, revision:
 */
public interface CNonlinearity extends Serializable
{
	/**
	 * the function
	 * @param u
	 * @return
	 */
	public Complex g(Complex u);
	
	/**
	 * corresponding derivative
	 * @param u
	 * @return
	 */
	public Complex dg(Complex u);
	
	/**
	 * derivate of dg
	 * @param u
	 * @return
	 */
	public Complex ddg(Complex u);
}
