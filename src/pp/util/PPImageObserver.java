package pp.util;
import java.io.*;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.imageio.*;
import javax.swing.*;

/**
 * <h1>Description</h1>
 * Used to see experiment result.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version last modified: Mar 30, 2010
 */
public class PPImageObserver extends JDialog
{
private static final long serialVersionUID=-402878248852385925L;
private static final int MIN_WIDTH=300;
private static final int MIN_HEIGHT=200;
//supported image formats
private static final Set<String> SUPPORTED_FORMATS=new HashSet<String>();

	static
	{
		SUPPORTED_FORMATS.add("jpg");
		SUPPORTED_FORMATS.add("jpeg");
		SUPPORTED_FORMATS.add("png");
		SUPPORTED_FORMATS.add("bmp");
		SUPPORTED_FORMATS.add("gif");
	}

private ImagePanel imgpanel;
private JLabel lcoordinate;//used to show pixel coordinate

	/**
	 * @param img
	 * the image need to be shown
	 */
	public PPImageObserver(BufferedImage img)
	{
		super((Frame)null,"PP Image Observer");
		this.addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent e)
			{
				System.exit(0);
			}
		});
		this.getContentPane().setLayout(new BorderLayout(5,5));
		initGUI(img);
		/*
		 * show frame
		 */
		this.setSize(
				Math.max(img.getWidth()+20,MIN_WIDTH),
				Math.max(img.getHeight()+80,MIN_HEIGHT));
		DisplayMode dmode=GraphicsEnvironment.getLocalGraphicsEnvironment().
			getDefaultScreenDevice().getDisplayMode();
		this.setLocation(
				Math.max((dmode.getWidth()-this.getWidth())/2,0),
				Math.max((dmode.getHeight()-this.getHeight())/2,0));
		this.setVisible(true);
	}
	
	/**
	 * Show image in a dialog. Dialog location is not set, and user still 
	 * need to call setVisible(true) to show the dialog.
	 * @param owner
	 * dialog owner
	 * @param img
	 * image need to show
	 */
	public PPImageObserver(Frame owner,BufferedImage img)
	{
		super(owner,"PP Image Observer");
		this.addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent e)
			{
				PPImageObserver.this.setVisible(false);
			}
		});
		this.getContentPane().setLayout(new BorderLayout(5,5));
		initGUI(img);
		/*
		 * show dialog
		 */
		this.setSize(
				Math.max(img.getWidth()+20,MIN_WIDTH),
				Math.max(img.getHeight()+80,MIN_HEIGHT));
	}
	
	/**
	 * initialize GUI
	 * @param img
	 * image need to show
	 */
	private void initGUI(BufferedImage img)
	{
		/*
		 * panel used to show image
		 */
		imgpanel=new ImagePanel(img);
		imgpanel.addMouseMotionListener(new MouseMotionAdapter(){
			public void mouseMoved(MouseEvent e)
			{
				lcoordinate.setText("  ("+(e.getX()-imgpanel.originX())+", "+(e.getY()-imgpanel.originY())+")");
			}
		});
		this.getContentPane().add(imgpanel,BorderLayout.CENTER);
		/*
		 * the south panel
		 */
		JPanel psouth=new JPanel();
		psouth.setLayout(new GridLayout(1,2,5,5));
		this.getContentPane().add(psouth,BorderLayout.SOUTH);
		/*
		 * pixel coordinate label
		 */
		lcoordinate=new JLabel();
		psouth.add(lcoordinate);
		/*
		 * save as button
		 */
		JPanel psave=new JPanel();
		psave.setLayout(new FlowLayout(FlowLayout.RIGHT,5,5));
		psouth.add(psave);
		
		JButton bsave=new JButton("Save As...");
		bsave.addActionListener(new ActionListener(){
		private Pattern p=Pattern.compile("^(.+)\\.(.+)$");	
			
			public void actionPerformed(ActionEvent e)
			{
			JFileChooser chooser;
			int retval;
			File destpath;
			Matcher m;
			String format;
			
				chooser=new JFileChooser();
				retval=chooser.showSaveDialog(PPImageObserver.this);
				if(retval==JFileChooser.APPROVE_OPTION)
				{
					try
					{
						destpath=chooser.getSelectedFile();
						/*
						 * check file name and format
						 */
						m=p.matcher(destpath.getName());
						if(!m.find()) throw new IOException(
								"illegal image name: "+destpath.getName());
						format=m.group(2);
						if(!SUPPORTED_FORMATS.contains(format.toLowerCase())) 
							throw new IOException("unsupported image format: "+format);
						ImageIO.write(
								imgpanel.image(),
								format,
								destpath);
					}
					catch(IOException exc)
					{
						JOptionPane.showMessageDialog(
								PPImageObserver.this,
								exc.getMessage(),
								"Failed to save image",
								JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		});
		psave.add(bsave);
	}
}
