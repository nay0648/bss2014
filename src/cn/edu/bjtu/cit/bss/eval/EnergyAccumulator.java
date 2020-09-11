package cn.edu.bjtu.cit.bss.eval;
import java.io.*;
import java.util.*;
import org.apache.commons.math.complex.*;
import org.apache.commons.math.transform.*;
import pp.util.BLAS;
import cn.edu.bjtu.cit.bss.signalio.*;
import cn.edu.bjtu.cit.bss.util.*;

/**
 * <h1>Description</h1>
 * Used to accumulate T-F data's energy in time domain, 
 * to avoid the overlap add effect.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Nov 26, 2012 8:56:56 AM, revision:
 */
public class EnergyAccumulator implements Serializable
{
private static final long serialVersionUID=-4577080474999768361L;
private double energy=0;//total energy
private ShortTimeFourierTransformer stft;//used to perform stft
private FastFourierTransformer fft;//used to perform ifft
private double[] ifftr=null;//ifft signal
private double[] overlapr=null;//buffer for overlapping samples
private double factor;//used to cancel the overlap add effect

	/**
	 * @param stft
	 * the stft reference
	 */
	public EnergyAccumulator(ShortTimeFourierTransformer stft)
	{
		this.stft=stft;
		
		fft=new FastFourierTransformer();
		ifftr=new double[stft.fftSize()];
		overlapr=new double[stft.fftSize()-stft.stftShift()];
		//real magnitude=factor x magnitude
		factor=stft.scalingFactor();
	}

	/**
	 * accumulate energy in t-f domain
	 * @param frame
	 * a t-f frame
	 */
	public void accumulateEnergy(Complex[] frame)
	{
	Complex[] ifft;
	double temp;
	
		if(frame.length!=stft.fftSize()) throw new IllegalArgumentException(
				"fft size not match: "+frame.length+", "+stft.fftSize());
		
		/*
		 * perform ifft
		 */
		ifft=fft.inversetransform(frame);
		for(int t=0;t<ifftr.length;t++) ifftr[t]=ifft[t].getReal();
		
		//cancel the windowing effect
		BLAS.scalarMultiply(factor,ifftr,ifftr);
		
		//add overlapping samples
		for(int i=0;i<overlapr.length;i++) ifftr[i]+=overlapr[i];
		
		//accumulate energy
		for(int t=0;t<ifftr.length-overlapr.length;t++) 
		{
			temp=ifftr[t];
			energy+=temp*temp;
		}
		
		//copy overlapping samples for next window
		for(int i=0;i<overlapr.length;i++) 
			overlapr[i]=ifftr[ifftr.length-overlapr.length+i];
	}
	
	/**
	 * accumulate energy in t-f domain
	 * @param stftdata
	 * t-f data [frequency index][frame index]
	 */
	public void accumulateEnergy(Complex[][] stftdata)
	{
	Complex[] frame;
	
		frame=new Complex[stft.fftSize()];
		for(int tau=0;tau<stftdata[0].length;tau++) 
		{
			for(int f=0;f<frame.length;f++) frame[f]=stftdata[f][tau];
			accumulateEnergy(frame);	
		}
	}
	
	public double energy()
	{
	double res=0,temp;
	
		//calculate the residual
		for(int t=0;t<overlapr.length;t++) 
		{
			temp=overlapr[t];
			res+=temp*temp;
		}
		
		return energy+res;
	}
	
	/**
	 * reset the accumulator
	 */
	public void reset()
	{
		Arrays.fill(overlapr,0);
		energy=0;
	}

	public static void main(String[] args) throws IOException
	{
	ShortTimeFourierTransformer stft;
	SignalSource ss;
	Complex[][] fds=null;
	EnergyAccumulator eacc;
	double[] s;
	double e2=0;
	
		stft=new ShortTimeFourierTransformer(1024,1024*7/8,2048,null);
		ss=new GaussianNoiseSource(10000,10,11);
		fds=stft.stft(ss);
		ss.close();

		eacc=new EnergyAccumulator(stft);
		eacc.accumulateEnergy(fds);
		
		s=stft.istft(fds);
		for(int t=0;t<s.length;t++) e2+=s[t]*s[t];
		
		System.out.println(eacc.energy());
		System.out.println(e2);
	}
}
