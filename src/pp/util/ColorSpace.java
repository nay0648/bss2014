package pp.util;
import java.io.*;
import java.util.*;

/**
 * <h1>Description</h1>
 * Color transformation among different color spaces. Some methods are 
 * based on wikipedia Pascal Getreuer's colorspace matlab code.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version last modified: Sep 7, 2009
 */
public class ColorSpace implements Serializable
{
private static final long serialVersionUID=2567058772841425713L;
/*
 * transformation pairs between RGB and XYZ
 */
private static final double[][] RGB2XYZ=
	{{0.412453220144161,0.357579581293500,0.180422589970536},
	 {0.212671121341218,0.715159205310783,0.072168776776132},
	 {0.019333816461991,0.119193540206625,0.950226922289707}};
private static final double[][] XYZ2RGB=
	{{3.240479,-1.53715,-0.498535},
	 {-0.969256,1.875992,0.041556},
	 {0.055648,-0.204043,1.057311}};
/*
 * for XYZ to LUV
 */
//white point for matrices / 709 + D65 / sRGB
private static final double[] WHITE_POINT={0.950456,1,1.088754};
private static final double UN=4*WHITE_POINT[0]/(WHITE_POINT[0]+15*WHITE_POINT[1]+3*WHITE_POINT[2]);
private static final double VN=9*WHITE_POINT[1]/(WHITE_POINT[0]+15*WHITE_POINT[1]+3*WHITE_POINT[2]);

	/**
	 * convert color from RGB color space to HSV color space
	 * @param rgb
	 * The red, green, blue component, each component belongs to [0, 1].
	 * @param hsv
	 * Destination for the hue, saturation, value component, each component 
	 * belongs to [0, 1].
	 */
	public static void rgb2HSV(double[] rgb,double[] hsv)
	{
	double cmax,cmin,redc,greenc,bluec,temp;
	
		cmax=Math.max(Math.max(rgb[0],rgb[1]),rgb[2]);
		cmin=Math.min(Math.min(rgb[0],rgb[1]),rgb[2]);
		temp=cmax-cmin;
		hsv[2]=cmax;//the value component
		//the saturation component
		if(cmax==0) hsv[1]=0;else hsv[1]=temp/cmax;
		/*
		 * the hue component
		 */
		if(cmax==cmin) hsv[0]=0;//hue is undifined
		else
		{
			redc=(cmax-rgb[0])/temp;
			greenc=(cmax-rgb[1])/temp;
			bluec=(cmax-rgb[2])/temp;
			if(rgb[0]==cmax) hsv[0]=bluec-greenc;
			else if(rgb[1]==cmax) hsv[0]=2+redc-bluec;
			else hsv[0]=4+greenc-redc;
			hsv[0]/=6;
			if(hsv[0]<0) hsv[0]+=1;
		}
	}
	
	/**
	 * convert color from HSV color space to RGB color space
	 * @param hsv
	 * The hue, saturation, value component, each component belongs to [0, 1].
	 * @param rgb
	 * Destination for the red, green, blue component, each 
	 * component belongs to [0, 1].
	 */
	public static void hsv2RGB(double[] hsv,double[] rgb)
	{
	double h,f,p,q,t;
	
		//only contains gray information when saturation is 0
		if(hsv[1]==0)
		{
			rgb[0]=hsv[2];
			rgb[1]=hsv[2];
			rgb[2]=hsv[2];
		}
		else
		{
			h=(hsv[0]-Math.floor(hsv[0]))*6;
			f=h-Math.floor(h);
			p=hsv[2]*(1-hsv[1]);
			q=hsv[2]*(1-hsv[1]*f);
			t=hsv[2]*(1-hsv[1]*(1-f));
			switch((int)h)
			{
				case 0:
				{
					rgb[0]=hsv[2];
					rgb[1]=t;
					rgb[2]=p;
				}break;
				case 1:
				{
					rgb[0]=q;
					rgb[1]=hsv[2];
					rgb[2]=p;
				}break;
				case 2:
				{
					rgb[0]=p;
					rgb[1]=hsv[2];
					rgb[2]=t;
				}break;
				case 3:
				{
					rgb[0]=p;
					rgb[1]=q;
					rgb[2]=hsv[2];
				}break;
				case 4:
				{
					rgb[0]=t;
					rgb[1]=p;
					rgb[2]=hsv[2];
				}break;
				case 5:
				{
					rgb[0]=hsv[2];
					rgb[1]=p;
					rgb[2]=q;
				}break;
				default: throw new RuntimeException("illegal hue: "+h);
			}
		}
	}
	
