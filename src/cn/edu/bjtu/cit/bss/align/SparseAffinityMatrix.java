package cn.edu.bjtu.cit.bss.align;
import java.io.*;
import java.util.*;
import pp.util.BLAS;

/**
 * <h1>Description</h1>
 * Affinity matrix for spectral clustering, the matrix is sparse, square, and 
 * should be symmetric.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Aug 25, 2011 10:11:00 PM, revision:
 */
public class SparseAffinityMatrix extends AffinityMatrix
{
private static final long serialVersionUID=-5376457181291585350L;
//row table, the first int is the row index, the second int is the column index
private ArrayList<SortedMap<Integer,Value>> rowtable;
//column table, the first int is the column index, the second int is the row index
private ArrayList<SortedMap<Integer,Value>> columntable;
private int size;//matrix size
private int numnonzero=0;//number of nonzero entries

	/**
	 * <h1>Description</h1>
	 * Matrix entry value.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Jul 6, 2010 3:13:35 PM, revision:
	 */
	private class Value implements Serializable
	{
	private static final long serialVersionUID=9155070891153549551L;
	double value;

		/**
		 * @param value
		 * entry value
		 */
		public Value(double value)
		{
			this.value=value;
		}
	}

	/**
	 * @param size
	 * matrix size
	 */
	public SparseAffinityMatrix(int size)
	{
		this.size=size;
		/*
		 * allocate new space
		 */
		rowtable=new ArrayList<SortedMap<Integer,Value>>(size);
		columntable=new ArrayList<SortedMap<Integer,Value>>(size);
		for(int i=0;i<size;i++)
		{
			rowtable.add(new TreeMap<Integer,Value>());
			columntable.add(new TreeMap<Integer,Value>());
		}
	}
	
	/**
	 * make a copy of another affinity matrix
	 * @param another
	 */
	public SparseAffinityMatrix(AffinityMatrix another)
	{
	this(another.size());
	Entry e;
	
		for(Iterator<Entry> it=another.rowMajorIterator();it.hasNext();) 
		{
			e=it.next();
			this.setAffinity(e.rowIndex(),e.columnIndex(),e.value());
		}
	}
	
	/**
	 * build affinity matrix from double array
	 * @param m
	 * double matrix
	 */
	public SparseAffinityMatrix(double[][] m)
	{
	this(m.length);
	
		if(m.length!=m[0].length) throw new IllegalArgumentException(
				"square matrix required: "+m.length+", "+m[0].length);
		
		for(int i=0;i<m.length;i++) 
			for(int j=0;j<m[i].length;j++) 
				if(m[i][j]!=0) setAffinity(i,j,m[i][j]);
	}
	
	/**
	 * get matrix size
	 * @return
	 */
	public int size()
	{
		return size;
	}
	
	/**
	 * get the number of nonzero entries
	 * @return
	 */
	public int numNonzeroEntries()
	{
		return numnonzero;
	}
	
	/**
	 * check affinity matrix index bounds
	 * @param idx
	 * affinity matrix index
	 */
	private void checkBounds(int idx)
	{
		if(idx<0||idx>=size()) throw new IndexOutOfBoundsException(idx+", "+size());
	}
	
	/**
	 * get affinity at a specified entry
	 * @param idx1
	 * row index
	 * @param idx2
	 * column index
	 * @return
	 */
	public double getAffinity(int idx1,int idx2)
	{
	SortedMap<Integer,Value> rowm;
	Value v;
	
		checkBounds(idx1);
		checkBounds(idx2);
		
		rowm=rowtable.get(idx1);//find by row index
		
		/*
		 * find by column index
		 */
		v=rowm.get(idx2);
		if(v==null) return 0;//corresponding entry not exists
		else return v.value;
	}
	
