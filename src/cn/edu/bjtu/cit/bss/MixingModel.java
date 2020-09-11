package cn.edu.bjtu.cit.bss;
import java.io.*;
import java.util.*;
import org.apache.commons.math.complex.*;
import pp.util.BLAS;
import cn.edu.bjtu.cit.bss.eval.*;
import cn.edu.bjtu.cit.bss.util.*;

/**
 * <h1>Description</h1>
 * The mixing model, used to mix source signals for experiment.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Apr 3, 2012 4:38:43 PM, revision:
 */
public class MixingModel extends BSSModel
{
private static final long serialVersionUID=-6365317234307167713L;
private double[][][] tdmixf;//time domain mixing filters
private int fftsize;//fft block size

	/**
	 * @param tdmixf
	 * time domain mixing filters, [sensor index][source index][tap index]
	 * @param fftsize
	 * fft block size
	 */
	public MixingModel(double[][][] tdmixf,int fftsize)
	{
		this.tdmixf=tdmixf;
		
		if(!SpectralAnalyzer.isPowerOf2(fftsize)) throw new IllegalArgumentException(
				"fft size must be powers of 2: "+fftsize);
		if(fftsize<tdmixf[0][0].length) throw new IllegalArgumentException(
				"fft size smaller than filter length: "+fftsize+", "+tdmixf[0][0].length);
		this.fftsize=fftsize;
	}

	public int numSources()
	{
		return tdmixf[0].length;
	}

	public int numSensors()
	{
		return tdmixf.length;
	}

	public int fftSize()
	{
		return fftsize;
	}
	
	public double[][][] tdFilters()
	{
		return tdmixf;
	}
	
	public Complex[][][] fdFilters()
	{
	double[] h;
	Complex[][][] fdmixf;
	
		h=new double[fftsize];
		fdmixf=new Complex[this.numSensors()][this.numSources()][];
			
		for(int sensorj=0;sensorj<fdmixf.length;sensorj++) 
			for(int sourcei=0;sourcei<fdmixf[sensorj].length;sourcei++) 
			{
				Arrays.fill(h,0);
				System.arraycopy(tdmixf[sensorj][sourcei],0,h,0,tdmixf[sensorj][sourcei].length);

				fdmixf[sensorj][sourcei]=SpectralAnalyzer.fft(h);
			}
		
		return fdmixf;
	}
	
	/**
	 * get the ideal demixing model corresponds to this mixing model
	 * @return
	 */
	public DemixingModel idealDemixingModel()
	{
	DemixingModel demixm;
	Complex[][][] fdmixf;
	Complex[][] mix,demix;
		
		demixm=new DemixingModel(this.numSources(),this.numSensors(),this.fftSize());
		fdmixf=this.fdFilters();
		
		mix=new Complex[this.numSensors()][this.numSources()];
		for(int binidx=0;binidx<demixm.fftSize()/2+1;binidx++)
		{
			//get the mixing matrix for a frequency bin
			for(int sensorj=0;sensorj<mix.length;sensorj++) 
				for(int sourcei=0;sourcei<mix[sensorj].length;sourcei++) 
					mix[sensorj][sourcei]=fdmixf[sensorj][sourcei][binidx];
			
			demix=BLAS.pinv(mix,null);//calculate corresponding demixing matrix
			demixm.setDemixingMatrix(binidx,demix);
		}

		return demixm;
	}
	
	public static void main(String[] args) throws IOException
	{
	VirtualRoom room;
	MixingModel mixm;
	DemixingModel demixm;
//	EvalModel evalm;
	
		room=new VirtualRoom(new File("data/VirtualRooms/2x2/SawadaRoom.txt"));
		mixm=new MixingModel(room.tdFilters(),2048);
		mixm.visualize();
//		mixm.fdVisualize();
		
		demixm=mixm.idealDemixingModel();
		demixm.visualize();
//		demixm.fdVisualize();
		
//		evalm=new EvalModel(mixm,demixm);
//		evalm.visualize();
//		evalm.fdVisualize();
	}
}
