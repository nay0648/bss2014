package cn.edu.bjtu.cit.bss.signalio;
import java.io.*;
import java.util.*;
import org.apache.commons.math.complex.*;

/**
 * <h1>Description</h1>
 * This represents an abstract signal source, samples can be readed from it. Both single 
 * channel signal and multichannel signal, both real signal and complex signal are supported. 
 * Not safe for multithread access.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Mar 8, 2011 11:15:44 AM, revision:
 */
public abstract class SignalSource
{
private int currentch=0;//current channel for single channel signal input
private double[] rframe=null;//a frame for real signals
private Complex[] cframe=null;//a frame for complex signals

	/**
	 * throw execpeion if size not match the channel size
	 * @param size
	 * channel size
	 */
	protected void checkFrameSize(int size)
	{
		if(size!=numChannels()) throw new IllegalArgumentException(
				"number of channels not match: "+size+", required: "+numChannels());
	}

	/**
	 * get number of channels of this signal source
	 * @return
	 */
	public abstract int numChannels();
	
	/**
	 * get current channel for single channel signal input
	 * @return
	 */
	public int getCurrentChannel()
	{
		return currentch;
	}
	
	/**
	 * set channel index for single channel signal input
	 * @param ch
	 * channel index
	 */
	public void setCurrentChannel(int ch)
	{
		if(ch<0||ch>=numChannels()) throw new IllegalArgumentException("illegal channel index: "+ch);
		currentch=ch;
	}
	
	/**
	 * read a frame from signal source
	 * @param frame
	 * buffer for frame
	 * @throws IOException
	 * @throws EOFException
	 */
	public abstract void readFrame(double[] frame) throws IOException, EOFException;
	
	/**
	 * read a complex frame from signal source
	 * @param frame
	 * buffer for frame
	 * @throws IOException
	 * @throws EOFException
	 */
	public abstract void readFrame(Complex[] frame) throws IOException, EOFException;
	
	/**
	 * close the signal source
	 * @throws IOException
	 */
	public abstract void close() throws IOException;
	
	public void finalize()
	{
		try
		{
			close();
		}
		catch(IOException e)
		{}
	}
	
	/**
	 * read a real frame into buffer
	 * @throws EOFException
	 * @throws IOException
	 */
	private void readFrame() throws EOFException, IOException
	{
		if(rframe==null) rframe=new double[numChannels()];
		readFrame(rframe);
	}
	
	/**
	 * read a complex frame into buffer
	 * @throws IOException 
	 * @throws EOFException
	 */
	private void readComplexFrame() throws EOFException, IOException
	{
		if(cframe==null) cframe=new Complex[numChannels()];
		readFrame(cframe);
	}
	
	/**
	 * read a sample of current channel from signal source
	 * @return
	 * @throws IOException
	 * @throws EOFException
	 */
	public double readSample() throws IOException, EOFException
	{
		readFrame();
		return rframe[currentch];
	}
	
	/**
	 * read a complex sample of current channel from signal source
	 * @return
	 * @throws IOException
	 * @throws EOFException
	 */
	public Complex readComplexSample() throws IOException, EOFException
	{
		readComplexFrame();
		return cframe[currentch];
	}
	
	/**
	 * read samples from signal source
	 * @param buffer
	 * destination buffer used to store loaded data
	 * @param offset
	 * the start offset of the data
	 * @param len
	 * the number of samples to read
	 * @return
	 * Number of actually loaded samples, or -1 if get an EOF.
	 * @throws IOException
	 */
	public int readSamples(double[] buffer,int offset,int len) throws IOException
	{
	int count=0;
	
		for(int i=offset;i<offset+len;i++)
		{
			try
			{
				readFrame();
			}
			catch(EOFException e)
			{
				if(count>0) return count;else return -1;
			}
			buffer[i]=rframe[currentch];
			count++;
		}
		return count;
	}
	
