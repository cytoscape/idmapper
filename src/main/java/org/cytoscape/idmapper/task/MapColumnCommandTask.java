package org.cytoscape.idmapper.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.command.StringToModel;
import org.cytoscape.idmapper.IdMapping;
import org.cytoscape.idmapper.MappingSource;
import org.cytoscape.idmapper.Species;
import org.cytoscape.idmapper.internal.BridgeDbIdMapper;
import org.cytoscape.idmapper.internal.MappingUtil;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ContainsTunables;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.json.JSONResult;
import org.cytoscape.work.util.ListSingleSelection;

/*
 * #%L
 * Cytoscape idmapper (org.cytoscape.idmapper.task)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2010 - 2018 The Cytoscape Consortium
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
//	private final CyJSONUtil cyJSONUtil;
	
	@ProvidesTitle
	public String getTitle() {
		return "Map Column";
	}
	@ContainsTunables
//	public TableTunable tableTunable = null;


	@Tunable(description="Species", gravity=0.0, 	         
		context="nogui", required=true,
		longDescription="The combined common or latin name of the species to which the identifiers apply",
		exampleStringValue = "Yeast")
	public ListSingleSelection<String> species  =  new ListSingleSelection<String>(Species.fullNames());
	
	@Tunable(description="Column name",
	         longDescription="Specifies the column nmae where the source identifiers are located",
			exampleStringValue="name", context="nogui", required=true)
	public ListSingleSelection<String> columnName = new ListSingleSelection<String>();

	@Tunable(description="Source Database",
			longDescription="Specifies the database describing the existing identifiers",
			exampleStringValue="Ensembl", context="nogui", required=true)
	public ListSingleSelection<String> mapFrom = new ListSingleSelection<String>();

	@Tunable(description="New Column Name",
	         longDescription="Specifies the database identifiers to be looked up",
	    	         context="nogui", exampleStringValue="SGD", required=true)
	public ListSingleSelection<String> mapTo = new ListSingleSelection<String>();

	@Tunable(description="Force single ", gravity=3.0, longDescription="When multiple identifiers can be mapped from a single term, this forces a singular result", exampleStringValue="true")
	public boolean forceSingle = true;

	@Tunable(description="Network ", context="nogui", longDescription="Which network is used in the mapping", exampleStringValue="current")
	public String networkName = "current";
	
	@Tunable(description="Table ", context="nogui", longDescription="Which table is used as the source of the identifiers", exampleStringValue="default node")
	public String table  = "node:current";
	
	
	
	
	CyColumn column = null;
	CyServiceRegistrar serviceRegistrar;
	
	MapColumnCommandTask(CyServiceRegistrar registrar) {
		tableManager = registrar.getService(CyTableManager.class);
//		cyJSONUtil = registrar.getService(CyJSONUtil.class);
//		tableTunable = new TableTunable(tableManager);
		serviceRegistrar = registrar;
		if (VERBOSE) System.out.println("create MapColumnCommandTask");		
	}
	private boolean VERBOSE = false;
	private Set<String> matched_ids;
	private Set<String> unmatched_ids;
	private Map<String, IdMapping> res;
	private CyTable networkTable;
	private CyTable nodeTable;
	private String new_column_name;

	@Override
	public void run(final TaskMonitor taskMonitor) throws Exception {
		if (VERBOSE) System.out.println("run MapColumnCommandTask");		
		StringToModel stMod = serviceRegistrar.getService(StringToModel.class);
		if (stMod == null) 
		{
			taskMonitor.showMessage(TaskMonitor.Level.ERROR,  "Unable to StringToModel bundle");
			return;
		}
		CyNetwork network = stMod.getNetwork(networkName);
		if (network == null)
			network = serviceRegistrar.getService(CyApplicationManager.class).getCurrentNetwork();
		networkTable = stMod.getTable("defaultNetwork");
			
		
		
		nodeTable = stMod.getTable(table);			//"node:current"
		if (nodeTable == null) {
	//		nodeTable = serviceRegistrar.getService(CyApplicationManager.class).getCurrentTable();
			nodeTable = network.getDefaultNodeTable();
			// TODO:  taskMonitor.showMessage flashes the text in bottom left corner, but its gone for user can read it
			if (nodeTable == null) {
				taskMonitor.showMessage(TaskMonitor.Level.ERROR,  "Unable to find node table");
				return;
			}
		}
		
		
		if (mapFrom == null) {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR,  "Column name must be specified");
			return;
		}

		if (mapTo == null) {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR,  "New column name must be specified");
			return;
		}

		String rawTarget = mapTo.getSelectedValue();
		String rawSource = columnName.getSelectedValue();
		column = nodeTable.getColumn(rawSource);
		if (column == null) {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR,  "Can't find column "+rawSource+" in table "+nodeTable.toString());
			return;
		}
//		column.setName(target_selection.getSelectedValue());
		String speciesVal = null;
		CyRow networkTableRow0 = null;
		System.out.println("networkTable " + networkTable);
		if (networkTable != null)
		{
			networkTableRow0 = networkTable.getRow(0);
			networkTableRow0.set("idmapper.species", speciesVal);
			System.out.println("write species as " + speciesVal);
			
			if (speciesVal != null)
				species.setSelectedValue(speciesVal);
		}
		else taskMonitor.showMessage(TaskMonitor.Level.ERROR,  "networkTable not found");
		
		speciesVal = species.getSelectedValue();
			final MappingSource source = MappingSource.nameLookup("" + mapFrom.getSelectedValue());
			if (column.getType() ==  Double.class || column.getType() ==  Integer.class || column.getType() ==  Boolean.class)
			{
				if (VERBOSE) System.out.println("Can't map a numeric column as identifiers");		// tell the user?
				return;
			}
//			System.out.println("raw str: " + rawTarget);
			MappingSource saveTarget = MappingSource.nameLookup(rawTarget);
//			System.out.println("reading target as " + saveTarget);
			Species saveSpecies = Species.lookup(speciesVal);
			if (VERBOSE) System.out.println("saving species as " + saveSpecies.name());


			boolean source_is_list = false;
			if (column.getType() == List.class)
				source_is_list = true;

			final List values = column.getValues(column.getType());

			final List<String> ids = new ArrayList<String>();
			for (final Object v : values) {
				// System.out.println(v);
				if (v != null) {
					if (source_is_list) {
						for (final Object lv : (List) v)
							MappingUtil.addCleanedStrValueToList(ids, lv);
					} else
						MappingUtil.addCleanedStrValueToList(ids, v);
				}
			}

			try {
				final BridgeDbIdMapper map = new BridgeDbIdMapper();
				res = map.map(ids, source.system(), saveTarget.system(), saveSpecies.name(), saveSpecies.name());
				matched_ids = map.getMatchedIds();
				unmatched_ids = map.getUnmatchedIds();
			} catch (final Exception e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						JOptionPane.showMessageDialog(null, e.getMessage(), "ID Mapping Error", JOptionPane.ERROR_MESSAGE);
					}
				});
				return;
			}

			if (VERBOSE) {
				System.out.println();
				System.out.println("Unmatched:");
				if (unmatched_ids != null) {
					for (final String u : unmatched_ids) {
						System.out.println(u);
					}
				}
				System.out.println();
				System.out.println("Matched:");
				if (matched_ids != null) {
					for (final String u : matched_ids) {
						System.out.println(u);
					}
				}
				System.out.println();
			}
			new_column_name = saveTarget.descriptor();
			new_column_name = MappingUtil.makeNewColumnName(new_column_name,
					source.descriptor(), new_column_name, column);

			boolean all_unique = true;
			int non_unique = 0;
			int unique = 0;
			int min = Integer.MAX_VALUE;
			int max = 0;

			if (res != null)
				for (final Entry<String, IdMapping> entry : res.entrySet()) {
					final Set<String> v = entry.getValue().getTargetIds();
					if (v != null) {
						if (v.size() > 1) {
							all_unique = false;
							++non_unique;
							if (v.size() > max)		max = v.size();
							if (v.size() < min)		min = v.size();
						} else
							++unique;
					}
				}

//			final CyTable table = column.getTable();
			
	// TODO -- #3666 add the new column after the original, not at end of table
//			int index = getColumnIndex(table, column);
//			System.out.println("Index = " + index);
			
			
			boolean many_to_one = false;
			if (matched_ids.size() > 0) {
				boolean all_single = false;
				if (forceSingle) 
					nodeTable.createColumn(new_column_name, String.class, false);	//index, 
				else {  
					all_single = MappingUtil.isAllSingle(source_is_list, res, column, nodeTable);
					if (all_single) 
						nodeTable.createColumn(new_column_name, String.class, false);		//index, 
					 else 
						 nodeTable.createListColumn(new_column_name, String.class, false);	//index, 
				}
				many_to_one = MappingUtil.fillNewColumn(source_is_list, res, nodeTable, column, new_column_name,
						forceSingle || all_single);

//				moveLastColumnTo(table, index+1);
//				System.out.println("moveLastColumnTo " + (index+1));
			}
			String targ = saveTarget.descriptor();
			String src = source.descriptor();
			String msg = MappingUtil.createMsg(new_column_name, targ, src, ids, matched_ids, all_unique, non_unique,
					unique, min, max, many_to_one, forceSingle);

//			taskMonitor.showMessage(TaskMonitor.Level.INFO, msg);

//			putSpeciesProperty(saveSpecies.name());
		}

	@SuppressWarnings("unchecked")
	@Override
	public <R> R getResults(Class<? extends R> type) {
		if (type.equals(String.class)) {
			if (column == null)
				return (R)"Unable to map column";
			String res = "Mapped column "+mapFrom.getSelectedValue()+" in table "+nodeTable+" to "+mapTo.getSelectedValue();
			return (R)res;
		} else if (type.equals(JSONResult.class)) {
		
			JSONResult res = () -> {if (column == null)
				return "{}";
			else {
				if (VERBOSE) System.out.println("column = " + new_column_name);   // cyJSONUtil.toJson(column, true, false));
				return "{ \"new column\" : \"" + new_column_name + " \" }" ;  // cyJSONUtil.toJson(column, true, false);
			}};
			return (R)res;
		}
		return (R)column;
	}

	@Override
	public List<Class<?>> getResultClasses() {	return Arrays.asList(CyColumn.class, String.class, JSONResult.class);	}
}
