package cn.edu.bjtu.cit.bss.signalio;
import java.io.*;
import org.apache.commons.math.complex.*;

/**
 * <h1>Description</h1>
 * This is used to save signals into raw stream, each real sample is saved 
 * as a double, each complex sample is saved as two doubles.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Mar 8, 2011 8:44:30 PM, revision:
 */
public class RawSignalSink extends SignalSink
{
private DataOutputStream rawout=null;
private int numch;

	/**
	 * @param out
	 * underlying output stream
	 * @param numch
	 * number of channels
	 */
	public RawSignalSink(OutputStream out,int numch)
	{
		rawout=new DataOutputStream(out);
		this.numch=numch;
	}
	
	public void flush() throws IOException
	{
		rawout.flush();
	}
	
	public void close() throws IOException
	{
		rawout.close();
	}

	public int numChannels()
	{
		return numch;
	}

	public void writeFrame(double[] frame) throws IOException
	{
		this.checkFrameSize(frame.length);
		for(int i=0;i<frame.length;i++) rawout.writeDouble(frame[i]);
	}

	public void writeFrame(Complex[] frame) throws IOException
	{
		this.checkFrameSize(frame.length);
		for(int i=0;i<frame.length;i++) 
		{	
			rawout.writeDouble(frame[i].getReal());
			rawout.writeDouble(frame[i].getImaginary());
		}
	}
	
	public static void main(String[] args) throws IOException
	{
	RawSignalSink sink;
	
		sink=new RawSignalSink(new FileOutputStream("/home/nay0648/raw.dat"),2);
		sink.writeSample(1);
		sink.writeSample(2);
		sink.writeSample(3);
		sink.writeSample(4);
		sink.writeSample(5);
		sink.flush();
		sink.close();
	}
}
