package cn.edu.bjtu.cit.bss.ica;
import java.io.*;
import java.util.*;
//import org.apache.commons.math.*;
//import org.apache.commons.math.analysis.solvers.*;
import org.apache.commons.math.complex.*;
import cn.edu.bjtu.cit.bss.*;
import cn.edu.bjtu.cit.bss.preprocess.*;
import cn.edu.bjtu.cit.bss.util.*;
import pp.util.BLAS;

/**
 * <h1>Description</h1>
 * The RobustICA algorithm implementation. This algorithm dosen't need prewhitening and 
 * is robust for short data records, since it applies the general kurtosis as its contrast 
 * function. Please see the paper: Vicente Zarzoso, Robust Independent Component Analysis 
 * by Iterative Maximization of the Kurtosis Contrast With Algebraic Optimal Step Size, 2010, 
 * for algorithm details, the matlab code on the web: http://www.i3s.unice.fr/~zarzoso/robustica.html 
 * also helps a lot.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: May 24, 2011 10:50:14 AM, revision:
 */
public class RobustICA extends ICA
{
private static final long serialVersionUID=-3987589263027163199L;
private boolean prewhitening=true;//true means whitening is required
private int maxiteration=5000;//max iteration times
private double tol=1e-15;//used for termination test
private double eps=2.220446049250313e-16;//absolute value smaller than this threshold is regarded as zero
private double pcath=1e-6;//threshold used in PCA

	/**
	 * Calculate the kurtosis, the normalized forth-order marginal cumulant, 
	 * K(y)=(E{|y|^4}-2*E^2{|y|^2}-|E{y^2}|^2)/E^2{|y|^2}.
	 * @param y
	 * samples of a complex random variable
	 * @return
	 */
	public double kurtosis(Complex[] y)
	{
	double absy;
	double eabsy4=0;//E{|y|^4}
	double eabsy2=0;//E{|y|^2}
	double ey2r=0,ey2i=0;//real and imaginary part of E{y^2}
		
		/*
		 * calculate the expectations
		 */
		for(Complex sample:y)
		{
			absy=sample.abs();
			eabsy4+=absy*absy*absy*absy;
			eabsy2+=absy*absy;
			ey2r+=sample.getReal()*sample.getReal()-sample.getImaginary()*sample.getImaginary();
			ey2i+=2*sample.getReal()*sample.getImaginary();
		}
		eabsy4/=y.length;
		eabsy2/=y.length;
		ey2r/=y.length;
		ey2i/=y.length;
		
		//K(y)=(E{|y|^4}-2*E^2{|y|^2}-|E{y^2}|^2)/E^2{|y|^2}
		return (eabsy4-2*eabsy2*eabsy2-(ey2r*ey2r+ey2i*ey2i))/(eabsy2*eabsy2);
	}
	
