package cn.edu.bjtu.cit.bss.align;
import org.apache.commons.math.complex.*;
import pp.util.BLAS;
import cn.edu.bjtu.cit.bss.*;

/**
 * <h1>Description</h1>
 * Rescale by minimal distortion principle (MDL). See: K. Matsuoka, 
 * Minimal Distortion Principle for Blind Source Separation, SICE, 
 * vol. 4, pp. 2138-2143, 2002.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Apr 19, 2012 10:33:31 AM, revision:
 */
public class MDLRescale extends ScalingPolicy
{
private static final long serialVersionUID=8278063271474918988L;

	public Complex[][] scalingFactors(DemixingModel model)
	{
	Complex[][] scale,demix,mix=null;
		
		scale=new Complex[model.numSources()][model.fftSize()/2+1];

		for(int binidx=0;binidx<model.fftSize()/2+1;binidx++)
		{
			demix=model.getDemixingMatrix(binidx);
			//estimate the mixing matrix for current frequency bin
			mix=BLAS.pinv(demix,mix);
			
			for(int sourcei=0;sourcei<demix.length;sourcei++) 
				scale[sourcei][binidx]=mix[sourcei][sourcei];
		}
		
		return scale;
	}
}
