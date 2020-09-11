package cn.edu.bjtu.cit.bss.ica;
import java.io.*;

/**
 * <h1>Description</h1>
 * Nonlinearity function used in ICA algorithms.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Jan 5, 2011 5:32:32 PM, revision:
 */
public interface Nonlinearity extends Serializable
{
	/**
	 * the function
	 * @param u
	 * @return
	 */
	public double g(double u);
	
	/**
	 * corresponding derivative
	 * @param u
	 * @return
	 */
	public double dg(double u);
	
	/**
	 * derivate of dg
	 * @param u
	 * @return
	 */
	public double ddg(double u);
}
