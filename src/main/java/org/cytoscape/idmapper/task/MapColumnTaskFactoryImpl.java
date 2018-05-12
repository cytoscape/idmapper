package org.cytoscape.idmapper.task;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.cytoscape.model.CyColumn;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.AbstractTableColumnTaskFactory;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TunableSetter;
import org.cytoscape.work.undo.UndoSupport;

public class MapColumnTaskFactoryImpl extends AbstractTableColumnTaskFactory implements MapColumnTaskFactory, TaskFactory {

	private final UndoSupport undoSupport;
	private CyServiceRegistrar serviceRegistrar;
	private final TunableSetter tunableSetter; 

	public MapColumnTaskFactoryImpl(final UndoSupport undo, final TunableSetter tunable,final CyServiceRegistrar reg) {
		this.undoSupport = undo;
		this.tunableSetter = tunable;
		serviceRegistrar = reg;
	}

	public String ERROR = "Can't map this column type as identifiers";
	class EmptyTask extends AbstractTask
	{
		@Override	public void run(TaskMonitor taskMonitor) throws Exception {	
			SwingUtilities.invokeLater(new Runnable() {
				@Override public void run() {
					JOptionPane.showMessageDialog(null, ERROR, "ID Mapping Result", JOptionPane.WARNING_MESSAGE);
				} });

		}
	}
	@Override
	public TaskIterator createTaskIterator(final CyColumn column) {

		if (column == null)
			throw new IllegalStateException("you forgot to set the CyColumn on this task factory.");

		Class<?>	 type = column.getType();
		if (type == Double.class) return new TaskIterator(new EmptyTask());
		if (type == Integer.class) return new TaskIterator(new EmptyTask());
		
		return new TaskIterator(new ColumnMappingTask(undoSupport, column, serviceRegistrar));   //ColumnMultiMappingTask
	}

	@Override
	public TaskIterator createTaskIterator(final CyColumn column, final String newColumnName) {
	return null;
//	final Map<String, Object> m = new HashMap<String, Object>();
//		m.put("newColumnName", newColumnName);
//
//		return tunableSetter.createTaskIterator(this.createTaskIterator(column), m); 
	}

	@Override
	public TaskIterator createTaskIterator() {
		return new TaskIterator(new MapColumnCommandTask(serviceRegistrar));
	}

	@Override
	public boolean isReady() {
		return true;
	}

}