	/**
	 * set affinity for a specified entry
	 * @param idx1
	 * row index
	 * @param idx2
	 * column index
	 * @param value
	 * entry value
	 */
	public void setAffinity(int idx1,int idx2,double value)
	{
	SortedMap<Integer,Value> rowm,colm;
	Value v;
	
		checkBounds(idx1);
		checkBounds(idx2);
		
		if(value==0) removeEntry(idx1,idx2);
		else
		{
			rowm=rowtable.get(idx1);//get corresponding row
			colm=columntable.get(idx2);//get corresponding column
			/*
			 * set value
			 */
			v=rowm.get(idx2);
			//add new entry
			if(v==null)
			{
				v=new Value(value);
				rowm.put(idx2,v);
				colm.put(idx1,v);
				numnonzero++;
			}
			else v.value=value;
		}
	}
	
	/**
	 * remove an entry from sparse matrix
	 * @param idx1
	 * row index
	 * @param idx2
	 * column index
	 */
	private void removeEntry(int idx1,int idx2)
	{
	Map<Integer,Value> vm;
	Value vv;
	
		/*
		 * remove from row table
		 */
		vm=rowtable.get(idx1);
		vv=vm.remove(idx2);
		if(vv!=null) numnonzero--;
		/*
		 * remove from column table
		 */
		vm=columntable.get(idx2);
		vm.remove(idx1);
	}
	
	/**
	 * used to traverse a row
	 * @param idx1
	 * row index
	 * @return
	 */
	public SparseRowIterator rowIterator(int idx1)
	{
		checkBounds(idx1);
		return new SparseRowIterator(idx1);
	}
	
	/**
	 * used to traverse a column
	 * @param idx2
	 * column index
	 * @return
	 */
	public SparseColumnIterator columnIterator(int idx2)
	{
		checkBounds(idx2);
		return new SparseColumnIterator(idx2);
	}
	
	/**
	 * calculate this+another
	 * @param another
	 * another affinity matrix
	 * @return
	 */
	public AffinityMatrix add(AffinityMatrix another)
	{
	AffinityMatrix result;
	Entry e;
	double value;
	
		if(size()!=another.size()) throw new IllegalArgumentException(
				"matrix size not match: "+size()+", "+another.size());
	
		result=new SparseAffinityMatrix(this);
		for(Iterator<Entry> it=another.iterator();it.hasNext();)
		{
			e=it.next();
			value=result.getAffinity(e.rowIndex(),e.columnIndex())+e.value();
			result.setAffinity(e.rowIndex(),e.columnIndex(),value);
		}
		
		return result;
	}
	
	/**
	 * calculate this-another
	 * @param another
	 * another affinity matrix
	 * @return
	 */
	public AffinityMatrix substract(AffinityMatrix another)
	{
	AffinityMatrix result;
	Entry e;
	double value;
		
		if(size()!=another.size()) throw new IllegalArgumentException(
				"matrix size not match: "+size()+", "+another.size());
		
		result=new SparseAffinityMatrix(this);
		for(Iterator<Entry> it=another.iterator();it.hasNext();)
		{
			e=it.next();
			value=result.getAffinity(e.rowIndex(),e.columnIndex())-e.value();
			result.setAffinity(e.rowIndex(),e.columnIndex(),value);
		}

		return result;		
	}
	
	/**
	 * calculate this multiply another
	 * @param another
	 * another affinity matrix
	 * @return
	 */
	public AffinityMatrix multiply(AffinityMatrix another)
	{
	AffinityMatrix result;
	Entry e;
	double sum;
	
		if(size()!=another.size()) throw new IllegalArgumentException(
				"matrix size not match: "+size()+", "+another.size());
	
		result=new SparseAffinityMatrix(size());
		for(int i=0;i<result.size();i++) 
			for(int j=0;j<result.size();j++)
			{
				sum=0;

				for(Iterator<Entry> it=rowIterator(i);it.hasNext();)
				{
					e=it.next();
					sum+=e.value()*another.getAffinity(e.columnIndex(),j);					
				}

				result.setAffinity(i,j,sum);
			}

		return result;
	}
	
