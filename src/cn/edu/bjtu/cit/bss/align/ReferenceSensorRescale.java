package cn.edu.bjtu.cit.bss.align;
import org.apache.commons.math.complex.*;
import pp.util.BLAS;
import cn.edu.bjtu.cit.bss.*;

/**
 * <h1>Description</h1>
 * Adjust scale according to a reference sensor, the rescaled signals are equal to 
 * the recorded components by the reference sensor. This is especially useful when 
 * a standard sensor is required in the data capture procedure.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Apr 19, 2012 10:41:56 AM, revision:
 */
public class ReferenceSensorRescale extends ScalingPolicy
{
private static final long serialVersionUID=347149416644025824L;
private int refidx=0;//reference sensor index

	/**
	 * get reference sensor index
	 * @return
	 */
	public int getReferenceSensorIndex()
	{
		return refidx;
	}
	
	/**
	 * set reference sensor index
	 * @param refidx
	 * new index
	 */
	public void setReferenceSensorIndex(int refidx)
	{
		this.refidx=refidx;
	}

	/**
	 * calculate scaling factors according to a reference sensor
	 * @param model
	 * the demixing model
	 * @param refidx
	 * reference sensor index
	 * @return
	 */
	public Complex[][] scalingFactors(DemixingModel model,int refidx)
	{
	Complex[][] scale,demix,mix=null;
		
		if(refidx<0||refidx>=model.numSensors()) throw new IndexOutOfBoundsException(
				"sensor index out of bounds: "+refidx+", "+model.numSensors());
		
		scale=new Complex[model.numSources()][model.fftSize()/2+1];
		
		for(int binidx=0;binidx<model.fftSize()/2+1;binidx++)
		{
			demix=model.getDemixingMatrix(binidx);
			mix=BLAS.pinv(demix,mix);//corresponding mixing matrix
			
			for(int sourcei=0;sourcei<demix.length;sourcei++) 
				scale[sourcei][binidx]=mix[refidx][sourcei];
		}
		
		return scale;
	}

	public Complex[][] scalingFactors(DemixingModel model)
	{
		return scalingFactors(model,refidx);
	}
}
