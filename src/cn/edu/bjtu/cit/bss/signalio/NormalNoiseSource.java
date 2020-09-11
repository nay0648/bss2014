package cn.edu.bjtu.cit.bss.signalio;
import java.io.*;
import java.util.*;
import org.apache.commons.math.complex.*;

/**
 * <h1>Description</h1>
 * Generate random signal of normal distribution.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Jun 30, 2011 1:52:00 PM, revision:
 */
public class NormalNoiseSource extends SignalSource
{
private long len;//signal length, 0 for infinite length
private double range;//normal distribution [-range, range]
private Random random;
private long count=0;//number of samples already read

	/**
	 * @param len
	 * signal length, 0 for infinite length
	 * @param range
	 * generate normal distribution in [-range, range]
	 * @param seed
	 * random seed
	 */
	public NormalNoiseSource(long len,double range,long seed)
	{
		this.len=len;
		this.range=range;
		random=new Random(seed);
	}
	
	/**
	 * @param len
	 * signal length, 0 for infinite length
	 * @param range
	 * generate normal distribution in [-range, range]
	 */
	public NormalNoiseSource(long len,double range)
	{
		this.len=len;
		this.range=range;
		random=new Random();
	}
	
	public int numChannels()
	{
		return 1;
	}

	public void readFrame(double[] frame) throws IOException,EOFException
	{
		this.checkFrameSize(frame.length);
		if(len>0&&(count++)>=len) throw new EOFException();
		else frame[0]=(random.nextDouble()-0.5)*2*range;
	}

	public void readFrame(Complex[] frame) throws IOException,EOFException
	{
		this.checkFrameSize(frame.length);
		if(len>0&&(count++)>=len) throw new EOFException();
		else frame[0]=new Complex(
				(random.nextDouble()-0.5)*2*range,
				(random.nextDouble()-0.5)*2*range);
	}

	public void close() throws IOException
	{}
	
	public static void main(String[] args) throws IOException
	{
	SignalSource source;
	
		source=new NormalNoiseSource(10,1);
		for(;;) 
		{
			try
			{
				System.out.println(source.readSample());
			}
			catch(EOFException e)
			{
				break;
			}
		}
		source.close();
	}
}
