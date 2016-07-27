package org.cytoscape.idmap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyTable;
import org.cytoscape.task.AbstractTableColumnTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.undo.UndoSupport;
import org.cytoscape.work.util.ListSingleSelection;

/**
 * A column task which uses the Id Mapping service BridgeDB for mapping.
 *
 *
 * @author cmzmasek AST - tunables reordered, prompts changed
 *
 *         List of source databases can be found here:
 * 
 *         //https://github.com/bridgedb/BridgeDb/blob/master/org.bridgedb.bio/
 *         resources/org/bridgedb/bio/datasources.txt
 *
 */
public class MapColumnTaskBridgeDb extends AbstractTableColumnTask {

	public static final boolean DEBUG = true;
	public static final boolean VERBOSE = true;

	MapColumnTaskBridgeDb(final UndoSupport undoSupport, final CyColumn column) {
		super(column);
	}

	@ProvidesTitle
	public String getTitle() {
		return "ID Mapping";
	}

	@Tunable(description = "Species")
	public ListSingleSelection<String> species_selection = new ListSingleSelection<String>(BridgeDbIdMapper.Human,
			BridgeDbIdMapper.Mouse, BridgeDbIdMapper.Rat, BridgeDbIdMapper.Frog, BridgeDbIdMapper.Zebra_fish,
			BridgeDbIdMapper.Fruit_fly, BridgeDbIdMapper.Mosquito, BridgeDbIdMapper.Worm,
			BridgeDbIdMapper.Arabidopsis_thaliana, BridgeDbIdMapper.Yeast, BridgeDbIdMapper.Escherichia_coli,
			BridgeDbIdMapper.Tuberculosis);

	@Tunable(description = "Map from")
	public ListSingleSelection<String> source_selection = new ListSingleSelection<String>(BridgeDbIdMapper.ENSEMBL,
			BridgeDbIdMapper.EMBL, BridgeDbIdMapper.Entrez_Gene, BridgeDbIdMapper.Gene_ID, BridgeDbIdMapper.GO,
			BridgeDbIdMapper.GenBank, BridgeDbIdMapper.Illumina, BridgeDbIdMapper.InterPro, BridgeDbIdMapper.MGI,
			BridgeDbIdMapper.PDB, BridgeDbIdMapper.RefSeq, BridgeDbIdMapper.UniGene, BridgeDbIdMapper.UNIPROT,
			BridgeDbIdMapper.Uniprot_TrEMBL, BridgeDbIdMapper.UCSC_Genome_Browser);

	@Tunable(description = "To")
	public ListSingleSelection<String> target_selection = new ListSingleSelection<String>(BridgeDbIdMapper.ENSEMBL,
			BridgeDbIdMapper.EMBL, BridgeDbIdMapper.Entrez_Gene, BridgeDbIdMapper.Gene_ID, BridgeDbIdMapper.GO,
			BridgeDbIdMapper.GenBank, BridgeDbIdMapper.Illumina, BridgeDbIdMapper.InterPro, BridgeDbIdMapper.MGI,
			BridgeDbIdMapper.PDB, BridgeDbIdMapper.RefSeq, BridgeDbIdMapper.UniGene, BridgeDbIdMapper.UNIPROT,
			BridgeDbIdMapper.Uniprot_TrEMBL, BridgeDbIdMapper.UCSC_Genome_Browser);
//
//	@Tunable(description = "New column name")
	public String new_column_name = "";

	@Tunable(description = "Force single ")
	public boolean only_use_one = true;

	@SuppressWarnings("rawtypes")
	@Override
	public void run(final TaskMonitor taskMonitor) {
		final String target = BridgeDbIdMapper.LONG_TO_SHORT.get(target_selection.getSelectedValue());
		final String source = BridgeDbIdMapper.LONG_TO_SHORT.get(source_selection.getSelectedValue());
		final String species = species_selection.getSelectedValue();

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
			res = map.map(ids, source, target, species, species);
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
		new_column_name = BridgeDbIdMapper.SHORT_TO_LONG.get(target);
		new_column_name = MappingUtil.makeNewColumnName(new_column_name,
				BridgeDbIdMapper.SHORT_TO_LONG.get(source), new_column_name, column);

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
						if (v.size() > max)
							max = v.size();
						if (v.size() < min)
							min = v.size();
					} else
						++unique;
				}
			}

		final CyTable table = column.getTable();
		Collection<CyColumn> cols = table.getColumns();
		System.out.println("Indexing " + cols.toString());
		int index = -1;
		if (cols instanceof ArrayList)
		{
			System.out.println("Indexing " + column.getName());
			index = ((ArrayList) cols).indexOf(column.getName());
		}
		System.out.println("Index = " + index);
		
		boolean many_to_one = false;
		if (matched_ids.size() > 0) {
			boolean all_single = false;
			if (only_use_one) {
				table.createColumn(new_column_name, String.class, false);
			} else {
				all_single = MappingUtil.isAllSingle(source_is_list, res, column, table);
				if (all_single) {
					table.createColumn(new_column_name, String.class, false);
				} else {
					table.createListColumn(new_column_name, String.class, false);
				}
			}
			many_to_one = MappingUtil.fillNewColumn(source_is_list, res, table, column, new_column_name,
					only_use_one || all_single);
		}
		String targ = BridgeDbIdMapper.SHORT_TO_LONG.get(target);
		String src = BridgeDbIdMapper.SHORT_TO_LONG.get(source);
		final String msg = MappingUtil.createMsg(new_column_name, targ, src, ids, matched_ids, all_unique, non_unique,
				unique, min, max, many_to_one);

		taskMonitor.showMessage(TaskMonitor.Level.INFO, msg);

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				JOptionPane.showMessageDialog(null, msg, "ID Mapping Result",
						(matched_ids.size() < 1 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE));
			}
		});

	}

}
