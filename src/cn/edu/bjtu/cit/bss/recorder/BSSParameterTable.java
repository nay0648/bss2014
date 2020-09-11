package cn.edu.bjtu.cit.bss.recorder;
import java.awt.*;
import java.awt.event.*;
import java.util.EventObject;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import cn.edu.bjtu.cit.bss.*;
import cn.edu.bjtu.cit.bss.FDBSS.Parameter;
import cn.edu.bjtu.cit.bss.preprocess.*;
import cn.edu.bjtu.cit.bss.ica.*;
import cn.edu.bjtu.cit.bss.iva.*;
import cn.edu.bjtu.cit.bss.align.*;

/**
 * 
 * <h1>Description</h1>
 * FDBSS parameter table.
 * <h1>abstract</h1>
 * <h1>keywords</h1>
 * @author nay0648<br>
 * if you have any questions, advices, suggests, or find any bugs, 
 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
 * @version created on: Apr 13, 2012 3:19:00 PM, revision:
 */
public class BSSParameterTable extends JTable
{
private static final long serialVersionUID=7374649141636410183L;
private BSSParameterTableModel paramodel;//underlying table model
private BSSRecorder recorder;//recorder reference
private TableCellEditor numsourceseditor;//edit number of sources
private STFTOverlapEditor overlapeditor;//edit stft overlap factor
private PreprocessorEditor preeditor;//edit preprocessor
private ICAEditor icaeditor;//edit ica algorithms
private AlignEditor aligneditor;//edit align policy

	private class ParamContent
	{
	String name;//name of this parameter
	Parameter key;
	String value;

		public ParamContent(String name,Parameter key,String value)
		{
			this.name=name;
			this.key=key;
			this.value=value;
		}
	}

	/**
	 * <h1>Description</h1>
	 * Set parameters for FDBSS.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Apr 13, 2012 10:14:18 AM, revision:
	 */
	private class BSSParameterTableModel implements TableModel
	{
	private List<ParamContent> param;//parameters
	private List<TableModelListener> listener=new LinkedList<TableModelListener>();

		public BSSParameterTableModel()
		{		
			param=new ArrayList<ParamContent>(7);
			
			param.add(new ParamContent("Number of Sources:",Parameter.num_sources,"2"));
			
			param.add(new ParamContent("STFT Block Size:",Parameter.stft_size,"1024"));
			param.add(new ParamContent("STFT Block Overlap:",Parameter.stft_overlap,"0.875"));
			param.add(new ParamContent("FFT Block Size:",Parameter.fft_size,"2048"));
			
			param.add(new ParamContent("Preprocessor:",Parameter.preprocessor,"cn.edu.bjtu.cit.bss.preprocess.Whitening"));
			param.add(new ParamContent("ICA Algorithm:",Parameter.ica_algorithm,"cn.edu.bjtu.cit.bss.ica.CFastICA"));
			param.add(new ParamContent("Align Policy:",Parameter.align_policy,"cn.edu.bjtu.cit.bss.align.RegionGrow"));
		}

		public int getRowCount()
		{
			return param.size();
		}

		public int getColumnCount()
		{
			return 2;
		}

		public String getColumnName(int columnIndex)
		{
			if(columnIndex==0) return "Parameter Name";
			else if(columnIndex==1) return "value";
			else return "";
		}

		public Class<?> getColumnClass(int columnIndex)
		{
			return String.class;
		}

		public boolean isCellEditable(int rowIndex,int columnIndex)
		{
			if(columnIndex==0) return false;
			else return true;
		}

		public Object getValueAt(int rowIndex,int columnIndex)
		{
			if(columnIndex==0) return param.get(rowIndex).name;
			else if(columnIndex==1) return param.get(rowIndex).value;
			else return null;
		}

		public void setValueAt(Object aValue,int rowIndex,int columnIndex)
		{
		TableModelEvent event;
		
			if(columnIndex==1) 
			{	
				param.get(rowIndex).value=aValue.toString().trim();
				
				/*
				 * send event
				 */
				event=new TableModelEvent(this,rowIndex,rowIndex,columnIndex,TableModelEvent.UPDATE);
				for(TableModelListener l:new LinkedList<TableModelListener>(listener)) l.tableChanged(event);
			}
		}
		
