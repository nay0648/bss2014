package cn.edu.bjtu.cit.bss.eval;
import java.io.*;
import java.util.*;
import javax.sound.sampled.*;
import org.apache.commons.math.complex.*;
import pp.util.BLAS;
import cn.edu.bjtu.cit.bss.*;
import cn.edu.bjtu.cit.bss.FDBSS.Operation;
import cn.edu.bjtu.cit.bss.FDBSS.Parameter;
import cn.edu.bjtu.cit.bss.align.*;
import cn.edu.bjtu.cit.bss.signalio.*;
import cn.edu.bjtu.cit.bss.util.*;
import cn.edu.bjtu.cit.bss.util.ShortTimeFourierTransformer.STFTIterator;

/**
 * <h1>Description</h1>
 * Used to evaluate BSS algorithms.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Sep 10, 2011 2:26:13 PM, revision:
 */
public class Evaluator implements Serializable
{
private static final long serialVersionUID=8839636462001722446L;
//store FIR filters for experiments
private static final File FILTER_BASE=new File("data/filters");
private List<File> sourcefl=new LinkedList<File>();//source file list
//used to open signal sources
private SignalSourceFactory sourcefactory;
private long samplelimit=0;//sample length limit, 0 for no limit
private ShortTimeFourierTransformer stft;//used to perform stft
private MixingModel model;//the mixing model

	/**
	 * <h1>Description</h1>
	 * used to open signal sources
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Sep 10, 2011 3:05:36 PM, revision:
	 */
	public interface SignalSourceFactory extends Serializable
	{
		/**
		 * open a signal source
		 * @param path
		 * file path
		 * @return
		 * @throws IOException
		 */
		public SignalSource open(File path) throws IOException;
	}
	
	/**
	 * <h1>Description</h1>
	 * Used to open wav.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Sep 10, 2011 3:08:24 PM, revision:
	 */
	public class WaveSourceFactory implements SignalSourceFactory
	{
	private static final long serialVersionUID=-8270925512655221374L;

		public SignalSource open(File path) throws IOException
		{
			try
			{
				return new LimitedSignalSource(new WaveSource(path,true),samplelimit);
			}
			catch(UnsupportedAudioFileException e)
			{
				throw new IOException("failed to open wave file: "+path,e);
			}
		}	
	}
	
	/**
	 * <h1>Description</h1>
	 * Used to open text signals.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Aug 8, 2012 4:19:52 PM, revision:
	 */
	public class TextSignalSourceFactory implements SignalSourceFactory
	{
	private static final long serialVersionUID=6457440655942723292L;

		public SignalSource open(File path) throws IOException
		{
			return new LimitedSignalSource(
					new TextSignalSource(new FileInputStream(path)),
					samplelimit);
		}
	}
	
	/**
	 * <h1>Description</h1>
	 * Source signal type.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Oct 12, 2012 9:56:30 AM, revision:
	 */
	public enum SourceType
	{
		/**
		 * Wave source (*.wav).
		 */
		wave,
		/**
		 * Text source, each column is a channel.
		 */
		text,
	}
	
	/**
	 * @param stft
	 * used to perform stft, should be the same transformer with the bss algorithm
	 * @param type
	 * source type
	 */
	public Evaluator(ShortTimeFourierTransformer stft,SourceType type)
	{
		this.stft=stft;
		
		switch(type)
		{
			case wave:
				sourcefactory=new WaveSourceFactory();
				break;
			case text:
				sourcefactory=new TextSignalSourceFactory();
				break;
			default:
				throw new IllegalArgumentException("unknown source type: "+type);
		}
	}
	
	/**
	 * @param stft
	 * used to perform stft, should be the same transformer with the bss algorithm
	 */
	public Evaluator(ShortTimeFourierTransformer stft)
	{
		this(stft,SourceType.wave);
	}
	
	/**
	 * apply allpass filter to a signal
	 * @param x
	 * input signal
	 * @param g
	 * the scaling parameter
	 * @param m
	 * delay taps
	 * @return
	 * @deprecated
	 * use RIRGenerator
	 */
	private static double[] applyAllpassFilter(double[] x,double g,int m)
	{
	double[] x2,y;
	
		x2=new double[x.length];
		System.arraycopy(x,0,x2,0,x.length);
		y=new double[x.length];
		
		for(int i=0;i<x.length;i++)
		{
			if(i-m>=0) y[i]=x2[i-m];//delay
			y[i]+=-g*x[i];//feed forward
			x2[i]+=g*y[i];//feed back
		}
		return y;
	}	
	
	/**
	 * generate a synthetic impluse response for experiments
	 * @param len
	 * filter length
	 * @param fs
	 * sample rate
	 * @param d
	 * distance from source to sensor, in meters
	 * @param c
	 * wave speed, in m/s
	 * @param r
	 * reverberation time, in seconds
	 * @return
	 * @deprecated
	 * use RIRGenerator
	 */
	public static double[] artificialImpulseResponse(int len,double fs,double d,double c,double r)
	{
	int delay,maxidx=0;
	double max=0;
	double amp,amp2;
	double[] h;
	double rever;
		
		delay=(int)(d*fs/c);//delay taps
		amp=1/(1+4*Math.PI*d*d);//amplitude of the first tap
		
		h=new double[len];
		h[delay]=1;//delayed impulse
		
		/*
		 * generate reverberation by cascated allpass filters
		 * 2 3 5 7 11 13 17 19 23 29 31 37 41 43 47 53 59 61 67 71 73 79 83 89 97
		 */
//		h=applyAllpassFilter(h,0.9,573);
//		h=applyAllpassFilter(h,0.8,151);
//		h=applyAllpassFilter(h,0.7,53);
		h=applyAllpassFilter(h,0.37*Math.random(),2);
		h=applyAllpassFilter(h,0.38*Math.random(),3);
		h=applyAllpassFilter(h,0.39*Math.random(),5);
		h=applyAllpassFilter(h,0.40*Math.random(),7);
		h=applyAllpassFilter(h,0.41*Math.random(),11);
//		h=applyAllpassFilter(h,0.42*Math.random(),13);
//		h=applyAllpassFilter(h,0.43*Math.random(),17);
//		h=applyAllpassFilter(h,0.44*Math.random(),19);
//		h=applyAllpassFilter(h,0.45*Math.random(),23);
//		h=applyAllpassFilter(h,0.46*Math.random(),29);
//		h=applyAllpassFilter(h,0.47*Math.random(),31);
//		h=applyAllpassFilter(h,0.48*Math.random(),37);
//		h=applyAllpassFilter(h,0.49*Math.random(),41);
//		h=applyAllpassFilter(h,0.50*Math.random(),47);

		/*
		 * normalize the max value to proper magnitude
		 */
		for(int i=0;i<h.length;i+=7) 
			if(Math.abs(h[i])>max) 
			{	
				max=Math.abs(h[i]);
				maxidx=i;
			}
		BLAS.scalarMultiply(amp/max,h,h);

		/*
		 * use 10^(-rever*t) to modulate reverberation time
		 */
		amp2=Math.sqrt(amp*amp/1e6);//amplitude decreases 60dB
		//amp*10^(rever*r*fs)=amp2
		rever=Math.log10(amp2/amp)/(r*fs);//the reverberation coefficient
		for(int i=maxidx;i<h.length;i++) h[i]*=Math.pow(10,rever*(i-maxidx));
		return h;
	}
	
