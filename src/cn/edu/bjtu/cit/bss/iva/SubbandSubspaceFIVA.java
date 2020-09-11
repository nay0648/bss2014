package cn.edu.bjtu.cit.bss.iva;
import java.awt.Point;
import java.io.*;
import java.util.*;
import java.util.logging.*;
import javax.sound.sampled.*;
import org.apache.commons.math.complex.*;
import pp.util.BLAS;
import cn.edu.bjtu.cit.bss.*;
import cn.edu.bjtu.cit.bss.FDBSS.Operation;
import cn.edu.bjtu.cit.bss.FDBSS.Parameter;
import cn.edu.bjtu.cit.bss.preprocess.*;
import cn.edu.bjtu.cit.bss.signalio.*;
import cn.edu.bjtu.cit.bss.util.*;

/**
 * <h1>Description</h1>
 * Fast fixed-point IVA with subband and subspace nonlinearity, please see the paper: 
 * Yueyue Na, Jian Yu, Independent Vector Analysis Using Subband and subspace Nonlinearity 
 * for more information.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Sep 16, 2012 8:25:40 AM, revision:
 */
public class SubbandSubspaceFIVA extends IVA
{
private static final long serialVersionUID=-5589479109440037576L;
private static final String LOGGER_NAME="cn.edu.bjtu.cit.bss";
private SubspaceNonlinearity nonlinearity=new SubspaceNonlinearity();//the subspace nonlinearity
private int maxiteration=500;//max iteration times
private double tol=1e-10;//threshold controls when the algorithm converge
/*
 * iva will performed on subbands from high frequency to low frequency
 */
private int subbandsize=100;//subband size in taps, 0 for full frequency band
private int subbandshift=subbandsize/8;//subband shift taps

	/**
	 * <h1>Description</h1>
	 * IVA intermediate status.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Sep 16, 2012 8:43:11 AM, revision:
	 */
	private class SubbandIVAStat implements Serializable
	{
	private static final long serialVersionUID=2405055568733254664L;
	private FDBSSAlgorithm fdbss;//the bss algorithm reference
	private int numsources;//source number
	private int offset;//subband offset
	/*
	 * full frequency band information
	 */
	private Complex[][][] prew;//preprocessing matrix [bin index][][] for the full frequency band
	private Complex[][][] wp;//demixing matrices [binidx][][] for the full frequency band
	private double[] cosine;//cosine for each demixing matrix for the full frequency band
	private boolean[] converged;//true if a frequency is converged
	private int binsleft=0;//number of unconverged frequency bins in the full frequency band
	/*
	 * subband information
	 */
	private Complex[][][] subbandx;//already preprocessed data [binidx][sourcei][tau] in a subband
	private Complex[][][] subbandy;//estimated source data [binidx][sourcei][tau] in a subband
	private double[][][] sabsy;//signal envelop in a subband [sourcei][binidx][tau]
	private double[][][] scovy;//covariance matrices [source index][][] for a subband
	private int sbinsleft=0;//number of frequency bins left in current subband
	
		/**
		 * @param subbandsize
		 * subband size
		 */
		public SubbandIVAStat(int subbandsize)
		{
		int numbins;
		
			//get bss algorithm reference
			fdbss=SubbandSubspaceFIVA.this.getFDBSSAlgorithm();
			numsources=fdbss.numSources();
			
			/*
			 * allocate space
			 */
			numbins=fdbss.fftSize()/2+1;
			offset=numbins;
			prew=new Complex[numbins][][];			
			cosine=new double[numbins];
			
			/*
			 * initialize demixing matrices
			 */
			wp=new Complex[numbins][][];
			for(int binidx=0;binidx<wp.length;binidx++) 
				wp[binidx]=BLAS.eyeComplex(numSources(),numSources());
			
			/*
			 * initialize not converged bins
			 */
			converged=new boolean[numbins];
			Arrays.fill(converged,false);
			binsleft=numbins;
			
			subbandx=new Complex[subbandsize][][];
			subbandy=new Complex[subbandsize][][];
			sabsy=new double[numsources][][];
			scovy=new double[numsources][subbandsize][subbandsize];
		}
		
