package cn.edu.bjtu.cit.bss.eval;
import java.io.*;
import java.util.*;
import cn.edu.bjtu.cit.bss.util.*;

/**
 * <h1>Description</h1>
 * Room Impulse Response generator, modified from E.A.P. Habets' code.
 * See: http://home.tiscali.nl/ehabets/rir_generator.html
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Mar 27, 2012 3:16:52 PM, revision:
 */
public class RIRGenerator implements Serializable
{
private static final long serialVersionUID=-3009637100202238814L;
private static final double M_PI=3.14159265358979323846;
private double c;//signal propagation speed (m/s)
private double fs;//sampling rate (Hz)
private double[] roomsize;//room size x, y, z (m)
private double rt60;//reverberation time (s)

	/**
	 * @param c
	 * signal propagation speed (m/s)
	 * @param fs
	 * sampling rate (Hz)
	 * @param roomsize
	 * room size x, y, z (m)
	 * @param rt60
	 * reverberation time (s)
	 */
	public RIRGenerator(double c,double fs,double[] roomsize,double rt60)
	{
		this.c=c;
		this.fs=fs;
		
		if(roomsize.length!=3) throw new IllegalArgumentException(
				"illegal room size: "+Arrays.toString(roomsize));
		this.roomsize=roomsize;
		
		this.rt60=rt60;
	}
	
	/**
	 * get signal propagation velocity in m/s
	 * @return
	 */
	public double propagationVelocity()
	{
		return c;
	}
	
	/**
	 * get sampling rate in Hz
	 * @return
	 */
	public double samplingRate()
	{
		return fs;
	}
	
	/**
	 * get room size x, y, z in meters
	 * @return
	 */
	public double[] roomSize()
	{
		return roomsize;
	}
	
	/**
	 * get room reverberation time in seconds
	 * @return
	 */
	public double reverberationTime()
	{
		return rt60;
	}

	/**
	 * show usage
	 */
	public static void usage()
	{
		System.out.println("--------------------------------------------------------------------\n"+
				"| Room Impulse Response Generator                                  |\n"+
				"|                                                                  |\n"+
				"| Computes the response of an acoustic source to one or more       |\n"+
	            "| microphones in a reverberant room using the image method [1,2].  |\n"+
				"|                                                                  |\n"+
				"| Author    : dr.ir. Emanuel Habets (ehabets@dereverberation.org)  |\n"+
				"|                                                                  |\n"+
				"| Version   : 2.0.20100920                                         |\n"+
				"|                                                                  |\n"+
				"| Copyright (C) 2003-2010 E.A.P. Habets, The Netherlands.          |\n"+
				"|                                                                  |\n"+
				"| [1] J.B. Allen and D.A. Berkley,                                 |\n"+
				"|     Image method for efficiently simulating small-room acoustics,|\n"+
				"|     Journal Acoustic Society of America,                         |\n"+
				"|     65(4), April 1979, p 943.                                    |\n"+
				"|                                                                  |\n"+
				"| [2] P.M. Peterson,                                               |\n"+
				"|     Simulating the response of multiple microphones to a single  |\n"+
				"|     acoustic source in a reverberant room, Journal Acoustic      |\n"+
				"|     Society of America, 80(5), November 1986.                    |\n"+
				"--------------------------------------------------------------------\n\n"+
				"function [h, beta_hat] = rir_generator(c, fs, r, s, L, beta, nsample,\n"+
	            " mtype, order, dim, orientation, hp_filter);\n\n"+
				"Input parameters:\n"+
				" c           : sound velocity in m/s.\n"+
				" fs          : sampling frequency in Hz.\n"+
				" r           : M x 3 array specifying the (x,y,z) coordinates of the\n"+
	            "               receiver(s) in m.\n"+
				" s           : 1 x 3 vector specifying the (x,y,z) coordinates of the\n"+
	            "               source in m.\n"+
				" L           : 1 x 3 vector specifying the room dimensions (x,y,z) in m.\n"+
				" beta        : 1 x 6 vector specifying the reflection coefficients\n"+
				"               [beta_x1 beta_x2 beta_y1 beta_y2 beta_z1 beta_z2] or\n"+
	            "               beta = Reverberation Time (T_60) in seconds.\n"+
				" nsample     : number of samples to calculate, default is T_60*fs.\n"+
				" mtype       : [omnidirectional, subcardioid, cardioid, hypercardioid,\n"+
	            "               bidirectional], default is omnidirectional.\n"+
				" order       : reflection order, default is -1, i.e. maximum order.\n"+
				" dim         : room dimension (2 or 3), default is 3.\n"+
				" orientation : direction in which the microphones are pointed, specified using\n"+
	            "               azimuth and elevation angles (in radians), default is [0 0].\n"+
				" hp_filter   : use 'false' to disable high-pass filter, the high-pass filter\n"+
				"               is enabled by default.\n\n"+
				"Output parameters:\n"+
				" h           : M x nsample matrix containing the calculated room impulse\n"+
	            "               response(s).\n"+
				" beta_hat    : In case a reverberation time is specified as an input parameter\n"+
				"               the corresponding reflection coefficient is returned.\n\n");
		
		System.out.print("Room Impulse Response Generator (Version 2.0.20100920) by Emanuel Habets\n"+
			"Copyright (C) 2003-2010 E.A.P. Habets, The Netherlands.\n");
	}

