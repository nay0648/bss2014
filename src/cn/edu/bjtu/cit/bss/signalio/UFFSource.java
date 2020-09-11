package cn.edu.bjtu.cit.bss.signalio;
import java.io.*;
import java.util.*;
import org.apache.commons.math.complex.*;
import cn.edu.bjtu.cit.bss.util.*;

/**
 * <h1>Description</h1>
 * Used to read the UFF (Universal File Format) data.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Jun 1, 2011 3:48:26 PM, revision:
 */
public class UFFSource extends SignalSource
{
private static final String BLOCK_END="-1";
private BufferedReader in=null;//underlying reader
private String[] header;//uff header, each entry for a line
private int universaldatasetnumber;
private long len;//data length in samples
private double fs;//sample rate in Hz
private List<Double> linebuffer=new LinkedList<Double>();//cache samples of a line

	/**
	 * @param uffin
	 * uff file input stream
	 * @throws IOException 
	 */
	public UFFSource(InputStream uffin) throws IOException
	{
		in=new BufferedReader(new InputStreamReader(uffin));
		
		/*
		 * read header
		 */
		header=new String[12];
		
		//the first "-1"
		if(!BLOCK_END.equals(readLine())) 
			throw new DataFormatException("uff file should start with: "+BLOCK_END);
		
		for(int i=0;i<header.length;i++) header[i]=readLine();
		parseHeader(header);
	}
	
	/**
	 * parse information from header
	 * @param header
	 * uff header
	 */
	private void parseHeader(String[] header)
	{
	String[] temp;
	
		//get universal dataset number
		universaldatasetnumber=Integer.parseInt(header[0].trim());
		
		/*
		 * parse record 7
		 */
		temp=record(7).trim().split("\\s+");
		len=Long.parseLong(temp[1]);//field 2: number of samples
		fs=1.0/Double.parseDouble(temp[4]);//field 5: absciissa increment
	}
	
	/**
	 * get uff header
	 * @return
	 */
	public String[] header()
	{
	String[] h2;
	
		/*
		 * make a copy to prevent modification
		 */
		h2=new String[header.length];
		System.arraycopy(header,0,h2,0,header.length);
		
		return h2;
	}
	
	/**
	 * get record from header
	 * @param i
	 * from 1 to 11
	 * @return
	 */
	public String record(int i)
	{
		if(i<1||i>11) throw new IllegalArgumentException(
				"record number should between 1 and 11: "+i);
		return header[i];
	}
	
	/**
	 * get universal dataset number
	 * @return
	 */
	public int universalDatasetNumber()
	{
		return universaldatasetnumber;
	}
	
	/**
	 * get data length in number of samples
	 * @return
	 */
	public long length()
	{
		return len;
	}
	
	/**
	 * get sample rate in Hertz
	 * @return
	 */
	public double sampleRate()
	{
		return fs;
	}

	public int numChannels()
	{
		return 1;
	}
	
	/**
	 * skip empty line
	 * @return
	 * @throws IOException
	 */
	private String readLine() throws IOException
	{
		for(String ts=null;(ts=in.readLine())!=null;)
		{
			ts=ts.trim();
			if(ts.length()>0) return ts;
		}
		return null;
	}
	
	/**
	 * read samples in next line
	 * @throws IOException, EOFException 
	 */
	private void readDataLine() throws IOException, EOFException
	{
	String ts;
	
		ts=readLine();
		if(ts==null||BLOCK_END.equals(ts)) throw new EOFException();
		for(String ss:ts.trim().split("\\s+")) linebuffer.add(Double.parseDouble(ss));
	}

	public void readFrame(double[] frame) throws IOException,EOFException
	{
		this.checkFrameSize(frame.length);
		
		if(linebuffer.isEmpty()) readDataLine();//read data from next line
		
		frame[0]=linebuffer.remove(0);
	}

	public void readFrame(Complex[] frame) throws IOException,EOFException
	{
		this.checkFrameSize(frame.length);
		
		if(linebuffer.isEmpty()) readDataLine();//read data from next line
		
		frame[0]=new Complex(linebuffer.remove(0),0);
	}

	public void close() throws IOException
	{
		if(in!=null) in.close();
	}
	
	/**
	 * Split multichannel uff data file into multiple single channel uff files, 
	 * output files will be named as channel0.uff, channel1.uff... under the 
	 * same directory as the input uff path.
	 * @param path
	 * uff data file path
	 * @throws IOException
	 */
	public static void splitUFF(File path) throws IOException
	{
	int chidx=0;//channel index
	BufferedReader in=null;
	BufferedWriter out=null;
	boolean segstarted=false;
	
		try
		{
			in=new BufferedReader(new InputStreamReader(new FileInputStream(path)));

			for(String ts=null;(ts=in.readLine())!=null;)
			{
				ts=ts.trim();
				if(ts.length()==0) continue;
				
				if(BLOCK_END.equals(ts))
				{
					//segment end
					if(segstarted)
					{
						out.write(ts+"\n");
						out.flush();
						out.close();
						segstarted=false;
					}
					//a new segment start
					else
					{
						System.out.println("channel "+chidx+":");
						out=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
								new File(path.getParent(),"channel"+(chidx++)+".uff"))));
						out.write(ts+"\n");
						segstarted=true;
					}
				}
				else out.write(ts+"\n");
			}
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
	}
	
	public static void main(String[] args) throws IOException
	{
	UFFSource source;
	double[] data=null;
	
//		UFFSource.splitUFF(new File("/home/nay0648/data/research/highspeed_train/noisedata/BKdata/380/380.uff"));

		source=new UFFSource(new FileInputStream(
				"/home/nay0648/data/research/highspeed_train/noisedata/BKdata/380/channel8.uff"));
		
		for(int i=1;i<=11;i++) System.out.println(source.record(i));
		
		data=source.toArray(data);
		Util.playAsAudio(data,source.sampleRate());
		
		source.close();
	}
}
