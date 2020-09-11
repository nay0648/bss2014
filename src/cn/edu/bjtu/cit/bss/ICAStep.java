package cn.edu.bjtu.cit.bss;
import java.io.*;
import cn.edu.bjtu.cit.bss.preprocess.*;

/**
 * <h1>Description</h1>
 * ICA steps in frequency domain BSS, used to estimate demixing matrices in each frequency bin.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Mar 22, 2012 9:32:54 AM, revision:
 */
public abstract class ICAStep implements Serializable
{
private static final long serialVersionUID=4140847379597422339L;
private FDBSSAlgorithm fdbss;//the algorithm reference
private Preprocessor preprocessor=new Whitening();//used to preprocess bin-wise data
	
	/**
	 * get frequency domain bss algorithm
	 * @return
	 */
	public FDBSSAlgorithm getFDBSSAlgorithm()
	{
		return fdbss;
	}
	
	/**
	 * set fdbss algorithm reference
	 * @param fdbss
	 * new algorithm reference
	 */
	public void setFDBSSAlgorithm(FDBSSAlgorithm fdbss)
	{
		this.fdbss=fdbss;
	}
	
	/**
	 * get preprocessor for ica
	 * @return
	 */
	public Preprocessor preprocessor()
	{
		return preprocessor;
	}
	
	/**
	 * set preprocessor
	 * @param name
	 * preprocessor class name
	 */
	public void setPreprocessor(String name)
	{
		try
		{
			preprocessor=(Preprocessor)Class.forName(name).newInstance();
		}
		catch(InstantiationException e)
		{
			throw new RuntimeException("failed to instantiate preprocessor: "+name,e);
		}
		catch(IllegalAccessException e)
		{
			throw new RuntimeException("failed to instantiate preprocessor: "+name,e);
		}
		catch(ClassNotFoundException e)
		{
			throw new RuntimeException("failed to instantiate preprocessor: "+name,e);
		}
	}
	
	/**
	 * apply ICA on each frequency bin to get the demixing model
	 * @return
	 */
	public abstract DemixingModel applyICA();
}
