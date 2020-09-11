package cn.edu.bjtu.cit.bss.recorder;
import java.awt.Color;
import java.awt.Paint;
import java.io.*;
import java.util.*;
import javax.sound.sampled.*;
import org.jfree.chart.*;
import org.jfree.data.*;
import org.jfree.data.general.*;
import org.jfree.data.xy.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.entity.*;
import cn.edu.bjtu.cit.bss.signalio.*;

/**
 * <h1>Description</h1>
 * Used to build and update playback chart panel.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Mar 15, 2012 8:43:33 AM, revision:
 */
public class PlaybackChartManager
{
private static final Color SELECTED_SUBPLOT_BG=Color.BLACK;//selectd subplot background color
private static final Color MARKER_COLOR=Color.GREEN;//color for the marker
private static final int VIEW_SIZE=2000;//number of samples in view
private BSSRecorder recorder;//recorder reference
private int buffersize;//buffer size in number of samples
private List<CircularBuffer> bufferlist=new LinkedList<CircularBuffer>();//underlying buffers
private List<CircularBufferView> datasetlist=new LinkedList<CircularBufferView>();//dataset list
private List<XYPlot> subplotlist=new LinkedList<XYPlot>();//contains all subplots
private Paint defplotbg;//default plot background color
private CombinedDomainXYPlot cplot;//the combined plot
private JFreeChart chart;//the chart
private ChartPanel chartpanel;//the chart panel
private int selectedchidx=-1;//selected channel index, -1 means no one is selected
private PlaybackThread pth=null;//thread used for playback

	/**
	 * @param recorder
	 * recorder reference
	 * @param buffersize
	 * buffer size in number of samples
	 */
	public PlaybackChartManager(BSSRecorder recorder,int buffersize)
	{
		this.recorder=recorder;
		this.buffersize=buffersize;
	
		/*
		 * the combined plot
		 */
		cplot=new CombinedDomainXYPlot(new NumberAxis("Time (Second)"));
		cplot.setGap(5);
		cplot.getDomainAxis().setRange(0,buffersize/recorder.audioFormat().getSampleRate());
		cplot.getDomainAxis().setAutoRange(false);
//		cplot.getDomainAxis().setVisible(false);

		/*
		 * the combined chart
		 */
		chart=new JFreeChart(cplot);
		chart.removeLegend();
		
		/*
		 * construct the chart panel
		 */
		chartpanel=new ChartPanel(chart,true);//with buffer		
		chartpanel.addChartMouseListener(new ChartMouseListener(){
			public void chartMouseClicked(ChartMouseEvent e)
			{
				selectChannel(e.getEntity());
			}

			public void chartMouseMoved(ChartMouseEvent e)
			{}
		});
	}
	
	/**
	 * get recorder reference
	 * @return
	 */
	public BSSRecorder recorder()
	{
		return recorder;
	}
	
