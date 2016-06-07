package org.cytoscape.task.internal.table;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;

public final class MappingUtil {

    public final static void addCleanedStrValueToList(final List<String> ids, final Object v) {
        if ((ids != null) && (v != null)) {
            String v_str = (String) v;
            if (v_str != null) {
                v_str = v_str.trim();
                if (v_str.length() > 0) {
                    ids.add(v_str);
                }
            }
        }
    }

    public final static boolean fillNewColumn(final boolean source_is_list,
                                              final Map<String, IdMapping> matched_ids,
                                              final CyTable table,
                                              final CyColumn column,
                                              final String new_column_name,
                                              final boolean single) {
        final List<CyRow> rows = table.getAllRows();
        boolean many_to_one = false;
        if (source_is_list) {
            for (final CyRow row : rows) {
                final List in_vals = (List) row.get(column.getName(),
                                                    column.getType());
                if (in_vals != null) {
                    final TreeSet<String> ts = new TreeSet<String>();
                    for (final Object iv : in_vals) {
                        final String in_val = (String) iv;
                        if ((in_val != null) && (in_val.length() > 0)) {
                            if (matched_ids.containsKey(in_val)) {
                                final Set<String> matched = matched_ids.get(in_val).getTargetIds();
                                if (!matched.isEmpty()) {
                                    for (final String m : matched) {
                                        if ((m != null) && (m.length() > 0)) {
                                            if (ts.contains(m)) {
                                                many_to_one = true;
                                            }
                                            else {
                                                ts.add(m);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    final List<String> l = new ArrayList<String>(ts);
                    if (!l.isEmpty()) {
                        if (single) {
                            row.set(new_column_name,
                                    l.get(0));
                        }
                        else {
                            row.set(new_column_name,
                                    l);
                        }
                    }
                }
            }
        }
        else {
            for (final CyRow row : rows) {
                final String in_val = (String) row.get(column.getName(),
                                                       column.getType());
                if ((in_val != null) && (in_val.length() > 0)) {
                    if (matched_ids.containsKey(in_val)) {
                        final Set<String> matched = matched_ids.get(in_val).getTargetIds();
                        if (!matched.isEmpty()) {
                            if (single) {
                                row.set(new_column_name,
                                        matched.iterator().next());
                            }
                            else {
                                final TreeSet<String> ts = new TreeSet<String>();
                                for (final String m : matched) {
                                    if ((m != null) && (m.length() > 0)) {
                                        if (ts.contains(m)) {
                                            many_to_one = true;
                                        }
                                        else {
                                            ts.add(m);
                                        }
                                    }
                                }
                                final List<String> l = new ArrayList<String>(ts);
                                row.set(new_column_name,
                                        l);
                            }
                        }
                    }
                }
            }
        }
        return many_to_one;
    }

    public final static boolean isAllSingle(final boolean source_is_list, final Map<String, IdMapping> matched_ids, final CyColumn column, final CyTable table) {
        final List<CyRow> rows = table.getAllRows();
        final ArrayList<Set<String>> list = new ArrayList<Set<String>>();
        if (source_is_list) {

            for (final CyRow row : rows) {

                final List in_vals = (List) row.get(column.getName(),
                                                    column.getType());
                if (in_vals != null) {
                    final TreeSet<String> ts = new TreeSet<String>();
                    for (final Object iv : in_vals) {
                        final String in_val = (String) iv;
                        if ((in_val != null) && (in_val.length() > 0)) {
                            if (matched_ids.containsKey(in_val)) {
                                final IdMapping matched = matched_ids.get(in_val);
                                if (!matched.getTargetIds().isEmpty()) {
                                    for (final String m : matched.getTargetIds()) {
                                        if ((m != null) && (m.length() > 0)) {

                                            ts.add(m);

                                        }
                                    }
                                }
                            }
                        }
                    }

                    list.add(ts);
                }
            }
        }
        else {
            for (final CyRow row : rows) {
                final String in_val = (String) row.get(column.getName(),
                                                       column.getType());
                if ((in_val != null) && (in_val.length() > 0)) {
                    if (matched_ids.containsKey(in_val)) {
                        final Set<String> matched = matched_ids.get(in_val).getTargetIds();
                        if (!matched.isEmpty()) {
                            // if (single) {
                            // row.set(new_column_name, matched.iterator()
                            // .next());
                            // }
                            // else {
                            final TreeSet<String> ts = new TreeSet<String>();
                            for (final String m : matched) {
                                if ((m != null) && (m.length() > 0)) {

                                    ts.add(m);

                                }
                            }
                            final List<String> l = new ArrayList<String>(ts);
                            list.add(ts);
                            // }
                        }
                    }
                }
            }
        }
        boolean all_single = true;
        for (final Set<String> set : list) {
            if (set.size() > 1) {
                all_single = false;
                break;
            }
        }
        return all_single;
    }

    public final static String validateNewColumnName(final String target, final String source, final String new_column_name, final CyColumn column) {

        final String my_target = BridgeDbIdMapper.SHORT_TO_LONG.get(target);
        final String my_source = BridgeDbIdMapper.SHORT_TO_LONG.get(source);
        String my_col_name;
        if ((new_column_name == null) || (new_column_name.trim().length() < 1)) {
            my_col_name = column.getName() + ": " + my_source + "->" + my_target;
        }
        else {
            my_col_name = new_column_name.trim();
        }
        final CyTable table = column.getTable();
        if (table.getColumn(new_column_name) != null) {
            int counter = 1;
            String new_new_column_name = new_column_name + " (" + counter + ")";
            while (table.getColumn(new_new_column_name) != null) {
                ++counter;
                new_new_column_name = new_column_name + " (" + counter + ")";
            }
            my_col_name = new_new_column_name;
        }
        return my_col_name;
    }

    public final static String createMsg(final String new_column_name,
                                         final String target,
                                         final String source,
                                         final List<String> ids,
                                         final Set<String> matched_ids,
                                         final boolean all_unique,
                                         final int non_unique,
                                         final int unique,
                                         final int min,
                                         final int max,
                                         final boolean many_to_one) {
        final String msg;

        if (matched_ids.size() < 1) {
            msg = "Failed to map any identifier" + "\n" + "Total identifiers: " + ids.size() + "\n" + "Source type: "
                    + BridgeDbIdMapper.SHORT_TO_LONG.get(source) + "\n" + "Target type: " + BridgeDbIdMapper.SHORT_TO_LONG.get(target);
        }
        else {
            final String o2o;

            if (all_unique) {
                o2o = "All mappings one-to-one" + "\n";
            }
            else {
                o2o = "Not all mappings one-to-one:" + "\n" + "  one-to-one: " + unique + "\n" + "  one-to-many: " + non_unique + " (range: " + min + "-" + max
                        + ")" + "\n";
            }

            final String m2o;
            if (many_to_one) {
                m2o = "Same/all mappings many-to-one" + "\n";
            }
            else {
                m2o = "";

            }

            msg = "Successfully mapped identifiers: " + matched_ids.size() + "\n" + "Total source identifiers: " + ids.size() + "\n" + o2o + m2o
                    + "Source type: " + BridgeDbIdMapper.SHORT_TO_LONG.get(source) + "\n" + "Target type: " + BridgeDbIdMapper.SHORT_TO_LONG.get(target) + "\n"
                    + "New column: " + new_column_name;
        }
        return msg;
    }

}
