package cn.edu.bjtu.cit.bss;
import java.io.*;
import java.awt.image.*;
import java.util.*;
import java.util.logging.*;
import java.util.regex.*;
import javax.sound.sampled.*;
import org.apache.commons.math.complex.*;
import cn.edu.bjtu.cit.bss.align.*;
import cn.edu.bjtu.cit.bss.ica.*;
import cn.edu.bjtu.cit.bss.iva.*;
import cn.edu.bjtu.cit.bss.signalio.*;
import cn.edu.bjtu.cit.bss.util.*;
import pp.util.BLAS;

/**
 * <h1>Description</h1>
 * Frequency domain blind source separation. Please see: Shoji MAKINO, Blind Source 
 * Separation of Convolutive Mixtures of Speech in Frequency Domain, 2005 as a general 
 * reference.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Feb 27, 2011 10:59:28 AM, revision:
 */
public class FDBSS extends FDBSSAlgorithm
{
private static final long serialVersionUID=6156713960690500066L;
private static final String LOGGER_NAME="cn.edu.bjtu.cit.bss";

/*
 * file prefix
 */
private static final String STFT_PREFIX="stftx";//stft file prefix
private static final String EST_STFT_PREFIX="stfty";//estimated stft file prefix
private static final String EST_PREFIX="y";//estimated source signal file prefix

/*
 * True to output intermediate results in raw format, false in txt format. 
 * Save intermediate data in text format is mainly for experimental purpose.
 */
private boolean rawformat=true;
private int nsources=0;//designated number of sources, 0 to auto detect

/*
 * buffer for sensor T-F data
 */
private int tfbuffersize=300;//number of frequency bins buffered
private TFBuffer tfbuffer;

/*
 * for instantaneous ICA
 */
private String preprocessorname="cn.edu.bjtu.cit.bss.preprocess.Whitening";//preprocessor name for ICA
private String icaname="cn.edu.bjtu.cit.bss.ica.CFastICA";//ICA algorithm name
private ICAStep icastep;//ICA in frequency domain BSS

private AlignPolicy apolicy;//used to solve the permutation and the scaling ambiguity
private ScalingPolicy spolicy;//solve the scaling ambiguity

private File workingdir;//working directories for temp files

	/**
	 * @param workingdir
	 * working directory for temp files
	 */
	public FDBSS(File workingdir)
	{
		if(workingdir==null) this.workingdir=new File(".");else this.workingdir=workingdir;
		if(!workingdir.exists()) workingdir.mkdirs();
		
		/*
		 * initialize instantaneous ICA
		 */
		icastep=new CommonICAStep();
		icastep.setFDBSSAlgorithm(this);
		icastep.setPreprocessor(preprocessorname);
		((CommonICAStep)icastep).setICA(icaname);
		
		/*
		 * initialize align and scaling policy
		 */
		apolicy=new RegionGrow();
		apolicy.setFDBSSAlgorithm(this);
		
		spolicy=new MaxContributionRescale();
	}
	
	/**
	 * get ica algorithm reference
	 * @return
	 */
	public ICAStep icaAlgorithm()
	{
		return icastep;
	}
	
	/**
	 * get align policy
	 * @return
	 */
	public AlignPolicy alignPolicy()
	{
		return apolicy;
	}
	
	/**
	 * <h1>Description</h1>
	 * Supported parameters.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Jun 2, 2011 11:05:49 AM, revision:
	 */
	public enum Parameter
	{
		/**
		 * Number of sources, 0 to auto detect.
		 */
		num_sources,
		/**
		 * Segment size for short time Fourier transform.
		 */
		stft_size,
		/**
		 * STFT block overlap.
		 */
		stft_overlap,
		/**
		 * FFT block size.
		 */
		fft_size,
		/**
		 * Istantaneous ICA preprocessor name.
		 */
		preprocessor,
		/**
		 * Instaneous ICA algorithm used.
		 */
		ica_algorithm,
		/**
		 * Align policy used to solve the permutation and scaling ambiguities.
		 */
		align_policy
	}
	
