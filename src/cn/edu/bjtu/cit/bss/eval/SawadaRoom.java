package cn.edu.bjtu.cit.bss.eval;
import java.io.*;

/**
 * <h1>Description</h1>
 * The virtual room: http://www.kecl.ntt.co.jp/icl/signal/sawada/demo/bss2to4/index.html
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Mar 29, 2012 4:55:26 PM, revision:
 */
public class SawadaRoom implements Serializable
{
private static final long serialVersionUID=-6749433290434860199L;

	public static void main(String[] args) throws IOException
	{
	double c=340;
	double fs=8000;
	double[] roomsize={3.55,4.45,2.5};
	double rt60=0.13;
	/*
	 * 2x2
	 */
//	double[][] sourceloc={	{0.37,	3.09,	1.35},
//							{3,		3.09,	1.35}};
//	double[][] sensorloc={	{1.36,	2.34,	1.35},
//							{2.1,	2.34,	1.35}};
	/*
	 * 3x3
	 */
	double[][] sourceloc={	{0.37,	3.09,	1.35},
							{1.22,	3.65,	1.35},
//							{2.25,	3.65,	1.35},
							{3,		3.09,	1.35}};
	double[][] sensorloc={	{1.36,	2.34,	1.35},
							{1.63,	2.34,	1.35},
//							{1.89,	2.34,	1.35},
							{2.1,	2.34,	1.35}};
	/*
	 * 4x4
	 */
//	double[][] sourceloc={	{0.37,	3.09,	1.35},
//							{1.22,	3.65,	1.35},
//							{2.25,	3.65,	1.35},
//							{3,		3.09,	1.35}};
//	double[][] sensorloc={	{1.36,	2.34,	1.35},
//							{1.63,	2.34,	1.35},
//							{1.89,	2.34,	1.35},
//							{2.1,	2.34,	1.35}};
	int len=1024;
	
	RIRGenerator rirg;
	VirtualRoom room;
	
		rirg=new RIRGenerator(c,fs,roomsize,rt60);
		room=rirg.generateVirtualRoom(sourceloc,sensorloc,len);
		room.save(new File("data/VirtualRooms/3x3/SawadaRoom3x3.txt"));
		System.out.println(room);
	}
}