	/**
	 * calculate the gradient vector of kurtosis for the searching vector w
	 * @param y
	 * an estimated IC, y=w^Hx
	 * @param x
	 * input signals
	 * @return
	 */
	public Complex[] dkurtosis(Complex[] y,Complex[][] x)
	{
	double absy,absy2;
	
	double eabsy2=0;//E{|y|^2}
	double eabsy4=0;//E{|y|^4}
	double ey2r=0,ey2i=0;//real and imaginary part of E{y^2}
	double[] eyxr,eyxi;//real and imaginary part of E{yx}
	double[] econjyxr,econjyxi;//real and imaginary part of E{Y^*x}
	double[] eabsy2conjyxr,eabsy2conjyxi;//real and imaginary part of E{|y|^2y^*x}
	
	Complex ey2,econjy2;//E{y^2}, E{y^*^2}
	Complex[] eyx;//E{yx}
	Complex[] econjyx;//E{y^*x}
	Complex[] eabsy2conjyx;//E{|y|^2y^*x}
	
	Complex[] g;//the gradient vector
		
		if(y.length!=x[0].length) throw new IllegalArgumentException(
				"sample size not match: "+y.length+", "+x[0].length);
	
		/*
		 * init temp vectors
		 */
		eyxr=new double[x.length];
		eyxi=new double[x.length];
		econjyxr=new double[x.length];
		econjyxi=new double[x.length];
		eabsy2conjyxr=new double[x.length];
		eabsy2conjyxi=new double[x.length];
		
		/*
		 * calculate different kinds of expectations
		 */
		for(int j=0;j<y.length;j++)
		{
			absy=y[j].abs();//|y|
			absy2=absy*absy;//|y|^2
			
			eabsy2+=absy2;//accumulate E{|y|^2}
			
			eabsy4+=absy2*absy2;//accumulate //E{|y|^4}
			
			/*
			 * accumulate E{y^2}
			 */
			ey2r+=y[j].getReal()*y[j].getReal()-y[j].getImaginary()*y[j].getImaginary();
			ey2i+=2*y[j].getReal()*y[j].getImaginary();
			
			//accumulate E{yx}
			for(int i=0;i<x.length;i++)
			{
				eyxr[i]+=y[j].getReal()*x[i][j].getReal()-y[j].getImaginary()*x[i][j].getImaginary();
				eyxi[i]+=y[j].getReal()*x[i][j].getImaginary()+y[j].getImaginary()*x[i][j].getReal();
			}
			
			//accumulate E{Y^*x}
			for(int i=0;i<x.length;i++)
			{
				econjyxr[i]+=y[j].getReal()*x[i][j].getReal()+y[j].getImaginary()*x[i][j].getImaginary();
				econjyxi[i]+=y[j].getReal()*x[i][j].getImaginary()-y[j].getImaginary()*x[i][j].getReal();
			}
			
			//accumulate E{|y|^2y^*x}
			for(int i=0;i<x.length;i++)
			{
				eabsy2conjyxr[i]+=absy2*(y[j].getReal()*x[i][j].getReal()+y[j].getImaginary()*x[i][j].getImaginary());
				eabsy2conjyxi[i]+=absy2*(y[j].getReal()*x[i][j].getImaginary()-y[j].getImaginary()*x[i][j].getReal());
			}
		}
		
		eabsy2/=y.length;//E{|y|^2}
		eabsy4/=y.length;//E{|y|^4}

		/*
		 * E{y^2}, E{y^*^2}
		 */
		ey2r/=y.length;
		ey2i/=y.length;
		ey2=new Complex(ey2r,ey2i);//E{y^2}
		econjy2=new Complex(ey2r,-ey2i);//E{y^*^2}

		/*
		 * E{yx}
		 */
		BLAS.scalarMultiply(1.0/y.length,eyxr,eyxr);
		BLAS.scalarMultiply(1.0/y.length,eyxi,eyxi);
		eyx=BLAS.buildComplexVector(eyxr,eyxi);

		/*
		 * E{y^*x}
		 */
		BLAS.scalarMultiply(1.0/y.length,econjyxr,econjyxr);
		BLAS.scalarMultiply(1.0/y.length,econjyxi,econjyxi);
		econjyx=BLAS.buildComplexVector(econjyxr,econjyxi);

		/*
		 * E{|y|^2y^*x}
		 */
		BLAS.scalarMultiply(1.0/y.length,eabsy2conjyxr,eabsy2conjyxr);
		BLAS.scalarMultiply(1.0/y.length,eabsy2conjyxi,eabsy2conjyxi);
		eabsy2conjyx=BLAS.buildComplexVector(eabsy2conjyxr,eabsy2conjyxi);
		
		/*
		 * calculate the gradient vector
		 */	
		g=new Complex[x.length];
		//E{yx}=E{yx}E{y^*^2}	
		for(int i=0;i<x.length;i++) eyx[i]=eyx[i].multiply(econjy2);
		//g=E{|y|^2y^*x}-E{yx}E{y^*^2}
		BLAS.substract(eabsy2conjyx,eyx,g);
		
		//E{|y|^4}=E{|y|^4}-|E{y^2}|^2
		eabsy4-=BLAS.absSquare(ey2);
		//E{y^*x}=((E{|y|^4}-|E{y^2}|^2)E{y^*x})/E{|y|^2}
		BLAS.scalarMultiply(eabsy4/eabsy2,econjyx,econjyx);
		//g={...}
		BLAS.substract(g,econjyx,g);
		
		//g=(4/E^2{|y|^2}){...}
		BLAS.scalarMultiply(4/(eabsy2*eabsy2),g,g);
		return g;
	}
	
