package cn.edu.bjtu.cit.bss;
import java.io.*;
import java.util.*;
import javax.sound.sampled.*;
import org.apache.commons.math.complex.*;
import pp.util.BLAS;
import cn.edu.bjtu.cit.bss.FDBSS.Operation;
import cn.edu.bjtu.cit.bss.FDBSS.Parameter;
import cn.edu.bjtu.cit.bss.signalio.*;

/**
 * <h1>Description</h1>
 * Caches sensor T-F data for ICA algorithms.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Sep 7, 2012 8:26:28 AM, revision:
 */
public class TFBuffer implements Serializable
{
private static final long serialVersionUID=-7656882334262448244L;
private FDBSS fdbss;//bss algorithm reference
private int buffersize;//get number of frequency bins the buffer holds
//used to allocate and recycle buffers for a channel
private LinkedList<Complex[]> bqueue=new LinkedList<Complex[]>();
private int offset=-1;//frequency bin index offset
private int len=0;//valid buffer length
private int lastbinidx=-1;//last visited frequency bin index
//the cached sensor data, channel list, stft frame list
private List<List<Complex[]>> xdata;

	/**
	 * @param fdbss
	 * bss algorithm reference
	 * @param buffersize
	 * number of frequency bins need to be cached
	 */
	public TFBuffer(FDBSS fdbss,int buffersize)
	{
		this.fdbss=fdbss;
		this.buffersize=buffersize;
		
		/*
		 * construct t-f buffer structure
		 */
		//for channels
		xdata=new ArrayList<List<Complex[]>>(fdbss.numSensors());
		//for stft frames
		for(int m=0;m<fdbss.numSensors();m++) xdata.add(new LinkedList<Complex[]>());
	}
	
	/**
	 * get number of frequency bins the buffer holds
	 * @return
	 */
	public int buffersize()
	{
		return buffersize;
	}
	
	/**
	 * get number of stft frames in the buffer
	 * @return
	 */
	public int numSTFTFrames()
	{
		return xdata.get(0).size();
	}
	
	/**
	 * get data in a frequency bin
	 * @param binidx
	 * frequency bin index
	 * @param buffer
	 * space used to store data, null to allocate new space
	 * @return
	 * @throws IOException 
	 */
	public Complex[][] binData(int binidx,Complex[][] buffer) throws IOException
	{
	int n=0,tau;
	
		if(binidx<0||binidx>(fdbss.fftSize()/2)) throw new IndexOutOfBoundsException(
				"frequency bin index out of bounds: "+binidx+", [0, "+(fdbss.fftSize()/2)+"]");

		//buffer missed, load from file
		if(len==0||binidx<offset||binidx>=offset+len) 
		{
			//the lowest subband
			if(binidx==0) loadTFData(0);
			//the highest subband
			else if(binidx==fdbss.fftSize()/2) loadTFData(Math.max(binidx-buffersize()+1,0));
			//from high frequency to low frequency
			else if(lastbinidx-binidx>0) loadTFData(Math.max(binidx-buffersize()+1,0));
			//default: from low frequency to high frequency
			else loadTFData(binidx);
		}
	
		/*
		 * fetch data from buffer
		 */
		if(buffer==null) buffer=new Complex[xdata.size()][numSTFTFrames()];
		else BLAS.checkDestinationSize(buffer,xdata.size(),numSTFTFrames());
		
		for(List<Complex[]> bl:xdata) 
		{
			tau=0;
			for(Complex[] buff:bl) buffer[n][tau++]=buff[binidx-offset];
			n++;
		}
		
		lastbinidx=binidx;
		return buffer;
	}
	
	/**
	 * allocate a new buffer
	 * @return
	 */
	private Complex[] allocateBuffer()
	{
		if(bqueue.isEmpty()) return new Complex[buffersize];
		else return bqueue.remove(0);
	}
	
	/**
	 * recycle buffers for further use
	 * @param bl
	 * buffer list
	 */
	private void recycleBuffer(List<Complex[]> bl)
	{
		bqueue.addAll(bl);
		bl.clear();
	}
	
	/**
	 * load T-F data from file into buffer
	 * @param offset
	 * the starting frequency bin index
	 * @throws IOException 
	 */
	private void loadTFData(int offset) throws IOException
	{
	SignalSource[] stftin;//input stream for stft data from each channel
	Complex[] stftframe;//frame for a stft block
	Complex[] buff;//buffer
	
		if(offset<0||offset>(fdbss.fftSize()/2)) throw new IndexOutOfBoundsException(
				"offset out of bounds: "+offset+", [0, "+(fdbss.fftSize()/2)+"]");
		
		//recycle old buffers
		for(List<Complex[]> bl:xdata) recycleBuffer(bl);
		
		/*
		 * calculate number of frequency bins need to be loaded
		 */
		this.offset=offset;
		if(offset+buffersize()<=fdbss.fftSize()/2+1) len=buffersize();
		else len=fdbss.fftSize()/2+1-offset;
	
//		System.out.println("load ["+offset+", "+(offset+len-1)+"] from file");
		
		//used to read stft data, stft data must be perpared first
		stftin=new SignalSource[fdbss.numSensors()];
		stftframe=new Complex[fdbss.fftSize()];//frame size is equal to the fft size

		/*
		 * load frequency bins
		 */
		//open stft files
		for(int m=0;m<stftin.length;m++) stftin[m]=fdbss.openSTFTSource(m);
		
		//read stft data of each channel
eof:	for(;;)
		{
			//read one stft frame from each channel
			for(int m=0;m<stftin.length;m++)
			{
				try
				{
					stftin[m].readFrame(stftframe);
				}
				catch(EOFException e)
				{
					break eof;
				}
				
				/*
				 * copy curresponding part into buffer
				 */
				buff=allocateBuffer();
				System.arraycopy(stftframe,offset,buff,0,len);
				xdata.get(m).add(buff);
			}
		}
	
		//close stft file
		for(int m=0;m<stftin.length;m++) stftin[m].close();
	}
	
	public static void main(String[] args) throws IOException, UnsupportedAudioFileException
	{
	FDBSS fdbss;
	List<String> fsigs;
	WaveSource[] sigs;
	SignalMixer mixer;
	TFBuffer buffer;
	Complex[][] xdata=null;
	
		/*
		 * init bss algorithm
		 */
		fdbss=new FDBSS(new File("temp"));
		fdbss.setParameter(Parameter.stft_size,"512");
		fdbss.setParameter(Parameter.stft_overlap,Integer.toString((int)(512*3/4)));
		fdbss.setParameter(Parameter.fft_size,"1024");
		
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

		mixer=new SignalMixer(sigs);
		
		/*
		 * perform stft
		 */
		fdbss.separate(mixer,Operation.stft);
		mixer.close();
		
		/*
		 * test the buffer
		 */
		buffer=new TFBuffer(fdbss,100);
		for(int binidx=0;binidx<fdbss.fftSize()/2+1;binidx++) 
		{
			System.out.println("get frequency bin "+binidx+":");
			xdata=buffer.binData(binidx,xdata);
		}
		
		System.out.println();
		for(int binidx=fdbss.fftSize()/2;binidx>=0;binidx--) 
		{
			System.out.println("get frequency bin "+binidx+":");
			xdata=buffer.binData(binidx,xdata);
		}
	}
}
