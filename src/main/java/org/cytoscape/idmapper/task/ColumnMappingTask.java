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
import org.cytoscape.idmapper.IdMapping;
import org.cytoscape.idmapper.MappingSource;
import org.cytoscape.idmapper.Species;
import org.cytoscape.idmapper.internal.BridgeDbIdMapper;
import org.cytoscape.idmapper.internal.MappingUtil;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.AbstractTableColumnTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.json.JSONResult;
import org.cytoscape.work.undo.UndoSupport;
import org.cytoscape.work.util.ListChangeListener;
import org.cytoscape.work.util.ListSelection;
import org.cytoscape.work.util.ListSingleSelection;

public class ColumnMappingTask extends AbstractTableColumnTask
								implements ListChangeListener<String>, ObservableTask {


//	public static final boolean DEBUG = true;
	public static final boolean VERBOSE = true;
	private static Species saveSpecies = Species.Human;
	private static MappingSource saveSource = MappingSource.Entrez;
	private static MappingSource saveTarget = MappingSource.Entrez;
	private  final CyServiceRegistrar registrar;

	class MappingSourceListener implements  ListChangeListener<MappingSource>
	{
		MappingSourceListener(ColumnMappingTask task)		{ }
		@Override	public void listChanged(ListSelection<MappingSource> source) { mappingSourceChanged(source); }
		@Override	public void selectionChanged(ListSelection<MappingSource> source) { mappingSourceChanged(source); }
	}
	
	public  	ColumnMappingTask(final UndoSupport undoSupport, final CyColumn column, final CyServiceRegistrar reg) {
		super(column);
		registrar = reg;
		if (column.getType() == String.class)
		{
			Species.buildMaps();
			speciesList.setPossibleValues(Species.fullNames());
			speciesList.addListener(this);
			mapFrom.addListener(new MappingSourceListener(this));
			mapTo.addListener(new MappingSourceListener(this));
			resetSpecies();
			String species = readSpeciesFromNetworkTable();
			if (species != null)
				speciesList.setSelectedValue(species);
		}
	}
	
	private String readSpeciesFromNetworkTable() 		
	{
		String speciesStr = getValueFromNetworkTable("idmapper.species");
		if (speciesStr != null)
		{
			Species sp = Species.lookup(speciesStr);		// matches name or common or latin
			if (VERBOSE) System.out.println("read as " + sp);
			if (sp != null)
				saveSpecies = sp;
		}
		if (VERBOSE) System.out.println("saveSpecies read as " + saveSpecies.fullname());
		return saveSpecies.fullname();
		
	}		

	public void storeSpeciesIntoNetworkTable()
	{
		putValueIntoNetworkTable("idmapper.species", saveSpecies.toString());
	}
	
	public void speciesSelectionChanged(ListSelection<String> source) 
	{
		if (source instanceof ListSingleSelection<?>)
		{
			ListSingleSelection<String> src = (ListSingleSelection<String>)source;
			if (src == speciesList)
			{
				String selected = speciesList.getSelectedValue();
				saveSpecies = Species.lookup(selected);
				if (VERBOSE) System.out.println("setting Species to: " + saveSpecies.fullname());
				resetSpecies();
			}
		}
	}
		
	//=========================================================================
	private String getValueFromNetworkTable(String key) 		
	{
		CyNetwork network  = registrar.getService(CyApplicationManager.class).getCurrentNetwork();
		CyTable networkTable = network.getDefaultNetworkTable();
		List<CyRow> rows = networkTable.getAllRows();
		if (rows.isEmpty()) return null;
		CyRow row = rows.get(0);
		return row.get(key, String.class);
	}
	
	private void putValueIntoNetworkTable(String key, String value) 		
	{
		CyNetwork network  = registrar.getService(CyApplicationManager.class).getCurrentNetwork();
		CyTable networkTable = network.getDefaultNetworkTable();
		if (networkTable != null )
		{
			CyColumn col = networkTable.getColumn(key);
			if (col == null)
				networkTable.createColumn(key, String.class, false); 
		}
		List<CyRow> rows = networkTable.getAllRows();
		if (rows.isEmpty()) 
		{
			return;
		}
		CyRow row = rows.get(0);
		row.set(key, value);
	}
	//=========================================================================
	public void mappingSourceChanged(ListSelection<MappingSource> source) 
	{
//		String name = null;
//		if (source == mapFrom) name = "source_selection";
//		if (source == mapTo) 	name = "target_selection";
//		System.out.println("selectionChanged: at " + name);
		
		if (source instanceof ListSingleSelection<?>)
		{
			ListSingleSelection<MappingSource> src = (ListSingleSelection<MappingSource>)source;
			if (src == mapFrom)		resetSource();
			else if (src == mapTo)
			{}			//	System.out.println("target is: " + target_selection.getSelectedValue());
			else
			System.err.println("selectionChanged error: " + source.toString());
		}
	}
		
		
	private void resetSpecies() {
		speciesList.setSelectedValue(saveSpecies.fullname());
		guessSource();
	}	
		
	private void guessSource() {
		
		List<MappingSource> strs = MappingSource.filteredStrings(saveSpecies, null);
		if (VERBOSE) System.out.println("filteredStrings: " + strs);
		mapFrom.setPossibleValues(strs);
		
		if (column != null)
		{
			final List<String> ids = column.getValues(String.class);
			saveSource = MappingSource.guessSource(saveSpecies, ids);
			mapFrom.setSelectedValue(saveSource);
			if (VERBOSE) System.out.println("\nguessed Source: " + saveSource.getMenuString());
			resetSource();
		}		
	}
	
	private void resetSource() {
		saveSource = mapFrom.getSelectedValue();
		if (VERBOSE) System.out.println("resettingSource: " + (saveSource == null ? "N/A" : saveSource.descriptor()));
		resetTarget(saveSource);
	}

//=========================================================================
private void resetTarget(MappingSource src)
{
	//	filter the targets to remove the source, and anything species-specific
	saveTarget = mapTo.getSelectedValue();
	if ((saveTarget == null || saveTarget == src) && mapTo.getPossibleValues().size() > 0)
		saveTarget = mapTo.getPossibleValues().get(0);
	if (saveTarget == null) 
		saveTarget = MappingSource.Ensembl;
	List<MappingSource> filtered = MappingSource.filteredStrings(saveSpecies, src);
	mapTo.setPossibleValues(filtered);
	mapTo.setSelectedValue(saveTarget);

}

//=========================================================================
	
	@ProvidesTitle
	public String getTitle() {		return "ID Mapping";	}
	
	// look at AbstractCyNetworkReader:98 for an example of Tunables with methods
	@Tunable(description="Species", gravity=0.0, longDescription="The latin name of the species to which the identifiers apply",exampleStringValue = "Homo Sapiens")
	public ListSingleSelection<String> speciesList  =  new ListSingleSelection<String>(Species.fullNames());

	@Tunable(description="Map from", gravity=1.0, longDescription="Specifies the database describing the existing identifiers", exampleStringValue="ENSEMBL")
	public ListSingleSelection<MappingSource> mapFrom = new ListSingleSelection<MappingSource>();

	@Tunable(description="To", gravity=2.0, longDescription="Specifies the database identifiers to be looked up", exampleStringValue="Entrez")
	public ListSingleSelection<MappingSource> mapTo	= new ListSingleSelection<MappingSource>();

	@Tunable(description="Force single ", gravity=3.0, longDescription="When multiple identifiers can be mapped from a single term, this forces a singular result", exampleStringValue="false")
	public boolean forceSingle = true;
	
	//------------------------------------------------------------------------
	public String new_column_name = "";

	@SuppressWarnings("rawtypes")
	@Override
	public void run(final TaskMonitor taskMonitor) {
		
		String species = speciesList.getSelectedValue();
		MappingSource rawTarget = mapTo.getSelectedValue();
		MappingSource source = mapFrom.getSelectedValue();
		if (column.getType() ==  Double.class || column.getType() ==  Integer.class || column.getType() ==  Boolean.class)
		{
			if (VERBOSE) System.out.println("Can't map a numeric column as identifiers");		// tell the user?
			return;
		}
		if (VERBOSE) System.out.println("raw str: " + rawTarget);
		saveTarget = rawTarget;
		if (VERBOSE) System.out.println("reading target as " + saveTarget);
		saveSpecies = Species.lookup(species);
		if (VERBOSE) System.out.println("saving species as " + saveSpecies.name());
		boolean source_is_list = false;
		if (column.getType() == List.class)
			source_is_list = true;
		
		storeSpeciesIntoNetworkTable();
	
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

		final Set<String> matched_ids;
		final Set<String> unmatched_ids;
		final Map<String, IdMapping> res;
		try {
			final BridgeDbIdMapper map = new BridgeDbIdMapper();
			res = map.map(ids, source.system(), saveTarget.system(), saveSpecies.name(), saveSpecies.name());
			matched_ids = map.getMatchedIds();
			unmatched_ids = map.getUnmatchedIds();
		} catch (final Exception e) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override public void run() {
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

		final CyTable table = column.getTable();
		
// TODO -- #3666 add the new column after the original, not at end of table
//		int index = getColumnIndex(table, column);
//		System.out.println("Index = " + index);
		
		
		boolean many_to_one = false;
		if (matched_ids.size() > 0) {
			boolean all_single = false;
			if (forceSingle) 
				table.createColumn(new_column_name, String.class, false);	//index, 
			else {  
				all_single = MappingUtil.isAllSingle(source_is_list, res, column, table);
				if (all_single) 
					table.createColumn(new_column_name, String.class, false);		//index, 
				 else 
					table.createListColumn(new_column_name, String.class, false);	//index, 
			}
			many_to_one = MappingUtil.fillNewColumn(source_is_list, res, table, column, new_column_name,
					forceSingle || all_single);

//			moveLastColumnTo(table, index+1);
//			System.out.println("moveLastColumnTo " + (index+1));
		}
		String targ = saveTarget.descriptor();
		String src = source.descriptor();
		final String msg = MappingUtil.createMsg(new_column_name, targ, src, ids, matched_ids, all_unique, non_unique,
				unique, min, max, many_to_one, forceSingle);

//		taskMonitor.showMessage(TaskMonitor.Level.INFO, msg);

//		putSpeciesProperty(saveSpecies.name());
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				JOptionPane.showMessageDialog(null, msg, "ID Mapping Result",
						(matched_ids.size() < 1 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE));
			}
		});

	}
	@Override
	public void listChanged(ListSelection<String> source) {
		speciesSelectionChanged(source);
	}
	@Override
	public void selectionChanged(ListSelection<String> source) {
		speciesSelectionChanged(source);

	}