	/**
	 * calculate the value of the polynomial: a[0]+a[1]*x+a[2]*x^2+...+a[n-1]*x^(n-1)
	 * @param a
	 * polynomial coefficients, lower index for lower order
	 * @param x
	 * the independent variable value
	 * @return
	 */
	public double polyval(double[] a,double x)
	{
	double y;
	double orderx=1;
	
		y=a[0];
		for(int i=1;i<a.length;i++)
		{
			orderx*=x;
			y+=a[i]*orderx;
		}
		return y;
	}

	/**
	 * calculate the optimal step size
	 * @param y
	 * an estimated IC
	 * @param x
	 * input signals
	 * @param dkurt
	 * the gradient vector of kurtosis relative to w
	 * @return
	 */
	private double optimalStepSize(Complex[] y,Complex[][] x,Complex[] dkurt)
	{
	Complex[] a,b,c;
	double[] d;
	double h0,h1,h2,h3,h4,i0,i1,i2;
	List<Complex> mu;//the roots

		/*
		 * calculate a, b, c, d
		 */
		{
		Complex g;
			
			a=new Complex[x[0].length];
			b=new Complex[x[0].length];
			c=new Complex[x[0].length];
			d=new double[x[0].length];
			
			//traverse all samples
			for(int j=0;j<x[0].length;j++) 
			{	
				/*
				 * calculate g=gradient^Hx
				 */
				g=new Complex(0,0);
				for(int i=0;i<x.length;i++) g=g.add(dkurt[i].conjugate().multiply(x[i][j]));
		
				a[j]=y[j].multiply(y[j]);//a=y^2
				b[j]=g.multiply(g);//b=g^2
				c[j]=y[j].multiply(g);//c=yg
				d[j]=y[j].multiply(g.conjugate()).getReal();//d=real(yg^*)
			}
		}
		
		//calculate h0, h1, h2, h3, h4, i0, i1, i2
		{
		Complex ea,eb,ec;//E{a}, E{b}, E{c}
			
			ea=Expectation.e(a);
			eb=Expectation.e(b);
			ec=Expectation.e(c);
			
			//h0
			h0=Expectation.eAbsSquare(a)-BLAS.absSquare(ea);

			//h1
			{
			double eabsad=0;
					
				/*
				 * E{|a|d}
				 */
				for(int j=0;j<x[0].length;j++) eabsad+=a[j].abs()*d[j];
				eabsad/=x[0].length;

				h1=4*eabsad-4*ea.multiply(ec.conjugate()).getReal();
			}
				
			//h2
			{
			double eabsaabsb=0;
					
				/*
				 * E{|a||b|}
				 */
				for(int j=0;j<x[0].length;j++) eabsaabsb+=a[j].abs()*b[j].abs();
				eabsaabsb/=x[0].length;
					
				h2=4*Expectation.eSquare(d)+2*eabsaabsb
					-4*BLAS.absSquare(ec)
					-2*ea.multiply(eb.conjugate()).getReal();
			}
				
			//h3
			{
			double eabsbd=0;
				
				/*
				 * E{|b|d}
				 */
				for(int j=0;j<x[0].length;j++) eabsbd+=b[j].abs()*d[j];
				eabsbd/=x[0].length;
					
				h3=4*eabsbd-4*eb.multiply(ec.conjugate()).getReal();
			}
				
			//h4
			h4=Expectation.eAbsSquare(b)-BLAS.absSquare(eb);
				
			i0=Expectation.eAbs(a);
			i1=2*Expectation.e(d);
			i2=Expectation.eAbs(b);
		}

		/*
		 * calculate coefficients, the quartic equation is: 
		 * pa4*mu^4+pa3*mu^3+pa2*mu^2+pa1*mu+pa0=0
		 */
		mu=roots4(
				-h3*i2+2*h4*i1,//a4
				-2*h2*i2+h3*i1+4*h4*i0,//a3
				-3*h1*i2+3*h3*i0,//a2
				-4*h0*i2-h1*i1+2*h2*i0,//a1
				-2*h0*i1+h1*i0);//a0
		
		/*
		 * also can be solved by apache commons solver
		 */
//		LaguerreSolver solver=new LaguerreSolver();
//		double[] co=new double[5];
//		co[0]=-2*h0*i1+h1*i0;//a0
//		co[1]=-4*h0*i2-h1*i1+2*h2*i0;//a1
//		co[2]=-3*h1*i2+3*h3*i0;//a2
//		co[3]=-2*h2*i2+h3*i1+4*h4*i0;//a3
//		co[4]=-h3*i2+2*h4*i1;//a4
		
//		try
//		{
//			mu=solver.solveAll(co,0);
//		}
//		catch(ConvergenceException e)
//		{
//			throw new RuntimeException("failed to solve 4 order polynomial",e);
//		}
//		catch(FunctionEvaluationException e)
//		{
//			throw new RuntimeException("failed to solve 4 order polynomial",e);
//		}

		//select the optimal step size
		{
		double[] cp,cq;
		double valp,valq;
		double maxabskurt=0,optmu=0,abskurt;
		double rmu;
		
			cp=new double[5];
			cp[0]=h0;
			cp[1]=h1;
			cp[2]=h2;
			cp[3]=h3;
			cp[4]=h4;
			
			cq=new double[3];
			cq[0]=i0;
			cq[1]=i1;
			cq[2]=i2;
			
			for(Complex root:mu)
			{
				rmu=root.getReal();//only use the real part
				/*
				 * calculate the |kurtosis|
				 */
				valp=polyval(cp,rmu);
				valq=polyval(cq,rmu);
				if(Math.abs(valq)<eps) continue;//denominator
				abskurt=Math.abs(valp/(valq*valq)-2);
				
				//find a better one
				if(abskurt>maxabskurt)
				{
					maxabskurt=abskurt;
					optmu=rmu;
				}
			}
			return optmu;	
		}
	}

