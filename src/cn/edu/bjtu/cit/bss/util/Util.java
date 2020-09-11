package cn.edu.bjtu.cit.bss.util;
import java.io.*;
import java.util.*;
import javax.sound.sampled.*;
import com.mathworks.toolbox.javabuilder.*;
import org.apache.commons.math.complex.*;
import pp.util.BLAS;
import icam.*;

/**
 * <h1>Description</h1>
 * Common methods for ICA.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Jan 4, 2011 1:59:04 PM, revision:
 */
public class Util implements Serializable
{
private static final long serialVersionUID=-6112770370462162062L;

	/**
	 * <h1>Description</h1>
	 * Used to indicate the channel dimension.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Jan 4, 2011 2:15:00 PM, revision:
	 */
	public enum Dimension
	{
		/**
		 * Each row of the data file is a observed channel.
		 */
		ROW,
		/**
		 * Each column of the data file is a observed channel.
		 */
		COLUMN
	}
	
	/**
	 * load a singal channel's signal from file
	 * @param path
	 * file path
	 * @param d
	 * indicate the channel dimension
	 * @return
	 * @throws IOExceptoin
	 */
	public static double[] loadSignal(File path,Dimension d) throws IOException
	{
	BufferedReader in=null;
	
		try
		{
			in=new BufferedReader(new InputStreamReader(new FileInputStream(path)));
			switch(d)
			{
				case ROW:
				{
				String[] sentry;	
				double[] sig;	
				
					for(String ts=null;(ts=in.readLine())!=null;)
					{
						ts=ts.trim();
						if(ts.length()==0) continue;
						sentry=ts.split("\\s+");
						sig=new double[sentry.length];
						for(int i=0;i<sig.length;i++) sig[i]=Double.parseDouble(sentry[i]);
						return sig;
					}
				}break;
				case COLUMN:
				{
				List<Double> sigl;
				double[] sig;
				int idx=0;
				
					sigl=new LinkedList<Double>();
					for(String ts=null;(ts=in.readLine())!=null;)
					{
						ts=ts.trim();
						if(ts.length()==0) continue;
						sigl.add(Double.parseDouble(ts));
					}
					sig=new double[sigl.size()];
					for(double s:sigl) sig[idx++]=s;
					return sig;
				}
				default: throw new IllegalArgumentException("unknown dimension: "+d);
			}
			return null;
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
	}

	/**
	 * load all observed signals from file
	 * @param path
	 * file path
	 * @param d
	 * indicate the channel dimension
	 * @return
	 * @throws IOException
	 * each row of the array is a observed channel
	 */
	public static double[][] loadSignals(File path,Dimension d) throws IOException
	{
	BufferedReader in=null;

		try
		{
			in=new BufferedReader(new InputStreamReader(new FileInputStream(path)));
			switch(d)
			{
				case ROW:
				{
				List<double[]> sigl;
				String[] sentry;
				double[] sig;
				double[][] sigs;
				int idx=0;
					
					/*
					 * load data from file
					 */
					sigl=new LinkedList<double[]>();
					for(String ts=null;(ts=in.readLine())!=null;)
					{
						ts=ts.trim();
						if(ts.length()==0) continue;
						sentry=ts.split("\\s+");
						sig=new double[sentry.length];
						for(int i=0;i<sig.length;i++) sig[i]=Double.parseDouble(sentry[i]);
						sigl.add(sig);
					}
					/*
					 * convert from list to array
					 */
					sigs=new double[sigl.size()][];
					for(double[] ch:sigl) sigs[idx++]=ch;
					return sigs;
				}
				case COLUMN:
				{
				List<String[]> sigl;
				double[][] sigs;
				int j=0;
					
					/*
					 * load data from file 
					 */
					sigl=new LinkedList<String[]>();
					for(String ts=null;(ts=in.readLine())!=null;)
					{
						ts=ts.trim();
						if(ts.length()==0) continue;
						sigl.add(ts.split("\\s+"));
					}
					/*
					 * convert to array
					 */
					sigs=new double[sigl.get(0).length][sigl.size()];
					for(String[] sig:sigl) 
					{
						for(int i=0;i<sig.length;i++) sigs[i][j]=Double.parseDouble(sig[i]);
						j++;
					}
					return sigs;	
				}
				default: throw new IllegalArgumentException("unknown dimension: "+d);
			}
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
	}
	
	/**
	 * save a signal sequence into file
	 * @param sig
	 * a signal sequence
	 * @param path
	 * destination file path
	 * @param d
	 * indicate the saved signal's dimension
	 * @throws IOException
	 */
	public static void saveSignal(double[] sig,File path,Dimension d) throws IOException
	{
	BufferedWriter out=null;
	
		try
		{
			out=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path)));
			switch(d)
			{
				case ROW:
				{
					for(double s:sig) out.write(s+" ");
					out.write("\n");
					out.flush();
				}break;
				case COLUMN:
				{
					for(double s:sig) out.write(s+"\n");
					out.flush();
				}break;
				default: throw new IllegalArgumentException("unknown dimension: "+d);
			}
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
	}
	
