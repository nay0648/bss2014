package cn.edu.bjtu.cit.bss.recorder;
import java.io.*;
import javax.sound.sampled.*;
import cn.edu.bjtu.cit.bss.signalio.*;

/**
 * <h1>Description</h1>
 * Used to read data from microphone for BSS.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Mar 11, 2012 8:24:40 PM, revision:
 */
public class CaptureChannel
{
private BSSRecorder recorder;//recorder reference
private CaptureDevice device;//underlying capture device
private double duration1=0.02;//time duration in seconds for buffer1
private double[] buffer1;//used to read data from line
private CircularBuffer buffer2;//used to cache the latest data
private RecordingThread rth=null;//used to read data

	/**
	 * <h1>Description</h1>
	 * Used to record data.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Mar 11, 2012 9:26:15 PM, revision:
	 */
	private class RecordingThread extends Thread
	{
		public void run()
		{
		int count;
		WaveSource wsource=null;

			try
			{
				/*
				 * Must open line first, then construct audio input 
				 * stream, or the default audio format will be used!!!
				 */
				line().open(audioFormat());
				wsource=new WaveSource(
						new AudioInputStream(device.line()),
						buffer1.length*audioFormat().getChannels()*audioFormat().getSampleSizeInBits()/Byte.SIZE,
						true);
				line().start();
				
				for(;recorder.getRecorderState()==RecorderState.CAPTURE;)
				{
					count=wsource.readSamples(buffer1);
					
					synchronized(buffer2)
					{
						buffer2.write(buffer1,0,count);
					}					
				}
			}
			catch(LineUnavailableException e)
			{
				e.printStackTrace();
				recorder.showErrorMessageDialog("Failed to capture sound.",e);
			}
			catch(IOException e)
			{
				e.printStackTrace();
				recorder.showErrorMessageDialog("Failed to capture sound.",e);
			}
			finally
			{
				line().stop();
				line().flush();//discard data leaving in the mix buffer
				line().close();
				try
				{
					if(wsource!=null) wsource.close();
				}
				catch(IOException e)
				{}
				
				rth=null;
			}
		}
	}

	/**
	 * @param recorder
	 * recorder reference
	 * @param device
	 * capture device
	 * @param buffer2
	 * circular buffer used to cache signal samples
	 */
	public CaptureChannel(BSSRecorder recorder,CaptureDevice device,CircularBuffer buffer2)
	{
		this.recorder=recorder;
		this.device=device;
		
		//preallocated buffer to make the capture start as quick as possible
		buffer1=new double[(int)(duration1*recorder.audioFormat().getSampleRate())];
		this.buffer2=buffer2;
	}
	
	/**
	 * get audio format
	 * @return
	 */
	public AudioFormat audioFormat()
	{
		return recorder.audioFormat();
	}
	
	/**
	 * get capture device
	 * @return
	 */
	public CaptureDevice device()
	{
		return device;
	}
	
	/**
	 * get the mixer
	 * @return
	 */
	public Mixer mixer()
	{
		return device.mixer();
	}
	
	/**
	 * get the line
	 * @return
	 */
	public TargetDataLine line()
	{
		return device.line();
	}
	
	/**
	 * get circular buffer size in number of samples
	 * @return
	 */
	public int bufferSize()
	{
		return buffer2.getBufferSize();
	}
	
	/**
	 * get the circular buffer
	 * @return
	 */
	public CircularBuffer circularBuffer()
	{
		return buffer2;
	}
	
	/**
	 * start capturing sound data from line
	 */
	public synchronized void startCapture()
	{
		if(rth!=null) return;//already started
		
		buffer2.clear();
		rth=new RecordingThread();
		rth.start();
	}
}