//	 #3666
// 	private void moveLastColumnTo(CyTable table, int index) {
//		JTable nativeTable = table.
//		Collection<CyColumn> cols = table.getColumnModel();
//		if (cols instanceof ArrayList)
//		{
//			List<CyColumn> colList = (ArrayList<CyColumn>) cols;
//			CyColumn lastCol = colList.get(colList.size()-1);
//			colList.remove(lastCol);
//			colList.add(index, lastCol);
//		}
//	}

//	private int getColumnIndex(CyTable table, CyColumn column) {
//		Collection<CyColumn> cols = table.getColumns();
//		if (cols instanceof ArrayList)
//		{
//			List<CyColumn> colList = (ArrayList<CyColumn>) cols;
//			for (int i=0; i < colList.size(); i++)
//			{
//				CyColumn col = colList.get(i);
//				if (col == column)
//					return i;
//			}
//		}
//		return -1;
//	}
	public List<Class<?>> getResultClasses() {	return Arrays.asList(String.class, JSONResult.class);	}
	public Object getResults(Class requestedType) {
		if (requestedType.equals(String.class))			return new_column_name;
		if (requestedType.equals(JSONResult.class)) 
		{
			JSONResult res = () -> { if (new_column_name == null) 		return "{ }";
			return new_column_name;
		};
		return res;
		}
		return null;
		}

}