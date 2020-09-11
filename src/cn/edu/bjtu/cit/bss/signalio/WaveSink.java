package cn.edu.bjtu.cit.bss.signalio;
import java.io.*;
import javax.sound.sampled.*;
import org.apache.commons.math.complex.*;

/**
 * <h1>Description</h1>
 * Output signals as wav data. See: https://ccrma.stanford.edu/courses/422/projects/WaveFormat/ 
 * for detail wave file header information.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Apr 1, 2011 11:02:32 AM, revision:
 */
public class WaveSink extends SignalSink
{
private AudioFormat format;//audio format
private int bps;//bytes per sample
private File path;//wav file path
private RandomAccessFile out=null;
private int quantization;//max quantized value
private int halfquantization;//used to adjust signed value

	/**
	 * initialize wave signal sink
	 * @param format
	 * audio format
	 * @param path
	 * audio file path
	 * @throws IOException
	 */
	private void initialize(AudioFormat format,File path) throws IOException
	{
		if(format.getEncoding()!=AudioFormat.Encoding.PCM_SIGNED&&
				format.getEncoding()!=AudioFormat.Encoding.PCM_UNSIGNED) 
			throw new IllegalArgumentException("only PCM encoding is supported");
		this.format=format;
		bps=format.getSampleSizeInBits()/Byte.SIZE;
		
		quantization=(int)Math.pow(2,format.getSampleSizeInBits())-1;
		halfquantization=(int)Math.pow(2,format.getSampleSizeInBits()-1);
		
		this.path=path;
		out=new RandomAccessFile(path,"rw");
		out.setLength(0);
		writeHeader();
	}

	/**
	 * @param format
	 * audio format of the wave file
	 * @param path
	 * wave file path
	 * @throws IOException
	 */
	public WaveSink(AudioFormat format,File path) throws IOException
	{
		initialize(format,path);
	}
	
	/**
	 * @param fs
	 * sample rate in Hertz
	 * @param samplesize
	 * sample size in bits
	 * @param numch
	 * number of channels
	 * @param signed
	 * true to use signed value
	 * @param bigendian
	 * true to use big endian, false to use little endian
	 * @param path
	 * wave file path
	 * @throws IOException
	 */
	public WaveSink(double fs,int samplesize,int numch,boolean signed,boolean bigendian,File path) 
		throws IOException
	{
		if(samplesize!=8&&samplesize!=16&&samplesize!=24) throw new IllegalArgumentException(
				"only 8, 16, 24 bits of sample size are supported");
		initialize(new AudioFormat((float)fs,samplesize,numch,signed,bigendian),path);
	}
	
	/**
	 * @param fs
	 * sample rate in Hertz
	 * @param samplesize
	 * sample size in bits
	 * @param numch
	 * number of channels
	 * @param path
	 * wave file path
	 * @throws IOException
	 */
	public WaveSink(double fs,int samplesize,int numch,File path) throws IOException
	{
	AudioFormat format;
		
		if(samplesize!=8&&samplesize!=16&&samplesize!=24) throw new IllegalArgumentException(
				"only 8, 16, 24 bits of sample size are supported");

		//construct audio format
		if(samplesize==8) format=new AudioFormat((float)fs,samplesize,numch,false,false);
		else format=new AudioFormat((float)fs,samplesize,numch,true,false);
		
		initialize(format,path);
	}
	
	public void flush() throws IOException
	{}
	
	public void close() throws IOException
	{
		/*
		 * write data size
		 */
		
		/*
		 * ChunkSize
		 */
		out.seek(4);
		writeInt((int)out.length()-8);
		
		/*
		 * Subchunk2Size
		 */
		out.seek(40);
		writeInt((int)out.length()-44);
		
		out.close();
	}
	
	/**
	 * get wave file path
	 * @return
	 */
	public File path()
	{
		return path;
	}

	/**
	 * get underlying wav file format
	 * @return
	 */
	public AudioFormat audioFormat()
	{
		return format;
	}
	
	public int numChannels()
	{
		return format.getChannels();
	}
	
	/**
	 * quantize a sample according to the audio format
	 * @param sample
	 * a sample
	 * @return
	 */
	public int quantize(double sample)
	{
	int quantized;
	
		//cut to [-1, 1]
		if(sample>1) sample=1;else if(sample<-1) sample=-1;

		quantized=(int)Math.round(((sample+1)*quantization)/2.0);
		if(quantized<0) quantized=0;else if(quantized>quantization) quantized=quantization;
		
		//adjust value for signed format
		if(AudioFormat.Encoding.PCM_SIGNED.equals(format.getEncoding())) quantized-=halfquantization;
		
		return quantized;
	}
	
