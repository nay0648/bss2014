package cn.edu.bjtu.cit.bss.signalio;
import java.io.*;
import java.util.*;
import org.apache.commons.math.complex.*;

/**
 * <h1>Description</h1>
 * This is used to concatenate multiple signal sources together to form a 
 * single signal source, instaneous and convolutive mix with zero padding 
 * are also supported to mix signals from different channels. Only real 
 * signals are supported. This class is not safe for multithread access.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Mar 18, 2011 3:41:39 PM, revision:
 */
public class SignalMixer extends SignalSource
{
private SignalSource[] source;//underlying signal sources
private int numchin=0;//number of input channels
private double[][] buffr;//buffer for underlying sources, each row is a frame
private Complex[][] buffc;//buffer for underlying sources, each row is a frame
private double[] tempr;//temp frame used to concatenate small frames
private Complex[] tempc;//temp frame used to concatenate small frames
private Mixer mixer=null;//the mixer

	/**
	 * <h1>Description</h1>
	 * Used to mix signals from different channels
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Apr 20, 2011 2:42:52 PM, revision:
	 */
	public static interface Mixer extends Serializable
	{
		/**
		 * get number of input channels
		 * @return
		 */
		public int numInputChannels();
		
		/**
		 * get number of output channels
		 * @return
		 */
		public int numOutputChannels();
		
		/**
		 * mix a real frame
		 * @param framein
		 * input frame, null means got an eof in input channels
		 * @param frameout
		 * space for output frame
		 * @throws EOFException
		 */
		public void mix(double[] framein,double[] frameout) throws EOFException;
		
		/**
		 * mix a complex frame
		 * @param framein
		 * input frame, null means got an eof in input channels
		 * @param frameout
		 * space for output frame
		 * @throws EOFException
		 */
		public void mix(Complex[] framein,Complex[] frameout) throws EOFException;
	}

	/**
	 * <h1>Description</h1>
	 * Used for instantaneous mixing.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Apr 20, 2011 2:49:50 PM, revision:
	 */
	public static class InstantaneousMixer implements Mixer
	{
	private static final long serialVersionUID=829662802565871243L;
	private double[][] rmix;
	private Complex[][] cmix;
	
		/**
		 * @param mix
		 * the mixing matrix
		 */
		public InstantaneousMixer(double[][] mix)
		{
			rmix=mix;
			cmix=new Complex[rmix.length][rmix[0].length];
			for(int i=0;i<cmix.length;i++)
				for(int j=0;j<cmix[i].length;j++) cmix[i][j]=new Complex(rmix[i][j],0);
		}
		
		/**
		 * @param mix
		 * the mixing matrix
		 */
		public InstantaneousMixer(Complex[][] mix)
		{
			cmix=mix;
			rmix=new double[cmix.length][cmix[0].length];
			for(int i=0;i<rmix.length;i++)
				for(int j=0;j<rmix[i].length;j++) rmix[i][j]=cmix[i][j].getReal();
		}
	
		public int numInputChannels()
		{
			return rmix[0].length;
		}

		public int numOutputChannels()
		{
			return rmix.length;
		}

		public void mix(double[] framein,double[] frameout) throws EOFException
		{
			if(framein==null) throw new EOFException();
			for(int i=0;i<rmix.length;i++)
			{
				frameout[i]=0;
				for(int j=0;j<rmix[i].length;j++) 
					frameout[i]+=rmix[i][j]*framein[j];
			}
		}

		public void mix(Complex[] framein,Complex[] frameout) throws EOFException
		{
			if(framein==null) throw new EOFException();
			for(int i=0;i<cmix.length;i++)
			{
				frameout[i]=new Complex(0,0);
				for(int j=0;j<cmix[i].length;j++)
					frameout[i]=frameout[i].add(cmix[i][j].multiply(framein[j]));
			}
		}
	}
	
	/**
	 * <h1>Description</h1>
	 * Used for mixing signals in convolutive manner.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Apr 20, 2011 3:27:33 PM, revision:
	 */
	public static class ConvolutiveMixer implements Mixer
	{
	private static final long serialVersionUID=1382357296586092135L;
	private double[][][] rmix;//real mixing filters
	private Complex[][][] cmix;//complex mixing filters
	private List<List<Double>> rbuffer;//real sample buffer
	private List<List<Complex>> cbuffer;//complex sample buffer
	private int eofcount=0;//count the number of eofs from substreams
	private boolean turncated;//true to stop if get an eof, false to output the remaining data
	
