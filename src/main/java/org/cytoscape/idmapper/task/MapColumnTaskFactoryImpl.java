package org.cytoscape.idmapper.task;

import org.cytoscape.model.CyColumn;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.AbstractTableColumnTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TunableSetter;
import org.cytoscape.work.undo.UndoSupport;

public class MapColumnTaskFactoryImpl extends AbstractTableColumnTaskFactory implements MapColumnTaskFactory {

	private final UndoSupport undoSupport;
	private CyServiceRegistrar serviceRegistrar;

	public MapColumnTaskFactoryImpl(final UndoSupport undo, final TunableSetter tunable,final CyServiceRegistrar reg) {
		this.undoSupport = undo;
		serviceRegistrar = reg;
	}

	@Override
	public TaskIterator createTaskIterator(final CyColumn column) {

		if (column == null)
			throw new IllegalStateException("you forgot to set the CyColumn on this task factory.");

		Class<?>	 type = column.getType();
		if (type == Double.class) return null;
		if (type == Integer.class) return null;
		return new TaskIterator(new ColumnMappingTask(undoSupport, column, serviceRegistrar));
	}

	@Override
	public TaskIterator createTaskIterator(final CyColumn column, final String newColumnName) {
		return null;
	}

}