	/**
	 * write a 4 byte integer in little endian form
	 * @param value
	 * a integer
	 * @throws IOException
	 */
	private void writeInt(int value) throws IOException
	{
		for(int i=0;i<4;i++)
		{
			out.writeByte((byte)(value&0x000000ff));
			value>>=8;
		}
	}
	
	/**
	 * write a 2 byte short in little endian form
	 * @param value
	 * a short integer
	 * @throws IOException
	 */
	private void writeShort(short value) throws IOException
	{
		for(int i=0;i<2;i++)
		{
			out.writeByte((byte)(value&0x000000ff));
			value>>=8;
		}
	}
	
	/**
	 * write audio format header into output stream
	 * @throws IOException
	 */
	private void writeHeader() throws IOException
	{
		/*
		 * ChunkID
		 */
		out.writeByte('R');
		out.writeByte('I');
		out.writeByte('F');
		out.writeByte('F');
		//ChunkSize, not specified yet
		writeInt(0);
		/*
		 * Format
		 */
		out.writeByte('W');
		out.writeByte('A');
		out.writeByte('V');
		out.writeByte('E');
		
		/*
		 * Subchunk1ID
		 */
		out.writeByte('f');
		out.writeByte('m');
		out.writeByte('t');
		out.writeByte(' ');
		//Subchunk1Size
		writeInt(16);
		//AudioFormat
		writeShort((short)1);
		//NumChannels
		writeShort((short)format.getChannels());
		//SampleRate
		writeInt((int)format.getSampleRate());
		//ByteRate, SampleRate * NumChannels * BitsPerSample/8
		writeInt((int)(format.getSampleRate()*format.getChannels()*bps));
		//BlockAlign, NumChannels * BitsPerSample/8
		writeShort((short)(format.getChannels()*bps));
		//BitsPerSample
		writeShort((short)format.getSampleSizeInBits());
		
		/*
		 * Subchunk2ID
		 */
		out.writeByte('d');
		out.writeByte('a');
		out.writeByte('t');
		out.writeByte('a');
		//Subchunk2Size, NumSamples * NumChannels * BitsPerSample/8, not specified yet
		writeInt(0);
	}
	
	/**
	 * get the wave header length
	 * @return
	 */
	private static int waveHeaderLength()
	{
		return 44;
	}
	
	/**
	 * output a sample without seek the file pointer
	 * @param sample
	 * a sample
	 * @throws IOException
	 */
	private void outputSample(double sample) throws IOException
	{
	int quantized;
	
		quantized=quantize(sample);

		if(format.isBigEndian()) for(int i=0;i<bps;i++)
			out.write((byte)((quantized>>((bps-1-i)*Byte.SIZE))&0x000000ff));
		else for(int i=0;i<bps;i++) 
			out.writeByte((byte)((quantized>>(i*Byte.SIZE))&0x000000ff));
	}

	public void writeFrame(double[] frame) throws IOException
	{
		for(double sample:frame) outputSample(sample);
	}

	public void writeFrame(Complex[] frame) throws IOException
	{
		for(Complex sample:frame) outputSample(sample.getReal());
	}
	
	public void writeSample(double s) throws IOException
	{
		out.seek(out.getFilePointer()+this.getCurrentChannel()*bps);
		outputSample(s);
		out.seek(out.getFilePointer()+(format.getChannels()-1-this.getCurrentChannel())*bps);
	}
	
	public void writeComplexSample(Complex s) throws IOException
	{
		writeSample(s.getReal());
	}
	
	public void writeSamples(double[] buffer,int offset,int len) throws IOException
	{	
		for(int i=offset;i<offset+len;i++) writeSample(buffer[i]);
	}
	
	public void writeSamples(Complex[] buffer,int offset,int len) throws IOException
	{
		for(int i=offset;i<offset+len;i++) writeComplexSample(buffer[i]);
	}
	
	/**
	 * seek the wave file to the specified frame position
	 * @param position
	 * frame position
	 * @throws IOException
	 */
	public void seek(long position) throws IOException
	{
		out.seek(waveHeaderLength()+position*bps*format.getChannels());
	}

	public static void main(String[] args) throws IOException, UnsupportedAudioFileException
	{
	WaveSink sink;
	WaveSource source;
	double[] buffer=new double[1024];
	
		sink=new WaveSink(8000,24,2,new File("/home/nay0648/wavesink.wav"));
		System.out.println(sink.audioFormat());

		source=new WaveSource(new File("data/source1.wav"),true);
		for(int count=0;(count=source.readSamples(buffer))>0;) sink.writeSamples(buffer,0,count);
		source.close();
		
		sink.seek(0);
		sink.setCurrentChannel(1);
		source=new WaveSource(new File("data/source2.wav"),true);
		for(int count=0;(count=source.readSamples(buffer))>0;) sink.writeSamples(buffer,0,count);
		source.close();
		
		sink.flush();
		sink.close();
	}
}
