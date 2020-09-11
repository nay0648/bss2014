package cn.edu.bjtu.cit.bss.signalio;
import java.io.*;
import org.apache.commons.math.complex.*;
import pp.util.BLAS;

/**
 * <h1>Description</h1>
 * Used for complex signal input, each channel is represented by two columns, 
 * one for real part, and one for imaginary part.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Aug 25, 2011 7:17:31 PM, revision:
 */
public class ComplexTextSignalSource extends SignalSource
{
private BufferedReader textin=null;//underlying reader
private int numch;//number of channels
private String[] sframe;//string format frame for real and complex part

	/**
	 * @param in
	 * underlying input stream
	 * @throws IOException
	 */
	public ComplexTextSignalSource(InputStream in) throws IOException
	{
		textin=new BufferedReader(new InputStreamReader(in));
		sframe=nextFrame();
		numch=(int)Math.ceil(sframe.length/2.0);
	}
	
	public int numChannels()
	{
		return numch;
	}

	public void readFrame(double[] frame) throws IOException,EOFException
	{
		if(sframe==null) throw new EOFException();
		super.checkFrameSize(frame.length);
		super.checkFrameSize(sframe.length/2);
		
		for(int i=0;i<frame.length;i++)
			frame[i]=Double.parseDouble(sframe[2*i]);
		
		sframe=nextFrame();
	}

	public void readFrame(Complex[] frame) throws IOException,EOFException
	{
	double real,imag;
	
		if(sframe==null) throw new EOFException();
		super.checkFrameSize(frame.length);
		super.checkFrameSize((int)Math.ceil(sframe.length/2.0));
		
		for(int i=0;i<frame.length;i++)
		{
			real=Double.parseDouble(sframe[2*i]);
			if((2*i+1)<sframe.length) imag=Double.parseDouble(sframe[2*i+1]);
			else imag=0;
			
			frame[i]=new Complex(real,imag);
		}
		
		sframe=nextFrame();
	}

	public void close() throws IOException
	{
		textin.close();		
	}

	/**
	 * read next frame
	 * @return
	 * return null if get an eof
	 * @throws IOException
	 */
	private String[] nextFrame() throws IOException
	{
	String ts;
	
		for(ts=null;(ts=textin.readLine())!=null;)
		{
			ts=ts.trim();
			if(ts.length()==0) continue;
			return ts.split("\\s+");
		}
		return null;
	}
	
	public static void main(String[] args) throws IOException
	{
	ComplexTextSignalSource source;
	Complex[] frame;
	
		source=new ComplexTextSignalSource(new FileInputStream("/home/nay0648/test.txt"));
		frame=new Complex[source.numChannels()];
		
		for(;;)
		{
			try
			{
				source.readFrame(frame);
				System.out.println(BLAS.toString(frame));
			}
			catch(EOFException e)
			{
				break;
			}
		}
		
		source.close();
	}
}