		public void addTableModelListener(TableModelListener l)
		{
			listener.add(l);
		}

		public void removeTableModelListener(TableModelListener l)
		{
			listener.remove(l);		
		}
		
		/**
		 * get stft overlap in number of samples
		 * @return
		 */
		private int stftOverlap()
		{
		int stftsize=0;
		double factor=0;
		
			for(ParamContent p:param) 
				if(p.key==Parameter.stft_size) stftsize=Integer.parseInt(p.value);
				else if(p.key==Parameter.stft_overlap) factor=Double.parseDouble(p.value);
		
			return (int)(stftsize*factor);
		}
		
		/**
		 * set parameters
		 * @param fdbss
		 * bss algorithm reference
		 */
		public void setParameters(FDBSS fdbss)
		{
			for(ParamContent p:param) 
				if(p.key==Parameter.stft_overlap) 
					fdbss.setParameter(p.key,Integer.toString(stftOverlap()));
				else fdbss.setParameter(p.key,p.value);
		}
		
		/**
		 * get parameter key
		 * @param row
		 * @return
		 */
		public Parameter paramKey(int row)
		{
			return param.get(row).key;
		}
	}
	
	/**
	 * <h1>Description</h1>
	 * Abstract class for parameter cell editor.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Apr 13, 2012 3:36:06 PM, revision:
	 */
	private abstract class ParamCellEditor implements TableCellEditor
	{
	private List<CellEditorListener> listener=new LinkedList<CellEditorListener>();
		
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
	}
	
	/**
	 * <h1>Description</h1>
	 * Edit number of sources.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Apr 13, 2012 3:36:58 PM, revision:
	 */
	private class NumSourcesEditor extends ParamCellEditor
	{
	private JComboBox cbox;

		public Object getCellEditorValue()
		{
			return cbox.getSelectedItem();
		}
		
		public Component getTableCellEditorComponent(JTable table,Object value,
				boolean isSelected,int row,int column)
		{
			/*
			 * update number of sensors every time
			 */
			cbox=new JComboBox();
			for(int n=2;n<=recorder.numSensors();n++) cbox.addItem(Integer.toString(n));

			cbox.addItemListener(new ItemListener(){
				public void itemStateChanged(ItemEvent e)
				{
					stopCellEditing();
				}
			});
			
			cbox.setSelectedItem(value);
			return cbox;
		}
	}
	
	/**
	 * <h1>Description</h1>
	 * Edit stft overlap factor
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Apr 13, 2012 4:24:05 PM, revision:
	 */
	private class STFTOverlapEditor extends ParamCellEditor
	{
	private JComboBox cbox;
	
		public STFTOverlapEditor()
		{
			cbox=new JComboBox();
			cbox.addItem("0.5");
			cbox.addItem("0.75");
			cbox.addItem("0.875");
			cbox.addItem("0.9375");
			
			cbox.addItemListener(new ItemListener(){
				public void itemStateChanged(ItemEvent e)
				{
					stopCellEditing();
				}
			});
		}
		
		public Object getCellEditorValue()
		{
			return cbox.getSelectedItem();
		}
		
		public Component getTableCellEditorComponent(JTable table,Object value,
				boolean isSelected,int row,int column)
		{	
			cbox.setSelectedItem(value);
			return cbox;
		}
	}
	
	/**
	 * <h1>Description</h1>
	 * Preprocessor editor.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Apr 13, 2012 4:28:27 PM, revision:
	 */
	private class PreprocessorEditor extends ParamCellEditor
	{
	private JComboBox cbox;
	
		public PreprocessorEditor()
		{
			cbox=new JComboBox();
			cbox.addItem(Centering.class.getName());
			cbox.addItem(PCA.class.getName());
			cbox.addItem(Whitening.class.getName());
			cbox.addItem(FOBI.class.getName());
			
			cbox.addItemListener(new ItemListener(){
				public void itemStateChanged(ItemEvent e)
				{
					stopCellEditing();
				}
			});
		}
		
		public Object getCellEditorValue()
		{
			return cbox.getSelectedItem();
		}
		