	/**
	 * generate sequential filter indices used to load filters
	 * @param n
	 * number of sources
	 * @param m
	 * number of sensors
	 * @return
	 */
	public static int[] sequentialFilterIndices(int n,int m)
	{
	int[] idx;
	
		idx=new int[n*m];
		for(int i=0;i<idx.length;i++) idx[i]=i;
		return idx;
	}
	
	/**
	 * load filters from filter base
	 * @param len
	 * filter length
	 * @return
	 * each row is a FIR filter
	 * @throws IOException
	 */
	public static double[][] loadFilters(int len) throws IOException
	{
		return Util.loadSignals(new File(FILTER_BASE,"h"+len+".txt"),Util.Dimension.ROW);
	}
	
	/**
	 * select filters randomly from filter base
	 * @param len
	 * filter length
	 * @param num
	 * number of filters needed
	 * @return
	 * each row is a FIR filter
	 * @throws IOException
	 */
	public static double[][] loadRandomFilters(int len,int num) throws IOException
	{
	double[][] filters;
	double[][] rfilters;
	List<double[]> fl;
	
		filters=loadFilters(len);
		
		if(num>filters.length) throw new IndexOutOfBoundsException(
				"not enough filters in filter base: "+filters.length+", "+num);
		
		/*
		 * select randomly
		 */	
		rfilters=new double[num][];
		
		fl=new ArrayList<double[]>();
		for(double[] f:filters) fl.add(f);
		
		for(int i=0;i<num;i++)
			rfilters[i]=fl.remove((int)(Math.random()*fl.size()));
		
		return rfilters;
	}
	
	/**
	 * get sample limit
	 * @return
	 */
	public long getSampleLimit()
	{
		return samplelimit;
	}
	
	/**
	 * set sample length limit
	 * @param limit
	 * return eof if sample length longer than this limit
	 */
	public void setSampleLimit(long limit)
	{
		samplelimit=limit;
	}

	/**
	 * add source signals
	 * @param sources
	 * source signal paths
	 */
	public void addSources(File... sources)
	{
		for(File sf:sources) 
		{	
			if(sourcefl.contains(sf)) 
				throw new IllegalArgumentException("duplicate source: "+sf);
			sourcefl.add(sf);
		}
	}
	
	/**
	 * add source signals
	 * @param sources
	 * source signal paths
	 */
	public void addSources(String... sources)
	{
		for(String sf:sources) addSources(new File(sf));
	}
	
	/**
	 * clear all added sources
	 */
	public void clearSources()
	{
		sourcefl.clear();
	}
	
	/**
	 * get the number of sources
	 * @return
	 */
	public int numSources()
	{
		if(model==null) return 0;
		else return model.numSources();
	}
	
	/**
	 * get number of sensors
	 * @return
	 */
	public int numSensors()
	{
		if(model==null) return 0;
		else return model.numSensors();
	}
	
	/**
	 * get added source path
	 * @param sidx
	 * source index
	 * @return
	 */
	public File sourcePath(int sidx)
	{
		return sourcefl.get(sidx);
	}
	
	/**
	 * get the short time Fourier transformer
	 * @return
	 */
	public ShortTimeFourierTransformer stfTransformer()
	{
		return stft;
	}
	
	/**
	 * get fft block size
	 * @return
	 */
	public int fftSize()
	{
		return stft.fftSize();
	}
	
	/**
	 * open a source signal
	 * @param sourcei
	 * source signal index
	 * @return
	 * @throws IOException
	 */
	public SignalSource openSourceData(int sourcei) throws IOException
	{
		return sourcefactory.open(sourcefl.get(sourcei));
	}
	
	/**
	 * open all source signals
	 * @return
	 * @throws IOException
	 */
	public SignalMixer openSourceData() throws IOException
	{
	SignalSource[] ss;
		
		ss=new SignalSource[numSources()];
		for(int i=0;i<ss.length;i++) ss[i]=openSourceData(i);
		return new SignalMixer(ss);
	}
	
	/**
	 * open mixed sources as sensor data, the mixture is in time domain
	 * @return
	 * @throws IOException
	 */
	public SignalMixer openSensorData() throws IOException
	{
		return new SignalMixer(model.tdFilters(),false,openSourceData());
	}
	
	/**
	 * Open mixed signals as sensor data, however, the mixing procedure is performed 
	 * in frequency domain. Mainly used for experiments.
	 * @return
	 * @throws IOException
	 */
	public SignalMixer OpenFDMixedSensorData() throws IOException
	{
	Complex[][][] stfts;//source signal t-f data [source index][frequency bin index][frame index]
	Complex[][][] stftx;//sensor signal t-f data
	double[][] x;//sensor signals
	ArraySignalSource[] tdx;
	
		//transform source signals to frequency domain
		{
		SignalSource s;
		ArraySignalSink[] fds;//source signal in frequency domain
			
			stfts=new Complex[numSources()][][];
			fds=new ArraySignalSink[numSources()];
				
			for(int n=0;n<fds.length;n++) 
			{
				s=openSourceData(n);
				fds[n]=new ArraySignalSink(stft.fftSize());
				
				stft.stft(s,fds[n]);
				
				s.close();
				fds[n].flush();
				stfts[n]=fds[n].toArray(stfts[n]);
				fds[n].close();
			}
		}
			
		//mix in frequency domain
		{
		ArraySignalSource[] fds;
		ArraySignalSink[] fdx;
			
			fds=new ArraySignalSource[stfts.length];
			for(int n=0;n<fds.length;n++) fds[n]=new ArraySignalSource(stfts[n]);
				
			fdx=new ArraySignalSink[numSensors()];
			for(int m=0;m<fdx.length;m++) fdx[m]=new ArraySignalSink(stft.fftSize());
				
			model.applyFDFilters(fds,fdx);
				
			for(int n=0;n<fds.length;n++) fds[n].close();
			stftx=new Complex[fdx.length][][];
			for(int m=0;m<fdx.length;m++) 
			{
				fdx[m].flush();
				stftx[m]=fdx[m].toArray(stftx[m]);
				fdx[m].close();
			}
		}
			
		/*
		 * perform ifft
		 */
		x=new double[stftx.length][];
		for(int m=0;m<x.length;m++) x[m]=stft.istft(stftx[m]);
			
		/*
		 * return results
		 */
		tdx=new ArraySignalSource[x.length];
		for(int m=0;m<tdx.length;m++) tdx[m]=new ArraySignalSource(x[m]);
		
		return new SignalMixer(tdx);
	}
	
