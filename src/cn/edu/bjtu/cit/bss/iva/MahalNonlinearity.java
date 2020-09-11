package cn.edu.bjtu.cit.bss.iva;
import org.apache.commons.math.complex.*;
import pp.util.BLAS;

/**
 * <h1>Description</h1>
 * Nonlinearity function based on Mahalanobis distance.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Jul 27, 2012 3:05:57 PM, revision:
 */
public abstract class MahalNonlinearity implements NonlinearityTable
{
private static final long serialVersionUID=2246949922728857040L;
private double a2=0.1;
private Complex[][][] ydata;//the source data [binidx][sourcei][tau]
private double[][] quad;//the quadratic form [sourcei][tau]
/*
 * intermediate results
 */
private int binidx0=-1,sourcei0=-1,tau0=-1;
private double g,dg,ddg;
	
	/**
	 * build the transform used to calculate M. distance
	 * @param sourcei
	 * source index
	 * @return
	 * [new dimension][frequency bin index]
	 */
	public abstract double[][] buildMahalTransform(int sourcei);

	public Complex[][][] getYData()
	{
		return ydata;
	}
	
	public void setYData(Complex[][][] ydata)
	{
	double[][] absy;//for |y|
	double[][] tran;//the M. transform	
	double[][] ty;//the transformed data
	
		/*
		 * initialize
		 */
		this.ydata=ydata;		
		absy=new double[ydata.length][ydata[0][0].length];
		quad=new double[ydata[0].length][ydata[0][0].length];
		
		//calculate the quadratic form source by source
		for(int sourcei=0;sourcei<ydata[0].length;sourcei++) 
		{
			//get data for one source
			for(int binidx=0;binidx<ydata.length;binidx++) 
				for(int tau=0;tau<ydata[0][0].length;tau++) 
					absy[binidx][tau]=ydata[binidx][sourcei][tau].abs();
			
			//build the M. transform
			tran=buildMahalTransform(sourcei);
			
			//the transformed data
			ty=BLAS.multiply(tran,absy,null);
			
			//calculate the quadratic form for current source
			for(int tau=0;tau<ydata[0][0].length;tau++) 
			{
				quad[sourcei][tau]=a2;
				
				/*
				 * Mahalanobis distance in the original space equals to Euclidean 
				 * distance in the transformed subspace.
				 */
				for(int tf=0;tf<ty.length;tf++) 
					quad[sourcei][tau]+=ty[tf][tau]*ty[tf][tau];
			}
		}
	}
	
	/**
	 * initialize nonlinearity
	 * @param binidx
	 * frequency bin index
	 * @param sourcei
	 * source index
	 * @param tau
	 * stft frame index
	 */
	private void initNonlinearity(int binidx,int sourcei,int tau)
	{
	double quad;
	
		//already initialized
		if(binidx0==binidx&&sourcei0==sourcei&&tau0==tau) return;
		
		binidx0=binidx;
		sourcei0=sourcei;
		tau0=tau;
		
		quad=this.quad[sourcei][tau];
		
		/*
		 * G=log
		 */
		g=Math.log(quad);
		dg=1.0/quad;
		ddg=-1.0/(quad*quad);
		
		/*
		 * SSL, G=sqrt
		 */
//		g=Math.sqrt(quad);
//		dg=1.0/(2.0*quad*quad);
//		ddg=-1.0/(4.0*Math.pow(quad,1.5));	
	}

	public double g(int binidx,int sourcei,int tau)
	{
		initNonlinearity(binidx,sourcei,tau);
		return g;
	}

	public double dg(int binidx,int sourcei,int tau)
	{
		initNonlinearity(binidx,sourcei,tau);
		return dg;
	}

	public double ddg(int binidx,int sourcei,int tau)
	{
		initNonlinearity(binidx,sourcei,tau);
		return ddg;
	}
}