		public Component getTableCellEditorComponent(JTable table,Object value,
				boolean isSelected,int row,int column)
		{
			cbox.setSelectedItem(value);
			return cbox;
		}
	}
	
	/**
	 * <h1>Description</h1>
	 * Edit ICA algorithms.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Apr 13, 2012 4:32:02 PM, revision:
	 */
	private class ICAEditor extends ParamCellEditor
	{
	private JComboBox cbox;
	
		public ICAEditor()
		{
			cbox=new JComboBox();
			cbox.addItem(CFastICA.class.getName());
			cbox.addItem(NCFastICA.class.getName());
			cbox.addItem(RobustICA.class.getName());
			cbox.addItem(ScaledInfomaxICA.class.getName());
			cbox.addItem(MLICA.class.getName());
			cbox.addItem(FastMLICA.class.getName());
			cbox.addItem(IVABSS.class.getName());
			cbox.addItem(FIVABSS.class.getName());
			cbox.addItem(GradientIVA.class.getName());
			cbox.addItem(FastIVA.class.getName());
			cbox.addItem(SubbandSubspaceFIVA.class.getName());
			
			cbox.addItemListener(new ItemListener(){
				public void itemStateChanged(ItemEvent e)
				{
					stopCellEditing();
				}
			});
		}
		
		public Object getCellEditorValue()
		{
			return cbox.getSelectedItem();
		}
		
		public Component getTableCellEditorComponent(JTable table,Object value,
				boolean isSelected,int row,int column)
		{
			cbox.setSelectedItem(value);
			return cbox;
		}
	}
	
	/**
	 * <h1>Description</h1>
	 * Edit align policy.
	 * <h1>abstract</h1>
	 * <h1>keywords</h1>
	 * @author nay0648<br>
	 * if you have any questions, advices, suggests, or find any bugs, 
	 * please mail me: <a href="mailto:nay0648@163.com">nay0648@163.com</a>
	 * @version created on: Apr 13, 2012 4:36:34 PM, revision:
	 */
	private class AlignEditor extends ParamCellEditor
	{
	private JComboBox cbox;
	
		public AlignEditor()
		{
			cbox=new JComboBox();
			cbox.addItem(IdentityAlign.class.getName());
			cbox.addItem(SequentialAlign.class.getName());
			cbox.addItem(DyadicSorting.class.getName());
			cbox.addItem(RegionGrow.class.getName());
			cbox.addItem(SpectralOrdering.class.getName());
			cbox.addItem(CWKKMeans.class.getName());
			
			cbox.addItemListener(new ItemListener(){
				public void itemStateChanged(ItemEvent e)
				{
					stopCellEditing();
				}
			});
		}
		
		public Object getCellEditorValue()
		{
			return cbox.getSelectedItem();
		}
		
		public Component getTableCellEditorComponent(JTable table,Object value,
				boolean isSelected,int row,int column)
		{
			cbox.setSelectedItem(value);
			return cbox;
		}
	}
	
	/**
	 * @param recorder
	 * recorder reference
	 */
	public BSSParameterTable(BSSRecorder recorder)
	{
		super();
		this.recorder=recorder;
		paramodel=new BSSParameterTableModel();
		this.setModel(paramodel);
		this.setRowHeight(20);
		
		/*
		 * customize cell editors
		 */
		numsourceseditor=new NumSourcesEditor();
		overlapeditor=new STFTOverlapEditor();
		preeditor=new PreprocessorEditor();
		icaeditor=new ICAEditor();
		aligneditor=new AlignEditor();
	}
	
	public TableCellEditor getCellEditor(int row,int column)
	{
		if(column==1) 
			switch(paramodel.paramKey(row)) 
			{
				case num_sources: 
					return numsourceseditor;
				case stft_overlap:
					return overlapeditor;
				case preprocessor:
					return preeditor;
				case ica_algorithm:
					return icaeditor;
				case align_policy:
					return aligneditor;
			}
		
		//use default editor
		return this.getDefaultEditor(String.class);
	}
	
	/**
	 * set parameters
	 * @param fdbss
	 * bss algorithm reference
	 */
	public void setParameters(FDBSS fdbss)
	{
		paramodel.setParameters(fdbss);
	}
}