	private static double sinc(double x)
	{
		if (x == 0) return(1.);
		else return (Math.sin(x)/x);
	}
	
	private static double sim_microphone(double x, double y, double z, double[] angle, char mtype)
	{
	    if (mtype=='b' || mtype=='c' || mtype=='s' || mtype=='h')
	    {
	        double strength, vartheta, varphi, alpha=0;

	        // Polar Pattern         alpha
	        // ---------------------------
	        // Bidirectional         0
	        // Hypercardioid         0.25    
	        // Cardioid              0.5
	        // Subcardioid           0.75
	        // Omnidirectional       1

	        switch(mtype)
	        {
	        case 'b':
	            alpha = 0;
	            break;
	        case 'h':
	            alpha = 0.25;	
	            break;
	        case 'c':
	            alpha = 0.5;
	            break;
	        case 's':
	            alpha = 0.75;
	            break;
	        };
	                
	        vartheta = Math.acos(z/Math.sqrt(Math.pow(x,2)+Math.pow(y,2)+Math.pow(z,2)));
	        varphi = Math.atan2(y,x);

	        strength = Math.sin(M_PI/2-angle[1]) * Math.sin(vartheta) * Math.cos(angle[0]-varphi) + Math.cos(M_PI/2-angle[1]) * Math.cos(vartheta);
	        strength = alpha + (1-alpha) * strength;
	                
	        return strength;
	    }
	    else
	    {
	        return 1;
	    }
	}
	
