package cn.edu.bjtu.cit.bss.util;
import java.io.*;
import java.util.List;
import java.util.LinkedList;
import java.awt.*;
import java.awt.event.*;
import javax.sound.sampled.*;
import javax.swing.*;
import org.apache.commons.math.complex.*;
import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.data.*;
import org.jfree.data.general.*;
import org.jfree.data.xy.*;
import pp.util.BLAS;
import cn.edu.bjtu.cit.bss.signalio.*;

/**
 * <h1>Description</h1>
 * Used to show real time power spectrum.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Jan 24, 2011 9:57:01 PM, revision:
 */
public class RealTimePowerSpectrumViewer implements Serializable
{
private static final long serialVersionUID=8807886961138493296L;
private static double P0_SQUARE=2e-5*2e-5;//the basic sound pressure used to calculate sound level
private int nbits=16;//bits per sample
private double bufferduration=0.02;//buffer duration in seconds used to capture sound
private double fs;//sampling rate
private AudioFormat format;//underlying audio format
private ShortTimeFourierTransformer stft;
private PowerSpectrumDataset dataset;//the dataset
private JFreeChart chart;//the chart
private JPanel chartpanel;//the chart panel
private STFTThread stftth=null;//used to perform stft

	/**
	 * <h1>Description</h1>
	 * Used to store and plot power spectrum.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: May 13, 2012 4:21:28 PM, revision:
	 */
	private class PowerSpectrumDataset implements XYDataset
	{
	private List<DatasetChangeListener> llist=new LinkedList<DatasetChangeListener>();
	private double[] f;//the x coordinate for frequency
	private double[] power;//corresponding y coordinate
	private double alpha;//used to adjust power spectrum
	
		public PowerSpectrumDataset()
		{
		double df;
		
			df=fs/stft.fftSize();
			f=new double[stft.fftSize()/2+1];
			for(int i=0;i<f.length;i++) f[i]=df*i;
			
			power=new double[f.length];
			
			//energy conservation between time domain and frequency domain
			alpha=1.0/stft.fftSize();
			//cancel the window overlapping effect
			alpha*=2.0*(stft.stftSize()-stft.stftOverlap())/stft.stftSize();
			//average energy for one sample, so energy will not change with signal length
			alpha*=1.0/stft.fftSize();
		}
		
		public int getSeriesCount()
		{
			return 1;
		}

		@SuppressWarnings("rawtypes")
		public Comparable getSeriesKey(int arg0)
		{
			if(arg0==0) return "Power Spectrum";
			else throw new IndexOutOfBoundsException(1+", "+arg0);
		}

		@SuppressWarnings("rawtypes")
		public int indexOf(Comparable arg0)
		{
			if("Power Spectrum".equals(arg0)) return 0;
			else return -1;
		}

		public void addChangeListener(DatasetChangeListener arg0)
		{
			llist.add(arg0)	;		
		}
		
		public void removeChangeListener(DatasetChangeListener arg0)
		{
			llist.remove(arg0);
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

		public int getItemCount(int arg0)
		{
			return f.length;
		}

		public Number getX(int arg0,int arg1)
		{
			if(arg0==0) return f[arg1];
			else throw new IndexOutOfBoundsException(1+", "+arg0);
		}

		public double getXValue(int arg0,int arg1)
		{
			return getX(arg0,arg1).doubleValue();
		}

		public Number getY(int arg0,int arg1)
		{
			if(arg0==0) return power[arg1];
			else throw new IndexOutOfBoundsException(1+", "+arg0);
		}

		public double getYValue(int arg0,int arg1)
		{
			return getY(arg0,arg1).doubleValue();
		}
		
		/**
		 * update dataset
		 * @param fx
		 * fft segment
		 */
		public void update(Complex[] fx)
		{
		DatasetChangeEvent event;
			
			if(fx.length!=stft.fftSize()) throw new IllegalArgumentException(
					"fft size not match: "+fx.length+", "+stft.stftSize());
		
			power[0]=val2dB(BLAS.absSquare(fx[0])*alpha);//the DC part
			power[power.length-1]=val2dB(BLAS.absSquare(fx[power.length-1])*alpha);//the highest frequency
			//two halves for other parts
			for(int i=1;i<power.length-1;i++) 
				power[i]=val2dB(2*BLAS.absSquare(fx[i])*alpha);
			
			/*
			 * notify dataset changed
			 */
			event=new DatasetChangeEvent(this,this);
			for(DatasetChangeListener l:llist) l.datasetChanged(event);
		}
		
		/**
		 * convert to db
		 * @param val
		 * @return
		 */
		private double val2dB(double val)
		{
			if(val<P0_SQUARE) val=P0_SQUARE;
			return 10*Math.log10(val/P0_SQUARE);
		}
	}
	
