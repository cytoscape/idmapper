package org.cytoscape.task.internal.table;

import org.cytoscape.model.CyColumn;
import org.cytoscape.task.AbstractTableColumnTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TunableSetter;
import org.cytoscape.work.undo.UndoSupport;

public class MapColumnTaskFactoryImpl  extends AbstractTableColumnTaskFactory implements MapColumnTaskFactory {

	private final UndoSupport undoSupport;

	private final TunableSetter tunableSetter; 

	public MapColumnTaskFactoryImpl(final UndoSupport undoSupport, TunableSetter tunableSetter) {
		this.undoSupport = undoSupport;
		this.tunableSetter = tunableSetter;
	}
	
	@Override
	public TaskIterator createTaskIterator(CyColumn column) {
		if (column == null)
			throw new IllegalStateException("you forgot to set the CyColumn on this task factory.");
		return new TaskIterator(new MapColumnTask(undoSupport, column));
	}
	

	@Override
	public TaskIterator createTaskIterator(CyColumn column, String newColumnName) {
		// TODO Auto-generated method stub
		return null;
	}

}
