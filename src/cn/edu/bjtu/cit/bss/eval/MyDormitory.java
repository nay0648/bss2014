package cn.edu.bjtu.cit.bss.eval;
import java.io.*;

/**
 * <h1>Description</h1>
 * My small living room in school.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Apr 3, 2012 8:23:55 AM, revision:
 */
public class MyDormitory implements Serializable
{
private static final long serialVersionUID=6643880371071326530L;

	public static void main(String[] args) throws IOException
	{
	double c=340;
	double fs=8000;
	double[] roomsize={2.8,5.3,2.6};
	double rt60=0.3;
	/*
	 * sources and sensors are on my desk
	 */
	double[][] sourceloc={	{0.15,	0.9,	0.75},
							{0.5,	0.9,	0.75}};
	double[][] sensorloc={	{0.15,	1.85,	0.75},
							{0.5,	1.85,	0.75}};
	int len=2048;
	
	RIRGenerator rirg;
	VirtualRoom room;
	
		rirg=new RIRGenerator(c,fs,roomsize,rt60);
		room=rirg.generateVirtualRoom(sourceloc,sensorloc,len);
		room.save(new File("data/VirtualRooms/2x2/MyDormitory.txt"));
		System.out.println(room);
	}	
}
