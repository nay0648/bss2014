package cn.edu.bjtu.cit.bss.util;
import java.io.*;
import java.util.Arrays;
import org.apache.commons.math.complex.*;
import org.apache.commons.math.transform.*;
import cn.edu.bjtu.cit.bss.signalio.*;
import pp.util.BLAS;

/**
 * <h1>Description</h1>
 * Methods for spectrum analysis.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Jun 16, 2011 5:20:00 PM, revision:
 */
public class SpectralAnalyzer implements Serializable
{
private static final long serialVersionUID=-8229357512218936962L;
private static final FastFourierTransformer fft=new FastFourierTransformer();//used for fft
	
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
	 * calculate the next power of 2
	 * @param len
	 * segment length
	 * @return
	 */
	public static int nextPowerOf2(int len)
	{
		return (int)Math.pow(2,Math.ceil(Math.log(len)/Math.log(2)));
	}
	
	/**
	 * perform fft
	 * @param x
	 * input signal
	 * @return
	 * always has the length of the next power of two of the input signal length
	 */
	public static Complex[] fft(double[] x)
	{
	int len2;
	double[] x2;
	
		len2=nextPowerOf2(x.length);
		if(len2==x.length) return fft.transform(x);
		//get the proper fft length and pad with zero
		else
		{
			x2=new double[len2];
			System.arraycopy(x,0,x2,0,x.length);
			return fft.transform(x2);
		}
	}
	
	/**
	 * perform the inverse fft
	 * @param fx
	 * must has the length of power of 2
	 * @return
	 */
	public static Complex[] ifft(Complex[] fx)
	{
		return fft.inversetransform(fx);
	}
	
	/**
	 * perform inverse fft and keep only the real part
	 * @param fx
	 * must has the length of power of 2
	 * @return
	 */
	public static double[] ifftReal(Complex[] fx)
	{
	double[] x;
	
		x=new double[fx.length];
		fx=fft.inversetransform(fx);
		for(int i=0;i<x.length;i++) x[i]=fx[i].getReal();
		return x;
	}
	
	/**
	 * used to forcus low frequency part to the center of the signal segment
	 * @param x
	 * a signal segment
	 */
	public static void fftshift(double[] x)
	{
	int idx1,idx2;
	double temp;
	
		if(x.length%2!=0) throw new IllegalArgumentException(
				"even taps required: "+x.length);
		
		for(idx1=0,idx2=x.length/2;idx2<x.length;idx1++,idx2++)
		{
			temp=x[idx1];
			x[idx1]=x[idx2];
			x[idx2]=temp;
		}
	}
	
	/**
	 * used to forcus low frequency part to the center of the signal segment
	 * @param x
	 * a signal segment
	 */
	public static void fftshift(Complex[] x)
	{
	int idx1,idx2;
	Complex temp;
		
		if(x.length%2!=0) throw new IllegalArgumentException(
				"even taps required: "+x.length);
			
		for(idx1=0,idx2=x.length/2;idx2<x.length;idx1++,idx2++)
		{
			temp=x[idx1];
			x[idx1]=x[idx2];
			x[idx2]=temp;
		}
	}
	
	/**
	 * calculate the total energy of a time domain signal segment as: 
	 * |x0|^2+|x1|^2+...+|xn|^2.
	 * @param x
	 * a signal segment
	 * @return
	 */
	public static double tdEnergy(double[] x)
	{
	double e=0;
	
		for(double s:x) e+=s*s;
		return e;
	}
	
	/**
	 * calculate the total energy of a frequency domain signal segment 
	 * as: (|x0|^2+|x1|^2+...+|xn|^2)/n.
	 * @param x
	 * a signal segment
	 * @return
	 */
	public static double fdEnergy(Complex[] x)
	{
	double e=0;
	
		for(Complex s:x) e+=BLAS.absSquare(s);
		return e/x.length;
	}

	/**
	 * Convert frequency domain segment to energy spectrum, energy is |X|^2 
	 * where X is one entry of fft result.
	 * @param fx
	 * fft segment
	 * @return
	 * Has the length of nfft/2+1, where nfft is the length of fft segment.
	 */
	public static double[] energySpectrum(Complex[] fx)
	{
	double[] energy;
	
		energy=new double[fx.length/2+1];
		
		energy[0]=BLAS.absSquare(fx[0]);//DC part
		energy[energy.length-1]=BLAS.absSquare(fx[energy.length-1]);//the highest frequency
		//two halves for other parts
		for(int i=1;i<energy.length-1;i++) energy[i]=2*BLAS.absSquare(fx[i]);
		
		//fft segment size is divide to keep energy conservation
		BLAS.scalarMultiply(1.0/fx.length,energy,energy);
		
		return energy;
	}
	
