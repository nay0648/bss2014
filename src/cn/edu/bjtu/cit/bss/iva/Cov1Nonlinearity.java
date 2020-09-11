package cn.edu.bjtu.cit.bss.iva;
import java.io.*;
import java.util.*;
import org.apache.commons.math.complex.*;
import org.apache.commons.math.util.*;
import org.apache.commons.math3.linear.*;
import pp.util.BLAS;

/**
 * <h1>Description</h1>
 * Only use the first eigenvalue and eigenvector.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Aug 21, 2012 3:41:52 PM, revision:
 */
public class Cov1Nonlinearity extends MahalNonlinearity
{
private static final long serialVersionUID=4124055287577578074L;
private double edth=1e-15;//threshold for eigenvalue calculation
private int maxedit=100;//max iteration times for power iteration

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
	private static final long serialVersionUID=-4575100585214480812L;
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
	
	public double[][] buildMahalTransform(int sourcei)
	{
	Complex[][][] ydata;
	double[][] cov;
	EigenContainer eigen1;
	
		ydata=this.getYData();
		
		//calculate covariance matrix
		{
			cov=new double[ydata.length][ydata.length];
			for(int tau=0;tau<ydata[0][0].length;tau++) 
				for(int f1=0;f1<ydata.length;f1++) 
					for(int f2=f1;f2<ydata.length;f2++) 
						cov[f1][f2]+=ydata[f1][sourcei][tau].abs()*ydata[f2][sourcei][tau].abs();
		
			for(int f1=0;f1<cov.length;f1++) 
				for(int f2=0;f2<cov[0].length;f2++) 
				{
					if(f2>=f1) cov[f1][f2]/=ydata[0][0].length;
					else cov[f1][f2]=cov[f2][f1];
				}
		}
	
		//perform eigen decomposition
		eigen1=eig1(cov);
		
		//construct the transform
		{
		double[][] tran;
			
			tran=new double[1][];
				
//			tran[0]=BLAS.scalarMultiply(1.0/Math.sqrt(eigen1.ed),eigen1.ev,eigen1.ev);
//			tran[0]=BLAS.scalarMultiply(Math.sqrt(eigen1.ed),eigen1.ev,eigen1.ev);
			tran[0]=eigen1.ev;
			
			return tran;
		}
	}

	public double[][] buildMahalTransform2(int sourcei)
	{
	Complex[][][] ydata;
	RealMatrix cov;
	EigenDecomposition eigen;
	
		ydata=this.getYData();
		
		//calculate covariance matrix
		{
		double temp;
		
			cov=new Array2DRowRealMatrix(ydata.length,ydata.length);
			for(int tau=0;tau<ydata[0][0].length;tau++) 
				for(int f1=0;f1<ydata.length;f1++) 
					for(int f2=f1;f2<ydata.length;f2++) 
					{
						temp=cov.getEntry(f1,f2);
						temp+=ydata[f1][sourcei][tau].abs()*ydata[f2][sourcei][tau].abs();
						cov.setEntry(f1,f2,temp);
					}
		
			for(int f1=0;f1<cov.getRowDimension();f1++) 
				for(int f2=0;f2<cov.getColumnDimension();f2++) 
				{
					if(f2>=f1) 
					{
						temp=cov.getEntry(f1,f2);
						temp/=ydata[0][0].length;
						cov.setEntry(f1,f2,temp);
					}
					else cov.setEntry(f1,f2,cov.getEntry(f2,f1));
				}
		}
	
		//perform eigen decomposition
		eigen=new EigenDecomposition(cov,MathUtils.SAFE_MIN);
		
		//construct the transform
		{
		double[][] tran;
		double ed;
		double[] ev;
			
			tran=new double[1][];
			
			ed=eigen.getRealEigenvalue(0);
			ev=eigen.getEigenvector(0).toArray();
				
//			tran[0]=BLAS.scalarMultiply(1.0/Math.sqrt(ed),ev,ev);
			tran[0]=BLAS.scalarMultiply(Math.sqrt(ed),ev,ev);
//			tran[0]=ev;
			
			return tran;
		}
	}
	
	public static void main(String[] args) throws IOException
	{
	Cov1Nonlinearity foo;
	double[][] matrix;
	
		foo=new Cov1Nonlinearity();
		
		matrix=BLAS.loadDoubleMatrix(new File("/home/nay0648/cov.txt"));
		foo.eig1(matrix);
	}
}
