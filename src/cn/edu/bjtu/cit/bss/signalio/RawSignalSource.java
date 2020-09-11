package cn.edu.bjtu.cit.bss.signalio;
import java.io.*;
import org.apache.commons.math.complex.*;
import pp.util.BLAS;

/**
 * <h1>Description</h1>
 * Used to read signals from raw signal stream, each real sample is 
 * saved as a double, each complex sample is saved as two doubles.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Mar 8, 2011 8:41:58 PM, revision:
 */
public class RawSignalSource extends SignalSource
{
private DataInputStream rawin=null;
private int numch;

	/**
	 * @param in
	 * underlying input stream
	 * @param numch
	 * number of channels
	 */
	public RawSignalSource(InputStream in,int numch)
	{
		rawin=new DataInputStream(in);
		this.numch=numch;
	}
	
	public void close() throws IOException
	{
		rawin.close();
	}

	public int numChannels()
	{
		return numch;
	}

	public void readFrame(double[] frame) throws IOException,EOFException
	{
		this.checkFrameSize(frame.length);
		for(int i=0;i<frame.length;i++) frame[i]=rawin.readDouble();
	}

	public void readFrame(Complex[] frame) throws IOException,EOFException
	{
	double real,imag;
	
		this.checkFrameSize(frame.length);
		for(int i=0;i<frame.length;i++) 
		{
			real=rawin.readDouble();
			imag=rawin.readDouble();
			frame[i]=new Complex(real,imag);
		}
	}
	
	public static void main(String[] args) throws IOException
	{
	RawSignalSource source;
	
		source=new RawSignalSource(new FileInputStream("/home/nay0648/raw.dat"),2);
		System.out.println(BLAS.toString(source.toArray((double[][])null)));
		source.close();
	}
}
