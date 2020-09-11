package cn.edu.bjtu.cit.bss.recorder;
import java.io.*;
import java.util.*;

/**
 * <h1>Description</h1>
 * Circular buffer used to cache latest samples, it is not safe for multithread access.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Mar 11, 2012 8:59:29 PM, revision:
 */
public class CircularBuffer implements Serializable, Iterable<Double>
{
private static final long serialVersionUID=8753018513463367366L;
private double[] buffer;//the underlying buffer
private int rear=0;//the rear pointer, the next byte will be placed here
private long latestidx;//latest sample index

	/**
	 * @param buffersize
	 * buffer size in number of samples
	 */
	public CircularBuffer(int buffersize)
	{
		buffer=new double[buffersize];
		latestidx=buffersize-1;//to the last sample of the buffer
	}
	
	/**
	 * get buffer size in number of samples
	 * @return
	 */
	public int getBufferSize()
	{
		return buffer.length;
	}
	
	/**
	 * set buffer size in number of samples
	 * @param size
	 * new buffer size
	 */
	public void setBufferSize(int size)
	{
	double[] buffer2;
	
		buffer2=new double[size];
		
		//copy data
		for(int i=0;i<Math.min(getBufferSize(),buffer2.length);i++) buffer2[i]=getValue(i);

		buffer=buffer2;
		rear=0;
		latestidx=size-1;
	}
	
	/**
	 * get the value at specified position
	 * @param index
	 * buffer index
	 * @return
	 */
	public double getValue(int index)
	{
		if(index<0||index>=buffer.length) 
			throw new ArrayIndexOutOfBoundsException(index+", "+buffer.length);
		
		index+=rear;
		if(index>=buffer.length) index-=buffer.length;
		
		return buffer[index];
	}
	
	/**
	 * set value at specified position
	 * @param index
	 * buffer index
	 * @param value
	 * new value
	 */
	public void setValue(int index,double value)
	{
		if(index<0||index>=buffer.length) 
			throw new ArrayIndexOutOfBoundsException(index+", "+buffer.length);
		
		index+=rear;
		if(index>=buffer.length) index-=buffer.length;
		
		buffer[index]=value;
	}
	
	/**
	 * set underlying data
	 * @param data
	 * data array, not copied
	 */
	public void setData(double[] data)
	{
		buffer=data;
		rear=0;
		latestidx=data.length-1;
	}
	
	/**
	 * get the sample index which is added into buffer latestly
	 * @return
	 */
	public long latestSampleIndex()
	{
		return latestidx;
	}
	
	/**
	 * write a sample into buffer
	 * @param data
	 * a sample
	 */
	public void write(double data)
	{
		buffer[rear++]=data;
		if(rear>=buffer.length) rear=0;
		
		latestidx++;
	}
	
	/**
	 * clear the buffer
	 */
	public void clear()
	{
		Arrays.fill(buffer,0);
		rear=0;
		latestidx=buffer.length-1;
	}
	
	/**
	 * write data into the circular buffer
	 * @param data
	 * the data array
	 * @param offset
	 * data offset
	 * @param len
	 * data length
	 */
	public void write(double[] data,int offset,int len)
	{
		for(int i=offset;i<offset+len;i++) write(data[i]);
	}
	
	/**
	 * get samples in the buffer
	 * @param data
	 * data destination, null to allocate new space
	 * @return
	 */
	public double[] toArray(double[] data)
	{
	int p;
	
		if(data==null) data=new double[getBufferSize()];
		
		p=rear;
		for(int i=0;i<Math.min(getBufferSize(),data.length);i++) 
		{
			data[i]=buffer[p++];
			if(p>=buffer.length) p=0;
		}

		return data;
	}
	
	public Iterator<Double> iterator()
	{
		return new CircularBufferIterator();
	}
	
	/**
	 * <h1>Description</h1>
	 * Used to traverse the buffer.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Mar 12, 2012 10:32:38 AM, revision:
	 */
	private class CircularBufferIterator implements Iterator<Double>
	{
	private int count=0;
	private int p=rear;
	
		public boolean hasNext()
		{
			return count<getBufferSize();
		}

		public Double next()
		{
		double data;
		
			data=buffer[p++];
			if(p>=buffer.length) p=0;
			count++;
			return data;
		}

		public void remove()
		{
			throw new UnsupportedOperationException();
		}
	}
	
	public static void main(String[] args)
	{
	CircularBuffer buffer;
	double[] data;
	
		buffer=new CircularBuffer(5);
		for(int i=0;i<7;i++) buffer.write((byte)i);
		System.out.println(Arrays.toString(buffer.buffer));
		
		data=new double[buffer.getBufferSize()];
		data=buffer.toArray(data);
		System.out.println(Arrays.toString(data));
		
		for(double d:buffer) System.out.print(d+", ");
		System.out.println();
	}
}