	/**
	 * get a parameter
	 * @param key
	 * parameter name
	 * @return
	 */
	public String getParameter(Parameter key)
	{
		switch(key)
		{
			case num_sources:
				return Integer.toString(nsources);
			case stft_size:
				return Integer.toString(this.stftSize());
			case stft_overlap:
				return Integer.toString(this.stftOverlap());
			case fft_size:	
				return Integer.toString(this.fftSize());
			case preprocessor:
				return preprocessorname;
			case ica_algorithm:
				return icaname;
			case align_policy:
				return apolicy.getClass().getName();
			default: throw new IllegalArgumentException("unknown parameter: "+key);
		}
	}
	
	/**
	 * set a parameter
	 * @param key
	 * parameter name
	 * @param value
	 * corresponding value
	 */
	public void setParameter(Parameter key,String value)
	{
		switch(key)
		{
			case num_sources:
				nsources=Integer.parseInt(value);
				break;
			/*
			 * for STFT
			 */
			case stft_size:
			{
			int stftsize,stftoverlap,fftsize;
			
				stftsize=Integer.parseInt(value);
				stftoverlap=stftsize*3/4;//overlap is also changed
				fftsize=ShortTimeFourierTransformer.nextPowerOf2(stftsize);
				this.setSTFTParameters(stftsize,stftoverlap,fftsize);
			}break;
			case stft_overlap:
			{
			int stftsize,stftoverlap,fftsize;
				
				stftsize=this.stftSize();
				stftoverlap=Integer.parseInt(value);
				fftsize=this.fftSize();
				this.setSTFTParameters(stftsize,stftoverlap,fftsize);
			}break;
			case fft_size:
			{
			int stftsize,stftoverlap,fftsize;
				
				stftsize=this.stftSize();
				stftoverlap=this.stftOverlap();
				fftsize=Integer.parseInt(value);
				this.setSTFTParameters(stftsize,stftoverlap,fftsize);
			}break;
			
			//data preprocessor name
			case preprocessor:
			{
				icastep.setPreprocessor(value);
				preprocessorname=value;
			}break;
			
			//set ica algorithm
			case ica_algorithm:
			{
			Object rawica;
				
				try
				{
					rawica=Class.forName(value).newInstance();
					
					//is a instantaneous ica
					if(rawica instanceof ICA)
					{
						icastep=new CommonICAStep();
						icastep.setFDBSSAlgorithm(this);
						icastep.setPreprocessor(preprocessorname);
						((CommonICAStep)icastep).setICA(value);
					}
					
					//is an iva
					else if(rawica instanceof IVA)
					{
						icastep=(ICAStep)rawica;
						icastep.setFDBSSAlgorithm(this);
						icastep.setPreprocessor(preprocessorname);
					}
					else throw new IllegalArgumentException("unknown ICA: "+value);
					
					icaname=value;
				}
				catch(InstantiationException e)
				{
					throw new RuntimeException("failed to set ica algorithm",e);
				}
				catch(IllegalAccessException e)
				{
					throw new RuntimeException("failed to set ica algorithm",e);
				}
				catch(ClassNotFoundException e)
				{
					throw new RuntimeException("failed to set ica algorithm",e);
				}
			}break;
	
			//set align policy
			case align_policy:
			{
				try
				{
					apolicy=(AlignPolicy)Class.forName(value).newInstance();
					apolicy.setFDBSSAlgorithm(this);
				}
				catch(InstantiationException e)
				{
					throw new IllegalArgumentException("failed to initialize align policy: "+value,e);
				}
				catch(IllegalAccessException e)
				{
					throw new IllegalArgumentException("failed to initialize align policy: "+value,e);
				}
				catch(ClassNotFoundException e)
				{
					throw new IllegalArgumentException("failed to initialize align policy: "+value,e);
				}
			}break;
			
			default: throw new IllegalArgumentException("unknown parameter: "+key);
		}
	}

	/**
	 * get working directory for temp files
	 * @return
	 */
	public File workingDirectory()
	{
		return workingdir;
	}
	
	/**
	 * clear temp files, stft files, bin files, demixing matrices file, 
	 * estimated data from working directory
	 * @throws IOException
	 */
	public void clearWorkingDirectory() throws IOException
	{
		for(File f:workingdir.listFiles()) 
			if(f.isFile()) f.delete();
	}
	
