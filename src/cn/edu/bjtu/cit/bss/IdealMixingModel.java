package cn.edu.bjtu.cit.bss;
import java.io.*;
import org.apache.commons.math.complex.*;
import pp.util.BLAS;
import cn.edu.bjtu.cit.bss.util.*;

/**
 * <h1>Description</h1>
 * Estimated mixing model, underlying frequency domain mixing matrices are stored.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Mar 19, 2012 9:03:46 PM, revision:
 */
public class IdealMixingModel extends BSSModel
{
private static final long serialVersionUID=7996956684185568326L;
private Complex[][][] fdmixm;//mixing matrices for each frequency bin, only half of the frequency band are saved
private int nsources;//number of sources
private int msensors;//number of sensors

	/**
	 * construct an empty mixing model
	 * @param nsources
	 * number of sources
	 * @param msensors
	 * number of sensors
	 * @param fftsize
	 * fft block size
	 */
	public IdealMixingModel(int nsources,int msensors,int fftsize)
	{
		this.nsources=nsources;
		this.msensors=msensors;
	
		if(!SpectralAnalyzer.isPowerOf2(fftsize)) 
			throw new IllegalArgumentException("fft block size must be powers of 2: "+fftsize);
		fdmixm=new Complex[fftsize/2+1][][];
	}

	public int numSources()
	{
		return nsources;
	}

	public int numSensors()
	{
		return msensors;
	}

	public int fftSize()
	{
		return (fdmixm.length-1)*2;
	}
	
	/**
	 * get mixing matrix for a frequency bin, data is not copied
	 * @param binidx
	 * frequency bin index, the maximum size is fftsize/2+1
	 * @return
	 */
	public Complex[][] getMixingMatrix(int binidx)
	{
		return fdmixm[binidx];
	}
	
	/**
	 * set mixing matrix for a frequency bin, data is not copied
	 * @param binidx
	 * frequency bin index, the maximum size is fftsize/2+1
	 * @param mix
	 * corresponding mixing matrix
	 */
	public void setMixingMatrix(int binidx,Complex[][] mix)
	{
		if(mix.length!=numSensors()) throw new IllegalArgumentException(
				"number of sensors not match: "+mix.length+", "+numSensors());
		else if(mix[0].length!=numSources()) throw new IllegalArgumentException(
				"number of sources not match: "+mix[0].length+", "+numSources());
		
		fdmixm[binidx]=mix;
	}
	
	public String toString()
	{
	StringBuilder s;
	double[][][] rmix;
		
		s=new StringBuilder();
		
		for(int binidx=0;binidx<fdmixm.length;binidx++)
		{
			rmix=BLAS.splitComplexMatrix(fdmixm[binidx]);
			s.append("frequency bin "+binidx+":\n");
			s.append("real part:\n");
			s.append(BLAS.toString(rmix[0])+"\n");
			s.append("imaginary part:\n");
			s.append(BLAS.toString(rmix[1])+"\n");	
		}	

		return s.toString();
	}
	
	/**
	 * save the demixing model into file as text
	 * @param path
	 * destination file path
	 * @throws IOException
	 */
	public void save(File path) throws IOException
	{
	BufferedWriter out=null;
	
		try
		{
			out=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path)));
		
			out.write(this.toString());
			out.flush();
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
	 * get frequency domain mixing filters
	 * @return
	 * [sensor index][source index][tap index]
	 */
	public Complex[][][] fdFilters()
	{
	Complex[][][] fdmixf;
	int len;
	
		/*
		 * rearrange indices order
		 */
		fdmixf=new Complex[numSensors()][numSources()][fftSize()];
		
		for(int taps=0;taps<fdmixm.length;taps++)
			for(int sensorj=0;sensorj<fdmixf.length;sensorj++)
				for(int sourcei=0;sourcei<fdmixf.length;sourcei++) 
					fdmixf[sensorj][sourcei][taps]=fdmixm[taps][sensorj][sourcei];

		//expand to full frequency band
		for(int sensorj=0;sensorj<fdmixf.length;sensorj++) 
			for(int sourcei=0;sourcei<fdmixf[sensorj].length;sourcei++) 
			{
				len=fdmixf[sensorj][sourcei].length;
				for(int taps=1;taps<len/2;taps++) 
					fdmixf[sensorj][sourcei][len-taps]=fdmixf[sensorj][sourcei][taps].conjugate();
			}
		
		return fdmixf;
	}
	
	/**
	 * get estimated mixing filters in time domain
	 * @return
	 * [sensor index][source index][tap index]
	 */
	public double[][][] tdFilters()
	{
	Complex[][][] fdmixf;
	double[][][] tdmixf;
		
		fdmixf=fdFilters();
		
		tdmixf=new double[fdmixf.length][fdmixf[0].length][fdmixf[0][0].length];
		for(int sensorj=0;sensorj<tdmixf.length;sensorj++)
			for(int sourcei=0;sourcei<tdmixf[sensorj].length;sourcei++) 
			{
				tdmixf[sensorj][sourcei]=SpectralAnalyzer.ifftReal(fdmixf[sensorj][sourcei]);
				SpectralAnalyzer.fftshift(tdmixf[sensorj][sourcei]);
			}

		return tdmixf;			
	}
}
