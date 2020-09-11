package cn.edu.bjtu.cit.bss.align;
import java.util.*;
import org.apache.commons.math.complex.*;
import cn.edu.bjtu.cit.bss.*;

/**
 * <h1>Description</h1>
 * Not perform rescale, used for experiments.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Apr 19, 2012 11:10:31 AM, revision:
 */
public class NotRescaled extends ScalingPolicy
{
private static final long serialVersionUID=6190766259379366180L;

	public Complex[][] scalingFactors(DemixingModel model)
	{
	Complex[][] scale;
	
		scale=new Complex[model.numSources()][model.fftSize()/2+1];
		for(int sourcei=0;sourcei<scale.length;sourcei++) 
			Arrays.fill(scale[sourcei],Complex.ONE);
		return scale;
	}
}
