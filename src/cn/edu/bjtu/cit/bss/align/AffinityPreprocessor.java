package cn.edu.bjtu.cit.bss.align;
import java.io.*;

/**
 * <h1>Description</h1>
 * Convert an affinity matrix to another affinity matrix, some semi-supervised 
 * algorithms can be integrated in.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Nov 14, 2011 6:49:39 PM, revision:
 */
public interface AffinityPreprocessor extends Serializable
{
	/**
	 * convert an affinity matrix to another affinity matrix
	 * @param am
	 * an affinity matrix
	 * @return
	 */
	public AffinityMatrix preprocess(AffinityMatrix am);
}