	/**
	 * convert from RGB to XYZ
	 * @param rgb
	 * R, G, B components.
	 * @param xyz
	 * Space for the X, Y, Z components.
	 */
	public static void rgb2XYZ(double[] rgb,double[] xyz)
	{
	double[] rgb2;
	
		rgb2=new double[3];
		//Undo gamma correction, because the rgb color is gamma corrected.
		ColorSpace.invGammaCorrection(rgb,rgb2);
		xyz[0]=RGB2XYZ[0][0]*rgb2[0]+RGB2XYZ[0][1]*rgb2[1]+RGB2XYZ[0][2]*rgb2[2];
		xyz[1]=RGB2XYZ[1][0]*rgb2[0]+RGB2XYZ[1][1]*rgb2[1]+RGB2XYZ[1][2]*rgb2[2];
		xyz[2]=RGB2XYZ[2][0]*rgb2[0]+RGB2XYZ[2][1]*rgb2[1]+RGB2XYZ[2][2]*rgb2[2];
	}
	
	/**
	 * convert from XYZ to RGB
	 * @param xyz
	 * X, Y, Z components.
	 * @param rgb
	 * Space for the r, g, b components.
	 */
	public static void xyz2RGB(double[] xyz,double[] rgb)
	{
	double pad,scale;
	
		rgb[0]=XYZ2RGB[0][0]*xyz[0]+XYZ2RGB[0][1]*xyz[1]+XYZ2RGB[0][2]*xyz[2];
		rgb[1]=XYZ2RGB[1][0]*xyz[0]+XYZ2RGB[1][1]*xyz[1]+XYZ2RGB[1][2]*xyz[2];
		rgb[2]=XYZ2RGB[2][0]*xyz[0]+XYZ2RGB[2][1]*xyz[1]+XYZ2RGB[2][2]*xyz[2];
		/*
		 * desaturate and rescale to constrain resulting RGB values to [0,1]
		 */
		pad=-Math.min(Math.min(Math.min(rgb[0],rgb[1]),rgb[2]),0);
		scale=Math.max(Math.max(Math.max(rgb[0],rgb[1]),rgb[2])+pad,1);
		for(int i=0;i<rgb.length;i++) rgb[i]=(rgb[i]+pad)/scale;
		ColorSpace.gammaCorrection(rgb,rgb);//perform the gamma correction
	}
	
	/**
	 * convert from RGB color space to CIE L*u*v* color space
	 * @param rgb
	 * The R, G, B components belong to [0, 1].
	 * @param luv
	 * Sapce for the L*, u*, v* components.
	 */
	public static void rgb2LUV(double[] rgb,double[] luv)
	{
	double[] cxyz;
	double temp;
	
		/*
		 * convert RGB to XYZ
		 */
		cxyz=new double[3];
		ColorSpace.rgb2XYZ(rgb,cxyz);
		//The L* component, (6/29)^3=0.00885645167904, 1/3=0.333333333333.
		if((cxyz[1]/WHITE_POINT[1])>0.00885645167904) 
			luv[0]=116*Math.pow(cxyz[1]/WHITE_POINT[1],0.333333333333)-16;
		else luv[0]=903.296296296*(cxyz[1]/WHITE_POINT[1]);//(29/3)^3=903.296296296
		/*
		 * The u*, v* components.
		 */
		temp=cxyz[0]+15*cxyz[1]+3*cxyz[2];
		if(temp!=0)
		{
			luv[1]=13*luv[0]*(4*cxyz[0]/temp-UN);
			luv[2]=13*luv[0]*(9*cxyz[1]/temp-VN);
		}
		else
		{
			luv[1]=0;
			luv[2]=0;
		}
	}
	
