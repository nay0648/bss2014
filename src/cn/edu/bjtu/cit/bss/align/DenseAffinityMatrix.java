package cn.edu.bjtu.cit.bss.align;
import pp.util.BLAS;

/**
 * <h1>Description</h1>
 * A dense affinity matrix implementation.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Dec 31, 2011 10:16:43 AM, revision:
 */
public class DenseAffinityMatrix extends AffinityMatrix
{
private static final long serialVersionUID=764112753654530711L;
private double[][] data;//data array
private int nonzero=0;//number of nonzero entries

	/**
	 * @param size
	 * affinity matrix size
	 */
	public DenseAffinityMatrix(int size)
	{
		data=new double[size][size];
	}
	
	/**
	 * @param data
	 * affinity matrix data
	 */
	public DenseAffinityMatrix(double[][] data)
	{
		if(data.length!=data[0].length) throw new IllegalArgumentException(
				"square matrix required");
		
		this.data=new double[data.length][data[0].length];
		for(int i=0;i<data.length;i++) 
			for(int j=0;j<data[i].length;j++) setAffinity(i,j,data[i][j]);
	}
	
	/**
	 * make a copy
	 * @param another
	 * another affinity matrix
	 */
	public DenseAffinityMatrix(AffinityMatrix another)
	{
		this(another.size());
		for(Entry e:another) setAffinity(e,e.value());
	}
	
	public int size()
	{
		return data.length;
	}

	public int numNonzeroEntries()
	{
		return nonzero;
	}

	public double getAffinity(int idx1,int idx2)
	{
		return data[idx1][idx2];
	}

	public void setAffinity(int idx1,int idx2,double value)
	{
		if(data[idx1][idx2]==0&&value!=0) nonzero++;
		else if(data[idx1][idx2]!=0&&value==0) nonzero--;
		
		data[idx1][idx2]=value;
	}

	public RowIterator rowIterator(int idx1)
	{
		return new DenseRowIterator(idx1);
	}

	public ColumnIterator columnIterator(int idx2)
	{
		return new DenseColumnIterator(idx2);
	}
	
	/**
	 * <h1>Description</h1>
	 * Used to traverse a row.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Dec 31, 2011 3:27:34 PM, revision:
	 */
	private class DenseRowIterator extends AffinityMatrix.RowIterator
	{
	private int idx1,idx2=0;
		
		/**
		 * @param idx1
		 * row index
		 */
		public DenseRowIterator(int idx1)
		{
			this.idx1=idx1;
		}

		public boolean hasNext()
		{
			return idx2<data[idx1].length;
		}

		public Entry next()
		{
			return new Entry(idx1,idx2++);
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
	 * @version created on: Dec 31, 2011 3:31:16 PM, revision:
	 */
	private class DenseColumnIterator extends ColumnIterator
	{
	private int idx1=0,idx2;
	
		/**
		 * @param idx2
		 * column index
		 */
		public DenseColumnIterator(int idx2)
		{
			this.idx2=idx2;
		}
	
		public boolean hasNext()
		{
			return idx1<data.length;
		}

		public Entry next()
		{
			return new Entry(idx1++,idx2);
		}		
	}
	
	public static void main(String[] args)
	{
	double[][] data;
	AffinityMatrix am;
	
		data=BLAS.randMatrix(3,3);
		am=new DenseAffinityMatrix(data);
		am.setAffinity(0,0,0);
		am.setAffinity(0,2,0);
		am.setAffinity(1,2,0);
		am.setAffinity(2,0,0);
		System.out.println("nonzero entries: "+am.numNonzeroEntries());

		for(Entry e:am) System.out.println(e);
		
		System.out.println();
		for(Entry e:am.columnMajorIterator()) System.out.println(e);
	}
}