	/**
	 * output all sensor signals as wave file named as x0.wav, x1.wav...
	 * @param format
	 * audio format
	 * @param dest
	 * destination directory
	 * @throws IOException
	 */
	public void outputSensorDataAsWave(AudioFormat format,File dest) throws IOException
	{
	SignalSource mixed=null;
	double[][] buffer;
	SignalSink[] xsink=null;
	
		try
		{
			mixed=openSensorData();
			buffer=new double[mixed.numChannels()][1024];
			xsink=new WaveSink[mixed.numChannels()];
			for(int sensorj=0;sensorj<xsink.length;sensorj++) 
				xsink[sensorj]=new WaveSink(format,new File(dest,"x"+sensorj+".wav"));
			
			for(int count=0;(count=mixed.readSamples(buffer))>=0;) 
				for(int sensorj=0;sensorj<xsink.length;sensorj++) 
					xsink[sensorj].writeSamples(buffer[sensorj],0,count);
			
			for(int sensorj=0;sensorj<xsink.length;sensorj++) xsink[sensorj].flush();
		}
		finally
		{
			try
			{
				if(mixed!=null) mixed.close();
			}
			catch(IOException e)
			{}
			
			if(xsink!=null) 
				for(int sensorj=0;sensorj<xsink.length;sensorj++) 
					try
					{
						if(xsink[sensorj]!=null) xsink[sensorj].close();
					}
					catch(IOException e)
					{}
		}
	}
	
	/**
	 * load source signal
	 * @param sourcei
	 * source index
	 * @return
	 */
	public double[] sourceSignal(int sourcei)
	{
	SignalSource source;
	double[] data=null;
	
		try
		{
			source=openSourceData(sourcei);
			data=source.toArray(data);
		}
		catch(IOException e)
		{
			throw new RuntimeException("failed to load source "+sourcei,e);
		}
		
		return data;
	}
	
	/**
	 * get signal from source i to sensor j
	 * @param sourcei
	 * source index
	 * @param sensorj
	 * sensor index
	 * @return
	 */
	public double[] sensorSignalImage(int sourcei,int sensorj)
	{
	double[] signal,filter;
	
		signal=sourceSignal(sourcei);
		filter=model.tdFilters()[sensorj][sourcei];
		
		return Filter.convolve(signal,filter,null,Filter.Padding.zero);
	}
	
	/**
	 * set time domain mixing filters for generating mixed signals and evaluation
	 * @param numsensors
	 * number of sources
	 * @param numsources
	 * number of sensors
	 * @param path
	 * path for impulse response, each file contains one column for the impulse response, 
	 * the order is: h00, h01... h10, h11...
	 * @throws IOException 
	 */
	public void setMixingFilters(int numsensors,int numsources,File... path) throws IOException
	{
	int idx=0;
	double[][][] tdmixf;
	double[] h;
	
		if(path==null||path.length!=numsensors*numsources) throw new IllegalArgumentException(
				"number of filters not match, required: "+(numsensors*numsources));
		if(numsources!=sourcefl.size()) throw new IllegalArgumentException(
				"number of sources not match: "+numsources+", "+sourcefl.size());

		/*
		 * load time domain filters
		 */
		tdmixf=new double[numsensors][numsources][];
		for(int sensorj=0;sensorj<tdmixf.length;sensorj++) 
			for(int sourcei=0;sourcei<tdmixf[sensorj].length;sourcei++) 
			{
				h=Util.loadSignal(path[idx++],Util.Dimension.COLUMN);
				if(h.length>fftSize()) throw new IllegalArgumentException(
						"filter is too long: "+h.length+", "+fftSize());

				tdmixf[sensorj][sourcei]=h;
			}
		
		model=new MixingModel(tdmixf,fftSize());
	}
	
	/**
	 * choose filters from filter base as mixing filters
	 * @param numsensors
	 * number of sensors
	 * @param numsources
	 * number of sources
	 * @param len
	 * filter length
	 * @param filterindex
	 * selected filter indices
	 * @throws IOException
	 */
	public void setMixingFilters(int numsensors,int numsources,int len,int... filterindex) throws IOException
	{
	double[][][] tdmixf;
	double[][] filters;
	int idx=0;
	
		if(numsensors*numsources!=filterindex.length) throw new IllegalArgumentException(
				"number of filters not match: "+(numsensors*numsources)+", "+filterindex.length);
		if(numsources!=sourcefl.size()) throw new IllegalArgumentException(
				"number of sources not match: "+numsources+", "+sourcefl.size());
			
		tdmixf=new double[numsensors][numsources][];
		filters=loadFilters(len);

		for(int sensorj=0;sensorj<tdmixf.length;sensorj++) 
			for(int sourcei=0;sourcei<tdmixf[sensorj].length;sourcei++) 
				tdmixf[sensorj][sourcei]=filters[filterindex[idx++]];
		
		model=new MixingModel(tdmixf,fftSize());
	}
	
	/**
	 * set mixing filters from virtual room
	 * @param room
	 * a virtual room
	 */
	public void setMixingFilters(VirtualRoom room)
	{
		if(room.numSources()!=sourcefl.size()) throw new IllegalArgumentException(
				"number of sources not match: "+room.numSources()+", "+sourcefl.size());
		
		model=new MixingModel(room.tdFilters(),fftSize());
	}
	
