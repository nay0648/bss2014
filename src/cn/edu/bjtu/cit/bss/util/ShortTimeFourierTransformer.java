package cn.edu.bjtu.cit.bss.util;
import java.io.*;
import java.util.*;
import java.awt.Color;
import java.awt.image.*;
import javax.sound.sampled.*;
import org.apache.commons.math.complex.*;
import org.apache.commons.math.transform.*;
import pp.util.BLAS;
import cn.edu.bjtu.cit.bss.signalio.*;

/**
 * <h1>Description</h1>
 * This is used to calculate the short time Fourier transform of a signal.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Jan 18, 2011 9:36:34 PM, revision:
 */
public class ShortTimeFourierTransformer implements Serializable
{
private static final long serialVersionUID=-7691710855523349087L;
private int stftsize;//stft block size
private int stftoverlap;//stft block overlap
private int fftsize;//size of fft block
//filter applied to segments before fft, null means no filter will be used
private double[] filter=null;

	/**
	 * use default fft size
	 * @param stftsize
	 * stft block size
	 * @param stftoverlap
	 * stft block overlap in taps
	 * @param filter
	 * filter applied to the signal block, null means no filter will be applied
	 */
	public ShortTimeFourierTransformer(int stftsize,int stftoverlap,double[] filter)
	{
		this.stftsize=stftsize;
		if(stftoverlap<0||stftoverlap>=stftsize) throw new IllegalArgumentException(
				"illegal stft overlap: "+stftoverlap);
		this.stftoverlap=stftoverlap;
		this.filter=filter;
	
		fftsize=nextPowerOf2(stftsize);
	}
	
	/**
	 * use specified fft size
	 * @param stftsize
	 * stft block size
	 * @param stftoverlap
	 * stft block overlap in taps
	 * @param fftsize
	 * specified fft size
	 * @param filter
	 * filter applied to the signal block, null means no filter will be applied
	 */
	public ShortTimeFourierTransformer(int stftsize,int stftoverlap,int fftsize,double[] filter)
	{
		this(stftsize,stftoverlap,filter);
		
		if(fftsize<this.fftsize) throw new IllegalArgumentException(
				"illegal fft size: "+fftsize+", must larger than or equal to: "+this.fftsize);
		if(nextPowerOf2(fftsize)!=fftsize) throw new IllegalArgumentException(
				"fft size must powers of 2: "+fftsize);
		
		this.fftsize=fftsize;	
	}
	
	/**
	 * to see if a number is powers of 2
	 * @param p
	 * a number
	 * @return
	 */
	public static boolean isPowerOf2(int p)
	{
		return p==nextPowerOf2(p);
	}
	
	/**
	 * return next power of 2 closest to a specified integer
	 * @param p
	 * an integer
	 * @return
	 */
	public static int nextPowerOf2(int p)
	{
		return (int)Math.pow(2,Math.ceil(Math.log(p)/Math.log(2)));
	}

	/**
	 * generate a n-point Hann (Hanning) window as: 0.5*(1-cos((2*pi*x)/(n-1)))
	 * @param n
	 * number of points
	 * @return
	 */
	public static double[] hannWindow(int n)
	{
	double[] w;
	double factor;
	
		w=new double[n];
		for(int i=0;i<n;i++) w[i]=0.5*(1-Math.cos((2*Math.PI*i)/(n-1)));
		
		/*
		 * normalize to have sum(w)=n/2
		 */
		factor=n/(2.0*BLAS.sum(w));
		BLAS.scalarMultiply(factor,w,w);
		
		return w;
	}
	
	/**
	 * get stft block size
	 * @return
	 */
	public int stftSize()
	{
		return stftsize;
	}
	
	/**
	 * get stft block overlap in taps
	 * @return
	 */
	public int stftOverlap()
	{
		return stftoverlap;
	}
	
	/**
	 * get number of samples each stft block shifts: stftsize-stftoverlap
	 * @return
	 */
	public int stftShift()
	{
		return stftsize-stftoverlap;
	}
	
	/**
	 * get fft block size
	 * @return
	 */
	public int fftSize()
	{
		return fftsize;
	}
	
	/**
	 * return the scaling factor 2*stftshift/stftsize 
	 * which is used to cancel the overlap add effect in istft
	 * @return
	 */
	public double scalingFactor()
	{
		return 2.0*stftShift()/stftSize();
	}
	
