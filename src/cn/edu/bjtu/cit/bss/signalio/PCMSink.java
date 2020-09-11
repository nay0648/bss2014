package cn.edu.bjtu.cit.bss.signalio;
import java.io.*;
import javax.sound.sampled.*;
import org.apache.commons.math.complex.*;

/**
 * <h1>Description</h1>
 * Used to output raw PCM data.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Mar 13, 2012 3:33:22 PM, revision:
 */
public class PCMSink extends SignalSink
{
private AudioFormat format;//audio format
private BufferedOutputStream out=null;//underlying output stream
private int bps;//bytes per sample
private int quantization;//max quantized value
private int halfquantization;//used to adjust signed value

	/**
	 * @param format
	 * audio format
	 * @param out
	 * underlying output stream
	 */
	public PCMSink(AudioFormat format,OutputStream out)
	{
		this.format=format;
		this.out=new BufferedOutputStream(out);
		
		if(format.getEncoding()!=AudioFormat.Encoding.PCM_SIGNED&&
				format.getEncoding()!=AudioFormat.Encoding.PCM_UNSIGNED) 
			throw new IllegalArgumentException("only PCM encoding is supported");
		bps=format.getSampleSizeInBits()/Byte.SIZE;
		
		quantization=(int)Math.pow(2,format.getSampleSizeInBits())-1;
		halfquantization=(int)Math.pow(2,format.getSampleSizeInBits()-1);
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
	 * get audio format
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
			out.write((byte)((quantized>>(i*Byte.SIZE))&0x000000ff));
	}

	public void writeFrame(double[] frame) throws IOException
	{
		for(double sample:frame) outputSample(sample);
	}

	public void writeFrame(Complex[] frame) throws IOException
	{
		for(Complex sample:frame) outputSample(sample.getReal());
	}

	public void flush() throws IOException
	{
		out.flush();		
	}

	public void close() throws IOException
	{
		out.close();
	}
}
