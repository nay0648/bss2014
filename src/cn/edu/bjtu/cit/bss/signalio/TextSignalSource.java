package cn.edu.bjtu.cit.bss.signalio;
import java.io.*;
import java.util.regex.*;
import org.apache.commons.math.complex.*;
import pp.util.BLAS;

/**
 * <h1>Description</h1>
 * Used to read signal from text streams.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Mar 8, 2011 11:46:18 AM, revision:
 */
public class TextSignalSource extends SignalSource
{
private static final Pattern FORMAT1=Pattern.compile("^(.+)(\\+|-)(.+)[i|j]$");//real and imaginary part
private static final Pattern FORMAT2=Pattern.compile("^(.+)[i|j]$");//only imaginary part
private BufferedReader textin=null;//nuderlying reader
private int numch;//number of channels
private String[] sframe=null;

	/**
	 * @param in
	 * underlying input stream
	 * @throws IOException
	 */
	public TextSignalSource(InputStream in) throws IOException
	{
		textin=new BufferedReader(new InputStreamReader(in));
		sframe=nextFrame();
		numch=sframe.length;
	}
	
	public void close() throws IOException
	{
		textin.close();
	}

	public int numChannels()
	{
		return numch;
	}
	
	/**
	 * read next frame
	 * @return
	 * return null if get an eof
	 * @throws IOException
	 */
	private String[] nextFrame() throws IOException
	{
	String ts;
	
		for(ts=null;(ts=textin.readLine())!=null;)
		{
			ts=ts.trim();
			if(ts.length()==0) continue;
			return ts.split("\\s+");
		}
		return null;
	}
	
	/**
	 * parse a sample as double
	 * @param ssample
	 * string format of a sample
	 * @return
	 */
	private double parseDouble(String ssample)
	{
	Matcher m;
	
		m=FORMAT1.matcher(ssample);
		if(m.find()) return Double.parseDouble(m.group(1));
		m=FORMAT2.matcher(ssample);
		if(m.find()) return 0;
		return Double.parseDouble(ssample);
	}
	
	/**
	 * parse a sample as complex
	 * @param ssample
	 * string format of a sample
	 * @return
	 */
	private Complex parseComplex(String ssample)
	{
	Matcher m;
	double real,imag;
		
		m=FORMAT1.matcher(ssample);
		if(m.find()) 
		{
			real=Double.parseDouble(m.group(1));
			imag=Double.parseDouble(m.group(3));
			if("-".equals(m.group(2))) imag*=-1;
			return new Complex(real,imag);
		}
		m=FORMAT2.matcher(ssample);
		if(m.find()) return new Complex(0,Double.parseDouble(m.group(1)));
		return new Complex(Double.parseDouble(ssample),0);
	}

	public void readFrame(double[] frame) throws IOException,EOFException
	{
		this.checkFrameSize(frame.length);
		if(sframe==null) throw new EOFException();
		this.checkFrameSize(sframe.length);
		for(int i=0;i<frame.length;i++) frame[i]=parseDouble(sframe[i]);
		sframe=nextFrame();
	}

	public void readFrame(Complex[] frame) throws IOException, EOFException
	{
		this.checkFrameSize(frame.length);
		if(sframe==null) throw new EOFException();
		this.checkFrameSize(sframe.length);
		for(int i=0;i<frame.length;i++) frame[i]=parseComplex(sframe[i]);
		sframe=nextFrame();
	}
	
	public static void main(String[] args) throws IOException
	{
	TextSignalSource source;
	
		source=new TextSignalSource(new FileInputStream("/home/nay0648/test.txt"));
		System.out.println(BLAS.toString(source.toArray((double[][])null)));
		source.close();
		
		source=new TextSignalSource(new FileInputStream("/home/nay0648/test.txt"));
		System.out.println(BLAS.toString(source.toArray((Complex[][])null)));
		source.close();
	}
}
