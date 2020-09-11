package cn.edu.bjtu.cit.bss.ica;
import java.io.*;
import org.apache.commons.math.complex.*;

/**
 * <h1>Description</h1>
 * Super class for all ICA algorithms.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: May 23, 2011 3:12:04 PM, revision:
 */
public abstract class ICA implements Serializable
{
private static final long serialVersionUID=4408263879099542601L;
	
	/**
	 * Perform ICA on already preprocessed signals and return results.
	 * @param sigsp
	 * already preprocessed signals
	 * @param seeds
	 * initial seeds, each row is a seed
	 * @return
	 */
	public abstract ICAResult icaPreprocessed(Complex[][] sigsp,Complex[][] seeds);
	
	/**
	 * perform ICA on input signals and return results, including: 
	 * signal means, whitening matrix or dimensionality reduction 
	 * matrix, demixing matrix for preprocessed signals, demixing 
	 * matrix for original inputs, estimated sources, and estimated 
	 * mixing matrix. Input signals will be centered after this 
	 * method invocation.
	 * @param sigs
	 * Input signals, each row is a channel. Input signals will be centered.
	 * @return
	 */
	public abstract ICAResult ica(Complex[][] sigs);
}
