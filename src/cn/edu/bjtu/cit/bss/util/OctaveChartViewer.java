package cn.edu.bjtu.cit.bss.util;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.*;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.*;
import org.apache.commons.math.complex.*;
import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.*;
import org.jfree.data.category.*;
import pp.util.BLAS;
import cn.edu.bjtu.cit.bss.signalio.*;

/**
 * <h1>Description</h1>
 * Used to draw 1/3 octave band chart.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Mar 30, 2012 10:17:01 AM, revision:
 */
public class OctaveChartViewer implements Serializable
{
private static final long serialVersionUID=377902461453501772L;
private static double P0SQUARE=2e-5*2e-5;//p0 is 2e-5 pa
//frequency band intervals
private static final double[] OCTAVE_INTERVAL=new double[29];
//frequency band centers
private static final double[] OCTAVE_CENTER=new double[28];	
private ShortTimeFourierTransformer stft;//used to perform stft
/*
 * Unweighted octave chart data. The first row is the center frequency 
 * for each band, the second row is corresponding unweighted magnitude in dB.
 */
private double[][] octavedata;
private DecimalFormat format=new DecimalFormat("0.#");//used to format octave data
private DefaultCategoryDataset dataset;//the chart dataset
private JFreeChart chart;//the chart
private JPanel chartpanel;//the chart panel

	static
	{
		/*
		 * frequency band intervals
		 */
		OCTAVE_INTERVAL[0]=28.06;
		OCTAVE_INTERVAL[1]=35.6;
		OCTAVE_INTERVAL[2]=44.9;
		OCTAVE_INTERVAL[3]=56.1;
		OCTAVE_INTERVAL[4]=70.7;
		OCTAVE_INTERVAL[5]=89.8;
		OCTAVE_INTERVAL[6]=112;
		OCTAVE_INTERVAL[7]=140;
		OCTAVE_INTERVAL[8]=178;
		OCTAVE_INTERVAL[9]=224;
		OCTAVE_INTERVAL[10]=280;
		OCTAVE_INTERVAL[11]=353;
		OCTAVE_INTERVAL[12]=449;
		OCTAVE_INTERVAL[13]=561;
		OCTAVE_INTERVAL[14]=707;
		OCTAVE_INTERVAL[15]=898;
		OCTAVE_INTERVAL[16]=1122;
		OCTAVE_INTERVAL[17]=1403;
		OCTAVE_INTERVAL[18]=1796;
		OCTAVE_INTERVAL[19]=2245;
		OCTAVE_INTERVAL[20]=2806;
		OCTAVE_INTERVAL[21]=3535;
		OCTAVE_INTERVAL[22]=4490;
		OCTAVE_INTERVAL[23]=5612;
		OCTAVE_INTERVAL[24]=7071;
		OCTAVE_INTERVAL[25]=8980;
		OCTAVE_INTERVAL[26]=11220;
		OCTAVE_INTERVAL[27]=14030;
		OCTAVE_INTERVAL[28]=17960;
		
		/*
		 * frequency band centers
		 */
		OCTAVE_CENTER[0]=31.5;
		OCTAVE_CENTER[1]=40;
		OCTAVE_CENTER[2]=50;
		OCTAVE_CENTER[3]=63;
		OCTAVE_CENTER[4]=80;
		OCTAVE_CENTER[5]=100;
		OCTAVE_CENTER[6]=125;
		OCTAVE_CENTER[7]=160;
		OCTAVE_CENTER[8]=200;
		OCTAVE_CENTER[9]=250;
		OCTAVE_CENTER[10]=315;
		OCTAVE_CENTER[11]=400;
		OCTAVE_CENTER[12]=500;
		OCTAVE_CENTER[13]=630;
		OCTAVE_CENTER[14]=800;
		OCTAVE_CENTER[15]=1000;
		OCTAVE_CENTER[16]=1250;
		OCTAVE_CENTER[17]=1600;
		OCTAVE_CENTER[18]=2000;
		OCTAVE_CENTER[19]=2500;
		OCTAVE_CENTER[20]=3150;
		OCTAVE_CENTER[21]=4000;
		OCTAVE_CENTER[22]=5000;
		OCTAVE_CENTER[23]=6300;
		OCTAVE_CENTER[24]=8000;
		OCTAVE_CENTER[25]=10000;
		OCTAVE_CENTER[26]=12500;
		OCTAVE_CENTER[27]=16000;
	}
	
	/**
	 * <h1>Description</h1>
	 * Weighting in 1/3 octave band chart.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Jun 16, 2011 5:12:52 PM, revision:
	 */
	public enum Weighting
	{
		/**
		 * Without weighting.
		 */
		NONE,
		/**
		 * A weighting.
		 */
		A
	}
	