		/**
		 * initialize sample buffer with zero padding
		 */
		private void initSampleBuffer()
		{
		List<Double> rtemp;
		List<Complex> ctemp;
		
			rbuffer=new ArrayList<List<Double>>(numInputChannels());
			for(int i=0;i<numInputChannels();i++)
			{
				rtemp=new LinkedList<Double>();
				for(int j=0;j<filterLength();j++) rtemp.add(0.0);
				rbuffer.add(rtemp);
			}
			
			cbuffer=new ArrayList<List<Complex>>(numInputChannels());
			for(int i=0;i<numInputChannels();i++)
			{
				ctemp=new LinkedList<Complex>();
				for(int j=0;j<filterLength();j++) ctemp.add(new Complex(0,0));
				cbuffer.add(ctemp);
			}
		}
	
		/**
		 * @param mix
		 * the mixing filter
		 * @param turncated
		 * true to stop if get an eof, false to output the remaining data
		 */
		public ConvolutiveMixer(double[][][] mix,boolean turncated)
		{
			rmix=mix;
			cmix=new Complex[rmix.length][rmix[0].length][rmix[0][0].length];
			for(int i=0;i<cmix.length;i++)
				for(int j=0;j<cmix[i].length;j++)
					for(int k=0;k<cmix[i][j].length;k++) cmix[i][j][k]=new Complex(rmix[i][j][k],0);
			this.turncated=turncated;
			
			initSampleBuffer();
		}
		
		/**
		 * @param mix
		 * the mixing filter
		 * @param turncated
		 * true to stop if get an eof, false to output the remaining data
		 */
		public ConvolutiveMixer(Complex[][][] mix,boolean turncated)
		{
			cmix=mix;
			rmix=new double[cmix.length][cmix[0].length][cmix[0][0].length];
			for(int i=0;i<rmix.length;i++)
				for(int j=0;j<rmix[i].length;j++)
					for(int k=0;k<rmix[i][j].length;k++) rmix[i][j][k]=cmix[i][j][k].getReal();
			this.turncated=turncated;
			
			initSampleBuffer();
		}
		
		/**
		 * to see if the convolution operation is need to be turncated
		 * @return
		 * true to stop if get an eof, false to output the remaining data
		 */
		public boolean turncated()
		{
			return turncated;
		}
		
		/**
		 * get mixing filter length
		 * @return
		 */
		public int filterLength()
		{
			return rmix[0][0].length;
		}

		public int numInputChannels()
		{
			return rmix[0].length;
		}

		public int numOutputChannels()
		{
			return rmix.length;
		}
		
		/**
		 * perform dot product, the filter is flipped
		 * @param buffer
		 * sample buffer
		 * @param filter
		 * filter need to applied
		 * @return
		 */
		private double dotProduct(List<Double> buffer,double[] filter)
		{
		double sum=0;
		int idx=0;
		
			for(double s:buffer) sum+=s*filter[filter.length-1-(idx++)];
			return sum;
		}
		
		/**
		 * perform dot product, the filter is flipped
		 * @param buffer
		 * sample buffer
		 * @param filter
		 * filter need to applied
		 * @return
		 */
		private Complex dotProduct(List<Complex> buffer,Complex[] filter)
		{
		Complex sum;
		int idx=0;
		
			sum=new Complex(0,0);
			for(Complex s:buffer) sum=sum.add(s.multiply(filter[filter.length-1-(idx++)]));
			return sum;
		}

		public void mix(double[] framein,double[] frameout) throws EOFException
		{
			if(framein==null)
			{
				if(turncated||(eofcount++)>=filterLength()-1) throw new EOFException();
				
				//pad with zero
				for(int i=0;i<numInputChannels();i++)
				{
					rbuffer.get(i).remove(0);
					rbuffer.get(i).add(0.0);
					cbuffer.get(i).remove(0);
					cbuffer.get(i).add(new Complex(0,0));
				}				
			}
			//perform normal convolution
			else
			{
				//add sample
				for(int i=0;i<numInputChannels();i++)
				{
					rbuffer.get(i).remove(0);
					rbuffer.get(i).add(framein[i]);
					cbuffer.get(i).remove(0);
					cbuffer.get(i).add(new Complex(framein[i],0));
				}
			}
				
			//perform dot product
			for(int i=0;i<frameout.length;i++)
			{
				frameout[i]=0;
				for(int j=0;j<numInputChannels();j++) 
					frameout[i]+=dotProduct(rbuffer.get(j),rmix[i][j]);
			}
		}