	/**
	 * convert from RGB color space to CIE L*a*b* color space
	 * @param rgb
	 * The R, G, B components belong to [0, 1].
	 * @param lab
	 * Sapce for the L*, a*, b* components.
	 */
	public static void rgb2LAB(double[] rgb,double[] lab)
	{
	double[] cxyz;
	double fx,fy,fz;
	
		/*
		 * convert RGB to XYZ
		 */
		cxyz=new double[3];
		ColorSpace.rgb2XYZ(rgb,cxyz);
		/*
		 * convert xyz to lab
		 */
		fx=f(cxyz[0]/WHITE_POINT[0]);
		fy=f(cxyz[1]/WHITE_POINT[1]);
		fz=f(cxyz[2]/WHITE_POINT[2]);
		lab[0]=116*fy-16;
		lab[1]=500*(fx-fy);
		lab[2]=200*(fy-fz);   
	}
	
	/**
	 * f(t)=t^1/3 if t>(6/29)^3, else f(t)=(1/3)*(29/6)^2*t+4/29, this 
	 * function is used in xyz to lab and luv.
	 * @param t
	 * @return
	 */
	private static double f(double t)
	{
		//(6/29)^3=0.00885645167904, 1/3=0.333333333333
		if(t>0.00885645167904) return Math.pow(t,0.333333333333);
		//(1/3)*(29/6)^2=7.787037037037037, 4/29=0.13793103448275862
		else return 7.787037037037037*t+0.13793103448275862;
	}
	
	/**
	 * perform the gamma correction
	 * @param c
	 * the color vector
	 * @param cprime
	 * to store the corrected color
	 */
	public static void gammaCorrection(double[] c,double[] cprime)
	{	
		for(int i=0;i<c.length;i++)
			if(c[i]>0.018)
				cprime[i]=1.099*Math.pow(c[i],0.45)-0.099;
			else cprime[i]=c[i]*4.5138;
	}
	
	/**
	 * perform the inverse gamma correction
	 * @param cprime
	 * the corrected color vector
	 * @param c
	 * to store the uncorrected color
	 */
	public static void invGammaCorrection(double[] cprime,double[] c)
	{
		for(int i=0;i<cprime.length;i++)
		{
			//1/0.45=2.22222222222
			c[i]=Math.pow((cprime[i]+0.099)/1.099,2.22222222222);
			if(c[i]<=0.018) c[i]=cprime[i]/4.5138;
		}
	}
	
	/**
	 * calculate the square error
	 * @param v1, v2
	 * two vectors
	 * @return
	 */
	public static double squareError(double[] v1,double[] v2)
	{
	double eps=0,temp;
	
		if(v1.length!=v2.length) throw new IllegalArgumentException(
				"vector length do not match: "+v1.length+", "+v2.length);
		for(int i=0;i<v1.length;i++)
		{
			temp=v2[i]-v1[i];
			eps+=temp*temp;
		}
		return Math.sqrt(eps);
	}

	public static void main(String[] args)
	{
	double[] rgb={0.278498218867048,   0.546881519204984,   0.957506835434298},dest,rgb2;

		dest=new double[3];
		rgb2=new double[3];
		rgb[0]=Math.random();
		rgb[1]=Math.random();
		rgb[2]=Math.random();
		System.out.println("RGB: "+Arrays.toString(rgb)+"\n");

		ColorSpace.rgb2HSV(rgb,dest);
		System.out.println("HSV: "+Arrays.toString(dest));
		ColorSpace.hsv2RGB(dest,rgb2);
		System.out.println("RGB: "+Arrays.toString(rgb2));
		System.out.println("error: "+ColorSpace.squareError(rgb,rgb2)+"\n");
		
		ColorSpace.rgb2XYZ(rgb,dest);
		System.out.println("XYZ: "+Arrays.toString(dest));
		ColorSpace.xyz2RGB(dest,rgb2);
		System.out.println("RGB: "+Arrays.toString(rgb2));
		System.out.println("error: "+ColorSpace.squareError(rgb,rgb2)+"\n");
		
		ColorSpace.rgb2LUV(rgb,dest);
		System.out.println("LUV: "+Arrays.toString(dest));
		
		System.out.println();
		ColorSpace.rgb2LAB(rgb,dest);
		System.out.println("LAB: "+Arrays.toString(dest));
	}
}