	/**
	 * @param source
	 * the signal source
	 * @param fs
	 * sampling rate
	 * @param stftsize
	 * stft block size
	 * @param stftoverlap
	 * stft block overlapping in taps
	 * @param fftsize
	 * fft block size, bust be powers of 2
	 * @throws IOException
	 */
	public OctaveChartViewer(SignalSource source,double fs,
			int stftsize,int stftoverlap,int fftsize) throws IOException
	{
	CategoryPlot plot;
	
		stft=new ShortTimeFourierTransformer(stftsize,stftoverlap,fftsize,null);
		//calculate unweighted octave data in dB
		octavedata=octaveSpectrum(source,fs);
		dataset=new DefaultCategoryDataset();
		
		/*
		 * create the chart
		 */
		chart=ChartFactory.createBarChart(
				"",//title
				"Frequency (Hz)",//domain axis label
				"",//range axis label
				dataset,//the dataset
				PlotOrientation.VERTICAL,
				false,//include legend
				true,//tooltips?
				false);//urls?
			
		plot=(CategoryPlot)chart.getPlot();
		//set frequency vertical
		plot.getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.DOWN_45);
		
		/*
		 * construct the chart panel
		 */
		chartpanel=new JPanel();
		chartpanel.setLayout(new BorderLayout(5,5));
		
		ChartPanel cp=new ChartPanel(chart,true);
		chartpanel.add(cp,BorderLayout.CENTER);
		
		JPanel pw=new JPanel();
		pw.setLayout(new FlowLayout(FlowLayout.LEFT,5,5));
		pw.setBorder(new TitledBorder(""));
		chartpanel.add(pw,BorderLayout.NORTH);
		
		pw.add(new JLabel("Weighting: "));
		
