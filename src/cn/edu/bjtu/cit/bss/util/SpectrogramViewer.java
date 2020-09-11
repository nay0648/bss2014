package cn.edu.bjtu.cit.bss.util;
import java.io.*;
import java.util.List;
import java.util.LinkedList;
import java.text.*;
import java.awt.*;
import java.awt.event.*;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.*;
import org.apache.commons.math.complex.*;
import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.data.xy.*;
import org.jfree.chart.renderer.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.chart.title.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.entity.*;
import org.jfree.ui.*;
import pp.util.BLAS;
import cn.edu.bjtu.cit.bss.signalio.*;
import cn.edu.bjtu.cit.bss.util.ShortTimeFourierTransformer.STFTIterator;

/**
 * <h1>Description</h1>
 * Used to view signal spectrogram.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Mar 16, 2012 4:47:01 PM, revision:
 */
public class SpectrogramViewer implements Serializable
{
private static final long serialVersionUID=-3511026273669376826L;
private static double EPS=2.2204e-16;//a small positive number to prevent -inf dB
//private static double P0SQUARE=2e-5*2e-5;//p0 is 2e-5 pa for sound pressure
private ShortTimeFourierTransformer stft;//used to perform stft
private double fs;//sampling rate
private MatrixSeries data;//spectrogram data
private double[] xaxisval;//values for x axis
private double minval=Double.MAX_VALUE,maxval=Double.MIN_VALUE;//min value and max value
private Color[] colormap=ShortTimeFourierTransformer.colormapJet();//colormap used to draw spectrogram
private JFreeChart chart;//the spectrogram chart
private JPanel spectropanel;//panel for spectrogram
private JLabel lt,lf,lm;//show coordinate
/*
 * used to format coordinate text
 */
private DecimalFormat tformat=new DecimalFormat("0.000");
private DecimalFormat fformat=new DecimalFormat("0.00");
private DecimalFormat mformat=new DecimalFormat("0.00");

	/**
	 * @param stft
	 * used to perform STFT
	 * @param source
	 * signal source
	 * @param fs
	 * sampling rate
	 * @throws IOException
	 */
	public SpectrogramViewer(ShortTimeFourierTransformer stft,SignalSource source,double fs) throws IOException
	{
	List<Complex[]> stftdata;//each element is a stft frame
	List<Double> timelist;//time for each frame
	int tidx;
	double db;
	
		this.stft=stft;
		this.fs=fs;
		
		/*
		 * perform stft
		 */
		stftdata=new LinkedList<Complex[]>();
		timelist=new LinkedList<Double>();
		for(STFTIterator it=stft.stftIterator(source);it.hasNext();) 
		{
			stftdata.add(it.next());
			timelist.add(it.sampleIndex()/fs);
		}
			
		/*
		 * get spectrogram data
		 */
		data=new MatrixSeries("Spectrogram",stft.fftSize()/2+1,stftdata.size());

		tidx=0;
		for(Complex[] frame:stftdata) 
		{	
			for(int fidx=0;fidx<data.getRowCount();fidx++) 
			{
				db=val2db(frame[fidx]);
				data.update(fidx,tidx,db);
				
				if(db<minval) minval=db;
				if(db>maxval) maxval=db;
			}
			
			tidx++;
		}
		
		/*
		 * the x axis value
		 */
		xaxisval=new double[timelist.size()];
		tidx=0;
		for(double t:timelist) xaxisval[tidx++]=t;
		
		initSpectrogramPanel();//build chart panel
	}
	