	/**
	 * set mixing filters directly
	 * @param tdmixf
	 * [sensor index][source index][tap index]
	 */
	public void setMixingFilters(double[][][] tdmixf)
	{
		if(tdmixf[0].length!=sourcefl.size()) throw new IllegalArgumentException(
				"number of sources not match: "+tdmixf[0].length+", "+sourcefl.size());
		
		model=new MixingModel(tdmixf,fftSize());
	}
	
	/**
	 * set instantaneous mixing matrix, for experiments
	 * @param mixm
	 * [sensor index][source index]
	 */
	public void setMixingFilters(double[][] mixm)
	{
	double[][][] tdmixf;
	
		tdmixf=new double[mixm.length][mixm[0].length][1];
		for(int sensorj=0;sensorj<tdmixf.length;sensorj++) 
			for(int sourcei=0;sourcei<tdmixf[sensorj].length;sourcei++) 
				tdmixf[sensorj][sourcei][0]=mixm[sensorj][sourcei];
		
		setMixingFilters(tdmixf);
	}
	
	/**
	 * get the mixing model used to mix source signals
	 * @return
	 */
	public MixingModel mixingModel()
	{
		return model;
	}
	
	/**
	 * Get the eval model to see U=WA, i.e. interaction between 
	 * the mixing model and corresponding demixing model.
	 * @param demixm
	 * corresponding demixing model
	 * @return
	 */
	public EvalModel evalModel(DemixingModel demixm)
	{
		return new EvalModel(mixingModel(),demixm);
	}
	
	/**
	 * Performance index (PI) for each frequency bin. The smaller pi value, 
	 * the better ICA performance in a frequency bin. See: Yan Li, David Powers 
	 * and James Peach, Comparison of Blind Source Separation Algorithms, for 
	 * details.
	 * @param demixm
	 * demixing model calculated by bss algorithm
	 * @return
	 */
	public double[] pi(DemixingModel demixm)
	{
		return evalModel(demixm).pi();
	}

	/**
	 * calculate energy flow from each source to each sensor for input data
	 * @return
	 * [source index][sensor index][frequency bin index]
	 * @throws IOException 
	 */
	private double[][][] inputEnergyFlow() throws IOException
	{
	double[][][] energy;
	Complex[][][] fdmixf;
	SignalSource[] sources;//for source signals
	ShortTimeFourierTransformer stft;
	STFTIterator[] stftit;
	Complex[][] framein;
	Complex[] tempmul=null;
	double tempe;
	
		/*
		 * open signal streams
		 */
		//signal source for source data
		sources=new SignalSource[numSources()];
		for(int sourcei=0;sourcei<sources.length;sourcei++) 
			sources[sourcei]=openSourceData(sourcei);

		stft=stfTransformer();
		stftit=new STFTIterator[sources.length];
		for(int sourcei=0;sourcei<stftit.length;sourcei++) 
			stftit[sourcei]=stft.stftIterator(sources[sourcei]);

		/*
		 * accumulate energy
		 */
		energy=new double[numSources()][numSensors()][fftSize()/2+1];
		framein=new Complex[sources.length][];
		fdmixf=model.fdFilters();
		
eof:	for(;;)
		{
			//get frame from source stft
			for(int sourcei=0;sourcei<stftit.length;sourcei++) 
			{
				if(!stftit[sourcei].hasNext()) break eof;
				framein[sourcei]=stftit[sourcei].next();
			}
				
			for(int sensorj=0;sensorj<fdmixf.length;sensorj++) 
				for(int sourcei=0;sourcei<fdmixf[sensorj].length;sourcei++) 
				{
					//frequency domain dot product
					tempmul=BLAS.entryMultiply(fdmixf[sensorj][sourcei],framein[sourcei],tempmul);
						
					//accumulate energy
					for(int binidx=0;binidx<energy[sourcei][sensorj].length;binidx++) 
					{
						tempe=BLAS.absSquare(tempmul[binidx]);
						energy[sourcei][sensorj][binidx]+=tempe;
						
						//its complex conjugate counter part
						if(binidx>0&&binidx<energy[sourcei][sensorj].length-1) 
							energy[sourcei][sensorj][binidx]+=tempe;
					}
				}
		}
				
		//close files
		for(int sourcei=0;sourcei<sources.length;sourcei++) sources[sourcei].close();

		return energy;
	}

	/**
	 * throw exceptions if frequency subbands size out of bounds
	 * @param offset
	 * frequency subbands offset
	 * @param len
	 * subbands length
	 */
	private void checkSubbandSize(int offset,int len)
	{
		if(offset<0||offset>=fftSize()/2+1) throw new IndexOutOfBoundsException(
				"subbands offset out of bounds: "+offset+", "+(fftSize()/2+1));
		
		if(len<1||offset+len>fftSize()/2+1) throw new IndexOutOfBoundsException(
				"subbands length out of bounds: "+offset+", "+len+", "+(fftSize()/2+1));
	}
	
	/**
	 * calculate signal-to-interference ratio by energy flow matrix
	 * @param energyflow
	 * row means energy source, column means energy destination
	 * @return
	 * sir for each source location
	 */
	private double[] sir(double[][] energyflow)
	{
	double[] sir;
	double sum,maxsir;
	
		//calculate energy ratio from energy flow matrix
		for(int j=0;j<energyflow[0].length;j++) 
		{
			/*
			 * energy sum at a destination
			 */
			sum=0;
			for(int i=0;i<energyflow.length;i++) 
				sum+=energyflow[i][j];
		
			/*
			 * energy ratio corresponding to current source
			 */
			for(int i=0;i<energyflow.length;i++) 
				energyflow[i][j]/=(sum-energyflow[i][j]);
		}
	
		/*
		 * calculate sir
		 */
		sir=new double[energyflow.length];
		for(int i=0;i<sir.length;i++) 
		{
			maxsir=Double.NEGATIVE_INFINITY;
			for(int j=0;j<energyflow[0].length;j++) 
				if(energyflow[i][j]>maxsir) maxsir=energyflow[i][j];
			
			//convert to dB
			sir[i]=10*Math.log10(maxsir);
		}
		
		return sir;
	}
	