	/**
	 * get file path used to store stft for input signals
	 * @param index
	 * input channel index
	 * @return
	 */
	public File stftFile(int index)
	{
		if(rawformat) return new File(workingdir,STFT_PREFIX+index+".dat");
		else return new File(workingdir,STFT_PREFIX+index+".txt");
	}
	
	/**
	 * get file name path used to store estimated stft for output signals
	 * @param index
	 * output channel index
	 * @return
	 */
	public File estimatedSTFTFile(int index)
	{
		if(rawformat) return new File(workingdir,EST_STFT_PREFIX+index+".dat");
		else return new File(workingdir,EST_STFT_PREFIX+index+".txt");
	}
	
	/**
	 * get file path for demixing matrices of all frequency bins which are not aligned
	 * @return
	 */
	public File demixFile()
	{
		return new File(workingdir,"demix.txt");
	}
	
	/**
	 * get demixing model path whose permutation problem is solved
	 * @return
	 */
	public File alignedDemixFile()
	{
		return new File(workingdir,"demixp.txt");
	}
	
	/**
	 * get file path for demixing model whose permutation ambiguity and 
	 * scaling ambiguity are solved
	 * @return
	 */
	public File alignedAndScaledDemixFile()
	{
		return new File(workingdir,"demixps.txt");
	}
	
