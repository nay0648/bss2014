package cn.edu.bjtu.cit.bss.signalio;
import java.io.*;
import java.util.*;
import org.apache.commons.math.complex.*;
import pp.util.BLAS;

/**
 * <h1>Description</h1>
 * Used to write signal into arrays.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Mar 8, 2011 3:27:32 PM, revision:
 */
public class ArraySignalSink extends SignalSink
{
private List<List<Complex>> data;//underlying data

	/**
	 * @param numch
	 * number of channels
	 */
	public ArraySignalSink(int numch)
	{
		data=new ArrayList<List<Complex>>(numch);
		for(int i=0;i<numch;i++) data.add(new LinkedList<Complex>());
	}

	public int numChannels()
	{
		return data.size();
	}

	public void writeFrame(double[] frame) throws IOException
	{
		this.checkFrameSize(frame.length);
		for(int i=0;i<frame.length;i++) data.get(i).add(new Complex(frame[i],0));
	}

	public void writeFrame(Complex[] frame) throws IOException
	{
		this.checkFrameSize(frame.length);
		for(int i=0;i<frame.length;i++) data.get(i).add(frame[i]);
	}
	
	public void flush() throws IOException
	{}
	
	public void close() throws IOException
	{}
	
	/**
	 * get the outputed signal as double array
	 * @param buffer
	 * buffer for signal, or null to allocate new space
	 * @return
	 */
	public double[][] toArray(double[][] buffer)
	{
	int j;
		
		if(buffer==null) buffer=new double[data.size()][data.get(0).size()];
		else this.checkFrameSize(buffer.length);
		for(int i=0;i<buffer.length;i++)
		{
			j=0;
			for(Complex s:data.get(i))
			{
				buffer[i][j++]=s.getReal();
				if(j>=buffer[i].length) break;
			}
		}
		return buffer;
	}
	
	/**
	 * get the outputed signal as complex array
	 * @param buffer
	 * buffer for signal, or null to allocate new space
	 * @return
	 */
	public Complex[][] toArray(Complex[][] buffer)
	{
	int j;	
		
		if(buffer==null) buffer=new Complex[data.size()][data.get(0).size()];
		else this.checkFrameSize(buffer.length);
		for(int i=0;i<buffer.length;i++)
		{
			j=0;
			for(Complex s:data.get(i))
			{
				buffer[i][j++]=s;
				if(j>=buffer[i].length) break;
			}
		}
		return buffer;
	}
	
	public static void main(String[] args) throws IOException
	{
	ArraySignalSink sink;
	double[][] buffer;
	
		sink=new ArraySignalSink(2);
		sink.writeSample(1);
		sink.writeSample(2);
		sink.writeSample(3);
		sink.writeSample(4);
		sink.writeSample(5);

		buffer=sink.toArray((double[][])null);
		System.out.println(BLAS.toString(buffer));
	}
}
