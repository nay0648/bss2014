package cn.edu.bjtu.cit.bss.signalio;
import java.io.*;
import java.util.*;
import javax.sound.sampled.*;
import org.apache.commons.math.complex.*;
import org.apache.commons.math.transform.*;
import pp.util.BLAS;
import cn.edu.bjtu.cit.bss.eval.*;
import cn.edu.bjtu.cit.bss.util.*;

/**
 * <h1>Description</h1>
 * Perform istft, and send result signal into stream
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Nov 22, 2012 3:48:28 PM, revision:
 */
public class ISTFTSink extends SignalSink
{
private ShortTimeFourierTransformer stft;//used to perform stft
private boolean complex;//true to output complex signal
private SignalSink out;//underlying output stream
private double[] ifftr=null;//ifft signal
private double[] overlapr=null;//buffer for overlapping samples
private Complex[] overlapc=null;//buffer for overlapping samples
private FastFourierTransformer fft;//used to perform ifft
private double factor;//used to cancel the overlap add effect
private int numsegs=0;//number of stft blocks outputed

	public ISTFTSink(ShortTimeFourierTransformer stft,boolean complex,SignalSink out)
	{
		this.stft=stft;
		this.complex=complex;
		this.out=out;
		
		if(complex)
		{
			overlapc=new Complex[stft.fftSize()-stft.stftShift()];
			Arrays.fill(overlapc,Complex.ZERO);
		}
		else
		{
			ifftr=new double[stft.fftSize()];
			overlapr=new double[stft.fftSize()-stft.stftShift()];
		}
		
		fft=new FastFourierTransformer();
		//real magnitude=factor x magnitude
		factor=stft.scalingFactor();
	}
	
	public int numChannels()
	{
		return stft.fftSize();
	}
	
	/**
	 * get the number of stft frames outputted
	 * @return
	 */
	public int numSTFTFrames()
	{
		return numsegs;
	}
	
	public void writeFrame(double[] frame) throws IOException
	{
	Complex[] cframe;
	
		this.checkFrameSize(frame.length);
		
		cframe=new Complex[frame.length];
		for(int f=0;f<cframe.length;f++) cframe[f]=new Complex(frame[f],0);
		writeFrame(cframe);
	}

	public void writeFrame(Complex[] frame) throws IOException
	{
	Complex[] ifft;//ifft of the stft segment
		
		this.checkFrameSize(frame.length);
		
		ifft=fft.inversetransform(frame);//perform ifft
		
		if(complex)
		{
			//cancel the windowing effect
			BLAS.scalarMultiply(factor,ifft,ifft);
			
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
			//cancel the windowing effect
			BLAS.scalarMultiply(factor,ifftr,ifftr);
			
			//add overlapping samples
			for(int i=0;i<overlapr.length;i++) ifftr[i]+=overlapr[i];
			
			//write to result stream
			out.writeSamples(ifftr,0,ifftr.length-overlapr.length);
			
			//copy overlapping samples for next window
			for(int i=0;i<overlapr.length;i++) overlapr[i]=ifftr[ifftr.length-overlapr.length+i];
		}
		
		numsegs++;
	}
	
	public void flush() throws IOException
	{
		//write the last part
		if(numsegs>0)
		{
			if(complex) out.writeSamples(overlapc);
			else out.writeSamples(overlapr);
		}
		
		out.flush();
	}

	public void close() throws IOException
	{
		out.close();
	}
	
	public static void main(String[] args) throws IOException, UnsupportedAudioFileException
	{
	ShortTimeFourierTransformer stft;
	WaveSource ws;
	double[] s=null,h,x,ph,x2,err;
	Complex[][] fds;
	Complex[] fdh;
	ArraySignalSink as;
	ISTFTSink is;
	Complex[] frame;
	
		/*
		 * load signal
		 */
		ws=new WaveSource(new File("data/SawadaDataset/s1.wav"),true);
		s=ws.toArray(s);
		ws.close();
	
		//load impulse response
		h=Evaluator.loadFilters(1024)[0];
	
		//time domain convolution
		x=Filter.convolve(s,h,null,Filter.Padding.zero);
	
		/*
		 * perform stft
		 */
		stft=new ShortTimeFourierTransformer(1024,1024*7/8,2048,null);
		fds=stft.stft(s);
		
		/*
		 * frequency response
		 */
		ph=new double[stft.fftSize()];
		System.arraycopy(h,0,ph,0,h.length);
		fdh=SpectralAnalyzer.fft(ph);
		
		/*
		 * frequency domain multiplication and perform istft
		 */
		as=new ArraySignalSink(1);
		is=new ISTFTSink(stft,false,as);
		frame=new Complex[fds.length];
		
		for(int tau=0;tau<fds[0].length;tau++) 
		{
			for(int f=0;f<frame.length;f++) frame[f]=fds[f][tau].multiply(fdh[f]);
			is.writeFrame(frame);
		}
		
		is.flush();
		is.close();
		
		/*
		 * compare results, test the convolution theorem
		 */
		x2=as.toArray((double[][])null)[0];
		
		err=new double[Math.min(x.length,x2.length)];
		for(int t=0;t<err.length;t++) err[t]=x[t]-x2[t];
		Util.plotSignals(x,x2,err);
	}
}
