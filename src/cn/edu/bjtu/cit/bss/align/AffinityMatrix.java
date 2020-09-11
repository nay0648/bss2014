package cn.edu.bjtu.cit.bss.align;
import java.io.*;
import java.util.*;
import java.awt.image.*;
import org.apache.commons.math.linear.*;

/**
 * <h1>Description</h1>
 * Affinity matrix for spectral clustering.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Aug 25, 2011 10:11:00 PM, revision:
 */
public abstract class AffinityMatrix implements Serializable, Iterable<AffinityMatrix.Entry>
{
private static final long serialVersionUID=2011971348657677262L;

	/**
	 * <h1>Description</h1>
	 * Represents an entry in affinity matrix.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Jul 3, 2010 9:15:11 AM, revision:
	 */
	public class Entry implements Serializable
	{
	private static final long serialVersionUID=-3609738819927519634L;
	private int idx1,idx2;//row and column index

		/**
		 * @param idx1
		 * row index
		 * @param idx2
		 * column index
		 */
		public Entry(int idx1,int idx2)
		{
			this.idx1=idx1;
			this.idx2=idx2;
		}
	
		/**
		 * get row index in affinity matrix
		 * @return
		 */
		public int rowIndex()
		{
			return idx1;
		}
	
		/**
		 * get column index in affinity matrix
		 * @return
		 */
		public int columnIndex()
		{
			return idx2;
		}
	
		/**
		 * get entry value
		 * @return
		 */
		public double value()
		{
			return AffinityMatrix.this.getAffinity(idx1,idx2);
		}
	
		public String toString()
		{
			return "("+idx1+", "+idx2+"): "+value();
		}
	
		/**
		 * get the affinity matrix reference
		 * @return
		 */
		public AffinityMatrix affinityMatrix()
		{
			return AffinityMatrix.this;
		}
	
		public boolean equals(Object o)
		{
		Entry e2;
	
			if(o==null) return false;
			else if(this==o) return true;
			if(!(o instanceof Entry)) return false;
			e2=(Entry)o;
			if(!affinityMatrix().equals(e2.affinityMatrix())) return false;
			return idx1==e2.idx1&&idx2==e2.idx2;
		}
	
		public int hashCode()
		{
			return new Integer(idx1+idx2).hashCode();
		}
	}
	
	/**
	 * <h1>Description</h1>
	 * Used to traverse a row.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Dec 31, 2011 10:59:35 AM, revision:
	 */
	public abstract class RowIterator implements Iterable<Entry>, Iterator<Entry>
	{
		public void remove()
		{
			throw new UnsupportedOperationException(
					"can not remove matrix entry from iterator");
		}
		
		public Iterator<Entry> iterator()
		{
			return this;
		}
	}

	/**
	 * <h1>Description</h1>
	 * Used to traverse a column.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Dec 31, 2011 10:59:55 AM, revision:
	 */
	public abstract class ColumnIterator implements Iterable<Entry>, Iterator<Entry>
	{
		public void remove()
		{
			throw new UnsupportedOperationException(
					"can not remove matrix entry from iterator");
		}
		
		public Iterator<Entry> iterator()
		{
			return this;
		}
	}

	/**
	 * <h1>Description</h1>
	 * Used to traverse matrix according to row major order.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Aug 25, 2011 10:49:06 PM, revision:
	 */
	public class RowMajorIterator implements Iterable<Entry>, Iterator<Entry>
	{
	private int idx1=-1;
	private Iterator<Entry> rit=null;
	private Entry nexte;
	
		public RowMajorIterator()
		{
			nexte=findNextEntry();
		}
	
		public boolean hasNext()
		{
			return nexte!=null;
		}
		
		public Entry next()
		{
		Entry retval;
		
			retval=nexte;
			nexte=findNextEntry();
			return retval;
		}

		public void remove()
		{
			throw new UnsupportedOperationException(
					"can not remove matrix entry from iterator");
		}
		
		/**
		 * find next entry
		 * @return
		 * return null if no one left
		 */
		private Entry findNextEntry()
		{
			for(;;)
			{
				if(rit!=null&&rit.hasNext()) return rit.next();
				else
				{
					if(++idx1<AffinityMatrix.this.size()) rit=AffinityMatrix.this.rowIterator(idx1);
					else return null;
				}
			}
		}

		public Iterator<Entry> iterator()
		{
			return this;
		}
	}
	
	/**
	 * <h1>Description</h1>
	 * Used to traverse matrix according to column major order.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Aug 25, 2011 10:49:06 PM, revision:
	 */
	public class ColumnMajorIterator implements Iterable<Entry>, Iterator<Entry>
	{
	private int idx2=-1;
	private Iterator<Entry> cit=null;
	private Entry nexte;
	
		public ColumnMajorIterator()
		{
			nexte=findNextEntry();
		}
	
		public boolean hasNext()
		{
			return nexte!=null;
		}
		
		public Entry next()
		{
		Entry retval;
		
			retval=nexte;
			nexte=findNextEntry();
			return retval;
		}

		public void remove()
		{
			throw new UnsupportedOperationException(
					"can not remove matrix entry from iterator");
		}
		
		/**
		 * find next entry
		 * @return
		 * return null if no one left
		 */
		private Entry findNextEntry()
		{
			for(;;)
			{
				if(cit!=null&&cit.hasNext()) return cit.next();
				else
				{
					if(++idx2<AffinityMatrix.this.size()) cit=AffinityMatrix.this.columnIterator(idx2);
					else return null;
				}
			}
		}