	/**
	 * perform short time Fourier transform
	 * @param in
	 * signal source
	 * @param out
	 * output stream for transformed signals
	 * @return
	 * number of segments
	 * @throws IOException
	 */
	public int stft(SignalSource in,SignalSink out) throws IOException
	{
	STFTIterator it;
	Complex[] fs;
	int numsegs=0;//number of segments

		for(it=stftIterator(in);it.hasNext();)
		{
			fs=it.next();
			out.writeFrame(fs);
			numsegs++;
		}

		return numsegs;
	}
	
	/**
	 * perform stft for mulitchannel signals
	 * @param in
	 * multichannel signal source
	 * @param out
	 * each one for a channel
	 * @return
	 * @throws IOException
	 */
	public int stft(SignalSource in,SignalSink[] out) throws IOException
	{
	Complex[][] fs;
	int numblocks=0;
		
		if(in.numChannels()!=out.length) throw new IllegalArgumentException(
				"number of channels not match: "+in.numChannels()+", "+out.length);

		for(MultichannelSTFTIterator it=multichannelSTFTIterator(in);it.hasNext();)
		{
			fs=it.next();
			for(int chidx=0;chidx<out.length;chidx++) 
				out[chidx].writeFrame(fs[chidx]);
			numblocks++;
		}

		return numblocks;
	}
	
	/**
	 * perform short time Fourier transform
	 * @param in
	 * signal source
	 * @return
	 * The time-frequency data, each row is a frequency bin.
	 * @throws IOException
	 */
	public Complex[][] stft(SignalSource in) throws IOException
	{
	ArraySignalSink out=null;
	Complex[][] data=null;
	
		try
		{
			out=new ArraySignalSink(fftSize());
			stft(in,out);
			
			out.flush();
			data=out.toArray(data);
		}
		finally
		{
			try
			{
				if(out!=null) out.close();
			}
			catch(IOException e)
			{}
		}

		return data;
	}
	
	/**
	 * perform STFT experiment
	 * @param signal
	 * a signal sequence
	 * @return
	 */
	public Complex[][] stft(double[] signal)
	{
	ArraySignalSource in=null;
	Complex[][] stftdata;
			
		try
		{
			in=new ArraySignalSource(signal);
			stftdata=stft(in);
		}
		catch(IOException e)
		{
			throw new RuntimeException("failed to perform STFT",e);
		}
		finally
		{
			try
			{
				if(in!=null) in.close();
			}
			catch(IOException e)
			{}
		}
		
		return stftdata;
	}
		
	/**
	 * perform inverse short time Fourier transform
	 * @param in
	 * stft results of a signal, should be a multichannel complex signal
	 * @param out
	 * the istft result
	 * @param complex
	 * true to output complex signals, false to output real signals
	 * @return
	 * number of segments processed
	 * @throws IOException
	 */
	public int istft(SignalSource in,SignalSink out,boolean complex) throws IOException
	{
	Complex[] frame;//frame for a stft segment
	Complex[] ifft;//ifft of the stft segment
	double[] ifftr=null;//the real part of the ifft
	double[] overlapr=null;//buffer for overlapping samples
	Complex[] overlapc=null;//buffer for overlapping samples
	FastFourierTransformer fft;
	double factor;//used to cancel the windowing effect to signal magnitude
	int numsegs=0;
	
		if(in.numChannels()!=fftSize()) throw new IllegalArgumentException(
				"number of channels not match for input signal: "+in.numChannels()+", required: "+fftSize());
		
		/*
		 * initialize
		 */
		frame=new Complex[fftSize()];
		if(complex)
		{
			overlapc=new Complex[fftsize-stftShift()];
			Arrays.fill(overlapc,Complex.ZERO);
		}
		else 
		{
			ifftr=new double[fftsize];
			overlapr=new double[fftsize-stftShift()];
		}
		
		/*
		 * perform istft
		 */
		fft=new FastFourierTransformer();
		factor=scalingFactor();//real magnitude=factor x magnitude
		
		for(;;numsegs++)
		{
			//read a segment from stft
			try
			{
				in.readFrame(frame);
			}
			catch(EOFException e)
			{
				break;
			}
			
			ifft=fft.inversetransform(frame);//perform ifft
			
			if(complex)
			{
				BLAS.scalarMultiply(factor,ifft,ifft);//cancel the windowing effect
				
				//add overlapping samples
				for(int i=0;i<overlapc.length;i++) ifft[i]=ifft[i].add(overlapc[i]);
				
				//write to result stream
				out.writeSamples(ifft,0,ifft.length-overlapc.length);
				
				//copy overlapping samples for next window
				for(int i=0;i<overlapc.length;i++) overlapc[i]=ifft[ifft.length-overlapc.length+i];
			}
			else
			{
				for(int i=0;i<ifftr.length;i++) ifftr[i]=ifft[i].getReal();
				BLAS.scalarMultiply(factor,ifftr,ifftr);//cancel the windowing effect
				
				//add overlapping samples
				for(int i=0;i<overlapr.length;i++) ifftr[i]+=overlapr[i];
				
				//write to result stream
				out.writeSamples(ifftr,0,ifftr.length-overlapr.length);
				
				//copy overlapping samples for next window
				for(int i=0;i<overlapr.length;i++) overlapr[i]=ifftr[ifftr.length-overlapr.length+i];
			}
		}
		
		//write the last part
		if(numsegs>0)
		{
			if(complex) out.writeSamples(overlapc);
			else out.writeSamples(overlapr);
		}
		
		return numsegs;
	}
	
