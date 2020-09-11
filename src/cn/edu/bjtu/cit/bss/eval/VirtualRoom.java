package cn.edu.bjtu.cit.bss.eval;
import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * <h1>Description</h1>
 * A virtual reverberation room mixing environment for experiment.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Mar 28, 2012 7:19:58 PM, revision:
 */
public class VirtualRoom implements Serializable
{
private static final long serialVersionUID=-5686479996636895054L;
private static final String C="signal propagation velocity:";
private static final String FS="sampling rate:";
private static final String ROOMSIZE="room size:";
private static final String RT60="reverberation time:";
private static final String SOURCELOCS="source locations:";
private static final String SENSORLOCS="sensor locations:";
private static final String FILTERS="filters:";

private double c;//signal propagation velocity
private double fs;//sampling rate
private double[] roomsize;//room size
private double rt60;//reverberation time
private double[][] sourceloc;//source locations (N x 3)
private double[][] sensorloc;//sensor locations (M x 3)
private double[][][] tdmixf;//mixing filters [sensor index][source index][tap index]

	/**
	 * @param c
	 * signal propagation velocity (m/s)
	 * @param fs
	 * sampling rate (Hz)
	 * @param roomsize
	 * room size x, y, z (m)
	 * @param rt60
	 * reverberation time (s)
	 * @param sourceloc
	 * source locations (N x 3)
	 * @param sensorloc
	 * sensor locations (M x 3)
	 * @param tdmixf
	 */
	public VirtualRoom(double c,double fs,double[] roomsize,double rt60,
			double[][] sourceloc,double[][] sensorloc,double[][][] tdmixf)
	{
		this.c=c;
		this.fs=fs;
		this.roomsize=roomsize;
		this.rt60=rt60;
		this.sourceloc=sourceloc;
		this.sensorloc=sensorloc;
		this.tdmixf=tdmixf;
		
		if(sourceloc.length!=tdmixf[0].length) throw new IllegalArgumentException(
				"number of sources not match: "+sourceloc.length+", "+tdmixf[0].length);
		if(sensorloc.length!=tdmixf.length) throw new IllegalArgumentException(
				"number of sensors not match: "+sensorloc.length+", "+tdmixf.length);
	}
	
