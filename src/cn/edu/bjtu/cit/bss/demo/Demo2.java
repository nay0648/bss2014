package cn.edu.bjtu.cit.bss.demo;
import java.io.*;
import java.util.*;
import cn.edu.bjtu.cit.bss.eval.*;
import cn.edu.bjtu.cit.bss.signalio.*;
import cn.edu.bjtu.cit.bss.*;
import cn.edu.bjtu.cit.bss.FDBSS.Operation;
import cn.edu.bjtu.cit.bss.FDBSS.Parameter;

/**
 * <h1>Description</h1>
 * Perform separation evaluation.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Dec 31, 2011 9:39:49 AM, revision:
 */
public class Demo2 implements Serializable
{
private static final long serialVersionUID=7351365047677060915L;

	public static void main(String[] args) throws IOException
	{
	FDBSS fdbss;
	Evaluator evaluator;
	SignalMixer sources;
			
		fdbss=new FDBSS(new File("temp"));
			
		/*
		 * set parameters if needed
		 */
		fdbss.setParameter(Parameter.stft_size,"1024");//stft block size
		fdbss.setParameter(Parameter.stft_overlap,Integer.toString((int)(1024*7/8)));//stft overlap
		fdbss.setParameter(Parameter.fft_size,"2048");//fft size, must be powers of 2
		
		//try another ICA algorithm
		fdbss.setParameter(Parameter.ica_algorithm,"cn.edu.bjtu.cit.bss.ica.CFastICA");
		//changing preprocessing policy
		fdbss.setParameter(Parameter.preprocessor,"cn.edu.bjtu.cit.bss.preprocess.FOBI");
		//use different permutation policy
		fdbss.setParameter(Parameter.align_policy,"cn.edu.bjtu.cit.bss.align.DyadicSorting");

		evaluator=new Evaluator(fdbss.stfTransformer());//construct evaluator
		
		//add source files
		evaluator.addSources(
				"data/SawadaDataset/s1.wav",
				"data/SawadaDataset/s2.wav");

		//load a virtual room for mixing environment
		evaluator.setMixingFilters(new VirtualRoom(new File("data/VirtualRooms/2x2/SawadaRoom2x2.txt")));
	
		/*
		 * or use random generated filters to mix signals
		 */
		//set mixing filters from filter base
//		evaluator.setMixingFilters(
				//2 x 2 mixing
//				2,2,
				//filter taps
//				64,
				//filter indices in filter base
//				0,1,2,3);
				
		/*
		 * perform separation
		 */
		sources=evaluator.openSensorData();
		fdbss.separate(sources,Operation.stft,Operation.ica);
		fdbss.separate(sources,Operation.align,Operation.demix);
		sources.close();

		/*
		 * evaluate separation results
		 */
		System.out.println("input SIR: "+Arrays.toString(evaluator.inputSIR()));
		System.out.println("output SIR: "+Arrays.toString(evaluator.outputSIR(fdbss.loadDemixingModel())));
		System.out.println("SIR improvement: "+evaluator.sirImprovement(fdbss.loadDemixingModel()));
	}
}
