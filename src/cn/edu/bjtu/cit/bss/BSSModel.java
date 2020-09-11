package cn.edu.bjtu.cit.bss;
import java.io.*;
import java.util.*;
import org.apache.commons.math.complex.*;
import pp.util.BLAS;
import cn.edu.bjtu.cit.bss.signalio.*;
import cn.edu.bjtu.cit.bss.util.*;

/**
 * <h1>Description</h1>
 * Abstract class for mixing and demixing model.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Mar 19, 2012 8:55:08 PM, revision:
 */
public abstract class BSSModel implements Serializable
{
private static final long serialVersionUID=-1230079485615357764L;
private static final int BUFFER_SIZE=1024;

	/**
	 * get number of sources
	 * @return
	 */
	public abstract int numSources();

	/**
	 * get number of sensors
	 * @return
	 */
	public abstract int numSensors();

	/**
	 * get fft block size
	 * @return
	 */
	public abstract int fftSize();
	
	/**
	 * get time domain filters
	 * @return
	 */
	public abstract double[][][] tdFilters();
	
	/**
	 * get frequency domain filters
	 * @return
	 */
	public abstract Complex[][][] fdFilters();
	
	/**
	 * apply time domain filters to signal stream
	 * @param in
	 * multichannel signal source stream
	 * @param out
	 * multichannel signal output stream for results
	 * @throws IOException
	 */
	public void applyTDFilters(SignalSource in,SignalSink out) throws IOException
	{
	double[][][] tdf;
	SignalMixer mixer;
	double[][] buffer;
	
		tdf=tdFilters();
		if(in.numChannels()!=tdf[0].length) throw new IllegalArgumentException(
				"number of input channels not match: "+in.numChannels()+", "+tdf[0].length);
		if(out.numChannels()!=tdf.length) throw new IllegalArgumentException(
				"number of output channels not match: "+out.numChannels()+", "+tdf.length);
		
		mixer=new SignalMixer(tdf,false,in);
		buffer=new double[mixer.numChannels()][BUFFER_SIZE];
		for(int count=0;(count=mixer.readSamples(buffer))>=0;) 
			out.writeSamples(buffer,0,count);
	}
	
	/**
	 * apply time domain filters to signal stream
	 * @param in
	 * multichannel signal input stream
	 * @param out
	 * each one for a single channel output stream
	 * @throws IOException
	 */
	public void applyTDFilters(SignalSource in,SignalSink[] out) throws IOException
	{
	double[][][] tdf;
	SignalMixer mixer;
	double[][] buffer;
		
		tdf=tdFilters();
		if(in.numChannels()!=tdf[0].length) throw new IllegalArgumentException(
				"number of input channels not match: "+in.numChannels()+", "+tdf[0].length);
		if(out.length!=tdf.length) throw new IllegalArgumentException(
				"number of output channels not match: "+out.length+", "+tdf.length);
		
		mixer=new SignalMixer(tdf,false,in);
		buffer=new double[mixer.numChannels()][BUFFER_SIZE];
		for(int count=0;(count=mixer.readSamples(buffer))>=0;) 
			for(int chidx=0;chidx<out.length;chidx++) 
				out[chidx].writeSamples(buffer[chidx],0,count);
	}
	
	/**
	 * apply time domain filters to signal stream
	 * @param in
	 * each one for a single channel input stream
	 * @param out
	 * each one for a single channel output stream
	 * @throws IOException
	 */
	public void applyTDFilters(SignalSource[] in,SignalSink[] out) throws IOException
	{
		applyTDFilters(new SignalMixer(in),out);
	}
	
	/**
	 * apply frequency domain filters to signal's STFT stream
	 * @param in
	 * STFT streams for input signals
	 * @param out
	 * STFT streams for output signals
	 * @throws IOException
	 */
	public void applyFDFilters(SignalSource[] in,SignalSink[] out) throws IOException
	{
	Complex[][][] fdf;
	Complex[][] framein;
	Complex[] tempadd,tempmul;
			
		fdf=fdFilters();//get frequency domain filters
	
		/*
		 * check compatibility
		 */
		if(in.length!=fdf[0].length) throw new IllegalArgumentException(
				"number of input channels not match: "+in.length+", "+fdf[0].length);
		if(out.length!=fdf.length) throw new IllegalArgumentException(
				"number of output channels not match: "+out.length+", "+fdf.length);

		/*
		 * calculate estimated data
		 */	
		framein=new Complex[fdf[0].length][fftSize()];
		tempadd=new Complex[fftSize()];
		tempmul=new Complex[fftSize()];
		
eof:	for(;;)
		{
			//read frame from sensor stft files	
			for(int inidx=0;inidx<framein.length;inidx++)
			{
				try
				{
					in[inidx].readFrame(framein[inidx]);
				}
				catch(EOFException e)
				{
					break eof;
				}
			}

			//calculate mixed frames for each output
			for(int outidx=0;outidx<fdf.length;outidx++)
			{
				Arrays.fill(tempadd,Complex.ZERO);
				
				for(int inidx=0;inidx<fdf[outidx].length;inidx++)
				{
					/*
					 * entry multiplication in frequency equals convolution 
					 * with periodic padding in time domain
					 */
					tempmul=BLAS.entryMultiply(fdf[outidx][inidx],framein[inidx],tempmul);
					tempadd=BLAS.add(tempadd,tempmul,tempadd);
				}
				
				out[outidx].writeFrame(tempadd);
			}
		}
	}
	
