package cn.edu.bjtu.cit.bss.align;
import java.io.*;
import java.util.*;
import java.awt.image.*;
import cn.edu.bjtu.cit.bss.*;

/**
 * <h1>Description</h1>
 * Each node only has connections with its nearest neighbor in each frequency band.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Nov 23, 2011 8:56:25 AM, revision:
 */
public class SingleLinkagePreprocessor implements AffinityPreprocessor
{
private static final long serialVersionUID=-2896322401623371442L;
private int numchout;//number of output channels

	/**
	 * @param numchout
	 * number of output channels
	 */
	public SingleLinkagePreprocessor(int numchout)
	{
		this.numchout=numchout;
	}

	public AffinityMatrix preprocess(AffinityMatrix am)
	{
	AffinityMatrix am2;
	Map<Integer,AffinityMatrix.Entry> nbmap;
	int fblen,binidx;
	AffinityMatrix.Entry entry,nb;
	
		am2=new DenseAffinityMatrix(am.size());
		nbmap=new HashMap<Integer,AffinityMatrix.Entry>();
		fblen=am.size()/numchout;//frequency band length
		
		//traverse all nodes
		for(int i=0;i<am.size();i++)
		{
			nbmap.clear();
		
			//traverse all nodes connecting to this node
			for(Iterator<AffinityMatrix.Entry> it=am.rowIterator(i);it.hasNext();)
			{
				entry=it.next();
				binidx=entry.columnIndex()%fblen;//frequency bin index of this neighbor
				
				nb=nbmap.get(binidx);
				if(nb==null||nb.value()<entry.value()) nbmap.put(binidx,entry);
			}
			
			//set single linkage value
			for(AffinityMatrix.Entry nb2:nbmap.values()) 
			{
				am2.setAffinity(nb2.rowIndex(),nb2.columnIndex(),nb2.value());
				am2.setAffinity(nb2.columnIndex(),nb2.rowIndex(),nb2.value());//symmetric
			}
		}

		return am2;
	}
	
	public static void main(String[] args) throws IOException
	{
	AffinityMatrixBuilder ambuilder;
	DemixingModel demixm;
	AffinityMatrix am,am2;
	BufferedImage img,img2;
	
		ambuilder=new DefaultAffinityMatrixBuilder(new FDBSS(new File("temp")),20);
		demixm=ambuilder.fdbssAlgorithm().loadDemixingModel();
		
		am=ambuilder.buildAffinityMatrix(demixm,0,200);
		img=am.toImage(false);
	
		am2=(new SingleLinkagePreprocessor(ambuilder.fdbssAlgorithm().numSources())).preprocess(am);
		img2=am2.toImage(false);
	
		pp.util.Util.showImage(pp.util.Util.drawResult(1,2,5,img,img2));
	}
}