	/**
	 * calculate the roots of a*x^2+b*x+c=0
	 * @param a, b, c
	 * coeffieients
	 * @return
	 */
	public List<Complex> roots2(double a,double b,double c)
	{
	List<Complex> roots;
	double delta;
		
		roots=new ArrayList<Complex>(2);
		
		//reduce to b*x+c=0
		if(Math.abs(a)<eps&&Math.abs(b)>=eps) roots.add(new Complex(-c/b,0));
		else
		{
			delta=b*b-4*a*c;
			
			//two real roots
			if(delta>0) 
			{
				delta=Math.sqrt(delta);
				roots.add(new Complex((-b+delta)/(2*a),0));
				roots.add(new Complex((-b-delta)/(2*a),0));
			}
			//two complex roots
			else if(delta<0)
			{
			double real,imag;
			
				real=-b/(2*a);
				imag=Math.sqrt(-delta)/(2*a);
				roots.add(new Complex(real,imag));
				roots.add(new Complex(real,-imag));
			}
			//two identical real roots
			else roots.add(new Complex(-b/(2*a),0));
		}
		return roots;
	}
	
	/**
	 * calculate the cubic root of a complex number
	 * @param real, imag
	 * real part and imaginary part
	 * @return
	 */
	public Complex cbrt(double real,double imag)
	{
	double r,theta;
	
		if(Math.abs(imag)<eps) return new Complex(Math.cbrt(real),0);
		
		r=Math.sqrt(real*real+imag*imag);
		theta=Math.acos(real/r);
		
		r=Math.cbrt(r);
		theta/=3;
		
		return new Complex(r*Math.cos(theta),r*Math.sin(theta));
	}

	/**
	 * calculate the roots of: x^3+p*x+q=0
	 * @param p, q
	 * coefficients
	 * @return
	 */
	public List<Complex> roots3(double p,double q)
	{
	List<Complex> roots;
	double a,b;
	Complex part1,part2;
	Complex w1,w2;
	
		p/=3.0;
		q/=2.0;
	
		a=-q;
		b=q*q+p*p*p;
		
		if(b>=0)
		{
			b=Math.sqrt(b);
			part1=cbrt(a+b,0);
			part2=cbrt(a-b,0);
		}
		else
		{
			b=Math.sqrt(-b);
			part1=cbrt(a,b);
			part2=cbrt(a,-b);
		}

		w1=new Complex(-0.5,Math.sqrt(3)/2.0);
		w2=new Complex(-0.5,-Math.sqrt(3)/2.0);
		
		/*
		 * return results
		 */
		roots=new ArrayList<Complex>(3);
		roots.add(part1.add(part2));
		roots.add(w1.multiply(part1).add(w2.multiply(part2)));
		roots.add(w2.multiply(part1).add(w1.multiply(part2)));
		return roots;
	}
	
