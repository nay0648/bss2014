package pp.util;
import java.io.*;
import java.awt.*;
import java.awt.image.*;

/**
 * <h1>Description</h1>
 * Some utility methods.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version last modified: Dec 9, 2009
 */
public class Util implements Serializable
{
private static final long serialVersionUID=-5925645488248582955L;
	
	/**
	 * convert an arbitrary 2-dimension array to an image
	 * @param data
	 * input data
	 * @return
	 * A RGB image, but its content is still gray.
	 */
	public static BufferedImage toImage(double[][] data)
	{
	BufferedImage img;
	double max=Double.MIN_VALUE,min=Double.MAX_VALUE;
	double factor;
	int temp,rgb;
		
		//find the max and the min value of the input
		for(int y=0;y<data.length;y++)
			for(int x=0;x<data[y].length;x++)
			{
				if(data[y][x]>max) max=data[y][x];
				if(data[y][x]<min) min=data[y][x];
			}
		/*
		 * Use color image to prevent different gray level model transformation, 
		 * here a pixel with the same R, G, B value stands for a gray color.
		 */
		img=new BufferedImage(data[0].length,data.length,BufferedImage.TYPE_INT_RGB);
		/*
		 * normalize value to 0~255
		 */
		if(min<0)
		{
			factor=255/(max-min);//compensate min value to zero
			for(int y=0;y<data.length;y++)
				for(int x=0;x<data[y].length;x++)
				{
					temp=(int)((data[y][x]-min)*factor);
					rgb=temp;
					rgb|=temp<<8;
					rgb|=temp<<16;
					img.setRGB(x,y,rgb);
				}
		}
		else
		{
			factor=255/max;
			for(int y=0;y<data.length;y++)
				for(int x=0;x<data[y].length;x++)
				{
					temp=(int)(data[y][x]*factor);
					rgb=temp;
					rgb|=temp<<8;
					rgb|=temp<<16;
					img.setRGB(x,y,rgb);
				}
		}
		return img;
	}
	
	/**
	 * convert a map matrix to an image
	 * @param map
	 * Its values range from 0 to 1, values smaller than 0 or larger than 1 
	 * will be turncated.
	 * @return
	 */
	public static BufferedImage mapToImage(double[][] map)
	{
	BufferedImage img;
	int temp,rgb;
		
		img=new BufferedImage(map[0].length,map.length,BufferedImage.TYPE_INT_RGB);
		for(int y=0;y<map.length;y++)
			for(int x=0;x<map[y].length;x++)
			{
				temp=(int)(map[y][x]*255.0);
				//turncate
				if(temp<0) temp=0;
				else if(temp>255) temp=255;
				rgb=temp;
				rgb|=temp<<8;
				rgb|=temp<<16;
				img.setRGB(x,y,rgb);
			}
		return img;
	}
	
	/**
	 * Convert data into image, data will be turncated to the range of 0..255 
	 * if data value exceeds this range.
	 * @param data
	 * input data
	 * @return
	 */
	public static BufferedImage turncatedToImage(double[][] data)
	{
	BufferedImage img;
	int temp,rgb;
	
		img=new BufferedImage(data[0].length,data.length,BufferedImage.TYPE_INT_RGB);
		for(int y=0;y<data.length;y++)
			for(int x=0;x<data[y].length;x++)
			{
				temp=(int)data[y][x];
				//turncate
				if(temp<0) temp=0;
				else if(temp>255) temp=255;
				rgb=temp;
				rgb|=temp<<8;
				rgb|=temp<<16;
				img.setRGB(x,y,rgb);
			}
		return img;
	}
	
	/**
	 * convert an image to a 2D array
	 * @param image
	 * an image
	 * @return
	 * each entry value belongs to [0, 255], represents the grayscale value
	 */
	public static double[][] image2GrayscaleMap(BufferedImage image)
	{
	WritableRaster raster;
	double[] pixel;
	double[][] map;
	
		raster=image.getRaster();
		pixel=new double[raster.getNumBands()];
		map=new double[image.getHeight()][image.getWidth()];
		for(int y=0;y<raster.getHeight();y++)
			for(int x=0;x<raster.getWidth();x++)
			{
				raster.getPixel(x,y,pixel);
				map[y][x]=BLAS.mean(pixel);
			}
		return map;
	}
	
	/**
	 * draw original image and result image into one image
	 * @param origin
	 * original image
	 * @param result
	 * result image
	 * @return
	 */
	public static BufferedImage drawResult(BufferedImage origin,BufferedImage result)
	{
	BufferedImage img;
	Graphics g;
	
		img=new BufferedImage(
				//with one pixel gap
				origin.getWidth()+result.getWidth()+1,
				Math.max(origin.getHeight(),result.getHeight()),
				result.getType());
		g=img.getGraphics();
		g.drawImage(origin,0,0,null);
		g.drawImage(result,origin.getWidth()+1,0,null);
		return img;
	}
	
	/**
	 * draw several image into one large image to show result
	 * @param row, column
	 * image matrix size
	 * @param padding
	 * padding pixels between images
	 * @param images
	 * several images
	 * @return
	 */
	public static BufferedImage drawResult(
			int row,int column,int padding,BufferedImage... images)
	{
	int maxw=0,maxh=0;
	BufferedImage result,subimg;
	Graphics g;
	int index=0;
	int x0,y0;
	
		if(images.length>row*column) throw new IllegalArgumentException(
				"illgal number of images: "+images.length+
				", required: less than or equal to "+(row*column));
		//find the max image width and height
		for(BufferedImage img:images)
		{
			if(img.getWidth()>maxw) maxw=img.getWidth();
			if(img.getHeight()>maxh) maxh=img.getHeight();
		}
		//the result image
		result=new BufferedImage(
				maxw*column+padding*(column-1),
				maxh*row+padding*(row-1),
				BufferedImage.TYPE_INT_ARGB);
		/*
		 * fill background transparent
		 */
		g=result.getGraphics();
		g.setColor(new Color(0,0,0,0));
		g.fillRect(0,0,result.getWidth(),result.getHeight());
		/*
		 * draw images
		 */
		y0=0;
draw:	for(int i=0;i<row;i++)
		{
			x0=0;
			for(int j=0;j<column;j++)
			{
				subimg=images[index++];
				g.drawImage(subimg,x0,y0,null);
				x0+=maxw+padding;
				if(index>=images.length) break draw;
			}
			y0+=maxh+padding;
		}
		return result;
	}

	/**
	 * show experiment result
	 * @param img
	 * result image
	 */
	public static void showImage(BufferedImage img)
	{
		new PPImageObserver(img);
	}
}
