package org.cytoscape.idmapper.normalization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 
 * Normalizes Curies and prefixes according to the CURIE specification
 * (https://www.w3.org/TR/curie/).
 */
public final class CurieNormalizer {

    private static final Pattern CURIE_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_.-]*:.+");
    private static final Pattern PREFIX_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_.-]*$");

    private CurieNormalizer() {
    }

    /**
     * Checks if value has a CURIE prefix. A CURIE prefix is defined as a string
     * that starts
     * with a valid prefix followed by a colon and then some value.
     * 
     * @param value
     * @return
     */
    public static boolean hasCuriePrefix(final String value) {
        return value != null && CURIE_PATTERN.matcher(value.trim()).matches();
    }

    /**
     * Normalizes a CURIE prefix by trimming whitespace and removing any trailing
     * colon.
     * 
     * @param prefix
     * @throws IllegalArgumentException if the prefix is invalid
     * @return
     */
    public static String normalizePrefix(final String prefix) {
        if (prefix == null)
            return null;

        String normalized = prefix.trim();
        if (normalized.isEmpty())
            return null;
        if (normalized.endsWith(":"))
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        if (normalized.isEmpty() || !PREFIX_PATTERN.matcher(normalized).matches())
            throw new IllegalArgumentException("Invalid CURIE prefix: " + prefix);
        return normalized;
    }

    /**
     * Given a value and a normalized prefix, return the value as a CURIE.
     * If the value already has a CURIE prefix, it is returned unchanged.
     * If the value is null or empty, null is returned.
     * If the normalized prefix is null, the value is returned unchanged.
     * 
     * @param value
     * @param normalizedPrefix
     * @return
     */
    public static String toCurie(final String value, final String normalizedPrefix) {
        if (value == null)
            return null;

        final String trimmed = value.trim();
        if (trimmed.isEmpty())
            return null;
        if (hasCuriePrefix(trimmed))
            return trimmed;

        if (normalizedPrefix == null)
            return trimmed;
        return normalizedPrefix + ":" + trimmed;
    }

    /**
     * Splits a list of identifiers into batches of the specified size.
     * 
     * If the identifiers list is null or empty, an empty list is returned.
     * 
     * @param identifiers
     * @param batchSize
     * @throws IllegalArgumentException if batchSize is less than or equal to zero
     * @return
     */
    public static List<List<String>> splitBatches(final List<String> identifiers, final int batchSize) {
        if (batchSize <= 0)
            throw new IllegalArgumentException("Batch size must be a positive integer");
        if (identifiers == null || identifiers.isEmpty())
            return Collections.emptyList();

        final List<List<String>> batches = new ArrayList<List<String>>();
        for (int i = 0; i < identifiers.size(); i += batchSize) {
            batches.add(new ArrayList<String>(identifiers.subList(i, Math.min(i + batchSize, identifiers.size()))));
        }
        return batches;
    }

    /**
     * Prepares a list of identifiers for normalization by creating a mapping of
     * CURIEs to the rows they appear in.
     * If the list of values is null or empty, an empty mapping is returned.
     * If the normalized prefix is null, the values are treated as-is without adding
     * a prefix.
     * 
     * @param values
     * @param normalizedPrefix
     * @return
     */
    public static PreparedIdentifiers prepare(final List<String> values, final String normalizedPrefix) {
        final Map<String, List<Integer>> rowsByCurie = new LinkedHashMap<String, List<Integer>>();
        final Set<String> uniqueCuries = new LinkedHashSet<String>();
        int rowsExamined = 0;

        if (values != null) {
            for (int i = 0; i < values.size(); i++) {
                rowsExamined++;
                final String curie = toCurie(values.get(i), normalizedPrefix);
                if (curie == null)
                    continue;

                uniqueCuries.add(curie);
                List<Integer> rows = rowsByCurie.get(curie);
                if (rows == null) {
                    rows = new ArrayList<Integer>();
                    rowsByCurie.put(curie, rows);
                }
                rows.add(i);
            }
        }

        return new PreparedIdentifiers(rowsExamined, new ArrayList<String>(uniqueCuries), rowsByCurie);
    }

    /**
     * Represents the result of preparing a list of identifiers for normalization.
     * Contains the number of rows examined, the list of unique CURIEs, and a
     * mapping
     * of CURIEs to the rows they appear in.
     */
    public static final class PreparedIdentifiers {
        private final int rowsExamined;
        private final List<String> uniqueCuries;
        private final Map<String, List<Integer>> rowsByCurie;

        /**
         * Creates a new PreparedIdentifiers instance.
         * 
         * @param rowsExamined
         * @param uniqueCuries
         * @param rowsByCurie
         */
        PreparedIdentifiers(final int rowsExamined, final List<String> uniqueCuries,
                final Map<String, List<Integer>> rowsByCurie) {
            this.rowsExamined = rowsExamined;
            this.uniqueCuries = uniqueCuries;
            this.rowsByCurie = rowsByCurie;
        }

        /**
         * Returns the number of rows examined during preparation.
         * 
         * @return
         */
        public int getRowsExamined() {
            return rowsExamined;
        }

        /**
         * Returns the list of unique CURIEs found during preparation.
         * 
         * @return
         */
        public List<String> getUniqueCuries() {
            return uniqueCuries;
        }

        /**
         * Returns a mapping of CURIEs to the rows they appear in.
         * 
         * @return
         */
        public Map<String, List<Integer>> getRowsByCurie() {
            return rowsByCurie;
        }
    }
}