	/**
	 * generate room impulse response
	 * @param c
	 * sound velocity in m/s
	 * @param fs
	 * sampling frequency in Hz
	 * @param rl
	 * 1 x 3 array specifying the (x,y,z) coordinates of the receiver(s) in m
	 * @param sl
	 * 1 x 3 vector specifying the (x,y,z) coordinates of the source in m
	 * @param rsize
	 * 1 x 3 vector specifying the room dimensions (x,y,z) in m
	 * @param beta2
	 * 1 x 6 vector specifying the reflection coefficients [beta_x1 beta_x2 beta_y1 beta_y2 beta_z1 beta_z2] 
	 * or beta = Reverberation Time (T_60) in seconds
	 * @return
	 */
	private static double[] rir(double c,double fs,double[] rl,double[] sl,double[] rsize,double[] beta2)
	{
		
//		function [h, beta_hat] = rir_generator(c, fs, r, s, L, beta, nsample,
//				 mtype, order, dim, orientation, hp_filter);
		
		// Load parameters
		double[] 	rr=rl;
		int 		nr_of_mics=1;
		double[] 	ss=sl;
		double[] 	LL=rsize;
		double[] 	beta_ptr=beta2;
		double[]	beta=new double[6];
		int 		nsamples;
		char[] 		mtype;
		int 		order;
//		int 		dim;
		double[] 	angle=new double[2];
		int 		hp_filter;
		double 		TR=0;
		
		double[] beta_hat = {0};
		
		// Reflection coefficients or Reverberation Time?
		if (beta_ptr.length==1)
		{
			double V = LL[0]*LL[1]*LL[2];
			double S = 2*(LL[0]*LL[2]+LL[1]*LL[2]+LL[0]*LL[1]);
			TR = beta_ptr[0];
			double alfa = 24*V*Math.log(10.0)/(c*S*TR);
			if (alfa > 1)
				throw new IllegalArgumentException(
						"Error: The reflection coefficients cannot be calculated using the current "+
						"room parameters, i.e. room size and reverberation time.Please "+
						"specify the reflection coefficients or change the room parameters.");
			beta_hat[0] = Math.sqrt(1-alfa);
			for (int i=0;i<6;i++)
				beta[i] = beta_hat[0];
		}
		else
		{
			for (int i=0;i<6;i++)
				beta[i] = beta_ptr[i];
		}
		
		// High-pass filter (optional)
//		if (nrhs > 11 &&  mxIsEmpty(prhs[11]) == false)
//		{
//			hp_filter = (int) mxGetScalar(prhs[11]);
//		}
//		else
//		{
			hp_filter = 1;
//		}
		
		// 3D Microphone orientation (optional)
//		if (nrhs > 10 &&  mxIsEmpty(prhs[10]) == false)
//		{
//	        const double* orientation = mxGetPr(prhs[10]);
//	        if (mxGetN(prhs[10]) == 1)
//	        {     
//	            angle[0] = orientation[0];
//	            angle[1] = 0;
//	        }
//	        else
//	        {
//	            angle[0] = orientation[0];
//	            angle[1] = orientation[1];
//	        }
//		}
//		else
//		{
			angle[0] = 0;
	        angle[1] = 0;
//		}
		
    	// Room Dimension (optional)
//    	if (nrhs > 9 &&  mxIsEmpty(prhs[9]) == false)
//    	{
//    		dim = (int) mxGetScalar(prhs[9]);
//    		if (dim != 2 && dim != 3)
//    			mexErrMsgTxt("Invalid input arguments!");
//
//    		if (dim == 2)
//    		{
//    			beta[4] = 0;
//    			beta[5] = 0;
//    		}
//    	}
//    	else
//    	{
//    		dim = 3;
//    	}
		
   		// Reflection order (optional)
//   	if (nrhs > 8 &&  mxIsEmpty(prhs[8]) == false)
//   	{
//   		order = (int) mxGetScalar(prhs[8]);
//   		if (order < -1)
//   			mexErrMsgTxt("Invalid input arguments!");
//   	}
//   	else
//   	{
   			order = -1;
//   	}
		
		// Type of microphone (optional)
//		if (nrhs > 7 &&  mxIsEmpty(prhs[7]) == false)
//		{
//			mtype = new char[mxGetN(prhs[7])+1];
//			mxGetString(prhs[7], mtype, mxGetN(prhs[7])+1);
//		}
//		else
//		{
			mtype = new char[1];
			mtype[0] = 'o';
//		}
		
		// Number of samples (optional)
//		if (nrhs > 6 &&  mxIsEmpty(prhs[6]) == false)
//		{
//			nsamples = (int) mxGetScalar(prhs[6]);
//		}
//		else
//		{
			if (beta_ptr.length>1)
			{
				double V = LL[0]*LL[1]*LL[2];
//				double S = 2*(LL[0]*LL[2]+LL[1]*LL[2]+LL[0]*LL[1]);
				double alpha = ((1-Math.pow(beta[0],2))+(1-Math.pow(beta[1],2)))*LL[0]*LL[2] +
					((1-Math.pow(beta[2],2))+(1-Math.pow(beta[3],2)))*LL[1]*LL[2] +
					((1-Math.pow(beta[4],2))+(1-Math.pow(beta[5],2)))*LL[0]*LL[1];
				TR = 24*Math.log(10.0)*V/(c*alpha);
				if (TR < 0.128)
					TR = 0.128;
			}
			nsamples = (int) (TR * fs);
//		}
		
		// Create output vector
		double[] imp=new double[nsamples];
		
		// Temporary variables and constants (high-pass filter)
		double W = 2*M_PI*100/fs;
		double R1 = Math.exp(-W);
		double B1 = 2*R1*Math.cos(W);
		double B2 = -R1 * R1;
		double A1 = -(1+R1);
		double       X0;
	    double[]     Y = new double[3];
	    
		// Temporary variables and constants (image-method)
		double Fc = 1;
		int    Tw = 2 * (int)Math.round(0.004*fs);
		double cTs = c/fs;
		double[]     hanning_window = new double[Tw+1];
		double[]     LPI = new double[Tw+1];
		double[]     r = new double[3];
		double[]     s = new double[3];
		double[]     L = new double[3];
		double[]     hu=new double[6];
		double[]     refl=new double[3];
		double       dist;
//		double       ll;
		double       strength;
		int          pos, fdist;
		int          n1,n2,n3;
		int          q, j, k;
		int          mx, my, mz;
		int          n;
		
		s[0] = ss[0]/cTs; s[1] = ss[1]/cTs; s[2] = ss[2]/cTs;
		L[0] = LL[0]/cTs; L[1] = LL[1]/cTs; L[2] = LL[2]/cTs;
		
		// Hanning window
		for (n = 0 ; n < Tw+1 ; n++)
		{
			hanning_window[n] = 0.5 * (1 + Math.cos(2*M_PI*(n+Tw/2)/Tw));
		}
		
		for (int mic_nr = 0; mic_nr < nr_of_mics ; mic_nr++)
		{
			// [x_1 x_2 ... x_N y_1 y_2 ... y_N z_1 z_2 ... z_N]
			r[0] = rr[mic_nr + 0*nr_of_mics] / cTs;
			r[1] = rr[mic_nr + 1*nr_of_mics] / cTs;
			r[2] = rr[mic_nr + 2*nr_of_mics] / cTs;

			n1 = (int) Math.ceil(nsamples/(2*L[0]));
			n2 = (int) Math.ceil(nsamples/(2*L[1]));
			n3 = (int) Math.ceil(nsamples/(2*L[2]));

			// Generate room impulse response
			for (mx = -n1 ; mx <= n1 ; mx++)
			{
				hu[0] = 2*mx*L[0];

				for (my = -n2 ; my <= n2 ; my++)
				{
					hu[1] = 2*my*L[1];

					for (mz = -n3 ; mz <= n3 ; mz++)
					{
						hu[2] = 2*mz*L[2];

						for (q = 0 ; q <= 1 ; q++)
						{
							hu[3] = (1-2*q)*s[0] - r[0] + hu[0];
							refl[0] = Math.pow(beta[0], Math.abs(mx-q)) * Math.pow(beta[1], Math.abs(mx));

							for (j = 0 ; j <= 1 ; j++)
							{
								hu[4] = (1-2*j)*s[1] - r[1] + hu[1];
								refl[1] = Math.pow(beta[2], Math.abs(my-j)) * Math.pow(beta[3], Math.abs(my));

								for (k = 0 ; k <= 1 ; k++)
								{
									hu[5] = (1-2*k)*s[2] - r[2] + hu[2];
									refl[2] = Math.pow(beta[4],Math.abs(mz-k)) * Math.pow(beta[5], Math.abs(mz));

									dist = Math.sqrt(Math.pow(hu[3], 2) + Math.pow(hu[4], 2) + Math.pow(hu[5], 2));

									if (Math.abs(2*mx-q)+Math.abs(2*my-j)+Math.abs(2*mz-k) <= order || order == -1)
									{
	                                    fdist = (int) Math.floor(dist);
										if (fdist < nsamples)
										{
											strength = sim_microphone(hu[3], hu[4], hu[5], angle, mtype[0])
												* refl[0]*refl[1]*refl[2]/(4*M_PI*dist*cTs);

											for (n = 0 ; n < Tw+1 ; n++)
												LPI[n] = hanning_window[n] * Fc * sinc(M_PI*Fc*(n-(dist-fdist)-(Tw/2)));

											pos = fdist-(Tw/2);
											for (n = 0 ; n < Tw+1; n++)
												if (pos+n >= 0 && pos+n < nsamples)
													imp[mic_nr + nr_of_mics*(pos+n)] += strength * LPI[n];
										}
									}
								}
							}
						}
					}
				}
			}

			// 'Original' high-pass filter as proposed by Allen and Berkley.
			if (hp_filter == 1)
			{
				for (int idx = 0 ; idx < 3 ; idx++) {Y[idx] = 0;}            
				for (int idx = 0 ; idx < nsamples ; idx++)
				{
					X0 = imp[mic_nr+nr_of_mics*idx];
					Y[2] = Y[1];
					Y[1] = Y[0];
					Y[0] = B1*Y[1] + B2*Y[2] + X0;
					imp[mic_nr+nr_of_mics*idx] = Y[0] + A1*Y[1] + R1*Y[2];
				}
			}
		}
		
		return imp;
	}
	