	/**
	 * perform inverse stft in memory
	 * @param stftdata
	 * stft data
	 * @return
	 */
	public double[] istft(Complex[][] stftdata)
	{
	ArraySignalSource in=null;
	ArraySignalSink out=null;
	double[][] data=null;
	
		try
		{
			in=new ArraySignalSource(stftdata);
			out=new ArraySignalSink(1);
		
			istft(in,out,false);
			
			out.flush();
			data=out.toArray(data);
		}
		catch(IOException e)
		{
			throw new RuntimeException("failed to perform stft in memory: ",e);
		}
		finally
		{
			try
			{
				if(in!=null) in.close();
			}
			catch(IOException e)
			{}
			try
			{
				if(out!=null) out.close();
			}
			catch(IOException e)
			{}
		}
		
		return data[0];
	}
	
	/**
	 * convert magnitude to dB
	 * @param p1
	 * usually sound pressure in Pa
	 * @param p0
	 * reference sound pressure, usually equals to 2e-5 Pa
	 * @return
	 */
	public static double magnitude2dB(double p1,double p0)
	{
		return 10*Math.log10((p1*p1)/(p0*p0));
	}
	
	/**
	 * convert magnitude to dB, suitable for spectrograms
	 * @param p1
	 * the magnitude
	 * @return
	 */
	public static double magnitude2dB(double p1)
	{
		return 10*Math.log10(p1*p1+1);
	}
	
	/**
	 * generate colormap for grayscale color
	 * @return
	 */
	public static Color[] colormapGray()
	{
	Color[] colormap;
	
		colormap=new Color[256];
		for(int i=0;i<colormap.length;i++) colormap[i]=new Color(i,i,i);
		return colormap;
	}
	
	/**
	 * used to draw for paper
	 * @return
	 */
	public static Color[] colormapAntigray()
	{
	Color[] colormap;
		
		colormap=new Color[256];
		for(int i=0;i<colormap.length;i++) colormap[i]=new Color(255-i,255-i,255-i);
		return colormap;
	}
	
	/**
	 * generate colormap for cold and warm color
	 * @return
	 */
	public static Color[] colormapJet()
	{
	Color[] colormap;
	double[] hsv,rgb;
	
		hsv=new double[3];
		rgb=new double[3];
		hsv[1]=1;
		colormap=new Color[240];//from blue to read
		for(int i=0;i<colormap.length;i++)
		{
			hsv[0]=(colormap.length-1-i)/360.0;//hue
			hsv[2]=0.1+0.9*(double)i/(colormap.length-1);//intensity
			pp.util.ColorSpace.hsv2RGB(hsv,rgb);
			colormap[i]=new Color((float)rgb[0],(float)rgb[1],(float)rgb[2]);
		}
		return colormap;
	}
	
	/**
	 * visualize stft results as an image
	 * @param stftdata
	 * stft results, row index is for frequency bins, column index is for time
	 * @param colormap
	 * adapted colormap
	 * @return
	 */
	public BufferedImage spectrogram(Complex[][] stftdata,Color[] colormap)
	{
	double[][] amp;
	double min=Double.MAX_VALUE,max=Double.MIN_VALUE;
	BufferedImage stftimg;
	int index;
		
		/*
		 * calculate magnitude and convert to DB, and find max and min magnitude
		 */
		//just need half of segments, and one more row for 0 DC component
		amp=new double[stftdata.length/2+1][stftdata[0].length];
		for(int i=0;i<=stftdata.length/2;i++)
			for(int j=0;j<stftdata[i].length;j++)
			{
				//including two half frequencies
				amp[amp.length-1-i][j]=magnitude2dB(2*stftdata[i][j].abs());
				if(amp[i][j]<min) min=amp[i][j];
				if(amp[i][j]>max) max=amp[i][j];
			}

		/*
		 * visualize as image
		 */
		stftimg=new BufferedImage(amp[0].length,amp.length,BufferedImage.TYPE_INT_RGB);
		max=(colormap.length-1.0)/(max-min);
		for(int y=0;y<stftimg.getHeight();y++)
			for(int x=0;x<stftimg.getWidth();x++)
			{
				/*
				 * quantize to fit for colormap
				 */
				index=(int)Math.round((amp[y][x]-min)*max);
				if(index<0) index=0;
				else if(index>colormap.length-1) index=colormap.length-1;
				stftimg.setRGB(x,y,colormap[index].getRGB());
			}
		return stftimg;
	}
	
