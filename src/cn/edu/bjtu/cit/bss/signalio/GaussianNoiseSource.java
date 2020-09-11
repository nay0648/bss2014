package cn.edu.bjtu.cit.bss.signalio;
import java.io.*;
import java.util.*;
import org.apache.commons.math.complex.*;

/**
 * <h1>Description</h1>
 * Generate random signal of Gaussian distribution.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Jun 30, 2011 1:52:00 PM, revision:
 */
public class GaussianNoiseSource extends SignalSource
{
private long len;//signal length, 0 for infinite length
private double sigma;//standard deviation for Gaussian
private Random random;
private long count=0;//number of samples already read

	/**
	 * @param len
	 * signal length, 0 for infinite length
	 * @param sigma
	 * standard deviation for Gaussian
	 * @param seed
	 * random seed
	 */
	public GaussianNoiseSource(long len,double sigma,long seed)
	{
		this.len=len;
		this.sigma=sigma;
		random=new Random(seed);
	}
	
	/**
	 * @param len
	 * signal length, 0 for infinite length
	 * @param sigma
	 * standard deviation for Gaussian
	 */
	public GaussianNoiseSource(long len,double sigma)
	{
		this.len=len;
		this.sigma=sigma;
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
		else frame[0]=random.nextGaussian()*sigma;
	}

	public void readFrame(Complex[] frame) throws IOException,EOFException
	{
		this.checkFrameSize(frame.length);
		if(len>0&&(count++)>=len) throw new EOFException();
		else frame[0]=new Complex(
				random.nextGaussian()*sigma,
				random.nextGaussian()*sigma);
	}

	public void close() throws IOException
	{}
	
	public static void main(String[] args) throws IOException
	{
	SignalSource source;
	
		source=new GaussianNoiseSource(10,1);
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
