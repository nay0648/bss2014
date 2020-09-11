package cn.edu.bjtu.cit.bss.align;
import java.io.*;
import java.util.*;
import java.awt.image.*;
import org.apache.commons.math.linear.*;
import org.apache.commons.math.util.*;
import cn.edu.bjtu.cit.bss.*;
import pp.util.BLAS;

/**
 * <h1>Description</h1>
 * Align frequency bins by NJW spectral clustering using power ratio of the 
 * separated data as the similarity criteria.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Aug 25, 2011 9:14:36 PM, revision:
 */
public class NJWAlign extends AlignPolicy
{
private static final long serialVersionUID=1759922667497454504L;
private double kmeansepsilon=1e-6;//threshold when k-means terminates
private int maxiteration=500;//max iteration times of k-means
private double orthogonalth=Math.cos(60*Math.PI/180.0);//used to select initial centers
private double eps=1e-10;//absolute value smaller than this threshold will be regarded as zero
private int neighborhood=20;//neighbor bins in affinity matrix construction
private AffinityMatrixBuilder ambuilder=null;//used to construct affinity matrix
private int numband=15;//number of frequency band
//private boolean debug=false;//true to draw mapped dataset

	/**
	 * get affinity matrix builder
	 * @return
	 */
	public AffinityMatrixBuilder affinityMatrixBuilder()
	{
		if(ambuilder==null) 
			ambuilder=new DefaultAffinityMatrixBuilder(
					(FDBSS)this.getFDBSSAlgorithm(),neighborhood);
		return ambuilder;
	}
	
	public void align(DemixingModel demixm)
	{
	int total,deltalen,remains,len,poffset=0,plen=0;
	AffinityMatrix am=null;
	AffinityPreprocessor ampreprocessor;
	int[] indicator;
	
		total=this.fftSize()/2+1;//total number of frequency bins
		deltalen=(int)Math.ceil(total/numband);//expected length of each alignment
		ampreprocessor=new SingleLinkagePreprocessor(this.numSources());
		
		for(int offset=0;offset<total;offset+=len)
		{				
			remains=total-(offset+deltalen);//number of frequency bins not algined
			if(remains<=deltalen/2) len=deltalen+remains;
			else len=deltalen;

			//build affinity matrix for a frequency band
			am=affinityMatrixBuilder().buildAffinityMatrix(demixm,offset,len);
			//apply single linkage preprocessor
			am=ampreprocessor.preprocess(am);
			//invocate njw
			indicator=njw(am,this.numSources());
			

			
//			System.out.println("offset: "+offset);
//			System.out.println(BLAS.toString(this.toIndicator2D(indicator)));
			
			
			
			//align current frequency band
			this.align(demixm,offset,len,indicator);

			/*
			 * merge with the revious subband
			 */
			if(plen!=0) this.merge(demixm,poffset,plen,offset,len);
			poffset=offset;
			plen=len;
		}
	}

	/**
	 * NJW algorithm
	 * @param am
	 * affinity matrix
	 * @param k
	 * number of clusters
	 * @return
	 * cluster indicator
	 */
	public int[] njw(AffinityMatrix am,int k)
	{
	EigenDecompositionImpl eigen;

		//build the normalized Laplacian matrix
		{
		double[] d;
		double temp;
		
			/*
			 * calculate the diagonal matrix D
			 */
			d=new double[am.size()];
			for(AffinityMatrix.Entry entry:am) d[entry.rowIndex()]+=entry.value();

			//calculate D^(-1/2)	
			for(int i=0;i<d.length;i++) d[i]=1.0/Math.sqrt(d[i]);
			
			//calculate L=D^(-1/2)*S*D^(-1/2) into similarity matrix
			for(AffinityMatrix.Entry entry:am)
			{
				if(entry.rowIndex()>=entry.columnIndex()) continue;//symmetric, diagonal is 0
				temp=d[entry.rowIndex()]*entry.value()*d[entry.columnIndex()];
				am.setAffinity(entry.rowIndex(),entry.columnIndex(),temp);
				am.setAffinity(entry.columnIndex(),entry.rowIndex(),temp);
			}
			
			//calculate the eigen decomposition
			eigen=new EigenDecompositionImpl(am.toCommonosMatrix(),MathUtils.SAFE_MIN);
		}

		//perform k-means clustering on new dataset
		{
		List<double[]> x;//the mapped dataset
		List<double[]> init;
		int[] indicator;
		double tolerance;

			indicator=new int[am.size()];
			x=buildMappedDataset(eigen,k);

			//draw mapped dataset
//			if(debug&&(x.get(0).length==2||x.get(0).length==3)) 
//				pp.segmentation.Experiment.drawDatasetWithOrigin(x);

			init=chooseInitialClusterCenter(x,k);
			tolerance=kmeans(x,init,indicator);
			
//			pp.segmentation.Experiment.drawResult(x,indicator);

			if(tolerance<0) return null;else return indicator;
		}
	}

