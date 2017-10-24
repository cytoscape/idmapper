package org.cytoscape.idmapper.task;

import java.util.Arrays;
import java.util.List;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.json.CyJSONUtil;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ContainsTunables;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.json.JSONResult;


 class TableTunable {
	CyTableManager tableManager;

	@Tunable(description="Table", context="nogui", longDescription="Specifies a table by table name. If the prefix ```SUID:``` is used, the table corresponding the SUID will be returned.", exampleStringValue="galFiltered.sif default node")
	public String table;

	public TableTunable(CyTableManager tableManager) {
		this.tableManager = tableManager;
	}

	public String getTableString() {
		return table;
	}

	public CyTable getTable() { 
		if (table == null) return null;

		if (table.toLowerCase().startsWith("suid:")) {
			String[] tokens = table.split(":");
			CyTable t = tableManager.getTable(Long.parseLong(tokens[1].trim()));
			return t;
		} else {
			for (CyTable t: tableManager.getAllTables(true)) {
				if (t.getTitle().equalsIgnoreCase(table))
					return t;
			}
		}
		return null;
	}
}

/*
 * #%L
 * Cytoscape Core Task Impl (core-task-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2010 - 2016 The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

public final class MapColumnCommandTask extends AbstractTask implements ObservableTask {
	private final CyTableManager tableManager;
	private final CyJSONUtil cyJSONUtil;
	
	@ProvidesTitle
	public String getTitle() {
		return "Map Column";
	}
	
	@Tunable(description="New Column Name",
	         longDescription="The new name of the column.",
	         exampleStringValue="NewColumnName",
	         required=true)
	public String newColumnName;

	@ContainsTunables
	public TableTunable tableTunable = null;

	@Tunable(description="Column name",
	         longDescription="The name of the column that will be mapped.",
					 exampleStringValue="ColumnName",
	         context="nogui",
	         required=true)
	public String columnName = null;

	CyColumn column = null;

	MapColumnCommandTask(CyServiceRegistrar registrar) {
		tableManager = registrar.getService(CyTableManager.class);
		cyJSONUtil = registrar.getService(CyJSONUtil.class);
		tableTunable = new TableTunable(tableManager);
	}

	@Override
	public void run(final TaskMonitor taskMonitor) throws Exception {
		CyTable table = tableTunable.getTable();
		if (table == null) {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR,  "Unable to find table '"+tableTunable.getTableString()+"'");
			return;
		}

		if (columnName == null) {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR,  "Column name must be specified");
			return;
		}

		if (newColumnName == null) {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR,  "New column name must be specified");
			return;
		}

		column = table.getColumn(columnName);
		if (column == null) {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR,  "Can't find column "+columnName+" in table "+table.toString());
			return;
		}
		column.setName(newColumnName);
		return;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <R> R getResults(Class<? extends R> type) {
		if (type.equals(String.class)) {
			if (column == null)
				return (R)"Unable to map column";
			String res = "Mapped column "+columnName+" in table "+tableTunable.getTable()+" to "+newColumnName;
			return (R)res;
		} else if (type.equals(JSONResult.class)) {
			JSONResult res = () -> {if (column == null)
				return "{}";
			else {
				return cyJSONUtil.toJson(column, true, false);
			}};
			return (R)res;
		}
		return (R)column;
	}

	@Override
	public List<Class<?>> getResultClasses() {	return Arrays.asList(CyColumn.class, String.class, JSONResult.class);	}
}