	/**
	 * read samples from signal source
	 * @param buffer
	 * space used to store samples
	 * @return
	 * Number of actually loaded samples, or -1 if get an EOF.
	 * @throws IOException
	 */
	public int readSamples(double[] buffer) throws IOException
	{
		return readSamples(buffer,0,buffer.length);
	}
	
	/**
	 * Read samples for mulitchannel signal, signals in stream should be arranged as: 
	 * s11, s21, ..., sm1, s12, s22, ..., sm2, ..., s1n, s2n, ..., smn, where m is 
	 * channel index, and n is sample index.
	 * @param buffer
	 * each row for a channel
	 * @return
	 * Number of loaded samples of each channel, or -1 if get an EOF.
	 * @throws IOException
	 */
	public int readSamples(double[][] buffer,int offset,int len) throws IOException
	{
	int count=0;
	
		checkFrameSize(buffer.length);
		for(int n=offset;n<offset+len;n++)
		{
			try
			{
				readFrame();
			}
			catch(EOFException e)
			{
				if(count>0) return count;else return -1;
			}
			for(int m=0;m<buffer.length;m++) buffer[m][n]=rframe[m];
			count++;
		}
		return count;
	}
	
	/**
	 * read multichannel signals
	 * @param buffer
	 * signal buffer, each row for a channel
	 * @return
	 * number of samples read, or -1 if get an eof
	 * @throws IOException
	 */
	public int readSamples(double[][] buffer) throws IOException
	{
		return readSamples(buffer,0,buffer[0].length);
	}
	
	/**
	 * read complex samples from signal source
	 * @param buffer
	 * data buffer used to store samples
	 * @param offset
	 * the start position for loaded samples
	 * @param len
	 * length expected to be loaded
	 * @return
	 * Actual number of loaded samples, or -1 if meet an EOF.
	 * @throws IOException
	 */
	public int readSamples(Complex[] buffer,int offset,int len) throws IOException
	{
	int count=0;
	
		for(int i=offset;i<offset+len;i++)
		{
			try
			{
				readComplexFrame();
			}
			catch(EOFException e)
			{
				if(count>0) return count;else return -1;
			}
			buffer[i]=cframe[currentch];
			count++;
		}
		return count;
	}
	
	/**
	 * read complex samples from signal source
	 * @param buffer
	 * data buffer used to store samples
	 * @return
	 * Actual number of loaded samples, or -1 if meet an EOF.
	 * @throws IOException
	 */
	public int readSamples(Complex[] buffer) throws IOException
	{
		return readSamples(buffer,0,buffer.length);
	}
	
	/**
	 * read multichannel complex signals
	 * @param buffer
	 * signal buffer, each row for a channel
	 * @param offset
	 * the start position
	 * @param len
	 * number of frames want to read
	 * @return
	 * actual loaded frames, or -1 if get an EOF
	 * @throws IOException
	 */
	public int readSamples(Complex[][] buffer,int offset,int len) throws IOException
	{
	int count=0;
	
		checkFrameSize(buffer.length);
		for(int n=offset;n<offset+len;n++)
		{
			try
			{
				readComplexFrame();
			}
			catch(EOFException e)
			{
				if(count>0) return count;else return -1;
			}
			for(int m=0;m<cframe.length;m++) buffer[m][n]=cframe[m];
			count++;
		}
		return count;
	}
	
	/**
	 * read multichannel complex signals
	 * @param buffer
	 * signal buffer
	 * @return
	 * actual loaded frames, or -1 if get an EOF
	 * @throws IOException
	 */
	public int readSamples(Complex[][] buffer) throws IOException
	{
		return readSamples(buffer,0,buffer[0].length);
	}
	
