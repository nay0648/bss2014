package cn.edu.bjtu.cit.bss.eval;
import java.io.*;
//import java.util.*;
import java.awt.image.*;
//import javax.imageio.*;
import cn.edu.bjtu.cit.bss.*;
import cn.edu.bjtu.cit.bss.FDBSS.Operation;
import cn.edu.bjtu.cit.bss.align.*;
import cn.edu.bjtu.cit.bss.signalio.*;

/**
 * <h1>Description</h1>
 * Used to evaluate an align policy.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Oct 18, 2011 2:47:28 PM, revision:
 */
public class AlignPolicyEvaluator
{
private FDBSS bss;//the bss algorithm
private Evaluator evaluator;//the evaluator
private boolean stepsbeforealign=true;//do steps before align
private boolean stepsaligndemix=true;//do align and demix

	public AlignPolicyEvaluator()
	{
		bss=new FDBSS(new File("temp"));
		evaluator=new Evaluator(bss.stfTransformer());
	}
	
	/**
	 * initialize the experiment environment
	 * @throws IOException
	 */
	private void initEnv() throws IOException
	{
		/*
		 * add sources
		 */
		evaluator.addSources("/home/nay0648/data/research/paper/BSS/dataset/Sawadademo/4sources/s1.wav");
		evaluator.addSources("/home/nay0648/data/research/paper/BSS/dataset/Sawadademo/4sources/s2.wav");
//		evaluator.addSources("/home/nay0648/data/research/paper/BSS/dataset/Sawadademo/4sources/s3.wav");
//		evaluator.addSources("/home/nay0648/data/research/paper/BSS/dataset/Sawadademo/4sources/s4.wav");

//		evaluator.addSources("data/source2.wav");
//		evaluator.addSources("data/source3.wav");
//		evaluator.addSources("data/source4.wav");
//		evaluator.addSources("data/source5.wav");
//		evaluator.addSources("data/source9.wav");
		
//		evaluator.addSources("/home/nay0648/data/research/paper/BSS/dataset/testsource/source6_20s.wav");
//		evaluator.addSources("/home/nay0648/data/research/paper/BSS/dataset/testsource/source7_20s.wav");
//		evaluator.addSources("/home/nay0648/data/research/paper/BSS/dataset/testsource/source9_20s.wav");
//		evaluator.addSources("/home/nay0648/data/research/paper/BSS/dataset/testsource/source10_20s.wav");
		
		//set mixing filters
		evaluator.setMixingFilters(
				2,2,1,
				0,1,2,3);
//		evaluator.setMixingFilters(
//				3,3,1024,
//				0,1,2,3,4,5,6,7,8);
//		evaluator.setMixingFilters(
//				4,4,2048,
//				0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15);
//		evaluator.setMixingFilters(
//				5,5,1,
//				0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24);
	}

	/**
	 * draw spectrograms for all output channels
	 * @return
	 * @throws IOException
	 */
	public BufferedImage outputSpectrograms() throws IOException
	{
	BufferedImage[] imgs;
	
		imgs=new BufferedImage[bss.numSources()];
		for(int chidx=0;chidx<imgs.length;chidx++) 
			imgs[chidx]=bss.spectrogram(bss.estimatedSTFTFile(chidx));
		
		return pp.util.Util.drawResult(1,imgs.length,5,imgs);
	}
	
	/**
	 * evaluate a specified align policy
	 * @param policies
	 * align policy names
	 * @throws IOException
	 */
	public void evaluate(String... policies) throws IOException
	{
	SignalMixer sources;
	long time;
		
		//initialize
		initEnv();
		
		/*
		 * perform STFT, rearrange, ICA
		 */
		sources=evaluator.openSensorData();
		if(stepsbeforealign) 
			bss.separate(sources,Operation.stft,Operation.ica);
		
		//evaluate
		for(String policy:policies)
		{
			bss.setParameter(FDBSS.Parameter.align_policy,policy);//change align policy
			
			/*
			 * perform align and demix
			 */
			time=0;
			if(stepsaligndemix)
			{
				time=System.currentTimeMillis();
				bss.separate(sources,Operation.align);
				time=System.currentTimeMillis()-time;
			
				bss.separate(sources,Operation.demix);
			}
			
			/*
			 * calculate SIR
			 */
			System.out.println("align policy: "+policy);
//			System.out.println("input SIR: "+Arrays.toString(evaluator.inputSIR()));
//			System.out.println("output SIR: "+Arrays.toString(evaluator.outputSIR()));
			System.out.println("SIR improvement: "+evaluator.sirImprovement(bss.loadDemixingModel()));
			System.out.println("time spent: "+time+" ms");
			System.out.println();
			
			//output spectrograms
//			ImageIO.write(outputSpectrograms(),"png",new File("/home/nay0648/"+policy+".png"));
		}
		
		sources.close();
	}
	
	public static void main(String[] args) throws IOException
	{
	AlignPolicyEvaluator evaluator;
	
		evaluator=new AlignPolicyEvaluator();
//		evaluator.stepsbeforealign=false;
//		evaluator.stepsaligndemix=false;
		
//		evaluator.evaluate(SequentialCorrelation.class.getName());
//		evaluator.evaluate(DyadicSorting.class.getName());
//		evaluator.evaluate(RegionGrow.class.getName());
//		evaluator.evaluate(NJWAlign.class.getName());
//		evaluator.evaluate(SpectralOrdering.class.getName());
//		evaluator.evaluate(CWKKMeans.class.getName());
		
		evaluator.evaluate(
				RegionGrow.class.getName(),
				CWKKMeans.class.getName());
//				SpectralOrdering.class.getName(),
//				NJWAlign.class.getName(),
//				SequentialCorrelation.class.getName(),
//				DyadicSorting.class.getName());
	}
}
