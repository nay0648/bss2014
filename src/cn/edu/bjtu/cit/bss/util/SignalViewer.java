package cn.edu.bjtu.cit.bss.util;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import org.jfree.chart.*;
import org.jfree.data.xy.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.chart.plot.*;

/**
 * <h1>Description</h1>
 * Used to plot signals.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Apr 3, 2012 5:21:27 PM, revision:
 */
public class SignalViewer implements Serializable
{
private static final long serialVersionUID=-7762986050534875816L;
private JFreeChart chart;//the chart
private XYSeries[] dataset;//the dataset
private ChartPanel chartpanel;//the panel contain the chart

	/**
	 * @param signals
	 * each one is a channel
	 */
	public SignalViewer(double[]... signals)
	{
		buildDataset(1,signals);
		buildChart("Time (Tap)");
	}
	
	/**
	 * @param fs
	 * sampling rate
	 * @param signals
	 * each one is a channel
	 */
	public SignalViewer(double fs,double[]... signals)
	{
		buildDataset(fs,signals);
		buildChart("Time (Second)");
	}
	
	/**
	 * build dataset
	 * @param fs
	 * sampling rate
	 * @param signals
	 * each one is a channel
	 */
	private void buildDataset(double fs,double[]... signals)
	{
	int len=Integer.MAX_VALUE;
	
		//find the minimum length
		for(double[] s:signals) if(s.length<len) len=s.length;
	
		dataset=new XYSeries[signals.length];
		for(int chidx=0;chidx<signals.length;chidx++)
		{
			dataset[chidx]=new XYSeries("Channel "+chidx);//the dataset
			for(int taps=0;taps<len;taps++) 
				dataset[chidx].add(taps/fs,signals[chidx][taps]);
		}
	}
	
	/**
	 * build the chart
	 * @param xlabel
	 * label for x axis
	 */
	private void buildChart(String xlabel)
	{
	CombinedDomainXYPlot cplot;//the combined plot
	XYPlot plot;
			
		/*
		 * the combined plot
		 */
		cplot=new CombinedDomainXYPlot(new NumberAxis(xlabel));
		cplot.setGap(5);
		cplot.getDomainAxis().setLowerMargin(0);
		cplot.getDomainAxis().setUpperMargin(0);
		
		//add each subplot
		for(XYSeries series:dataset)
		{		
			//construct the plot
			plot=new XYPlot(
					new XYSeriesCollection(series),
					null,
					new NumberAxis("Magnitude"),
					new StandardXYItemRenderer());
				
			cplot.add(plot);
		}
			
		/*
		 * the combined chart
		 */
		chart=new JFreeChart(cplot);
		chart.removeLegend();
		chartpanel=new ChartPanel(chart);		
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
	double[][] sigs;
	SignalViewer viewer;
	
		sigs=Util.loadSignals(new File("data/demosig.txt"),Util.Dimension.COLUMN);
		viewer=new SignalViewer(100,sigs);
		viewer.visualize();
	}
}
