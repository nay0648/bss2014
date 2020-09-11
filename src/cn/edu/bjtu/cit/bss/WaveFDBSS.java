package cn.edu.bjtu.cit.bss;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import javax.sound.sampled.*;
import cn.edu.bjtu.cit.bss.FDBSS.Operation;
import cn.edu.bjtu.cit.bss.FDBSS.Parameter;
import cn.edu.bjtu.cit.bss.signalio.*;

/**
 * <h1>Description</h1>
 * Frequency domain BSS for wave files.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Apr 6, 2012 9:01:07 AM, revision:
 */
public class WaveFDBSS implements Serializable
{
private static final long serialVersionUID=-4698443471142222877L;
private static final String EST_PREFIX="esty";//estimated source signal file prefix
private FDBSS fdbss;//the algorithm
private List<File> wavlist;//all sensor signal path

	/**
	 * @param tempdir
	 * directory for temp files
	 * @param wavpath
	 * path for wave files
	 */
	public WaveFDBSS(File tempdir,File... wavpath)
	{
		fdbss=new FDBSS(tempdir);
		
		wavlist=new ArrayList<File>(wavpath.length);
		for(File path:wavpath) wavlist.add(path);
	}
	
	/**
	 * get bss algorithm parameter
	 * @param key
	 * parameter name
	 * @return
	 */
	public String getParameter(Parameter key)
	{
		return fdbss.getParameter(key);
	}
	
	/**
	 * set bss algorithm parameter
	 * @param key
	 * parameter name
	 * @param value
	 * corresponding value
	 */
	public void setParameter(Parameter key,String value)
	{
		fdbss.setParameter(key,value);
	}
	
	/**
	 * get number of sources
	 * @return
	 */
	public int numSources()
	{
		return fdbss.numSources();
	}
	
	/**
	 * get number of sensors
	 * @return
	 */
	public int numSensors()
	{
		return wavlist.size();
	}
	
	/**
	 * get audio format
	 * @return
	 * @throws IOException
	 * @throws UnsupportedAudioFileException
	 */
	public AudioFormat audioFormat() throws IOException, UnsupportedAudioFileException
	{
	WaveSource source=null;
	AudioFormat format;
	
		try
		{
			source=openSensorSignal(0);
			format=source.audioFormat();
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
		
		return format;
	}
	
	/**
	 * open a wave signal
	 * @param sensoridx
	 * sensor index
	 * @return
	 * @throws IOException
	 * @throws UnsupportedAudioFileException
	 */
	public WaveSource openSensorSignal(int sensoridx) throws IOException, UnsupportedAudioFileException
	{
		return new WaveSource(wavlist.get(sensoridx),true);
	}

	/**
	 * open all wave signals as multichannel signal source
	 * @return
	 * @throws IOException
	 * @throws UnsupportedAudioFileException
	 */
	public SignalMixer openSensorSignals() throws IOException, UnsupportedAudioFileException
	{
	WaveSource[] ch;
//	AudioFormat format=null;
	
		ch=new WaveSource[numSensors()];
		for(int sensoridx=0;sensoridx<ch.length;sensoridx++) 
		{
			ch[sensoridx]=openSensorSignal(sensoridx);
	
			/*
			 * AudioFormat's equals will not work correctly!
			 */
//			if(format==null) format=ch[sensoridx].audioFormat();
//			else if(!format.equals(ch[sensoridx].audioFormat())) throw new IllegalArgumentException(
//					"wrong audio format: "+ch[sensoridx].audioFormat()+", required: "+format);
		}
		
		return new SignalMixer(ch);
	}
	
	/**
	 * perform frequency domain blind source separation
	 * @param ops
	 * required operations
	 * @throws IOException
	 * @throws UnsupportedAudioFileException
	 */
	public void separate(Operation... ops) throws IOException, UnsupportedAudioFileException
	{
		fdbss.separate(openSensorSignals(),ops);
	}
	
	/**
	 * perform frequency domain blind source separation
	 * @throws IOException
	 * @throws UnsupportedAudioFileException
	 */
	public void separate() throws IOException, UnsupportedAudioFileException
	{
		fdbss.separate(openSensorSignals());
	}
	
	/**
	 * load the demixing model of last separation
	 * @return
	 * @throws IOException
	 */
	public DemixingModel loadDemixingModel() throws IOException
	{
		return fdbss.loadDemixingModel();
	}
	
	/**
	 * output separation result as wave files
	 * @param dest
	 * destination directory path
	 * @param td
	 * true to use time domain demixing, false to use frequency domain demixing
	 * @throws IOException
	 * @throws UnsupportedAudioFileException
	 */
	public void outputAsWave(File dest,boolean td) throws IOException, UnsupportedAudioFileException
	{
	Pattern p=Pattern.compile("^"+EST_PREFIX+"\\d+\\.wav$");
	Matcher m;
	AudioFormat format;
		
		//delete old estimated wave files
		for(File f:dest.listFiles()) 
		{
			m=p.matcher(f.getName());
			if(m.find()) f.delete();
		}
		
		format=audioFormat();
		
		//time domain demixing
		if(td)
		{
		SignalMixer source;
		WaveSink[] sink;
		DemixingModel demixm;
		
			source=openSensorSignals();
			
			sink=new WaveSink[numSources()];
			for(int sourcei=0;sourcei<sink.length;sourcei++) 
				sink[sourcei]=new WaveSink(format,new File(dest,EST_PREFIX+sourcei+".wav"));
			
			demixm=loadDemixingModel();
			demixm.applyTDFilters(source,sink);
			
			source.close();
			for(WaveSink ws:sink) 
			{
				ws.flush();
				ws.close();
			}
		}
		//frequency domain demixing
		else
		{
		SignalSource source;
		WaveSink sink;
			
			for(int sourcei=0;sourcei<numSources();sourcei++)
			{
				source=fdbss.openEstimatedSTFTSource(sourcei);
				sink=new WaveSink(format,new File(dest,EST_PREFIX+sourcei+".wav"));
					
				fdbss.stfTransformer().istft(source,sink,false);
					
				source.close();
				sink.flush();
				sink.close();
			}					
		}
	}
	
	public static void main(String[] args) throws IOException, UnsupportedAudioFileException
	{
	WaveFDBSS foo;
	
		foo=new WaveFDBSS(new File("temp"),
				new File("data/rsm2_mA.wav"),
				new File("data/rsm2_mB.wav"));
		
		foo.separate();
		
		foo.outputAsWave(new File("/home/nay0648"),true);
	}
}
