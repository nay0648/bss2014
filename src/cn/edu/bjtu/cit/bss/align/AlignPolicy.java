package cn.edu.bjtu.cit.bss.align;
import java.awt.Point;
import java.io.*;
import java.util.*;
import org.apache.commons.math.complex.*;
import cn.edu.bjtu.cit.bss.*;

/**
 * <h1>Description</h1>
 * Used to solve the permutation ambiguity in frequency domain blind source separation.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Apr 6, 2011 8:38:08 PM, revision:
 */
public abstract class AlignPolicy implements Serializable
{
private static final long serialVersionUID=7982808318114397778L;
private FDBSSAlgorithm fdbss;//the bss algorithm reference

	public AlignPolicy()
	{}
	
	public FDBSSAlgorithm getFDBSSAlgorithm()
	{
		return fdbss;
	}
	
	public void setFDBSSAlgorithm(FDBSSAlgorithm fdbss)
	{
		this.fdbss=fdbss;
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
		return fdbss.numSensors();
	}
	
	/**
	 * get fft block size
	 * @return
	 */
	public int fftSize()
	{
		return fdbss.fftSize();
	}
	
	/**
	 * generate the indices permutation of P(n,n)
	 * @param n
	 * number of elements
	 * @return
	 */
	public static List<int[]> indexPermutation(int n)
	{
	List<int[]> p;
	int[] temp;
	
		if(n<=0) throw new IllegalArgumentException("n must be a positive integer: "+n);
		p=new LinkedList<int[]>();
		if(n==1) p.add(new int[]{0});
		else
		{
			for(int[] temp0:indexPermutation(n-1))
				//add new number into previous permutations
				for(int i=0;i<temp0.length+1;i++)
				{
					temp=new int[temp0.length+1];
					if(i>0) System.arraycopy(temp0,0,temp,0,i);
					temp[i]=n-1;
					if(i<temp.length-1) System.arraycopy(temp0,i,temp,i+1,temp0.length-i);
					p.add(temp);
				}
		}	
		return p;
	}
	
	/**
	 * throw exceptions if the demixing model is not compatible with the fdbss algorithm
	 * @param model
	 */
	public void checkDemixingModel(DemixingModel model)
	{
		if(model.numSources()!=numSources()) throw new IllegalArgumentException(
				"number of sources not match: "+model.numSources()+", "+numSources());
		
		if(model.numSensors()!=numSensors()) throw new IllegalArgumentException(
				"number of sensors not match: "+model.numSensors()+", "+numSensors());
		
		if(model.fftSize()!=fftSize()) throw new IllegalArgumentException(
				"fft block size not match: "+model.fftSize()+", "+fftSize());
	}
	
	/**
	 * throw exceptions if frequency subbands size out of bounds
	 * @param offset
	 * frequency subbands offset
	 * @param len
	 * subbands length
	 */
	public void checkSubbandSize(int offset,int len)
	{
		if(offset<0||offset>=fftSize()/2+1) throw new IndexOutOfBoundsException(
				"subbands offset out of bounds: "+offset+", "+(fftSize()/2+1));
		
		if(len<1||offset+len>fftSize()/2+1) throw new IndexOutOfBoundsException(
				"subbands length out of bounds: "+offset+", "+len+", "+(fftSize()/2+1));
	}
	
	/**
	 * throw exceptions if frequency bin coordinate out of bounds
	 * @param chidx
	 * output channel index
	 * @param binidx
	 * frequency bin index
	 */
	public void checkBinCoordinate(int chidx,int binidx)
	{
		if(chidx<0||chidx>=numSources()) throw new IndexOutOfBoundsException(
				"channel index out of bounds: "+chidx+", "+numSources());
		
		if(binidx<0||binidx>=fftSize()/2+1) throw new IndexOutOfBoundsException(
				"frequency bin index out of bounds: "+binidx+", "+(fftSize()/2+1));
	}
	
	/**
	 * convert (channel index, frequency bin index) to sample index for cluster algorithm 
	 * in a specified subbands
	 * @param chidx
	 * channel index
	 * @param binidx
	 * frequency bin index
	 * @param offset
	 * subbands offset
	 * @param len
	 * subbands length
	 * @return
	 */
	public int b2i(int chidx,int binidx,int offset,int len)
	{
		checkBinCoordinate(chidx,binidx);
		checkSubbandSize(offset,len);
		
		if(binidx<offset||binidx>=offset+len) throw new IndexOutOfBoundsException(
				"frequency bin index out of subbands boundary: "+binidx+", ["+offset+", "+(offset+len-1)+"]");
		
		return chidx*len+binidx-offset;
	}
	
