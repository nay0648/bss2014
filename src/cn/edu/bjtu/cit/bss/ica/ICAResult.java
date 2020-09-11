package cn.edu.bjtu.cit.bss.ica;
import java.io.*;
import org.apache.commons.math.complex.*;

/**
 * <h1>Description</h1>
 * Used to store results returned by ICA algorithms, including: 
 * signal means, whitening matrix or dimensionality reduction 
 * matrix, demixing matrix for preprocessed signals, demixing 
 * matrix for original inputs, estimated sources, and estimated 
 * mixing matrix.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: May 23, 2011 3:12:19 PM, revision:
 */
public class ICAResult implements Serializable
{
private static final long serialVersionUID=-4319624884567454536L;
private Complex[] means;//signal means
/*
 * If whitening is applied, this stores the whitening matrix; if only dimensionality 
 * reduction is performed, this stores the PCA dimensionality reduction matrix; if 
 * both whitening and PCA is not used, this variable is null.
 */
private Complex[][] whitening;//the whitening matrix
private Complex[][] demixp;//demixing matrix for centered and whitened signals
private Complex[][] demix;//the demixing matrix for input signals
private Complex[][] mix;//the estimated mixing matrix
private Complex[][] ests;//estimated signals

	public ICAResult()
	{}

	/**
	 * get input signal means
	 * @return
	 */
	public Complex[] getSignalMeans()
	{
		return means;
	}
	
	/**
	 * set input signal means
	 * @param means
	 */
	public void setSignalMeans(Complex[] means)
	{
		this.means=means;
	}
	
	/**
	 * Get whitening matrix so that Xcw=V*Xc, where Xc is centered signals, 
	 * V is whitening matrix, Xcw is preprocessed signals.
	 * @return
	 * If whitening is applied, this stores the whitening matrix; if only dimensionality 
	 * reduction is performed, this stores the PCA dimensionality reduction matrix; if 
	 * both whitening and PCA is not used, this variable is null.
	 */
	public Complex[][] getWhiteningMatrix()
	{
		return whitening;
	}
	
	/**
	 * set whitening matrix
	 * @param whitening
	 */
	public void setWhiteningMatrix(Complex[][] whitening)
	{
		this.whitening=whitening;
	}
	
	/**
	 * get demixing matrix for preprocessed signals
	 * @return
	 */
	public Complex[][] getDemixingMatrixPreprocessed()
	{
		return demixp;
	}
	
	/**
	 * set demixing matrix for preprocessed signals
	 * @param demixp
	 */
	public void setDemixingMatrixPreprocessed(Complex[][] demixp)
	{
		this.demixp=demixp;
	}
	
	/**
	 * Get demixing matrix for input signals so that Y=W*X, where Y 
	 * is estimated sources, W is demixing matrix, X is original inputs.
	 * @return
	 */
	public Complex[][] getDemixingMatrix()
	{
		return demix;
	}
	
	/**
	 * set demixing matrix for input signals
	 * @param demix
	 */
	public void setDemixingMatrix(Complex[][] demix)
	{
		this.demix=demix;
	}
	
	/**
	 * Get estimated mixing matrix so that X=A*Y, where X is original 
	 * inputs, A is mixing matrix, Y is estimated sources.
	 * @return
	 */
	public Complex[][] getMixingMatrix()
	{
		return mix;
	}
	
	/**
	 * set estimated mixing matrix
	 * @param mix
	 */
	public void setMixingMatrix(Complex[][] mix)
	{
		this.mix=mix;
	}
	
	/**
	 * get estimated source signals
	 * @return
	 */
	public Complex[][] getEstimatedSignals()
	{
		return ests;
	}
	
	/**
	 * set estimated signals
	 * @param ests
	 */
	public void setEstimatedSignals(Complex[][] ests)
	{
		this.ests=ests;
	}
	
	/**
	 * get the number of input channels
	 * @return
	 */
	public int numInputChannels()
	{
		return demix[0].length;
	}
	
	/**
	 * get the number of output channels
	 * @return
	 */
	public int numOutputChannels()
	{
		return demix.length;
	}
}