		/**
		 * get the number of sources
		 * @return
		 */
		public int numSources()
		{
			return numsources;
		}
		
		/**
		 * get subband size
		 * @return
		 */
		public int subbandSize()
		{
			return subbandx.length;
		}
		
		/**
		 * get current subband offset
		 * @return
		 */
		public int subbandOffset()
		{
			return offset;
		}
		
		/**
		 * get estimated source signal envelop in current subband
		 * @param sourcei
		 * source index
		 * @return
		 */
		public double[][] subbandSourceEnvelop(int sourcei)
		{
			return sabsy[sourcei];
		}
		
		/**
		 * get source covariance matrix in current subband
		 * @param sourcei
		 * source index
		 * @return
		 */
		public double[][] subbandCov(int sourcei)
		{
			return scovy[sourcei];
		}
		
		/**
		 * perform source separation and update status in current subband
		 */
		public void updateSubband()
		{
		double[][] abs,cov;
		List<Point> entryl;
			
			//traverse the subband
			for(int sbinidx=0;sbinidx<subbandSize();sbinidx++) 
			{
				//already converged
				if(isConverged(sbinidx)) continue;
				
				//demix data
				subbandy[sbinidx]=BLAS.multiply(
						demixingMatrixPreprocessed(sbinidx),
						subbandx[sbinidx],
						subbandy[sbinidx]);
				
				//calculate source envelop
				for(int sourcei=0;sourcei<numSources();sourcei++) 
				{
					//allocate new space
					if(sabsy[sourcei]==null) 
					{
						abs=new double[subbandy.length][subbandy[0][0].length];
						sabsy[sourcei]=abs;
					}
					else abs=sabsy[sourcei];
				
					for(int tau=0;tau<abs[0].length;tau++) 
						abs[sbinidx][tau]=subbandy[sbinidx][sourcei][tau].abs();
				}
			}
			
			/*
			 * calculate the covariance matrices
			 */
			/*
			 * find unconverged entries
			 */
			entryl=new LinkedList<Point>();
			for(int ii=0;ii<subbandSize();ii++) 
				//symmetric
				for(int jj=ii;jj<subbandSize();jj++) 
					if((!isConverged(ii))||(!isConverged(jj))) 
						entryl.add(new Point(ii,jj));

			for(int sourcei=0;sourcei<numSources();sourcei++) 
			{
				abs=sabsy[sourcei];
				cov=scovy[sourcei];
				
				for(int tau=0;tau<abs[0].length;tau++) 
					for(Point p:entryl) 
					{
						if(tau==0) cov[p.x][p.y]=0;
						cov[p.x][p.y]+=abs[p.x][tau]*abs[p.y][tau];
					}
				
				for(Point p:entryl) 
				{
					cov[p.x][p.y]/=abs[0].length;
					cov[p.y][p.x]=cov[p.x][p.y];
				}
			}
		}
		
		/**
		 * get estimated source data for a frequency bin in current subband
		 * @param sbinidx
		 * frequency bin index in subband
		 * @return
		 */
		public Complex[][] sourceData(int sbinidx)
		{
			return subbandy[sbinidx];
		}
		
		/**
		 * get sensor data for a frequency bin in current subband
		 * @param sbinidx
		 * frequency bin index in subband
		 * @return
		 */
		public Complex[][] sensorData(int sbinidx)
		{
			return subbandx[sbinidx];
		}
		
		/**
		 * get a demixing matrix in current subband
		 * @param sbinidx
		 * frequency bin index in subband
		 * @return
		 */
		public Complex[][] demixingMatrixPreprocessed(int sbinidx)
		{
			return wp[offset+sbinidx];
		}
		
