package cn.edu.bjtu.cit.bss.eval;
import java.io.*;
import java.util.*;
import org.apache.commons.math.complex.*;
import pp.util.BLAS;
import cn.edu.bjtu.cit.bss.*;
import cn.edu.bjtu.cit.bss.util.*;

/**
 * <h1>Description</h1>
 * Use mixing model and corresponding demixing model to evaluate algorithm performance.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Apr 6, 2012 4:34:07 PM, revision:
 */
public class EvalModel extends BSSModel
{
private static final long serialVersionUID=-6702737463068221121L;
private BSSModel mixm;//the mixing model
private BSSModel demixm;//corresponding demixing model

	/**
	 * @param mixm
	 * mixing model used to generate sensor signals
	 * @param demixm
	 * corresponding demixing model calculated by bss algorithm
	 */
	public EvalModel(BSSModel mixm,BSSModel demixm)
	{
		if(mixm.numSources()!=demixm.numSources()) throw new IllegalArgumentException(
				"number of sources not match: "+mixm.numSources()+", "+demixm.numSources());
		if(mixm.numSensors()!=demixm.numSensors()) throw new IllegalArgumentException(
				"number of sensors not match: "+mixm.numSensors()+", "+demixm.numSensors());
		if(mixm.fftSize()!=demixm.fftSize()) throw new IllegalArgumentException(
				"FFT block size not match: "+mixm.fftSize()+", "+demixm.fftSize());
		
		this.mixm=mixm;
		this.demixm=demixm;
	}
	
	public int numSources()
	{
		return mixm.numSources();
	}
	
	public int numSensors()
	{
		return mixm.numSensors();
	}
	
	public int fftSize()
	{
		return mixm.fftSize();
	}

	public double[][][] tdFilters()
	{
	double[][][] w,a,t;
	double[] temp=null;
	
		w=demixm.tdFilters();
		a=mixm.tdFilters();
		
		/*
		 * T=WA
		 */
		t=new double[w.length][a[0].length][];
		for(int i=0;i<t.length;i++) 
			for(int j=0;j<t[i].length;j++) 
				for(int k=0;k<w[i].length;k++) 
				{
					temp=Filter.convolve(w[i][k],a[k][j],temp,Filter.Padding.zero);
					
					if(t[i][j]==null) t[i][j]=new double[temp.length];
					BLAS.add(t[i][j],temp,t[i][j]);
				}

		return t;
	}
	
	public Complex[][][] fdFilters()
	{
	Complex[][][] w,a,t;
	Complex[] temp=null;
		
		w=demixm.fdFilters();
		a=mixm.fdFilters();
	
		/*
		 * T=WA
		 */
		t=new Complex[w.length][a[0].length][];
		for(int i=0;i<t.length;i++) 
			for(int j=0;j<t[i].length;j++) 
				for(int k=0;k<w[i].length;k++) 
				{
					temp=BLAS.entryMultiply(w[i][k],a[k][j],temp);

					if(t[i][j]==null) 
					{	
						t[i][j]=new Complex[temp.length];
						Arrays.fill(t[i][j],Complex.ZERO);
					}
					BLAS.add(t[i][j],temp,t[i][j]);
			}

		return t;
	}
	
	/**
	 * Performance index (PI) for each frequency bin. The smaller pi value, 
	 * the better ICA performance in a frequency bin. See: Yan Li, David Powers 
	 * and James Peach, Comparison of Blind Source Separation Algorithms, for 
	 * details.
	 * @return
	 * e1 criteria on the full frequency band (length=nfft/2+1)
	 */
	public double[] pi()
	{
	Complex[][][] fdf;
	double[] e1;
	double[] er,maxer,ec,maxec;
	double abs;
			
		fdf=this.fdFilters();
		e1=new double[this.fftSize()/2+1];
		er=new double[fdf.length];
		maxer=new double[fdf.length];
		ec=new double[fdf[0].length];
		maxec=new double[fdf[0].length];
		
		for(int binidx=0;binidx<this.fftSize()/2+1;binidx++)
		{
			Arrays.fill(maxer,Double.MIN_VALUE);
			Arrays.fill(maxec,Double.MIN_VALUE);
			Arrays.fill(er,0);
			Arrays.fill(ec,0);
				
			for(int i=0;i<fdf.length;i++) 
				for(int j=0;j<fdf[i].length;j++) 
				{
					abs=fdf[i][j][binidx].abs();
						
					er[i]+=abs;
					if(abs>maxer[i]) maxer[i]=abs;
						
					ec[j]+=abs;
					if(abs>maxec[j]) maxec[j]=abs;
				}
				
			for(int i=0;i<er.length;i++) er[i]=er[i]/maxer[i]-1;
			for(int j=0;j<ec.length;j++) ec[j]=ec[j]/maxec[j]-1;
				
			e1[binidx]=BLAS.sum(er)/fdf.length+BLAS.sum(ec)/fdf[0].length;
		}
		
		return e1;
	}

	public static void main(String[] args) throws IOException
	{
	MixingModel mixm;
	DemixingModel demixm;
	EvalModel eval;
	
		mixm=new MixingModel((new VirtualRoom(new File("data/VirtualRooms/2x2/SawadaRoom.txt"))).tdFilters(),4096);
		demixm=(new FDBSS(new File("temp"))).loadDemixingModel();
		eval=new EvalModel(mixm,demixm);

		eval.visualize();
		eval.fdVisualize();
		Util.plotSignals(eval.pi());
	}
}