	/**
	 * convert (channel index, frequency bin index) to sample index for cluster algorithm
	 * @param chidx
	 * channel index
	 * @param binidx
	 * frequency bin index
	 * @return
	 */
	public int b2i(int chidx,int binidx)
	{
		return b2i(chidx,binidx,0,fftSize()/2+1);
	}
	
	/**
	 * convert sample index to frequency bin index in a specified subbands
	 * @param idx
	 * sample index
	 * @param offset
	 * subbands offset
	 * @param len
	 * subbands length
	 * @return
	 */
	public int binIndex(int idx,int offset,int len)
	{
		checkSubbandSize(offset,len);
		
		if(idx<0||idx>=(numSources()*len)) throw new IndexOutOfBoundsException(
				"sample index out of bounds: "+(numSources()*len));
		
		return idx%len+offset;
	}
	
	/**
	 * convert sample index to frequency bin index
	 * @param idx
	 * sample index
	 * @return
	 */
	public int binIndex(int idx)
	{
		return binIndex(idx,0,fftSize()/2+1);
	}
	
	/**
	 * convert sample index to output channel index in a specified subbands
	 * @param idx
	 * sample index
	 * @param offset
	 * subbands offset
	 * @param len
	 * subbands length
	 * @return
	 */
	public int channelIndex(int idx,int offset,int len)
	{
		checkSubbandSize(offset,len);
		
		if(idx<0||idx>=(numSources()*len)) throw new IndexOutOfBoundsException(
				"sample index out of bounds: "+(numSources()*len));
		
		return idx/len;
	}
	
	/**
	 * convert sample index to channel index
	 * @param idx
	 * sample index
	 * @return
	 */
	public int channelIndex(int idx)
	{
		return channelIndex(idx,0,fftSize()/2+1);
	}
	
	/**
	 * convert sample index to (channel index, frequency bin index) in a 
	 * specified frequency subbands
	 * @param idx
	 * sample index
	 * @param offset
	 * frequency subbands offset
	 * @param len
	 * frequency subbands length
	 * @return
	 */
	public Point i2b(int idx,int offset,int len)
	{
		checkSubbandSize(offset,len);
		
		if(idx<0||idx>=(numSources()*len)) throw new IndexOutOfBoundsException(
				"sample index out of bounds: "+(numSources()*len));
		
		return new Point(idx/len,idx%len+offset);
	}
	
	/**
	 * convert sample index to (channel index, frequency bin index)
	 * @param idx
	 * channel index
	 * @return
	 */
	public Point i2b(int idx)
	{
		return i2b(idx,0,fftSize()/2+1);
	}
	