		public Iterator<Entry> iterator()
		{
			return this;
		}
	}

	/**
	 * get matrix size
	 * @return
	 */
	public abstract int size();
	
	/**
	 * get the number of nonzero entries
	 * @return
	 */
	public abstract int numNonzeroEntries();
	
	/**
	 * get affinity at a specified entry
	 * @param idx1
	 * row index
	 * @param idx2
	 * column index
	 * @return
	 */
	public abstract double getAffinity(int idx1,int idx2);
	
	/**
	 * set affinity for a specified entry
	 * @param idx1
	 * row index
	 * @param idx2
	 * column index
	 * @param value
	 * entry value
	 */
	public abstract void setAffinity(int idx1,int idx2,double value);
	
	/**
	 * set affinity for a specified entry
	 * @param entry
	 * an entry
	 * @param value
	 * new value
	 */
	public void setAffinity(Entry entry,double value)
	{
		setAffinity(entry.rowIndex(),entry.columnIndex(),value);
	}
	
	/**
	 * used to traverse a row
	 * @param idx1
	 * row index
	 * @return
	 */
	public abstract RowIterator rowIterator(int idx1);

	/**
	 * used to traverse a column
	 * @param idx2
	 * column index
	 * @return
	 */
	public abstract ColumnIterator columnIterator(int idx2);
	
	/**
	 * used to traverse the affinity matrix according to row major order
	 * @return
	 */
	public RowMajorIterator rowMajorIterator()
	{
		return new RowMajorIterator();
	}
	
	/**
	 * used to traverse the affinity matrix according to column major order
	 * @return
	 */
	public ColumnMajorIterator columnMajorIterator()
	{
		return new ColumnMajorIterator();
	}
	
	public Iterator<Entry> iterator()
	{
		return rowMajorIterator();
	}
	
	/**
	 * convert the affinity matrix to image for visualization
	 * @param normalize
	 * true to normalize affinity values to [0, 1] before convert to image
	 * @return
	 */
	public BufferedImage toImage(boolean normalize)
	{
	double[][] data;
	double maxentry=Double.MIN_VALUE,minentry=Double.MAX_VALUE;
	BufferedImage vimg;
	int intensity,rgb;
		
		data=toMatrix();

		if(normalize)
		{
			//find the range boundary
			for(Entry e:this) 
			{
				if(e.value()>maxentry) maxentry=e.value();
				if(e.value()<minentry) minentry=e.value();
			}
				
			//normalize
			for(int i=0;i<data.length;i++) 
				for(int j=0;i<data[i].length;j++) 
					data[i][j]=(data[i][j]-minentry)/(maxentry-minentry);
		}
		
		vimg=new BufferedImage(size(),size(),BufferedImage.TYPE_INT_RGB);
		
		
		for(int y=0;y<data.length;y++) 
			for(int x=0;x<data[y].length;x++) 
			{
				intensity=(int)Math.round(data[y][x]*255);
				if(intensity<0) intensity=0;else if(intensity>255) intensity=255;
						
				rgb=intensity<<16|intensity<<8|intensity;
				vimg.setRGB(x,y,rgb);
			}
		
		return vimg;
	}
	
	/**
	 * Visualize the affinity matrix as an grayscale image, the lighter 
	 * pixel intensity the larger corresponding affinity value. The range 
	 * of affinity matrix entry value is regarded in [0, 1]. WARNING: the 
	 * visualized image is very large!
	 * @param normalize
	 * true to normalize entry values to [0, 1]
	 */
	public void visualize(boolean normalize)
	{
		pp.util.Util.showImage(toImage(normalize));
	}
	
	/**
	 * Convert the data to matrix format. This is used just for experiment, real 
	 * sparse affinity matrix is too large to be stored into double array.
	 * @return
	 */
	public double[][] toMatrix()
	{
	double[][] am;
	Entry e;
	
		am=new double[size()][size()];
		for(Iterator<Entry> it=rowMajorIterator();it.hasNext();)
		{
			e=it.next();
			am[e.idx1][e.idx2]=e.value();
		}
		return am;
	}
	
	/**
	 * convert affinity matrix to matlab's sparse matrix format
	 * @return
	 * The first row is the entry row index, the second row is 
	 * the entry column index, the third row is the corresponding 
	 * entry value.
	 */
	public double[][] toMatlabSparseMatrix()
	{
	double[][] sparse;
	int index=0;
	Entry e;
	
		sparse=new double[3][numNonzeroEntries()];
		for(Iterator<Entry> it=rowMajorIterator();it.hasNext();)
		{
			e=it.next();
			sparse[0][index]=e.rowIndex();
			sparse[1][index]=e.columnIndex();
			sparse[2][index]=e.value();
			index++;
		}		
		return sparse;
	}
	
	/**
	 * convert to apache commonos sparse matrix
	 * @return
	 */
	public RealMatrix toCommonosMatrix()
	{
	OpenMapRealMatrix cm;
	Entry e;
	
		cm=new OpenMapRealMatrix(size(),size());
		for(Iterator<Entry> it=rowMajorIterator();it.hasNext();) 
		{
			e=it.next();
			cm.setEntry(e.rowIndex(),e.columnIndex(),e.value());
		}
		return cm;
	}
}
