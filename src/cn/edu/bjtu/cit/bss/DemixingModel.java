package cn.edu.bjtu.cit.bss;
import java.io.*;
import org.apache.commons.math.complex.*;
import pp.util.BLAS;
import cn.edu.bjtu.cit.bss.util.*;

/**
 * <h1>Description</h1>
 * Demixing model, a bank of frequency demixing filters. Used to separate estimated 
 * sources from observed signals.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Mar 19, 2012 6:42:43 PM, revision:
 */
public class DemixingModel extends BSSModel
{
private static final long serialVersionUID=-6432725535259036385L;
private Complex[][][] fddemixm;//demixing matrices for each frequency bin, only half of the frequency band are saved
private int nsources;//number of sources
private int msensors;//number of sensors

	/**
	 * make a copy
	 * @param another
	 * another model
	 */
	public DemixingModel(DemixingModel another)
	{
	Complex[][][] temp;
	
		nsources=another.nsources;
		msensors=another.msensors;
	
		temp=another.fddemixm;
		fddemixm=new Complex[temp.length][temp[0].length][temp[0][0].length];
		for(int binidx=0;binidx<fddemixm.length;binidx++) 
			for(int sourcei=0;sourcei<fddemixm[binidx].length;sourcei++) 
				for(int sensorj=0;sensorj<fddemixm[binidx][sourcei].length;sensorj++) 
					fddemixm[binidx][sourcei][sensorj]=temp[binidx][sourcei][sensorj];	
	}

	/**
	 * construct an empty demixing model
	 * @param nsources
	 * number of sources
	 * @param msensors
	 * number of sensors
	 * @param fftsize
	 * fft block size
	 */
	public DemixingModel(int nsources,int msensors,int fftsize)
	{
		this.nsources=nsources;
		this.msensors=msensors;
		
		if(!SpectralAnalyzer.isPowerOf2(fftsize)) 
			throw new IllegalArgumentException("fft block size must be powers of 2: "+fftsize);
		fddemixm=new Complex[fftsize/2+1][][];
	}
	
	/**
	 * load the demixing model from file
	 * @param path
	 * file path
	 * @throws IOException
	 */
	public DemixingModel(File path) throws IOException
	{
	Complex[][][] temp;
	
		temp=BSSModel.loadComplexMatrices(path);
		
		this.nsources=temp[0].length;
		this.msensors=temp[0][0].length;	
		if(!SpectralAnalyzer.isPowerOf2((temp.length-1)*2)) 
			throw new IllegalArgumentException("fft block size must be powers of 2: "+(temp.length-1)*2);
		fddemixm=new Complex[temp.length][][];
		
		for(int binidx=0;binidx<temp.length;binidx++) 
			setDemixingMatrix(binidx,temp[binidx]);
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
		return (fddemixm.length-1)*2;
	}
	
	/**
	 * get demixing matrix for a frequency bin, data is not copied
	 * @param binidx
	 * frequency bin index, the maximum size is fftsize/2+1
	 * @return
	 */
	public Complex[][] getDemixingMatrix(int binidx)
	{
		return fddemixm[binidx];
	}
	
	/**
	 * set demixing matrix for a frequency bin, data is not copied
	 * @param binidx
	 * frequency bin index, the maximum size is fftsize/2+1
	 * @param demix
	 * corresponding demixing matrix
	 */
	public void setDemixingMatrix(int binidx,Complex[][] demix)
	{
		if(demix.length!=numSources()) throw new IllegalArgumentException(
				"number of sources not match: "+demix.length+", "+numSources());
		else if(demix[0].length!=numSensors()) throw new IllegalArgumentException(
				"number of sensors not match: "+demix[0].length+", "+numSensors());
		
		fddemixm[binidx]=demix;
	}
	
