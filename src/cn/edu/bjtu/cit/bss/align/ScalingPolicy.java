package cn.edu.bjtu.cit.bss.align;
import java.io.*;
import org.apache.commons.math.complex.*;
import pp.util.BLAS;
import cn.edu.bjtu.cit.bss.*;
import cn.edu.bjtu.cit.bss.util.*;

/**
 * <h1>Description</h1>
 * Used to solve the scaling ambiguities.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Mar 22, 2012 7:13:18 PM, revision:
 */
public abstract class ScalingPolicy implements Serializable
{
private static final long serialVersionUID=7619671152002020971L;
	
	/**
	 * calculate scaling factors from demixing model
	 * @param model
	 * the demixing model
	 * @return
	 * scaling factors for each source in each frequency bin, [source index][frequency bin index]
	 */
	public abstract Complex[][] scalingFactors(DemixingModel model);
	
	/**
	 * get scaling factors as frequency domain filters
	 * @param model
	 * a demixing model
	 * @return
	 * [source index][bin index, length=nfft]
	 */
	public Complex[][] fdFilters(DemixingModel model)
	{
	Complex[][] scale,fdf;
	
		scale=scalingFactors(model);

		fdf=new Complex[scale.length][model.fftSize()];
		for(int sourcei=0;sourcei<fdf.length;sourcei++) 
			for(int binidx=0;binidx<scale[sourcei].length;binidx++) 
			{
				fdf[sourcei][binidx]=scale[sourcei][binidx];
				
				//its complex conjugate counterpart, conjugate is required
				if(binidx>0&&binidx<model.fftSize()/2) 
					fdf[sourcei][fdf[sourcei].length-binidx]=scale[sourcei][binidx].conjugate();
			}

		return fdf;
	}
	
	/**
	 * get time domain filters for scaling factors
	 * @param model
	 * a demixing model
	 * @return
	 * [source index][tap index]
	 */
	public double[][] tdFilters(DemixingModel model)
	{
	Complex[][] fdf;
	double[][] tdf;
	
		fdf=fdFilters(model);
		
		tdf=new double[fdf.length][];
		for(int sourcei=0;sourcei<tdf.length;sourcei++) 
			tdf[sourcei]=SpectralAnalyzer.ifftReal(fdf[sourcei]);
		
		return tdf;
	}

	/**
	 * rescale the model to solve the scaling ambiguity
	 * @param model
	 * the demixing model
	 */
	public void rescale(DemixingModel model)
	{
	Complex[][] factors;
	Complex[][] demixm;
	
		factors=scalingFactors(model);//calculate scaling factors
		
		if(factors.length!=model.numSources()) throw new IllegalArgumentException(
				"number of sources not match: "+factors.length+", "+model.numSources());
		if(factors[0].length!=model.fftSize()/2+1) throw new IllegalArgumentException(
				"number of frequency bins not match: "+factors[0].length+", "+(model.fftSize()/2+1));
		
		//apply scaling factors
		for(int binidx=0;binidx<model.fftSize()/2+1;binidx++)
		{
			demixm=model.getDemixingMatrix(binidx);
			for(int sourcei=0;sourcei<demixm.length;sourcei++) 
				BLAS.scalarMultiply(factors[sourcei][binidx],demixm[sourcei],demixm[sourcei]);
		}
	}
	
	/**
	 * visualize scaling factors magnitude
	 * @param model
	 * a demixing model
	 */
	public void visualizeScalingMagnitude(DemixingModel model)
	{
	Complex[][] scale;
	double[][] magnitude;
		
		scale=scalingFactors(model);
	
		magnitude=new double[scale.length][scale[0].length];
		for(int sourcei=0;sourcei<scale.length;sourcei++) 
			for(int binidx=0;binidx<scale[sourcei].length;binidx++) 
				magnitude[sourcei][binidx]=scale[sourcei][binidx].abs();
		
		Util.plotSignals(magnitude);
	}
	
	/**
	 * visualize scaling factors phase
	 * @param model
	 * a demixing model
	 */
	public void visualizeScalingPhase(DemixingModel model)
	{
	Complex[][] scale;
	double[][] phase;
		
		scale=scalingFactors(model);
	
		phase=new double[scale.length][scale[0].length];
		for(int sourcei=0;sourcei<scale.length;sourcei++) 
			for(int binidx=0;binidx<scale[sourcei].length;binidx++) 
				phase[sourcei][binidx]=scale[sourcei][binidx].getArgument();
	
		Util.plotSignals(phase);		
	}
}
