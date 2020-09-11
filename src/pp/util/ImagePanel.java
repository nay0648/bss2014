package pp.util;
import java.awt.*;
import java.awt.image.*;
import javax.swing.*;

/**
 * <h1>Description</h1>
 * Used to draw image on panel.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version last modified: Mar 30, 2010
 */
public class ImagePanel extends JPanel
{
private static final long serialVersionUID=-4435869310383338585L;
private BufferedImage img;
private int x0,y0;//the image origin

	/**
	 * @param img
	 * the image need to show
	 */
	public ImagePanel(BufferedImage img)
	{
		this.img=img;
	}
	
	/**
	 * get image width
	 * @return
	 */
	public int imageWidth()
	{
		return img.getWidth();
	}
	
	/**
	 * get image height
	 * @return
	 */
	public int imageHeight()
	{
		return img.getHeight();
	}
	
	/**
	 * get the image
	 * @return
	 */
	public BufferedImage image()
	{
		return img;
	}
	
	/**
	 * get x coordinate of the origin
	 * @return
	 */
	public int originX()
	{
		return x0;
	}

	/**
	 * get y coordinate of the origin
	 * @return
	 */
	public int originY()
	{
		return y0;
	}
	
	public void paintComponent(Graphics g)
	{
		super.paintComponents(g);
		x0=(this.getWidth()-img.getWidth())/2;
		if(x0<0) x0=0;
		y0=(this.getHeight()-img.getHeight())/2;
		if(y0<0) y0=0;
		g.drawImage(img,x0,y0,null);
	}
}