	/**
	 * load virtual room from file
	 * @param path
	 * file path
	 * @throws IOException
	 */
	public VirtualRoom(File path) throws IOException
	{
	BufferedReader in=null;
	boolean psourceloc=false,psensorloc=false,pfilters=false;
	List<double[]> sourcelocl,sensorlocl,filterl;
	Pattern ploc;//used to parse locations

		try
		{
			in=new BufferedReader(new InputStreamReader(new FileInputStream(path)));
			sourcelocl=new LinkedList<double[]>();
			sensorlocl=new LinkedList<double[]>();
			filterl=new LinkedList<double[]>();
			ploc=Pattern.compile("^(.+)\\s+(.+)\\s+(.+)$");
			
			for(String ts=null;(ts=in.readLine())!=null;)
			{
				ts=ts.trim();
				if(ts.length()<=0) continue;
				
				//signal propagation velocity
				if(ts.startsWith(C))
					c=Double.parseDouble(ts.substring(C.length()).trim());
				//sampling rate
				else if(ts.startsWith(FS)) 
					fs=Double.parseDouble(ts.substring(FS.length()).trim());
				//room size
				else if(ts.startsWith(ROOMSIZE)) 
				{
				Pattern proom;
				Matcher m;
				
					proom=Pattern.compile("^(.+)x(.+)x(.+)$");
					m=proom.matcher(ts.substring(ROOMSIZE.length()).trim());
					if(!m.find()) throw new IllegalArgumentException("illegal room size: "+ts);
					
					roomsize=new double[3];
					roomsize[0]=Double.parseDouble(m.group(1).trim());
					roomsize[1]=Double.parseDouble(m.group(2).trim());
					roomsize[2]=Double.parseDouble(m.group(3).trim());
				}
				//reverberation time
				else if(ts.startsWith(RT60)) 
					rt60=Double.parseDouble(ts.substring(RT60.length()).trim());
				//source locations
				else if(ts.startsWith(SOURCELOCS)) 
				{
					psourceloc=true;
					psensorloc=false;
					pfilters=false;
				}
				//sensor locations
				else if(ts.startsWith(SENSORLOCS))
				{
					psourceloc=false;
					psensorloc=true;
					pfilters=false;
				}
				//filters
				else if(ts.startsWith(FILTERS))
				{
					psourceloc=false;
					psensorloc=false;
					pfilters=true;
				}
				else
				{
					//add source location
					if(psourceloc)
					{
					Matcher m;
					double[] loc;

						m=ploc.matcher(ts);
						if(!m.find()) throw new IllegalArgumentException("illegal source location: "+ts);
						loc=new double[3];
						loc[0]=Double.parseDouble(m.group(1));
						loc[1]=Double.parseDouble(m.group(2));
						loc[2]=Double.parseDouble(m.group(3));
						sourcelocl.add(loc);
					}
					//add sensor location
					else if(psensorloc)
					{
					Matcher m;
					double[] loc;

						m=ploc.matcher(ts);
						if(!m.find()) throw new IllegalArgumentException("illegal sensor location: "+ts);
						loc=new double[3];
						loc[0]=Double.parseDouble(m.group(1));
						loc[1]=Double.parseDouble(m.group(2));
						loc[2]=Double.parseDouble(m.group(3));
						sensorlocl.add(loc);					
					}
					//add filter
					else if(pfilters)
					{
					String[] sf;
					double[] f;
					
						sf=ts.split("\\s");
						f=new double[sf.length];
						for(int i=0;i<f.length;i++) f[i]=Double.parseDouble(sf[i]);
						filterl.add(f);
					}
				}
			}

			//get locations and filters
			{
			int idx;
			
				sourceloc=new double[sourcelocl.size()][];
				idx=0;
				for(double[] d:sourcelocl) sourceloc[idx++]=d;
				
				sensorloc=new double[sensorlocl.size()][];
				idx=0;
				for(double[] d:sensorlocl) sensorloc[idx++]=d;
				
				tdmixf=new double[sensorloc.length][sourceloc.length][];
				idx=0;
				for(double[] d:filterl) 
				{
					tdmixf[idx/sourceloc.length][idx%sourceloc.length]=d;
					idx++;
				}
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
	
	/**
	 * get the number of sources
	 * @return
	 */
	public int numSources()
	{
		return sourceloc.length;
	}
	
	/**
	 * get the number of sensors
	 * @return
	 */
	public int numSensors()
	{
		return sensorloc.length;
	}
	
	/**
	 * get FIR filter length
	 * @return
	 */
	public int filterLength()
	{
		return tdmixf[0][0].length;
	}
	
	/**
	 * get time domain mixing filters, data are not copied
	 * @return
	 * [sensor index][source index][filter tap index]
	 */
	public double[][][] tdFilters()
	{
		return tdmixf;
	}
	
	/**
	 * save the virtual room into file
	 * @param path
	 * destination file path
	 * @throws IOException
	 */
	public void save(File path) throws IOException
	{
	BufferedWriter out=null;
	
		try
		{
			out=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path)));
			
			out.write(C+" "+c+"\n");
			out.write(FS+" "+fs+"\n");
			out.write(ROOMSIZE+" "+roomsize[0]+" x "+roomsize[1]+" x "+roomsize[2]+"\n");
			out.write(RT60+" "+rt60+"\n");
			
			out.write(SOURCELOCS+"\n");
			for(int sourcei=0;sourcei<sourceloc.length;sourcei++) 
				out.write(sourceloc[sourcei][0]+" "+sourceloc[sourcei][1]+" "+sourceloc[sourcei][2]+"\n");
				
			out.write(SENSORLOCS+"\n");
			for(int sensorj=0;sensorj<sensorloc.length;sensorj++) 
				out.write(sensorloc[sensorj][0]+" "+sensorloc[sensorj][1]+" "+sensorloc[sensorj][2]+"\n");

			out.write(FILTERS+"\n");
			for(int sensorj=0;sensorj<tdmixf.length;sensorj++) 
				for(int sourcei=0;sourcei<tdmixf[sensorj].length;sourcei++) 
					for(int taps=0;taps<tdmixf[sensorj][sourcei].length;taps++) 
					{
						out.write(Double.toString(tdmixf[sensorj][sourcei][taps]));
						if(taps<tdmixf[sensorj][sourcei].length-1) out.write(" ");
						else out.write("\n");
					}
			
			out.flush();
		}
		finally
		{
			try
			{
				if(out!=null) out.close();
			}
			catch(IOException e)
			{}
		}
	}
	
	public String toString()
	{
	StringBuilder s;
	
		s=new StringBuilder();
		
		s.append(C+" "+c+"\n");
		s.append(FS+" "+fs+"\n");
		s.append(ROOMSIZE+" "+roomsize[0]+" x "+roomsize[1]+" x "+roomsize[2]+"\n");
		s.append(RT60+" "+rt60+"\n");
		
		s.append(SOURCELOCS+"\n");
		for(int sourcei=0;sourcei<sourceloc.length;sourcei++) 
			s.append(sourceloc[sourcei][0]+" "+sourceloc[sourcei][1]+" "+sourceloc[sourcei][2]+"\n");
			
		s.append(SENSORLOCS+"\n");
		for(int sensorj=0;sensorj<sensorloc.length;sensorj++) 
			s.append(sensorloc[sensorj][0]+" "+sensorloc[sensorj][1]+" "+sensorloc[sensorj][2]+"\n");
		
		return s.toString();
	}
	
	public static void main(String[] args) throws IOException
	{
	RIRGenerator rirg;
	VirtualRoom model,model2;
	double[][] sourceloc={{0,0,0},{0,0,0},{0,0,0}};
	double[][] sensorloc={{1,1,1},{1,1,1},{1,1,1},{1,1,1}};
	
		rirg=new RIRGenerator(340,8000,new double[] {3.55,4.45,2.5},0.13);
		
		model=rirg.generateVirtualRoom(sourceloc,sensorloc,2048);
		model.save(new File("/home/nay0648/testroom.txt"));
		
		model2=new VirtualRoom(new File("/home/nay0648/testroom.txt"));
		model2.save(new File("/home/nay0648/testroom2.txt"));
		System.out.println(model2);
	}
}
