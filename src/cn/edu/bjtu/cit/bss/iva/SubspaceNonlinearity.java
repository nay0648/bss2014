package cn.edu.bjtu.cit.bss.iva;
import java.io.*;
import java.util.*;
import org.apache.commons.math.complex.*;
import cn.edu.bjtu.cit.bss.preprocess.*;
import pp.util.BLAS;

/**
 * <h1>Description</h1>
 * Nonlinearity based only on the dominant eigenvector.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Sep 2, 2012 10:02:10 AM, revision:
 */
public class SubspaceNonlinearity implements NonlinearityTable
{
private static final long serialVersionUID=7408259694942974010L;
private double a2=0.1;//a small constant
private double edth=1e-15;//threshold for eigenvalue calculation
private int maxedit=100;//max iteration times for power iteration
//the quadratic form, indexed by source index
private Map<Integer,double[]> quadmap=new HashMap<Integer,double[]>();

	/**
	 * <h1>Description</h1>
	 * Eigenvalue and corresponding eigenvector.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Aug 30, 2012 8:32:40 AM, revision:
	 */
	private class EigenContainer implements Serializable
	{
	private static final long serialVersionUID=-7748743447683758924L;
	double ed;
	double[] ev;

		public EigenContainer(int d)
		{
			ev=new double[d];
		}
	}
	
	/**
	 * power method to calculate the largest magnitude eigenvalue and 
	 * corresponding eigenvector
	 * @param cov
	 * symmetric covariance matrix
	 * @return
	 */
	private EigenContainer eig1(double[][] cov)
	{
	EigenContainer eigen1;
	double[] ev1,swap;
	double cos,temp;
	
		if(cov.length!=cov[0].length) throw new IllegalArgumentException(
				"square matrix required: "+cov.length+" x "+cov[0].length);
		
		/*
		 * initialize
		 */
		eigen1=new EigenContainer(cov.length);
		Arrays.fill(eigen1.ev,1);
		BLAS.normalize(eigen1.ev);
		
		ev1=new double[cov.length];
		
		//perform iteration
		for(int itcount=0;itcount<maxedit;itcount++)
		{
			/*
			 * calculate new eigenvector
			 */
			Arrays.fill(ev1,0);
			
			for(int i=0;i<cov.length;i++) 
				for(int k=0;k<cov[i].length;k++) 
					ev1[i]+=cov[i][k]*eigen1.ev[k];
			
			BLAS.normalize(ev1);
		
			/*
			 * convergence test
			 */
			cos=Math.abs(BLAS.innerProduct(eigen1.ev,ev1));

			/*
			 * swap space
			 */
			swap=eigen1.ev;
			eigen1.ev=ev1;
			ev1=swap;
			
			if(Math.abs(1-cos)<=edth) break;
		}
		
		//calculate the eigenvalue
		for(int i=0;i<cov.length;i++) 
			for(int j=i;j<cov[i].length;j++) 
			{
				temp=ev1[j]*cov[j][i]*ev1[i];
				eigen1.ed+=temp;
				
				if(i!=j) eigen1.ed+=temp;
			}

		return eigen1;
	}

	public void setYData(Complex[][][] ydata)
	{
	double[][] absy,cov;
	
		absy=new double[ydata.length][ydata[0][0].length];
		
		for(int sourcei=0;sourcei<ydata[0].length;sourcei++) 
		{
			//get data for one source
			for(int binidx=0;binidx<ydata.length;binidx++) 
				for(int tau=0;tau<ydata[0][0].length;tau++) 
					absy[binidx][tau]=ydata[binidx][sourcei][tau].abs();
			
			//calculate the covariance matrix
			cov=Preprocessor.covarianceMatrix(absy);
			
			setYData(sourcei,absy,cov);
		}
	}
	
	/**
	 * set source data to nonlinearity
	 * @param sourcei
	 * source index
	 * @param absy
	 * corresponding |y|
	 * @param cov
	 * corresponding covariance matrix
	 */
	public void setYData(int sourcei,double[][] absy,double[][] cov)
	{
	EigenContainer eigen1;	
	double[] tran,quad;
	double ty;
	
		//perform eigen decomposition
		eigen1=eig1(cov);
	
		//construct the transform
//		tran=BLAS.scalarMultiply(1.0/Math.sqrt(eigen1.ed),eigen1.ev,eigen1.ev);
		tran=BLAS.scalarMultiply(Math.sqrt(eigen1.ed),eigen1.ev,eigen1.ev);
//		tran=eigen1.ev;
	
		/*
		 * calculate the quadratic form
		 */
		quad=quadmap.get(sourcei);
		if(quad==null) 
		{
			quad=new double[absy[0].length];
			quadmap.put(sourcei,quad);
		}
		
		for(int tau=0;tau<absy[0].length;tau++) 
		{
			/*
			 * calculate the transformed data, 
			 * projecting to the dominant eigenvector
			 */
			ty=0;
			for(int binidx=0;binidx<absy.length;binidx++) 
				ty+=tran[binidx]*absy[binidx][tau];
			
			/*
			 * Mahalanobis distance in the original space equals to Euclidean 
			 * distance in the transformed subspace.
			 */
			quad[tau]=a2+ty*ty;
		}
	}
	
	public double g(int binidx,int sourcei,int tau)
	{
	double quad;
	
		quad=quadmap.get(sourcei)[tau];
		return Math.log(quad);
	}
	
	public double dg(int binidx,int sourcei,int tau)
	{
	double quad;
		
		quad=quadmap.get(sourcei)[tau];
		return 1.0/quad;
	}

	public double ddg(int binidx,int sourcei,int tau)
	{
	double quad;
		
		quad=quadmap.get(sourcei)[tau];
		return -1.0/(quad*quad);
	}
}
