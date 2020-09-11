package cn.edu.bjtu.cit.bss.preprocess;
import org.apache.commons.math.complex.*;
import pp.util.BLAS;

/**
 * <h1>Description</h1>
 * Do just centering on input data.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Oct 28, 2011 9:36:00 AM, revision:
 */
public class Centering extends Preprocessor
{
private static final long serialVersionUID=-4516080440610639938L;

	public Complex[][] calculateTransferMatrix(Complex[][] csigs,int numchout)
	{
		if(csigs.length!=numchout) throw new IllegalArgumentException(
				"number of output channels not match: "+csigs.length+", "+numchout);
		
		return BLAS.eyeComplex(csigs.length,csigs.length);
	}
}
