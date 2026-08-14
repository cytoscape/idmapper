package org.cytoscape.idmapper.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.cytoscape.idmapper.normalization.ApacheNodeNormalizationClient;
import org.cytoscape.idmapper.normalization.CurieCandidatePlan;
import org.cytoscape.idmapper.normalization.CurieCandidatePlanner;
import org.cytoscape.idmapper.normalization.CuriePrefix;
import org.cytoscape.idmapper.normalization.CuriePrefixCatalog;
import org.cytoscape.idmapper.normalization.CuriePrefixValue;
import org.cytoscape.idmapper.normalization.CurieNormalizer;
import org.cytoscape.idmapper.normalization.IdentifierFormatClassifier;
import org.cytoscape.idmapper.normalization.NodeNormalizationClient;
import org.cytoscape.idmapper.normalization.NodeNormalizationException;
import org.cytoscape.idmapper.normalization.NodeNormalizationProperties;
import org.cytoscape.idmapper.normalization.NodeNormalizationResult;
import org.cytoscape.idmapper.normalization.PreparedIdentifier;
import org.cytoscape.idmapper.normalization.NormalizeIdentifiersSummary;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.task.AbstractTableColumnTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.json.JSONResult;

public class NormalizeIdentifiersTask extends AbstractTableColumnTask implements ObservableTask {

    private final Properties nodeNormalizationProperties;
    private final NodeNormalizationClient injectedClient;
    private final CuriePrefixCatalog curiePrefixCatalog;
    private NormalizeIdentifiersSummary summary = new NormalizeIdentifiersSummary();

    @Tunable(description = "Source column", gravity = 0.0, longDescription = "Column containing identifiers to normalize")
    public String sourceColumnName;

    @CuriePrefix
    @Tunable(description = "CURIE prefix", gravity = 1.0, longDescription = "Optional CURIE prefix to apply to values without one", exampleStringValue = "HGNC")
    public CuriePrefixValue prefix = new CuriePrefixValue();

    @Tunable(description = "Output column name", gravity = 2.0, longDescription = "Column where canonical identifiers will be written")
    public String outputColumnName;

    @Tunable(description = "Batch size", gravity = 3.0, longDescription = "Number of unique identifiers sent in each request")
    public int batchSize;

    @Tunable(description = "Guess CURIE prefix for unprefixed values", gravity = 4.0, longDescription = "Try suggested prefixes for values that do not already have a CURIE prefix")
    public boolean guessPrefix = false;

    @Tunable(description = "Maximum prefix guesses", gravity = 5.0, longDescription = "Maximum number of guessed prefixes to try for each unprefixed value")
    public int maxPrefixGuesses;

    @Tunable(description = "Service endpoint", gravity = 6.0, longDescription = "Node Normalization base URL")
    public String serviceUrl;

    @Tunable(description = "Overwrite existing output column", gravity = 7.0, longDescription = "Allow reuse of an existing String output column")
    public boolean overwrite = false;

    public NormalizeIdentifiersTask(final CyColumn column, final Properties nodeNormalizationProperties) {
        this(column, nodeNormalizationProperties, null, null);
    }

    public NormalizeIdentifiersTask(final CyColumn column, final Properties nodeNormalizationProperties,
            final NodeNormalizationClient injectedClient, final CuriePrefixCatalog curiePrefixCatalog) {
        super(column);
        this.nodeNormalizationProperties = nodeNormalizationProperties;
        this.injectedClient = injectedClient;
        this.curiePrefixCatalog = curiePrefixCatalog;
        if (column != null) {
            sourceColumnName = column.getName();
            outputColumnName = defaultOutputColumnName(column.getName());
        }
        batchSize = NodeNormalizationProperties.getBatchSize(nodeNormalizationProperties);
        maxPrefixGuesses = NodeNormalizationProperties.getMaxPrefixGuesses(nodeNormalizationProperties);
        serviceUrl = NodeNormalizationProperties.getBaseUrl(nodeNormalizationProperties);
    }

    @ProvidesTitle
    public String getTitle() {
        return "Normalize Identifiers";
    }

    @Override
    public void run(final TaskMonitor taskMonitor) throws Exception {
        taskMonitor.setTitle("Normalize Identifiers");
        summary = normalize(column.getTable(), sourceColumnName, prefix.getPrefix(), outputColumnName, batchSize,
                serviceUrl, overwrite, guessPrefix, maxPrefixGuesses,
                NodeNormalizationProperties.getUseIdentifierFormatFilters(nodeNormalizationProperties),
                nodeNormalizationProperties, injectedClient, curiePrefixCatalog, this, taskMonitor);
    }