		/**
		 * Update a demixing matrix, this method will do nothing if 
		 * corresponding frequency bin is converged.
		 * @param sbinidx
		 * frequency bin index in subband
		 * @param wp1
		 * new demixing matrix
		 * @return
		 * true if the demixing matrix is converged
		 */
		public boolean updateDemixingMatrix(int sbinidx,Complex[][] wp1)
		{
		int binidx;
		
			//already converged
			if(isConverged(sbinidx)) return true;
			
			/*
			 * see if this bin is converged or not
			 */
			binidx=offset+sbinidx;//frequency bin index in the full frequency band
			
			cosine[binidx]=0;
			for(int sourcei=0;sourcei<wp1.length;sourcei++) 
				cosine[binidx]+=BLAS.innerProduct(wp[binidx][sourcei],wp1[sourcei]).abs();
			cosine[binidx]/=wp1.length;
			
			//converged
			if(Math.abs(1-cosine[binidx])<=tol) 
			{	
				converged[binidx]=true;
				binsleft--;
				sbinsleft--;
			}
			
			//copy demixing matrix
			wp[binidx]=BLAS.copy(wp1,wp[binidx]);
			
			return converged[binidx];
		}
		
		/**
		 * see if a frequency bin is converged
		 * @param binidx
		 * frequency bin index in subband
		 * @return
		 */
		public boolean isConverged(int sbinidx)
		{
			return converged[offset+sbinidx];
		}
		
		/**
		 * get the number of unconverged frequency bins in current subband
		 * @return
		 */
		public int numBinsLeftInSubband()
		{
			return sbinsleft;
		}
		
		/**
		 * get the number of unconverged frequency bins in the full frequency band
		 * @return
		 */
		public int numBinsLeft()
		{
			return binsleft;
		}
		
		/**
		 * get the average cosine for all demixing matrices, 
		 * can be used as objective function
		 * @return
		 */
		public double cosine()
		{
			return BLAS.mean(cosine);
		}
		
		/**
		 * move to next subband
		 * @return
		 * false means no subband left
		 */
		public boolean nextSubband()
		{
		Preprocessor preprocessor;//used to preprocess data
		int offset2;//new offset
		
			if(offset==0) return false;//already finished
			preprocessor=SubbandSubspaceFIVA.this.preprocessor();
			
			/*
			 * determine the new subband offset
			 */
			//the first subband
			if(offset>=fdbss.fftSize()/2+1) 
			{	
				offset=fdbss.fftSize()/2+1;
				offset2=fdbss.fftSize()/2+1-subbandSize();
			}
			else offset2=offset-subbandshift;
			if(offset2<0) offset2=0;
			
			//move data in subband to provide space for new added data	
			if(offset<fdbss.fftSize()/2+1)
			{
			int delta,destidx;
			Complex[][] swapc;
			double[] swapr;
					
				delta=offset-offset2;

				//move data
				for(int sbinidx=subbandSize()-delta-1;sbinidx>=0;sbinidx--) 
				{
					destidx=sbinidx+delta;
						
					/*
					 * move sensor data buffer
					 */
					swapc=subbandx[destidx];
					subbandx[destidx]=subbandx[sbinidx];
					subbandx[sbinidx]=swapc;
						
					/*
					 * move source data buffer
					 */
					swapc=subbandy[destidx];
					subbandy[destidx]=subbandy[sbinidx];
					subbandy[sbinidx]=swapc;
						
					//move signal envelop buffer
					for(int sourcei=0;sourcei<sabsy.length;sourcei++) 
					{
						swapr=sabsy[sourcei][destidx];
						sabsy[sourcei][destidx]=sabsy[sourcei][sbinidx];
						sabsy[sourcei][sbinidx]=swapr;
					}
					
					//move covariance matrix
					for(int sourcei=0;sourcei<scovy.length;sourcei++) 
						System.arraycopy(
								scovy[sourcei][sbinidx],0,
								scovy[sourcei][destidx],delta,
								scovy[sourcei][destidx].length-delta);
				}
			}

			//load data
			{
			int sbinidx;//index in subband
			Complex[][] bindata=null;
			
				//visit from high frequency to low frequency
				for(int binidx=offset-1;binidx>=offset2;binidx--) 
				{
					//load data
					bindata=fdbss.binData(binidx,bindata);
										
					/*
					 * preprocess
					 */
					Preprocessor.centering(bindata);
					
					//the corresponding index in subband
					sbinidx=binidx-offset2;
					subbandx[sbinidx]=preprocessor.preprocess(bindata,numSources());
					prew[binidx]=preprocessor.transferMatrix();
					
					//increase unconverged frequency bins
					if(!converged[binidx]) sbinsleft++;
				}
				
				offset=offset2;//adjust offset
			}
			
			return true;
		}
		
