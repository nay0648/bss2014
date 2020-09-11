package pp.util;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import org.apache.commons.math.complex.*;

/**
 * <h1>Description</h1>
 * Basic linear algebra subprograms.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Dec 25, 2009 3:01:55 PM, revision:
 */
public class BLAS implements Serializable
{
private static final long serialVersionUID=6106369074645421830L;

	/**
	 * throw exception if two vector's size not match
	 * @param v1, v2
	 * two vectors
	 */
	public static void checkSize(double[] v1,double[] v2)
	{
		if(v1.length!=v2.length) throw new IllegalArgumentException(
				"inconsistant vector dimension: "+v1.length+", "+v2.length);
	}
	
	/**
	 * throw exception if two vector's size not match
	 * @param v1, v2
	 * two vectors
	 */
	public static void checkSize(Object[] v1,Object[] v2)
	{
		if(v1.length!=v2.length) throw new IllegalArgumentException(
				"inconsistant vector dimension: "+v1.length+", "+v2.length);
	}
	
	/**
	 * throw exception if two matrics dimension not match
	 * @param m1, m2
	 * two matrices
	 */
	public static void checkSize(double[][] m1,double[][] m2)
	{
		if(m1.length!=m2.length||m1[0].length!=m2[0].length) 
			throw new IllegalArgumentException("insistant matrix dimension: "+
					m1.length+" x "+m1[0].length+", "+m2.length+" x "+m2[0].length);
	}
	
	/**
	 * throw exception if two matrics dimension not match
	 * @param m1, m2
	 * two matrices
	 */
	public static void checkSize(int[][] m1,int[][] m2)
	{
		if(m1.length!=m2.length||m1[0].length!=m2[0].length) 
			throw new IllegalArgumentException("insistant matrix dimension: "+
					m1.length+" x "+m1[0].length+", "+m2.length+" x "+m2[0].length);
	}
	
	/**
	 * throw exception if two matrics dimension not match
	 * @param m1, m2
	 * two matrices
	 */
	public static void checkSize(Object[][] m1,Object[][] m2)
	{
		if(m1.length!=m2.length||m1[0].length!=m2[0].length) 
			throw new IllegalArgumentException("insistant matrix dimension: "+
					m1.length+" x "+m1[0].length+", "+m2.length+" x "+m2[0].length);
	}
	
	/**
	 * throw exception if destination vector size not match
	 * @param dest
	 * the destination vector
	 * @param d
	 * the required dimension
	 */
	public static void checkDestinationSize(double[] dest,int d)
	{
		if(dest.length!=d) throw new IllegalArgumentException(
				"illegal destination vector dimension: "+dest.length+", required: "+d);
	}
	
	/**
	 * throw exception if destination vector size not match
	 * @param dest
	 * the destination vector
	 * @param d
	 * the required dimension
	 */
	public static void checkDestinationSize(Object[] dest,int d)
	{
		if(dest.length!=d) throw new IllegalArgumentException(
				"illegal destination vector dimension: "+dest.length+", required: "+d);
	}
	
	/**
	 * throw exception if destination matrix size not match
	 * @param dest
	 * the destination matrix
	 * @param m, n
	 * the required matrix dimension
	 */
	public static void checkDestinationSize(double[][] dest,int m,int n)
	{
		if(dest.length!=m||dest[0].length!=n) throw new IllegalArgumentException(
				"illegal destination matrix dimension: "+
				dest.length+" x "+dest[0].length+", required: "+
				m+" x "+n);
	}
	
	/**
	 * throw exception if destination matrix size not match
	 * @param dest
	 * the destination matrix
	 * @param m, n
	 * the required matrix dimension
	 */
	public static void checkDestinationSize(int[][] dest,int m,int n)
	{
		if(dest.length!=m||dest[0].length!=n) throw new IllegalArgumentException(
				"illegal destination matrix dimension: "+
				dest.length+" x "+dest[0].length+", required: "+
				m+" x "+n);
	}
	
	/**
	 * throw exception if destination matrix size not match
	 * @param dest
	 * the destination matrix
	 * @param m, n
	 * the required matrix dimension
	 */
	public static void checkDestinationSize(Object[][] dest,int m,int n)
	{
		if(dest.length!=m||dest[0].length!=n) throw new IllegalArgumentException(
				"illegal destination matrix dimension: "+
				dest.length+" x "+dest[0].length+", required: "+
				m+" x "+n);
	}

	/**
	 * print a matrix
	 * @param matrix
	 * a matrix
	 * @return
	 */
	public static String toString(double[][] matrix)
	{
	StringBuilder s;
	
		s=new StringBuilder();
		for(int i=0;i<matrix.length;i++)	
		{
			for(int j=0;j<matrix[i].length;j++) 
				s.append(matrix[i][j]+" ");
			s.append("\n");
		}
		return s.toString();	
	}
	
	/**
	 * print a matrix
	 * @param matrix
	 * a matrix
	 * @return
	 */
	public static String toString(int[][] matrix)
	{
	StringBuilder s;
	
		s=new StringBuilder();
		for(int i=0;i<matrix.length;i++)	
		{
			for(int j=0;j<matrix[i].length;j++) 
				s.append(matrix[i][j]+" ");
			s.append("\n");
		}
		return s.toString();	
	}
	
	/**
	 * convert an apache commons Complex number to string
	 * @param c
	 * a complex object
	 * @return
	 */
	public static String toString(Complex c)
	{
	double r,i;
		
		r=c.getReal();
		i=c.getImaginary();
		//real part is zero
		if(r==0)
		{
			if(i==0) return "0";
			else return Double.toString(i)+"j";
		}
		//real part is not zero
		else
		{
			if(i==0) return Double.toString(r);
			else if(i>0) return Double.toString(r)+"+"+Double.toString(i)+"j";
			else return Double.toString(r)+Double.toString(i)+"j";
		}
	}
	
	/**
	 * convert an apache commons complex array to string
	 * @param c
	 * a complex array
	 * @return
	 */
	public static String toString(Complex[] c)
	{
	StringBuilder s;
	
		s=new StringBuilder("[");
		for(int i=0;i<c.length;i++)
		{
			s.append(toString(c[i]));
			if(i<c.length-1) s.append(", ");
		}
		s.append("]");
		return s.toString();
	}
	
	/**
	 * convert a complex matrix to string
	 * @param matrix
	 * a complex matrix
	 * @return
	 */
	public static String toString(Complex[][] matrix)
	{
	StringBuilder s;
		
		s=new StringBuilder();
		for(int i=0;i<matrix.length;i++)	
		{
			for(int j=0;j<matrix[i].length;j++) 
				s.append(toString(matrix[i][j])+" ");
			s.append("\n");
		}
		return s.toString();
	}
	
	/**
	 * print a matrix
	 * @param matrix
	 * a matrix
	 */
	public static void println(int[][] matrix)
	{
		System.out.println(BLAS.toString(matrix));
	}
	
	/**
	 * print a matrix
	 * @param matrix
	 * a matrix
	 */
	public static void println(double[][] matrix)
	{
		System.out.println(BLAS.toString(matrix));
	}
	
	/**
	 * print a matrix
	 * @param matrix
	 * a matrix
	 */
	public static void println(Complex[][] matrix)
	{
		System.out.println(BLAS.toString(matrix));
	}
	
