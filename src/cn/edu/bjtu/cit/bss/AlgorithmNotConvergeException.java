package cn.edu.bjtu.cit.bss;

/**
 * <h1>Description</h1>
 * Algorithm not converge.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: May 16, 2010 11:01:54 AM, revision:
 */
public class AlgorithmNotConvergeException extends RuntimeException
{
private static final long serialVersionUID=6988723780291064927L;

	public AlgorithmNotConvergeException()
	{}

	public AlgorithmNotConvergeException(String message)
	{
		super(message);
	}

	public AlgorithmNotConvergeException(String message,Throwable cause)
	{
		super(message,cause);
	}

	public AlgorithmNotConvergeException(Throwable cause)
	{
		super(cause);
	}
}
