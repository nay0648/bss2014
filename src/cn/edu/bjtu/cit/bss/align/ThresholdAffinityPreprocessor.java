package cn.edu.bjtu.cit.bss.align;
import java.io.*;
import java.awt.image.*;
import java.util.*;
import cn.edu.bjtu.cit.bss.*;

/**
 * <h1>Description</h1>
 * Threshold the affinity matrix to make it more sparse.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Nov 18, 2011 4:05:31 PM, revision:
 */
public class ThresholdAffinityPreprocessor implements AffinityPreprocessor
{
private static final long serialVersionUID=-1487046616812377562L;

	public AffinityMatrix preprocess(AffinityMatrix am)
	{
	ArrayList<Double> val;
	int numcc,numcc2;
	int idxl,idxm,idxh;
	AffinityMatrix am2;
	
		//number of connected components of input affinity matrix
		numcc=numConnectedComponents(am);
		
		/*
		 * all possible thresholds in ascending order
		 */
		val=new ArrayList<Double>(am.numNonzeroEntries());
		for(AffinityMatrix.Entry entry:am) val.add(entry.value());
		Collections.sort(val);
		
		/*
		 * initialize half way search
		 */
		idxl=0;
		idxh=val.size()-1;
		idxm=(idxh+idxl)/2;

		for(;idxl<idxm;)
		{
			am2=prune(am,val.get(idxm));
			numcc2=numConnectedComponents(am2);

			//the threshold is too small
			if(numcc2==numcc)
				//still a legitimate solution
				idxl=idxm;
			//the threshold is too large
			else if(numcc2>numcc) idxh=idxm-1;
			
			idxm=(idxh+idxl)/2;
		}
		
		return am;
	}
	
	/**
	 * find the number of connected components of the graph represented by the 
	 * affinity matrix
	 * @param am
	 * an affinity matrix
	 * @return
	 */
	private int numConnectedComponents(AffinityMatrix am)
	{
	boolean[] visited;
	List<Integer> nodel;
	int n,numcc=0;
	
		/*
		 * initialize
		 */
		visited=new boolean[am.size()];
		nodel=new LinkedList<Integer>();
		
		for(int i=0;i<visited.length;i++)
		{
			if(visited[i]) continue;//this node is visited
			
			/*
			 * the first node of a new connected component
			 */
			numcc++;
			visited[i]=true;

			//add its unvisited neighbors into list
			for(Iterator<AffinityMatrix.Entry> it=am.rowIterator(i);it.hasNext();) 
			{
				n=it.next().columnIndex();
				
				if(!visited[n])
				{
					visited[n]=true;
					nodel.add(n);
				}
			}
			
			//traverse all neighbor nodes
			for(;!nodel.isEmpty();)
				for(Iterator<AffinityMatrix.Entry> it=am.rowIterator(nodel.remove(0));it.hasNext();) 
				{
					n=it.next().columnIndex();
					
					if(!visited[n])
					{
						visited[n]=true;
						nodel.add(n);
					}
				}
		}
	
		return numcc;
	}
	
	/**
	 * prune the affinity matrix
	 * @param am
	 * an affinity matrix
	 * @param th
	 * entries smaller than this threshold will be removed
	 * @return
	 */
	private AffinityMatrix prune(AffinityMatrix am,double th)
	{
	AffinityMatrix am2;
	
		am2=new DenseAffinityMatrix(am.size());
		for(AffinityMatrix.Entry entry:am) 
			if(entry.value()>=th) 
				am2.setAffinity(entry.rowIndex(),entry.columnIndex(),entry.value());
		
		return am2;
	}
	
	public static void main(String[] args) throws IOException
	{
	int offset=0,len=100;	
	
	AffinityMatrixBuilder ambuilder;
	DemixingModel demixm;
	ThresholdAffinityPreprocessor th;
	AffinityMatrix am,am2;
	BufferedImage img,img2;
			
		ambuilder=new DefaultAffinityMatrixBuilder(new FDBSS(new File("temp")),20);
		demixm=ambuilder.fdbssAlgorithm().loadDemixingModel();
		th=new ThresholdAffinityPreprocessor();

		am=ambuilder.buildAffinityMatrix(demixm,offset,len);
		img=am.toImage(false);

		am2=th.preprocess(am);
		img2=am2.toImage(false);
			
		pp.util.Util.showImage(pp.util.Util.drawResult(1,2,5,img,img2));
	}
}