	/**
	 * calculate the roots of a*x^3+b*x^2+c*x+d=0
	 * @param a, b, c, d
	 * coefficients
	 * @return
	 */
	public List<Complex> roots3(double a,double b,double c,double d)
	{
	List<Complex> temproots,roots;
	double p,q;
	Complex temp;
	
		if(Math.abs(a)<eps) return roots2(b,c,d);
		
		//solve x^3+p*x+q=0
		p=c/a-b*b/(3*a*a);
		q=2*b*b*b/(27*a*a*a)-b*c/(3*a*a)+d/a;
		temproots=roots3(p,q);
		
		temp=new Complex(b/(3*a),0);
		roots=new ArrayList<Complex>(temproots.size());
		for(Complex x:temproots) roots.add(x.subtract(temp));
		return roots;
	}

	/**
	 * calculate the real root of: x^3+p*x+q=0
	 * @param p, q
	 * coefficients
	 * @return
	 */
	public double realRoot(double p,double q)
	{
	double a,b2,b,theta,r;
		
		p/=3.0;
		q/=2.0;
		
		a=-q;
		b2=q*q+p*p*p;
		
		if(b2>=0)
		{
			b=Math.sqrt(b2);
			return Math.cbrt(a+b)+Math.cbrt(a-b);
		}
		//square root can not performed
		else
		{
			b=Math.sqrt(-b2);//the imaginary part
			r=Math.sqrt(a*a+b*b);//magnitude
			theta=Math.acos(a/r);//phase angle
			return 2.0*Math.cbrt(r)*Math.cos(theta/3.0);
		}
	}
	
	/**
	 * calculate the real root of: a*x^3+b*x^2+c*x+d=0
	 * @param a, b, c, d
	 * coefficients
	 * @return
	 */
	public double realRoot(double a,double b,double c,double d)
	{
	double p,q;
	
		if(Math.abs(a)<eps) throw new ArithmeticException(
				"coefficient of the cubic term can not be zero: "+a);
		p=c/a-b*b/(3*a*a);
		q=2*b*b*b/(27*a*a*a)-b*c/(3*a*a)+d/a;
		return realRoot(p,q)-b/(3*a);
	}
	
	/**
	 * solve a*x^4+b*x^3+c*x^2+d*x+e=0
	 * @param a, b, c, d, e
	 * coefficients
	 * @return
	 */
	public List<Complex> roots4(double a,double b,double c,double d,double e)
	{
	double y,sqrtterm;
	Complex tt,bb1,cc1,bb2,cc2;
	Complex delta1,delta2;
	List<Complex> roots;

		if(Math.abs(a)<eps) return roots3(b,c,d,e);

		/*
		 * convert to: x^4+b*x^3+c*x^2+d*x+e=0
		 */
		b/=a;
		c/=a;
		d/=a;
		e/=a;
		
		//solve: 8*y^3-4*c*y^2+(2*b*d-8*e)*y+e*(4*c-b^2)-d^2=0
		y=realRoot(
				8,
				-4*c,
				2*b*d-8*e,
				e*(4*c-b*b)-d*d);

		sqrtterm=8*y+b*b-4*c;

		roots=new ArrayList<Complex>(4);
		if(Math.abs(sqrtterm)<eps) return roots;//divide by zero
		else if(sqrtterm>0) tt=new Complex(Math.sqrt(sqrtterm),0);
		else tt=new Complex(0,Math.sqrt(-sqrtterm));
		
		bb1=(new Complex(b,0)).add(tt).multiply(0.5);
		cc1=(new Complex(y,0)).add((new Complex(b*y-d,0)).divide(tt));
		
		bb2=(new Complex(b,0)).subtract(tt).multiply(0.5);
		cc2=(new Complex(y,0)).subtract((new Complex(b*y-d,0)).divide(tt));
		
		delta1=bb1.multiply(bb1).subtract(cc1.multiply(4)).sqrt();
		delta2=bb2.multiply(bb2).subtract(cc2.multiply(4)).sqrt();
		
		roots.add(bb1.multiply(-1).add(delta1).multiply(0.5));
		roots.add(bb1.multiply(-1).subtract(delta1).multiply(0.5));
		roots.add(bb2.multiply(-1).add(delta2).multiply(0.5));
		roots.add(bb2.multiply(-1).subtract(delta2).multiply(0.5));
		return roots;
	}

