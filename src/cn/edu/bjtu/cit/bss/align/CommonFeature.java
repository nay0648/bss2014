package cn.edu.bjtu.cit.bss.align;
import java.io.*;
import java.awt.Color;
import java.awt.image.*;
import java.util.*;
import org.apache.commons.math.complex.*;
import cn.edu.bjtu.cit.bss.*;
import cn.edu.bjtu.cit.bss.util.ShortTimeFourierTransformer;
import pp.util.BLAS;

/**
 * <h1>Description</h1>
 * Features used in solving the permutation problem.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Nov 19, 2011 3:06:54 PM, revision:
 */
public class CommonFeature implements Serializable
{
private static final long serialVersionUID=2541061440754570093L;
private FDBSSAlgorithm fdbss;//bss algorithm reference

	/**
	 * @param fdbss
	 * bss algorithm reference
	 */
	public CommonFeature(FDBSSAlgorithm fdbss)
	{
		this.fdbss=fdbss;
	}
	
	/**
	 * get bss algorithm reference
	 * @return
	 */
	public FDBSSAlgorithm fdbssAlgorithm()
	{
		return fdbss;
	}
	
	/**
	 * calculate power ratio for a specified frequency bin
	 * @param model
	 * demixing model
	 * @param binidx
	 * frequency bin index
	 * @return
	 * Number of rows is the same as the number of estimated signals.
	 * @throws IOException
	 */
	public double[][] powerRatio(DemixingModel model,int binidx) throws IOException
	{
	Complex[][] bindata;//observed frequency bin data
	Complex[][] estdata;//estimated frequency bin data
	Complex[][] mix;//the mixing matrix
	Complex[] a,powv=null;
	double[][] powr;//the power ratio
	double sum;
		
		//load frequency bin data
		bindata=fdbss.binData(binidx,null);
		//calculate estimated frequency bin data
		estdata=BLAS.multiply(model.getDemixingMatrix(binidx),bindata,null);
		//corresponding mixing system
		mix=BLAS.pinv(model.getDemixingMatrix(binidx),null);

		a=new Complex[mix.length];//a column of mixing matrix
		powr=new double[estdata.length][estdata[0].length];
		
		//calculate the power for each channel
		for(int sourcei=0;sourcei<powr.length;sourcei++)
		{
			//get a column of the mixing matrix
			for(int k=0;k<a.length;k++) a[k]=mix[k][sourcei];

			//power for each channel
			for(int n=0;n<powr[sourcei].length;n++)
			{
				powv=BLAS.scalarMultiply(estdata[sourcei][n],a,powv);
				
				powr[sourcei][n]=BLAS.norm2(powv);
				powr[sourcei][n]=powr[sourcei][n]*powr[sourcei][n];
			}
		}

		//calculate the power ratio
		for(int n=0;n<powr[0].length;n++)
		{
			sum=0;
			for(int sourcei=0;sourcei<powr.length;sourcei++) sum+=powr[sourcei][n];
			for(int sourcei=0;sourcei<powr.length;sourcei++) powr[sourcei][n]/=sum;
		}

		return powr;
	}
	
	/**
	 * visualize power ratio of estimated data as image
	 * @param model
	 * the demixing model
	 * @throws IOException
	 */
	public void visualizePowerRatio(DemixingModel model) throws IOException
	{
	double[][][] dimg;//double format of power ratio spectrograms
	double[][] powr;//for power ratio
	BufferedImage[] img;//spectrograms
	Color[] colormap;
	int cidx;
		
		dimg=new double[fdbss.numSources()][fdbss.fftSize()/2+1][];
		
		//traverse half of bins
		for(int binidx=0;binidx<=fdbss.fftSize()/2;binidx++)
		{
			powr=powerRatio(model,binidx);//calculate power ratio
			
			//dispatch to corresponding spectrograms
			for(int sourcei=0;sourcei<powr.length;sourcei++) 
				dimg[sourcei][dimg[sourcei].length-binidx-1]=powr[sourcei];
		}
		
		/*
		 * convert to images
		 */
		img=new BufferedImage[dimg.length];
		colormap=ShortTimeFourierTransformer.colormapGray();
		
		for(int i=0;i<dimg.length;i++) 
		{
			img[i]=new BufferedImage(dimg[i][0].length,dimg[i].length,BufferedImage.TYPE_INT_RGB);
			
			for(int y=0;y<dimg[i].length;y++) 
				for(int x=0;x<dimg[i][0].length;x++) 
				{
					cidx=(int)Math.round(dimg[i][y][x]*colormap.length);
					if(cidx<0) cidx=0;else if(cidx>colormap.length-1) cidx=colormap.length-1;
					img[i].setRGB(x,y,colormap[cidx].getRGB());
				}
		}	
		
		//show results
		pp.util.Util.showImage(pp.util.Util.drawResult(1,img.length,5,img));
	}
	