	/**
	 * visualize stft results as an image with default colormap
	 * @param stftdata
	 * stft results, row index is for frequency bins, column index is for time
	 * @return
	 */
	public BufferedImage spectrogram(Complex[][] stftdata)
	{
		return spectrogram(stftdata,colormapJet());
	}
	
	/**
	 * visualize stft results as an image
	 * @param in
	 * signal source
	 * @return
	 */
	public BufferedImage spectrogram(SignalSource in) throws IOException
	{
	ArraySignalSink out;
	Complex[][] fs;//stft result
	
		/*
		 * perform stft and cache results into memory
		 */
		out=new ArraySignalSink(fftSize());
		stft(in,out);
		fs=out.toArray((Complex[][])null);
		return spectrogram(fs);
	}
	
	/**
	 * get an iterator to perform stft iteratively
	 * @param in
	 * signal source
	 * @return
	 * @throws IOException
	 */
	public STFTIterator stftIterator(SignalSource in) throws IOException
	{
		return new STFTIterator(in);
	}
	
	/**
	 * get an iterator to perform stft iteratively
	 * @param in
	 * a multichannel signal source
	 * @return
	 * @throws IOException
	 */
	public MultichannelSTFTIterator multichannelSTFTIterator(SignalSource in) throws IOException
	{
		return new MultichannelSTFTIterator(in);
	}
	
	/**
	 * <h1>Description</h1>
	 * Used to perform stft iteratively.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Jan 24, 2011 7:54:01 AM, revision:
	 */
	public class STFTIterator implements Iterator<Complex[]>
	{
	private SignalSource in;//signal source
	private double[] window;//the Hanning window
	private double[] buffer;//buffer used to load samples
	private double[] signal;//signal for fft
	private FastFourierTransformer fft;
	private long sampleidx=0;//max sample index processed
	//true means next() should be called, false means hasNext() should be called
	private boolean callnext=false;

		/**
		 * @param in
		 * signal source
		 */
		public STFTIterator(SignalSource in) throws IOException
		{
		int count;
		
			this.in=in;
			window=hannWindow(stftsize);//generate Hanning window
			buffer=new double[stftsize];//used to read samples
			//used to perform fft, has the length of next power of 2 of the signal segment size
			signal=new double[fftSize()];
			fft=new FastFourierTransformer();
			//read overlapping samples for the first segment
			count=in.readSamples(buffer,0,stftoverlap);
			if(count>0) sampleidx+=count;
		}

		/**
		 * @throws RuntimeException
		 * caused by IOException
		 */
		public boolean hasNext()
		{
		int count;
		
			if(callnext) throw new IllegalStateException(
					"the next() method should be called before call this method");
			callnext=true;
			
			/*
			 * read samples
			 */
			try
			{
				count=in.readSamples(buffer,stftoverlap,buffer.length-stftoverlap);
			}
			catch(IOException e)
			{
				throw new RuntimeException("failed to read signal",e);
			}
			if(count<0) return false;
			sampleidx+=count;//accumulate sample index
			
			/*
			 * prepare to perform fft
			 */
			if(filter==null) System.arraycopy(buffer,0,signal,0,buffer.length);//copy samples
			//apply filter to reduce noise, mirror padding is used
			else for(int i=0;i<buffer.length;i++) signal[i]=Filter.applyFilter(buffer,filter,i,Filter.Padding.mirror);
			
			//pad with 0
			for(int i=stftoverlap+count;i<signal.length;i++) signal[i]=0;
			
			//modulate by window function
			for(int i=0;i<window.length;i++) signal[i]*=window[i];
			
			//produce overlapping with next segment
			for(int i=0;i<stftoverlap;i++) buffer[i]=buffer[buffer.length-stftoverlap+i];
			
			return true;
		}

