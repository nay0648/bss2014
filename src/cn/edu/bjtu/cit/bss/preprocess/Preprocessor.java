package cn.edu.bjtu.cit.bss.preprocess;
import java.io.*;
import org.apache.commons.math.complex.*;
import pp.util.BLAS;

/**
 * <h1>Description</h1>
 * Used to perform data preprocessing before ICA.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Oct 19, 2011 2:43:19 PM, revision:
 */
public abstract class Preprocessor implements Serializable
{
private static final long serialVersionUID=-3792218348050868726L;
protected Complex[] signalmeans;//mean value of each channel
protected Complex[][] transform;//the transfer matrix

	/**
	 * Centering the signal, i.e. substract the signal mean from original signal.
	 * @param sigs
	 * Signal of all channels, will be replaced by the centered signals. Each row 
	 * for a channel.
	 * @return
	 * corresponding mean value
	 */
	public static double[] centering(double[][] sigs)
	{
	double[] mean;

		/*
		 * calculate the mean value for each signal
		 */
		mean=new double[sigs.length];
		for(int i=0;i<mean.length;i++) mean[i]=BLAS.mean(sigs[i]);
		
		//centering
		for(int i=0;i<mean.length;i++) BLAS.scalarAdd(-mean[i],sigs[i],sigs[i]);
		return mean;
	}

	/**
	 * substract signal means from each channel to get zero-mean signals
	 * @param sigs
	 * Input signals, each row is a channel, values will be modified.
	 * @return
	 * means for each channel
	 */
	public static Complex[] centering(Complex[][] sigs)
	{
	double[] meanr,meani;

		meanr=new double[sigs.length];
		meani=new double[sigs.length];

		/*
		 * calculate the mean value for each channel
		 */
		for(int i=0;i<sigs.length;i++) 
			for(int j=0;j<sigs[i].length;j++) 
			{
				meanr[i]+=sigs[i][j].getReal();
				meani[i]+=sigs[i][j].getImaginary();
			}
		
		BLAS.scalarMultiply(1.0/sigs[0].length,meanr,meanr);
		BLAS.scalarMultiply(1.0/sigs[0].length,meani,meani);

		//substract means from original signals
		for(int i=0;i<sigs.length;i++)
			for(int j=0;j<sigs[i].length;j++) 
				sigs[i][j]=new Complex(sigs[i][j].getReal()-meanr[i],sigs[i][j].getImaginary()-meani[i]);
		return BLAS.buildComplexVector(meanr,meani);
	}
	
	/**
	 * calculate covariance matrix for real signals
	 * @param sigs
	 * each row is a channel
	 * @return
	 */
	public static double[][] covarianceMatrix(double[][] sigs)
	{
	double[][] cov;
	
		cov=new double[sigs.length][sigs.length];
		
		//traverse all samples
		for(int j=0;j<sigs[0].length;j++) 
			//calculate outer product
			for(int ii=0;ii<sigs.length;ii++) 
				//symmetric
				for(int jj=ii;jj<sigs.length;jj++) 
					cov[ii][jj]+=sigs[ii][j]*sigs[jj][j];
		
		for(int i=0;i<cov.length;i++) 
			//symmetric
			for(int j=i;j<cov[i].length;j++) 
			{
				cov[i][j]/=sigs[0].length;
				cov[j][i]=cov[i][j];
			}
		
		return cov;
	}
	
