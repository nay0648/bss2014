package cn.edu.bjtu.cit.bss.align;
import java.util.*;
import org.apache.commons.math.complex.*;
import pp.util.BLAS;
import cn.edu.bjtu.cit.bss.*;

/**
 * <h1>Description</h1>
 * Select the max contribution from one source to all sensors to rescale.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Apr 19, 2012 10:27:55 AM, revision:
 */
public class MaxContributionRescale extends ScalingPolicy
{
private static final long serialVersionUID=-5227727087143725292L;

	public Complex[][] scalingFactors(DemixingModel model)
	{
	Complex[][] scale;
	Complex[][] demix,mix=null;
	double abs;
	double[] maxabs;
		
		scale=new Complex[model.numSources()][model.fftSize()/2+1];
		maxabs=new double[model.numSources()];
						
		for(int binidx=0;binidx<model.fftSize()/2+1;binidx++)
		{
			demix=model.getDemixingMatrix(binidx);
			mix=BLAS.pinv(demix,mix);//estimate corresponding mixing matrix

			/*
			 * find the max scale for each source
			 */
			Arrays.fill(maxabs,Double.MIN_VALUE);			
			for(int sensorj=0;sensorj<mix.length;sensorj++) 
				for(int sourcei=0;sourcei<mix[sensorj].length;sourcei++)
				{
					abs=mix[sensorj][sourcei].abs();
					if(abs>maxabs[sourcei]) 
					{
						maxabs[sourcei]=abs;
						scale[sourcei][binidx]=mix[sensorj][sourcei];
					}
				}
		}
		
		return scale;
	}	
}
