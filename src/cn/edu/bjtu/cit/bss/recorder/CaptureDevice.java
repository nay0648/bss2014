package cn.edu.bjtu.cit.bss.recorder;
import javax.sound.sampled.*;

/**
 * <h1>Description</h1>
 * Represents a sound capture device.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Apr 12, 2012 10:18:44 AM, revision:
 */
public class CaptureDevice
{
private Mixer mixer;
private TargetDataLine line;

	/**
	 * @param mixer
	 * the mixer
	 * @param line
	 * corresponding line
	 */
	public CaptureDevice(Mixer mixer,TargetDataLine line)
	{
		this.mixer=mixer;
		this.line=line;
	}

	/**
	 * get mixer
	 * @return
	 */
	public Mixer mixer()
	{
		return mixer;
	}

	/**
	 * get corresponding line
	 * @return
	 */
	public TargetDataLine line()
	{
		return line;
	}
	
	/**
	 * get device description
	 * @return
	 */
	public String description()
	{
		return mixer.getMixerInfo().getName()+"\n"+mixer.getMixerInfo().getDescription()+"\n"+line.getLineInfo();
	}

	public String toString()
	{
	StringBuilder s;

		s=new StringBuilder();
		s.append("mixer info:\n");
		s.append("name: "+mixer.getMixerInfo().getName()+"\n");
		s.append("descirption: "+mixer.getMixerInfo().getDescription()+"\n");
		s.append("vendor: "+mixer.getMixerInfo().getVendor()+"\n");
		s.append("version: "+mixer.getMixerInfo().getVersion()+"\n");
	
		s.append("line info:\n");
		s.append(line.getLineInfo());
	
		return s.toString();
	}
	
	public boolean equals(Object o)
	{
	CaptureDevice device2;
	
		if(o==null) return false;
		else if(this==o) return true;
		
		if(!(o instanceof CaptureDevice)) return false;
		device2=(CaptureDevice)o;
		
		/*
		 * !!!
		 * line always not equal
		 */
//		return mixer.equals(device2.mixer)&&line.equals(device2.line);
		return mixer.equals(device2.mixer);
	}
	
	public int hashCode()
	{
		return mixer.hashCode();
//		return mixer.hashCode()+line.hashCode()*3;
	}
}
