package cn.edu.bjtu.cit.bss.recorder;
import java.io.*;
import java.text.*;
import java.util.List;
import java.util.LinkedList;
import java.awt.*;
import java.awt.event.*;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.*;
import cn.edu.bjtu.cit.bss.*;
import cn.edu.bjtu.cit.bss.signalio.*;
import cn.edu.bjtu.cit.bss.util.*;

/**
 * <h1>Description</h1>
 * A multichannel sound recorder for blind source separation demonstration.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Mar 11, 2012 5:27:01 PM, revision:
 */
public class BSSRecorder extends JFrame
{
private static final long serialVersionUID=1536044107851449262L;
//(float sampleRate, int sampleSizeInBits, int channels, boolean signed, boolean bigEndian) 
private AudioFormat format=new AudioFormat(16000,16,1,true,false);//input audio format
private CaptureChartManager cmanager;//used to construct and update chart for recorder
private PlaybackChartManager pmanager;//used to construct and update chart for bss playback
private RecorderState state=RecorderState.STOP;//recorder state
private JTabbedPane tab;//the tabbed pane

private BSSParameterTable paramtable;//parameters for bss
private FDBSS fdbss=new FDBSS(new File("temp"));//the bss algorithm
private JButton bbss;//button starts bss
private BSSOperationPanel bssop;//operations for bss
private BSSThread bssth=null;//thread used to run bss

private JTextField tffs;//for sampling rate
private JTextField tfbuffersize;//buffer size in seconds
private File currentdir=null;//base directory of last opened wave file

	public BSSRecorder() throws LineUnavailableException
	{
		super("BSS Recorder");
		
		/*
		 * chart manager
		 */
		cmanager=new CaptureChartManager(this,(int)(10*format.getSampleRate()));
		cmanager.addChannel();
		cmanager.addChannel();
		pmanager=new PlaybackChartManager(this,(int)(10*format.getSampleRate()));

		initGUI();
	}
	
	/**
	 * show error message dialog
	 * @param message
	 * error message
	 */
	public void showErrorMessageDialog(Object message)
	{
		JOptionPane.showMessageDialog(this,message,"Error",JOptionPane.ERROR_MESSAGE);
	}
	
	/**
	 * show error message dialog
	 * @param message
	 * error message
	 * @param e
	 * corresponding exception
	 */
	public void showErrorMessageDialog(String message,Exception e)
	{
		showErrorMessageDialog(
				message+"\n"+
				e.getClass().getName()+"\n"+
				e.getMessage());
	}
	