	public RealTimePowerSpectrumViewer(double fs,int stftsize,int stftoverlap,int fftsize) throws LineUnavailableException
	{		
		this.fs=fs;
		stft=new ShortTimeFourierTransformer(stftsize,stftoverlap,fftsize,null);
		format=new AudioFormat((float)fs,nbits,1,true,false);
		
		initGUI();
		
		stftth=new STFTThread();
		stftth.start();
	}
	
	/**
	 * initialize the chart gui
	 */
	private void initGUI()
	{
	XYPlot plot;
		
		dataset=new PowerSpectrumDataset();

		//the chart
		chart = ChartFactory.createXYLineChart(
				null,// chart title
				"Frequency (Hz)",// x axis label
				"Magnitude (dB)",// y axis label
				dataset,// data
				PlotOrientation.VERTICAL,
				false,// include legend
				true,// tooltips
				false// urls
				);
		
		/*
		 * set frequency range
		 */
		plot=(XYPlot)chart.getPlot();
		plot.getDomainAxis().setLowerMargin(0);
		plot.getDomainAxis().setUpperMargin(0);
		
		plot.getRangeAxis().setRange(0,90);
		
		/*
		 * the chart panel
		 */
		chartpanel=new JPanel();
		chartpanel.setLayout(new BorderLayout(5,5));
		chartpanel.add(new ChartPanel(chart,true),BorderLayout.CENTER);
		
		JFrame frame=new JFrame("Real Time Power Spectrum Viewer");
		frame.addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent e)
			{
				System.exit(0);
			}
		});
		
		frame.getContentPane().add(chartpanel,BorderLayout.CENTER);
		
		frame.pack();
		DisplayMode dmode=GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode();
		frame.setLocation(
				Math.max((dmode.getWidth()-frame.getWidth())/2,0),
				Math.max((dmode.getHeight()-frame.getHeight())/2,0));
		frame.setVisible(true);	
	}
	
	/**
	 * <h1>Description</h1>
	 * Used to perform stft.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Jan 24, 2011 10:03:25 PM, revision:
	 */
	private class STFTThread extends Thread
	{
	private boolean capture=true;//true for capture sound
	
		public void run()
		{
		TargetDataLine line=null;
		WaveSource source=null;
		ShortTimeFourierTransformer.STFTIterator stftit;
		
			try
			{
				line=AudioSystem.getTargetDataLine(format);
				source=new WaveSource(
						new AudioInputStream(line),
						(int)(bufferduration*fs*format.getChannels()*format.getSampleSizeInBits()/Byte.SIZE),
						true);
				
				stftit=stft.stftIterator(source);
				line.open();
				line.start();
				
				for(;capture&&stftit.hasNext();)
					dataset.update(stftit.next());
			}
			catch(IOException e)
			{
				throw new RuntimeException("failed to perform STFT: ",e);
			}
			catch(LineUnavailableException e)
			{
				throw new RuntimeException("failed to perform STFT: ",e);
			}
			finally
			{
				if(line!=null) 
				{
					line.stop();
					line.close();
				}
				
				try
				{
					if(source!=null) source.close();
				}
				catch(IOException e)
				{}
			}
		}
	}
	
	public static void main(String[] args) throws IOException, UnsupportedAudioFileException, LineUnavailableException
	{
	double fs=44100;
	int stftsize=3000;
	int stftoverlap=stftsize*1/2;
	int fftsize=4096;
	
		new RealTimePowerSpectrumViewer(fs,stftsize,stftoverlap,fftsize);
	}
}
