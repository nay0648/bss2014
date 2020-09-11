package cn.edu.bjtu.cit.bss.recorder;
import java.util.List;
import java.util.LinkedList;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import cn.edu.bjtu.cit.bss.FDBSS.Operation;

/**
 * <h1>Description</h1>
 * Used to set bss operations.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Apr 13, 2012 6:07:13 PM, revision:
 */
public class BSSOperationPanel extends JPanel
{
private static final long serialVersionUID=6567615169835913483L;
private JCheckBox bopstft;//perform stft
private JCheckBox bopica;//perform ica
private JCheckBox bopalign;//perform align
private JCheckBox bopdemix;//perform demix

	public BSSOperationPanel()
	{	
		this.setBorder(new TitledBorder(""));
		this.setLayout(new FlowLayout(FlowLayout.LEFT,5,5));
	
		this.add(new JLabel("Operations: "));
		
		bopstft=new JCheckBox("STFT");
		bopstft.setSelected(true);
		bopstft.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				if(bopstft.isSelected())
				{
					bopica.setSelected(true);
					bopica.setEnabled(false);
					
					bopalign.setSelected(true);
					bopalign.setEnabled(false);
					
					bopdemix.setSelected(true);
					bopdemix.setEnabled(false);
				}
				else bopica.setEnabled(true);
			}
		});
		this.add(bopstft);
		
		bopica=new JCheckBox("ICA");
		bopica.setSelected(true);
		bopica.setEnabled(false);
		bopica.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				if(bopica.isSelected())
				{
					bopalign.setSelected(true);
					bopalign.setEnabled(false);
					
					bopdemix.setSelected(true);
					bopdemix.setEnabled(false);
				}
				else bopalign.setEnabled(true);
			}
		});
		this.add(bopica);
		
		bopalign=new JCheckBox("Align");
		bopalign.setSelected(true);
		bopalign.setEnabled(false);
		bopalign.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				if(bopalign.isSelected())
				{
					bopdemix.setSelected(true);
					bopdemix.setEnabled(false);
				}
				else bopdemix.setEnabled(true);
			}
		});
		this.add(bopalign);
		
		bopdemix=new JCheckBox("Demix");
		bopdemix.setSelected(true);
		bopdemix.setEnabled(false);
		this.add(bopdemix);
	}
	
	/**
	 * get selected operations
	 * @return
	 */
	public Operation[] operations()
	{
	List<Operation> opl;
	Operation[] ops;
	
		opl=new LinkedList<Operation>();
		
		if(bopstft.isSelected()) opl.add(Operation.stft);
		if(bopica.isSelected()) opl.add(Operation.ica);
		if(bopalign.isSelected()) opl.add(Operation.align);
		if(bopdemix.isSelected()) opl.add(Operation.demix);
		
		ops=new Operation[opl.size()];
		ops=opl.toArray(ops);
		return ops;
	}
}
