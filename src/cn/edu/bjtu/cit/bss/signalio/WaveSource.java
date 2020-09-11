package cn.edu.bjtu.cit.bss.signalio;
import java.io.*;
import javax.sound.sampled.*;
import org.apache.commons.math.complex.*;
import cn.edu.bjtu.cit.bss.util.*;

/**
 * <h1>Description</h1>
 * This is used to read wave signals. Not safe for multithread access.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Mar 8, 2011 8:58:44 PM, revision:
 */
public class WaveSource extends SignalSource
{
private BufferedInputStream in=null;//underlying input stream
private AudioFormat format;//audio format
private boolean normalize=false;//true to normalize sample values to [-1, 1]
private int bps;//bytes per sample
private long maxval;//max unsigned value
private byte[] bframe;//bytes data for a frame
private double[] rframe;//used to read complex frame

	/**
	 * @param in
	 * underlying stream
	 * @param buffersize
	 * buffersize in bytes
	 * @param normalize
	 * true to normalize sample values to [-1, 1]
	 */
	public WaveSource(AudioInputStream in,int buffersize,boolean normalize)
	{
		this.in=new BufferedInputStream(in,buffersize);//use buffer to speedup real time access
		format=in.getFormat();
		this.normalize=normalize;

		bps=format.getSampleSizeInBits()/Byte.SIZE;
		maxval=(long)Math.pow(2,format.getSampleSizeInBits());
		bframe=new byte[format.getChannels()*format.getSampleSizeInBits()/Byte.SIZE];
		rframe=new double[format.getChannels()];
	}
	
	/**
	 * get buffer size in bytes needed to cache a sound segment
	 * @param format
	 * audio format
	 * @param duration
	 * sound duration in seconds
	 * @return
	 */
	private static int calculateBufferSize(AudioFormat format,double duration)
	{
		return (int)Math.ceil(duration*format.getSampleRate()*format.getChannels()*format.getSampleSizeInBits()/Byte.SIZE);
	}

	/**
	 * @param in
	 * underlying stream
	 * @param normalize
	 * true to normalize sample values to [-1, 1]
	 */
	public WaveSource(AudioInputStream in,boolean normalize)
	{
		this(in,calculateBufferSize(in.getFormat(),0.02),normalize);
	}
	
	/**
	 * construct signal source from audio file
	 * @param audiofile
	 * audio file path
	 * @param normalize
	 * true to normalize sample values to [-1, 1]
	 * @throws IOException
	 * @throws UnsupportedAudioFileException
	 */
	public WaveSource(File audiofile,boolean normalize) throws IOException, UnsupportedAudioFileException
	{
		this(AudioSystem.getAudioInputStream(audiofile),normalize);
	}
	
	public void close() throws IOException
	{
		in.close();
	}

	/**
	 * get underlying audio format
	 * @return
	 */
	public AudioFormat audioFormat()
	{
		return format;
	}
	
	public int numChannels()
	{
		return format.getChannels();
	}

	public void readFrame(double[] frame) throws IOException,EOFException
	{
	int offset=0,count=0;
	long sample;
		
		this.checkFrameSize(frame.length);
		
		//read a frame in byte form		
		for(;offset<bframe.length;)
		{
			count=in.read(bframe,offset,bframe.length-offset);
			if(count==-1) throw new EOFException();
			offset+=count;
		}
		
		//parse frame
		for(int i=0;i<frame.length;i++)
		{
			sample=0;
			//the higher digits are stored at lower address
			if(format.isBigEndian()) 
			{
				for(int j=0;j<bps;j++) sample|=bframe[i*bps+j]&0x000000ff<<((bps-j)*Byte.SIZE);
				//adjuse value for signed signal
				if(AudioFormat.Encoding.PCM_SIGNED.equals(format.getEncoding())&&
						(bframe[i*bps]&0x80)!=0) sample-=maxval;
			}
			//the higher digits are stored at higher address
			else 
			{
				for(int j=0;j<bps;j++) sample|=(bframe[i*bps+j]&0x000000ff)<<(j*Byte.SIZE);	
				//adjuse value for signed signal
				if(AudioFormat.Encoding.PCM_SIGNED.equals(format.getEncoding())&&
						(bframe[i*bps+bps-1]&0x80)!=0) sample-=maxval;
			}
			frame[i]=sample;
			
			//normalize sample value to [-1, 1] if needed
			if(normalize)
			{
				if(AudioFormat.Encoding.PCM_SIGNED.equals(format.getEncoding())) 
					frame[i]/=(maxval/2);
				else frame[i]=(frame[i]/maxval-0.5)*2;
				if(frame[i]<-1) frame[i]=-1;else if(frame[i]>1) frame[i]=1;
			}
		}
	}

	public void readFrame(Complex[] frame) throws IOException,EOFException
	{
		this.checkFrameSize(frame.length);		
		readFrame(rframe);
		for(int i=0;i<frame.length;i++) frame[i]=new Complex(rframe[i],0);
	}
	
	public static void main(String[] args) throws IOException, UnsupportedAudioFileException
	{
	AudioInputStream audioin;
	WaveSource source;
	double[][] data;
	
		audioin=AudioSystem.getAudioInputStream(new File(
				"data/source2.wav"));
//				"/home/nay0648/test.wav"));
		source=new WaveSource(audioin,true);
		data=source.toArray((double[][])null);
		
		Util.plotSignals(source.audioFormat().getSampleRate(),data);
		
		source.close();
	}
}
