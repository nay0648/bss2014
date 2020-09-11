package cn.edu.bjtu.cit.bss.signalio;
import java.io.*;
import org.apache.commons.math.complex.*;

/**
 * <h1>Description</h1>
 * Used to save signal as text.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Mar 8, 2011 1:45:55 PM, revision:
 */
public class TextSignalSink extends SignalSink
{
private BufferedWriter textout=null;
private int numch;//number of channels

	/**
	 * @param out
	 * underlying output stream
	 * @param numch
	 * number of channels
	 * @throws IOException
	 */
	public TextSignalSink(OutputStream out,int numch) throws IOException
	{
		this.numch=numch;
		textout=new BufferedWriter(new OutputStreamWriter(out));
	}
	
	public void flush() throws IOException
	{
		textout.flush();
	}
	
	public void close() throws IOException
	{
		textout.close();
	}

	public int numChannels()
	{
		return numch;
	}

	public void writeFrame(double[] frame) throws IOException
	{
		this.checkFrameSize(frame.length);
		for(int i=0;i<frame.length;i++)
		{
			textout.write(Double.toString(frame[i]));
			if(i<frame.length-1) textout.write(" ");
		}
		textout.write("\n");
	}

	public void writeFrame(Complex[] frame) throws IOException
	{
	double real,imag;
	
		this.checkFrameSize(frame.length);
		for(int i=0;i<frame.length;i++)
		{
			real=frame[i].getReal();
			imag=frame[i].getImaginary();
			textout.write(Double.toString(real));
			if(imag>=0) textout.write("+");
			textout.write(Double.toString(imag)+"j");
			if(i<frame.length-1) textout.write(" ");
		}
		textout.write("\n");
	}
	
	public static void main(String[] args) throws IOException
	{
	TextSignalSink sink;
	
		sink=new TextSignalSink(System.out,2);
		sink.writeSample(1);
		sink.writeComplexSample(new Complex(1,2));
		sink.writeComplexSample(new Complex(3,-4));
		sink.flush();
	}
}