		/**
		 * get demixing matrix for original signals
		 * @param binidx
		 * frequency bin index
		 * @return
		 */
		public Complex[][] demixingMatrix(int binidx)
		{
			return BLAS.multiply(wp[binidx],prew[binidx],null);
		}
	}
	
	public SubbandSubspaceFIVA()
	{}
	
	/**
	 * fast fixed-point IVA on a subband
	 * @param ivastat
	 * data information
	 */
	private void iva(SubbandIVAStat ivastat)
	{
	Complex[][] xdata;//sensor data
	Complex[][] ydata;//source data
	Complex[][] wp,wp1;//the demixing matrix
	double e1;//the first expectation
	Complex[] e2;//the second expectation
	Logger logger;
		
		/*
		 * initialize
		 */
		wp1=new Complex[ivastat.numSources()][ivastat.numSources()];
		e2=new Complex[ivastat.numSources()];
		logger=Logger.getLogger(LOGGER_NAME);
			
		//perform iteration
		for(int itcount=0;itcount<maxiteration;itcount++)
		{
			//update status in current subband
			ivastat.updateSubband();
			
			//prepare for nonlinearity
			for(int sourcei=0;sourcei<ivastat.numSources();sourcei++) 
				nonlinearity.setYData(
						sourcei,
						ivastat.subbandSourceEnvelop(sourcei),
						ivastat.subbandCov(sourcei));
			
			/*
			 * update layer by layer, sbinidx is the relative bin index in 
			 * current subband
			 */
			for(int sbinidx=0;sbinidx<ivastat.subbandSize();sbinidx++) 
			{
				//this frequency bin is converged
				if(ivastat.isConverged(sbinidx)) continue;
					
				//sensor data for current frequency bin
				xdata=ivastat.sensorData(sbinidx);
				//estimated source data for current frequency bin
				ydata=ivastat.sourceData(sbinidx);
				//old demixing matrix for current frequency bin
				wp=ivastat.demixingMatrixPreprocessed(sbinidx);

				//update source by source
				for(int sourcei=0;sourcei<ivastat.numSources();sourcei++)
				{
					/*
					 * calculate expectations
					 */
					e1=0;
					Arrays.fill(e2,Complex.ZERO);
							
					for(int tau=0;tau<xdata[0].length;tau++) 
					{
					double dg,ddg;
					Complex temp;
						
						dg=nonlinearity.dg(sbinidx,sourcei,tau);
						ddg=nonlinearity.ddg(sbinidx,sourcei,tau);
							
						//G'+|y|^2*G''
						e1+=dg+BLAS.absSquare(ydata[sourcei][tau])*ddg;
							
						temp=ydata[sourcei][tau].conjugate().multiply(dg);
						for(int ii=0;ii<e2.length;ii++) 
							e2[ii]=e2[ii].add(xdata[ii][tau].multiply(temp));
					}
						
					e1/=xdata[0].length;
					e2=BLAS.scalarMultiply(1.0/xdata[0].length,e2,e2);
						
					/*
					 * update demixing vector for current sourcei
					 */
					for(int jj=0;jj<wp1[sourcei].length;jj++) 
						wp1[sourcei][jj]=
							wp[sourcei][jj].conjugate().multiply(e1).subtract(e2[jj]).conjugate();
				}

				//perform symmetric decorrelation
				symmetricDecorrelation(wp1);
					
				//update demixing matrix for current bin
				ivastat.updateDemixingMatrix(sbinidx,wp1);
			}
			
			logger.info("iteration "+itcount+", obj(->1)="+ivastat.cosine()+
					", "+ivastat.numBinsLeft()+" frequency bins left");
			//all bins are converged in current subband
			if(ivastat.numBinsLeftInSubband()<=0) return;
		}
	}
	