	/**
	 * calculate signal to interference ratio for input data in a subband
	 * @param offset
	 * subband offset
	 * @param len
	 * subband length
	 * @return
	 * @throws IOException
	 */
	public double[] inputSIR(int offset,int len) throws IOException
	{
	double[][][] eflow;
	double[][] etotal;
		
		checkSubbandSize(offset,len);
		
		//energy flow from each source to each sensor
		eflow=inputEnergyFlow();
		
		/*
		 * calculate total energy
		 */
		etotal=new double[eflow.length][eflow[0].length];
			
		for(int sourcei=0;sourcei<etotal.length;sourcei++) 
			for(int sensorj=0;sensorj<etotal[sourcei].length;sensorj++) 
				for(int binidx=offset;binidx<offset+len;binidx++) 
					etotal[sourcei][sensorj]+=eflow[sourcei][sensorj][binidx];
			
		return sir(etotal);
	}
	
	/**
	 * calculate signal to interference ratio for input data
	 * @return
	 * @throws IOException
	 */
	public double[] inputSIR() throws IOException
	{
		return inputSIR(0,fftSize()/2+1);
	}

	/**
	 * calculate energy flow for output data, from each source to each estimated source
	 * @param demixm
	 * demixing model calculated by bss algorithm
	 * @return
	 * [source index][estimated source index][frequency bin index]
	 * @throws IOException
	 */
	private double[][][] outputEnergyFlow(DemixingModel demixm) throws IOException
	{
	Complex[][][] u;//the final filter
	double[][][] energy;
	SignalSource[] sources;//for source signals
	ShortTimeFourierTransformer stft;
	STFTIterator[] stftit;
	Complex[][] framein;
	Complex[] tempmul=null;
	double tempe;
		
		//filters from each source to each estimated source
		u=evalModel(demixm).fdFilters();
		
		/*
		 * open signal streams
		 */
		//signal source for source data
		sources=new SignalSource[numSources()];
		for(int sourcei=0;sourcei<sources.length;sourcei++) 
			sources[sourcei]=openSourceData(sourcei);

		stft=stfTransformer();
		stftit=new STFTIterator[sources.length];
		for(int sourcei=0;sourcei<stftit.length;sourcei++) 
			stftit[sourcei]=stft.stftIterator(sources[sourcei]);

		/*
		 * accumulate energy
		 */
		energy=new double[numSources()][numSources()][fftSize()/2+1];
		framein=new Complex[sources.length][];
		
eof:	for(;;)
		{
			//get frame from source stft
			for(int sourcei=0;sourcei<stftit.length;sourcei++) 
			{
				if(!stftit[sourcei].hasNext()) break eof;
				framein[sourcei]=stftit[sourcei].next();
			}
	
			for(int estidx=0;estidx<u.length;estidx++) 
				for(int sidx=0;sidx<u[estidx].length;sidx++) 
				{
					//frequency domain dot product
					tempmul=BLAS.entryMultiply(u[estidx][sidx],framein[sidx],tempmul);
					
					//accumulate energy
					for(int binidx=0;binidx<energy[sidx][estidx].length;binidx++) 
					{
						tempe=BLAS.absSquare(tempmul[binidx]);
						//energy flow from source to estimated source
						energy[sidx][estidx][binidx]+=tempe;
						
						//its complex conjugate counter part
						if(binidx>0&&binidx<energy[sidx][estidx].length-1) 
							energy[sidx][estidx][binidx]+=tempe;
					}
				}
		}

		//close files
		for(int sourcei=0;sourcei<sources.length;sourcei++) sources[sourcei].close();

		return energy;
	}

	/**
	 * calculate signal-to-interference ratio for output data on a subband
	 * @param demixm
	 * demixing model outputted by BSS algorithm
	 * @param offset
	 * subband offset
	 * @param len
	 * subband length
	 * @return
	 * @throws IOException
	 */
	public double[] outputSIR(DemixingModel demixm,int offset,int len) throws IOException
	{
	double[][][] eflow;
	double[][] etotal;
			
		checkSubbandSize(offset,len);
		
		//energy flow from each source to each estimated source
		eflow=outputEnergyFlow(demixm);
				
		/*
		 * calculate total energy
		 */
		etotal=new double[eflow.length][eflow[0].length];
		
		for(int sidx=0;sidx<etotal.length;sidx++) 
			for(int estidx=0;estidx<etotal[sidx].length;estidx++) 
				for(int binidx=offset;binidx<offset+len;binidx++) 
					etotal[sidx][estidx]+=eflow[sidx][estidx][binidx];
		
		return sir(etotal);
	}
	
	/**
	 * calculate signal-to-interference ratio for output data
	 * @param demixm
	 * demixing model outputted by BSS algorithm
	 * @return
	 * @throws IOException
	 */
	public double[] outputSIR(DemixingModel demixm) throws IOException
	{
		return outputSIR(demixm,0,fftSize()/2+1);
	}

	/**
	 * calculate average SIR improvement (output_sir-input_sir) on a subband
	 * @param demixm
	 * demixing model provided by BSS algorithm
	 * @param offset
	 * subband offset
	 * @param len
	 * subband length
	 * @return
	 * @throws IOException
	 */
	public double sirImprovement(DemixingModel demixm,int offset,int len) throws IOException
	{
	double[] siri,siro;
	double sir=0;
	
		/*
		 * calculate input and output sir
		 */
		siri=inputSIR(offset,len);
		siro=outputSIR(demixm,offset,len);
		
		/*
		 * direct average
		 */
		for(int i=0;i<siri.length;i++) sir+=siro[i]-siri[i];
		sir/=siri.length;
		
		return sir;
	}
	
	/**
	 * calculate average SIR improvement (output_sir-input_sir)
	 * @param demixm
	 * demixing model provided by BSS algorithm
	 * @return
	 * @throws IOException
	 */
	public double sirImprovement(DemixingModel demixm) throws IOException
	{
		return sirImprovement(demixm,0,fftSize()/2+1);
	}
		
