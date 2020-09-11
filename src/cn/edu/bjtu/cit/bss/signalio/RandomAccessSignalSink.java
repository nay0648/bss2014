package cn.edu.bjtu.cit.bss.signalio;
import java.io.*;
import org.apache.commons.math.complex.*;
import pp.util.BLAS;

/**
 * <h1>Description</h1>
 * Used to achieve random writing to a raw signal file. Output samples to 
 * a single channel will not affect other data of other channels.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Mar 15, 2011 9:30:14 AM, revision:
 */
public class RandomAccessSignalSink extends SignalSink
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
	public RandomAccessSignalSink(File path,int numch) throws IOException
	{
		this.path=path;
		file=new RandomAccessFile(path,"rw");
		this.numch=numch;
	}
	
	public void flush() throws IOException
	{}

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

	public void writeFrame(double[] frame) throws IOException
	{
		this.checkFrameSize(frame.length);
		for(double s:frame) file.writeDouble(s);
	}

	public void writeFrame(Complex[] frame) throws IOException
	{
		this.checkFrameSize(frame.length);
		for(Complex s:frame)
		{
			file.writeDouble(s.getReal());
			file.writeDouble(s.getImaginary());
		}
	}
	
	/**
	 * write a sample to current channel, data of other channels will not affected
	 * @param
	 * sample value
	 * @throws IOException
	 */
	public void writeSample(double s) throws IOException
	{
		file.seek(file.getFilePointer()+this.getCurrentChannel()*8);
		file.writeDouble(s);
		file.seek(file.getFilePointer()+(numch-1-this.getCurrentChannel())*8);
	}
	
	/**
	 * write a sample to current channel, data of other channels will not affected
	 * @param
	 * sample value
	 * @throws IOException
	 */
	public void writeComplexSample(Complex s) throws IOException
	{
		file.seek(file.getFilePointer()+this.getCurrentChannel()*16);
		file.writeDouble(s.getReal());
		file.writeDouble(s.getImaginary());
		file.seek(file.getFilePointer()+(numch-1-this.getCurrentChannel())*16);
	}
	
	/**
	 * write samples to current channel, data of other channels will not affected
	 * @param buffer
	 * buffer contains signal samples
	 * @param offset
	 * the start position
	 * @param len
	 * length of samples need to be written
	 * @throws IOException
	 */
	public void writeSamples(double[] buffer,int offset,int len) throws IOException
	{
		for(int i=offset;i<offset+len;i++) writeSample(buffer[i]);
	}
	
	/**
	 * write samples to current channel, data of other channels will not affected
	 * @param buffer
	 * signal buffer contains samples
	 * @param offset
	 * the start position
	 * @param len
	 * length of samples to be written
	 * @throws IOException
	 */
	public void writeSamples(Complex[] buffer,int offset,int len) throws IOException
	{
		for(int i=offset;i<offset+len;i++) writeComplexSample(buffer[i]);
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
	
	/**
	 * set file length
	 * @param len
	 * file length in bytes
	 * @throws IOException
	 */
	public void setLength(long len) throws IOException
	{
		file.setLength(len);
	}
	
	public static void main(String[] args) throws IOException
	{
		RandomAccessSignalSink sink=new RandomAccessSignalSink(
				new File("/home/nay0648/randomaccess.dat"),2);
		sink.writeFrame(new double[]{1,6});
		sink.writeFrame(new double[]{2,7});
		sink.writeFrame(new double[]{3,8});
		sink.writeFrame(new double[]{4,9});
		sink.writeFrame(new double[]{5,10});
		sink.close();
		
		sink=new RandomAccessSignalSink(new File("/home/nay0648/randomaccess.dat"),2);
		sink.setCurrentChannel(1);
		sink.writeSample(11);
		sink.writeSample(12);
		sink.writeSample(13);
		sink.writeSample(14);
		sink.writeSample(15);
		sink.close();

		RandomAccessSignalSource source=new RandomAccessSignalSource(
				new File("/home/nay0648/randomaccess.dat"),2);
		System.out.println(BLAS.toString(source.toArray((double[][])null)));
		source.close();
	}
}