	/**
	 * build mapped dataset from eigen decomposition
	 * @param eigen
	 * the eigen decomposition of Laplacian matrix from original dataset
	 * @param k
	 * number of clusters prefered
	 * @return
	 */
	private List<double[]> buildMappedDataset(EigenDecomposition eigen,int k)
	{
	int numsamples;
	List<double[]> dataset;
	RealVector ev;
	
		/*
		 * build empty dataset
		 */
		numsamples=eigen.getEigenvector(0).getDimension();
		if(k>numsamples) throw new IllegalArgumentException(
				"too many clusters: "+k+", should smaller than or equal to: "+numsamples);
		dataset=new ArrayList<double[]>(numsamples);
		for(int i=0;i<numsamples;i++) dataset.add(new double[k]);
		//copy data
		for(int j=0;j<k;j++)
		{
			/*
			 * Get the top-k largest eigenvalue corresponding eigenvector, 
			 * the eigenvalues are already sorted according to decendent order.
			 */
			ev=eigen.getEigenvector(j);
			for(int i=0;i<numsamples;i++) dataset.get(i)[j]=ev.getEntry(i);
		}
		//normalize to unit vector
		for(int i=0;i<dataset.size();i++) BLAS.normalize(dataset.get(i));
		return dataset;
	}
	
	/**
	 * Choose initial cluster center from mapped dataset. It is based on the 
	 * prior information that samples belong to different clusters are almost 
	 * orthogonal to each other.
	 * @param dataset
	 * the mapped dataset
	 * @param k
	 * number of clusters
	 * @return
	 */
	private List<double[]> chooseInitialClusterCenter(List<double[]> dataset,int k)
	{
	List<double[]> centers;
	int dimension;
	double[] sample,center,temp;
	
		centers=new ArrayList<double[]>(k);
		dimension=dataset.get(0).length;//sample dimension
		/*
		 * use the first sample as a center
		 */
		temp=new double[dimension];
		System.arraycopy(dataset.get(0),0,temp,0,temp.length);
		centers.add(temp);
		//find point almost orthogonal to already found points
next:	for(int i=1;i<dataset.size();i++)
		{
			sample=dataset.get(i);//get a sample
			for(int ii=0;ii<centers.size();ii++)
			{
				center=centers.get(ii);//get a center
				//current does not suitable as initial points, use orthogonalth as threshold
				if(Math.abs(BLAS.innerProduct(sample,center))>orthogonalth) continue next;
			}
			/*
			 * found one
			 */
			temp=new double[dimension];
			System.arraycopy(sample,0,temp,0,temp.length);
			centers.add(temp);
			if(centers.size()>=k) break;
		}
		return centers;
	}
	