	/**
	 * build panel for spectrogram
	 */
	private void initSpectrogramPanel()
	{
		/*
		 * build spectrogram plot
		 */
		SpectrogramPaintScale paintscale=new SpectrogramPaintScale();
		XYBlockRenderer renderer=new XYBlockRenderer();
		renderer.setPaintScale(paintscale);
		/*
		 * !!!
		 * This must be set, the default value is CENTER, and will cause 
		 * misalign because the block size is small.
		 */
		renderer.setBlockAnchor(RectangleAnchor.BOTTOM_LEFT);
		
		NumberAxis taxis=new NumberAxis("Time (Second)");
		taxis.setRange(xaxisval[0],xaxisval[xaxisval.length-1]);
		taxis.setAutoRange(false);
			
		NumberAxis faxis=new NumberAxis("Frequency (Hz)");
		faxis.setRange(0,fs/2.0);
		faxis.setAutoRange(false);
				
		XYPlot plot=new XYPlot(new SpectrogramDataset(data),taxis,faxis,renderer);

		/*
		 * build the chart
		 */
		chart=new JFreeChart(plot);
		chart.setBackgroundPaint(Color.WHITE);

		chart.removeLegend();
		NumberAxis laxis=new NumberAxis("Magnitude (dB)");
		PaintScaleLegend legend=new PaintScaleLegend(paintscale,laxis);
		legend.setPadding(5,5,5,5);
		legend.setBackgroundPaint(chart.getBackgroundPaint());
		legend.setPosition(RectangleEdge.RIGHT);
		chart.addSubtitle(legend);
			
		/*
		 * build chart panel
		 */
		ChartPanel chartpanel=new ChartPanel(chart,true);		
		chartpanel.addChartMouseListener(new ChartMouseListener(){

			public void chartMouseClicked(ChartMouseEvent e)
			{}

			public void chartMouseMoved(ChartMouseEvent e)
			{
			ChartEntity entity;

				entity=e.getEntity();
				if((entity instanceof XYItemEntity)) 
				{
					showCoordinate(((XYItemEntity)entity).getItem());
				}
			}
		});
		
		/*
		 * build spectrogram panel
		 */
		spectropanel=new JPanel();
		spectropanel.setLayout(new BorderLayout(5,5));
		spectropanel.add(chartpanel,BorderLayout.CENTER);
		
		/*
		 * control panel
		 */
		JPanel pctrl=new JPanel();
		pctrl.setBorder(new TitledBorder(""));
		pctrl.setLayout(new FlowLayout(FlowLayout.LEFT,5,5));
		spectropanel.add(pctrl,BorderLayout.SOUTH);
		
		/*
		 * show coordinate
		 */
		JPanel pcoor=new JPanel();
		pcoor.setLayout(new GridLayout(3,1));
		pctrl.add(pcoor);
		lt=new JLabel("Time (s): ");
		pcoor.add(lt);
		lf=new JLabel("Frequency (Hz): ");
		pcoor.add(lf);
		lm=new JLabel("Magnitude (dB): ");
		pcoor.add(lm);
		
		/*
		 * colormap panel
		 */
		JPanel pcolormap=new JPanel();
		pcolormap.setBorder(new TitledBorder(""));
		pcolormap.setLayout(new FlowLayout(FlowLayout.LEFT,5,5));
		spectropanel.add(pcolormap,BorderLayout.NORTH);
		
		pcolormap.add(new JLabel("Colormap: "));
		
		JRadioButton bjet=new JRadioButton("Jet");
		bjet.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				setColormap(ShortTimeFourierTransformer.colormapJet());
			}
		});
		pcolormap.add(bjet);
		
		JRadioButton bgray=new JRadioButton("Gray");
		bgray.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				setColormap(ShortTimeFourierTransformer.colormapGray());
			}
		});
		pcolormap.add(bgray);
		
		JRadioButton bantigray=new JRadioButton("Antigray");
		bantigray.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				setColormap(ShortTimeFourierTransformer.colormapAntigray());
			}
		});
		pcolormap.add(bantigray);
		
		ButtonGroup bgcolormap=new ButtonGroup();
		bgcolormap.add(bjet);
		bgcolormap.add(bgray);
		bgcolormap.add(bantigray);
		bgcolormap.setSelected(bjet.getModel(),true);
	}
	
	/**
	 * set coordinate text according to selected item index
	 * @param itemidx
	 * item index in dataset
	 */
	private void showCoordinate(int itemidx)
	{
	double t,f,m;
	
		t=timeValue(data.getItemColumn(itemidx));
		f=frequencyValue(data.getItemRow(itemidx));
		m=data.getItem(itemidx).doubleValue();
		
		lt.setText("Time (s): "+tformat.format(t));
		lf.setText("Frequency (Hz): "+fformat.format(f));
		lm.setText("Magnitude (dB): "+mformat.format(m));
	}
	
	/**
	 * convert signal value to dB
	 * @param val
	 * spectrogram data value
	 * @return
	 */
	public double val2db(Complex val)
	{
	double energy;
	
		energy=2*BLAS.absSquare(val)/stft.fftSize();
		
//		if(energy<P0SQUARE) energy=P0SQUARE;//prevent negative dB
//		return 10*Math.log10(energy/P0SQUARE);
	
		if(energy<EPS) energy=EPS;
		return 10*Math.log10(energy);
		
		/*
		 * better looking, but not the real dB value
		 */
//		return 10*Math.log10(BLAS.absSquare(val)+1);
	}
	
	/**
	 * get STFT
	 * @return
	 */
	public ShortTimeFourierTransformer stfTransformer()
	{
		return stft;
	}
	
	/**
	 * get colormap used to draw spectrogram
	 * @return
	 */
	public Color[] getColormap()
	{
		return colormap;
	}
	
	/**
	 * set colormap used to draw spectrogram
	 * @param colormap
	 * new colormap
	 */
	public void setColormap(Color[] colormap)
	{
		this.colormap=colormap;
		chart.fireChartChanged();
	}

	/**
	 * convert spectrogram data row index to time
	 * @param idx
	 * row index
	 * @return
	 */
	public double timeValue(int idx)
	{
		return xaxisval[idx];
	}
	
	/**
	 * convert spectrogram data column index to frequency
	 * @param idx
	 * column index;
	 * @return
	 */
	public double frequencyValue(int idx)
	{
		return idx*fs/stft.fftSize();
	}
	
	/**
	 * get spectrogram chart
	 * @return
	 */
	public JFreeChart spectrogramChart()
	{
		return chart;
	}
	
	/**
	 * get the chart panel for spectrogram visualization
	 * @return
	 */
	public JPanel spectrogramPanel()
	{
		return spectropanel;
	}
	
	/**
	 * visualize the spectrogram
	 */
	public void visualize()
	{
	JFrame frame;
	
		frame=new JFrame("Spectrogram Viewer");
		frame.addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent e)
			{
				System.exit(0);
			}
		});
		
		frame.getContentPane().add(spectrogramPanel(),BorderLayout.CENTER);
		
		/*
		 * show GUI
		 */
		frame.pack();
		DisplayMode dmode=GraphicsEnvironment.getLocalGraphicsEnvironment().
			getDefaultScreenDevice().getDisplayMode();
		frame.setLocation(
				Math.max((dmode.getWidth()-frame.getWidth())/2,0),
				Math.max((dmode.getHeight()-frame.getHeight())/2,0));	
		frame.setVisible(true);
	}
	
	/**
	 * <h1>Description</h1>
	 * Dataset for spectrogram.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Mar 16, 2012 8:38:04 PM, revision:
	 */
	private class SpectrogramDataset extends MatrixSeriesCollection
	{
	private static final long serialVersionUID=6795860176857642694L;
	
		public SpectrogramDataset(MatrixSeries series)
		{
			super(series);
		}
		
		public double getXValue(int seriesidx,int itemidx)
		{
			return getX(seriesidx,itemidx);
		}
		
		public Double getX(int seriesidx,int itemidx)
		{
			return timeValue(this.getSeries(seriesidx).getItemColumn(itemidx));
		}

		public double getYValue(int seriesidx,int itemidx)
		{
			return getY(seriesidx,itemidx);
		}
		
		public Double getY(int seriesidx,int itemidx)
		{
			return frequencyValue(this.getSeries(seriesidx).getItemRow(itemidx));
		}
	}

	/**
	 * <h1>Description</h1>
	 * Convert dB to color.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Mar 17, 2012 10:14:17 AM, revision:
	 */
	private class SpectrogramPaintScale implements Serializable, Cloneable, PaintScale
	{
	private static final long serialVersionUID=-3728201055226312440L;
	
		public double getLowerBound()
		{
			return minval;
		}
		
		public double getUpperBound()
		{
			return maxval;
		}

		public Paint getPaint(double val)
		{
		int idx;
		
			idx=(int)Math.round(colormap.length*(val-getLowerBound())/(getUpperBound()-getLowerBound()));
			if(idx<0) idx=0;
			else if(idx>colormap.length-1) idx=colormap.length-1;
			return colormap[idx];
		}
	}
	
	public static void main(String[] args) throws IOException, UnsupportedAudioFileException
	{
	ShortTimeFourierTransformer stft;
	WaveSource source;
	SpectrogramViewer foo;
	
		stft=new ShortTimeFourierTransformer(512,512*7/8,1024,null);
		source=new WaveSource(new File("data/source3.wav"),true);
		
//		pp.util.Util.showImage(stft.spectrogram(source));
		foo=new SpectrogramViewer(stft,source,source.audioFormat().getSampleRate());
		foo.visualize();
		
		source.close();
	}
}
