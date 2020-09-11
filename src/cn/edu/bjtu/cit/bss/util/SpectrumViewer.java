package cn.edu.bjtu.cit.bss.util;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import org.apache.commons.math.complex.*;
import org.jfree.chart.*;
import org.jfree.data.xy.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.chart.plot.*;
import pp.util.BLAS;
import cn.edu.bjtu.cit.bss.eval.*;

/**
 * <h1>Description</h1>
 * Draw energy and phase spectrum for a fft block.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Apr 23, 2012 2:39:16 PM, revision:
 */
public class SpectrumViewer implements Serializable
{
private static final long serialVersionUID=-7690142830982609593L;
private static double EPS=2.2204e-16;//a small positive number to prevent -inf dB
private JFreeChart chart;//the chart
private ChartPanel chartpanel;//the panel contain the chart

	/**
	 * @param signal
	 * a signal sequence, must be powers of 2
	 * @param fs
	 * sampling rate
	 */
	public SpectrumViewer(double[] signal,double fs)
	{
	Complex[] fx;
	double df,energy;
	XYSeries edataset;//energy
	XYSeries pdataset;//phase
	CombinedDomainXYPlot cplot;//the combined plot
	XYPlot eplot,pplot;
	
		fx=SpectralAnalyzer.fft(signal);//perform fft
		df=fs/fx.length;//the frequency coordinate increment

		/*
		 * build dataset for energy
		 */
		edataset=new XYSeries("Energy");
		for(int fidx=0;fidx<fx.length/2+1;fidx++) 
		{
			energy=BLAS.absSquare(fx[fidx])/fx.length;
			if(fidx>0&&fidx<fx.length/2) energy*=2;
			energy=val2db(energy);//convert to dB
			edataset.add(fidx*df,energy);
		}
		
		/*
		 * build dataset for phase
		 */
		pdataset=new XYSeries("Phase");
		for(int fidx=0;fidx<fx.length/2+1;fidx++) 
			pdataset.add(fidx*df,fx[fidx].getArgument());
		
		/*
		 * the combined plot
		 */
		cplot=new CombinedDomainXYPlot(new NumberAxis("Frequency (Hz)"));
		cplot.setGap(5);
		cplot.getDomainAxis().setLowerMargin(0);
		cplot.getDomainAxis().setUpperMargin(0);
			
		/*
		 * add each subplot
		 */
		eplot=new XYPlot(
				new XYSeriesCollection(edataset),
				null,
				new NumberAxis("Energy (dB)"),
				new StandardXYItemRenderer());
		cplot.add(eplot);
		
		pplot=new XYPlot(
				new XYSeriesCollection(pdataset),
				null,
				new NumberAxis("Phase (Radian)"),
				new StandardXYItemRenderer());
		cplot.add(pplot);
				
		/*
		 * the combined chart
		 */
		chart=new JFreeChart(cplot);
		chart.removeLegend();
		chartpanel=new ChartPanel(chart);
	}
	
	/**
	 * convert a energy magnitude to dB
	 * @param val
	 * energy magnitude
	 * @return
	 */
	public double val2db(double val)
	{
		if(Math.abs(val)<EPS) val=EPS;
		return 10*Math.log10(Math.abs(val));
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
	 * visualize the chart
	 */
	public void visualize()
	{
	JFrame frame;
		
		frame=new JFrame("Signal Viewer");
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
	
	public static void main(String[] args) throws IOException
	{
	VirtualRoom room;
	double[] signal;
	SpectrumViewer viewer;
	
		room=new VirtualRoom(new File("data/VirtualRooms/2x2/SawadaRoom2x2.txt"));
		signal=room.tdFilters()[0][0];
		viewer=new SpectrumViewer(signal,8000);
		viewer.visualize();
	}
}
