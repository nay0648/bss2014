package cn.edu.bjtu.cit.bss.eval;
import java.io.*;
import cn.edu.bjtu.cit.bss.*;

/**
 * <h1>Description</h1>
 * Virtual environment for our lab.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Apr 5, 2012 4:45:57 PM, revision:
 */
public class OurLab implements Serializable
{
private static final long serialVersionUID=-8678933766283420354L;

	public static void main(String[] args) throws IOException
	{
	double c=340;
	double fs=8000;
	double[] roomsize={10.6,6.5,2.7};
	double rt60=0.2;
	double[][] sourceloc={	{0.11,	2.95,	1.1},
							{0.11,	3.95,	1.1}};
	double[][] sensorloc={	{1.3,	2.95,	1.1},
							{1.3,	3.95,	1.1}};
	int len=1024;

	RIRGenerator rirg;
	VirtualRoom room;
	MixingModel mixm;
	
		rirg=new RIRGenerator(c,fs,roomsize,rt60);
		room=rirg.generateVirtualRoom(sourceloc,sensorloc,len);
		room.save(new File("data/VirtualRooms/2x2/OurLab.txt"));
		
		System.out.println(room);
		mixm=new MixingModel(room.tdFilters(),2048);
		mixm.visualize();
	}
}
