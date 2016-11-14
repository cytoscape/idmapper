package org.cytoscape.idmapper.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.cytoscape.idmapper.IdMapping;
import org.cytoscape.idmapper.internal.BridgeDbIdMapper;
import org.cytoscape.idmapper.internal.MappingSource;
import org.cytoscape.idmapper.internal.MappingUtil;
import org.cytoscape.idmapper.internal.Species;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyTable;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.AbstractTableColumnTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.undo.UndoSupport;
import org.cytoscape.work.util.ListChangeListener;
import org.cytoscape.work.util.ListSelection;
import org.cytoscape.work.util.ListSingleSelection;

public class ColumnMappingTask extends AbstractTableColumnTask 
								implements ListChangeListener<String> {


//	public static final boolean DEBUG = true;
	public static final boolean VERBOSE = false;
	private static Species saveSpecies = Species.Human;
	private static MappingSource saveSource = MappingSource.Entrez_Gene;
	private static MappingSource saveTarget = MappingSource.Entrez_Gene;
//	private  final CyServiceRegistrar registrar;
	ColumnMappingTask(final UndoSupport undoSupport, final CyColumn column, final CyServiceRegistrar reg) {
		super(column);
//		registrar = reg;
		Species.buildMaps();
		speciesList.addListener(this);
		source_selection.addListener(this);
		target_selection.addListener(this);
		resetSpecies();
	}
	public void selectionChanged(ListSelection<String> source) 
	{
		String name = null;
		if (source == speciesList) name = "speciesList";
		if (source == source_selection) name = "source_selection";
		if (source == target_selection) name = "target_selection";
//		System.out.println("selectionChanged: at " + name);
		if (source instanceof ListSingleSelection<?>)
		{
			ListSingleSelection<String> src = (ListSingleSelection<String>)source;
			if (src == speciesList)
				resetSpecies();
			else if (src == source_selection)
				resetSource();
			else if (src == target_selection)
			{}//	System.out.println("target is: " + target_selection.getSelectedValue());
			else
			System.err.println("selectionChanged error: " + source.toString());
		}
	}
		
		
	private void resetSpecies() {
		String selected = speciesList.getSelectedValue();
		saveSpecies = Species.lookup(selected);
//		System.out.println("resetSpecies: " + saveSpecies.name());
		speciesList.setPossibleValues(Species.fullNames());
		if (!selected.equals(saveSpecies.fullname()))
			speciesList.setSelectedValue(saveSpecies.fullname());
		guessSource();
	}	
	
	private void guessSource() {
		source_selection.setPossibleValues(MappingSource.filteredStrings(saveSpecies, null));
		if (column != null)
		{
			final List<String> ids = column.getValues(String.class);
			saveSource = MappingSource.guessSource(saveSpecies, ids);
			source_selection.setSelectedValue(saveSource.getMenuString());
			System.out.println("\nguessed Source: " + saveSource.getMenuString());
			resetSource();
		}		
	}
	private void resetSource() {
		String src = source_selection.getSelectedValue();
		saveSource = MappingSource.nameLookup(src);
//		System.out.println("resetSource: " + src + ", " + saveSource.descriptor());
//		source_selection.setSelectedValue(saveSource.getMenuString());
		resetTarget(saveSource);
	}

//=========================================================================
private void resetTarget(MappingSource src)
{
	//	filter the targets to remove the source, and anything species-specific
	saveTarget = MappingSource.nameLookup(target_selection.getSelectedValue());
	if ((saveTarget == null || saveTarget == src) && target_selection.getPossibleValues().size() > 0)
		saveTarget = MappingSource.nameLookup(target_selection.getPossibleValues().get(0));
	if (saveTarget == null) 
		saveTarget = MappingSource.ENSEMBL;
	//	System.out.println("resetTarget: " + saveTarget.descriptor());
	List<String> filtered = MappingSource.filteredStrings(saveSpecies, src);
	target_selection.setPossibleValues(filtered);
	target_selection.setSelectedValue(saveTarget.getMenuString());

//	System.out.println("---------------\n" );
}

	//------------------------------------------------------------------------	
	void saveSpeciesProperty(Species cur)	{		saveSpecies = cur;	}
	
	@ProvidesTitle
	public String getTitle() {		return "ID Mapping";	}
	
	
	// look at AbstractCyNetworkReader:98 for an example of Tunables with methods
	@Tunable(description="Species", gravity=0.0)
	public ListSingleSelection<String> speciesList  =  new ListSingleSelection<String>(Species.fullNames());;
//	public ListSingleSelection<String> getspecies_selection()
//	{
//		if (speciesList == null)
//			speciesList =  new ListSingleSelection<String>(Species.fullNames());
//		return speciesList;
//	}
//
//	public void setspecies_selection(ListSingleSelection<String> list)
//	{
//		String cur = speciesList.getSelectedValue();
////		System.out.println("setSpecies_selection " + cur);
//		int idx = cur.indexOf(" (");
//		if (idx >0)
//			cur = cur.substring(0, idx);
//		saveSpecies = Species.lookup(cur);
//		System.out.println("Species saved as " + saveSpecies.toString());
//		resetInterfaceToSpecies();
//	}
	//------------------------------------------------------------------------
	@Tunable(description="Map from", gravity=1.0)
//	public ListSingleSelection<String> source_selection = new ListSingleSelection<String>(MappingSource.allStrings());
	public ListSingleSelection<String> source_selection = new ListSingleSelection<String>();

	@Tunable(description="To", gravity=2.0)
//	public ListSingleSelection<String> target_selection	= new ListSingleSelection<String>(MappingSource.allStrings());
	public ListSingleSelection<String> target_selection	= new ListSingleSelection<String>();

	@Tunable(description="Force single ", gravity=3.0)
	public boolean only_use_one = true;
	
	//------------------------------------------------------------------------
	public String new_column_name = "";

	@SuppressWarnings("rawtypes")
	@Override
	public void run(final TaskMonitor taskMonitor) {
		String species = speciesList.getSelectedValue();
		String rawTarget = target_selection.getSelectedValue();
		String rawSource = source_selection.getSelectedValue();
		final MappingSource source = MappingSource.nameLookup(rawSource);
//		System.out.println("prior target was " + saveTarget);
//		System.out.println("raw str: " + rawTarget);
		saveTarget = MappingSource.nameLookup(rawTarget);
//		System.out.println("reading target as " + saveTarget);
		saveSpecies = Species.lookup(species);
//		System.out.println("saving species as " + saveSpecies.name());
//		System.out.println("saving source as " + source.name());
//		System.out.println("saving target as " + saveTarget.name());
//		System.out.println("--------------------------");
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

		final CyTable table = column.getTable();
		
// TODO -- #3666 add the new column after the original, not at end of table
//		int index = getColumnIndex(table, column);
//		System.out.println("Index = " + index);
		
		
		boolean many_to_one = false;
		if (matched_ids.size() > 0) {
			boolean all_single = false;
			if (only_use_one) 
				table.createColumn(new_column_name, String.class, false);	//index, 
			else {  
				all_single = MappingUtil.isAllSingle(source_is_list, res, column, table);
				if (all_single) 
					table.createColumn(new_column_name, String.class, false);		//index, 
				 else 
					table.createListColumn(new_column_name, String.class, false);	//index, 
			}
			many_to_one = MappingUtil.fillNewColumn(source_is_list, res, table, column, new_column_name,
					only_use_one || all_single);

//			moveLastColumnTo(table, index+1);
//			System.out.println("moveLastColumnTo " + (index+1));
		}
		String targ = saveTarget.descriptor();
		String src = source.descriptor();
		final String msg = MappingUtil.createMsg(new_column_name, targ, src, ids, matched_ids, all_unique, non_unique,
				unique, min, max, many_to_one, only_use_one);

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
		// TODO Auto-generated method stub
		
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

}