	/**
	 * print a vector
	 * @param v
	 * a vector
	 */
	public static void println(int[] v)
	{
		System.out.println(Arrays.toString(v));
	}
	
	/**
	 * print a vector
	 * @param v
	 * a vector
	 */
	public static void println(double[] v)
	{
		System.out.println(Arrays.toString(v));
	}
	
	/**
	 * print a vector
	 * @param v
	 * a vector
	 */
	public static void println(Complex[] v)
	{
		System.out.println(BLAS.toString(v));
	}
	
	/**
	 * write a matrix into file
	 * @param matrix
	 * a matrix
	 * @param file
	 * the destination file path
	 * @throws IOException
	 */
	public static void save(int[][] matrix,File file) throws IOException
	{
	BufferedWriter out=null;
	
		try
		{
			out=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
			out.write(toString(matrix));
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
	 * write a matrix into file
	 * @param matrix
	 * a matrix
	 * @param file
	 * the destination file path
	 * @throws IOException
	 */
	public static void save(double[][] matrix,File file) throws IOException
	{
	BufferedWriter out=null;
	
		try
		{
			out=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
			out.write(toString(matrix));
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
	 * save a complex matrix to file
	 * @param matrix
	 * a complex matrix
	 * @param file
	 * destination file
	 * @throws IOException
	 */
	public static void save(Complex[][] matrix,File file) throws IOException
	{
	BufferedWriter out=null;
		
		try
		{
			out=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
			out.write(toString(matrix));
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
	 * load a double matrix which is written in text format from file
	 * @param path
	 * file path
	 * @return
	 * @throws IOException
	 */
	public static double[][] loadDoubleMatrix(File path) throws IOException
	{
	BufferedReader in=null;
	List<double[]> lrow;
	String[] srow;
	double[] row;
	double[][] matrix;
	int r=0;
	
		try
		{
			lrow=new LinkedList<double[]>();
			in=new BufferedReader(new InputStreamReader(new FileInputStream(path)));
			for(String ts=null;(ts=in.readLine())!=null;)
			{
				ts=ts.trim();
				if(ts.length()==0) continue;
				srow=ts.split("\\s+");
				row=new double[srow.length];
				//convert a row to double
				for(int i=0;i<row.length;i++) row[i]=Double.parseDouble(srow[i]);
				lrow.add(row);
			}
			matrix=new double[lrow.size()][];
			for(double[] temp:lrow) matrix[r++]=temp;
			return matrix;
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
	 * load a integer matrix which is written in text format from file
	 * @param path
	 * file path
	 * @return
	 * @throws IOException
	 */
	public static int[][] loadIntMatrix(File path) throws IOException
	{
	BufferedReader in=null;
	List<int[]> lrow;
	String[] srow;
	int[] row;
	int[][] matrix;
	int r=0;
	
		try
		{
			lrow=new LinkedList<int[]>();
			in=new BufferedReader(new InputStreamReader(new FileInputStream(path)));
			for(String ts=null;(ts=in.readLine())!=null;)
			{
				ts=ts.trim();
				if(ts.length()==0) continue;
				srow=ts.split("\\s+");
				row=new int[srow.length];
				//convert a row to double
				for(int i=0;i<row.length;i++) row[i]=Integer.parseInt(srow[i]);
				lrow.add(row);
			}
			matrix=new int[lrow.size()][];
			for(int[] temp:lrow) matrix[r++]=temp;
			return matrix;
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
	 * load a complex written in text format from file
	 * @param path
	 * file path
	 * @return
	 * @throws IOException
	 */
	public static Complex[][] loadComplexMatrix(File path) throws IOException
	{
	List<String[]> lrow;
	BufferedReader in=null;
	Complex[][] data;
	int ridx=0;
	Pattern format1=Pattern.compile("^(.+)([\\+|-])(.*)[i|j]$");
	Pattern format2=Pattern.compile("^(.*)[i|j]$");
	Matcher m;
	double real,imag;
	
		try
		{
			/*
			 * load text data from file
			 */
			lrow=new LinkedList<String[]>();
			in=new BufferedReader(new InputStreamReader(new FileInputStream(path)));
			for(String ts=null;(ts=in.readLine())!=null;)
			{
				ts=ts.trim();
				if(ts.length()==0) continue;
				lrow.add(ts.split("\\s+"));
			}
			
			/*
			 * parse complex number
			 */
			data=new Complex[lrow.size()][];
			for(String[] sdata:lrow)
			{
				data[ridx]=new Complex[sdata.length];
				for(int j=0;j<sdata.length;j++)
				{
					/*
					 * real and imaginary part
					 */
					m=format1.matcher(sdata[j]);
					if(m.find())
					{
						real=Double.parseDouble(m.group(1));
						if("".equals(m.group(3))) imag=1;else imag=Double.parseDouble(m.group(3));
						if("-".equals(m.group(2))) imag*=-1;
						data[ridx][j]=new Complex(real,imag);
						continue;
					}
					
					/*
					 * only has imaginary part
					 */
					m=format2.matcher(sdata[j]);
					if(m.find())
					{
						if("".equals(m.group(1))) data[ridx][j]=new Complex(0,1);
						else if("-".equals(m.group(1))) data[ridx][j]=new Complex(0,-1);
						else data[ridx][j]=new Complex(0,Double.parseDouble(m.group(1)));
						continue;
					}
					
					//only has real part
					data[ridx][j]=new Complex(Double.parseDouble(sdata[j]),0);
				}
				ridx++;
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
		return data;
	}
	
	/**
	 * copy a matrix
	 * @param matrix
	 * a matrix
	 * @param destination
	 * The destination, null to allocate new space
	 * @return
	 */
	public static double[][] copy(double[][] matrix,double[][] destination)
	{
		if(destination==null) destination=new double[matrix.length][matrix[0].length];
		else checkDestinationSize(destination,matrix.length,matrix[0].length);
		for(int i=0;i<matrix.length;i++)
			for(int j=0;j<matrix[i].length;j++) destination[i][j]=matrix[i][j];
		return destination;
	}
	
	/**
	 * copy a matrix
	 * @param matrix
	 * a matrix
	 * @param destination
	 * The destination, null to allocate new space
	 * @return
	 */
	public static Complex[][] copy(Complex[][] matrix,Complex[][] destination)
	{
		if(destination==null) destination=new Complex[matrix.length][matrix[0].length];
		else checkDestinationSize(destination,matrix.length,matrix[0].length);
		for(int i=0;i<matrix.length;i++)
			for(int j=0;j<matrix[i].length;j++) 
				destination[i][j]=new Complex(matrix[i][j].getReal(),matrix[i][j].getImaginary());
		return destination;
	}
	
	/**
	 * copy a matrix
	 * @param matrix
	 * a matrix
	 * @param destination
	 * The destination, null to allocate new space
	 * @return
	 */
	public static int[][] copy(int[][] matrix,int[][] destination)
	{
		if(destination==null) destination=new int[matrix.length][matrix[0].length];
		else checkDestinationSize(destination,matrix.length,matrix[0].length);
		for(int i=0;i<matrix.length;i++)
			for(int j=0;j<matrix[i].length;j++) destination[i][j]=matrix[i][j];
		return destination;
	}
	
	/**
	 * copy a complex vector
	 * @param v
	 * a complex vector
	 * @param dest
	 * The destination, null to allocate new space
	 * @return
	 */
	public static Complex[] copy(Complex[] v,Complex[] dest)
	{
		if(dest==null) dest=new Complex[v.length];
		else checkDestinationSize(dest,v.length);
		
		for(int i=0;i<dest.length;i++) dest[i]=v[i];
		return dest;
	}
	
	/**
	 * generate an identity matrix
	 * @param row
	 * row number
	 * @param column
	 * column number
	 * @return
	 */
	public static double[][] eye(int row,int column)
	{
	double[][] eye;
	int len;
		
		eye=new double[row][column];
		
		len=Math.min(row,column);
		for(int i=0;i<len;i++) eye[i][i]=1;
	
		return eye;
	}
	
	/**
	 * generate an identity matrix
	 * @param row
	 * row number
	 * @param column
	 * column number
	 * @return
	 */
	public static Complex[][] eyeComplex(int row,int column)
	{
	Complex[][] eye;
		
		eye=new Complex[row][column];
		for(int i=0;i<eye.length;i++) 
			for(int j=0;j<eye[i].length;j++) 
				if(i==j) eye[i][j]=Complex.ONE;else eye[i][j]=Complex.ZERO;
		
		return eye;
	}
	
	/**
	 * fill the matrix to a specified value
	 * @param matrix
	 * the matrix
	 * @param value
	 * the specified value
	 */
	public static void fill(double[][] matrix,double value)
	{
		for(int i=0;i<matrix.length;i++)
			for(int j=0;j<matrix[i].length;j++) matrix[i][j]=value;
	}
	
	/**
	 * fill a multi-channel matrix a specified value
	 * @param multichannel
	 * a multi-channel value
	 * @param value
	 * the filled value
	 */
	public static void fill(double[][][] multichannel,double value)
	{
		for(int y=0;y<multichannel.length;y++)
			for(int x=0;x<multichannel[y].length;x++)
				Arrays.fill(multichannel[y][x],value);
	}
	
	/**
	 * fill a matrix with specified value
	 * @param matrix
	 * destination matrix
	 * @param value
	 * a complex value
	 */
	public static void fill(Complex[][] matrix,Complex value)
	{
		for(int i=0;i<matrix.length;i++)
			for(int j=0;j<matrix[i].length;j++) matrix[i][j]=value;
	}
	
	/**
	 * normalize a vector x to unit vector so that ||x||=1
	 * @param x
	 * a vector
	 */
	public static void normalize(double[] x)
	{
	double temp=0;
	
		for(double xi:x) temp+=xi*xi;
		if(temp==0) return;//the zero vector
		temp=Math.sqrt(temp);
		for(int i=0;i<x.length;i++) x[i]/=temp;
	}
	
	/**
	 * normalize a complex vector x so that ||x||=1
	 * @param x
	 * a complex vector
	 */
	public static void normalize(Complex[] x)
	{
	double temp=0;
	
		for(Complex c:x) temp+=c.getReal()*c.getReal()+c.getImaginary()*c.getImaginary();
		if(temp==0) return;//zero vector
		temp=1.0/Math.sqrt(temp);
		for(int i=0;i<x.length;i++) x[i]=x[i].multiply(temp);
	}
	
	/**
	 * calculate a vector's 2-norm
	 * @param x
	 * a vector
	 * @return
	 */
	public static double norm2(double[] x)
	{
	double n2=0;
	
		for(int i=0;i<x.length;i++) n2+=x[i]*x[i];
		return Math.sqrt(n2);
	}
	
	/**
	 * calculate a vector's 2-norm
	 * @param x
	 * a vector
	 * @return
	 */
	public static double norm2(Complex[] x)
	{
	double n2=0;
		
		for(int i=0;i<x.length;i++) 
			n2+=x[i].getReal()*x[i].getReal()+x[i].getImaginary()*x[i].getImaginary();
		return Math.sqrt(n2);
	}
	
	/**
	 * calculate the maximum norm for a vector: max(|x1|, |x2|, ..., |xn|)
	 * @param x
	 * a vector
	 * @return
	 */
	public static double normInf(double[] x)
	{
	double n=0,absxx;
	
		for(double xx:x) 
		{
			absxx=Math.abs(xx);
			if(absxx>n) n=absxx;
		}
		
		return n;
	}
	
	/**
	 * calculate the maximum norm for a matrix
	 * @param m
	 * a matrix
	 * @return
	 */
	public static double normInf(double[][] m)
	{
	double n=0,absxx;
		
		for(int i=0;i<m.length;i++) 
			for(int j=0;j<m[i].length;j++) 
			{
				absxx=Math.abs(m[i][j]);
				if(absxx>n) n=absxx;
			}
		
		return n;		
	}
	
	/**
	 * calculate the maximum norm for a matrix
	 * @param m
	 * a matrix
	 * @return
	 */
	public static double normInf(Complex[][] m)
	{
	double n=0,absxx;
		
		for(int i=0;i<m.length;i++) 
			for(int j=0;j<m[i].length;j++) 
			{
				absxx=m[i][j].abs();
				if(absxx>n) n=absxx;
			}

		return n;
	}
	
	/**
	 * calculate the inner product of two vectors
	 * @param x, y
	 * two vectors
	 * @return
	 */
	public static double innerProduct(double[] x,double[] y)
	{
	double temp=0;
	
		checkSize(x,y);
		for(int i=0;i<x.length;i++) temp+=x[i]*y[i];
		return temp;
	}
	
	/**
	 * calculate the inner product of two complex vectors: x'*y
	 * @param x, y
	 * two complex vectors
	 * @return
	 */
	public static Complex innerProduct(Complex[] x,Complex[] y)
	{
	Complex temp;
	
		checkSize(x,y);
		temp=new Complex(0,0);
		for(int i=0;i<x.length;i++) temp=temp.add(x[i].conjugate().multiply(y[i]));
		return temp;
	}
	
	/**
	 * calculate the outer product of two vectors
	 * @param x
	 * a vector
	 * @param y
	 * another vector
	 * @param result
	 * The result destination, null to allocate new space
	 * @return
	 */
	public static double[][] outerProduct(double[] x,double[] y,double[][] result)
	{
		if(result==null) result=new double[x.length][y.length];
		else checkDestinationSize(result,x.length,y.length);
		for(int i=0;i<x.length;i++)
			for(int j=0;j<y.length;j++) result[i][j]=x[i]*y[j];
		return result;
	}
	
	/**
	 * calculate m1 * m2
	 * @param m1
	 * the first matrix
	 * @param m2
	 * the second matrix
	 * @param result
	 * The result destination, null to allocate new space
	 * @return
	 */
	public static double[][] multiply(double[][] m1,double[][] m2,double[][] result)
	{
	double[] temp;
	
		if(m1[0].length!=m2.length) throw new IllegalArgumentException(
				"inconsistant matrix dimension for multiplication: "+m1[0].length+", "+m2.length);
		if(result==null) result=new double[m1.length][m2[0].length];
		else checkDestinationSize(result,m1.length,m2[0].length);
		//the first matrix is used to store result
		if(result==m1)
		{
			temp=new double[result[0].length];//a row for result
			//calculate each row of the result matrix
			for(int i=0;i<result.length;i++)
			{
				Arrays.fill(temp,0);
				//fill each column of current row
				for(int j=0;j<temp.length;j++)
					//fill an entry
					for(int k=0;k<m1[i].length;k++) temp[j]+=m1[i][k]*m2[k][j];
				System.arraycopy(temp,0,result[i],0,temp.length);
			}
		}
		//the second matrix is used to store result
		else if(result==m2)
		{
			temp=new double[m2.length];//a column of result
			//calculate each column of the result matrix
			for(int j=0;j<result[0].length;j++)
			{
				Arrays.fill(temp,0);
				//fill each row of current column
				for(int i=0;i<temp.length;i++)
					//fill an entry
					for(int k=0;k<m1[i].length;k++) temp[i]+=m1[i][k]*m2[k][j];
				for(int k=0;k<temp.length;k++) result[k][j]=temp[k];
			}
		}
		//other destination
		else
		{
			for(int i=0;i<result.length;i++)
				for(int j=0;j<result[i].length;j++)
					for(int k=0;k<m1[i].length;k++) result[i][j]+=m1[i][k]*m2[k][j];
		}
		return result;
	}
	
	/**
	 * calculate m1 * m2 for complex matrices
	 * @param m1
	 * a complex matrix
	 * @param m2
	 * another complex matrix
	 * @param result
	 * space for result, or null to allocate new space
	 * @return
	 */
	public static Complex[][] multiply(Complex[][] m1,Complex[][] m2,Complex[][] result)
	{
		if(m1[0].length!=m2.length) throw new IllegalArgumentException(
				"inconsistant matrix dimension for multiplication: "+m1[0].length+", "+m2.length);
		if(result==null) result=new Complex[m1.length][m2[0].length];
		else checkDestinationSize(result,m1.length,m2[0].length);
		//the first matrix is used to store result
		if(result==m1)
		{
		Complex[] temp;	
			
			temp=new Complex[result[0].length];//a row for result
			//calculate each row of the result matrix
			for(int i=0;i<result.length;i++)
			{
				for(int j=0;j<temp.length;j++) temp[j]=new Complex(0,0);
				//fill each column of current row
				for(int j=0;j<temp.length;j++)
					//fill an entry
					for(int k=0;k<m1[i].length;k++) temp[j]=temp[j].add(m1[i][k].multiply(m2[k][j]));
				for(int k=0;k<temp.length;k++) result[i][k]=temp[k];
			}
		}
		//the second matrix is used to store result
		else if(result==m2)
		{
		Complex[] temp;
			
			temp=new Complex[m2.length];//a column of result
			//calculate each column of the result matrix
			for(int j=0;j<result[0].length;j++)
			{
				for(int i=0;i<temp.length;i++) temp[i]=new Complex(0,0);
				//fill each row of current column
				for(int i=0;i<temp.length;i++)
					//fill an entry
					for(int k=0;k<m1[i].length;k++) temp[i]=temp[i].add(m1[i][k].multiply(m2[k][j]));
				for(int k=0;k<temp.length;k++) result[k][j]=temp[k];
			}
		}
		//other destination
		else
		{
		Complex temp;
		
			for(int i=0;i<result.length;i++)
				for(int j=0;j<result[i].length;j++)
				{
					temp=new Complex(0,0);
					for(int k=0;k<m1[i].length;k++) temp=temp.add(m1[i][k].multiply(m2[k][j]));
					result[i][j]=temp;
				}
		}
		return result;
	}
	
	/**
	 * perform entry by entry mulitplication
	 * @param v1
	 * a vector
	 * @param v2
	 * another vector
	 * @param result
	 * buffer for result, null to allocate new space
	 * @return
	 */
	public static double[] entryMultiply(double[] v1,double[] v2,double[] result)
	{
		checkSize(v1,v2);
		if(result==null) result=new double[v1.length];
		else checkSize(result,v1);
		for(int i=0;i<result.length;i++) result[i]=v1[i]*v2[i];
		return result;
	}
	
	/**
	 * perform entry by entry mulitplication
	 * @param v1
	 * a vector
	 * @param v2
	 * another vector
	 * @param result
	 * buffer for result, null to allocate new space
	 * @return
	 */
	public static Complex[] entryMultiply(Complex[] v1,Complex[] v2,Complex[] result)
	{
		checkSize(v1,v2);
		if(result==null) result=new Complex[v1.length];
		else checkSize(result,v1);
		for(int i=0;i<result.length;i++) result[i]=v1[i].multiply(v2[i]);
		return result;
	}
	
	/**
	 * perform entry by entry multiplication
	 * @param m1
	 * a matrix
	 * @param m2
	 * another matrix
	 * @param result
	 * buffer for result, null to allocate new space
	 * @return
	 */
	public static double[][] entryMultiply(double[][] m1,double[][] m2,double[][] result)
	{
		checkSize(m1,m2);
		if(result==null) result=new double[m1.length][m1[0].length];
		else checkSize(result,m1);
		for(int i=0;i<result.length;i++)
			for(int j=0;j<result[i].length;j++) result[i][j]=m1[i][j]*m2[i][j];
		return result;
	}
	
	/**
	 * calculate the scalar multiplication of a scalar and a vector
	 * @param s
	 * a scalar
	 * @param v
	 * a vector
	 * @param result
	 * Result destination, null to allocate new space.
	 * @return
	 */
	public static double[] scalarMultiply(double s,double[] v,double[] result)
	{
		if(result==null) result=new double[v.length];
		else checkDestinationSize(result,v.length);
		for(int i=0;i<result.length;i++) result[i]=s*v[i];
		return result;
	}
	
	/**
	 * calculate the scalar multiplication of a scalar and a vector
	 * @param s
	 * a scalar
	 * @param v
	 * a vector
	 * @param result
	 * Result destination, null to allocate new space.
	 * @return
	 */
	public static Complex[] scalarMultiply(double s,Complex[] v,Complex[] result)
	{
		if(result==null) result=new Complex[v.length];
		else checkDestinationSize(result,v.length);
		for(int i=0;i<result.length;i++) result[i]=v[i].multiply(s);
		return result;
	}
	
	/**
	 * calculate the scalar multiplication of a scalar and a vector
	 * @param s
	 * a scalar
	 * @param v
	 * a vector
	 * @param result
	 * Result destination, null to allocate new space.
	 * @return
	 */
	public static Complex[] scalarMultiply(Complex s,Complex[] v,Complex[] result)
	{
		if(result==null) result=new Complex[v.length];
		else checkDestinationSize(result,v.length);
		for(int i=0;i<result.length;i++) result[i]=v[i].multiply(s);
		return result;
	}
	
	/**
	 * calculate the scalar multiplication of a scalar and a matrix
	 * @param s
	 * a scalar
	 * @param m
	 * a matrix
	 * @param result
	 * Result destination, null to allocate new space.
	 * @return
	 */
	public static double[][] scalarMultiply(double s,double[][] m,double[][] result)
	{
		if(result==null) result=new double[m.length][m[0].length];
		else checkDestinationSize(result,m.length,m[0].length);
		for(int i=0;i<result.length;i++)
			for(int j=0;j<result[i].length;j++) result[i][j]=s*m[i][j];
		return result;
	}
	
	/**
	 * calculate the scalar multiplication of a scalar and a matrix
	 * @param s
	 * a scalar
	 * @param m
	 * a matrix
	 * @param result
	 * Result destination, null to allocate new space.
	 * @return
	 */
	public static Complex[][] scalarMultiply(double s,Complex[][] m,Complex[][] result)
	{
		if(result==null) result=new Complex[m.length][m[0].length];
		else checkDestinationSize(result,m.length,m[0].length);
		for(int i=0;i<result.length;i++)
			for(int j=0;j<result[i].length;j++) result[i][j]=m[i][j].multiply(s);
		return result;
	}
	
	/**
	 * calculate the sum of two vectors
	 * @param v1
	 * a vector
	 * @param v2
	 * another vector
	 * @param result
	 * Result destination, null to allocate new space.
	 * @return
	 */
	public static double[] add(double[] v1,double[] v2,double[] result)
	{
		checkSize(v1,v2);
		if(result==null) result=new double[v1.length];
		else checkDestinationSize(result,v1.length);
		for(int i=0;i<result.length;i++) result[i]=v1[i]+v2[i];
		return result;
	}
	
	/**
	 * calculate the sum of two matrices
	 * @param m1, m2
	 * two matrices
	 * @param result
	 * Result destination, null to allocate new space.
	 * @return
	 */
	public static double[][] add(double[][] m1,double[][] m2,double[][] result)
	{
		checkSize(m1,m2);
		if(result==null) result=new double[m1.length][m1[0].length];
		else checkDestinationSize(result,m1.length,m1[0].length);
		for(int i=0;i<result.length;i++)
			for(int j=0;j<result[i].length;j++) result[i][j]=m1[i][j]+m2[i][j];
		return result;
	}
	
	/**
	 * calculate the sum of two matrices
	 * @param m1, m2
	 * two matrices
	 * @param result
	 * Result destination, null to allocate new space.
	 * @return
	 */
	public static int[][] add(int[][] m1,int[][] m2,int[][] result)
	{
		checkSize(m1,m2);
		if(result==null) result=new int[m1.length][m1[0].length];
		else checkDestinationSize(result,m1.length,m1[0].length);
		for(int i=0;i<result.length;i++)
			for(int j=0;j<result[i].length;j++) result[i][j]=m1[i][j]+m2[i][j];
		return result;
	}
	
	/**
	 * calculate the sum of two matrices
	 * @param m1, m2
	 * two matrices
	 * @param result
	 * Result destination, null to allocate new space.
	 * @return
	 */
	public static Complex[][] add(Complex[][] m1,Complex[][] m2,Complex[][] result)
	{
		checkSize(m1,m2);
		
		if(result==null) result=new Complex[m1.length][m1[0].length];
		else checkDestinationSize(result,m1.length,m1[0].length);
		for(int i=0;i<result.length;i++)
			for(int j=0;j<result[i].length;j++) result[i][j]=m1[i][j].add(m2[i][j]);
		
		return result;
	}
	
	/**
	 * calculate the sum of two vectors
	 * @param v1
	 * a vector
	 * @param v2
	 * another vector
	 * @param result
	 * Result destination, null to allocate new space.
	 * @return
	 */
	public static Complex[] add(Complex[] v1,Complex[] v2,Complex[] result)
	{
		checkSize(v1,v2);
		if(result==null) result=new Complex[v1.length];
		else checkDestinationSize(result,v1.length);
		for(int i=0;i<result.length;i++) result[i]=v1[i].add(v2[i]);
		return result;
	}
	
	/**
	 * calculate the difference of two vectors
	 * @param v1
	 * a vector
	 * @param v2
	 * another vector
	 * @param result
	 * Result destination, null to allocate new space
	 * @return
	 */
	public static double[] substract(double[] v1,double[]v2,double[] result)
	{
		checkSize(v1,v2);
		if(result==null) result=new double[v1.length];
		else checkDestinationSize(result,v1.length);
		for(int i=0;i<result.length;i++) result[i]=v1[i]-v2[i];
		return result;
	}
	
	/**
	 * calculate the difference of two vectors
	 * @param v1
	 * a vector
	 * @param v2
	 * another vector
	 * @param result
	 * Result destination, null to allocate new space
	 * @return
	 */
	public static Complex[] substract(Complex[] v1,Complex[] v2,Complex[] result)
	{
		checkSize(v1,v2);
		if(result==null) result=new Complex[v1.length];
		else checkDestinationSize(result,v1.length);
		for(int i=0;i<result.length;i++) result[i]=v1[i].subtract(v2[i]);
		return result;
	}
	
	/**
	 * calculate the difference of two matrices
	 * @param m1
	 * a matrix
	 * @param m2
	 * another matrix
	 * @param result
	 * Result destination, null to allocate new space
	 * @return
	 */
	public static double[][] substract(double[][] m1,double[][] m2,double[][] result)
	{
		checkSize(m1,m2);
		if(result==null) result=new double[m1.length][m1[0].length];
		else checkDestinationSize(result,m1.length,m1[0].length);
		for(int i=0;i<result.length;i++)
			for(int j=0;j<result[i].length;j++) result[i][j]=m1[i][j]-m2[i][j];
		return result;
	}
	
	/**
	 * calculate the difference of two matrices
	 * @param m1
	 * a matrix
	 * @param m2
	 * another matrix
	 * @param result
	 * Result destination, null to allocate new space
	 * @return
	 */
	public static Complex[][] substract(Complex[][] m1,Complex[][] m2,Complex[][] result)
	{
		checkSize(m1,m2);
		if(result==null) result=new Complex[m1.length][m1[0].length];
		else checkDestinationSize(result,m1.length,m1[0].length);
		for(int i=0;i<result.length;i++)
			for(int j=0;j<result[i].length;j++) result[i][j]=m1[i][j].subtract(m2[i][j]);
		return result;
	}
	
	/**
	 * add a scalar into a vector
	 * @param s
	 * a scalar
	 * @param v
	 * a vector
	 * @param result
	 * space for result, null to allocate new space
	 * @return
	 */
	public static double[] scalarAdd(double s,double[] v,double[] result)
	{
		if(result==null) result=new double[v.length];
		else checkSize(v,result);
		for(int i=0;i<v.length;i++) result[i]=s+v[i];
		return result;
	}
	
	/**
	 * add a scalar into a matrix
	 * @param s
	 * a scalar
	 * @param m
	 * a matrix
	 * @param result
	 * space for result, null to allocate new space
	 * @return
	 */
	public static double[][] scalarAdd(double s,double[][] m,double[][] result)
	{
		if(result==null) result=new double[m.length][m[0].length];
		else checkSize(m,result);
		for(int i=0;i<m.length;i++)
			for(int j=0;j<m[i].length;j++) result[i][j]=s+m[i][j];
		return result;
	}
	
	/**
	 * calculate the sum of entries of a vector
	 * @param v
	 * a vector
	 * @return
	 */
	public static double sum(double[] v)
	{
	double sum=0;
	
		for(double e:v) sum+=e;
		return sum;
	}
	
	/**
	 * calculate the sum of entries of a matrix
	 * @param m
	 * a matrix
	 * @return
	 */
	public static double sum(double[][] m)
	{
	double sum=0;
	
		for(int i=0;i<m.length;i++)
			for(int j=0;j<m[i].length;j++) sum+=m[i][j];
		return sum;
	}
	
	/**
	 * calculate the mean value of a vector
	 * @param v
	 * a vector
	 * @return
	 */
	public static double mean(double[] v)
	{
		return sum(v)/v.length;
	}
	
	/**
	 * calculate the mean velue of a matrix
	 * @param m
	 * a matrix
	 * @return
	 */
	public static double mean(double[][] m)
	{
		return sum(m)/(m.length*m[0].length);
	}
	
	/**
	 * calculate the determinant of a square matrix
	 * @param matrix
	 * a square matrix
	 * @return
	 */
	public static double det(double[][] matrix)
	{
	double[][] m;
	double[] tempr;
	int nonzero;
	double sign=1,temp;
	
		if(matrix.length!=matrix[0].length) throw new IllegalArgumentException(
				"a square matrix is required");
		/*
		 * convert to the upper trangular matrix then 
		 * calculate the determinant
		 */
		//copy the original matrix because the data will be modified
		m=copy(matrix,null);
		//process the first n-1 row
		for(int i=0;i<m.length-1;i++)
		{
			//find the row the ith element is not zero
			for(nonzero=i;nonzero<m.length;nonzero++) if(m[nonzero][i]!=0) break;
			//can not find such row
			if(nonzero>=m.length) return 0;
			//swap m[i][] and m[nonzero][]
			else if(nonzero!=i)
			{
				tempr=m[i];
				m[i]=m[nonzero];
				m[nonzero]=tempr;
				sign*=-1;//the sign is changed when swap two rows
			}
			/*
			 * eliminate the elements in the ith column from the 
			 * i+1th row
			 */
			for(int ii=i+1;ii<m.length;ii++)
			{
				if(m[ii][i]==0) continue;
				temp=-m[ii][i]/m[i][i];
				for(int j=i;j<m[i].length;j++) m[ii][j]+=(m[i][j]*temp);
			}
		}
		/*
		 * calculate the final result
		 */
		if(m[m.length-1][m.length-1]==0) return 0;
		for(int i=0;i<m.length;i++) sign*=m[i][i];
		return sign;
	}
	
	/**
	 * calculate the determinant of a square matrix
	 * @param matrix
	 * a square matrix
	 * @return
	 */
	public static Complex det(Complex[][] matrix)
	{
	Complex[][] m;
	Complex[] tempr;
	int nonzero;
	Complex temp,sign=Complex.ONE;
	
		if(matrix.length!=matrix[0].length) throw new IllegalArgumentException(
				"a square matrix is required");

		/*
		 * convert to the upper trangular matrix then 
		 * calculate the determinant
		 */
		//copy the original matrix because the data will be modified
		m=copy(matrix,null);
		
		//process the first n-1 row
		for(int i=0;i<m.length-1;i++)
		{
			//find the row the ith element is not zero
			for(nonzero=i;nonzero<m.length;nonzero++) 
				if(!m[nonzero][i].equals(Complex.ZERO)) break;

			//can not find such row
			if(nonzero>=m.length) return Complex.ZERO;
			
			//swap m[i][] and m[nonzero][]
			else if(nonzero!=i)
			{
				tempr=m[i];
				m[i]=m[nonzero];
				m[nonzero]=tempr;
				sign=sign.multiply(-1);//the sign is changed when swap two rows
			}
			
			/*
			 * eliminate the elements in the ith column from the 
			 * i+1th row
			 */
			for(int ii=i+1;ii<m.length;ii++)
			{
				if(m[ii][i].equals(Complex.ZERO)) continue;
				
				temp=m[ii][i].negate().divide(m[i][i]);
				for(int j=i;j<m[i].length;j++) 
					m[ii][j]=m[ii][j].add(m[i][j].multiply(temp));
			}
		}
		
		/*
		 * calculate the final result
		 */
		if(m[m.length-1][m.length-1].equals(Complex.ZERO)) return Complex.ZERO;
		for(int i=0;i<m.length;i++) sign=sign.multiply(m[i][i]);
		return sign;
	}
	
	/**
	 * calculate the inverse matrix
	 * @param matrix
	 * the input matrix
	 * @param inv
	 * The result destination, null to allocate new space.
	 * @return
	 * return null if the matrix is singular
	 */
	public static double[][] inv(double[][] matrix,double[][] inv)
	{
	double[][] m;
	double[] tempr;
	int nonzero;
	double temp;
	
		if(matrix.length!=matrix[0].length) throw new IllegalArgumentException(
				"a square matrix is required");
		if(inv==null) inv=new double[matrix.length][matrix[0].length];
		else checkDestinationSize(inv,matrix.length,matrix[0].length);
		m=copy(matrix,null);//to prevent the original matrix be modified
		//starts from the identity matrix
		for(int i=0;i<inv.length;i++) 
			for(int j=0;j<inv[i].length;j++) 
				if(i==j) inv[i][j]=1;else inv[i][j]=0;
		//process the first n-1 row
		for(int i=0;i<m.length-1;i++)
		{
			//find the pivotal row
			for(nonzero=i;nonzero<m.length;nonzero++) if(m[nonzero][i]!=0) break;
			//can not find such row
			if(nonzero>=m.length) return null;
			//swap m[i][] and the pivotal row
			else if(nonzero!=i)
			{
				tempr=m[i];
				m[i]=m[nonzero];
				m[nonzero]=tempr;
				/*
				 * the same elementary row operation to inverse matrix
				 */
				tempr=inv[i];
				inv[i]=inv[nonzero];
				inv[nonzero]=tempr;
			}
			/*
			 * make the pivot to 1
			 */
			temp=1/m[i][i];
			for(int j=i;j<m[i].length;j++) m[i][j]*=temp;
			for(int j=0;j<inv[i].length;j++) inv[i][j]*=temp;
			//eliminate the elements in the ith column from the i+1th row
			for(int ii=i+1;ii<m.length;ii++)
			{
				if(m[ii][i]==0) continue;
				temp=-m[ii][i];//the pivot is 1
				for(int j=i;j<m[i].length;j++) m[ii][j]+=(m[i][j]*temp);
				//the same elementary row operation to inverse matrix
				for(int j=0;j<inv[i].length;j++) inv[ii][j]+=(inv[i][j]*temp);
			}
		}
		/*
		 * process the last row
		 */
		if(m[m.length-1][m[m.length-1].length-1]==0) return null;
		else
		{
			temp=1/m[m.length-1][m[m.length-1].length-1];
			m[m.length-1][m[m.length-1].length-1]=1;
			for(int j=0;j<inv[inv.length-1].length;j++) inv[inv.length-1][j]*=temp;
		}
		/*
		 * back substitution
		 */
		for(int i=inv.length-1;i>=1;i--)
			for(int ii=i-1;ii>=0;ii--)
			{
				temp=-m[ii][i];
				for(int j=0;j<inv[i].length;j++) inv[ii][j]+=(inv[i][j]*temp);	
			}
		return inv;
	}
	
	/**
	 * calculate the inverse matrix of a complex matrix
	 * @param matrix
	 * a complex matrix
	 * @param inv
	 * destination for result, null to allocate new space
	 * @return
	 */
	public static Complex[][] inv(Complex[][] matrix,Complex[][] inv)
	{
	double[][] temp;
	double real,imag;
		
		if(matrix.length!=matrix[0].length) throw new IllegalArgumentException(
				"a square matrix is required");
		if(inv==null) inv=new Complex[matrix.length][matrix[0].length];
		else checkDestinationSize(inv,matrix.length,matrix[0].length);
		
		/*
		 * construct [real(M) imag(M); -imag(M) real(M)]
		 */
		temp=new double[matrix.length*2][matrix[0].length*2];
		for(int i=0;i<matrix.length;i++)
			for(int j=0;j<matrix[i].length;j++)
			{
				real=matrix[i][j].getReal();
				imag=matrix[i][j].getImaginary();
				temp[i][j]=real;
				temp[i+matrix.length][j+matrix[i].length]=real;
				temp[i][j+matrix[i].length]=imag;
				temp[i+matrix.length][j]=-imag;
			}
		
		/*
		 * calculate the inverse, the result is: 
		 * [real(inv(M)) imag(inv(M)); -imag(inv(M)) real(inv(M))]
		 */
		temp=BLAS.inv(temp,temp);
		if(temp==null) return null;
		
		//get the result
		for(int i=0;i<inv.length;i++)
			for(int j=0;j<inv[i].length;j++)
				inv[i][j]=new Complex(temp[i][j],temp[i][j+inv[i].length]);
		return inv;
	}
	
	/**
	 * calculate the pseudo inverse (M'*inv(M*M')) for a matrix
	 * @param matrix
	 * a matrix
	 * @param inv
	 * space for result matrix, or null to allocate new space
	 * @return
	 */
	public static double[][] pinv(double[][] matrix,double[][] inv)
	{
	double[][] product;
				
		if(inv==null) inv=new double[matrix[0].length][matrix.length];
		else checkDestinationSize(inv,matrix[0].length,matrix.length);
			
		inv=BLAS.transpose(matrix,inv);//M'
		product=BLAS.multiply(matrix,inv,null);//M*M'
		BLAS.inv(product,product);//inv(M*M')
		BLAS.multiply(inv,product,inv);//M'*inv(M*M')

		return inv;
	}
	
	/**
	 * calculate the pseudo inverse (M'*inv(M*M')) for a complex matrix
	 * @param matrix
	 * a complex matrix
	 * @param inv
	 * space for result matrix, or null to allocate new space
	 * @return
	 */
	public static Complex[][] pinv(Complex[][] matrix,Complex[][] inv)
	{
	double[][] temp,temp2,product;
	double real,imag;
			
		if(inv==null) inv=new Complex[matrix[0].length][matrix.length];
		else checkDestinationSize(inv,matrix[0].length,matrix.length);
			
		/*
		 * construct [real(M) imag(M); -imag(M) real(M)]
		 */
		temp=new double[matrix.length*2][matrix[0].length*2];
		for(int i=0;i<matrix.length;i++)
			for(int j=0;j<matrix[i].length;j++)
			{
				real=matrix[i][j].getReal();
				imag=matrix[i][j].getImaginary();
				temp[i][j]=real;
				temp[i+matrix.length][j+matrix[i].length]=real;
				temp[i][j+matrix[i].length]=imag;
				temp[i+matrix.length][j]=-imag;
			}
		
		
		temp2=BLAS.transpose(temp,null);//M'
		product=BLAS.multiply(temp,temp2,null);//M*M'
		BLAS.inv(product,product);//inv(M*M')
		BLAS.multiply(temp2,product,temp2);//M'*inv(M*M')
		
		//get the result
		for(int i=0;i<inv.length;i++)
			for(int j=0;j<inv[i].length;j++)
				inv[i][j]=new Complex(temp2[i][j],temp2[i][j+inv[i].length]);
		return inv;
	}
	
	/**
	 * perform the transpose
	 * @param matrix
	 * input matrix
	 * @param result
	 * space for transposed matrix, or null to allocate new space
	 * @return
	 */
	public static double[][] transpose(double[][] matrix,double[][] result)
	{
		if(result==null) result=new double[matrix[0].length][matrix.length];
		else
		{
			if(matrix==result) throw new IllegalArgumentException(
					"result space and input matrix can not be the same matrix");
			BLAS.checkDestinationSize(result,matrix[0].length,matrix.length);
		}
		for(int i=0;i<matrix.length;i++)
			for(int j=0;j<matrix[i].length;j++)
				result[j][i]=matrix[i][j];
		return result;
	}
	
	/**
	 * perform the transpose
	 * @param matrix
	 * input matrix
	 * @param result
	 * space for transposed matrix, or null to allocate new space
	 * @return
	 */
	public static int[][] transpose(int[][] matrix,int[][] result)
	{
		if(result==null) result=new int[matrix[0].length][matrix.length];
		else
		{
			if(matrix==result) throw new IllegalArgumentException(
					"result space and input matrix can not be the same matrix");
			BLAS.checkDestinationSize(result,matrix[0].length,matrix.length);
		}
		for(int i=0;i<matrix.length;i++)
			for(int j=0;j<matrix[i].length;j++)
				result[j][i]=matrix[i][j];
		return result;
	}
	
	/**
	 * perform Hermitian transpose for a complex matrix
	 * @param matrix
	 * input matrix
	 * @param result
	 * space for transposed matrix, or null to allocate new space
	 * @return
	 */
	public static Complex[][] transpose(Complex[][] matrix,Complex[][] result)
	{
		if(result==null) result=new Complex[matrix[0].length][matrix.length];
		else
		{
			if(matrix==result) throw new IllegalArgumentException(
					"result space and input matrix can not be the same matrix");
			BLAS.checkDestinationSize(result,matrix[0].length,matrix.length);
		}
		for(int i=0;i<matrix.length;i++)
			for(int j=0;j<matrix[i].length;j++)
				result[j][i]=matrix[i][j].conjugate();
		return result;
	}
	
	/**
	 * get the complex conjugate
	 * @param matrix
	 * input complex matrix
	 * @param result
	 * space for result, null to allocate new space
	 * @return
	 */
	public static Complex[][] conjugate(Complex[][] matrix,Complex[][] result)
	{
		if(result==null) result=new Complex[matrix.length][matrix[0].length];
		else BLAS.checkSize(matrix,result);
		for(int i=0;i<matrix.length;i++)
			for(int j=0;j<matrix[i].length;j++) result[i][j]=matrix[i][j].conjugate();
		return result;
	}
	
	/**
	 * build complex matrix by its real part and imaginary part
	 * @param real
	 * the real part
	 * @param imag
	 * the real part
	 * @return
	 */
	public static Complex[][] buildComplexMatrix(double[][] real,double[][] imag)
	{
	Complex[][] matrix;
	
		BLAS.checkSize(real,imag);
		matrix=new Complex[real.length][real[0].length];
		for(int i=0;i<matrix.length;i++)
			for(int j=0;j<matrix[i].length;j++) 
				matrix[i][j]=new Complex(real[i][j],imag[i][j]);
		return matrix;
	}
	
	/**
	 * split a complex to get its real part and imaginary part
	 * @param matrix
	 * a complex matrix
	 * @return
	 * the first matrix is its real part, the second one is the imaginary part
	 */
	public static double[][][] splitComplexMatrix(Complex[][] matrix)
	{
	double[][][] result;
	
		result=new double[2][matrix.length][matrix[0].length];
		for(int i=0;i<matrix.length;i++)
			for(int j=0;j<matrix[i].length;j++)
			{
				result[0][i][j]=matrix[i][j].getReal();
				result[1][i][j]=matrix[i][j].getImaginary();
			}
		return result;
	}
	
	/**
	 * build complex vector from its real and imaginary part
	 * @param real
	 * its real part
	 * @param imag
	 * its imaginary part
	 * @return
	 */
	public static Complex[] buildComplexVector(double[] real,double[] imag)
	{
	Complex[] cv;
	
		BLAS.checkSize(real,imag);
		cv=new Complex[real.length];
		for(int i=0;i<cv.length;i++) cv[i]=new Complex(real[i],imag[i]);
		return cv;
	}
	
	/**
	 * get the magnitude of a complex vector
	 * @param v
	 * a complex vector
	 * @param result
	 * space for results, null to allocate new space
	 * @return
	 */
	public static double[] abs(Complex[] v,double[] result)
	{
		if(result==null) result=new double[v.length];
		else checkDestinationSize(result,v.length);
		for(int i=0;i<v.length;i++) result[i]=v[i].abs();
		return result;
	}
	
	/**
	 * get the magnitude of a complex matrix
	 * @param m
	 * a complex matrix
	 * @param result
	 * space for results, null to allocate new space
	 * @return
	 */
	public static double[][] abs(Complex[][] m,double[][] result)
	{
		if(result==null) result=new double[m.length][m[0].length];
		else checkDestinationSize(result,m.length,m[0].length);
		for(int i=0;i<result.length;i++)
			for(int j=0;j<result[i].length;j++) result[i][j]=m[i][j].abs();
		return result;
	}
	
	/**
	 * calculate |a|^2
	 * @param a
	 * a complex number
	 * @return
	 */
	public static double absSquare(Complex a)
	{
		return a.getReal()*a.getReal()+a.getImaginary()*a.getImaginary();
	}
	
	/**
	 * generate a random matrix of uniform distribution of [0, 1]
	 * @param row
	 * row dimension
	 * @param col
	 * column dimension
	 * @return
	 */
	public static double[][] randMatrix(int row,int col)
	{
	double[][] m;
	
		m=new double[row][col];
		for(int i=0;i<m.length;i++)
			for(int j=0;j<m[i].length;j++) m[i][j]=Math.random();
		return m;
	}
	
	/**
	 * generate a random complex matrix of uniform distribution of [0, 1]
	 * @param row
	 * row dimension
	 * @param col
	 * column dimension
	 * @return
	 */
	public static Complex[][] randComplexMatrix(int row,int col)
	{
	Complex[][] m;
	
		m=new Complex[row][col];
		for(int i=0;i<m.length;i++)
			for(int j=0;j<m[i].length;j++) m[i][j]=new Complex(Math.random(),Math.random());
		return m;
	}
	
	/**
	 * generate a random vector of uniform distribution of [0, 1]
	 * @param d
	 * vector dimension
	 * @return
	 */
	public static double[] randVector(int d)
	{
	double[] v;
	
		v=new double[d];
		for(int i=0;i<v.length;i++) v[i]=Math.random();
		return v;
	}
	
	/**
	 * generate a random complex vector of uniform distribution of [0, 1]
	 * @param d
	 * vector dimension
	 * @return
	 */
	public static Complex[] randComplexVector(int d)
	{
	Complex[] v;
	
		v=new Complex[d];
		for(int i=0;i<v.length;i++) v[i]=new Complex(Math.random(),Math.random());
		return v;
	}
	
	/**
	 * fill the matrix with [0, 1] uniformly destributed random values
	 * @param m
	 */
	public static void randomize(double[][] m)
	{
		for(int i=0;i<m.length;i++)
			for(int j=0;j<m[i].length;j++) m[i][j]=Math.random();
	}
	
	/**
	 * fill the vector with [0, 1] uniformly destributed random values
	 * @param v
	 */
	public static void randomize(double[] v)
	{
		for(int i=0;i<v.length;i++) v[i]=Math.random();
	}
	
	/**
	 * perform Gram-Schmidt orthogonalization on rows of the input matrix
	 * @param matrix
	 * a matrix
	 */
	public static void orthogonalize(double[][] matrix)
	{
	double[] temp=null;
		
		for(int i=0;i<matrix.length;i++)
		{
			//orthogonalization
			for(int j=0;j<i;j++)
			{
				temp=BLAS.scalarMultiply(BLAS.innerProduct(matrix[j],matrix[i]),matrix[j],temp);
				BLAS.substract(matrix[i],temp,matrix[i]);
			}
			//normalization
			BLAS.normalize(matrix[i]);
		}
	}
	
	/**
	 * perform Gram-Schmidt orthogonalization on rows of the input matrix
	 * @param matrix
	 * a matrix
	 */
	public static void orthogonalize(Complex[][] matrix)
	{
	Complex[] temp=null;
	
		for(int i=0;i<matrix.length;i++)
		{
			//orthogonalization
			for(int j=0;j<i;j++)
			{
				//<a,b>=<b,a>*, the order is important!!!
				temp=BLAS.scalarMultiply(BLAS.innerProduct(matrix[j],matrix[i]),matrix[j],temp);
				BLAS.substract(matrix[i],temp,matrix[i]);
			}
			//normalization
			BLAS.normalize(matrix[i]);	
		}
	}
	
	/**
	 * to see whteher two matrices are equal or not
	 * @param m1, m2
	 * two matrices
	 * @return
	 */
	public static boolean equals(int[][] m1,int[][] m2)
	{
		if(m1==null&&m2==null) return true;
		else if(m1==m2) return true;
		else if(m1==null||m2==null) return false;
		else if(m1.length!=m2.length||m1[0].length!=m2[0].length) return false;
		else
		{
			for(int i=0;i<m1.length;i++) 
				for(int j=0;j<m1[i].length;j++) 
					if(m1[i][j]!=m2[i][j]) return false;
		}
		
		return true;
	}

	public static void main(String[] args) throws IOException
	{
	double[][] m={{6,5,4},{0,0,7},{0,3,9}};
	double[][] inv;
	
//		m=loadDoubleMatrix(new File("/home/nay0648/m1.txt"));
		System.out.println("determinant: "+det(m));
		
		System.out.println();
		inv=inv(m,null);
		System.out.println("inverse matrix:");
		if(inv==null) System.out.println("none");
		else 
		{	
			System.out.println(toString(inv));
			System.out.println(toString(multiply(m,inv,null)));
		}
		
		System.out.println();
		System.out.println("complex vector normalization:");
		Complex[] cv;
		cv=BLAS.randComplexVector(4);
		System.out.println(BLAS.toString(cv));
		System.out.println(BLAS.toString(BLAS.innerProduct(cv,cv)));
		
		BLAS.normalize(cv);
		System.out.println(BLAS.toString(cv));
		System.out.println(BLAS.toString(BLAS.innerProduct(cv,cv)));
	}
}
