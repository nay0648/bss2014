package cn.edu.bjtu.cit.bss.util;
import java.io.*;
import java.util.*;
import cn.edu.bjtu.cit.bss.signalio.*;

/**
 * <h1>Description</h1>
 * Convolution operations for 1D signal. In the convolution operation, 
 * filter is filpped and shifted.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Jan 26, 2011 8:43:58 AM, revision:
 */
public class Filter implements Serializable
{
private static final long serialVersionUID=3757316841636229873L;

	/**
	 * <h1>Description</h1>
	 * Padding policy for the convolution operation.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Apr 19, 2011 2:32:03 PM, revision:
	 */
	public enum Padding
	{
		/**
		 * pad with zero: ... 0, 0, 0, s1, s2, s3, 0, 0, 0...
		 */
		zero,
		/**
		 * perform mirror padding: ... s3, s3, s2, s1, s1, s2, s3, s3, s2, s1, s1...
		 */
		mirror,
		/**
		 * expand the signal segment to period signal to perform padding: 
		 * ... s1, s2, s3, s1, s2, s3, s1, s2, s3...
		 */
		periodic
	}

	/**
	 * <h1>Description</h1>
	 * A static loop queue structure used to store buffered data in 
	 * convolve operations.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Jul 16, 2010 6:22:25 PM, revision:
	 */
	public static class SampleBuffer implements Serializable
	{
	private static final long serialVersionUID=5153535467398957907L;
	private double[] buffer;
	private int head=0;

		/**
		 * @param len
		 * array length
		 */
		public SampleBuffer(int len)
		{
			buffer=new double[len];
		}
		
		/**
		 * get buffer size
		 * @return
		 */
		public int size()
		{
			return buffer.length;
		}
	
		/**
		 * get a data
		 * @param index
		 * the index
		 * @return
		 */
		public double get(int index)
		{
		int idx2;
	
			if(index<0||index>=buffer.length) 
				throw new ArrayIndexOutOfBoundsException(index+", "+buffer.length);
			idx2=(head+index)%buffer.length;
			if(idx2<0) idx2=buffer.length+idx2;
			return buffer[idx2];
		}
	
		/**
		 * add obsolete data
		 * @param value
		 * data value
		 */
		public void add(double value)
		{
			buffer[head++]=value;
			if(head>=buffer.length) head=0;
		}
		
		public String toString()
		{
		StringBuilder s;
		
			s=new StringBuilder();
			s.append("[");
			for(int i=0;i<size();i++)
			{
				s.append(get(i));
				if(i<size()-1) s.append(", ");
			}
			s.append("]");
			return s.toString();
		}
	}
	
	/**
	 * calculate the dot product between a buffered signal and a filter, 
	 * the filter is reversed
	 * @param buffer
	 * signal buffer
	 * @param filter
	 * a filter
	 * @return
	 */
	public static double dotProduct(SampleBuffer buffer,double[] filter)
	{
	double sum=0;
	
		if(buffer.size()!=filter.length) throw new IllegalArgumentException(
				"filter length not match: "+filter.length+", required: "+buffer.size());
		for(int i=0;i<buffer.size();i++) sum+=buffer.get(i)*filter[filter.length-1-i];
		return sum;
	}
	
	/**
	 * get signal sample according to padding policy
	 * @param signal
	 * the signal
	 * @param index
	 * sample index
	 * @param padding
	 * padding policy
	 * @return
	 */
	public static double getSample(double[] signal,int index,Padding padding)
	{
		switch(padding)
		{
			case zero:
			{
				if(index<0||index>=signal.length) return 0;
				else return signal[index];
			}
			case mirror:
			{
				if(index<-signal.length) return signal[signal.length-1];
				else if(index>2*signal.length-1) return signal[0];
				else
				{
					if(index<0) return signal[-index-1];
					else if(index>=signal.length) return signal[2*signal.length-index-1];
					else return signal[index];
				}
			}
			case periodic:
			{
				index=index%signal.length;
				if(index<0) index+=signal.length;
				return signal[index];
			}
			default: throw new IllegalArgumentException("unknown padding policy: "+padding);
		}
	}
	
	/**
	 * Apply a filter to a single position of a signal for the convolution operation, 
	 * the filter is flipped before use.
	 * @param signal
	 * the signal buffer
	 * @param filter
	 * the filter
	 * @param index
	 * index of the result signal
	 * @param padding
	 * padding policy
	 * @return
	 */
	public static double applyFilter(double[] signal,double[] filter,int index,Padding padding)
	{
	double sum=0;
	
		for(int i=0;i<filter.length;i++)
			sum+=getSample(signal,index-filter.length+1+i,padding)*filter[filter.length-i-1];
		return sum;
	}
	
	/**
	 * convolve a signal with a filter
	 * @param signal
	 * signal buffer
	 * @param filter
	 * the applied filter
	 * @param result
	 * destination for results, null to allocate new space
	 * @param padding
	 * padding policy
	 * @return
	 */
	public static double[] convolve(double[] signal,double[] filter,double[] result,Padding padding)
	{
		if(result==null) result=new double[signal.length+filter.length-1];
		else if(result.length!=signal.length+filter.length-1) 
			throw new IllegalArgumentException("result signal length not match: "+result.length+
					", required: "+(signal.length+filter.length-1));
		for(int i=0;i<result.length;i++) result[i]=applyFilter(signal,filter,i,padding);
		return result;
	}
	