	/**
	 * save signal of all channels into file
	 * @param sigs
	 * signal of all channels, each row is from a channel
	 * @param path
	 * destination file path
	 * @param d
	 * indicate the saved signal dimension
	 * @throws IOException
	 */
	public static void saveSignals(double[][] sigs,File path,Dimension d) throws IOException
	{
	BufferedWriter out=null;
	
		try
		{
			out=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path)));
			switch(d)
			{
				case ROW:
				{
					for(int i=0;i<sigs.length;i++)
					{
						for(int j=0;j<sigs[i].length;j++) out.write(sigs[i][j]+" ");
						out.write("\n");
					}
					out.flush();
				}break;
				case COLUMN:
				{
					for(int j=0;j<sigs[0].length;j++)
					{
						for(int i=0;i<sigs.length;i++) out.write(sigs[i][j]+" ");
						out.write("\n");
					}
					out.flush();
				}break;
				default: throw new IllegalArgumentException("unknown dimension: "+d);
			}
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
	}
	
	/**
	 * save a group of complex signals into file
	 * @param sigs
	 * each row for a channel
	 * @param realf
	 * destination for the real part
	 * @param imagf
	 * desitnation for the imaginary part
	 * @param d
	 * indicate the saved signal dimension
	 * @throws IOException
	 */
	public static void saveSignals(Complex[][] sigs,File realf,File imagf,Dimension d) throws IOException
	{
	double[][] real,imag;
	
		/*
		 * get its real and imaginary parts
		 */
		real=new double[sigs.length][sigs[0].length];
		imag=new double[sigs.length][sigs[0].length];
		for(int i=0;i<sigs.length;i++)
			for(int j=0;j<sigs[i].length;j++)
			{
				real[i][j]=sigs[i][j].getReal();
				imag[i][j]=sigs[i][j].getImaginary();
			}
		/*
		 * save signals
		 */
		saveSignals(real,realf,d);
		saveSignals(imag,imagf,d);
	}

	/**
	 * plot all channel's signals
	 * @param fs
	 * sample rate
	 * @param sigs
	 * each row is a channel
	 */
	public static void plotSignals(double fs,double[]... sigs)
	{
	SignalViewer viewer;
		
		viewer=new SignalViewer(fs,sigs);
		viewer.visualize();
	}
	
	/**
	 * plot all channel's signals
	 * @param sigs
	 * each row is a channel
	 */
	public static void plotSignals(double[]... sigs)
	{
	SignalViewer viewer;
		
		viewer=new SignalViewer(sigs);
		viewer.visualize();
	}
	
	/**
	 * plot spectrograms of the transformed segments by fft
	 * @param fs
	 * sample rate
	 * @param fx
	 * fft segments
	 */
	public static void plotSpectrum(double fs,Complex[]... fx)
	{
	Visualization v;
	double[][][] fxri;
	
		try
		{
			v=new Visualization();
			fxri=BLAS.splitComplexMatrix(fx);
			v.plotSpectrums(fs,fxri[0],fxri[1]);
		}
		catch(MWException e)
		{
			throw new RuntimeException("failed to invocate matlab",e);
		}
	}
	
	/**
	 * load an audio file as signal
	 * @param path
	 * audio file path
	 * @return
	 * @throws IOException
	 */
	public static double[] loadAudio(File path) throws IOException
	{
	AudioInputStream in=null;
	List<Integer> audiol;
	double[] audio=null;
	int idx=0;
	
		try
		{
			audiol=new LinkedList<Integer>();
			in=AudioSystem.getAudioInputStream(path);
			for(int a=0;(a=in.read())>=0;) audiol.add(a);
			audio=new double[audiol.size()];
			for(int a:audiol) audio[idx++]=a;
		}
		catch(UnsupportedAudioFileException e)
		{
			throw new RuntimeException("failed to load audio: "+path.getName(),e);
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
		return audio;
	}
	
	/**
	 * play a signal as audio
	 * @param sig
	 * a signal
	 * @param fs
	 * sample rate
	 */
	public static void playAsAudio(double[] sig,double fs)
	{
	double min=Double.MAX_VALUE,max=Double.MIN_VALUE;
	double mean=0,amp;
	byte[] audio;
	int temp;
	AudioFormat format;
	Clip clip;
		
		/*
		 * normalize to 16 bits format
		 */
		for(double s:sig)
		{
			mean+=s;
			if(s<min) min=s;
			if(s>max) max=s;
		}
		mean/=sig.length;
		amp=Math.max(Math.abs(min-mean),Math.abs(max-mean));
		
		audio=new byte[sig.length*2];
		for(int i=0;i<sig.length;i++)
		{
			temp=(int)Math.round((sig[i]-mean)*32768.0/amp);
			if(temp<-32768) temp=-32768;
			else if(temp>32767) temp=32767;

			/*
			 * little endian
			 */
			audio[2*i]=(byte)(temp&0x000000ff);
			audio[2*i+1]=(byte)((temp>>>8)&0x000000ff);
		}
		/*
		 * play back as sound
		 */
		//construct corresponding audio format
		format=new AudioFormat((float)fs,16,1,true,false);
		try
		{
			clip=AudioSystem.getClip();
			clip.open(format,audio,0,audio.length);
			clip.start();
			Thread.sleep((long)((1.0/fs)*sig.length*1000));
			clip.close();
		}
		catch(LineUnavailableException e)
		{
			throw new RuntimeException("failed to play signal as audio",e);
		}
		catch(InterruptedException e)
		{}
	}
	
	public static void main(String[] args) throws IOException
	{
	double[][] sigs;
	double[][] mix;
	
		sigs=Util.loadSignals(new File("data/demosig.txt"),Dimension.COLUMN);
		mix=BLAS.randMatrix(sigs.length,sigs.length);
		BLAS.multiply(mix,sigs,sigs);
		Util.plotSignals(sigs);
	}
}