    static NormalizeIdentifiersSummary normalize(final CyTable table, final String sourceColumnName,
            final String prefix,
            final String outputColumnName, final int batchSize, final String serviceUrl, final boolean overwrite,
            final boolean guessPrefix, final int maxPrefixGuesses, final boolean useIdentifierFormatFilters,
            final Properties properties, final NodeNormalizationClient injectedClient,
            final CuriePrefixCatalog curiePrefixCatalog,
            final NormalizeIdentifiersTask task,
            final TaskMonitor taskMonitor) throws Exception {
        final NormalizeIdentifiersSummary summary = new NormalizeIdentifiersSummary();
        final String cleanOutputColumn = validateOutputColumnName(outputColumnName);
        summary.setOutputColumnName(cleanOutputColumn);

        if (table == null)
            throw new IllegalArgumentException("Target table is not available");
        if (sourceColumnName == null || sourceColumnName.trim().isEmpty())
            throw new IllegalArgumentException("Source column name is required");
        if (batchSize <= 0)
            throw new IllegalArgumentException("Batch size must be a positive integer");
        if (maxPrefixGuesses <= 0)
            throw new IllegalArgumentException("Maximum prefix guesses must be a positive integer");

        String normalizedPrefix = CurieNormalizer.normalizePrefix(prefix);

        final CyColumn sourceColumn = table.getColumn(sourceColumnName.trim());
        if (sourceColumn == null)
            throw new IllegalArgumentException("Column not found: " + sourceColumnName);
        validateSourceColumn(sourceColumn);
        validateOutputColumn(table, cleanOutputColumn, overwrite);

        final List<CyRow> rows = table.getAllRows();
        final List<String> sourceValues = new ArrayList<String>();
        for (final CyRow row : rows)
            sourceValues.add(row.get(sourceColumn.getName(), String.class));

        final CurieCandidatePlan plan = new CurieCandidatePlanner(new IdentifierFormatClassifier()).plan(sourceValues,
                normalizedPrefix, guessPrefix,
                guessPrefix && curiePrefixCatalog != null ? curiePrefixCatalog.getSuggestions()
                        : java.util.Collections.emptyList(),
                maxPrefixGuesses, useIdentifierFormatFilters);
        summary.setRowsExamined(plan.getRowsExamined());
        summary.setRowsWithPrefixGuesses(plan.getRowsWithPrefixGuesses());
        summary.setRowsWithFormatFilteredGuesses(plan.getRowsWithFormatFilteredGuesses());
        summary.setCandidateCuriesSubmitted(plan.getCandidateCuriesSubmitted());
        summary.setUniqueCuriesSubmitted(plan.getUniqueCandidateCuries().size());

        final NodeNormalizationClient client = injectedClient == null
                ? new ApacheNodeNormalizationClient(resolveServiceUrl(serviceUrl, properties),
                        NodeNormalizationProperties.getConnectTimeout(properties),
                        NodeNormalizationProperties.getRequestTimeout(properties))
                : injectedClient;

        final Map<String, NodeNormalizationResult> allResults = new LinkedHashMap<String, NodeNormalizationResult>();
        final List<List<String>> batches = CurieNormalizer.splitBatches(plan.getUniqueCandidateCuries(), batchSize);
        for (int i = 0; i < batches.size(); i++) {
            if (task != null && task.cancelled) {
                summary.setCancelled(true);
                taskMonitor.showMessage(TaskMonitor.Level.WARN,
                        "Normalize Identifiers cancelled before batch " + (i + 1));
                break;
            }

            taskMonitor.setStatusMessage("Normalizing batch " + (i + 1) + " of " + batches.size());
            try {
                allResults.putAll(client.normalize(batches.get(i)));
            } catch (NodeNormalizationException e) {
                summary.incrementFailedBatches();
                summary.addError(e.getMessage());
                taskMonitor.showMessage(TaskMonitor.Level.ERROR, e.getMessage());
            }
            taskMonitor.setProgress(batches.isEmpty() ? 1.0d : (double) (i + 1) / (double) batches.size());
        }

        final Map<Integer, NodeNormalizationResult> selectedResults = selectRowResults(plan, allResults);
        int resolved = selectedResults.size();
        int unresolved = plan.getPreparedIdentifiers().size() - resolved;
        summary.setSuccessfullyNormalized(resolved);
        summary.setUnresolved(unresolved);

        if (!summary.isCancelled())
            writeOutput(table, rows, cleanOutputColumn, overwrite, plan, selectedResults);

        taskMonitor.setProgress(1.0d);
        taskMonitor.setStatusMessage(summary.toHumanString());
        return summary;
    }

