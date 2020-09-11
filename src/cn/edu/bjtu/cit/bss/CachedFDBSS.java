package cn.edu.bjtu.cit.bss;
import java.io.*;
import java.awt.image.*;
import java.util.*;
import java.util.logging.*;
import javax.sound.sampled.*;
import org.apache.commons.math.complex.*;
import pp.util.BLAS;
import cn.edu.bjtu.cit.bss.signalio.*;
import cn.edu.bjtu.cit.bss.align.*;
import cn.edu.bjtu.cit.bss.util.*;

/**
 * <h1>Description</h1>
 * All data are cached in memory.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Mar 21, 2012 5:01:19 PM, revision:
 */
public class CachedFDBSS extends FDBSSAlgorithm
{
private static final long serialVersionUID=3951355180845358445L;
private static final String LOGGER_NAME="cn.edu.bjtu.cit.bss";
private Complex[][][] stftx;//stft data for observed signals, [channel index][frequency bin index][stft frame index]
private Complex[][][] stfty;//estimated source stfts
private int nsources=0;//designated number of sources
/*
 * for instantaneous ica
 */
private String preprocessorname="cn.edu.bjtu.cit.bss.preprocess.Whitening";//preprocessor name for ICA
private String icaname="cn.edu.bjtu.cit.bss.ica.CFastICA";//ICA algorithm name
private ICAStep icastep;//used to perform ICA on each frequency bin
/*
 * for permutation and scaling ambiguities
 */
private AlignPolicy apolicy;
private ScalingPolicy spolicy;

	public CachedFDBSS()
	{
		icastep=new CommonICAStep();
		icastep.setFDBSSAlgorithm(this);
		icastep.setPreprocessor(preprocessorname);
		((CommonICAStep)icastep).setICA(icaname);
		
		apolicy=new RegionGrow();
		apolicy.setFDBSSAlgorithm(this);
		
		spolicy=new MaxContributionRescale();
	}

	public int numSources()
	{
		return getNumSources();
	}
	
	/**
	 * get the number of sources
	 * @return
	 */
	public int getNumSources()
	{
		if(nsources==0) return numSensors();
		else return nsources;
	}
	
	/**
	 * set the number of sources
	 * @param nsources
	 * number of sources
	 */
	public void setNumSources(int nsources)
	{
		this.nsources=nsources;
	}

	public int numSensors()
	{
		if(stftx==null) return 0;
		else return stftx.length;
	}
	
	/**
	 * perform stft for input signals
	 * @param x
	 * multichannel sensor signals
	 * @throws IOException 
	 */
	public void stft(SignalSource x) throws IOException
	{
	ArraySignalSink[] sinks;
	
		/*
		 * signal sink for stft
		 */
		sinks=new ArraySignalSink[x.numChannels()];
		for(int chidx=0;chidx<sinks.length;chidx++) 
			sinks[chidx]=new ArraySignalSink(this.fftSize());
		
		this.stfTransformer().stft(x,sinks);//perform stft
		
		/*
		 * get data
		 */
		stftx=new Complex[sinks.length][][];
		for(int chidx=0;chidx<stftx.length;chidx++) 
			stftx[chidx]=sinks[chidx].toArray((Complex[][])null);	
	}

	public Complex[][] binData(int binidx,Complex[][] buffer)
	{
		//[number of channels][number of samples in each frequency bin]
		if(buffer==null) buffer=new Complex[stftx.length][stftx[0][0].length];
		else BLAS.checkDestinationSize(buffer,stftx.length,stftx[0][0].length);
		
		//copy data
		for(int chidx=0;chidx<stftx.length;chidx++) 
			for(int tidx=0;tidx<buffer[chidx].length;tidx++) 
				buffer[chidx][tidx]=stftx[chidx][binidx][tidx];
		
		return buffer;
	}

	public DemixingModel estimateDemixingModel(SignalSource x) throws IOException
	{
	Logger logger;
	DemixingModel model;
	
		logger=Logger.getLogger(LOGGER_NAME);

		/*
		 * perform stft
		 */
		logger.info("perform STFT with "+
				this.stftSize()+" STFT block size, "+
				this.stftOverlap()+" STFT overlap, "+
				this.fftSize()+" FFT block size");		
		stft(x);

		/*
		 * apply ica on each frequency bin
		 */
		logger.info("apply "+icaname);
		model=icastep.applyICA();
		
		/*
		 * solving permutation and scaling ambiguities
		 */
		logger.info("align demixing matrices with policy: "+apolicy.getClass().getName());
		apolicy.align(model);		
		spolicy.rescale(model);

		return model;
	}
	
