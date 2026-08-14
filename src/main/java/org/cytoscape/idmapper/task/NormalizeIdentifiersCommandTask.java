package org.cytoscape.idmapper.task;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.command.StringToModel;
import org.cytoscape.idmapper.normalization.CuriePrefixCatalog;
import org.cytoscape.idmapper.normalization.NodeNormalizationProperties;
import org.cytoscape.idmapper.normalization.NormalizeIdentifiersSummary;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.json.JSONResult;

public class NormalizeIdentifiersCommandTask extends AbstractTask implements ObservableTask {

    private final CyServiceRegistrar serviceRegistrar;
    private final Properties nodeNormalizationProperties;
    private final CuriePrefixCatalog curiePrefixCatalog;
    private NormalizeIdentifiersSummary summary = new NormalizeIdentifiersSummary();

    @Tunable(description = "Network", context = "nogui",
            longDescription = "Network name or SUID. Defaults to the current network.", exampleStringValue = "current")
    public String network = "current";

    @Tunable(description = "Table", context = "nogui",
            longDescription = "Target table. Defaults to the current node table.", exampleStringValue = "default node")
    public String table = "node:current";

    @Tunable(description = "Column name", context = "nogui", required = true,
            longDescription = "Source column containing identifiers", exampleStringValue = "name")
    public String columnName;

    @Tunable(description = "CURIE prefix", context = "nogui",
            longDescription = "Optional prefix to apply to values without a CURIE prefix", exampleStringValue = "HGNC")
    public String prefix = "";

    @Tunable(description = "Output column name", context = "nogui",
            longDescription = "Column where canonical identifiers will be written",
            exampleStringValue = "idmapper::normalized::name")
    public String outputColumnName;

    @Tunable(description = "Batch size", context = "nogui",
            longDescription = "Overrides the configured default batch size")
    public int batchSize = -1;

    @Tunable(description = "Service URL", context = "nogui",
            longDescription = "Optional per-command Node Normalization base URL override")
    public String serviceUrl = "";

    @Tunable(description = "Overwrite", context = "nogui",
            longDescription = "Whether an existing String output column may be reused")
    public boolean overwrite = false;

    @Tunable(description = "Guess prefix", context = "nogui",
            longDescription = "Try suggested CURIE prefixes for values that do not already have a CURIE prefix")
    public boolean guessPrefix = false;

    @Tunable(description = "Maximum prefix guesses", context = "nogui",
            longDescription = "Maximum number of guessed prefixes to try for each unprefixed value")
    public int maxPrefixGuesses = -1;

    public NormalizeIdentifiersCommandTask(final CyServiceRegistrar serviceRegistrar,
            final Properties nodeNormalizationProperties, final CuriePrefixCatalog curiePrefixCatalog) {
        this.serviceRegistrar = serviceRegistrar;
        this.nodeNormalizationProperties = nodeNormalizationProperties;
        this.curiePrefixCatalog = curiePrefixCatalog;
    }

    @ProvidesTitle
    public String getTitle() {
        return "Normalize Identifiers";
    }

    @Override
    public void run(final TaskMonitor taskMonitor) throws Exception {
        final CyTable targetTable = resolveTable();
        final String resolvedOutputColumnName = outputColumnName == null || outputColumnName.trim().isEmpty()
                ? NormalizeIdentifiersTask.defaultOutputColumnName(columnName)
                : outputColumnName;
        final int resolvedBatchSize = batchSize == -1
                ? NodeNormalizationProperties.getBatchSize(nodeNormalizationProperties)
                : batchSize;
        final int resolvedMaxPrefixGuesses = maxPrefixGuesses == -1
                ? NodeNormalizationProperties.getMaxPrefixGuesses(nodeNormalizationProperties)
                : maxPrefixGuesses;

        final NormalizeIdentifiersTask adapter = new NormalizeIdentifiersTask(targetTable.getColumn(columnName),
                nodeNormalizationProperties, null, curiePrefixCatalog);
        summary = NormalizeIdentifiersTask.normalize(targetTable, columnName, prefix, resolvedOutputColumnName,
                resolvedBatchSize, serviceUrl, overwrite, guessPrefix, resolvedMaxPrefixGuesses,
                NodeNormalizationProperties.getUseIdentifierFormatFilters(nodeNormalizationProperties),
                nodeNormalizationProperties, null, curiePrefixCatalog, adapter, taskMonitor);
    }

    private CyTable resolveTable() {
        final StringToModel stringToModel = serviceRegistrar.getService(StringToModel.class);
        CyNetwork targetNetwork = null;
        if (stringToModel != null && network != null && !network.trim().isEmpty() && !"current".equals(network.trim()))
            targetNetwork = stringToModel.getNetwork(network.trim());
        if (targetNetwork == null)
            targetNetwork = serviceRegistrar.getService(CyApplicationManager.class).getCurrentNetwork();
        if (targetNetwork == null)
            throw new IllegalArgumentException("No current network is available");

        CyTable targetTable = null;
        if (stringToModel != null && table != null && !table.trim().isEmpty())
            targetTable = stringToModel.getTable(table.trim());
        if (targetTable == null)
            targetTable = targetNetwork.getDefaultNodeTable();
        if (targetTable == null)
            throw new IllegalArgumentException("Unable to resolve target table");
        return targetTable;
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
