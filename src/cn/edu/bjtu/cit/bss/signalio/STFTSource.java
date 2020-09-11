package cn.edu.bjtu.cit.bss.signalio;
import java.io.*;
import javax.sound.sampled.*;
import org.apache.commons.math.complex.*;
import cn.edu.bjtu.cit.bss.util.*;

/**
 * <h1>Description</h1>
 * Perform short time Fourier transform for a time domain signal 
 * and read results as stream.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Nov 17, 2012 9:30:22 AM, revision:
 */
public class STFTSource extends SignalSource
{
private ShortTimeFourierTransformer stft;//used to perform stft
private ShortTimeFourierTransformer.STFTIterator stftit;
private SignalSource ss;//underlying signal source

	/**
	 * @param stft
	 * used to perform stft
	 * @param ss
	 * underlying signal source
	 * @throws IOException 
	 */
	public STFTSource(ShortTimeFourierTransformer stft,SignalSource ss) throws IOException
	{
		this.stft=stft;
		this.ss=ss;
		
		stftit=stft.stftIterator(ss);
	}
	
	public int numChannels()
	{
		return stft.fftSize();
	}
	
	public void readFrame(double[] frame) throws IOException,EOFException
	{
	Complex[] temp;
		
		this.checkFrameSize(frame.length);
		
		if(stftit.hasNext()) 
		{
			temp=stftit.next();
			for(int f=0;f<frame.length;f++) frame[f]=temp[f].getReal();
		}
		else throw new EOFException();
	}
	
	public void readFrame(Complex[] frame) throws IOException,EOFException
	{
	Complex[] temp;
		
		this.checkFrameSize(frame.length);
		
		if(stftit.hasNext()) 
		{
			temp=stftit.next();
			System.arraycopy(temp,0,frame,0,frame.length);
		}
		else throw new EOFException();
	}
	
	public void close() throws IOException
	{
		ss.close();		
	}
	
	public static void main(String[] args) throws IOException, UnsupportedAudioFileException
	{
	ShortTimeFourierTransformer stft;
	STFTSource stfts;
	Complex[][] data=null;
	
		stft=new ShortTimeFourierTransformer(256,256*3/4,512,null);
		stfts=new STFTSource(stft,new WaveSource(new File("data/source2.wav"),true));
		
		data=stfts.toArray(data);
		stfts.close();
		
		pp.util.Util.showImage(stft.spectrogram(data));
	}
}
