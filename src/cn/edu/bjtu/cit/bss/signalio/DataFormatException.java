package cn.edu.bjtu.cit.bss.signalio;

/**
 * <h1>Description</h1>
 * For signal data format exception.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Jun 1, 2011 4:04:33 PM, revision:
 */
public class DataFormatException extends RuntimeException
{
private static final long serialVersionUID=-140544613059813529L;

	public DataFormatException()
	{}

	public DataFormatException(String message)
	{
		super(message);
	}

	public DataFormatException(String message,Throwable cause)
	{
		super(message,cause);
	}

	public DataFormatException(Throwable cause)
	{
		super(cause);
	}
}