	/**
	 * parse complex matrix from string information, the format is: <br>
	 * real part:<br>
	 * x x x<br>
	 * x x x<br>
	 * <br>
	 * imaginary part:<br>
	 * x x x<br>
	 * x x x<br>
	 * @param info
	 * information contains a complex matrix
	 * @return
	 */
	private static Complex[][] parseComplexMatrix(List<String> info)
	{
	List<String[]> sreal,simag;
	int state=0;//0 for nothing, 1 for real part, 2 for imaginary part
	Complex[][] matrix;
	Iterator<String[]> itr,itc;
	String[] rowr,rowc;
	
		sreal=new LinkedList<String[]>();
		simag=new LinkedList<String[]>();
		//dispatch information
		for(String ts:info)
		{
			if(ts.startsWith("real part:")) state=1;
			else if(ts.startsWith("imaginary part:")) state=2;
			else
			{
				if(state==1) sreal.add(ts.split("\\s+"));
				else if(state==2) simag.add(ts.split("\\s+"));
			}
		}
		
		/*
		 * parse matrix
		 */
		if(sreal.size()!=simag.size()) throw new IllegalArgumentException(
				"wrong demixing matrix format: "+info);
		itr=sreal.iterator();
		itc=simag.iterator();
		matrix=new Complex[sreal.size()][sreal.get(0).length];
		for(int i=0;i<matrix.length;i++)
		{
			rowr=itr.next();
			rowc=itc.next();
			if(rowr.length!=rowc.length) throw new IllegalArgumentException(
					"wrong demixing matrix format: "+info);
			for(int j=0;j<matrix[i].length;j++)
				matrix[i][j]=new Complex(Double.parseDouble(rowr[j]),Double.parseDouble(rowc[j]));
		}
		return matrix;
	}
	
	/**
	 * load complex matrices from file
	 * @param path
	 * matrix file path
	 * @return
	 * complex matrices of each frequency bin
	 * @throws IOException
	 */
	public static Complex[][][] loadComplexMatrices(File path) throws IOException
	{
	BufferedReader in=null;
	List<Complex[][]> matrixl;//used to hold demixing matrix for each bin
	List<String> info;//information contains one demixing matrix
	Complex[][][] matrices;
	
		try
		{
			matrixl=new LinkedList<Complex[][]>();
			info=new LinkedList<String>();
			in=new BufferedReader(new InputStreamReader(new FileInputStream(path)));
			//read text file
			for(String ts=null;(ts=in.readLine())!=null;)
			{
				ts=ts.trim();
				if(ts.length()==0) continue;
				if(ts.startsWith("frequency bin")&&(!info.isEmpty()))
				{
					matrixl.add(parseComplexMatrix(info));
					info.clear();
				}
				else info.add(ts);//info of one demixing matrix is not finished
			}
			//the last one
			if(!info.isEmpty()) matrixl.add(parseComplexMatrix(info));
			
			/*
			 * copy into array
			 */
			matrices=new Complex[matrixl.size()][][];
			matrixl.toArray(matrices);
		}
		finally
		{
			try
			{
				if(in!=null) in.close();
			}
			catch(IOException e)
			{}
		}
		return matrices;
	}
	
	/**
	 * visualize filters
	 */
	public void visualize()
	{
	double[][][] tdf;
	double[][] f2;
	int idx=0;
	
		tdf=tdFilters();
		f2=new double[tdf.length*tdf[0].length][];
		for(int i=0;i<tdf.length;i++) 
			for(int j=0;j<tdf[i].length;j++) 
				f2[idx++]=tdf[i][j];
		
		Util.plotSignals(f2);
	}
	
	/**
	 * visualize frequency domain filter envelop
	 */
	public void fdVisualize()
	{
	Complex[][][] fdf;
	double[][] envelop;
	int idx=0;
	
		fdf=fdFilters();
		envelop=new double[fdf.length*fdf[0].length][];
		
		for(int i=0;i<fdf.length;i++) 
			for(int j=0;j<fdf[i].length;j++) 
				envelop[idx++]=BLAS.abs(fdf[i][j],null);
		
		Util.plotSignals(envelop);
	}
}
