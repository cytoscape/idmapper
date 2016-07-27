package org.cytoscape.idmap;

import org.cytoscape.model.CyColumn;
import org.cytoscape.task.AbstractTableColumnTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TunableSetter;
import org.cytoscape.work.undo.UndoSupport;

public class MapColumnTaskFactoryImpl extends AbstractTableColumnTaskFactory implements MapColumnTaskFactory {

	private final UndoSupport undoSupport;

	public static enum MAP_SERVICE {
		KO, BRIDGE_DB;
	}

	final private MAP_SERVICE ms = MAP_SERVICE.BRIDGE_DB;

	public MapColumnTaskFactoryImpl(final UndoSupport undoSupport, final TunableSetter tunableSetter) {
		this.undoSupport = undoSupport;
	}

	@Override
	public TaskIterator createTaskIterator(final CyColumn column) {

		if (column == null)
			throw new IllegalStateException("you forgot to set the CyColumn on this task factory.");

		switch (ms) {
		case BRIDGE_DB:
			return new TaskIterator(new MapColumnTaskBridgeDb(undoSupport, column));
		default:
			return new TaskIterator(new MapColumnTaskKO(undoSupport, column));
		}
	}

	@Override
	public TaskIterator createTaskIterator(final CyColumn column, final String newColumnName) {
		return null;
	}

}