	/**
	 * Load cluster indicate vector from file, points belong to the same 
	 * cluster share the same label.
	 * @param path
	 * file path
	 * @return
	 * @throws IOException
	 */
	public static int[] loadIndicator(File path) throws IOException
	{
	BufferedReader in=null;
	String ts;
	String[] slabel;
	int[] label;
	
		try
		{
			in=new BufferedReader(new InputStreamReader(new FileInputStream(path)));
			for(ts=null;(ts=in.readLine())!=null;)
			{
				ts=ts.trim();
				if(ts!=null&&ts.length()!=0) break;
			}
			if(ts.charAt(0)=='['&&ts.charAt(ts.length()-1)==']')
				ts=ts.substring(1,ts.length()-1);
			/*
			 * convert to int
			 */
			slabel=ts.split("\\s*[,|\\s]\\s*");
			label=new int[slabel.length];
			for(int i=0;i<label.length;i++) label[i]=Integer.parseInt(slabel[i]);
			return label;
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
	 * convert 1D indicator to 2D indicator
	 * @param indicator
	 * permutation indicator
	 * @return
	 */
	public int[][] toIndicator2D(int[] indicator)
	{
	int[][] i2;
	int idx=0;
	
		if(indicator.length%numSources()!=0) throw new IllegalArgumentException(
				"illegal indicator length: "+indicator.length);
		
		i2=new int[numSources()][indicator.length/numSources()];
		for(int chidx=0;chidx<i2.length;chidx++) 
			for(int binidx=0;binidx<i2[chidx].length;binidx++) 
				i2[chidx][binidx]=indicator[idx++];
		
		return i2;
	}
	
	/**
	 * align the demixing model among frequency bins to overcome the permutation ambiguitiy
	 * @param model
	 * Demixing model for bin(0) ~ bin(fftsize/2+1)
	 */
	public abstract void align(DemixingModel model);
	
	/**
	 * 
	 * @param model
	 * the demixing model
	 * @param offset
	 * frequency bin offset
	 * @param len
	 * subband length
	 * @param indicator
	 * cluster indicator
	 */
	public void align(DemixingModel model,int offset,int len,int[][] indicator)
	{
	Complex[][] demix,temp;
		
		checkDemixingModel(model);
		checkSubbandSize(offset,len);
		if(indicator.length!=numSources()||indicator[0].length!=len) throw new IllegalArgumentException(
				"indicator size not match: "+indicator.length+" x "+indicator[0].length+
				", required: "+numSources()+" x "+len);
		
		temp=new Complex[numSources()][];
		for(int binidx=offset;binidx<offset+len;binidx++)
		{
			demix=model.getDemixingMatrix(binidx);
			
			/*
			 * change row orders, inverse permutation needed!!!
			 */
			for(int chidx=0;chidx<temp.length;chidx++) 
				temp[indicator[chidx][binidx-offset]]=demix[chidx];
			
			//copy back
			for(int chidx=0;chidx<demix.length;chidx++) demix[chidx]=temp[chidx];
		}
	}
	
	/**
	 * align the demixing model according to the indicator
	 * @param model
	 * the demixing model
	 * @param indicator
	 * a two dimension indicator, each row for a channel
	 */
	public void align(DemixingModel model,int[][] indicator)
	{
		align(model,0,fftSize()/2+1,indicator);
	}
	
	/**
	 * align the demixing model according to the indicator in a specified frequency bands
	 * @param model
	 * the demixing model
	 * @param offset
	 * frequency bin offset
	 * @param len
	 * subband length
	 * @param indicator
	 * cluster indicator
	 */
	public void align(DemixingModel model,int offset,int len,int[] indicator)
	{
		align(model,offset,len,toIndicator2D(indicator));
	}
	
	/**
	 * align the demixing model according to the indicator
	 * @param model
	 * the demixing model
	 * @param indicator
	 * indicator of the clustering result
	 */
	public void align(DemixingModel model,int[] indicator)
	{
		align(model,0,fftSize()/2+1,indicator);
	}
		
	/**
	 * rearrange rows of demixing matrices in a specified frequency subband 
	 * according to a specified permutation
	 * @param model
	 * demixing model
	 * @param offset
	 * frequency subband offset
	 * @param len
	 * frequency subband length
	 * @param p
	 * permutation
	 */
	public void rearrange(DemixingModel model,int offset,int len,int[] p)
	{
	Complex[][] temp,demixm;
	
		checkDemixingModel(model);
		checkSubbandSize(offset,len);
		if(p.length!=numSources()) throw new IllegalArgumentException(
				"permutation size not match the number of sources: "+p.length+", "+numSources());
		
		temp=new Complex[numSources()][];
		for(int binidx=offset;binidx<offset+len;binidx++)
		{
			demixm=model.getDemixingMatrix(binidx);
			
			//rearrange
			for(int chidx=0;chidx<temp.length;chidx++) temp[chidx]=demixm[p[chidx]];
			//copy back
			for(int chidx=0;chidx<demixm.length;chidx++) demixm[chidx]=temp[chidx];
		}
	}
	
	/**
	 * merge two already aligned subbands so that the second one has the same permutation 
	 * as the first one
	 * @param model
	 * the demixing model
	 * @param offset1
	 * offset for the lower frequency subband
	 * @param len1
	 * length for the lower frequency subband
	 * @param offset2
	 * offset for the higher frequency subband
	 * @param len2
	 * length for the higher frequency subband
	 */
	public void merge(DemixingModel model,int offset1,int len1,int offset2,int len2)
	{
	List<int[]> pl;//all possible permutations
	CommonFeature feature;
	double[][] powr1,powr2;
	int[] bestp=null;//the best permutation
	double dis,mindis=Double.MAX_VALUE;
	
		checkDemixingModel(model);
		checkSubbandSize(offset1,len1);
		checkSubbandSize(offset2,len2);
		if(offset1+len1-1>=offset2) throw new IllegalArgumentException(
				"overlap subbands: "+offset1+", "+len1+", "+offset2+", "+len2);
		
		//generate all permutations
		pl=indexPermutation(numSources());
		feature=new CommonFeature(getFDBSSAlgorithm());
		
		try
		{
			//power ratio of the last frequency bin of the first subband
			powr1=feature.powerRatio(model,offset1+len1-1);
			//power ratio of the first frequency bin of the second subband
			powr2=feature.powerRatio(model,offset2);
		}
		catch(IOException e)
		{
			throw new RuntimeException("failed to calculate power ratio",e);
		}
		
		//try all permutations
		for(int[] p:pl)
		{
			dis=0;
			
			for(int i=0;i<powr1.length;i++) 
				dis+=CommonFeature.distance(powr1[i],powr2[p[i]]);

			if(dis<mindis)
			{
				mindis=dis;
				bestp=p;
			}
		}

		//rearrange the second subband according to the best permutation
		rearrange(model,offset2,len2,bestp);
	}
}