	/**
	 * perform the k-means clustering
	 * @param dataset
	 * The dataset, each row is a sample.
	 * @param kinit
	 * K initial samples, each row is a initial sample. Cluster centers 
	 * will also be returned from here.
	 * @param indicator
	 * Clustering result, indicator index starts from 0.
	 * @return
	 * The tolerance, i.e. average distance from each sample to corresponding 
	 * clustering center, then average again for each cluster. Return -1 if 
	 * failed to cluster.
	 */
	public double kmeans(List<double[]> dataset,List<double[]> kinit,int[] indicator)
	{
	double d,mind,var;
	int[] count;//number of samples belong to each cluster
	double[] tolerance;//average distance from each sample to its center for every center
	double[][] kcenter2;
	
		/*
		 * check parameters
		 */
		if(dataset.get(0).length!=kinit.get(0).length) throw new IllegalArgumentException(
				"sample dimension not compatible: "+dataset.get(0).length+", "+kinit.get(0).length);
		if(dataset.size()!=indicator.length) throw new IllegalArgumentException(
				"incompatible indicator length: "+indicator.length+", required: "+dataset.size());
		/*
		 * perform the clustering
		 */
		count=new int[kinit.size()];
		tolerance=new double[kinit.size()];
		//cluster centers, each row is a center
		kcenter2=new double[kinit.size()][kinit.get(0).length];
		for(int itcount=0;itcount<maxiteration;itcount++)
		{
			Arrays.fill(count,0);//clear cluster count
			Arrays.fill(tolerance,0);//clear tolerance
			BLAS.fill(kcenter2,0);//clear new cluster center
			//traverse dataset
			for(int i=0;i<dataset.size();i++)
			{
				/*
				 * assign samples to clusters
				 */
				mind=Double.MAX_VALUE;
				//calculate distance from a sample to all cluster centers
				for(int ii=0;ii<kinit.size();ii++)
				{
					//distance from a sample to a cluster center
					d=CommonFeature.distance(dataset.get(i),kinit.get(ii));
					//find a smaller distance
					if(d<mind)
					{
						mind=d;
						indicator[i]=ii;
					}
				}
				/*
				 * here indicator[i] is the sample i's cluster label
				 */
				count[indicator[i]]++;//count the sample belongs to the cluster
				tolerance[indicator[i]]+=mind;//accumulate tolerance of a cluster
				//accumulate the new cluster center
				BLAS.add(dataset.get(i),kcenter2[indicator[i]],kcenter2[indicator[i]]);
			}
			//average tolerance for each cluster
			for(int i=0;i<tolerance.length;i++) tolerance[i]/=count[i];
			//calculate new cluster centers
			for(int i=0;i<kcenter2.length;i++) 
				BLAS.scalarMultiply(1.0/count[i],kcenter2[i],kcenter2[i]);
			/*
			 * calculate average cluster centers displacement
			 */
			var=0;
			for(int i=0;i<kinit.size();i++) var+=CommonFeature.distance(kinit.get(i),kcenter2[i]);
			var/=kinit.size();
			//stop condition: cluster centers become stable
			if(var<=kmeansepsilon) return BLAS.mean(tolerance);
			//copy cluster centers and continue
			else for(int i=0;i<kinit.size();i++) 
				System.arraycopy(kcenter2[i],0,kinit.get(i),0,kcenter2[i].length);
		}
		throw new AlgorithmNotConvergeException("max iteration times of k-means: "+maxiteration);
	}
	
	/**
	 * test njw algorithm on artificial dataset
	 * @throws IOException
	 */
	public void testNjw() throws IOException
	{
	double[][] dataset;
//	int k;
	//Scaling parameter, the smaller sigma, the smaller the similarity.
	double sigma=0.03;
	AffinityMatrix am;
	
		dataset=BLAS.loadDoubleMatrix(new File("/home/nay0648/data/research/paper/segmentation/dataset/a.txt"));
//		k=3;
		
		//build affinity matrix
		{
		double[] sample1,sample2;
		double temp,d2,sigma2,similarity;

			am=new DenseAffinityMatrix(dataset.length);
			sigma2=sigma*sigma;
			
			for(int i=0;i<dataset.length;i++)
				//symmetric
				for(int j=i+1;j<dataset.length;j++)
				{
					/*
					 * calculate the square of the diatance
					 */
					sample1=dataset[i];
					sample2=dataset[j];
					d2=0;
					for(int ii=0;ii<sample1.length;ii++)
					{
						temp=sample2[ii]-sample1[ii];
						d2+=temp*temp;
					}
					
					/*
					 * calculate and set similarity
					 */
					similarity=Math.exp(-d2/sigma2);
					if(Math.abs(similarity)>=eps)
					{
						am.setAffinity(i,j,similarity);
						am.setAffinity(j,i,similarity);
					}
				}
		}
		
//		for(Iterator<AffinityMatrix.Entry> it=am.rowMajorIterator();it.hasNext();) 
//			System.out.println(it.next());
		
		//apply njw and show result
		{
//		int[] indicator;
		List<double[]> dataset2;
		
//			indicator=njw(am,k);
//			indicator=njwMatlab(am,k);
			
			dataset2=new ArrayList<double[]>(dataset.length);
			for(double[] s:dataset) dataset2.add(s);
//			pp.segmentation.Experiment.drawResult(dataset2,indicator);
		}
	}
	