	/**
	 * visualize the correlation coefficients between two estimated channels as image
	 * @param model
	 * the demixing model
	 * @param estch1
	 * index of the first channel
	 * @param estch2
	 * index of the second channel
	 * @throws IOException
	 */
	public void visualizePowerRatioCorrelation(DemixingModel model,int estch1,int estch2) throws IOException
	{
	List<double[]> powr1,powr2;
	double[][] powr;
	double[][] dcorr;
	BufferedImage icorr;
	Color[] cmap;
	int cidx;
	
		if(estch1<0||estch1>=fdbss.numSources()) throw new IndexOutOfBoundsException(
				"the first output channel index out of bounds: "+estch1+", "+fdbss.numSources());
		if(estch2<0||estch2>=fdbss.numSources()) throw new IndexOutOfBoundsException(
				"the second output channel index out of bounds: "+estch2+", "+fdbss.numSources());
	
		/*
		 * calculate the power ratio of two channels
		 */
		powr1=new ArrayList<double[]>(fdbss.fftSize()/2+1);
		powr2=new ArrayList<double[]>(fdbss.fftSize()/2+1);
		
		for(int binidx=0;binidx<=fdbss.fftSize()/2;binidx++)
		{
			powr=powerRatio(model,binidx);
			powr1.add(powr[estch1]);
			powr2.add(powr[estch2]);
		}
		
		/*
		 * calculate the correlation
		 */
		dcorr=new double[powr1.size()][powr2.size()];
		for(int i=0;i<dcorr.length;i++) 
			for(int j=0;j<dcorr[i].length;j++) 
				dcorr[i][j]=correlationCoefficient(powr1.get(i),powr2.get(j));
		
		/*
		 * convert to image
		 */
		icorr=new BufferedImage(dcorr[0].length,dcorr.length,BufferedImage.TYPE_INT_RGB);
		cmap=ShortTimeFourierTransformer.colormapGray();
		
		for(int y=0;y<dcorr.length;y++) 
			for(int x=0;x<dcorr[y].length;x++)
			{
				cidx=(int)Math.round(((dcorr[y][x]+1)/2.0)*cmap.length);
				if(cidx<0) cidx=0;else if(cidx>cmap.length-1) cidx=cmap.length-1;
				icorr.setRGB(x,y,cmap[cidx].getRGB());
			}
		
		pp.util.Util.showImage(icorr);
	}
		
	/**
	 * calculate the envelop for a specified frequency bin
	 * @param model
	 * demixing model
	 * @param binidx
	 * frequency bin index
	 * @return
	 * @throws IOException
	 */
	public double[][] envelop(DemixingModel model,int binidx) throws IOException
	{
	Complex[][] bindata;//observed frequency bin data
	Complex[][] estdata;//estimated frequency bin data
	double[][] e;
		
		//load frequency bin data
		bindata=fdbss.binData(binidx,null);
		//calculate estimated frequency bin data
		estdata=BLAS.multiply(model.getDemixingMatrix(binidx),bindata,null);

		e=new double[estdata.length][estdata[0].length];
		for(int i=0;i<e.length;i++) 
			for(int j=0;j<e[i].length;j++) e[i][j]=estdata[i][j].abs();

		return e;
	}
	
