package org.cytoscape.idmapper.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.cytoscape.idmapper.normalization.ApacheNodeNormalizationClient;
import org.cytoscape.idmapper.normalization.CurieNormalizer;
import org.cytoscape.idmapper.normalization.CurieNormalizer.PreparedIdentifiers;
import org.cytoscape.idmapper.normalization.NodeNormalizationClient;
import org.cytoscape.idmapper.normalization.NodeNormalizationException;
import org.cytoscape.idmapper.normalization.NodeNormalizationProperties;
import org.cytoscape.idmapper.normalization.NodeNormalizationResult;
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
    private NormalizeIdentifiersSummary summary = new NormalizeIdentifiersSummary();

    @Tunable(description = "Source column", gravity = 0.0, longDescription = "Column containing identifiers to normalize")
    public String sourceColumnName;

    @Tunable(description = "CURIE prefix", gravity = 1.0, longDescription = "Optional CURIE prefix to apply to values without one", exampleStringValue = "HGNC")
    public String prefix = "";

    @Tunable(description = "Output column name", gravity = 2.0, longDescription = "Column where canonical identifiers will be written")
    public String outputColumnName;

    @Tunable(description = "Batch size", gravity = 3.0, longDescription = "Number of unique identifiers sent in each request")
    public int batchSize;

    @Tunable(description = "Service endpoint", gravity = 4.0, longDescription = "Node Normalization base URL")
    public String serviceUrl;

    @Tunable(description = "Overwrite existing output column", gravity = 5.0, longDescription = "Allow reuse of an existing String output column")
    public boolean overwrite = false;

    public NormalizeIdentifiersTask(final CyColumn column, final Properties nodeNormalizationProperties) {
        this(column, nodeNormalizationProperties, null);
    }

    public NormalizeIdentifiersTask(final CyColumn column, final Properties nodeNormalizationProperties,
            final NodeNormalizationClient injectedClient) {
        super(column);
        this.nodeNormalizationProperties = nodeNormalizationProperties;
        this.injectedClient = injectedClient;
        if (column != null) {
            sourceColumnName = column.getName();
            outputColumnName = defaultOutputColumnName(column.getName());
        }
        batchSize = NodeNormalizationProperties.getBatchSize(nodeNormalizationProperties);
        serviceUrl = NodeNormalizationProperties.getBaseUrl(nodeNormalizationProperties);
    }

    @ProvidesTitle
    public String getTitle() {
        return "Normalize Identifiers";
    }

    @Override
    public void run(final TaskMonitor taskMonitor) throws Exception {
        taskMonitor.setTitle("Normalize Identifiers");
        summary = normalize(column.getTable(), sourceColumnName, prefix, outputColumnName, batchSize, serviceUrl,
                overwrite,
                nodeNormalizationProperties, injectedClient, this, taskMonitor);
    }

    static NormalizeIdentifiersSummary normalize(final CyTable table, final String sourceColumnName,
            final String prefix,
            final String outputColumnName, final int batchSize, final String serviceUrl, final boolean overwrite,
            final Properties properties, final NodeNormalizationClient injectedClient,
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

        final PreparedIdentifiers prepared = CurieNormalizer.prepare(sourceValues, normalizedPrefix);
        summary.setRowsExamined(prepared.getRowsExamined());
        summary.setUniqueCuriesSubmitted(prepared.getUniqueCuries().size());

        final NodeNormalizationClient client = injectedClient == null
                ? new ApacheNodeNormalizationClient(resolveServiceUrl(serviceUrl, properties),
                        NodeNormalizationProperties.getConnectTimeout(properties),
                        NodeNormalizationProperties.getRequestTimeout(properties))
                : injectedClient;

        final Map<String, NodeNormalizationResult> allResults = new LinkedHashMap<String, NodeNormalizationResult>();
        final List<List<String>> batches = CurieNormalizer.splitBatches(prepared.getUniqueCuries(), batchSize);
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

        int resolved = 0;
        int unresolved = 0;
        for (final String curie : prepared.getUniqueCuries()) {
            final NodeNormalizationResult result = allResults.get(curie);
            if (result != null && result.isResolved())
                resolved++;
            else
                unresolved++;
        }
        summary.setSuccessfullyNormalized(resolved);
        summary.setUnresolved(unresolved);

        if (!summary.isCancelled())
            writeOutput(table, rows, cleanOutputColumn, overwrite, prepared, allResults);

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

    private static void writeOutput(final CyTable table, final List<CyRow> rows, final String outputColumnName,
            final boolean overwrite, final PreparedIdentifiers prepared,
            final Map<String, NodeNormalizationResult> results) {
        if (table.getColumn(outputColumnName) == null)
            table.createColumn(outputColumnName, String.class, false);

        final Map<Integer, String> curieByRowIndex = new LinkedHashMap<Integer, String>();
        for (final Map.Entry<String, List<Integer>> entry : prepared.getRowsByCurie().entrySet()) {
            for (final Integer rowIndex : entry.getValue())
                curieByRowIndex.put(rowIndex, entry.getKey());
        }

        for (int i = 0; i < rows.size(); i++) {
            final CyRow row = rows.get(i);
            if (overwrite)
                row.set(outputColumnName, null);
            final String curie = curieByRowIndex.get(Integer.valueOf(i));
            if (curie == null)
                continue;

            final NodeNormalizationResult result = results.get(curie);
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
