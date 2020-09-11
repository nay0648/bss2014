package cn.edu.bjtu.cit.bss.recorder;
import java.util.EventObject;
import java.util.List;
import java.util.LinkedList;
import java.awt.*;
import java.awt.event.*;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

/**
 * <h1>Description</h1>
 * Used to assign capture devices to channels.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Apr 12, 2012 10:51:49 AM, revision:
 */
public class DeviceBindingDialog extends JDialog
{
private static final long serialVersionUID=-7725555024636546027L;
private BSSRecorder recorder;//recorder reference
private DeviceTableModel tmodel;//underlying table model

	/**
	 * <h1>Description</h1>
	 * Device-Channel pair.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Apr 12, 2012 6:24:21 PM, revision:
	 */
	private class DeviceAssignment
	{
	CaptureDevice device;//capture device
	int chidx;//corresponding channel index, -1 for not binded
	
		public DeviceAssignment(CaptureDevice device)
		{
			this.device=device;
			this.chidx=-1;
		}
	}
	
	/**
	 * <h1>Description</h1>
	 * Underlying device-channel table model.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Apr 12, 2012 11:07:42 AM, revision:
	 */
	private class DeviceTableModel implements TableModel
	{
	private DeviceAssignment[] assignment;//the binding table
	private List<TableModelListener> listener=new LinkedList<TableModelListener>();
	
		/**
		 * @throws LineUnavailableException
		 */
		public DeviceTableModel() throws LineUnavailableException
		{
		List<CaptureDevice> devicel;//has the length of number of devices
		CaptureDevice[] bdev;//has the length of number of channels
		int didx;
		
			/*
			 * initialize all supported unbinded devices
			 */
			devicel=recorder.listCaptureDevices();
			assignment=new DeviceAssignment[devicel.size()];
			for(int devidx=0;devidx<assignment.length;devidx++) 
				assignment[devidx]=new DeviceAssignment(devicel.get(devidx));

			bdev=recorder.bindedCaptureDevices();//binded devices for each channel in last set
			for(int chidx=0;chidx<bdev.length;chidx++) 
			{
				if(bdev[chidx]==null) continue;//this channel has no binded device
			
				didx=deviceIndex(bdev[chidx]);
				if(didx>=0&&didx<assignment.length) assignment[didx].chidx=chidx;
			}
		}
		
		/**
		 * find device index in device list
		 * @param device
		 * a device
		 * @return
		 * -1 for not found
		 */
		public int deviceIndex(CaptureDevice device)
		{
		int devidx;
		
			if(device==null) return -1;
			
			//find device index
			for(devidx=0;devidx<assignment.length;devidx++) 
				if(assignment[devidx].device.equals(device)) break;
			
			if(devidx<0||devidx>=assignment.length) return -1;
			else return devidx;
		}

		public void addTableModelListener(TableModelListener arg0)
		{
			listener.add(arg0);
		}
		
		public void removeTableModelListener(TableModelListener arg0)
		{
			listener.remove(arg0);
		}

		public Class<?> getColumnClass(int arg0)
		{
			return String.class;
		}

		public int getColumnCount()
		{
			return 2;
		}

		public String getColumnName(int arg0)
		{
			if(arg0==0) return "Capture Device";
			else if(arg0==1) return "Channel";
			else return "";
		}

		public int getRowCount()
		{
			return assignment.length;
		}

		public Object getValueAt(int arg0,int arg1)
		{
		Mixer.Info minfo;
		
			if(arg1==0) 
			{
				minfo=assignment[arg0].device.mixer().getMixerInfo();
				return minfo.getName()+"\n"+minfo.getDescription();
			}
			else 
			{
				if(assignment[arg0].chidx<0) return "";
				else return Integer.toString(assignment[arg0].chidx);
			}
		}

		public boolean isCellEditable(int arg0,int arg1)
		{
			if(arg1==0) return false;else return true;
		}

		/**
		 * bind a device to a channel
		 * @param deviceidx
		 * device index
		 * @param chidx
		 * channel index, -1 for not bind
		 */
		private void bindChannel(int deviceidx,int chidx)
		{
			if(deviceidx<0||deviceidx>=assignment.length) throw new IndexOutOfBoundsException(
					"capture device index out of bounds: "+deviceidx+", "+assignment.length);
			
			if(chidx>=recorder.numSensors()) throw new IndexOutOfBoundsException(
					"channel index out of bounds: "+chidx+", "+recorder.numSensors());
			
			if(chidx<0) 
			{
				assignment[deviceidx].chidx=-1;
				fileTableChanged(deviceidx);
			}
			else
			{
				//cancel duplicate bind
				for(int i=0;i<assignment.length;i++) 
					if(i!=deviceidx&&assignment[i].chidx==chidx) 
					{	
						assignment[i].chidx=-1;
						fileTableChanged(i);
					}
				
				assignment[deviceidx].chidx=chidx;
				fileTableChanged(deviceidx);
			}
		}
		
		/**
		 * new channel binded
		 * @param row
		 * device index
		 */
		private void fileTableChanged(int row)
		{
		TableModelEvent event;
		
			event=new TableModelEvent(this,row,row,1,TableModelEvent.UPDATE);
			for(TableModelListener l:listener) l.tableChanged(event);
		}

		public void setValueAt(Object arg0,int arg1,int arg2)
		{
		String sch;
		int ch;
		
			if(arg2==1)
			{
				if(arg0==null) sch="";else sch=arg0.toString().trim();
				if(sch.length()==0) ch=-1;else ch=Integer.parseInt(sch);
					
				bindChannel(arg1,ch);
			}
		}
		