	public DemixingModel separate(SignalSource x,SignalSink y) throws IOException
	{
	DemixingModel model;
	Logger logger;
	ArraySignalSource[] xin;
	ArraySignalSink[] yout;
	
		if(y.numChannels()!=this.numSources()) throw new IllegalArgumentException(
				"number of sources not match: "+y.numChannels()+", "+this.numSources());

		//estimate demixing model
		model=this.estimateDemixingModel(x);
	
		/*
		 * demix signals
		 */
		logger=Logger.getLogger(LOGGER_NAME);
		logger.info("demix stft data");
		
		xin=new ArraySignalSource[this.numSensors()];
		for(int sensorj=0;sensorj<xin.length;sensorj++) 
			xin[sensorj]=new ArraySignalSource(stftx[sensorj]);
	
		yout=new ArraySignalSink[this.numSources()];
		for(int sourcei=0;sourcei<yout.length;sourcei++) 
			yout[sourcei]=new ArraySignalSink(this.fftSize());
		
		model.applyFDFilters(xin,yout);
		for(ArraySignalSource temp:xin) temp.close();
		for(ArraySignalSink temp:yout) 
		{
			temp.flush();
			temp.close();
		}
		
		stfty=new Complex[this.numSources()][][];
		for(int sourcei=0;sourcei<stfty.length;sourcei++) 
			stfty[sourcei]=yout[sourcei].toArray((Complex[][])null);
		
		/*
		 * output estimated sources
		 */

		
		
		
		
		return model;
	}
	
	/**
	 * visualize sensor signals
	 */
	public void visualizeSensorSpectrograms()
	{
	BufferedImage[] imgs;
	ShortTimeFourierTransformer stft;
	
		if(stftx==null) return;
		stft=this.stfTransformer();
		
		imgs=new BufferedImage[stftx.length];
		for(int sensorj=0;sensorj<imgs.length;sensorj++) 
			imgs[sensorj]=stft.spectrogram(stftx[sensorj]);
		
		pp.util.Util.showImage(pp.util.Util.drawResult(1,imgs.length,5,imgs));
	}
	
	/**
	 * visualize estimated source signals
	 */
	public void visualizeSourceSpectrograms()
	{
	BufferedImage[] imgs;
	ShortTimeFourierTransformer stft;
		
		if(stfty==null) return;
		stft=this.stfTransformer();
			
		imgs=new BufferedImage[stfty.length];
		for(int sourcei=0;sourcei<imgs.length;sourcei++) 
			imgs[sourcei]=stft.spectrogram(stfty[sourcei]);
			
		pp.util.Util.showImage(pp.util.Util.drawResult(1,imgs.length,5,imgs));	
	}
	
	public static void main(String[] args) throws IOException, UnsupportedAudioFileException
	{
	CachedFDBSS foo;
	List<String> fsigs;
	WaveSource[] sigs;
	ArraySignalSink ests;
			
		/*
		 * get input channels
		 */
		fsigs=new LinkedList<String>();
		fsigs.add("data/source3.wav");
		fsigs.add("data/source4.wav");
					
		sigs=new WaveSource[fsigs.size()];
		for(int i=0;i<sigs.length;i++) 
		{	
			sigs[i]=new WaveSource(new File(fsigs.get(i)),true);
			System.out.println(sigs[i].audioFormat());
		}

		SignalMixer mixer=new SignalMixer(sigs);
		mixer.setMixer(new SignalMixer.InstantaneousMixer(
				BLAS.randMatrix(sigs.length+1,sigs.length)));
		
		/*
		 * perform frequency domain BSS
		 */
		foo=new CachedFDBSS();
		foo.setSTFTParameters(512,512*3/4,1024);
		foo.setNumSources(fsigs.size());
		
		ests=new ArraySignalSink(foo.numSources());
		foo.separate(mixer,ests);
		mixer.close();
		ests.flush();
		ests.close();
		
//		foo.visualizeSensorSpectrograms();
		foo.visualizeSourceSpectrograms();
	}
}