	/**
	 * calculate the scalar multiply
	 * @param s
	 * a scalar
	 * @return
	 */
	public AffinityMatrix scalarMultiply(double s)
	{
	AffinityMatrix result;
	Entry e;
	
		result=new SparseAffinityMatrix(size());
		
		for(Iterator<Entry> it=rowMajorIterator();it.hasNext();) 
		{
			e=it.next();
			result.setAffinity(e.rowIndex(),e.columnIndex(),s*e.value());
		}
		
		return result;
	}
	
	/**
	 * <h1>Description</h1>
	 * Used to traverse a row.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Aug 25, 2011 10:43:31 PM, revision:
	 */
	private class SparseRowIterator extends RowIterator
	{
	private int idx1;//row index
	private Iterator<Integer> it;
	
		/**
		 * @param idx1
		 * row index
		 */
		public SparseRowIterator(int idx1)
		{
			this.idx1=idx1;
			it=rowtable.get(idx1).keySet().iterator();
		}
		
		public boolean hasNext()
		{
			return it.hasNext();
		}

		public Entry next()
		{
			return new Entry(idx1,it.next());
		}

		public void remove()
		{
			throw new UnsupportedOperationException("can not remove entry from iterator");
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
	 * @version created on: Aug 25, 2011 10:43:31 PM, revision:
	 */
	private class SparseColumnIterator extends ColumnIterator
	{
	private int idx2;//row index
	private Iterator<Integer> it;
	
		/**
		 * @param idx1
		 * column index
		 */
		public SparseColumnIterator(int idx2)
		{
			this.idx2=idx2;
			it=columntable.get(idx2).keySet().iterator();
		}
		
		public boolean hasNext()
		{
			return it.hasNext();
		}

		public Entry next()
		{
			return new Entry(it.next(),idx2);
		}

		public void remove()
		{
			throw new UnsupportedOperationException("can not remove entry from iterator");
		}
	}
		
	public static void main(String[] args)
	{
	AffinityMatrix m;
	double[][] dm;
	
		m=new SparseAffinityMatrix(10);
		for(int i=0;i<10;i++) m.setAffinity((int)(Math.random()*m.size()),(int)(Math.random()*m.size()),i);
		
		dm=m.toMatrix();
		System.out.println(BLAS.toString(dm));
		
//		for(int i=0;i<m.size();i++)
//			for(Iterator<Entry> it=m.rowIterator(i);it.hasNext();) 
//				System.out.println(it.next());
		
//		System.out.println();
//		for(int j=0;j<m.size;j++) 
//			for(Iterator<Entry> it=m.columnIterator(j);it.hasNext();) 
//				System.out.println(it.next());
		
//		System.out.println();
//		for(Iterator<Entry> it=m.rowMajorIterator();it.hasNext();) 
//			System.out.println(it.next());
		
//		System.out.println();
//		for(Iterator<Entry> it=m.columnMajorIterator();it.hasNext();) 
//			System.out.println(it.next());
		
//		System.out.println();
//		System.out.println(BLAS.toString(m.toMatlabSparseMatrix()));
		
//		AffinityMatrix m2=new AffinityMatrix(10);
//		for(int i=0;i<10;i++) m2.setAffinity((int)(Math.random()*m.size()),(int)(Math.random()*m.size()),i);
//		AffinityMatrix m3=m.add(m2);
//		double[][] diff=BLAS.substract(BLAS.add(m.toMatrix(),m2.toMatrix(),null),m3.toMatrix(),null);
//		System.out.println(BLAS.toString(diff));
		
		AffinityMatrix m2=new SparseAffinityMatrix(10);
		for(int i=0;i<10;i++) m2.setAffinity((int)(Math.random()*m.size()),(int)(Math.random()*m.size()),i);
		AffinityMatrix m3=((SparseAffinityMatrix)m).multiply(m2);
		double[][] diff=BLAS.substract(BLAS.multiply(m.toMatrix(),m2.toMatrix(),null),m3.toMatrix(),null);
		System.out.println(BLAS.toString(diff));
	}
}
