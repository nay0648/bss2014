package cn.edu.bjtu.cit.bss.recorder;

/**
 * <h1>Description</h1>
 * Recorder states.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Mar 13, 2012 6:39:24 PM, revision:
 */
public enum RecorderState
{
	/**
	 * Recorder is stoped.
	 */
	STOP,
	/**
	 * Is capturing the sound.
	 */
	CAPTURE,
	/**
	 * Is playing the sound.
	 */
	PLAYBACK,
	/**
	 * Is performing the BSS.
	 */
	BSS
}
