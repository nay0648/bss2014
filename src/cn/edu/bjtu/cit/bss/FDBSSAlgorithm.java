package cn.edu.bjtu.cit.bss;
import java.io.*;
import org.apache.commons.math.complex.*;
import cn.edu.bjtu.cit.bss.signalio.*;
import cn.edu.bjtu.cit.bss.util.*;

/**
 * <h1>Description</h1>
 * Abstract class for frequency domain BSS algorithms.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Mar 19, 2012 6:13:22 PM, revision:
 */
public abstract class FDBSSAlgorithm implements Serializable
{
private static final long serialVersionUID=5782785655914151281L;
//used to perform stft and inverse stft, with initial parameters
private ShortTimeFourierTransformer stft=new ShortTimeFourierTransformer(1024,1024*7/8,2048,null);
	
	/**
	 * set parameters for STFT
	 * @param stftsize
	 * stft block size
	 * @param stftoverlap
	 * overlapped stft block taps
	 * @param fftsize
	 * fft block size, must be powers of 2
	 */
	public void setSTFTParameters(int stftsize,int stftoverlap,int fftsize)
	{
		stft=new ShortTimeFourierTransformer(stftsize,stftoverlap,fftsize,null);
	}
	
	/**
	 * get the stft transformer
	 * @return
	 */
	public ShortTimeFourierTransformer stfTransformer()
	{
		return stft;
	}
	
	/**
	 * get stft block size
	 * @return
	 */
	public int stftSize()
	{
		return stft.stftSize();
	}
	
	/**
	 * get stft block overlap in taps
	 * @return
	 */
	public int stftOverlap()
	{
		return stft.stftOverlap();
	}
	
	/**
	 * get fft block size
	 * @return
	 */
	public int fftSize()
	{
		return stft.fftSize();
	}
	
	/**
	 * get number of sources
	 * @return
	 * 0 for auto detect number of sources
	 */
	public abstract int numSources();
	
	/**
	 * get number of sensors
	 * @return
	 */
	public abstract int numSensors();
	
	/**
	 * load data in a frequency bin
	 * @param binidx
	 * frequency bin index
	 * @param buffer
	 * space used to store data, null to allocate new space
	 * @return
	 */
	public abstract Complex[][] binData(int binidx,Complex[][] buffer);
	
	/**
	 * estimate demixing filters from observed signals
	 * @param x
	 * multichannel sensor signals
	 * @return
	 * @throws IOException
	 */
	public abstract DemixingModel estimateDemixingModel(SignalSource x) throws IOException;
	
	/**
	 * perform frequency domain blind source separation
	 * @param x
	 * observed sensor signals
	 * @param y
	 * estimated source signals
	 * @return
	 * estimated demixing model
	 * @throws IOException
	 */
	public abstract DemixingModel separate(SignalSource x,SignalSink y) throws IOException;
}
