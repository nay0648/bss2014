package cn.edu.bjtu.cit.bss.signalio;
import java.io.*;
import javax.sound.sampled.*;
import org.apache.commons.math.complex.*;
import cn.edu.bjtu.cit.bss.util.*;

/**
 * <h1>Description</h1>
 * Signal source with limited length, used for experiments purpose.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Dec 26, 2011 8:10:48 PM, revision:
 */
public class LimitedSignalSource extends SignalSource
{
private SignalSource ss;//the underlying signal source
private long limit=0;//length limit, 0 for no limit
private long len;//already loaded samples

	/**
	 * @param ss
	 * underlying signal source
	 * @param limit
	 * length limit
	 */
	public LimitedSignalSource(SignalSource ss,long limit)
	{
		this.ss=ss;
		this.limit=limit;
	}
	
	public int numChannels()
	{
		return ss.numChannels();
	}
	
	/**
	 * get number of samples already read
	 * @return
	 */
	public long numSamplesRead()
	{
		return len;
	}

	public void readFrame(double[] frame) throws IOException,EOFException
	{
		if(limit>0&&len>=limit) throw new EOFException();
		ss.readFrame(frame);
		len++;
	}

	public void readFrame(Complex[] frame) throws IOException,EOFException
	{
		if(limit>0&&len>=limit) throw new EOFException();
		ss.readFrame(frame);
		len++;
	}

	public void close() throws IOException
	{
		ss.close();
	}
	
	public static void main(String[] args) throws IOException, UnsupportedAudioFileException
	{
	WaveSource ws;
	LimitedSignalSource lss;
	double[] data=null;
	
		ws=new WaveSource(new File("data/source3.wav"),false);
		
		lss=new LimitedSignalSource(ws,5000);
		data=lss.toArray(data);
		System.out.println(data.length);
		
		Util.playAsAudio(data,ws.audioFormat().getSampleRate());
		
		lss.close();
	}
}
