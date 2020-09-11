package cn.edu.bjtu.cit.bss.recorder;
import java.io.*;
import java.util.*;
import org.apache.commons.math.complex.*;
import cn.edu.bjtu.cit.bss.signalio.*;

/**
 * <h1>Description</h1>
 * Read data from circular buffers, not synchronized with circular buffers.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Mar 17, 2012 8:05:05 AM, revision:
 */
public class CircularBufferSource extends SignalSource
{
private List<CircularBuffer> bufferlist;
private List<Iterator<Double>> itlist;
	
	/**
	 * @param buffer
	 * a buffer
	 */
	public CircularBufferSource(CircularBuffer buffer)
	{
		bufferlist=new ArrayList<CircularBuffer>(1);
		bufferlist.add(buffer);
		itlist=new ArrayList<Iterator<Double>>(bufferlist.size());
		for(CircularBuffer b:bufferlist) itlist.add(b.iterator());
	}
	
	/**
	 * @param bufferlist
	 * each buffer for a channel
	 */
	public CircularBufferSource(List<CircularBuffer> bufferlist)
	{
		this.bufferlist=new ArrayList<CircularBuffer>(bufferlist);
		itlist=new ArrayList<Iterator<Double>>(bufferlist.size());
		for(CircularBuffer b:bufferlist) itlist.add(b.iterator());
	}
		
	public int numChannels()
	{
		return bufferlist.size();
	}

	public void readFrame(double[] frame) throws IOException,EOFException
	{
	Iterator<Double> it;
		
		this.checkFrameSize(frame.length);	
			
		for(int i=0;i<frame.length;i++)
		{
			it=itlist.get(i);
			if(it.hasNext()) frame[i]=it.next();
			else throw new EOFException();
		}
	}

	public void readFrame(Complex[] frame) throws IOException,EOFException
	{
	Iterator<Double> it;
			
		this.checkFrameSize(frame.length);	
			
		for(int i=0;i<frame.length;i++)
		{
			it=itlist.get(i);
			if(it.hasNext()) frame[i]=new Complex(it.next(),0);
			else throw new EOFException();
		}
	}

	public void close() throws IOException
	{}
}
