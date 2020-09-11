package cn.edu.bjtu.cit.bss;
import java.io.*;
import org.apache.commons.math.complex.*;
import cn.edu.bjtu.cit.bss.preprocess.*;
import cn.edu.bjtu.cit.bss.util.*;
import pp.util.BLAS;

/**
 * <h1>Description</h1>
 * Used to estimate the number of output channels, N is the number of sources.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Dec 26, 2011 9:54:03 PM, revision:
 */
public class NChooser implements Serializable
{
private static final long serialVersionUID=-434222618490313675L;
private FDBSS fdbss;//algorithm reference
private HermitianEigensolver solver=new CommonsEigensolver();//used to calculate eigenvalues

	/**
	 * @param fdbss
	 * algorithm reference
	 */
	public NChooser(FDBSS fdbss)
	{
		this.fdbss=fdbss;
	}
	
	/**
	 * calculate all eigenvalues of covariance matrix for a frequency band
	 * @param bindata
	 * data of a frequency band
	 * @return
	 */
	public double[] covEigs(Complex[][] bindata)
	{
	double[][][] cov;
	HermitianEigensolver.EigenDecomposition eigen;
	double[] ed;
	
		Preprocessor.centering(bindata);
		cov=Preprocessor.covarianceMatrix(bindata);
		
		eigen=solver.eig(cov[0],cov[1]);
		ed=new double[bindata.length];
		//all eigenvalues are real
		for(int i=0;i<ed.length;i++) ed[i]=eigen.eigenvalue(i).getReal();

		return ed;
	}
	
	/**
	 * calculate covariance matrix eigenvalues for all frequency bins
	 * @return
	 * @throws IOException
	 * each row is eigenvalues for a frequency bin
	 */
	public double[][] covEigs() throws IOException
	{
	Complex[][] bindata=null;
	double[][] eds;
	
		eds=new double[fdbss.fftSize()/2+1][];
		
		for(int binidx=0;binidx<eds.length;binidx++)
		{
			bindata=fdbss.binData(binidx,bindata);
			eds[binidx]=covEigs(bindata);			
		}
		
		return eds;
	}
	
	public static void main(String[] args) throws IOException
	{
	NChooser foo;
	
		foo=new NChooser(new FDBSS(new File("temp")));
		BLAS.save(foo.covEigs(),new File("/home/nay0648/coveigs.txt"));
	}
}