	/**
	 * get the chart panel
	 * @return
	 */
	public ChartPanel chartPanel()
	{
		return chartpanel;
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
	 * get buffer size in number of samples
	 * @return
	 */
	public int getBufferSize()
	{
		return buffersize;
	}
	
	/**
	 * set buffersize in number of samples
	 * @param size
	 * new buffer size
	 */
	public void setBufferSize(int size)
	{
		if(size==buffersize) return;
		
		buffersize=size;
		for(CircularBuffer buffer:bufferlist) buffer.setBufferSize(size);
		
		cplot.getDomainAxis().setRange(0,buffersize/audioFormat().getSampleRate());
		
		updateChart();
	}
	
	/**
	 * <h1>Description</h1>
	 * Used for wave form presentation.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Apr 8, 2012 8:12:17 PM, revision:
	 */
	private class CircularBufferView implements XYDataset
	{
	private static final long serialVersionUID=4132458729752711464L;
	private CircularBuffer buffer;//underlying buffer
	private List<DatasetChangeListener> listener=new LinkedList<DatasetChangeListener>();
	
		/**
		 * @param buffer
		 * underlying data buffer
		 */
		public CircularBufferView(CircularBuffer buffer)
		{
			this.buffer=buffer;
		}
		
		/**
		 * get underlying circular buffer
		 * @return
		 */
		public CircularBuffer circularBuffer()
		{
			return buffer;
		}
		
		public int getSeriesCount()
		{
			return 1;
		}

		@SuppressWarnings("rawtypes")
		public Comparable getSeriesKey(int arg0)
		{
			return "Channel 0";
		}

		@SuppressWarnings("rawtypes")
		public int indexOf(Comparable arg0)
		{
			if(getSeriesKey(0).equals(arg0)) return 0;
			else return -1;
		}

		public void addChangeListener(DatasetChangeListener arg0)
		{
			listener.add(arg0);
		}
		
		public void removeChangeListener(DatasetChangeListener arg0)
		{
			listener.remove(arg0);
		}

		public DatasetGroup getGroup()
		{
			return null;
		}

		public void setGroup(DatasetGroup arg0)
		{}

		public DomainOrder getDomainOrder()
		{
			return DomainOrder.ASCENDING;
		}
		
		/**
		 * get number of samples in the dataset
		 * @return
		 */
		public int viewSize()
		{
			if(buffer.getBufferSize()<VIEW_SIZE) return buffer.getBufferSize();
			else return VIEW_SIZE;
		}

		public int getItemCount(int arg0)
		{
			return viewSize();
		}
		
		/**
		 * get original sample index in buffer
		 * @param viewidx
		 * sample index in view
		 * @return
		 */
		private int originalIndex(int viewidx)
		{
		int oidx;
		
			if(viewidx<0||viewidx>=viewSize()) 
				throw new ArrayIndexOutOfBoundsException(viewidx+", "+viewSize());
			
			oidx=(int)Math.round((double)buffer.getBufferSize()*viewidx/viewSize());
			if(oidx<0) oidx=0;
			else if(oidx>buffer.getBufferSize()-1) oidx=buffer.getBufferSize()-1;
			
			return oidx;
		}

		public Number getX(int arg0,int arg1)
		{
			return (double)originalIndex(arg1)/audioFormat().getSampleRate();
		}

		public double getXValue(int arg0,int arg1)
		{
			return getX(arg0,arg1).doubleValue();
		}

		public Number getY(int arg0,int arg1)
		{
			return buffer.getValue(originalIndex(arg1));
		}

		public double getYValue(int arg0,int arg1)
		{
			return getY(arg0,arg1).doubleValue();
		}
		
		/**
		 * send dataset changed event
		 */
		public void fireDatasetChanged()
		{
		DatasetChangeEvent e;
		
			e=new DatasetChangeEvent(this,this);
			for(DatasetChangeListener l:new LinkedList<DatasetChangeListener>()) 
				l.datasetChanged(e);
		}
	}
			
	/**
	 * add a new channel into the end
	 * @return
	 * underlying buffer of the new added channel
	 */
	public CircularBuffer addChannel()
	{
	XYPlot plot;
	CircularBuffer buffer;
	CircularBufferView dataset;
	
		/*
		 * add new buffer
		 */
		buffer=new CircularBuffer(buffersize);
		bufferlist.add(buffer);
		
		/*
		 * add new subplot
		 */
		dataset=new CircularBufferView(buffer);
		datasetlist.add(dataset);
		
		plot=new XYPlot(
				dataset,
				null,
				new NumberAxis("Magnitude"),
				new StandardXYItemRenderer());
		defplotbg=plot.getBackgroundPaint();//save default background color
		plot.getRangeAxis().setRange(-1,1);
		plot.getRangeAxis().setAutoRange(false);
		plot.getRangeAxis().setVisible(false);
		subplotlist.add(plot);
		cplot.add(plot);

		return buffer;
	}
		
	/**
	 * remove a channel
	 * @param chidx
	 * channel index
	 * @return
	 * removed channel buffer
	 */
	public CircularBuffer removeChannel(int chidx)
	{
	CircularBuffer buffer;
	
		//update selected channel index if needed
		if(selectedchidx>chidx) selectedchidx--;
		else if(selectedchidx==chidx) selectedchidx=-1;

		buffer=bufferlist.remove(chidx);
		datasetlist.remove(chidx);
		cplot.remove(subplotlist.remove(chidx));
		
		return buffer;
	}
	
	/**
	 * clear all channels from chart
	 */
	public void clearChannels()
	{
		selectedchidx=-1;
		
		for(XYPlot p:subplotlist) cplot.remove(p);
		subplotlist.clear();
		datasetlist.clear();
		bufferlist.clear();
	}
	
	/**
	 * get the number of channels in the chart
	 * @return
	 */
	public int getNumChannels()
	{
		return bufferlist.size();
	}
	
	/**
	 * set number of channels
	 * @param numch
	 * number of channels
	 */
	public void setNumChannels(int numch)
	{
	int numch0;
	
		if(numch<0) throw new IllegalArgumentException("illegal number of channels: "+numch);
	
		numch0=getNumChannels();//number of channels before change
		if(numch==numch0) return;
		//add channels
		else if(numch>numch0) for(int i=0;i<(numch-numch0);i++) addChannel();
		//remove channels
		else for(int i=0;i<(numch0-numch);i++) removeChannel(getNumChannels()-1);
	}
		
	/**
	 * select or unselect channel by the entity clicked by mouse
	 * @param entity
	 * entity clicked by mouth
	 */
	private void selectChannel(ChartEntity entity)
	{
		//click on plot
		if((entity instanceof PlotEntity)) 
			selectChannel(subplotlist.indexOf(((PlotEntity)entity).getPlot()));		
		//click on curve
		else if((entity instanceof XYItemEntity)) 
		{
		CircularBufferView dataset;
		
			/*
			 * find channel index by series
			 */
			dataset=(CircularBufferView)((XYItemEntity)entity).getDataset();
			selectChannel(bufferlist.indexOf(dataset.circularBuffer()));
		}
	}

	/**
	 * select a channel for playback, select the same channel twice to cancel the selection
	 * @param chidx
	 * selected channel index -1 to cancel all selection
	 */
	public void selectChannel(int chidx)
	{
		//cancel all selection
		if(chidx<0) 
		{
			for(XYPlot p:subplotlist) p.setBackgroundPaint(defplotbg);
			selectedchidx=-1;
		}
		else
		{
			//cancel previous selection
			if(selectedchidx>=0) subplotlist.get(selectedchidx).setBackgroundPaint(defplotbg);
			//new selection
			if(chidx==selectedchidx) selectedchidx=-1;
			else
			{
				subplotlist.get(chidx).setBackgroundPaint(SELECTED_SUBPLOT_BG);
				selectedchidx=chidx;
			}
		}
	}
	
	/**
	 * get selected channel index for playback
	 * @return
	 * -1 means no one is selected
	 */
	public int selectedChannelIndex()
	{
		return selectedchidx;
	}
	
	/**
	 * get a channel buffer
	 * @param idx
	 * channel index
	 * @return
	 */
	public CircularBuffer channelBuffer(int idx)
	{
		return bufferlist.get(idx);
	}
	
	/**
	 * find channel by circular buffer
	 * @param buffer
	 * buffer for a channel
	 * @return
	 * -1 if not found
	 */
	public int channelIndex(CircularBuffer buffer)
	{
		return bufferlist.indexOf(buffer);
	}
	
	/**
	 * set underlying signal data
	 * @param data
	 * each row for a channel
	 */
	public void setData(double[][] data)
	{
		buffersize=data[0].length;//buffersize also changed
		
		setNumChannels(data.length);//set new channel number
		//set data
		for(int chidx=0;chidx<data.length;chidx++) channelBuffer(chidx).setData(data[chidx]);
		
		clearMarkers();
		updateChart();
	}
	
	/**
	 * update chart according to buffered data
	 */
	public void updateChart()
	{
		/*
		 * !!!
		 * still not synchronized
		 */
		for(CircularBufferView ds:datasetlist) ds.fireDatasetChanged();	
		chart.fireChartChanged();
	}
	
	/**
	 * clear all markers
	 */
	public void clearMarkers()
	{
		for(XYPlot plot:subplotlist) plot.clearDomainMarkers();
	}
	
	/**
	 * <h1>Description</h1>
	 * Thread used to playback sounds.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Mar 11, 2012 9:39:08 PM, revision:
	 */
	private class PlaybackThread extends Thread
	{
	private XYPlot plot;//selected plot
	private CircularBuffer buffer;//the sample buffer
	private Clip clip;//used to play back sounds
	
		public PlaybackThread()
		{
		int chidx;
		
			chidx=selectedChannelIndex();
			if(chidx<0) 
			{
				plot=null;
				buffer=null;
			}
			else 
			{
				plot=subplotlist.get(chidx);
				buffer=bufferlist.get(chidx);
			}
		}
		
		/**
		 * get cached signal as bytes, used for playback
		 * @param buffer
		 * buffer for signal samples
		 * @return
		 */
		public byte[] toByteArray(CircularBuffer buffer)
		{
		ByteArrayOutputStream bout=null;
		PCMSink sink=null;
		byte[] data=null;
		
			try
			{
				bout=new ByteArrayOutputStream(buffer.getBufferSize()*audioFormat().getSampleSizeInBits()/Byte.SIZE);
				sink=new PCMSink(audioFormat(),bout);
			
				synchronized(buffer)
				{
					for(double s:buffer) sink.writeSample(s);
				}
				sink.flush();
				
				data=bout.toByteArray();
			}
			catch(IOException e)
			{
				e.printStackTrace();
				recorder.showErrorMessageDialog("Failed to convert signal into bytes.",e);
			}
			finally
			{
				try
				{
					if(sink!=null) sink.close();
				}
				catch(IOException e)
				{}
			}
			
			return data;
		}
	
		public void run()
		{
		byte[] data;
		int position=0;
		ValueMarker marker;
		
			try
			{				
				if(buffer!=null)
				{
					data=toByteArray(buffer);//get data from channel
				
					clip=AudioSystem.getClip();
					clip.open(audioFormat(),data,0,data.length);
					clip.start();
					
					/*
					 * show playback position
					 */
					plot.clearDomainMarkers();
					marker=new ValueMarker(0);
					marker.setPaint(MARKER_COLOR);
					plot.addDomainMarker(marker);
					
					for(;recorder.getRecorderState()==RecorderState.PLAYBACK&&position<buffer.getBufferSize();)
					{
						position=clip.getFramePosition();						
						marker.setValue(position/audioFormat().getSampleRate());

						try
						{
							Thread.sleep(100);
						}
						catch(InterruptedException e)
						{}
					}
				}
				
				stopPlayback();
			}
			catch(LineUnavailableException e)
			{
				e.printStackTrace();
				recorder.showErrorMessageDialog("Failed to playback.",e);
			}
		}
		
		/**
		 * stop playback
		 */
		public void stopPlayback()
		{
			if(recorder.getRecorderState()==RecorderState.STOP) return;//already stopped by the "STOP" button

			if(clip!=null)
			{
				clip.stop();
				clip.close();
			}
			
			pth=null;
			recorder.setRecorderState(RecorderState.STOP);
		}
	}
	
	/**
	 * start playback
	 */
	public synchronized void startPlayback()
	{	
		if(pth!=null) return;//already started
	
		pth=new PlaybackThread();
		pth.start();
	}
	
	/**
	 * stop playback
	 */
	public synchronized void stopPlayback()
	{
		if(pth==null) return;//already stoped
		
		pth.stopPlayback();
		pth=null;
	}
	
	/**
	 * used to read cached for BSS
	 * @return
	 */
	public SignalSource bufferSignalSource()
	{
		return new CircularBufferSource(bufferlist);
	}
}