	/**
	 * demix one component from signals: y=w^HX
	 * @param w
	 * a demixing vector, used as column vector
	 * @param x
	 * input signals
	 * @param y
	 * space for result, null to allocate new space
	 * @return
	 */
	private Complex[] demix(Complex[] w,Complex[][] x,Complex[] y)
	{
		if(y==null) y=new Complex[x[0].length];
		else if(y.length!=x[0].length) throw new IllegalArgumentException(
				"destination array size not match: "+y.length+", required: "+x[0].length);
		
		/*
		 * calculate w^Hx
		 */
		Arrays.fill(y,new Complex(0,0));
		for(int j=0;j<y.length;j++) 
			for(int i=0;i<w.length;i++) y[j]=y[j].add(w[i].conjugate().multiply(x[i][j]));
		
		return y;
	}
	
	/**
	 * perform deflation by subtracting the estimated source contribution from 
	 * the observations
	 * @param x
	 * input signals, contribution of the source will be substracted from it
	 * @param y
	 * an estimated source
	 * @return
	 * corresponding mixing vector, used as column vector
	 */
	private Complex[] deflationRegression(Complex[][] x,Complex[] y)
	{
	double y2=0;
	Complex[] h;
		
		h=new Complex[x.length];
		Arrays.fill(h,new Complex(0,0));
		
		//extracted source power times sample size
		y2=BLAS.innerProduct(y,y).getReal();
		
		//don't perform subtraction if estimated component is null
		if(Math.abs(y2)<eps) return h;

		/*
		 * source direction estimated via least squares
		 */		
		for(int i=0;i<x.length;i++)
			for(int j=0;j<x[i].length;j++) h[i]=h[i].add(x[i][j].multiply(y[j].conjugate()));
		BLAS.scalarMultiply(1.0/y2,h,h);

		//substract its contribution
		for(int i=0;i<x.length;i++)
			for(int j=0;j<x[i].length;j++) x[i][j]=x[i][j].subtract(h[i].multiply(y[j]));
		
		return h;
	}
	
	public ICAResult icaPreprocessed(Complex[][] sigsp,Complex[][] seeds)
	{
	List<Complex[]> demixpl;//list of demixing vectors for perprocessed signals, column vectors are stored
	List<Complex[]> mixl;//list for mixing vectors, column vectors are stored
	Complex[] w,w1=null;//the estimated direction
	Complex[][] ests;//estimated signals
	
		demixpl=new ArrayList<Complex[]>(seeds.length);
		mixl=new ArrayList<Complex[]>(seeds.length);
		ests=new Complex[seeds.length][];
					
		//calculate demixing vectors one by one
		for(int found=0;found<seeds.length;found++)
		{
		Complex[] g;//the gradient vector;
		double normg;//the norm of the gradient vector
		double optmu=0;//the optimal step size

			/*
			 * get an initial vector
			 */
			w=new Complex[sigsp.length];
			for(int j=0;j<w.length;j++) w[j]=seeds[found][j];
			BLAS.normalize(w);
			
			//search for the direction iteratively
			for(int numit=1;;numit++)
			{
				if(numit>maxiteration) throw new AlgorithmNotConvergeException(
						"max iteration times reached: "+maxiteration);

				ests[found]=demix(w,sigsp,ests[found]);//estimated IC at current step

				/*
				 * calculate the gradient
				 */
				g=dkurtosis(ests[found],sigsp);
				normg=Math.sqrt(BLAS.innerProduct(g,g).getReal());//the gradient norm
				BLAS.scalarMultiply(1.0/normg,g,g);//normalize to ||g||=1

				//calculate the optimal step size
				optmu=optimalStepSize(ests[found],sigsp,g);
								
				/*
				 * update the searching direction
				 * w1=w+mu*g
				 */
				w1=BLAS.scalarMultiply(optmu,g,w1);
				w1=BLAS.add(w,w1,w1);
				BLAS.normalize(w1);

				/*
				 * to see if the direction converges
				 */
				if(Math.abs(1-BLAS.innerProduct(w1,w).abs())<=tol)
				{
					demixpl.add(w);
					break;
				}
				
				//not converge
				for(int i=0;i<w.length;i++) w[i]=w1[i];//copy the demixing vector
			}

			//substract the contribution of the estimated source from input signals
			mixl.add(deflationRegression(sigsp,ests[found]));
		}
		
		//return results
		{
		ICAResult icares;//results
		Complex[][] mix;//the mixing matrix
		Complex[][] demixp;//demixing matrix for perprocessed signals
		Complex[] tempv=null;
			
			icares=new ICAResult();
			icares.setEstimatedSignals(ests);//the estimated signals
				
			/*
			 * adjust demixing matrix to make it suitable for original input
			 */
			demixp=new Complex[demixpl.size()][];
			demixp[0]=demixpl.get(0);//the first demixing vector is the same
			
			for(int i=1;i<demixp.length;i++)
			{
				demixp[i]=BLAS.copy(demixpl.get(i),null);
					
				for(int ii=0;ii<i;ii++) 
				{
					tempv=BLAS.scalarMultiply(
							BLAS.innerProduct(mixl.get(ii),demixpl.get(i)),
							demixp[ii],tempv);
					BLAS.substract(demixp[i],tempv,demixp[i]);
				}
			}
			//row vectors will be used, so complex conjugate is required
			icares.setDemixingMatrixPreprocessed(BLAS.conjugate(demixp,demixp));
				
			/*
			 * The mixing matrix, this should be conjugated after the demixing matrix 
			 * is returned because mixing vectors are used in the demixing matrix 
			 * adjustment.
			 */
			mix=new Complex[mixl.size()][];
			for(int i=0;i<mix.length;i++) mix[i]=mixl.get(i);
			BLAS.conjugate(mix,mix);
			icares.setMixingMatrix(mix);
			
			return icares;
		}
	}