	public String toString()
	{
	StringBuilder s;
	double[][][] rmix;
		
		s=new StringBuilder();
		
		for(int binidx=0;binidx<fddemixm.length;binidx++)
		{
			rmix=BLAS.splitComplexMatrix(fddemixm[binidx]);
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
	 * estimate the ideal mixing model corresponds to this demixing model
	 * @return
	 */
	public IdealMixingModel idealMixingModel()
	{
	IdealMixingModel mm;
	Complex[][] mix;
	
		mm=new IdealMixingModel(numSources(),numSensors(),fftSize());
		
		for(int binidx=0;binidx<fftSize()/2+1;binidx++)
		{
			mix=BLAS.pinv(getDemixingMatrix(binidx),null);
			mm.setMixingMatrix(binidx,mix);
		}
		
		return mm;
	}
	
	/**
	 * get frequency domain demixing filters
	 * @return
	 * [source index][sensor index][tap index]
	 */
	public Complex[][][] fdFilters()
	{
	Complex[][][] fddemixf;
	int len;
		
		/*
		 * rearrange indices order
		 */
		fddemixf=new Complex[numSources()][numSensors()][fftSize()];
		for(int taps=0;taps<fddemixm.length;taps++) 
			for(int sourcei=0;sourcei<fddemixm[taps].length;sourcei++) 
				for(int sensorj=0;sensorj<fddemixm[taps][sourcei].length;sensorj++) 
					fddemixf[sourcei][sensorj][taps]=fddemixm[taps][sourcei][sensorj];
			
		//expand to full frequency band
		for(int sourcei=0;sourcei<fddemixf.length;sourcei++) 
			for(int sensorj=0;sensorj<fddemixf[sourcei].length;sensorj++) 
			{
				len=fddemixf[sourcei][sensorj].length;
				for(int taps=1;taps<len/2;taps++) 
					fddemixf[sourcei][sensorj][len-taps]=fddemixf[sourcei][sensorj][taps].conjugate();
			}

		/*
		 * apply spectral smoothing on each frequency domain filter
		 */
		for(int sourcei=0;sourcei<fddemixf.length;sourcei++) 
			for(int sensorj=0;sensorj<fddemixf[sourcei].length;sensorj++) 
				fddemixf[sourcei][sensorj]=spectralSmoothing(fddemixf[sourcei][sensorj]);		

		return fddemixf;
	}
	
	/**
	 * perform spectral smoothing, see: Hiroshi Sawada et al. SPECTRAL SMOOTHING 
	 * FOR FREQUENCY-DOMAIN BLIND SOURCE SEPARATION, International Workshop on Acoustic 
	 * Echo and Noise Control, pp. 311-314, 2003
	 * @param fdf
	 * a frequency domain filter
	 * @return
	 * the smoothed filter
	 */
	private Complex[] spectralSmoothing(Complex[] fdf)
	{
	Complex w0,w1,w2;
	Complex[] fdf2;
	
		fdf2=new Complex[fdf.length];
		
		for(int binidx=0;binidx<fdf.length/2+1;binidx++) 
		{
			w1=fdf[binidx];
			if(binidx-1>=0) w0=fdf[binidx-1];else w0=fdf[binidx];
			if(binidx+1<fdf.length/2+1) w2=fdf[binidx+1];else w2=fdf[binidx];
			
			fdf2[binidx]=w0.add(w1.multiply(2)).add(w2).multiply(1.0/4.0);
			
			//its complex conjugate counterpart
			if(binidx>0&&binidx<fdf.length/2) 
				fdf2[fdf2.length-binidx]=fdf2[binidx].conjugate();
		}
		
		return fdf2;
	}
	
	/**
	 * get time domain demixing filters
	 * @return
	 * [source index][sensor index][tap index]
	 */
	public double[][][] tdFilters()
	{
	Complex[][][] fddemixf;
	double[][][] tddemixf;
		
		fddemixf=fdFilters();
			
		tddemixf=new double[fddemixf.length][fddemixf[0].length][];
		for(int sourcei=0;sourcei<fddemixf.length;sourcei++)
			for(int sensorj=0;sensorj<fddemixf[sourcei].length;sensorj++) 
			{
				tddemixf[sourcei][sensorj]=SpectralAnalyzer.ifftReal(fddemixf[sourcei][sensorj]);
				SpectralAnalyzer.fftshift(tddemixf[sourcei][sensorj]);
			}
			
		return tddemixf;		
	}
	
	public static void main(String[] args) throws IOException
	{
	DemixingModel foo,foo2;
	IdealMixingModel mm;
	
		foo=new DemixingModel(2,3,8);
		for(int binidx=0;binidx<foo.fftSize()/2+1;binidx++) 
			foo.setDemixingMatrix(binidx,BLAS.randComplexMatrix(foo.numSources(),foo.numSensors()));
		
		System.out.println(foo);
		foo.save(new File("/home/nay0648/model.txt"));
		
		foo2=new DemixingModel(new File("/home/nay0648/model.txt"));
		System.out.println(foo2);
		
		mm=foo.idealMixingModel();
		System.out.println(mm);
	}
}