	/**
	 * build toy dataset for test
	 * @param numch
	 * number of output channels
	 * @param numbin
	 * number of frequency bins
	 * @param path
	 * output file path
	 * @throws IOException
	 */
	public void buildToyDataset(int numch,int numbin,File path) throws IOException
	{
	String[] mark={"*","+","x","o","#"};
	String[][] dataset;
	List<String> marks;
	int idx;
	BufferedWriter out=null;
	
		if(numch>=mark.length) throw new IllegalArgumentException(
				"too many output channels: "+numch+", "+mark.length);
	
		dataset=new String[numch][numbin];
		marks=new LinkedList<String>();
		for(int j=0;j<dataset[0].length;j++)
		{
			marks.clear();
			for(int i=0;i<numch;i++) marks.add(mark[i]);
		
			for(int i=0;i<numch;i++)
			{
				idx=(int)(Math.random()*marks.size());
				dataset[i][j]=marks.get(idx);
				marks.remove(idx);
			}
		}

		try
		{
			out=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path)));
			
			for(int i=0;i<dataset.length;i++) 
			{
				for(int j=0;j<dataset[i].length;j++) out.write(dataset[i][j]+" ");
				out.write("\n");
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
	
	/**
	 * build affinity matrix from toy data
	 * @param path
	 * toy data path
	 * @return
	 * @throws IOException
	 */
	public AffinityMatrix buildToyAffinityMatrix(File path) throws IOException
	{
	BufferedReader in=null;
	List<String[]> dataset;	
	AffinityMatrix am;
	String s1,s2;
	double sim;
	int idx1,idx2;
	
		/*
		 * load dataset
		 */
		try
		{
			in=new BufferedReader(new InputStreamReader(new FileInputStream(path)));
			dataset=new LinkedList<String[]>();
			
			for(String ts=null;(ts=in.readLine())!=null;) dataset.add(ts.split("\\s"));
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
		
		/*
		 * build affinity matrix
		 */
		am=new DenseAffinityMatrix(dataset.size()*dataset.get(0).length);
		
		for(int binidx1=0;binidx1<dataset.get(0).length;binidx1++)
			for(int binidx2=binidx1+1;binidx2<dataset.get(0).length&&binidx2<=binidx1+neighborhood;binidx2++)
			{
				for(int i=0;i<dataset.size();i++)
					for(int j=0;j<dataset.size();j++)
					{
						s1=dataset.get(i)[binidx1];
						s2=dataset.get(j)[binidx2];
						
						if(s1.equals(s2)) sim=0.9;else sim=0.1;
						
						idx1=i*dataset.get(0).length+binidx1;
						idx2=j*dataset.get(0).length+binidx2;
						am.setAffinity(idx1,idx2,sim);
						am.setAffinity(idx2,idx1,sim);
					}
			}
		return am;
	}
	
	/**
	 * align toy dataset
	 * @param path
	 * toy dataset path
	 * @throws IOException
	 */
	public void toyAlign(File path) throws IOException
	{
	BufferedReader in=null;
	List<String[]> dataset;	
	AffinityMatrix am;
	int[] indicator;
	String[][] alignedds;
	int idx=0;
	
		/*
		 * load dataset
		 */
		try
		{
			in=new BufferedReader(new InputStreamReader(new FileInputStream(path)));
			dataset=new LinkedList<String[]>();
		
			for(String ts=null;(ts=in.readLine())!=null;) dataset.add(ts.split("\\s"));
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
	
		/*
		 * apply njw
		 */
		am=buildToyAffinityMatrix(path);
		indicator=njw(am,dataset.size());
		
		/*
		 * output data
		 */
		alignedds=new String[dataset.size()][dataset.get(0).length];
		for(int i=0;i<alignedds.length;i++) 
			for(int j=0;j<alignedds[i].length;j++) 
				alignedds[i][j]=dataset.get(indicator[idx++])[j];
		
		for(String[] c:alignedds) System.out.println(Arrays.toString(c));
	}
	
	public static void main(String[] args) throws IOException
	{
	int offset=100,len=100;
	
	NJWAlign foo;
	DemixingModel demixm;
	AffinityMatrixBuilder ambuilder;
	AffinityMatrix am,am2;
	int[] indicator;
	int[][] indicator2d;
	BufferedImage img,img2;
	
		foo=new NJWAlign();
		foo.setFDBSSAlgorithm(new FDBSS(new File("temp")));
//		foo.debug=true;
		demixm=((FDBSS)foo.getFDBSSAlgorithm()).loadDemixingModelNotAligned();

		ambuilder=foo.affinityMatrixBuilder();
		am=ambuilder.buildAffinityMatrix(demixm,offset,len);
		img=am.toImage(false);

		am=(new SingleLinkagePreprocessor(foo.numSources())).preprocess(am);
		indicator=foo.njw(am,foo.numSources());
		indicator2d=foo.toIndicator2D(indicator);
		System.out.println(BLAS.toString(indicator2d));

		am2=ambuilder.buildAffinityMatrix(demixm,offset,len,indicator2d);
		img2=am2.toImage(false);
		pp.util.Util.showImage(pp.util.Util.drawResult(1,2,5,img,img2));
	}
}