	/**
	 * convolve a signal with a filter, and turncate at the right side to make the result 
	 * signal has the same length with the input signal
	 * @param signal
	 * a signal
	 * @param filter
	 * a filter
	 * @param result
	 * location for result signal, null to allocate new space
	 * @param padding
	 * padding policy
	 * @return
	 */
	public static double[] turncatedConvolve(double[] signal,double[] filter,double[] result,Padding padding)
	{
		SampleBuffer buffer;
		
		if(result==null) result=new double[signal.length];
		else if(result.length!=signal.length) throw new IllegalArgumentException(
				"result signal length not match: "+result.length+", required: "+signal.length);
		
		/*
		 * padding with proper data
		 */
		buffer=new SampleBuffer(filter.length);		
		for(int i=0;i<filter.length-1;i++) buffer.add(getSample(signal,i-filter.length+1,padding));
		
		//calculate the convolution
		for(int i=0;i<result.length;i++)
		{
			buffer.add(signal[i]);
			result[i]=dotProduct(buffer,filter);
		}
		return result;		
	}
	
	/**
	 * convolution operation for singal channel signal from stream
	 * @param in
	 * signal source
	 * @param out
	 * result destination
	 * @param filter
	 * applied filter
	 * @param padding
	 * padding policy
	 * @param turncated
	 * true to stop when get an eof, false to output the remaining buffered data
	 * @throws IOException
	 */
	public static void convolve(SignalSource in,SignalSink out,double[] filter,
			Padding padding,boolean turncated) throws IOException
	{
	double s;
	SampleBuffer buffer;
	List<Double> temp;
		
		if(padding==Padding.periodic) throw new IllegalArgumentException(
				"periodic padding is not supported for stream convolution");
		
		/*
		 * prepare padded data with mirror padding
		 */
		buffer=new SampleBuffer(filter.length);
		temp=new ArrayList<Double>(filter.length-1);
		for(int i=0;i<filter.length-1;i++)
		{
			try
			{
				s=in.readSample();
			}
			catch(EOFException e)
			{
				throw new IllegalStateException("signal is shorter than filter length: "+filter.length,e);
			}
			temp.add(s);
		}
		
		if(padding==Padding.zero) for(int i=0;i<buffer.size()-1;i++) buffer.add(0);
		else for(int i=temp.size()-1;i>=0;i--) buffer.add(temp.get(i));

		//process the loaded data
		for(int i=0;i<temp.size();i++)
		{
			buffer.add(temp.get(i));
			out.writeSample(dotProduct(buffer,filter));//write to stream
		}
		
		//process other data
eof:	for(;;)
		{
			try
			{
				s=in.readSample();		
			}
			catch(EOFException e)
			{
				if(turncated) return;else break eof;
			}
			buffer.add(s);
			out.writeSample(dotProduct(buffer,filter));
		}
		
		/*
		 * output the tail
		 */
		temp.clear();
		if(padding==Padding.zero) for(int i=0;i<filter.length-1;i++) temp.add(0.0);
		else for(int i=0;i<filter.length-1;i++) temp.add(buffer.get(i+1));
		
		for(int i=temp.size()-1;i>=0;i--)
		{
			buffer.add(temp.get(i));
			out.writeSample(dotProduct(buffer,filter));
		}
	}
	
	/**
	 * generate a filter has a half lobe of the standard Gaussian function
	 * @param sigma
	 * standard deviation of the Gaussian
	 * @return
	 */
	public static double[] halfGaussian(double sigma)
	{
	double[] filter;
	double amp,temp;
		
		if(sigma==0) return new double[]{1};//only one point
		//3 sigma policy
		filter=new double[(int)Math.ceil(sigma*3)+1];
		amp=1.0/(Math.sqrt(2.0*Math.PI)*sigma);//the amplitude
		temp=-1.0/(2.0*sigma*sigma);//1/(2sigma^2)
		filter[0]=amp;//the middle point
		for(int i=1;i<filter.length;i++) filter[i]=amp*Math.exp(temp*i*i);
		/*
		 * normalize
		 */
		temp=0;
		for(int i=0;i<filter.length;i++) temp+=filter[i];
		for(int i=0;i<filter.length;i++) filter[i]/=temp;
		return filter;	
	}
	
	/**
	 * calculate the correlation of two sequence: rxy(l)=sum(x(n)y(n-l))
	 * @param x, y
	 * two sequences
	 * @param l1
	 * starting index
	 * @param l2
	 * ending index
	 * @return
	 */
	public static double[] correlation(double[] x,double[] y,int l1,int l2)
	{
	double[] rxy;
	int ix,iy;
	
		rxy=new double[l2-l1+1];
		for(int l=l1;l<=l2;l++)
		{
			//indicate the starting index
			if(l>0)
			{
				ix=l;
				iy=0;
			}
			else if(l<0)
			{
				ix=0;
				iy=-l;
			}
			else
			{
				ix=0;
				iy=0;
			}
			for(;ix<x.length&&iy<y.length;ix++,iy++) rxy[l-l1]+=x[ix]*y[iy];
		}
		return rxy;
	}
	
	public static void main(String[] args) throws IOException
	{
	double[] signal={1,2,3,4,5};
	double[] filter={1,2,3,4};
	
		System.out.println(Arrays.toString(convolve(signal,filter,null,Padding.zero)));
		System.out.println(Arrays.toString(turncatedConvolve(signal,filter,null,Padding.zero)));
		
		ArraySignalSource source=new ArraySignalSource(signal);
		TextSignalSink sink=new TextSignalSink(System.out,1);
		Filter.convolve(source,sink,filter,Padding.zero,false);
		source.close();
		sink.flush();
	}
}