	/**
	 * visualize signal envelop of estimated data as image
	 * @param model
	 * the demixing model
	 * @throws IOException
	 */
	public void visualizeEnvelop(DemixingModel model) throws IOException
	{
	double[][][] dimg;//double format of power ratio spectrograms
	double[][] envelop;//for power ratio
	BufferedImage[] img;//spectrograms
	Color[] colormap;
	int cidx;
		
		dimg=new double[fdbss.numSources()][fdbss.fftSize()/2+1][];
		
		//traverse half of bins
		for(int binidx=0;binidx<=fdbss.fftSize()/2;binidx++)
		{
			envelop=envelop(model,binidx);//calculate power ratio
			
			//dispatch to corresponding spectrograms
			for(int sourcei=0;sourcei<envelop.length;sourcei++) 
				dimg[sourcei][dimg[sourcei].length-binidx-1]=envelop[sourcei];
		}
		
		/*
		 * convert to images
		 */
		img=new BufferedImage[dimg.length];
		colormap=ShortTimeFourierTransformer.colormapGray();
		
		for(int i=0;i<dimg.length;i++) 
		{
			img[i]=new BufferedImage(dimg[i][0].length,dimg[i].length,BufferedImage.TYPE_INT_RGB);
			
			for(int y=0;y<dimg[i].length;y++) 
				for(int x=0;x<dimg[i][0].length;x++) 
				{
					cidx=(int)Math.round(dimg[i][y][x]*colormap.length);
					if(cidx<0) cidx=0;else if(cidx>colormap.length-1) cidx=colormap.length-1;
					img[i].setRGB(x,y,colormap[cidx].getRGB());
				}
		}	
		
		//show results
		pp.util.Util.showImage(pp.util.Util.drawResult(1,img.length,5,img));
	}
	
	/**
	 * calculate the Euclidean distance of two vectors
	 * @param p1
	 * a vector
	 * @param p2
	 * another vector
	 * @return
	 */
	public static double distance(double[] p1,double[] p2)
	{
	double sum2=0,diff;
		
		if(p1.length!=p2.length) throw new IllegalArgumentException(
				"vector dimension do not match: "+p1.length+", "+p2.length);
		
		for(int i=0;i<p1.length;i++)
		{
			diff=p2[i]-p1[i];
			sum2+=diff*diff;
		}
		return Math.sqrt(sum2);		
	}
	
	/**
	 * calculate the correlation coefficient of two real vectors
	 * @param v1
	 * a vector
	 * @param v2
	 * another vector
	 * @return
	 * value belongs to [-1, 1]
	 */
	public static double correlationCoefficient(double[] v1,double[] v2)
	{
	double r12=0,mu1=0,mu2=0,sigma1=0,sigma2=0;
	
		if(v1.length!=v2.length) throw new IllegalArgumentException(
				"vector dimension not match: "+v1.length+", "+v2.length);
		
		for(int i=0;i<v1.length;i++)
		{
			r12+=v1[i]*v2[i];
			
			mu1+=v1[i];
			mu2+=v2[i];
			
			sigma1+=v1[i]*v1[i];
			sigma2+=v2[i]*v2[i];
		}
		
		r12/=v1.length;
		
		mu1/=v1.length;
		mu2/=v1.length;
		
		sigma1/=v1.length;
		sigma2/=v1.length;
		sigma1=Math.sqrt(sigma1-mu1*mu1);
		sigma2=Math.sqrt(sigma2-mu2*mu2);

		return (r12-mu1*mu2)/(sigma1*sigma2);
	}
	
	/**
	 * phase information of mixing matrices
	 * @param demixm
	 * demixing model
	 * @param binidx
	 * frequency bin index
	 * @param refidx
	 * reference channel index
	 * @return
	 * each row is a feature for an output channel
	 */
	public double[][] mixPhase(DemixingModel demixm,int binidx,int refidx)
	{
	Complex[][] esth;
	double[][] phase;
	int pidx;
	
		//estimated mixing matrix for the specified frequency bin
		esth=BLAS.pinv(demixm.getDemixingMatrix(binidx),null);
		
		phase=new double[esth[0].length][esth.length-1];
		for(int sourcei=0;sourcei<phase.length;sourcei++)
		{
			pidx=0;
			for(int sensorj=0;sensorj<esth.length;sensorj++) 
			{
				if(sensorj==refidx) continue;//skip reference index
				phase[sourcei][pidx++]=(esth[sensorj][sourcei].divide(esth[refidx][sourcei])).getArgument();
			}
		}
		
		return phase;
	}
}
