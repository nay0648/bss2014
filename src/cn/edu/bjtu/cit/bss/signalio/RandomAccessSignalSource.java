package cn.edu.bjtu.cit.bss.signalio;
import java.io.*;
import org.apache.commons.math.complex.*;

/**
 * <h1>Description</h1>
 * Used to achieve random read from a raw signal file.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Mar 15, 2011 9:17:25 AM, revision:
 */
public class RandomAccessSignalSource extends SignalSource
{
private File path;//signal file path
private RandomAccessFile file=null;
private int numch;//number of channels

	/**
	 * @param path
	 * signal file path
	 * @param numch
	 * number of channels
	 * @throws IOException
	 */
	public RandomAccessSignalSource(File path,int numch) throws IOException
	{
		this.path=path;
		file=new RandomAccessFile(path,"r");
		this.numch=numch;
	}
	
	public void close() throws IOException
	{
		file.close();
	}
	
	/**
	 * get signal file path
	 * @return
	 */
	public File path()
	{
		return path;
	}
	
	public int numChannels()
	{
		return numch;
	}

	public void readFrame(double[] frame) throws IOException,EOFException
	{
		this.checkFrameSize(frame.length);
		for(int i=0;i<frame.length;i++) frame[i]=file.readDouble();
	}
	
	/**
	 * seek frame index
	 * @param frameidx
	 * destination frame index
	 * @param complex
	 * true to seek complex frames, false to seek real frames
	 * @throws IOException
	 */
	public void seek(long frameidx,boolean complex) throws IOException
	{
		if(complex) file.seek(frameidx*16);
		else file.seek(frameidx*8);
	}

	public void readFrame(Complex[] frame) throws IOException,EOFException
	{
	double real,imag;
	
		this.checkFrameSize(frame.length);
		for(int i=0;i<frame.length;i++)
		{
			real=file.readDouble();
			imag=file.readDouble();
			frame[i]=new Complex(real,imag);
		}
	}
}