	/**
	 * calculate the covariance matrix of multichannel signals in double array format
	 * @param sigs
	 * Multichannel signals, each row is a channel.
	 * @return
	 * The first channel is for real part, the second one is for imaginary part.
	 */
	public static double[][][] covarianceMatrix(Complex[][] sigs)
	{
	double[][] covr,covi;//the real and imaginary part of the covariance matrix
	double[][][] cov;
		
		covr=new double[sigs.length][sigs.length];
		covi=new double[sigs.length][sigs.length];
		cov=new double[2][][];
		cov[0]=covr;
		cov[1]=covi;
		
		//traverse each frame
		for(int j=0;j<sigs[0].length;j++)
			//calculate the outer product
			for(int ii=0;ii<sigs.length;ii++)
				//symmetric
				for(int jj=ii;jj<sigs.length;jj++)
				{
					/*
					 * (a+bi)(c-di)
					 */
					
					/*
					 * the transpose operation for complex vectors is Hermitian transpose
					 * ac+bd
					 */
					covr[ii][jj]+=sigs[ii][j].getReal()*sigs[jj][j].getReal()+
						sigs[ii][j].getImaginary()*sigs[jj][j].getImaginary();
					
					if(jj==ii) covi[ii][jj]=0;
					//bc-ad
					else covi[ii][jj]+=sigs[ii][j].getImaginary()*sigs[jj][j].getReal()-
						sigs[ii][j].getReal()*sigs[jj][j].getImaginary();
				}
		
		for(int i=0;i<covr.length;i++) 
			//symmetric
			for(int j=i;j<covr[i].length;j++) 
			{
				covr[i][j]/=sigs[0].length;
				covr[j][i]=covr[i][j];
				
				covi[i][j]/=sigs[0].length;
				covi[j][i]=-covi[i][j];//antisymmetric
			}

		return cov;	
	}
	
	/**
	 * calculate the pseudo covariance matrix: E{xx^T}
	 * @param sigs
	 * each row is a channel
	 * @return
	 * real part and imaginary part of the complex matrix
	 */
	public static double[][][] pseudoCovarianceMatrix(Complex[][] sigs)
	{
	double[][] covr,covi;//the real and imaginary part of the covariance matrix
	double[][][] cov;
			
		covr=new double[sigs.length][sigs.length];
		covi=new double[sigs.length][sigs.length];
		cov=new double[2][][];
		cov[0]=covr;
		cov[1]=covi;
		
		//traverse each frame
		for(int j=0;j<sigs[0].length;j++)
			//calculate the outer product
			for(int ii=0;ii<sigs.length;ii++)
				//symmetric
				for(int jj=ii;jj<sigs.length;jj++)
				{
					/*
					 * (a+bi)(c+di)
					 */
					
					//ac-bd
					covr[ii][jj]+=sigs[ii][j].getReal()*sigs[jj][j].getReal()-
						sigs[ii][j].getImaginary()*sigs[jj][j].getImaginary();
					
					//bc+ad
					covi[ii][jj]+=sigs[ii][j].getImaginary()*sigs[jj][j].getReal()+
						sigs[ii][j].getReal()*sigs[jj][j].getImaginary();
				}
		
		for(int i=0;i<covr.length;i++) 
			//symmetric
			for(int j=i;j<covr[i].length;j++) 
			{
				covr[i][j]/=sigs[0].length;
				covr[j][i]=covr[i][j];
				
				covi[i][j]/=sigs[0].length;
				covi[j][i]=covi[i][j];//symmetric
			}

		return cov;
	}
	
	/**
	 * calculate the transfer matrix used for preprocessing
	 * @param sigs
	 * already centered signals, each row is a channel
	 * @param numchout
	 * number of output channels
	 * @return
	 */
	public abstract Complex[][] calculateTransferMatrix(Complex[][] csigs,int numchout);
	
	/**
	 * perform preprocessing 
	 * @param sigs
	 * input signals, each row is a channel
	 * @param numchout
	 * number of output channels
	 * @return
	 */
	public Complex[][] preprocess(Complex[][] sigs,int numchout)
	{
		signalmeans=centering(sigs);
		transform=calculateTransferMatrix(sigs,numchout);
		return BLAS.multiply(transform,sigs,null);
	}
	
	/**
	 * get mean value of each channel of last preprocessing
	 * @return
	 */
	public Complex[] signalMeans()
	{
		return signalmeans;
	}
	
	/**
	 * get the transfer matrix of last preprocessing
	 * @return
	 */
	public Complex[][] transferMatrix()
	{
		return transform;
	}
}
