package cn.edu.bjtu.cit.bss.iva;
import org.apache.commons.math.complex.*;
import org.apache.commons.math.util.*;
import org.apache.commons.math3.linear.*;
import pp.util.BLAS;

/**
 * <h1>Description</h1>
 * Construct M. matrix according to covariance matrix.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Aug 15, 2012 9:19:34 AM, revision:
 */
public class CovNonlinearity extends MahalNonlinearity
{
private static final long serialVersionUID=2402225337089152679L;
private double edenergyth=0.95;//eigenvalue energy threshold
private int maxnumed=50;//maximum number of eigenvalues

	public double[][] buildMahalTransform(int sourcei)
	{
	Complex[][][] ydata;
	RealMatrix cov;
	EigenDecomposition eigen;
	int numed;
	
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
		
		//find the number of eigenvalues
		{
		double ed,energy=0,penergy=0;
		
			for(int i=0;i<Math.min(maxnumed,cov.getRowDimension());i++) 
			{
				ed=eigen.getRealEigenvalue(i);
				energy+=ed*ed;
			}

			for(numed=1;numed<=Math.min(maxnumed,cov.getRowDimension());numed++) 
			{
				ed=eigen.getRealEigenvalue(numed-1);
				penergy+=ed*ed;
				
				if(penergy/energy>=edenergyth) break;
			}
			if(numed>Math.min(maxnumed,cov.getRowDimension())) 
				numed=Math.min(maxnumed,cov.getRowDimension());
		}
		
		//construct the transform
		{
		double[][] tran;
		double ed;
		double[] ev;
			
			tran=new double[numed][];
			
			for(int tf=0;tf<tran.length;tf++) 
			{
				ed=eigen.getRealEigenvalue(tf);
				ev=eigen.getEigenvector(tf).toArray();
				
//				tran[tf]=BLAS.scalarMultiply(1.0/Math.sqrt(ed),ev,ev);
				
				tran[tf]=BLAS.scalarMultiply(Math.sqrt(ed),ev,ev);
				
//				tran[tf]=ev;
			}
			
			return tran;
		}
	}
}