		public Complex[] next()
		{
			if(!callnext) throw new IllegalStateException(
					"the hasNext() method should be called before call this method");
			callnext=false;
			return fft.transform(signal);//perform fft
		}

		public void remove()
		{
			throw new UnsupportedOperationException("remove() is not supported");			
		}
		
		/**
		 * get max sample index already processed
		 * @return
		 */
		public long sampleIndex()
		{
			return sampleidx;
		}
	}
	
	/**
	 * <h1>Description</h1>
	 * Perform stft iteratively for multichannel signals. Not safe for multithread access.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Mar 20, 2012 9:15:21 PM, revision:
	 */
	public class MultichannelSTFTIterator implements Iterator<Complex[][]>
	{
	private SignalSource in;//signal source
	private double[] window;//the Hamming window
	private double[][] buffer;//buffer used to load samples, each row for a channel
	private double[] signal;//signal for fft
	private FastFourierTransformer fft;
	private long sampleidx=0;//max sample index processed
	//true means next() should be called, false means hasNext() should be called
	private boolean callnext=false;	
	private int count;//number of samples read in each iteration
	
		/**
		 * @param in
		 * signal source
		 */
		public MultichannelSTFTIterator(SignalSource in) throws IOException
		{
		int c;
	
			this.in=in;
			window=hannWindow(stftsize);//generate Hamming window
			buffer=new double[in.numChannels()][stftsize];//used to read samples
			
			//used to perform fft, has the length of next power of 2 of the signal segment size
			signal=new double[fftSize()];
			fft=new FastFourierTransformer();
			//read overlapping samples for the first segment
			c=in.readSamples(buffer,0,stftoverlap);
			if(c>0) sampleidx+=c;
		}

		public boolean hasNext()
		{			
			if(callnext) throw new IllegalStateException(
					"the next() method should be called before call this method");
			callnext=true;
			
			/*
			 * read samples
			 */
			try
			{
				count=in.readSamples(buffer,stftoverlap,buffer[0].length-stftoverlap);
			}
			catch(IOException e)
			{
				throw new RuntimeException("failed to read signal",e);
			}
			
			if(count<0) return false;
			sampleidx+=count;//accumulate sample index
			
			return true;
		}

		public Complex[][] next()
		{
		Complex[][] res;//stft results
		
			if(!callnext) throw new IllegalStateException(
					"the hasNext() method should be called before call this method");
			callnext=false;
			
			res=new Complex[in.numChannels()][];
			for(int chidx=0;chidx<res.length;chidx++) 
			{			
				/*
				 * prepare to perform fft
				 */
				if(filter==null) System.arraycopy(buffer[chidx],0,signal,0,buffer[chidx].length);//copy samples
				//apply filter to reduce noise, mirror padding is used
				else for(int i=0;i<buffer[chidx].length;i++) 
					signal[i]=Filter.applyFilter(buffer[chidx],filter,i,Filter.Padding.mirror);
				
				//pad with 0
				for(int i=stftoverlap+count;i<signal.length;i++) signal[i]=0;
				
				//modulate by window function
				for(int i=0;i<window.length;i++) signal[i]*=window[i];
				
				//produce overlapping with next segment
				for(int i=0;i<stftoverlap;i++) 
					buffer[chidx][i]=buffer[chidx][buffer[chidx].length-stftoverlap+i];
				
				res[chidx]=fft.transform(signal);//perform fft
			}

			return res;
		}

		public void remove()
		{
			throw new UnsupportedOperationException("remove() is not supported");
		}
		
		/**
		 * get max sample index already processed
		 * @return
		 */
		public long sampleIndex()
		{
			return sampleidx;
		}
	}
	
	public static void main(String[] args) throws IOException, UnsupportedAudioFileException
	{
	WaveSource in;	
	ShortTimeFourierTransformer stft;
	Complex[][] stftdata;
	double[] s2;
	
		stft=new ShortTimeFourierTransformer(256,(int)(256*0.75),1024,null);
		System.out.println("stft size: "+stft.stftSize());
		System.out.println("stft overlap: "+stft.stftOverlap());
		System.out.println("fft size: "+stft.fftSize());
	
		in=new WaveSource(new File("data/source4.wav"),true);
		stftdata=stft.stft(in);
		in.close();
		pp.util.Util.showImage(stft.spectrogram(stftdata));
		
		s2=stft.istft(stftdata);
		Util.playAsAudio(s2,in.audioFormat().getSampleRate());
	}
}