	/**
	 * perform evaluation for bss algorithms
	 * @param ss
	 * input stream for source stft data
	 * @param sy
	 * input stream for estimated source stft data
	 * @param u
	 * frequency domain filters for the total system: U=WA
	 * @return
	 * [source index][SDR, SIR, SAR, perm]
	 * @throws IOException
	 */
	private double[][] bssEval(SignalSource[] ss,SignalSource[] sy,Complex[][][] u) throws IOException
	{
	double[][] eflow;//energy flow
	double[] eartif0;//energy for artifact (beform permutation)

		/*
		 * check parameters
		 */
		if(ss.length!=sy.length) throw new IllegalArgumentException(
				"number of sources and estimated sources not match: "+ss.length+", "+sy.length);
		if(sy.length!=u.length) throw new IllegalArgumentException(
				"number of estimated sources and filters not match: "+sy.length+", "+u.length);
		if(ss.length!=u[0].length) throw new IllegalArgumentException(
				"number of sources and filters not match: "+ss.length+", "+u[0].length);
		if(ss[0].numChannels()!=u[0][0].length) throw new IllegalArgumentException(
				"fft size not match for source data: "+ss[0].numChannels()+", "+u[0][0].length);
		if(sy[0].numChannels()!=u[0][0].length) throw new IllegalArgumentException(
				"fft size not match for estimated source data: "+sy[0].numChannels()+", "+u[0][0].length);
	
		//accumulate energy
		{
		EnergyAccumulator[][] eflowacc;
		EnergyAccumulator[] eartif0acc;
		Complex[][] frames,framey;//frames for source data and estimated source data
		Complex[] tempmul=null,tempadd;
			
			/*
			 * allocate space
			 */
			eflowacc=new EnergyAccumulator[ss.length][ss.length];
			for(int i=0;i<eflowacc.length;i++) 
				for(int j=0;j<eflowacc[i].length;j++) 
					eflowacc[i][j]=new EnergyAccumulator(stft);
		
			eartif0acc=new EnergyAccumulator[ss.length];
			for(int i=0;i<eartif0acc.length;i++) 
				eartif0acc[i]=new EnergyAccumulator(stft);
					
			frames=new Complex[ss.length][];
			for(int sidx=0;sidx<frames.length;sidx++) 
				frames[sidx]=new Complex[ss[0].numChannels()];
		
			framey=new Complex[sy.length][];
			for(int estidx=0;estidx<framey.length;estidx++) 
				framey[estidx]=new Complex[sy[0].numChannels()];
		
			tempadd=new Complex[ss[0].numChannels()];
			
			//accumulate energy
eof:		for(;;)
			{
				//read frames from source stft and estimated source stft
				for(int sourcei=0;sourcei<ss.length;sourcei++) 
				{
					//get frame from source stft
					try
					{
						ss[sourcei].readFrame(frames[sourcei]);
					}
					catch(EOFException e)
					{
						break eof;
					}
				
					//get frame from estimated source
					try
					{
						sy[sourcei].readFrame(framey[sourcei]);
					}
					catch(EOFException e)
					{
						break eof;
					}
				}
			
				//accumulate energy
				for(int estidx=0;estidx<u.length;estidx++) 
				{
					//data flow from all sources to an estimated source
					Arrays.fill(tempadd,Complex.ZERO);
				
					for(int sidx=0;sidx<u[estidx].length;sidx++) 
					{
						//frequency domain dot product
						tempmul=BLAS.entryMultiply(u[estidx][sidx],frames[sidx],tempmul);
						tempadd=BLAS.add(tempmul,tempadd,tempadd);
					
						//accumulate energy from a source to an estimated source
						eflowacc[sidx][estidx].accumulateEnergy(tempmul);
					}
					
					/*
					 * accumulate energy for artifact
					 */
					tempadd=BLAS.substract(framey[estidx],tempadd,tempadd);
					eartif0acc[estidx].accumulateEnergy(tempadd);
				}
			}
			
			/*
			 * get energy
			 */
			eflow=new double[eflowacc.length][eflowacc[0].length];
			for(int i=0;i<eflow.length;i++) 
				for(int j=0;j<eflow[i].length;j++) 
					eflow[i][j]=eflowacc[i][j].energy();
			
			eartif0=new double[eartif0acc.length];
			for(int i=0;i<eartif0.length;i++) eartif0[i]=eartif0acc[i].energy();
		}
		
		//perform evaluation
		{
		double[] etotal;//total energy for each output
		double[][] sirflow;//signal-to-interference flow
		int[] perm=null;//the s-y correspondence
		double[] starget,einterf,eartif;//energy decomposition for each source
		double[][] eval;
		
			/*
			 * calculate total energy for each estimated source
			 */
			etotal=new double[eflow[0].length];
			for(int i=0;i<eflow.length;i++) 
				for(int j=0;j<eflow[i].length;j++) 
					etotal[j]+=eflow[i][j];
			
			/*
			 * calculate energy flow for sir
			 */
			sirflow=new double[eflow.length][eflow[0].length];
			for(int i=0;i<sirflow.length;i++) 
				for(int j=0;j<sirflow[i].length;j++) 
					sirflow[i][j]=eflow[i][j]/(etotal[j]-eflow[i][j]);
			
			//find the best s-y correspondence
			{
			double sirtotal,maxsirtotal=Double.NEGATIVE_INFINITY;
			
				//try all permutations
				for(int[] p:AlignPolicy.indexPermutation(sirflow.length)) 
				{
					/*
					 * accumulate total sir
					 */
					sirtotal=0;
					for(int i=0;i<sirflow.length;i++) sirtotal+=sirflow[i][p[i]];
					
					if(sirtotal>maxsirtotal) 
					{
						maxsirtotal=sirtotal;
						perm=p;
					}
				}
			}
			
			/*
			 * perform energy decomposition
			 */
			starget=new double[eflow.length];
			einterf=new double[eflow.length];
			eartif=new double[eflow.length];
			
			for(int i=0;i<eflow.length;i++) 
			{
				starget[i]=eflow[i][perm[i]];
				einterf[i]=etotal[perm[i]]-starget[i];
				eartif[i]=eartif0[perm[i]];
			}
			
			
//			System.out.println("starget: "+Arrays.toString(starget));
//			System.out.println("einterf: "+Arrays.toString(einterf));
//			System.out.println("eartif: "+Arrays.toString(eartif));
			
			
			/*
			 * calculate SDR, SIR, SAR
			 */
			eval=new double[eflow.length][4];
			for(int i=0;i<eval.length;i++) 
			{
				eval[i][0]=10*Math.log10(starget[i]/(einterf[i]+eartif[i]));//SDR
				eval[i][1]=10*Math.log10(starget[i]/einterf[i]);//SIR
				eval[i][2]=10*Math.log10((starget[i]+einterf[i])/eartif[i]);//SAR
				eval[i][3]=perm[i];//destination of the source signal
			}
			
			return eval;	
		}
	}
	