	public ICAResult ica(Complex[][] sigs)
	{
	Preprocessor preprocessor;
	Complex[] means;//signal means
	Complex[][] whitening=null;//whitening matrix or dimensionality reduction matrix
	Complex[][] sigsp;//preprocessed signals
	Complex[][] seeds;
	ICAResult icares;
	
		/*
		 * preprocessing
		 */
		if(prewhitening) 
		{	
			preprocessor=new Whitening();
			sigsp=((Whitening)preprocessor).preprocess(sigs,pcath);
		}
		else 
		{	
			preprocessor=new PCA();
			sigsp=((PCA)preprocessor).preprocess(sigs,pcath);
		}

		means=preprocessor.signalMeans();
		whitening=preprocessor.transferMatrix();

		/*
		 * generate initial seeds
		 */
		seeds=new Complex[sigsp.length][sigsp.length];
		for(int i=0;i<seeds.length;i++)
			for(int j=0;j<seeds[i].length;j++)
				if(i==j) seeds[i][j]=new Complex(1,1);
				else seeds[i][j]=new Complex(0,0);
//		seeds=BLAS.randComplexMatrix(sigsp.length,sigsp.length);
		BLAS.orthogonalize(seeds);
		
		//apply ica
		icares=icaPreprocessed(sigsp,seeds);
		
		/*
		 * set results
		 */
		icares.setSignalMeans(means);
		icares.setWhiteningMatrix(whitening);
		
		//demixing matrix for original input
		if(icares.getWhiteningMatrix()!=null) 
			icares.setDemixingMatrix(BLAS.multiply(
					icares.getDemixingMatrixPreprocessed(),
					icares.getWhiteningMatrix(),
					null));
		else icares.setDemixingMatrix(BLAS.copy(icares.getDemixingMatrixPreprocessed(),null));

		return icares;
	}
	
	public static void main(String[] args) throws IOException 
	{
	double[][] rs,rx,mix,ry;
	Complex[][]	cx,cy;
	RobustICA app;
	ICAResult icares;
		
		/*
		 * generate input signals
		 */
		rs=Util.loadSignals(new File("data/demosig.txt"),Util.Dimension.COLUMN);
		mix=BLAS.randMatrix(6,4);
		rx=BLAS.multiply(mix,rs,null);
		
		cx=new Complex[rx.length][];
		for(int i=0;i<cx.length;i++) cx[i]=SpectralAnalyzer.fft(rx[i]);
			
		/*
		 * apply ica
		 */	
		app=new RobustICA();
		icares=app.ica(cx);
//		cy=icares.getEstimatedSignals();
		cy=BLAS.multiply(icares.getDemixingMatrix(),cx,null);
	
		/*
		 * transform back to real signal
		 */
		ry=new double[cy.length][];
		for(int i=0;i<ry.length;i++) ry[i]=SpectralAnalyzer.ifftReal(cy[i]);
		
		Util.plotSignals(ry);		
	}
}