		/*
		 * use no weighting
		 */
		JRadioButton bn=new JRadioButton("No weighting");
		bn.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				setWeighting(Weighting.NONE);
			}
		});
		pw.add(bn);
		
		/*
		 * use A weighting
		 */
		JRadioButton ba=new JRadioButton("A weighting");
		ba.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				setWeighting(Weighting.A);
			}
		});
		pw.add(ba);
		
		ButtonGroup bg=new ButtonGroup();
		bg.add(bn);
		bg.add(ba);
		setWeighting(Weighting.NONE);
		bg.setSelected(bn.getModel(),true);
	}
	
	/**
	 * set weighting policy and redraw the chart
	 * @param weighting
	 * the weighting policy
	 */
	private void setWeighting(Weighting weighting)
	{
	double[][] woctave;
	String ylabel;
		
		/*
		 * update dataset
		 */
		woctave=weighting(octavedata,weighting);//apply weighting
		
		for(int binidx=0;binidx<woctave[0].length;binidx++) 
			dataset.setValue(woctave[1][binidx],"",format.format(woctave[0][binidx]));

		/*
		 * update label
		 */
		switch(weighting)
		{
			//no weighting is applied
			case NONE:
				ylabel="Leq (dB)";
				break;
			//A weighting
			case A:
				ylabel="LAeq (dB(A))";
				break;
			default: throw new IllegalArgumentException("unknown weighting: "+weighting);
		}
		
		((CategoryPlot)chart.getPlot()).getRangeAxis().setLabel(ylabel);
	}
	
	/**
	 * A weightning for a specified frequency in dB, see: 
	 * http://www.diracdelta.co.uk/science/source/a/w/aweighting/source.html
	 * @param f
	 * a frequency
	 * @return
	 */
	public static double weightingA(double f)
	{
	double f2,f4;
	double temp1,temp2;
	double temp21,temp22;
	
		f2=f*f;
		f4=f2*f2;
		
		temp1=(1.562339*f4)/((f2+107.65265*107.65265)*(f2+737.86223*737.86223));
		
		temp21=f2+20.598997*20.598997;
		temp22=f2+12194.22*12194.22;
		
		temp2=(2.242881e16*f4)/((temp21*temp21)*(temp22*temp22));
		return 10*Math.log10(temp1)+10*Math.log10(temp2);
	}
	
	/**
	 * get 1/3 octave band central frequencies
	 * @return
	 */
	public static double[] octaveCentralFrequencies()
	{
	double[] center;
	
		center=new double[OCTAVE_CENTER.length];
		System.arraycopy(OCTAVE_CENTER,0,center,0,OCTAVE_CENTER.length);
		return center;
	}
		
	/**
	 * calculate the unweighted octave data
	 * @param source
	 * signal source
	 * @param fs
	 * sampling rate
	 * @return
	 * the first row is the central frequency, the second row is the corresponding data
	 * @throws IOException
	 */
	private double[][] octaveSpectrum(SignalSource source,double fs) throws IOException
	{
	double[] energy;

		//calculate the mean energy from signal source
		{
		ShortTimeFourierTransformer.STFTIterator stftit;
		Complex[] fx;
		
			/*
			 * calculate total energy spectrum
			 */
			energy=new double[stft.fftSize()/2+1];
			stftit=stft.stftIterator(source);
			for(;stftit.hasNext();)
			{
				fx=stftit.next();

				energy[0]+=BLAS.absSquare(fx[0]);//DC part
				energy[energy.length-1]+=BLAS.absSquare(fx[energy.length-1]);//the highest frequency
				//two halves for other parts
				for(int i=1;i<energy.length-1;i++) energy[i]+=2*BLAS.absSquare(fx[i]);
			}

			//energy conservation between time domain and frequency domain
			BLAS.scalarMultiply(1.0/stft.fftSize(),energy,energy);
			//cancel the window overlapping effect
			BLAS.scalarMultiply(2.0*(stft.stftSize()-stft.stftOverlap())/stft.stftSize(),energy,energy);
			//average energy for one sample, so energy will not change with signal length
			BLAS.scalarMultiply(1.0/stftit.sampleIndex(),energy,energy);
		}
	
		//calculate unweighted octave data
		{
		double[][] octave;
		double f,df;
		int bandidx;
		
			df=fs/2.0/(energy.length-1);//frequency increment, the range is [0, fs/2]	
			octave=new double[2][OCTAVE_CENTER.length];
			//center frequencies
			System.arraycopy(OCTAVE_CENTER,0,octave[0],0,octave[0].length);
			
			/*
			 * accumulate energy in each frequency band
			 */
			f=0;
			bandidx=0;
			for(int i=0;i<energy.length;i++)
			{
				//out of intervals
				if(f<OCTAVE_INTERVAL[0]||
						f>OCTAVE_INTERVAL[OCTAVE_INTERVAL.length-1]) 
				{
					f+=df;
					continue;
				}
				
				/*
				 * find the frequency band
				 */
				for(;bandidx<octave[0].length;bandidx++) 
					if(f<OCTAVE_INTERVAL[bandidx+1]) break;
				if(bandidx>octave[0].length-1) bandidx=octave[0].length-1;
		
				//accumulate energy in each octave band
				octave[1][bandidx]+=energy[i];
				f+=df;
			}

			//to dB
			for(int j=0;j<octave[1].length;j++) 
			{
				if(Math.abs(octave[1][j])<P0SQUARE) octave[1][j]=P0SQUARE;//to prevent negative dB
				octave[1][j]=10*(Math.log10(octave[1][j])-Math.log10(P0SQUARE));
			}
							
			return octave;
		}
	}
	
	/**
	 * apply weighting on unweighted octave data
	 * @param octave
	 * unweighted octave data
	 * @param w
	 * weighting policy
	 * @return
	 */
	private double[][] weighting(double[][] octave,Weighting w)
	{
	double[][] result;
		
		result=new double[octave.length][octave[0].length];
		//copy central frequencies
		System.arraycopy(octave[0],0,result[0],0,result[0].length);
		
		//weighting
		switch(w)
		{
			//no weighting is applied
			case NONE:
				System.arraycopy(octave[1],0,result[1],0,result[1].length);
				break;
			//A weighting
			case A:
				for(int binidx=0;binidx<octave[1].length;binidx++) 
					result[1][binidx]=weightingA(octave[0][binidx])+octave[1][binidx];
				break;
			default: throw new IllegalArgumentException("unknown weighting: "+w);
		}
		
		//set negative dB to zero
		for(int binidx=0;binidx<result[1].length;binidx++) 
			if(result[1][binidx]<0) result[1][binidx]=0;
		
		return result;
	}
	
	/**
	 * calculate 1/3 octave bands spectrum from input signal
	 * @param source
	 * input signal
	 * @param fs
	 * sampling rate
	 * @param w
	 * weighting policy
	 * @return
	 * The first row is the center frequency for each band, 
	 * the second row is corresponding magnitude in dB.
	 * @throws IOException
	 */
	public double[][] octaveSpectrum(SignalSource source,double fs,Weighting w) throws IOException
	{
		return weighting(octaveSpectrum(source,fs),w);
	}
	
	/**
	 * get chart panel
	 * @return
	 */
	public JPanel chartPanel()
	{
		return chartpanel;
	}
	
	/**
	 * visualize the octave chart
	 */
	public void visualize()
	{
	JFrame frame;
	
		frame=new JFrame("1/3 Octave Band Chart");
		frame.addWindowListener(new WindowAdapter(){
		public void windowClosing(WindowEvent e)
			{
				System.exit(0);
			}
		});
		
		frame.getContentPane().add(chartPanel(),BorderLayout.CENTER);
		
		frame.pack();
		DisplayMode dmode=GraphicsEnvironment.getLocalGraphicsEnvironment().
		getDefaultScreenDevice().getDisplayMode();
		frame.setLocation(
				Math.max((dmode.getWidth()-frame.getWidth())/2,0),
				Math.max((dmode.getHeight()-frame.getHeight())/2,0));
		frame.setVisible(true);	
	}
		
	public static void main(String args[]) throws IOException, UnsupportedAudioFileException
	{
	SignalSource source;
	OctaveChartViewer octave;
	
		source=new WaveSource(new File("data/source2.wav"),true);
		octave=new OctaveChartViewer(source,8000,2048,2048*3/4,4096);
		source.close();
		
		octave.visualize();
	}
}