		public void mix(Complex[] framein,Complex[] frameout) throws EOFException
		{
			if(framein==null)
			{
				if(turncated||(eofcount++)>=filterLength()-1) throw new EOFException();
				
				//pad with zero
				for(int i=0;i<numInputChannels();i++)
				{
					rbuffer.get(i).remove(0);
					rbuffer.get(i).add(0.0);
					cbuffer.get(i).remove(0);
					cbuffer.get(i).add(new Complex(0,0));
				}
			}
			//perform normal convolution
			else
			{
				//add sample
				for(int i=0;i<numInputChannels();i++)
				{
					rbuffer.get(i).remove(0);
					rbuffer.get(i).add(framein[i].getReal());
					cbuffer.get(i).remove(0);
					cbuffer.get(i).add(framein[i]);
				}
			}
				
			//perform dot product
			for(int i=0;i<frameout.length;i++)
			{
				frameout[i]=new Complex(0,0);
				for(int j=0;j<numInputChannels();j++) 
					frameout[i]=frameout[i].add(dotProduct(cbuffer.get(j),cmix[i][j]));
			}
		}
	}

	/**
	 * initialize buffers
	 * @param source
	 */
	private void initialize(SignalSource... source)
	{
		this.source=source;
		buffr=new double[source.length][];
		buffc=new Complex[source.length][];
		for(int i=0;i<source.length;i++)
		{
			numchin+=source[i].numChannels();
			buffr[i]=new double[source[i].numChannels()];
			buffc[i]=new Complex[source[i].numChannels()];
		}
		tempr=new double[numchin];
		tempc=new Complex[numchin];
	}

	/**
	 * @param imix
	 * mixer used to mix signals from different channels
	 * @param source
	 * input signal sources
	 */
	public SignalMixer(double[][] imix,SignalSource... source)
	{
		initialize(source);
		setMixer(new InstantaneousMixer(imix));
	}
	
	/**
	 * @param cmix
	 * Convolutive mixing filters, the 1st index is for output 
	 * channels, the 2nd index is for input channels, the 3rd 
	 * index is filter sample index.
	 * @param turncated
	 * true to stop if get an eof, false to output the remaining data
	 * @param source
	 * underlying signal sources
	 */
	public SignalMixer(double[][][] cmix,boolean turncated,SignalSource... source)
	{
		initialize(source);
		setMixer(new ConvolutiveMixer(cmix,turncated));
	}
	
	/**
	 * @param source
	 * signal sources
	 */
	public SignalMixer(SignalSource... source)
	{
		initialize(source);
	}

	public int numChannels()
	{
		if(mixer!=null) return mixer.numOutputChannels();
		else return numchin;
	}
	
	/**
	 * get the number of input channels
	 * @return
	 */
	public int numInputChannels()
	{
		return numchin;
	}
	
	/**
	 * get mixer used to mix signals
	 * @return
	 */
	public Mixer getMixer()
	{
		return mixer;
	}
	
	/**
	 * set mixer used to mixer signals
	 * @param mixer
	 * null to cancel mixer
	 */
	public void setMixer(Mixer mixer)
	{
		this.mixer=mixer;
		if(mixer!=null&&mixer.numInputChannels()!=numInputChannels()) throw new IllegalArgumentException(
				"number of input channels not match: "+mixer.numInputChannels()+", required: "+numInputChannels());
	}

	public void readFrame(double[] frame) throws IOException,EOFException
	{
	int idx=0;
	
		this.checkFrameSize(frame.length);
		
		//concatenate underlying source's frame into a big frame
		try
		{
			for(int i=0;i<source.length;i++)
			{
				source[i].readFrame(buffr[i]);
				for(int j=0;j<buffr[i].length;j++) tempr[idx++]=buffr[i][j];
			}
		}
		catch(EOFException e)
		{
			tempr=null;
		}
		
		if(mixer!=null) mixer.mix(tempr,frame);//mix signals
		else if(tempr==null) throw new EOFException();
		else System.arraycopy(tempr,0,frame,0,frame.length);//no mixer is applied
	}

	public void readFrame(Complex[] frame) throws IOException,EOFException
	{
	int idx=0;
		
		this.checkFrameSize(frame.length);
			
		//concatenate underlying source's frame into a big frame
		try
		{
			for(int i=0;i<source.length;i++)
			{
				source[i].readFrame(buffc[i]);
				for(int j=0;j<buffc[i].length;j++) tempc[idx++]=buffc[i][j];
			}
		}
		catch(EOFException e)
		{
			tempc=null;
		}
		
		if(mixer!=null) mixer.mix(tempc,frame);//mix signals
		else if(tempr==null) throw new EOFException();
		else System.arraycopy(tempr,0,frame,0,frame.length);//no mixer is applied
	}

	public void close() throws IOException
	{
		for(SignalSource s:source) s.close();	
	}
}