	/**
	 * perform evaluation for bss algorithms according to definition
	 * @param fdbss
	 * the bss algorithm
	 * @return
	 * [source signal index][SDR, SIR, SAR, perm], perm is the destination index of the 
	 * source signal goes to the output signal, for the definition of SDR (signal-to-distortion ratio), 
	 * SIR (signal-to-interference ratio), and SAR (signal-to-artifact ratio), please see: 
	 * E. Vincent, R. Gribonval, C. Fevotte, "Performance Measurement in Blind Audio 
	 * Source Separation," IEEE Transactions on Audio, Speech, and Language Processing, 
	 * vol. 14, no. 4, pp. 1462-1469, 2006
	 * @throws IOException 
	 */
	public double[][] bssEval(FDBSS fdbss) throws IOException
	{
	SignalSource[] ss;//for source signals
	SignalSource[] sy;//for estimated source signals
	double[][] eval;
	
		/*
		 * check parameters
		 */
		if(numSources()!=fdbss.numSources()) throw new IllegalArgumentException(
				"number of sources not match: "+numSources()+", "+fdbss.numSources());
		if(numSensors()!=fdbss.numSensors()) throw new IllegalArgumentException(
				"number of sensors not match: "+numSensors()+", "+fdbss.numSensors());
		if(fftSize()!=fdbss.fftSize()) throw new IllegalArgumentException(
				"fft size not match: "+fftSize()+", "+fdbss.fftSize());
		
		/*
		 * open source signals
		 */
		ss=new SignalSource[numSources()];
		for(int sidx=0;sidx<ss.length;sidx++) 
			ss[sidx]=new STFTSource(stft,openSourceData(sidx));

		/*
		 * open estimated source data
		 */
		sy=new SignalSource[numSources()];
		for(int estidx=0;estidx<sy.length;estidx++) 
			sy[estidx]=fdbss.openEstimatedSTFTSource(estidx);
		
		//perform evaluation
		eval=bssEval(ss,sy,evalModel(fdbss.loadDemixingModel()).fdFilters());
		
		/*
		 * close files
		 */
		for(int sidx=0;sidx<ss.length;sidx++) ss[sidx].close();
		for(int estidx=0;estidx<sy.length;estidx++) sy[estidx].close();
		
		return eval;
	}

	/**
	 * Estimate frequency response. Please see: J. Schoukens, G. Vandersteen, K. Barbe, 
	 * R. Pintelon, Nonparametric preprocessing in system identification: a powerful tool, 
	 * European Journal of Control, vol. 3-4, pp. 260-274, 2009.
	 * @param sdata
	 * source data in t-f domain
	 * @param ydata
	 * separated source data in t-f domain
	 * @return
	 * @throws FileNotFoundException 
	 */
	private Complex[] frequencyResponse(Complex[][] sdata,Complex[][] ydata) throws IOException
	{
	Complex[] fh,nufh;
	double[] defh;

		if(sdata.length!=ydata.length) throw new IllegalArgumentException(
				"fft size not match: "+sdata.length+", "+ydata.length);
	
		nufh=new Complex[sdata.length/2+1];
		Arrays.fill(nufh,Complex.ZERO);
		defh=new double[sdata.length/2+1];

		for(int tau=0;tau<Math.min(sdata[0].length,ydata[0].length);tau++) 		
			//accumulate frequency response
			for(int f=0;f<nufh.length;f++)
			{
				nufh[f]=nufh[f].add(ydata[f][tau].multiply(sdata[f][tau].conjugate()));
				defh[f]+=BLAS.absSquare(sdata[f][tau]);
			}

		/*
		 * calculate frequency response
		 */
		fh=new Complex[sdata.length];
		for(int f=0;f<fh.length/2+1;f++) 
		{
			if(defh[f]>=1) fh[f]=nufh[f].multiply(1.0/defh[f]);
			else fh[f]=Complex.ZERO;
			
			//its complex conjugate counterpart
			if(f>0&&f<fh.length/2) fh[fh.length-f]=fh[f].conjugate();
		}
		
		fh=spectralSmoothing(fh);
		
		return fh;
	}
	
	/**
	 * perform spectral smoothing, see: Hiroshi Sawada et al. SPECTRAL SMOOTHING 
	 * FOR FREQUENCY-DOMAIN BLIND SOURCE SEPARATION, International Workshop on Acoustic 
	 * Echo and Noise Control, pp. 311-314, 2003
	 * @param fdf
	 * a frequency domain filter
	 * @return
	 * the smoothed filter
	 */
	private Complex[] spectralSmoothing(Complex[] fdf)
	{
	Complex w0,w1,w2;
	Complex[] fdf2;
	
		fdf2=new Complex[fdf.length];
		
		for(int binidx=0;binidx<fdf.length/2+1;binidx++) 
		{
			w1=fdf[binidx];
			if(binidx-1>=0) w0=fdf[binidx-1];else w0=fdf[binidx];
			if(binidx+1<fdf.length/2+1) w2=fdf[binidx+1];else w2=fdf[binidx];
			
			fdf2[binidx]=w0.add(w1.multiply(2)).add(w2).multiply(1.0/4.0);
			
			//its complex conjugate counterpart
			if(binidx>0&&binidx<fdf.length/2) 
				fdf2[fdf2.length-binidx]=fdf2[binidx].conjugate();
		}
		
		return fdf2;
	}
	
