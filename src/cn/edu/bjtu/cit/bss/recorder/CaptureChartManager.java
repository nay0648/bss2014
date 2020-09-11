package cn.edu.bjtu.cit.bss.recorder;
import java.util.*;

/**
 * <h1>Description</h1>
 * Used to construct and control recorder chart.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Mar 15, 2012 9:36:45 AM, revision:
 */
public class CaptureChartManager extends PlaybackChartManager
{
private long cupdatesleep=100;//sleep time in ms for capture updating
//binded channels
private Map<CircularBuffer,CaptureChannel> cmap=new HashMap<CircularBuffer,CaptureChannel>();
private CaptureUpdatingThread cuth=null;//used for updating the chart

	/**
	 * @param recorder
	 * recorder reference
	 * @param buffersize
	 * buffer size in number of samples
	 */
	public CaptureChartManager(BSSRecorder recorder,int buffersize)
	{
		super(recorder,buffersize);
	}

	/**
	 * bind a channel to a capture device
	 * @param chidx
	 * channel index
	 * @param device
	 * capture device, null to cancel bind
	 */
	public void bindCaptureDevice(int chidx,CaptureDevice device)
	{
	CircularBuffer buffer;
	CaptureChannel cch;
	
		buffer=this.channelBuffer(chidx);
		
		if(device==null) cmap.remove(buffer);
		else
		{
			cch=new CaptureChannel(this.recorder(),device,buffer);
			cmap.put(buffer,cch);
		}
	}
		
	/**
	 * get all binded capture devices
	 * @return
	 * each channel for an entry, null means no device is binded
	 */
	public CaptureDevice[] bindedCaptureDevices()
	{
	CaptureDevice[] dev;
	int chidx;
	
		dev=new CaptureDevice[this.getNumChannels()];
		
		for(CircularBuffer buffer:cmap.keySet())
		{
			chidx=this.channelIndex(buffer);
			if(chidx>=0) dev[chidx]=cmap.get(buffer).device();
		}
		
		return dev;
	}
	
	public CircularBuffer removeChannel(int chidx)
	{
	CircularBuffer buffer;
	
		buffer=super.removeChannel(chidx);
		cmap.remove(buffer);
		return buffer;
	}
	
	public void clearChannels()
	{
		super.clearChannels();
		cmap.clear();
	}
	
	/**
	 * clear all binded capture devices
	 */
	public void clearBindedCaptureDevices()
	{
		cmap.clear();
	}
	
	/**
	 * <h1>Description</h1>
	 * Used to updating the chart while capturing.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Mar 12, 2012 3:41:47 PM, revision:
	 */
	private class CaptureUpdatingThread extends Thread
	{
		public void run()
		{
			//clear all markers left by playback
			CaptureChartManager.this.clearMarkers();
			
			for(;CaptureChartManager.this.recorder().getRecorderState()==RecorderState.CAPTURE;)
			{
				
//				System.out.println("capture updating");

				CaptureChartManager.this.updateChart();

				//sleep some interval
				try
				{
					Thread.sleep(cupdatesleep);
				}
				catch(InterruptedException e)
				{}
			}
			
			cuth=null;
		}
	}
	
	/**
	 * start capturing sound
	 */
	public synchronized void startCapture()
	{
		if(cuth!=null) return;//already started
		
		//start capturing sound
		for(CaptureChannel ch:cmap.values()) ch.startCapture();
		
		/*
		 * start updating the chart
		 */
		cuth=new CaptureUpdatingThread();
		cuth.start();
	}
}
