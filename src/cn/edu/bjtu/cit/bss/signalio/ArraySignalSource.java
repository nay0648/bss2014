package cn.edu.bjtu.cit.bss.signalio;
import java.io.*;
import java.util.*;
import org.apache.commons.math.complex.*;

/**
 * <h1>Description</h1>
 * Encapsulate array into a signal source.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Mar 8, 2011 2:49:04 PM, revision:
 */
public class ArraySignalSource extends SignalSource
{
private double[][] rdata=null;
private Complex[][] cdata=null;
private int idx=0;//current index

	public ArraySignalSource(double[][] data)
	{
		rdata=data;
	}
	
	public ArraySignalSource(double[] data)
	{
		rdata=new double[1][];
		rdata[0]=data;
	}
	
	public ArraySignalSource(Complex[][] data)
	{
		cdata=data;
	}
	
	public ArraySignalSource(Complex[] data)
	{
		cdata=new Complex[1][];
		cdata[0]=data;
	}

	public int numChannels()
	{
		if(rdata!=null) return rdata.length;
		else return cdata.length;
	}

	public void readFrame(double[] frame) throws IOException,EOFException
	{
		this.checkFrameSize(frame.length);
		//read from double array
		if(rdata!=null)
		{
			if(idx>=rdata[0].length) throw new EOFException();
			for(int i=0;i<frame.length;i++) frame[i]=rdata[i][idx];
			idx++;
		}
		//read from complex array
		else
		{
			if(idx>=cdata[0].length) throw new EOFException();
			for(int i=0;i<frame.length;i++) frame[i]=cdata[i][idx].getReal();
			idx++;
		}
	}

	public void readFrame(Complex[] frame) throws IOException, EOFException
	{
		this.checkFrameSize(frame.length);
		//read from double array
		if(rdata!=null)
		{
			if(idx>=rdata[0].length) throw new EOFException();
			for(int i=0;i<frame.length;i++) frame[i]=new Complex(rdata[i][idx],0);
			idx++;
		}
		//read from complex array
		else
		{
			if(idx>=cdata[0].length) throw new EOFException();
			for(int i=0;i<frame.length;i++) frame[i]=cdata[i][idx];
			idx++;
		}
	}
	
	public void close() throws IOException
	{}
	
	public static void main(String[] args) throws IOException
	{
	double[][] data={{1,2,3,4,5},{6,7,8,9,10}};
	ArraySignalSource source;
	double[] frame;
	
		source=new ArraySignalSource(data);
		frame=new double[source.numChannels()];
		for(;;)
		{
			try
			{
				source.readFrame(frame);
			}
			catch(EOFException e)
			{
				break;
			}
			System.out.println(Arrays.toString(frame));
		}
	}
}