	/**
	 * initialize GUI
	 */
	private void initGUI()
	{
		this.addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent e)
			{
				System.exit(0);
			}
		});
		this.getContentPane().setLayout(new BorderLayout(5,5));
		
		/*
		 * for recorder and bss tab
		 */
		tab=new JTabbedPane();
		this.getContentPane().add(tab,BorderLayout.CENTER);
		
		//the recorder pane
		{
			/*
			 * the main panel
			 */
			JPanel pm=new JPanel();
			pm.setLayout(new BorderLayout(5,5));
			tab.add("Recorder",pm);
			
			//the recorder chart panel
			pm.add(cmanager.chartPanel(),BorderLayout.CENTER);
			
			//the controls
			{
				JPanel pctrl0=new JPanel();
				pctrl0.setBorder(new TitledBorder(""));
				pctrl0.setLayout(new BoxLayout(pctrl0,BoxLayout.Y_AXIS));
				pm.add(pctrl0,BorderLayout.NORTH);
				
				//buttons
				{
					JPanel pctrl=new JPanel();
					pctrl.setLayout(new FlowLayout(FlowLayout.LEFT,5,5));
					pctrl0.add(pctrl);
								
					/*
					 * add a channel
					 */
					JButton badd=new JButton(new ImageIcon(this.getClass().getResource("list-add.png")));
					badd.setToolTipText("Add channel");
					badd.addActionListener(new ActionListener(){
						public void actionPerformed(ActionEvent e)
						{
							BSSRecorder.this.addChannelClicked();
						}
					});
					pctrl.add(badd);
				
					/*
					 * remove a channel
					 */
					JButton bremove=new JButton(new ImageIcon(this.getClass().getResource("list-remove.png")));
					bremove.setToolTipText("Remove channel");
					bremove.addActionListener(new ActionListener(){
						public void actionPerformed(ActionEvent e)
						{
							BSSRecorder.this.removeChannelClicked();
						}
					});
					pctrl.add(bremove);
				
					/*
					 * cepturing device setting
					 */
					JButton bset=new JButton(new ImageIcon(this.getClass().getResource("audio-input-microphone.png")));
					bset.setToolTipText("Bind capture devices");
					bset.addActionListener(new ActionListener(){
						public void actionPerformed(ActionEvent e)
						{
							BSSRecorder.this.bindDeviceClicked();
						}
					});
					pctrl.add(bset);
				
					/*
					 * open file
					 */
					JButton bopen=new JButton(new ImageIcon(this.getClass().getResource("document-open.png")));
					bopen.setToolTipText("Open wave file");
					bopen.addActionListener(new ActionListener(){
						public void actionPerformed(ActionEvent e)
						{
							BSSRecorder.this.openButtonClicked();
						}
					});
					pctrl.add(bopen);
				
					/*
					 * save all
					 */
					JButton bsave=new JButton(new ImageIcon(this.getClass().getResource("document-save-all.png")));
					bsave.setToolTipText("Save all channels");
					bsave.addActionListener(new ActionListener(){
						public void actionPerformed(ActionEvent e)
						{
							BSSRecorder.this.saveButtonPressed();
						}
					});
					pctrl.add(bsave);

					/*
					 * record button
					 */
					JButton brec=new JButton(new ImageIcon(this.getClass().getResource("media-record.png")));
					brec.setToolTipText("Capture");
					brec.addActionListener(new ActionListener(){
						public void actionPerformed(ActionEvent e)
						{
							BSSRecorder.this.setRecorderState(RecorderState.CAPTURE);
						}
					});
					pctrl.add(brec);
				
					/*
					 * playback button
					 */
					JButton bplay=new JButton(new ImageIcon(this.getClass().getResource("media-playback-start.png")));
					bplay.setToolTipText("Playback");
					bplay.addActionListener(new ActionListener(){
						public void actionPerformed(ActionEvent e)
						{
							BSSRecorder.this.setRecorderState(RecorderState.PLAYBACK);
						}
					});
					pctrl.add(bplay);
				
					/*
					 * stop button
					 */
					JButton bstop=new JButton(new ImageIcon(this.getClass().getResource("media-playback-stop.png")));
					bstop.setToolTipText("Stop");
					bstop.addActionListener(new ActionListener(){
						public void actionPerformed(ActionEvent e)
						{
							BSSRecorder.this.setRecorderState(RecorderState.STOP);
						}
					});
					pctrl.add(bstop);
				
					/*
					 * show spectrogram
					 */
					JButton bstft=new JButton(new ImageIcon(this.getClass().getResource("office-chart-area-percentage.png")));
					bstft.setToolTipText("Draw spectrogram");
					bstft.addActionListener(new ActionListener(){
						public void actionPerformed(ActionEvent e)
						{
							spectrogramButtonPressed();
						}
					});
					pctrl.add(bstft);
				}
				
				//audio format information
				{
					JPanel pformat=new JPanel();
					pformat.setLayout(new FlowLayout(FlowLayout.LEFT,5,5));
					pctrl0.add(pformat);
				
					/*
					 * sampling rate
					 */
					pformat.add(new JLabel("Sampling Rate (Hz): "));
					tffs=new JTextField(8);
					tffs.setText(Double.toString(format.getSampleRate()));
					tffs.addActionListener(new ActionListener(){
						public void actionPerformed(ActionEvent e)
						{
						String sfs;
						double fs;
						
							sfs=((JTextField)e.getSource()).getText();
							try
							{
								fs=Double.parseDouble(sfs);
								BSSRecorder.this.setSamplingRate(fs);
							}
							catch(NumberFormatException ne)
							{
								BSSRecorder.this.showErrorMessageDialog("Wrong number format: "+sfs);
							}
						}
					});
					pformat.add(tffs);
				
					/*
					 * bits per sample
					 */
					pformat.add(new JLabel("  Bits per Sample: "));
					JComboBox cbbps=new JComboBox(new String[]{"8","16","24"});
					cbbps.setSelectedItem(Integer.toString(format.getSampleSizeInBits()));
					cbbps.addActionListener(new ActionListener(){
						public void actionPerformed(ActionEvent e)
						{
						int bps;
						
							bps=Integer.parseInt(((JComboBox)e.getSource()).getSelectedItem().toString());
							
							if(bps!=format.getSampleSizeInBits()) 
								format=new AudioFormat(format.getSampleRate(),bps,1,true,false);
						}
					});
					pformat.add(cbbps);
					
					/*
					 * buffer size
					 */
					pformat.add(new JLabel("  Buffer Size (Second): "));
					tfbuffersize=new JTextField(8);
					tfbuffersize.setText(Double.toString(buffersizeInSeconds()));
					tfbuffersize.addActionListener(new ActionListener(){
						public void actionPerformed(ActionEvent e)
						{
						String ssins;
						double sizeins;
							
							ssins=((JTextField)e.getSource()).getText();
							try
							{
								sizeins=Double.parseDouble(ssins);
								BSSRecorder.this.setBuffersizeInSamples((int)(sizeins*format.getSampleRate()));
							}
							catch(NumberFormatException ne)
							{
								BSSRecorder.this.showErrorMessageDialog("Wrong number format: "+ssins);
							}
						}
					});
					pformat.add(tfbuffersize);
				}
			}
		}
		
		//the bss pane
		{
			/*
			 * the main panel for bss
			 */
			JPanel pmbss=new JPanel();
			pmbss.setLayout(new BorderLayout(5,5));
			tab.add("BSS",pmbss);
			
			//used to set parameters and start bss
			{
				/*
				 * the panel
				 */
				JPanel pbss=new JPanel();
				pbss.setBorder(new TitledBorder(""));
				pbss.setLayout(new BorderLayout(5,5));
				pmbss.add(pbss,BorderLayout.NORTH);
				
				/*
				 * parameter table
				 */
				paramtable=new BSSParameterTable(this);
				pbss.add(paramtable,BorderLayout.CENTER);
				
				/*
				 * operations
				 */
				bssop=new BSSOperationPanel();
				pbss.add(bssop,BorderLayout.SOUTH);
				
				/*
				 * bss start button
				 */
				bbss=new JButton(new ImageIcon(this.getClass().getResource("applications-utilities.png")));
				bbss.setToolTipText("Perform BSS");
				bbss.addActionListener(new ActionListener(){
					public void actionPerformed(ActionEvent e)
					{
						BSSRecorder.this.setRecorderState(RecorderState.BSS);
					}
				});
				pbss.add(bbss,BorderLayout.EAST);
			}
			
			//play estimated signals
			{
				JPanel pest=new JPanel();
				pest.setLayout(new BorderLayout(5,5));
				pmbss.add(pest,BorderLayout.CENTER);
				
				pest.add(pmanager.chartPanel(),BorderLayout.CENTER);
				
				/*
				 * buttons
				 */
				JPanel pctrl=new JPanel();
				pctrl.setBorder(new TitledBorder(""));
				pctrl.setLayout(new FlowLayout(FlowLayout.LEFT,5,5));
				pest.add(pctrl,BorderLayout.NORTH);
				
				/*
				 * save all
				 */
				JButton bsave=new JButton(new ImageIcon(this.getClass().getResource("document-save-all.png")));
				bsave.setToolTipText("Save all channels");
				bsave.addActionListener(new ActionListener(){
					public void actionPerformed(ActionEvent e)
					{
						BSSRecorder.this.saveButtonPressed();
					}
				});
				pctrl.add(bsave);
				
				/*
				 * playback button
				 */
				JButton bplay=new JButton(new ImageIcon(this.getClass().getResource("media-playback-start.png")));
				bplay.setToolTipText("Playback");
				bplay.addActionListener(new ActionListener(){
					public void actionPerformed(ActionEvent e)
					{
						BSSRecorder.this.setRecorderState(RecorderState.PLAYBACK);
					}
				});
				pctrl.add(bplay);
				
				/*
				 * stop button
				 */
				JButton bstop=new JButton(new ImageIcon(this.getClass().getResource("media-playback-stop.png")));
				bstop.setToolTipText("Stop");
				bstop.addActionListener(new ActionListener(){
					public void actionPerformed(ActionEvent e)
					{
						BSSRecorder.this.setRecorderState(RecorderState.STOP);
					}
				});
				pctrl.add(bstop);
				
				/*
				 * show spectrogram
				 */
				JButton bstft=new JButton(new ImageIcon(this.getClass().getResource("office-chart-area-percentage.png")));
				bstft.setToolTipText("Draw spectrogram");
				bstft.addActionListener(new ActionListener(){
					public void actionPerformed(ActionEvent e)
					{
						spectrogramButtonPressed();
					}
				});
				pctrl.add(bstft);
			}
		}

		/*
		 * show GUI
		 */
		this.pack();
		DisplayMode dmode=GraphicsEnvironment.getLocalGraphicsEnvironment().
			getDefaultScreenDevice().getDisplayMode();
		this.setLocation(
				Math.max((dmode.getWidth()-this.getWidth())/2,0),
				Math.max((dmode.getHeight()-this.getHeight())/2,0));
		this.setVisible(true);		
	}
	
	/**
	 * get all supported lines for recording
	 * @return
	 * @throws LineUnavailableException 
	 */
	public List<CaptureDevice> listCaptureDevices() throws LineUnavailableException
	{
	List<CaptureDevice> linelist;
	Mixer mixer;
		
		linelist=new LinkedList<CaptureDevice>();
		
		for(Mixer.Info info:AudioSystem.getMixerInfo()) 
		{
			mixer=AudioSystem.getMixer(info);
			for(Line.Info linfo:mixer.getTargetLineInfo(new DataLine.Info(TargetDataLine.class,format)))
				linelist.add(new CaptureDevice(mixer,(TargetDataLine)mixer.getLine(linfo)));
		}
		
		return linelist;
	}
	
	/**
	 * get all binded capture devices
	 * @return
	 * each channel for an entry, null means no device is binded
	 */
	public CaptureDevice[] bindedCaptureDevices()
	{
		return cmanager.bindedCaptureDevices();
	}
	
	/**
	 * clear all binded capture devices
	 */
	public void clearBindedCaptureDevices()
	{
		cmanager.clearBindedCaptureDevices();
	}
	
	/**
	 * bind a channel to a capture device
	 * @param chidx
	 * channel index
	 * @param device
	 * capture device
	 */
	public void bindCaptureDevice(int chidx,CaptureDevice device)
	{
		cmanager.bindCaptureDevice(chidx,device);
	}
	
	/**
	 * number of estimated sources
	 * @return
	 */
	public int numSources()
	{
		return pmanager.getNumChannels();
	}
	
	/**
	 * number of input channels
	 * @return
	 */
	public int numSensors()
	{
		return cmanager.getNumChannels();
	}
	
	/**
	 * get recorder state
	 * @return
	 */
	public RecorderState getRecorderState()
	{
		return state;
	}
	
	/**
	 * set recorder state
	 * @param newstate
	 * new state
	 */
	public void setRecorderState(RecorderState newstate)
	{
		if(state==newstate) return;//state not changed
		
		switch(newstate)
		{
			//stop recorder
			case STOP:
			{
				if(bssth!=null) showErrorMessageDialog("Cannot stop BSS procedure.");
				else
				{
					if(state==RecorderState.CAPTURE) state=RecorderState.STOP;
					else if(state==RecorderState.PLAYBACK)
					{
						if(tab.getSelectedIndex()==0) cmanager.stopPlayback();
						else if(tab.getSelectedIndex()==1) pmanager.stopPlayback();			
						state=RecorderState.STOP;
					}
					else state=RecorderState.STOP;
				
					tffs.setEnabled(true);
					tfbuffersize.setEnabled(true);
				}
			}break;
			//start capture
			case CAPTURE:
			{
				if(state==RecorderState.STOP) 
				{
					tffs.setEnabled(false);
					tfbuffersize.setEnabled(false);
					
					state=RecorderState.CAPTURE;
					cmanager.startCapture();
				}
				else showErrorMessageDialog("BSSRecorder is running: state="+getRecorderState());
			}break;
			//start playback
			case PLAYBACK:
			{
				if(state==RecorderState.STOP)
				{
					if(tab.getSelectedIndex()==0) 
					{
						if(cmanager.selectedChannelIndex()<0) showErrorMessageDialog("No channel is selected.");
						else 
						{
							tffs.setEnabled(false);
							tfbuffersize.setEnabled(false);
							
							state=RecorderState.PLAYBACK;
							cmanager.startPlayback();
						}
					}
					else if(tab.getSelectedIndex()==1) 
					{
						if(pmanager.selectedChannelIndex()<0) showErrorMessageDialog("No channel is selected.");
						else 
						{
							tffs.setEnabled(false);
							tfbuffersize.setEnabled(false);
							
							state=RecorderState.PLAYBACK;
							pmanager.startPlayback();
						}
					}
				}
				else showErrorMessageDialog("BSSRecorder is running: state="+getRecorderState());
			}break;
			//perform bss
			case BSS:
			{		
				if(state==RecorderState.STOP&&bssth==null)
				{
					tffs.setEnabled(false);
					tfbuffersize.setEnabled(false);
					
					state=RecorderState.BSS;
					bssth=new BSSThread(bbss);
					bssth.start();
				}
				else showErrorMessageDialog("BSSRecorder is running: state="+getRecorderState());
			}break;
			default: throw new IllegalStateException("unknown recorder state: "+newstate);
		}
	}
	
	/**
	 * get audio format
	 * @return
	 */
	public AudioFormat audioFormat()
	{
		return format;
	}
	
	/**
	 * get fdbss algorithm reference
	 * @return
	 */
	public FDBSS fdbssAlgorithm()
	{
		return fdbss;
	}
	
	/**
	 * set sampling rate
	 * @param fs
	 * sampling rate in Hz
	 */
	private void setSamplingRate(double fs)
	{
	double sizeins;
	
		if(format.getSampleRate()==fs) return;

		sizeins=buffersizeInSeconds();//save buffer duration
		format=new AudioFormat((float)fs,format.getSampleSizeInBits(),1,true,false);		
		setBuffersizeInSamples((int)(sizeins*fs));//buffer size also changes
		
		tffs.setText(Double.toString(fs));
	}
		
	/**
	 * get buffer size in seconds
	 * @return
	 */
	public double buffersizeInSeconds()
	{
		return cmanager.getBufferSize()/format.getSampleRate();
	}
	
	/**
	 * set buffer size
	 * @param size
	 * buffer size in number of samples
	 */
	private void setBuffersizeInSamples(int size)
	{
	DecimalFormat bsformat;
	
		if(size==cmanager.getBufferSize()) return;

		cmanager.setBufferSize(size);
		pmanager.setBufferSize(size);
		
		bsformat=new DecimalFormat("0.000");
		tfbuffersize.setText(bsformat.format(buffersizeInSeconds()));
	}
	
	/**
	 * things need to be done when add channel button is clicked
	 */
	private void addChannelClicked()
	{
		if(getRecorderState()!=RecorderState.STOP) 
			showErrorMessageDialog("BSSRecorder is running: state="+getRecorderState());
		else cmanager.addChannel();
	}
	
	/**
	 * things need to be done when remove channel button is clicked
	 */
	private void removeChannelClicked()
	{
	int chidx;
	
		if(getRecorderState()!=RecorderState.STOP) 
			showErrorMessageDialog("BSSRecorder is running: state="+getRecorderState());
		else
		{
			chidx=cmanager.selectedChannelIndex();
			//no channel is selected
			if(chidx<0) showErrorMessageDialog("No channel is selected.");
			else cmanager.removeChannel(chidx);
		}
	}
	
	/**
	 * bind capture device
	 */
	private void bindDeviceClicked()
	{
		if(getRecorderState()!=RecorderState.STOP) 
			showErrorMessageDialog("BSSRecorder is running: state="+getRecorderState());
		else
		{
			try
			{
				new DeviceBindingDialog(this);
			}
			catch(LineUnavailableException e)
			{
				e.printStackTrace();
				showErrorMessageDialog("Failed to open device binding dialog.",e);
			}
		}
	}

	/**
	 * things need to be done when open wave file button is clicked
	 */
	private void openButtonClicked()
	{
	JFileChooser chooser;
	int chidx,retval;
	WaveSource ws=null;
	double[] data=null;
	CircularBuffer buffer;
			
		if(getRecorderState()!=RecorderState.STOP) 
		{	
			showErrorMessageDialog("BSSRecorder is running: state="+getRecorderState());
			return;
		}
		
		chidx=cmanager.selectedChannelIndex();
		
		//no channel is selected
		if(chidx<0) 
		{
			showErrorMessageDialog("No channel is selected.");
			return;
		}
		
		/*
		 * open file chooser
		 */
		if(currentdir==null) chooser=new JFileChooser();
		else chooser=new JFileChooser(currentdir);
		chooser.setFileFilter(new javax.swing.filechooser.FileFilter(){
			public boolean accept(File pathname)
			{
				if(pathname.isDirectory()) return true;
				else return pathname.getName().toLowerCase().endsWith(".wav");
			}

			public String getDescription()
			{
				return "*.wav";
			}
		});
		
		retval=chooser.showOpenDialog(BSSRecorder.this);
		if(retval==JFileChooser.APPROVE_OPTION)
		{
			currentdir=chooser.getCurrentDirectory();//save current directory
			
			try
			{
				ws=new WaveSource(chooser.getSelectedFile(),true);
				
				//adjust sampling rate if needed
				if(ws.audioFormat().getSampleRate()!=format.getSampleRate()) 
				{
				int conval;
				
					conval=JOptionPane.showConfirmDialog(
							this,
							"Sampling rate not match:\n\n"+
							"BSSRecorder:    "+format.getSampleRate()+" Hz\n"+
							"New wave file: "+ws.audioFormat().getSampleRate()+" Hz\n\n"+
							"Use new sampling rate?");
					
					if(conval==JOptionPane.YES_OPTION)
						this.setSamplingRate(ws.audioFormat().getSampleRate());
					else if(conval==JOptionPane.NO_OPTION)
					{}
					else return;
				}

				data=ws.toArray(data);
				buffer=cmanager.channelBuffer(chidx);
				
				setBuffersizeInSamples(data.length);
				buffer.setData(data);

				cmanager.updateChart();
			}
			catch(IOException e)
			{
				e.printStackTrace();
				showErrorMessageDialog("Failed to load wave file.",e);
			}
			catch(UnsupportedAudioFileException e)
			{
				e.printStackTrace();
				showErrorMessageDialog("Failed to load wave file.",e);
			}
			finally
			{
				try
				{
					if(ws!=null) ws.close();
				}
				catch(IOException e)
				{}
			}
		}		
	}
	
	/**
	 * save all channels into file
	 * @param prefix
	 * file prefix
	 * @throws IOException
	 */
	private void saveAllChannels(File prefix) throws IOException
	{
	PlaybackChartManager manager;
	CircularBuffer buffer;
	WaveSink sink=null;
	
		//only can save when the recorder is stopped
		if(getRecorderState()!=RecorderState.STOP) return;
		
		if(tab.getSelectedIndex()==0) manager=cmanager;
		else manager=pmanager;
		
		for(int chidx=0;chidx<manager.getNumChannels();chidx++) 
		{
			buffer=manager.channelBuffer(chidx);
			sink=new WaveSink(format,new File(prefix.getAbsoluteFile().toString()+chidx+".wav"));
			
			try
			{
				synchronized(buffer)
				{
					for(double sample:buffer) sink.writeSample(sample);
					sink.flush();
				}
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
		}
	}
	
	/**
	 * things need to be done when save button is pressed
	 */
	private void saveButtonPressed()
	{
	JFileChooser chooser;
	int retval;
		
		if(getRecorderState()!=RecorderState.STOP) 
		{
			showErrorMessageDialog("BSSRecorder is running: state="+getRecorderState());
			return;
		}
		
		chooser=new JFileChooser();
		retval=chooser.showSaveDialog(BSSRecorder.this);
		if(retval==JFileChooser.APPROVE_OPTION)
		{
			try
			{
				saveAllChannels(chooser.getSelectedFile());
			}
			catch(IOException e)
			{
				e.printStackTrace();
				showErrorMessageDialog("Failed to save wave files.",e);
			}
		}
	}
	
	/**
	 * show spectrogram
	 */
	private void spectrogramButtonPressed()
	{
	CircularBuffer buffer=null;
	CircularBufferSource source;
	SpectrogramViewer viewer;
	JDialog dialog;
	
		if(getRecorderState()!=RecorderState.STOP) 
		{
			showErrorMessageDialog("BSSRecorder is running: state="+getRecorderState());
			return;
		}
		
		/*
		 * get selected buffer
		 */
		if(tab.getSelectedIndex()==0) 
		{
			if(cmanager.selectedChannelIndex()>=0) 
				buffer=cmanager.channelBuffer(cmanager.selectedChannelIndex());
		}
		else if(tab.getSelectedIndex()==1)
		{
			if(pmanager.selectedChannelIndex()>=0) 
				buffer=pmanager.channelBuffer(pmanager.selectedChannelIndex());
		}
		//no channel is selected
		if(buffer==null) 
		{
			showErrorMessageDialog("No channel is selected.");
			return;
		}
		
		//show dialog
		try
		{
			source=new CircularBufferSource(buffer);
			viewer=new SpectrogramViewer(
					fdbss.stfTransformer(),
					source,
					format.getSampleRate());
			source.close();
			
			/*
			 * build and show dialog
			 */
			dialog=new JDialog(this,"Spectrogram",true);
			dialog.addWindowListener(new WindowAdapter(){
				public void windowClosing(WindowEvent e)
				{
				JDialog dialog;
				
					dialog=(JDialog)e.getSource();
					dialog.setVisible(false);
					/*
					 * !!!
					 * this must be called, or memory leak will occur
					 */
					dialog.dispose();
				}
			});
			dialog.add(viewer.spectrogramPanel(),BorderLayout.CENTER);
			
			//show dialog
			{
			int locx,locy;
			
				dialog.pack();

				locx=(this.getWidth()-dialog.getWidth())/2+this.getX();
				if(locx<0) locx=0;
				locy=(this.getHeight()-dialog.getHeight())/2+this.getY();
				if(locy<0) locy=0;
				dialog.setLocation(locx,locy);
				
				dialog.setVisible(true);
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
			showErrorMessageDialog("Failed to show spectrogram.",e);
		}
	}
	
	/**
	 * <h1>Description</h1>
	 * Used to perform bss.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Mar 17, 2012 8:26:56 AM, revision:
	 */
	private class BSSThread extends Thread
	{
	private JButton bbss;//the bss button
	
		/**
		 * @param bbss
		 * button starts the bss
		 */
		public BSSThread(JButton bbss)
		{
			this.bbss=bbss;
		}
		
		public void run()
		{
		SignalSource x=null;
		double[][] result;
		
			try
			{
				bbss.setEnabled(false);
				
				/*
				 * perform bss
				 */
				paramtable.setParameters(fdbss);//set parameters
				pmanager.setNumChannels(fdbss.numSources());
				x=cmanager.bufferSignalSource();
				fdbss.separate(x,bssop.operations());

				/*
				 * show results
				 */
				result=fdbss.estimatedSourceSignals();
				pmanager.setData(result);
			}
			catch(Exception e)
			{
				e.printStackTrace();//unexpected exceptions
				BSSRecorder.this.showErrorMessageDialog("Failed to perform BSS.",e);
			}
			finally
			{
				try
				{
					if(x!=null) x.close();
				}
				catch(IOException e)
				{}
				
				/*
				 * stop bss thread
				 */
				bssth=null;//the the only bss thread to null
				setRecorderState(RecorderState.STOP);
				bbss.setEnabled(true);
			}
		}
	}

	public static void main(String[] args) throws LineUnavailableException
	{
	BSSRecorder recorder=null;
	
		try
		{
			recorder=new BSSRecorder();
		}
		catch(Exception e)
		{
			e.printStackTrace();//unexpected exceptions
			if(recorder!=null) recorder.showErrorMessageDialog("Failed to perform BSS.",e);			
		}
	}
}
