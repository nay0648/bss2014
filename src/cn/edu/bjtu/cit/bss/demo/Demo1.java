package cn.edu.bjtu.cit.bss.demo;
import java.io.*;
import javax.sound.sampled.*;
import cn.edu.bjtu.cit.bss.signalio.*;
import cn.edu.bjtu.cit.bss.*;
import cn.edu.bjtu.cit.bss.util.*;

/**
 * <h1>Description</h1>
 * Separate two wave files.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Dec 31, 2011 9:25:23 AM, revision:
 */
public class Demo1 implements Serializable
{
private static final long serialVersionUID=2771952911522430009L;

	public static void main(String[] args) throws IOException, UnsupportedAudioFileException
	{
	FDBSS fdbss;
	WaveSource[] sigs;
	WaveSource estw;
	
		/*
		 * get input channels
		 */
		sigs=new WaveSource[2];
		sigs[0]=new WaveSource(
				new File("data/rsm2_mA.wav"),
				//true to normalize signal magnitude to [-1, 1]
				true);
		sigs[1]=new WaveSource(new File("data/rsm2_mB.wav"),true);

		/*
		 * construct the frequency domain BSS algorithm
		 */
		fdbss=new FDBSS(
				//directory for intermediate data
				new File("temp"));
			
		fdbss.separate(sigs);//perform separation

		fdbss.outputEstimatedSignals(new File("."),sigs[0].audioFormat());//output wave file if needed

		for(WaveSource ch:sigs) ch.close();//close input channels
		
		//visualize spectrograms
		fdbss.visualizeSourceSpectrograms(1,fdbss.numSources());
		
		/*
		 * play estimated files
		 */
		estw=new WaveSource(new File("y0.wav"),true);
		Util.playAsAudio(estw.toArray((double[])null),estw.audioFormat().getSampleRate());
		estw.close();
		
		estw=new WaveSource(new File("y1.wav"),true);
		Util.playAsAudio(estw.toArray((double[])null),estw.audioFormat().getSampleRate());
		estw.close();
	}
}