	/**
	 * cache current channel of signal data into buffer
	 * @param buffer
	 * buffer used to cache data, null to allocate new space
	 * @return
	 * @throws IOException
	 */
	public double[] toArray(double[] buffer) throws IOException
	{
		if(buffer==null)
		{
		List<Double> buff;
		int idx=0;
		
			buff=new LinkedList<Double>();
			for(;;)
			{
				try
				{
					readFrame();
				}
				catch(EOFException e)
				{
					break;
				}
				buff.add(rframe[currentch]);
			}
			buffer=new double[buff.size()];
			for(Double s:buff) buffer[idx++]=s;
		}
		else
		{
			for(int i=0;i<buffer.length;i++)
			{
				try
				{
					readFrame();
				}
				catch(EOFException e)
				{
					break;
				}
				buffer[i]=rframe[currentch];
			}
		}
		return buffer;
	}

	/**
	 * cache current channel of signal data into buffer
	 * @param buffer
	 * buffer used to cache data, null to allocate new space
	 * @return
	 * @throws IOException
	 */
	public Complex[] toArray(Complex[] buffer) throws IOException
	{	
		if(buffer==null)
		{
		List<Complex> buff;
		int idx=0;
		
			buff=new LinkedList<Complex>();
			for(;;)
			{
				try
				{
					readComplexFrame();
				}
				catch(EOFException e)
				{
					break;
				}
				buff.add(cframe[currentch]);
			}
			buffer=new Complex[buff.size()];
			for(Complex s:buff) buffer[idx++]=s;
		}
		else
		{
			for(int i=0;i<buffer.length;i++)
			{
				try
				{
					readComplexFrame();
				}
				catch(EOFException e)
				{
					break;
				}
				buffer[i]=cframe[currentch];
			}
		}
		return buffer;
	}
	
	/**
	 * cache signals into array
	 * @param buffer
	 * buffer for signal data, each row for a channel, null to allocate new space
	 * @return
	 * @throws IOException
	 */
	public double[][] toArray(double[][] buffer) throws IOException
	{
		if(buffer==null)
		{
		List<List<Double>> buff;
		int idx=0;
		
			buff=new ArrayList<List<Double>>(numChannels());
			for(int i=0;i<numChannels();i++) buff.add(new LinkedList<Double>());
			for(;;)
			{
				try
				{
					readFrame();
				}
				catch(EOFException e)
				{
					break;
				}
				for(int i=0;i<buff.size();i++) buff.get(i).add(rframe[i]);
			}
			buffer=new double[buff.size()][buff.get(0).size()];
			for(int i=0;i<buff.size();i++)
			{
				idx=0;
				for(Double s:buff.get(i)) buffer[i][idx++]=s;
			}
		}
		else
		{
			checkFrameSize(buffer.length);
			for(int i=0;i<buffer[0].length;i++)
			{
				try
				{
					readFrame();
				}
				catch(EOFException e)
				{
					break;
				}
				for(int ii=0;ii<buffer.length;ii++) buffer[ii][i]=rframe[ii];
			}
		}
		return buffer;
	}
	
	/**
	 * cache signals into array
	 * @param buffer
	 * buffer for signal data, each row for a channel, null to allocate new space
	 * @return
	 * @throws IOException
	 */
	public Complex[][] toArray(Complex[][] buffer) throws IOException
	{
		if(buffer==null)
		{
		List<List<Complex>> buff;
		int idx=0;
		
			buff=new ArrayList<List<Complex>>(numChannels());
			for(int i=0;i<numChannels();i++) buff.add(new LinkedList<Complex>());
			for(;;)
			{
				try
				{
					readComplexFrame();
				}
				catch(EOFException e)
				{
					break;
				}
				for(int i=0;i<buff.size();i++) buff.get(i).add(cframe[i]);
			}
			buffer=new Complex[buff.size()][buff.get(0).size()];
			for(int i=0;i<buff.size();i++)
			{
				idx=0;
				for(Complex s:buff.get(i)) buffer[i][idx++]=s;
			}
		}
		else
		{
			checkFrameSize(buffer.length);
			for(int i=0;i<buffer[0].length;i++)
			{
				try
				{
					readComplexFrame();
				}
				catch(EOFException e)
				{
					break;
				}
				for(int ii=0;ii<buffer.length;ii++) buffer[ii][i]=cframe[ii];
			}
		}
		return buffer;
	}
}