	/**
	 * perform evaluation for bss algorithms by system identification
	 * @param ss
	 * input stream for time domain source signals
	 * @param sy
	 * input stream for time domain estimated source signals
	 * @return
	 * [source signal index][SDR, SIR, SAR, perm], perm is the destination index of the 
	 * source signal goes to the output signal, for the definition of SDR (signal-to-distortion ratio), 
	 * SIR (signal-to-interference ratio), and SAR (signal-to-artifact ratio), please see: 
	 * E. Vincent, R. Gribonval, C. Fevotte, "Performance Measurement in Blind Audio 
	 * Source Separation," IEEE Transactions on Audio, Speech, and Language Processing, 
	 * vol. 14, no. 4, pp. 1462-1469, 2006
	 * @throws IOException
	 */
	public double[][] bssEval(SignalSource[] ss,SignalSource[] sy) throws IOException
	{
	Complex[][][] sdata;//source t-f data, [source index][][]
	Complex[][][] ydata;//estimated source t-f data, [source index][][]
	Complex[][][] u;//frequency domain filters for the total system
	SignalSource[] fdss,fdsy;
	double[][] eval;
	
		if(ss.length!=sy.length) throw new IllegalArgumentException(
				"number of sources not match: "+ss.length+", "+sy.length);
		
		/*
		 * load data
		 */
		sdata=new Complex[ss.length][][];
		for(int sidx=0;sidx<sdata.length;sidx++) sdata[sidx]=stft.stft(ss[sidx]);
			
		ydata=new Complex[sy.length][][];
		for(int estidx=0;estidx<ydata.length;estidx++) ydata[estidx]=stft.stft(sy[estidx]);
		
		/*
		 * estimate frequency responses from each s to each y
		 */
		u=new Complex[ydata.length][sdata.length][];
		for(int estidx=0;estidx<u.length;estidx++) 
			for(int sidx=0;sidx<u[estidx].length;sidx++) 
				u[estidx][sidx]=frequencyResponse(sdata[sidx],ydata[estidx]);

		/*
		 * perform evaluation
		 */
		fdss=new SignalSource[sdata.length];
		for(int sidx=0;sidx<fdss.length;sidx++) fdss[sidx]=new ArraySignalSource(sdata[sidx]);
		
		fdsy=new SignalSource[ydata.length];
		for(int estidx=0;estidx<fdsy.length;estidx++) fdsy[estidx]=new ArraySignalSource(ydata[estidx]);
	
		eval=bssEval(fdss,fdsy,u);
		
		for(int sidx=0;sidx<fdss.length;sidx++) fdss[sidx].close();
		for(int estidx=0;estidx<fdsy.length;estidx++) fdsy[estidx].close();
		
		return eval;
	}
	
	/**
	 * perform evaluation for bss algorithms by system identification
	 * @param wavfiles
	 * wave files for source signals and estimated source signals
	 * @return
	 * [source signal index][SDR, SIR, SAR, perm], perm is the destination index of the 
	 * source signal goes to the output signal, for the definition of SDR (signal-to-distortion ratio), 
	 * SIR (signal-to-interference ratio), and SAR (signal-to-artifact ratio), please see: 
	 * E. Vincent, R. Gribonval, C. Fevotte, "Performance Measurement in Blind Audio 
	 * Source Separation," IEEE Transactions on Audio, Speech, and Language Processing, 
	 * vol. 14, no. 4, pp. 1462-1469, 2006
	 * @throws IOException
	 * @throws UnsupportedAudioFileException 
	 */
	public double[][] bssEval(File... wavfiles) throws IOException, UnsupportedAudioFileException
	{
	SignalSource[] ss,sy;
	double[][] eval;	
	
		if(wavfiles.length%2!=0) throw new IllegalArgumentException(
				"even number of wave files required: "+wavfiles.length);
		
		ss=new SignalSource[wavfiles.length/2];
		for(int sidx=0;sidx<ss.length;sidx++) 
			ss[sidx]=new WaveSource(wavfiles[sidx],true);
		
		sy=new SignalSource[wavfiles.length/2];
		for(int estidx=0;estidx<sy.length;estidx++) 
			sy[estidx]=new WaveSource(wavfiles[ss.length+estidx],true);
		
		eval=bssEval(ss,sy);
		
		for(int sidx=0;sidx<ss.length;sidx++) ss[sidx].close();
		for(int estidx=0;estidx<sy.length;estidx++) sy[estidx].close();
		
		return eval;
	}
	
	public static void main(String[] args) throws IOException, UnsupportedAudioFileException
	{
	FDBSS fdbss;
	Evaluator evaluator;
	SignalMixer sources;
		
		/*
		 * init bss and set parameters
		 */
		fdbss=new FDBSS(new File("temp"));
		fdbss.setParameter(Parameter.stft_size,"256");
		fdbss.setParameter(Parameter.stft_overlap,Integer.toString((int)(256*3/4)));
		fdbss.setParameter(Parameter.fft_size,"512");
		
//		fdbss.setParameter(Parameter.ica_algorithm,"cn.edu.bjtu.cit.bss.iva.SubbandSubspaceFIVA");
//		fdbss.setParameter(Parameter.align_policy,"cn.edu.bjtu.cit.bss.align.IdentityAlign");
		
		/*
		 * add sources
		 */
		evaluator=new Evaluator(fdbss.stfTransformer());
		evaluator.addSources("data/SawadaDataset/s1.wav");
		evaluator.addSources("data/SawadaDataset/s2.wav");
//		evaluator.addSources("data/SawadaDataset/s3.wav");
//		evaluator.addSources("data/SawadaDataset/s4.wav");
		
		//set mixing filters
		evaluator.setMixingFilters(
				2,2,128,
				4,5,6,7);
//		evaluator.setMixingFilters(
//				3,3,1,
//				0,1,2,3,4,5,6,7,8);
//		evaluator.setMixingFilters(
//				4,4,1,
//				0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15);
		
//		evaluator.setMixingFilters(
//				new VirtualRoom(new File("data/VirtualRooms/2x2/SawadaRoom2x2.txt")));
		
		/*
		 * mix in frequency domain for experiments
		 */
//		sources=evaluator.OpenFDMixedSensorData();
//		sources=evaluator.openSourceData();
//		fdbss.fdMix(sources,evaluator.mixingModel());
		
		sources=evaluator.openSensorData();
		fdbss.separate(sources,Operation.stft);
		fdbss.separate(sources,Operation.ica);
		fdbss.separate(sources,Operation.align);
		fdbss.separate(sources,Operation.demix);
		sources.close();

		System.out.println("input SIR: "+Arrays.toString(evaluator.inputSIR()));
		System.out.println("output SIR: "+Arrays.toString(evaluator.outputSIR(fdbss.loadDemixingModel())));
		System.out.println("SIR improvement: "+evaluator.sirImprovement(fdbss.loadDemixingModel()));
		
		System.out.println();
		System.out.println("by bss_eval:");
		BLAS.println(evaluator.bssEval(fdbss));
		
		evaluator.outputSensorDataAsWave(new AudioFormat(8000,16,1,true,false),new File("/home/nay0648"));
		fdbss.outputEstimatedSignals(new File("/home/nay0648"),new AudioFormat(8000,16,1,true,false));
		
//		fdbss.visualizeSensorSpectrograms(fdbss.numSensors(),1);
		fdbss.visualizeSourceSpectrograms(fdbss.numSources(),1);
	}
}