	/**
	 * generate room impulse response
	 * @param c
	 * sound velocity in m/s
	 * @param fs
	 * sampling frequency in Hz
	 * @param rl
	 * 1 x 3 array specifying the (x,y,z) coordinates of the receiver(s) in m
	 * @param sl
	 * 1 x 3 vector specifying the (x,y,z) coordinates of the source in m
	 * @param rsize
	 * 1 x 3 vector specifying the room dimensions (x,y,z) in m
	 * @param rt60
	 * reverberation Time (T_60) in seconds
	 * @return
	 */
	public static double[] rir(double c,double fs,double[] rl,double[] sl,double[] rsize,double rt60)
	{
	double[] beta;
	
		beta=new double[1];
		beta[0]=rt60;
		return rir(c,fs,rl,sl,rsize,beta);
	}
	
	/**
	 * generate room impulse response
	 * @param c
	 * sound velocity in m/s
	 * @param fs
	 * sampling frequency in Hz
	 * @param rl
	 * 1 x 3 array specifying the (x,y,z) coordinates of the receiver(s) in m
	 * @param sl
	 * 1 x 3 vector specifying the (x,y,z) coordinates of the source in m
	 * @param rsize
	 * 1 x 3 vector specifying the room dimensions (x,y,z) in m
	 * @param rt60
	 * reverberation Time (T_60) in seconds
	 * @param len
	 * rir length in taps
	 * @return
	 */
	public static double[] rir(double c,double fs,double[] rl,double[] sl,double[] rsize,double rt60,int len)
	{
	double[] h,h2;
	
		h=rir(c,fs,rl,sl,rsize,rt60);
		h2=new double[len];
		System.arraycopy(h,0,h2,0,Math.min(h.length,h2.length));
		return h2;
	}
	