	/**
	 * Get number of output channels of the last calculation, this number 
	 * is determined by the demixing matrix size.
	 * @return
	 */
	private int numOutputChannels()
	{
	BufferedReader in=null;
	int count=0;
	boolean startcount=false;
	
		try
		{
			/*
			 * count the row number of the real part of the demixing matrices
			 */
			in=new BufferedReader(new InputStreamReader(new FileInputStream(demixFile())));
			for(String ts=null;(ts=in.readLine())!=null;)
			{
				ts=ts.trim();
				if(ts.length()==0) continue;
				if(ts.startsWith("real part:")) startcount=true;
				else if(ts.startsWith("imaginary part:")) break;
				else if(startcount) count++;
			}
		}
		catch(IOException e)
		{
			count=0;
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
		return count;
	}
	
	public int numSources()
	{
		if(nsources>0) return nsources;
		else nsources=numOutputChannels();//number of sources in last calculation
		
		if(nsources==0) return this.numSensors();
		else return nsources;
	}

	public int numSensors()
	{
	Pattern p;
	Matcher m;
	int count=0;	

		if(rawformat) p=Pattern.compile("^"+STFT_PREFIX+"\\d+\\.dat$");
		else p=Pattern.compile("^"+STFT_PREFIX+"\\d+\\.txt$");

		for(String file:workingdir.list())
		{
			m=p.matcher(file);
			if(m.find()) count++;
		}
		return count;
	}
	
	/**
	 * open stft file for input
	 * @param chidx
	 * input channel index
	 * @return
	 * @throws IOException
	 */
	public SignalSource openSTFTSource(int chidx) throws IOException
	{
	SignalSource source;
		
		//number of channels is fft block size
		if(rawformat) source=new RawSignalSource(
				new BufferedInputStream(new FileInputStream(stftFile(chidx))),
				this.fftSize());
		else 
		{
			source=new ComplexTextSignalSource(new FileInputStream(stftFile(chidx)));
			if(source.numChannels()!=this.fftSize()) throw new IllegalStateException(
					"channel size and fft size not match: "+source.numChannels()+", "+this.fftSize());
		}
			
		return source;
	}
	
	/**
	 * open stft file for output
	 * @param chidx
	 * input channel index
	 * @return
	 * @throws IOException
	 */
	private SignalSink openSTFTSink(int chidx) throws IOException
	{
		if(rawformat) return new RawSignalSink(
				new BufferedOutputStream(new FileOutputStream(stftFile(chidx))),
				this.fftSize());
		else return new ComplexTextSignalSink(
				new FileOutputStream(stftFile(chidx)),
				this.fftSize());
	}
	
	/**
	 * open estimated stft file for input
	 * @param chidx
	 * output channel index
	 * @return
	 * @throws IOException
	 */
	public SignalSource openEstimatedSTFTSource(int chidx) throws IOException
	{
	SignalSource source;
		
		if(rawformat) source=new RawSignalSource(
				new BufferedInputStream(new FileInputStream(estimatedSTFTFile(chidx))),
				this.fftSize());
		else
		{
			source=new ComplexTextSignalSource(new FileInputStream(estimatedSTFTFile(chidx)));
			if(source.numChannels()!=this.fftSize()) throw new IllegalStateException(
					"channel size and fft size not match: "+source.numChannels()+", "+this.fftSize());
		}
		
		return source;
	}
	
	/**
	 * open estimated stft file for output
	 * @param chidx
	 * output channel index
	 * @return
	 * @throws IOException
	 */
	private SignalSink openEstimatedSTFTSink(int chidx) throws IOException
	{
		if(rawformat) return new RawSignalSink(
				new BufferedOutputStream(new FileOutputStream(estimatedSTFTFile(chidx))),
				this.fftSize());
		else return new ComplexTextSignalSink(
				new FileOutputStream(estimatedSTFTFile(chidx)),
				this.fftSize());
	}
	
	/**
	 * Load complex data, including source and sensor stft files.
	 * @param path
	 * data file path
	 * @param buffer
	 * buffer used to cache data, null to allocate new space
	 * @return
	 * @throws IOException
	 */
	public Complex[][] loadComplexData(File path,Complex[][] buffer) throws IOException
	{
	int numchin;
	SignalSource source=null;
		
		/*
		 * specify number of channels
		 */
		if(!path.getAbsolutePath().startsWith(workingdir.getAbsolutePath())) 
			throw new IllegalArgumentException("data file is not in the working directory: "+
					path.getAbsolutePath());
		
		if(path.getName().startsWith(STFT_PREFIX)) numchin=this.fftSize();
		else if(path.getName().startsWith(EST_STFT_PREFIX)) numchin=this.fftSize();
		else throw new IllegalArgumentException("unknown signal data file: "+path.getName());
		
		try
		{
			/*
			 * load data
			 */
			if(rawformat) source=new RawSignalSource(
					new BufferedInputStream(new FileInputStream(path)),numchin);
			else 
			{	
				source=new ComplexTextSignalSource(new FileInputStream(path));
				if(source.numChannels()!=numchin) throw new IllegalStateException(
						"channel size not match: "+source.numChannels()+", "+numchin);
			}

			buffer=source.toArray(buffer);
		}
		finally
		{
			try
			{
				if(source!=null) source.close();
			}
			catch(IOException e)
			{}
		}

		return buffer;
	}
	
	/**
	 * get time-frequency data buffer, the buffer should be constructed 
	 * after STFT to get correct number of sensors
	 * @return
	 */
	private TFBuffer tfBuffer()
	{
		if(tfbuffer==null) tfbuffer=new TFBuffer(this,tfbuffersize);
		return tfbuffer;
	}
	
	public Complex[][] binData(int binidx,Complex[][] buffer)
	{
	Complex[][] bindata;
		
		try
		{
			bindata=tfBuffer().binData(binidx,buffer);
		}
		catch(IOException e)
		{
			throw new RuntimeException("failed to load frequency bin: "+binidx,e);
		}
		
		return bindata;
	}
	
	/**
	 * Perform stft on input signals, output data will be saved as: stft0.dat, 
	 * stft1.dat... in working directory.
	 * @param sigs
	 * multichannel input signals
	 * @return
	 * number of stft segments for input signal
	 * @throws IOException
	 */
	public int stft(SignalSource sigs) throws IOException
	{
	ShortTimeFourierTransformer stft;
	SignalSink[] stftsink=null;
	int segs;
	
		try
		{
			clearWorkingDirectory();//delete old temp files

			/*
			 * !!!
			 * stft must be get first to have parameters correctly set
			 */
			stft=this.stfTransformer();
			
			/*
			 * open streams
			 */
			stftsink=new SignalSink[sigs.numChannels()];
			for(int chidx=0;chidx<stftsink.length;chidx++) stftsink[chidx]=openSTFTSink(chidx);

			//perform stft
			segs=stft.stft(sigs,stftsink);
		
			for(SignalSink sink:stftsink) sink.flush();
		}
		finally
		{
			if(stftsink!=null) 
				for(SignalSink sink:stftsink) 
					try
					{
						if(sink!=null) sink.close();
					}
					catch(IOException e)
					{}
		}

		return segs;
	}
	
	/**
	 * load demixing model of last separation
	 * @return
	 * return null if failed
	 * @throws IOException
	 */
	public DemixingModel loadDemixingModel() throws IOException
	{
	File path;
	
		path=alignedAndScaledDemixFile();
		if(!path.exists()) return null;
		else return new DemixingModel(path);
	}
	
	/**
	 * load demixing model not aligned and rescaled
	 * @return
	 * return null if failed
	 * @throws IOException
	 */
	public DemixingModel loadDemixingModelNotAligned() throws IOException
	{
	File path;
		
		path=demixFile();
		if(!path.exists()) return null;
		else return new DemixingModel(path);		
	}
	
	/**
	 * separate mixed stft data by frequency domain demixing filters, and output 
	 * estimated source's stft data files as eststftx.dat in working directory.
	 * @throws IOException
	 */
	public void demixSTFT() throws IOException
	{
	DemixingModel model;
	SignalSource[] stftx=null;
	SignalSink[] stfty=null;
	
		try
		{
			/*
			 * load demixing model
			 */
			model=loadDemixingModel();
			if(model==null) throw new IllegalStateException("demixing model are not calculated");

			//delete old estimated stft files
			for(File f:workingdir.listFiles()) 
				if(f.getName().startsWith(EST_STFT_PREFIX)) f.delete();
		
			/*
			 * open signal streams
			 */
			//signal source for sensor data
			stftx=new SignalSource[this.numSensors()];
			for(int sensorj=0;sensorj<stftx.length;sensorj++) 
				stftx[sensorj]=openSTFTSource(sensorj);
		
			//signal sink for estimated source data
			stfty=new SignalSink[this.numSources()];
			for(int sourcei=0;sourcei<stfty.length;sourcei++) 
				stfty[sourcei]=openEstimatedSTFTSink(sourcei);

			/*
			 * demix in frequency domain
			 */
			model.applyFDFilters(stftx,stfty);
			for(SignalSink y:stfty) y.flush();
		}
		finally
		{
			if(stftx!=null) 
				for(SignalSource temp:stftx) 
					try
					{
						if(temp!=null) temp.close();
					}
					catch(IOException e)
					{}
			
			if(stfty!=null) 
				for(SignalSink temp:stfty) 
					try
					{
						if(temp!=null) temp.close();
					}
					catch(IOException e)
					{}
		}
	}
	
	public DemixingModel estimateDemixingModel(SignalSource x) throws IOException
	{
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Generate sensor signal's STFT directly, source signals are mixed 
	 * in frequency domain. Used for experiments.
	 * @param ss
	 * multichannel source signals
	 * @param mixm
	 * the mixing model
	 * the mixing model
	 * @throws IOException
	 */
	public void fdMix(SignalSource ss,MixingModel mixm) throws IOException
	{
	ShortTimeFourierTransformer stft;
	ArraySignalSink[] fds;
	Complex[][][] stfts;
	ArraySignalSource[] fds2;
	SignalSink[] fdx;
	
		clearWorkingDirectory();//clear temp results of last separation
	
		/*
		 * perform stft for source signals
		 */
		stft=stfTransformer();
		
		fds=new ArraySignalSink[ss.numChannels()];
		for(int n=0;n<fds.length;n++) fds[n]=new ArraySignalSink(stft.fftSize());
		
		stft.stft(ss,fds);

		stfts=new Complex[fds.length][][];
		for(int n=0;n<stfts.length;n++) 
		{
			fds[n].flush();
			stfts[n]=fds[n].toArray(stfts[n]);
			fds[n].close();
		}
		
		/*
		 * mix data and output to file
		 */
		fds2=new ArraySignalSource[stfts.length];
		for(int n=0;n<fds2.length;n++) fds2[n]=new ArraySignalSource(stfts[n]);
		
		fdx=new SignalSink[mixm.numSensors()];
		for(int m=0;m<fdx.length;m++) fdx[m]=this.openSTFTSink(m);
		
		mixm.applyFDFilters(fds2,fdx);
		
		for(int n=0;n<fds2.length;n++) fds2[n].close();
		for(int m=0;m<fdx.length;m++) 
		{
			fdx[m].flush();
			fdx[m].close();
		}
	}

	/**
	 * <h1>Description</h1>
	 * Operations in frequency domain blind source separation.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Mar 28, 2011 8:04:35 PM, revision:
	 */
	public enum Operation
	{
		/**
		 * perform stft on input signals
		 */
		stft,
		/**
		 * apply instaneout ICA on individual frequency bins
		 */
		ica,
		/**
		 * align and rescale demixing matrices
		 */
		align,
		/**
		 * calculate estimated source's stft data by demixing filters
		 */
		demix
	}
		
	/**
	 * Perform frequency domain blind source separation, selected operations 
	 * can be applied individually.
	 * @param sigs
	 * multichannel input signals
	 * @param ops
	 * selected operations
	 * @throws IOException
	 */
	public void separate(SignalSource sigs,Operation... ops) throws IOException
	{
	Logger logger;
	Set<Operation> opset;
		
		opset=new HashSet<Operation>();
		for(Operation op:ops) opset.add(op);
		logger=Logger.getLogger(LOGGER_NAME);
		
		/*
		 * step 1:
		 * perform stft transform
		 */
		if(opset.contains(Operation.stft))
		{
			/*
			 * perform stft
			 */
			logger.info("perform STFT with "+
					this.stftSize()+" STFT block size, "+
					this.stftOverlap()+" STFT overlap, "+
					this.fftSize()+" FFT block size");
			stft(sigs);//used as several single channel signal sources
		}
		
		/*
		 * step 2:
		 * apply complex ica to calculate demixing matrix for each frequency bin
		 */
		if(opset.contains(Operation.ica))
		{
		DemixingModel model;
		
			/*
			 * instantaneous ICA
			 */
			logger.info("apply "+icaname);
			model=icastep.applyICA();
			
			//save the demixing model
			model.save(demixFile());
		}
		
		/*
		 * step 3:
		 * alignment
		 */
		if(opset.contains(Operation.align))
		{	
		DemixingModel model;		
		
			logger.info("align demixing matrices with policy: "+apolicy.getClass().getName());
			
			model=new DemixingModel(demixFile());
			
			/*
			 * solve the permutation ambiguity and save result
			 */
			apolicy.align(model);
			model.save(alignedDemixFile());
			
			/*
			 * solve the scaling ambiguity and save result
			 */
			spolicy.rescale(model);
			model.save(alignedAndScaledDemixFile());
		}
		
		/*
		 * step 4:
		 * demixing stft data
		 */
		if(opset.contains(Operation.demix))
		{
			logger.info("demix stft data");
			demixSTFT();
		}
	}
	
	/**
	 * perform convolutive blind source separation in frequency domain
	 * @param sigs
	 * multichannel input signals
	 * @throws IOException
	 */
	public void separate(SignalSource sigs) throws IOException
	{
		separate(sigs,
				Operation.stft,
				Operation.ica,
				Operation.align,
				Operation.demix);
	}
	
	/**
	 * perform convolutive blind source separation
	 * @param sigs
	 * each for a single channel
	 * @throws IOException
	 */
	public void separate(SignalSource... sigs) throws IOException
	{
		separate(new SignalMixer(sigs));
	}
	
	public DemixingModel separate(SignalSource x,SignalSink y) throws IOException
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * load sensor signal
	 * @param sensorj
	 * sensor index
	 * @return
	 */
	public double[] sensorSignal(int sensorj)
	{
	SignalSource ss=null;
	Complex[][] stftx=null;
	double[] x=null;
		
		try
		{
			ss=openSTFTSource(sensorj);
			stftx=ss.toArray(stftx);
			x=this.stfTransformer().istft(stftx);
		}
		catch(IOException e)
		{
			throw new RuntimeException("failed to load sensor signal: "+sensorj,e);
		}
		finally
		{
			try
			{
				if(ss!=null) ss.close();
			}
			catch(IOException e)
			{}
		}
		
		return x;
	}
	
	/**
	 * get signal from the jth sensor to the ith estimated source
	 * @param sensorj
	 * sensor index
	 * @param sourcei
	 * estimated source index
	 * @return
	 */
	public double[] estimatedSourceSignalImage(int sensorj,int sourcei)
	{
	SignalSource ss=null;
	Complex[][] stftx=null;
	Complex[] fdf;
	double[] img;
		
		try
		{
			/*
			 * load sensor stft
			 */
			ss=openSTFTSource(sensorj);
			stftx=ss.toArray(stftx);
			
			/*
			 * apply filters
			 */
			fdf=loadDemixingModel().fdFilters()[sourcei][sensorj];
			for(int binidx=0;binidx<stftx.length;binidx++) 
				for(int tau=0;tau<stftx[binidx].length;tau++) 	
					stftx[binidx][tau]=stftx[binidx][tau].multiply(fdf[binidx]);
			
			//perform istft
			img=this.stfTransformer().istft(stftx);
		}
		catch(IOException e)
		{
			throw new RuntimeException(
					"failed to load sensor signal image: "+sourcei+", "+sensorj,e);
		}
		finally
		{
			try
			{
				if(ss!=null) ss.close();
			}
			catch(IOException e)
			{}
		}
		
		return img;
	}
	
	/**
	 * get estimated sources, mainly for experiment
	 * @param sourceidx
	 * source index
	 * @return
	 */
	public double[] estimatedSourceSignal(int sourceidx)
	{
	SignalSource ss=null;
	Complex[][] stfty=null;
	double[] y=null;
			
		try
		{
			ss=openEstimatedSTFTSource(sourceidx);
			stfty=ss.toArray(stfty);
			y=this.stfTransformer().istft(stfty);
		}
		catch(IOException e)
		{
			throw new RuntimeException("failed to load estimated source: "+sourceidx,e);
		}
		finally
		{
			try
			{
				if(ss!=null) ss.close();
			}
			catch(IOException e)
			{}
		}

		return y;
	}
	
	/**
	 * get estimated sources for experiment
	 * @return
	 * each row for a channel
	 */
	public double[][] estimatedSourceSignals()
	{
	double[][] esty;
	
		esty=new double[numSources()][];
		for(int sourcei=0;sourcei<esty.length;sourcei++) 
			esty[sourcei]=estimatedSourceSignal(sourcei);
		return esty;
	}
	
	/**
	 * output estimated source signals into file, named as: y0.wav(txt), y1.wav(txt)...
	 * @param destpath
	 * destination directory
	 * @param format
	 * audio format, null to output as text
	 * @throws IOException
	 */
	public void outputEstimatedSignals(File destpath,AudioFormat format) throws IOException
	{
	Pattern p;
	Matcher m;
	SignalSource source;
	SignalSink sink;
	
		/*
		 * delete old estimated signal files
		 */
		if(format==null) p=Pattern.compile("^"+EST_PREFIX+"\\d+\\.txt$");
		else p=Pattern.compile("^"+EST_PREFIX+"\\d+\\.wav$");
		
		for(File f:destpath.listFiles()) 
		{
			m=p.matcher(f.getName());
			if(m.find()) f.delete();
		}
		
		for(int sourcei=0;sourcei<this.numSources();sourcei++)
		{
			source=openEstimatedSTFTSource(sourcei);
			
			if(format==null) sink=new TextSignalSink(new BufferedOutputStream(
					new FileOutputStream(new File(destpath,EST_PREFIX+sourcei+".txt"))),1);
			else sink=new WaveSink(format,new File(destpath,EST_PREFIX+sourcei+".wav"));
			
			stfTransformer().istft(source,sink,false);
				
			source.close();
			sink.flush();
			sink.close();
		}
	}
	
	/**
	 * draw spectrogram for complex data
	 * @param path
	 * data file path
	 * @return
	 * @throws IOException
	 */
	public BufferedImage spectrogram(File path) throws IOException
	{
		return stfTransformer().spectrogram(loadComplexData(path,null));
	}
	
	/**
	 * visualize spectrograms of the input signals
	 * @param row
	 * row number of the subimages
	 * @param column
	 * column number of the subimages
	 * @throws IOException
	 */
	public void visualizeSensorSpectrograms(int row,int column) throws IOException
	{
	BufferedImage[] imgs;
	
		if(row*column<this.numSensors()) throw new IllegalArgumentException(
				"not enough subimages: "+(row*column)+
				", required: "+this.numSensors());
		
		imgs=new BufferedImage[this.numSensors()];
		for(int i=0;i<imgs.length;i++) imgs[i]=spectrogram(stftFile(i));
		pp.util.Util.showImage(pp.util.Util.drawResult(row,column,5,imgs));
	}
	
	/**
	 * visualize spectrograms of the output signals
	 * @param row
	 * row number of the subimages
	 * @param column
	 * column number of the subimages
	 * @throws IOException
	 */
	public void visualizeSourceSpectrograms(int row,int column) throws IOException
	{
	BufferedImage[] imgs;
	
		if(row*column<this.numSources()) throw new IllegalArgumentException(
				"not enough subimages: "+(row*column)+
				", required: "+this.numSources());
		
		imgs=new BufferedImage[this.numSources()];
		for(int i=0;i<imgs.length;i++) imgs[i]=spectrogram(estimatedSTFTFile(i));
		pp.util.Util.showImage(pp.util.Util.drawResult(row,column,5,imgs));
	}
	
	/**
	 * visualize spectrograms of input signals and output signals
	 * @param row
	 * row number of the subimages
	 * @param column
	 * column number of the subimages
	 * @throws IOException
	 */
	public void visualizeSpectrograms(int row,int column) throws IOException
	{
	BufferedImage[] imgs;
	
		if(row*column<(this.numSensors()+this.numSources())) throw new IllegalArgumentException(
				"not enough subimages: "+(row*column)+
				", required: "+(this.numSensors()+this.numSources()));
		imgs=new BufferedImage[this.numSensors()+this.numSources()];
		
		for(int i=0;i<this.numSensors();i++) imgs[i]=spectrogram(stftFile(i));
		for(int i=0;i<this.numSources();i++) imgs[i+this.numSensors()]=spectrogram(estimatedSTFTFile(i));
		pp.util.Util.showImage(pp.util.Util.drawResult(row,column,5,imgs));
	}

	public static void main(String[] args) throws IOException, UnsupportedAudioFileException
	{
	FDBSS foo;
	List<String> fsigs;
	WaveSource[] sigs;
	long t;
	
		foo=new FDBSS(new File("temp"));
		foo.setParameter(Parameter.stft_size,"512");//stft block size
		foo.setParameter(Parameter.stft_overlap,Integer.toString((int)(512*3/4)));//stft overlap
		foo.setParameter(Parameter.fft_size,"1024");//fft size, must be powers of 2
//		foo.setParameter(Parameter.ica_algorithm,"cn.edu.bjtu.cit.bss.ica.NCFastICA");
		
		/*
		 * get input channels
		 */
		fsigs=new LinkedList<String>();
		fsigs.add("data/SawadaDataset/s1.wav");
		fsigs.add("data/SawadaDataset/s2.wav");
				
		sigs=new WaveSource[fsigs.size()];
		for(int i=0;i<sigs.length;i++) 
		{	
			sigs[i]=new WaveSource(new File(fsigs.get(i)),true);
			System.out.println(sigs[i].audioFormat());
		}

		SignalMixer mixer=new SignalMixer(sigs);
		mixer.setMixer(new SignalMixer.InstantaneousMixer(
				BLAS.randMatrix(sigs.length,sigs.length)));

		/*
		 * perform frequency domain BSS
		 */
		t=System.currentTimeMillis();
		foo.separate(mixer);
		t=System.currentTimeMillis()-t;
		
		/*
		 * output results if needed
		 */
//		foo.outputEstimatedSignals(new File("/home/nay0648"),sigs[0].audioFormat());
		mixer.close();
		
		/*
		 * results visualization
		 */
		foo.visualizeSourceSpectrograms(1,foo.numSources());
		System.out.println("time spent: "+t+" ms");
	}
}