	/**
	 * Convert frequency domain segment to energy spectrum, energy is |X|^2 
	 * where X is one entry of fft result. Results are returned in dB: 
	 * 10*log10(energy/p0^2), where energy=|X|^2.
	 * @param fx
	 * frequency domain signal
	 * @param p0
	 * usually 2e-5 for sound pressure
	 * @return
	 */
	public static double[] energySpectrumDB(Complex[] fx,double p0)
	{
	double[] energy;
	
		energy=energySpectrum(fx);
		p0=p0*p0;
		for(int i=0;i<energy.length;i++) energy[i]=10*Math.log10(energy[i]/p0);
		return energy;
	}
	
	/**
	 * plot energy and phase spectrum for a fft block
	 * @param signal
	 * signal sequence, length must be powers of 2
	 * @param fs
	 * sampling rate
	 */
	public static void plotSpectrum(double[] signal,double fs)
	{
		(new SpectrumViewer(signal,fs)).visualize();
	}
	
	/**
	 * calculate the power spectrum for a signal sequence
	 * @param signal
	 * a signal sequence
	 * @param stft
	 * used to perform stft
	 * @return
	 * represents average energy for one sample in each frequency bin
	 * @throws IOException
	 */
	public static double[] powerSpectrum(
			SignalSource signal,ShortTimeFourierTransformer stft) throws IOException
	{
	double[] energy;
	ShortTimeFourierTransformer.STFTIterator stftit;
	Complex[] fx;
		
		/*
		 * calculate total energy spectrum
		 */
		energy=new double[stft.fftSize()/2+1];
		stftit=stft.stftIterator(signal);
		for(;stftit.hasNext();)
		{
			fx=stftit.next();

			energy[0]+=BLAS.absSquare(fx[0]);//DC part
			energy[energy.length-1]+=BLAS.absSquare(fx[energy.length-1]);//the highest frequency
			//two halves for other parts
			for(int i=1;i<energy.length-1;i++) energy[i]+=2*BLAS.absSquare(fx[i]);
		}

		//energy conservation between time domain and frequency domain
		BLAS.scalarMultiply(1.0/stft.fftSize(),energy,energy);
		//cancel the window overlapping effect
		BLAS.scalarMultiply(2.0*(stft.stftSize()-stft.stftOverlap())/stft.stftSize(),energy,energy);
		//average energy for one sample, so energy will not change with signal length
		BLAS.scalarMultiply(1.0/stftit.sampleIndex(),energy,energy);

		return energy;
	}
	
	/**
	 * calculate the power spectrum for a signal sequence
	 * @param signal
	 * a signal sequence
	 * @param stft
	 * used to perform stft
	 * @return
	 * represents average energy for one sample in each frequency bin
	 */
	public static double[] powerSpectrum(
			double[] signal,ShortTimeFourierTransformer stft)
	{
	ArraySignalSource source=null;
	
		try
		{
			source=new ArraySignalSource(signal);
			return powerSpectrum(source,stft);
		}
		catch(IOException e)
		{
			throw new RuntimeException("failed to calculate mean energy spectrum",e);
		}
		finally
		{
			try
			{
				if(source!=null) source.close();
			}
			catch(IOException e)
			{}
		}
	}
	
	/**
	 * Calculate average frequency response between two signals, this is useful when 
	 * the system has finite impulse response.
	 * @param chin
	 * input signal
	 * @param chout
	 * output signal
	 * @param stftsize
	 * segment size in stft
	 * @return
	 * the average frequency response
	 * @deprecated
	 * the estimation error is high, maybe
	 * @throws IOException
	 */
	public static Complex[] frequencyResponse(SignalSource chin,SignalSource chout,int stftsize) throws IOException
	{
	ShortTimeFourierTransformer stft;
	ShortTimeFourierTransformer.STFTIterator itin,itout;
	Complex[] fh,fxin,fxout;
	int[] count;
	double eps=1e-6;

		stft=new ShortTimeFourierTransformer(stftsize,(int)(0.5*stftsize),null);
		fh=new Complex[stft.fftSize()];
		count=new int[stft.fftSize()];
		Arrays.fill(fh,new Complex(0,0));
		
		itin=stft.stftIterator(chin);
		itout=stft.stftIterator(chout);
		
		for(;itin.hasNext()&&itout.hasNext();)
		{
			fxin=itin.next();
			fxout=itout.next();

			for(int i=0;i<fh.length;i++) 
			{
				if(BLAS.absSquare(fxin[i])<eps) continue;
				fh[i]=fh[i].add(fxout[i].divide(fxin[i]));
				count[i]++;
			}
		}

		//average frequency response
		for(int i=0;i<fh.length;i++) fh[i]=fh[i].multiply(1.0/count[i]);
		return fh;
	}
}
