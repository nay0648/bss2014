package cn.edu.bjtu.cit.bss.align;
import java.util.*;
import cn.edu.bjtu.cit.bss.*;

/**
 * <h1>Description</h1>
 * Make the permutation problem worse by random permutation, this is used 
 * for experiment purpose. The scaling ambiguity is not solved.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Sep 11, 2011 7:32:47 PM, revision:
 */
public class RandomAlign extends AlignPolicy
{
private static final long serialVersionUID=-6955294917473169143L;

	public RandomAlign()
	{}
	
	public void align(DemixingModel demixm)
	{
	int[] p;
	
		this.checkDemixingModel(demixm);
	
		for(int binidx=0;binidx<demixm.fftSize()/2+1;binidx++)
		{
			p=randPermutation();
			this.rearrange(demixm,binidx,1,p);
		}
	}
	
	/**
	 * generate random permutation
	 * @return
	 */
	private int[] randPermutation()
	{
	List<Integer> pl;
	int[] p;
	int ridx;
	
		pl=new LinkedList<Integer>();
		for(int chidx=0;chidx<this.numSources();chidx++) pl.add(chidx);
		
		p=new int[this.numSources()];
		for(int chidx=0;chidx<p.length;chidx++) 
		{
			ridx=(int)(Math.random()*pl.size());
			p[chidx]=pl.get(ridx);
			pl.remove(ridx);
		}
		
		return p;
	}
}