	/**
	 * perform symmetric decorrelation
	 * @param wp1
	 */
	private void symmetricDecorrelation(Complex[][] wp1)
	{
	HermitianEigensolver eigensolver;//used for decorrelation
	HermitianEigensolver.EigenDecomposition decomp;	
	Complex[][] wh=null,edm,ev,evh=null;//used for decorrelation
	
		eigensolver=new CommonsEigensolver();
		edm=BLAS.eyeComplex(wp1.length,wp1.length);
	
		/*
		 * W*W'
		 */
		wh=BLAS.transpose(wp1,wh);
		wh=BLAS.multiply(wp1,wh,wh);
		
		/*
		 * (W*W')^-0.5
		 */
		decomp=eigensolver.eig(wh);

		//construct D^(-0.5)
		for(int i=0;i<edm.length;i++) 
			//Hermitian matrix has only real eigenvalues
			edm[i][i]=new Complex(1.0/Math.sqrt(decomp.eigenvalue(i).getReal()),0);
			
		ev=decomp.eigenvectors();
		wh=BLAS.multiply(ev,edm,wh);
		evh=BLAS.transpose(ev,evh);
		wh=BLAS.multiply(wh,evh,wh);

		//(W*W')^-0.5*W
		wp1=BLAS.multiply(wh,wp1,wp1);
	}
	
	public DemixingModel applyICA()
	{
	SubbandIVAStat ivastat;
	
		ivastat=new SubbandIVAStat(subbandsize);
	
		//apply iva
		{
		Logger logger;

			logger=Logger.getLogger(LOGGER_NAME);

			for(;ivastat.nextSubband();)
			{
				logger.info("perform IVA on subband: ["+
						ivastat.subbandOffset()+", "+
						(ivastat.subbandOffset()+ivastat.subbandSize()-1)+"]");

				iva(ivastat);
			}
		}
		
		//return result
		{
		FDBSSAlgorithm fdbss;
		DemixingModel model;
	
			fdbss=this.getFDBSSAlgorithm();
			model=new DemixingModel(fdbss.numSources(),fdbss.numSensors(),fdbss.fftSize());
		
			for(int binidx=0;binidx<model.fftSize()/2+1;binidx++) 
				model.setDemixingMatrix(binidx,ivastat.demixingMatrix(binidx));

			return model;
		}
	}
	
	public static void main(String[] args) throws IOException, UnsupportedAudioFileException
	{
	SignalSource x0,x1,x;
	FDBSS fdbss;
	long t;
			
		x0=new WaveSource(new File("data/rsm2_mA.wav"),true);
		x1=new WaveSource(new File("data/rsm2_mB.wav"),true);
		x=new SignalMixer(x0,x1);
				
		fdbss=new FDBSS(new File("temp"));
		fdbss.setParameter(Parameter.stft_size,"512");//stft block size
		fdbss.setParameter(Parameter.stft_overlap,Integer.toString((int)(512*1/2)));//stft overlap
		fdbss.setParameter(Parameter.fft_size,"1024");//fft size, must be powers of 2
		fdbss.setParameter(Parameter.ica_algorithm,SubbandSubspaceFIVA.class.getName());
		fdbss.setParameter(Parameter.align_policy,"cn.edu.bjtu.cit.bss.align.IdentityAlign");//without permutation

		t=System.currentTimeMillis();
		fdbss.separate(x,Operation.stft);
		fdbss.separate(x,Operation.ica);
		fdbss.separate(x,Operation.align,Operation.demix);
		t=System.currentTimeMillis()-t;
		x.close();

		System.out.println("time spent: "+t);
		fdbss.visualizeSourceSpectrograms(1,fdbss.numSources());
	}
}