    private static String resolveServiceUrl(final String serviceUrl, final Properties properties) {
        if (serviceUrl != null && !serviceUrl.trim().isEmpty())
            return serviceUrl.trim();
        return NodeNormalizationProperties.getBaseUrl(properties);
    }

    /**
     * Validates that the source column is a scalar String column and not a List
     * column.
     * 
     * @param sourceColumn
     * @throws IllegalArgumentException if the source column is not a scalar String
     *                                  column
     */
    private static void validateSourceColumn(final CyColumn sourceColumn) {
        if (sourceColumn.getType() == List.class)
            throw new IllegalArgumentException("List columns are not supported by Normalize Identifiers");
        if (sourceColumn.getType() != String.class)
            throw new IllegalArgumentException("Normalize Identifiers requires a scalar String source column");
    }

    private static String validateOutputColumnName(final String outputColumnName) {
        if (outputColumnName == null || outputColumnName.trim().isEmpty())
            throw new IllegalArgumentException("Output column name is required");
        return outputColumnName.trim();
    }

    private static void validateOutputColumn(final CyTable table, final String outputColumnName,
            final boolean overwrite) {
        final CyColumn existing = table.getColumn(outputColumnName);
        if (existing == null)
            return;
        if (!overwrite)
            throw new IllegalArgumentException("Output column already exists: " + outputColumnName);
        if (existing.getType() != String.class)
            throw new IllegalArgumentException("Existing output column is not a String column: " + outputColumnName);
    }

    private static Map<Integer, NodeNormalizationResult> selectRowResults(final CurieCandidatePlan plan,
            final Map<String, NodeNormalizationResult> results) {
        final Map<Integer, NodeNormalizationResult> selected = new LinkedHashMap<Integer, NodeNormalizationResult>();
        for (final PreparedIdentifier preparedIdentifier : plan.getPreparedIdentifiers()) {
            for (final String curie : preparedIdentifier.getCandidateCuries()) {
                final NodeNormalizationResult result = results.get(curie);
                if (result != null && result.isResolved()) {
                    selected.put(Integer.valueOf(preparedIdentifier.getRowIndex()), result);
                    break;
                }
            }
        }
        return selected;
    }

    private static void writeOutput(final CyTable table, final List<CyRow> rows, final String outputColumnName,
            final boolean overwrite, final CurieCandidatePlan plan,
            final Map<Integer, NodeNormalizationResult> selectedResults) {
        if (table.getColumn(outputColumnName) == null)
            table.createColumn(outputColumnName, String.class, false);

        final Map<Integer, PreparedIdentifier> preparedByRowIndex = new LinkedHashMap<Integer, PreparedIdentifier>();
        for (final PreparedIdentifier preparedIdentifier : plan.getPreparedIdentifiers())
            preparedByRowIndex.put(Integer.valueOf(preparedIdentifier.getRowIndex()), preparedIdentifier);
        for (int i = 0; i < rows.size(); i++) {
            final CyRow row = rows.get(i);
            if (overwrite)
                row.set(outputColumnName, null);
            if (!preparedByRowIndex.containsKey(Integer.valueOf(i)))
                continue;

            final NodeNormalizationResult result = selectedResults.get(Integer.valueOf(i));
            row.set(outputColumnName, result != null && result.isResolved() ? result.getCanonicalCurie() : null);
        }
    }

    public static String defaultOutputColumnName(final String sourceColumnName) {
        if (sourceColumnName == null || sourceColumnName.trim().isEmpty())
            return "idmapper::normalized";
        return "idmapper::normalized_" + sourceColumnName.trim();
    }

    @Override
    public List<Class<?>> getResultClasses() {
        return Arrays.asList(String.class, JSONResult.class, NormalizeIdentifiersSummary.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> R getResults(final Class<? extends R> type) {
        if (type.equals(String.class))
            return (R) summary.toHumanString();
        if (type.equals(JSONResult.class)) {
            final JSONResult result = () -> summary.toJson();
            return (R) result;
        }
        if (type.equals(NormalizeIdentifiersSummary.class))
            return (R) summary;
        return null;
    }
}
