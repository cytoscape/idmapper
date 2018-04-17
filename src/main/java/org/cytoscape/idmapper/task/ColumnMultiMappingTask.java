//package org.cytoscape.idmapper.task;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Map;
//import java.util.Map.Entry;
//import java.util.Set;
//
//import javax.swing.JOptionPane;
//import javax.swing.SwingUtilities;
//
//import org.cytoscape.idmapper.IdMapping;
//import org.cytoscape.idmapper.MappingSource;
//import org.cytoscape.idmapper.Species;
//import org.cytoscape.idmapper.internal.BridgeDbIdMapper;
//import org.cytoscape.idmapper.internal.MappingUtil;
//import org.cytoscape.idmapper.task.ColumnMappingTask.MappingSourceListener;
//import org.cytoscape.model.CyColumn;
//import org.cytoscape.model.CyTable;
//import org.cytoscape.service.util.CyServiceRegistrar;
//import org.cytoscape.task.AbstractTableColumnTask;
//import org.cytoscape.work.ObservableTask;
//import org.cytoscape.work.ProvidesTitle;
//import org.cytoscape.work.TaskMonitor;
//import org.cytoscape.work.Tunable;
//import org.cytoscape.work.json.JSONResult;
//import org.cytoscape.work.undo.UndoSupport;
//import org.cytoscape.work.util.ListChangeListener;
//import org.cytoscape.work.util.ListMultipleSelection;
//import org.cytoscape.work.util.ListSelection;
//import org.cytoscape.work.util.ListSingleSelection;
//
//public class ColumnMultiMappingTask extends AbstractTableColumnTask
//								implements ListChangeListener<String>, ObservableTask {
//
//
////	public static final boolean DEBUG = true;
//	public static final boolean VERBOSE = true;
//	private static Species saveSpecies = Species.Human;
//	void saveSpeciesProperty(Species cur)	{		saveSpecies = cur;	}
//	private static List<MappingSource> saveSources = new ArrayList<MappingSource>();
//	private static List< MappingSource> saveTargets = new ArrayList<MappingSource>();
//	private  final CyServiceRegistrar registrar;
//
//	class MappingSourceListener implements  ListChangeListener<MappingSource>
//	{
//		MappingSourceListener()		{ }
//		@Override	public void listChanged(ListSelection<MappingSource> source) { mappingSourceChanged(source); }
//		@Override	public void selectionChanged(ListSelection<MappingSource> source) { mappingSourceChanged(source); }
//	}
//	public  	ColumnMultiMappingTask(final UndoSupport undoSupport, final CyColumn column, final CyServiceRegistrar reg) {
//		super(column);
//		registrar = reg;
//		saveSources.add(MappingSource.Entrez_Gene);
//		saveTargets.add(MappingSource.Entrez_Gene);
//		if (column.getType() == String.class)
//		{
//			Species.buildMaps();
//			speciesList.setPossibleValues(Species.fullNames());
//			speciesList.addListener(this);
//			mapFrom.addListener(new MappingSourceListener());
//			mapTo.addListener(new MappingSourceListener());
//			resetSpecies();
//		}
//	}
//	public void mappingSourceChanged(Object srcObject) 
//	{
//		if (srcObject instanceof ListMultipleSelection<?>)
//		{
//			ListMultipleSelection<MappingSource> multi = (ListMultipleSelection<MappingSource>)srcObject;
//			if (multi == mapFrom)		resetSource();
//			else if (multi == mapTo)
//			{}			//	System.out.println("target is: " + target_selection.getSelectedValue());
//		}
//	}
//	public void selectionChanged(Object srcObject) 
//	{
////		String name = null;
////		if (srcObject == mapFrom) name = "source_selection";
////		if (srcObject == mapTo) name = "target_selection";
////		System.out.println("selectionChanged: at " + name);
//		if (srcObject instanceof ListSingleSelection<?>)
//		{
//			ListSingleSelection<String> src = (ListSingleSelection<String>)srcObject;
//			if (src == speciesList)
//			{
//				String selected = speciesList.getSelectedValue();
//				saveSpecies = Species.lookup(selected);
////				name = "species_selection";
//				if (VERBOSE) System.out.println("setting Species to: " + saveSpecies.fullname());
//				resetSpecies();
//			}
//			else 
//			System.err.println("selectionChanged error: " + srcObject);
//		}
//	}
//		
//		
//	private void resetSpecies() {
//		speciesList.setSelectedValue(saveSpecies.fullname());
//		guessSource();
//	}	
//	
//	private void guessSource() {
//		
//		System.out.println("saveSpecies: " + saveSpecies);
//		List<MappingSource> strs = MappingSource.filteredStrings(saveSpecies, null);
//		if (VERBOSE) System.out.println("filteredStrings: " + strs);
//		mapFrom.setPossibleValues(strs);
//		if (VERBOSE) 
//			for (MappingSource src : saveSources)
//			System.out.println("\nsave Source: " + src);
//		
//		if (column != null)
//		{
//			final List<String> ids = column.getValues(String.class);
//			saveSources.add( MappingSource.guessSource(saveSpecies, ids));
//			for (MappingSource src : saveSources)
//			{
//				System.out.println("\nsave Source: " + src);
//			}
////			if (!saveSources.isEmpty())
////				mapFrom.setSelectedValues(saveSources);
//			if (VERBOSE) System.out.println("\nguessed Source: " + saveSources);
//			resetSource();
//		}		
//	}
//	private void resetSource() {
//		List<MappingSource> src = mapFrom.getSelectedValues();
//		saveSources = MappingSource.nameLookup(src);
//		if (VERBOSE) System.out.println("resettingSource: " + src + ", " + saveSources);
////		source_selection.setSelectedValue(saveSource.getMenuString());
//		resetTarget(saveSources);
//	}
//
////=========================================================================
//private void resetTarget(List<MappingSource> srcs)
//{
//	//	filter the targets to remove the source, and anything species-specific
//	List<MappingSource> saveTargets = MappingSource.nameLookup(mapTo.getSelectedValues());
//	if (srcs != null && mapTo.getPossibleValues().size() > 0)
//	{
//		if (saveTargets.isEmpty()) 
//			saveTargets.add(MappingSource.ENSEMBL);
//		List<MappingSource> filtered = MappingSource.filteredStringList(saveSpecies, srcs);
//		mapTo.setPossibleValues(filtered);
//		mapTo.setSelectedValues(saveTargets);
//	}
//
//}
//
//	//------------------------------------------------------------------------	
//	// look at AbstractCyNetworkReader:98 for an example of Tunables with methods
//	
//	@ProvidesTitle
//	public String getTitle() {		return "ID Mapping";	}
//	
//	@Tunable(description="Species", gravity=0.0, longDescription="The latin name of the species to which the identifiers apply",exampleStringValue = "Homo Sapiens")
//	public ListSingleSelection<String> speciesList  =  new ListSingleSelection<String>(Species.fullNames());
//
//	@Tunable(description="Map from", gravity=1.0, longDescription="Specifies the database describing the existing identifiers", exampleStringValue="ENSEMBL")
//	public ListMultipleSelection<MappingSource> mapFrom = new ListMultipleSelection<MappingSource>();
//
//	@Tunable(description="To", gravity=2.0, longDescription="Specifies the database identifiers to be looked up", exampleStringValue="Entrez")
//	public ListMultipleSelection<MappingSource> mapTo	= new ListMultipleSelection<MappingSource>();
//
//	@Tunable(description="Force single ", gravity=3.0, longDescription="When multiple identifiers can be mapped from a single term, this forces a singular result", exampleStringValue="false")
//	public boolean forceSingle = true;
//	
//	//------------------------------------------------------------------------
//	public String new_column_name = "";
//
//	@SuppressWarnings("rawtypes")
//	@Override
//	public void run(final TaskMonitor taskMonitor) {
//		String species = speciesList.getSelectedValue();
//		List<MappingSource> rawTargets = mapTo.getSelectedValues();
//		List<MappingSource> rawSources = mapFrom.getSelectedValues();
//		MappingSource rawTarget = rawTargets.size() > 0 ? rawTargets.get(0) : null;
//		MappingSource rawSource = rawSources.size() > 0 ? rawSources.get(0) : null;
////		List<String> sourceSystems = getSystems(rawSources);
//		final MappingSource source = rawSource; // MappingSource.nameLookup(rawSource);
//		if (column.getType() ==  Double.class || column.getType() ==  Integer.class || column.getType() ==  Boolean.class)
//		{
//			if (VERBOSE) System.out.println("Can't map a numeric column as identifiers");		// tell the user?
//			return;
//		}
//		if (VERBOSE) System.out.println("raw str: " + rawTarget);
//		saveTargets = rawTargets; // MappingSource.nameLookup(rawTargets);
//		if (VERBOSE) System.out.println("reading target as " + saveTargets);
//		saveSpecies = Species.lookup(species);
//		if (VERBOSE) System.out.println("saving species as " + saveSpecies.name());
//		boolean smartSticky = true;
//		if (smartSticky)
//		{
//			
//		}
//		boolean source_is_list = false;
//		if (column.getType() == List.class)
//			source_is_list = true;
//
//		final List values = column.getValues(column.getType());
//
//		final List<String> ids = new ArrayList<String>();
//		for (final Object v : values) {
//			// System.out.println(v);
//			if (v != null) {
//				if (source_is_list) {
//					for (final Object lv : (List) v)
//						MappingUtil.addCleanedStrValueToList(ids, lv);
//				} else
//					MappingUtil.addCleanedStrValueToList(ids, v);
//			}
//		}
//
//		final Set<String> matched_ids;
//		final Set<String> unmatched_ids;
//		final Map<String, IdMapping> res;
//		try {
//			final BridgeDbIdMapper map = new BridgeDbIdMapper();
//			res = map.mapList(ids, rawSources, rawTargets, saveSpecies.name(), saveSpecies.name());
//			matched_ids = map.getMatchedIds();
//			unmatched_ids = map.getUnmatchedIds();
//		} catch (final Exception e) {
//			SwingUtilities.invokeLater(new Runnable() {
//				@Override
//				public void run() {
//					JOptionPane.showMessageDialog(null, e.getMessage(), "ID Mapping Error", JOptionPane.ERROR_MESSAGE);
//				}
//			});
//			return;
//		}
//
//		if (VERBOSE) {
//			System.out.println();
//			System.out.println("Unmatched:");
//			if (unmatched_ids != null) 
//				for (final String u : unmatched_ids)  System.out.println(u);
//			
//			System.out.println();
//			System.out.println("Matched:");
//			if (matched_ids != null) 
//				for (final String u : matched_ids) System.out.println(u);
//				
//			System.out.println();
//		}
//		for (MappingSource t : saveTargets)
//		{
//			new_column_name = t.descriptor();
//			new_column_name = MappingUtil.makeNewColumnName(new_column_name,
//					source.descriptor(), new_column_name, column);
//			boolean all_unique = true;
//			int non_unique = 0;
//			int unique = 0;
//			int min = Integer.MAX_VALUE;
//			int max = 0;
//	
//			if (res == null) continue;
//			for (final Entry<String, IdMapping> entry : res.entrySet()) {
//				final Set<String> v = entry.getValue().getTargetIds();
//				if (v != null) {
//					if (v.size() > 1) {
//						all_unique = false;
//						++non_unique;
//						if (v.size() > max)		max = v.size();
//						if (v.size() < min)		min = v.size();
//					} else
//						++unique;
//				}
//			}
//
//		final CyTable table = column.getTable();
//		
//// TODO -- #3666 add the new column after the original, not at end of table
////		int index = getColumnIndex(table, column);
////		System.out.println("Index = " + index);
//		
//		
//		boolean many_to_one = false;
//		if (matched_ids.size() > 0) {
//			boolean all_single = false;
//			if (forceSingle) 
//				table.createColumn(new_column_name, String.class, false);	//index, 
//			else {  
//				all_single = MappingUtil.isAllSingle(source_is_list, res, column, table);
//				if (all_single) 
//					table.createColumn(new_column_name, String.class, false);		//index, 
//				 else 
//					table.createListColumn(new_column_name, String.class, false);	//index, 
//			}
//			many_to_one = MappingUtil.fillNewColumn(source_is_list, res, table, column, new_column_name,
//					forceSingle || all_single);
//
////			moveLastColumnTo(table, index+1);
////			System.out.println("moveLastColumnTo " + (index+1));
//		}
//		
//		
//		String target = saveTargets.get(0).descriptor();
//		String src = source.descriptor();
//		final String msg = MappingUtil.createMsg(new_column_name, target, src, ids, matched_ids, all_unique, non_unique,
//				unique, min, max, many_to_one, forceSingle);
//
//		SwingUtilities.invokeLater(new Runnable() {
//			@Override
//			public void run() {
//				JOptionPane.showMessageDialog(null, msg, "ID Mapping Result",
//						(matched_ids.size() < 1 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE));
//			}
//		});
//		}
//	}
//
//	public List<Class<?>> getResultClasses() {	return Arrays.asList(String.class, JSONResult.class);	}
//	public Object getResults(Class requestedType) {
//		if (requestedType.equals(String.class))			return new_column_name;
//		if (requestedType.equals(JSONResult.class)) 
//		{
//			JSONResult res = () -> { if (new_column_name == null) 		return "{ }";
//				return new_column_name;
//				};
//			return res;
//		}
//		return null;
//	}
//
//	@Override
//	public void selectionChanged(ListSelection<String> source) {
//		// TODO Auto-generated method stub
//		
//	}
//	@Override
//	public void listChanged(ListSelection<String> source) {
//		// TODO Auto-generated method stub
//		
//	}
//
//}
