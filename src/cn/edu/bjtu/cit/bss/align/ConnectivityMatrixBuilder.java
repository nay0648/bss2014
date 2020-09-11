package cn.edu.bjtu.cit.bss.align;
import java.io.*;
import java.awt.image.*;
import org.apache.commons.math.linear.*;
import org.apache.commons.math.util.*;
import cn.edu.bjtu.cit.bss.*;

/**
 * <h1>Description</h1>
 * Build connectivity matrix from affinity matrix. See: Chris Ding, et al. 
 * Linearized Cluster Assignment via Spectral Ordering.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Nov 14, 2011 6:53:23 PM, revision:
 */
public class ConnectivityMatrixBuilder implements AffinityPreprocessor
{
private static final long serialVersionUID=-1148850948701623744L;
private int order;//order of approximation
private double beta=0.8;//the threshold

	/**
	 * @param order
	 * order of approximation
	 */
	public ConnectivityMatrixBuilder(int order)
	{
		this.order=order;
	}

	public AffinityMatrix preprocess(AffinityMatrix am)
	{
	double[] d;//the degree
	AffinityMatrix laplacian;

		//build the normalized Laplacian matrix
		{
		double temp;
		double[] msqrtd;
			
			/*
			 * calculate the diagonal matrix D
			 */
			d=new double[am.size()];
			for(AffinityMatrix.Entry entry:am) d[entry.rowIndex()]+=entry.value();
					
			/*
			 * calculate D^(-1/2)	
			 */
			msqrtd=new double[d.length];
			for(int i=0;i<msqrtd.length;i++) msqrtd[i]=1.0/Math.sqrt(d[i]);
				
			/*
			 * calculate L=D^(-1/2)*S*D^(-1/2) into similarity matrix
			 */
			laplacian=new DenseAffinityMatrix(am.size());
			for(AffinityMatrix.Entry entry:am)
			{
				if(entry.rowIndex()>=entry.columnIndex()) continue;//symmetric, diagonal is 0
				temp=msqrtd[entry.rowIndex()]*entry.value()*msqrtd[entry.columnIndex()];
				laplacian.setAffinity(entry.rowIndex(),entry.columnIndex(),temp);
				laplacian.setAffinity(entry.columnIndex(),entry.rowIndex(),temp);
			}

			//make Laplacian matrix positive semidefinite
			for(int i=0;i<laplacian.size();i++) laplacian.setAffinity(i,i,1);
		}

		//build connectivity matrix
		{
		EigenDecompositionImpl eigen;
		double[][] wh,conn;
		double[] evector;
		double[] sqrtd;
		double temp;
		
			//calculate the eigen decomposition
			eigen=new EigenDecompositionImpl(laplacian.toCommonosMatrix(),MathUtils.SAFE_MIN);

			/*
			 * build approximation of the affinity matrix
			 */
			wh=new double[laplacian.size()][laplacian.size()];
			for(int i=0;i<order;i++)
			{
				evector=eigen.getEigenvector(i).getData();
				
				//calculate the outer product
				for(int ii=0;ii<evector.length;ii++) 
					//symmetric, diagonal is included
					for(int jj=ii;jj<evector.length;jj++) 
					{
						temp=evector[ii]*evector[jj];
						wh[ii][jj]+=temp;
						if(ii!=jj) wh[jj][ii]+=temp;
					}
			}

			/*
			 * build connectivity matrix
			 */
			sqrtd=new double[d.length];
			for(int i=0;i<sqrtd.length;i++) sqrtd[i]=Math.sqrt(d[i]);
			
			conn=new double[wh.length][wh[0].length];
			for(int i=0;i<conn.length;i++) 
				//symmetric
				for(int j=i;j<conn[i].length;j++) 
				{
					temp=sqrtd[i]*wh[i][j]*sqrtd[j];
					conn[i][j]=temp;
					conn[j][i]=temp;
				}
			
			/*
			 * apply threshold
			 */
			for(int i=0;i<conn.length;i++) 
				//symmetric
				for(int j=i;j<conn[i].length;j++) 
					if(conn[i][j]/(Math.sqrt(conn[i][i]*conn[j][j]))<beta)	
					{
						conn[i][j]=0;
						conn[j][i]=0;
					}
			
			return new DenseAffinityMatrix(conn);
		}
	}
	
	public static void main(String[] args) throws IOException
	{
	int offset=0,len=150;
	FDBSS fdbss;
	DemixingModel demixm;
	AffinityMatrixBuilder ambuilder;
	AffinityMatrix am,am2;
	BufferedImage img,img2;
	
		fdbss=new FDBSS(new File("temp"));
		demixm=fdbss.loadDemixingModel();
		
		ambuilder=new DefaultAffinityMatrixBuilder(fdbss,3);
		am=ambuilder.buildAffinityMatrix(demixm,offset,len);
		img=am.toImage(false);
		
		am2=(new ConnectivityMatrixBuilder(fdbss.numSources())).preprocess(am);
		img2=am2.toImage(false);
		
		pp.util.Util.showImage(pp.util.Util.drawResult(1,2,5,img,img2));
	}
}
