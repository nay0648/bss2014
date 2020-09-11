package cn.edu.bjtu.cit.bss.signalio;
import java.io.*;
import org.apache.commons.math.complex.*;

/**
 * <h1>Description</h1>
 * Used to output complex signals as text, each channel is outputed as 
 * two columns, one for real part, one for imaginary part.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Aug 25, 2011 7:37:25 PM, revision:
 */
public class ComplexTextSignalSink extends SignalSink
{
private BufferedWriter textout=null;
private int numch;

	/**
	 * @param out
	 * underlying output stream
	 * @param numch
	 * number of channels
	 * @throws IOException
	 */
	public ComplexTextSignalSink(OutputStream out,int numch) throws IOException
	{
		this.numch=numch;
		textout=new BufferedWriter(new OutputStreamWriter(out));
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
			textout.write(frame[i]+" 0");
			if(i<frame.length-1) textout.write(" ");
		}
		textout.write("\n");
	}

	public void writeFrame(Complex[] frame) throws IOException
	{
		this.checkFrameSize(frame.length);
		for(int i=0;i<frame.length;i++)
		{
			textout.write(frame[i].getReal()+" "+frame[i].getImaginary());
			if(i<frame.length-1) textout.write(" ");
		}
		textout.write("\n");
	}

	public void flush() throws IOException
	{
		textout.flush();
	}

	public void close() throws IOException
	{
		textout.close();
	}
	
	public static void main(String[] args) throws IOException
	{
	ComplexTextSignalSink sink;
//	Complex[] frame;
	double[] frame;
	
		sink=new ComplexTextSignalSink(new FileOutputStream("/home/nay0648/test.txt"),2);
//		frame=new Complex[sink.numChannels()];
		frame=new double[sink.numChannels()];
		for(int i=0;i<10;i++)
		{
//			frame[0]=new Complex(i,i+1);
//			frame[1]=new Complex(i+2,i+3);
			frame[0]=i;
			frame[1]=i+1;
			sink.writeFrame(frame);
		}
		sink.flush();
		sink.close();
	}
}