		/**
		 * apply all device bind settings
		 */
		public void applyBind()
		{
			recorder.clearBindedCaptureDevices();
			
			for(DeviceAssignment a:assignment) 
				if(a.chidx>=0) recorder.bindCaptureDevice(a.chidx,a.device);
		}
	}
	
	/**
	 * <h1>Description</h1>
	 * Used to select channel.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Apr 12, 2012 7:41:56 PM, revision:
	 */
	private class ChannelCellEditor implements TableCellEditor
	{
	private JComboBox chbox;
	private List<CellEditorListener> listener=new LinkedList<CellEditorListener>();
		
		public ChannelCellEditor()
		{
		String[] items;

			items=new String[recorder.numSensors()+1];
			items[0]="none";
			for(int i=1;i<items.length;i++) items[i]=Integer.toString(i-1);
			
			chbox=new JComboBox(items);
			chbox.addItemListener(new ItemListener(){
				public void itemStateChanged(ItemEvent e)
				{
					stopCellEditing();
				}
			});
		}
		
		public Object getCellEditorValue()
		{
		String val;
		
			val=chbox.getSelectedItem().toString();
			if("none".equals(val)) return "";else return val;
		}

		public boolean isCellEditable(EventObject anEvent)
		{
			return true;
		}

		public boolean shouldSelectCell(EventObject anEvent)
		{
			return true;
		}

		public boolean stopCellEditing()
		{
		ChangeEvent event;
			
			event=new ChangeEvent(this);
			/*
			 * !!!
			 * make a copy to prevent modification
			 */
			for(CellEditorListener l:new LinkedList<CellEditorListener>(listener)) l.editingStopped(event);
			return true;
		}

		public void cancelCellEditing()
		{
		ChangeEvent event;
		
			event=new ChangeEvent(this);
			for(CellEditorListener l:new LinkedList<CellEditorListener>(listener)) l.editingCanceled(event);
		}

		public void addCellEditorListener(CellEditorListener l)
		{
			listener.add(l);
		}

		public void removeCellEditorListener(CellEditorListener l)
		{
			listener.remove(l);
		}

		public Component getTableCellEditorComponent(JTable table,Object value,
				boolean isSelected,int row,int column)
		{
		String val;
		
			if(value==null) val="";else val=value.toString().trim();
			if("".equals(val)) val="none";
			
			chbox.setSelectedItem(val);
			
			return chbox;
		}
	}
	
	/**
	 * <h1>Description</h1>
	 * Show capture devices.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Apr 12, 2012 9:19:20 PM, revision:
	 */
	private class DeviceCellRenderer extends JPanel implements TableCellRenderer
	{
	private static final long serialVersionUID=-1961887070123185155L;
	private JLabel l0,l1;
	
		public DeviceCellRenderer()
		{
			this.setLayout(new GridLayout(2,1));
			l0=new JLabel();
			this.add(l0);
			l1=new JLabel();
			this.add(l1);
		}
	
		public Component getTableCellRendererComponent(JTable table,
				Object value,boolean isSelected,boolean hasFocus,int row,
				int column)
		{
		String[] val;
		
			if(value==null) 
			{
				l0.setText("");
				l1.setText("");
			}
			else
			{
				val=value.toString().trim().split("\n");
				l0.setText(val[0]);
				l1.setText(val[1]);
			}

			return this;
		}
	}

	/**
	 * @param recorder
	 * recorder reference
	 * @throws LineUnavailableException 
	 */
	public DeviceBindingDialog(BSSRecorder recorder) throws LineUnavailableException
	{
		super(recorder,"Bind Capture Devices",true);
		this.recorder=recorder;
		
		initGUI();
	}
		
	private void initGUI() throws LineUnavailableException
	{
		this.addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent e)
			{
				DeviceBindingDialog.this.setVisible(false);
				DeviceBindingDialog.this.dispose();
			}
		});
		
		this.setLayout(new BorderLayout(5,5));
		
		/*
		 * device table
		 */		
		
		JTable td=new JTable();
		tmodel=new DeviceTableModel();
		td.setModel(tmodel);
		td.getColumn(tmodel.getColumnName(0)).setCellRenderer(new DeviceCellRenderer());
		td.getColumn(tmodel.getColumnName(1)).setCellEditor(new ChannelCellEditor());
		
		td.setRowHeight(40);
		td.getColumn(tmodel.getColumnName(0)).setMinWidth(400);
		td.getColumn(tmodel.getColumnName(1)).setMinWidth(80);
		
		JScrollPane sp=new JScrollPane(td);
		this.add(sp,BorderLayout.CENTER);
		
		/*
		 * buttons
		 */
		JPanel pb=new JPanel();
		pb.setLayout(new FlowLayout(FlowLayout.RIGHT,5,5));
		this.add(pb,BorderLayout.SOUTH);
		
		JButton bok=new JButton("Ok");
		bok.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				tmodel.applyBind();
				
				DeviceBindingDialog.this.setVisible(false);
				DeviceBindingDialog.this.dispose();
			}
		});
		pb.add(bok);
		
		JButton bcancel=new JButton("Cancel");
		bcancel.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				DeviceBindingDialog.this.setVisible(false);
				DeviceBindingDialog.this.dispose();
			}
		});
		pb.add(bcancel);
	
		//show dialog
		{
		int locx,locy;

			this.pack();
			this.setSize(this.getWidth()+80,this.getHeight());

			locx=(recorder.getWidth()-this.getWidth())/2+recorder.getX();
			if(locx<0) locx=0;
			locy=(recorder.getHeight()-this.getHeight())/2+recorder.getY();
			if(locy<0) locy=0;
			this.setLocation(locx,locy);
			
			this.setVisible(true);
		}
	}
}