	/**
	 * generate a mixing system
	 * @param sourceloc
	 * N x 3 array for source locations
	 * @param sensorloc
	 * M x 3 array for sensor locations
	 * @param len
	 * filter length in taps
	 * @return
	 * M x N x len array for the mixing system
	 */
	public VirtualRoom generateVirtualRoom(double[][] sourceloc,double[][] sensorloc,int len)
	{
	double[][][] filters;
	
		/*
		 * generate rir filters
		 */
		filters=new double[sensorloc.length][sourceloc.length][];
		
		for(int sensorj=0;sensorj<sensorloc.length;sensorj++) 
			for(int sourcei=0;sourcei<sourceloc.length;sourcei++) 
				filters[sensorj][sourcei]=rir(c,fs,sensorloc[sensorj],sourceloc[sourcei],roomsize,rt60,len);

		return new VirtualRoom(c,fs,roomsize,rt60,sourceloc,sensorloc,filters);
	}

	public static void main(String[] args)
	{
	double c=340;
	double fs=16000;
	double[] rl={2,1.5,2};
	double[] sl={2,3.5,2};
	double[] rsize={5,4,6};
	double rt60=0.2;
	int len=2048;
	
	double[] h;
	
		h=rir(c,fs,rl,sl,rsize,rt60,len);
		Util.plotSignals(h);
		System.out.println(h.length);
	}
}
