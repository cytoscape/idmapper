package org.cytoscape.idmapper.task;

import org.cytoscape.model.CyColumn;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.AbstractTableColumnTaskFactory;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TunableSetter;
import org.cytoscape.work.undo.UndoSupport;

public class MapColumnTaskFactoryImpl extends AbstractTableColumnTaskFactory implements MapColumnTaskFactory, TaskFactory {

	private final UndoSupport undoSupport;
	private CyServiceRegistrar serviceRegistrar;
	private final TunableSetter tunableSetter; 

	public MapColumnTaskFactoryImpl(final UndoSupport undo, final TunableSetter tunable,final CyServiceRegistrar reg) {
		undoSupport = undo;
		tunableSetter = tunable;
		serviceRegistrar = reg;
	}

	@Override
	public TaskIterator createTaskIterator(final CyColumn column) {

		if (column == null)
			throw new IllegalStateException("you forgot to set the CyColumn on this task factory.");

		return new TaskIterator(new ColumnMappingTask(undoSupport, column, serviceRegistrar));  
	}

	@Override
	public TaskIterator createTaskIterator(final CyColumn column, final String newColumnName) {
	return createTaskIterator();
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
