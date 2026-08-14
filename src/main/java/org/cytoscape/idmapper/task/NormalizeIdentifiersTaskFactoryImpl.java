package org.cytoscape.idmapper.task;

import java.util.Properties;

import org.cytoscape.idmapper.normalization.CuriePrefixCatalog;
import org.cytoscape.model.CyColumn;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.AbstractTableColumnTaskFactory;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

public class NormalizeIdentifiersTaskFactoryImpl extends AbstractTableColumnTaskFactory
        implements NormalizeIdentifiersTaskFactory, TaskFactory {

    private final CyServiceRegistrar serviceRegistrar;
    private final Properties nodeNormalizationProperties;
    private final CuriePrefixCatalog curiePrefixCatalog;

    public NormalizeIdentifiersTaskFactoryImpl(final CyServiceRegistrar serviceRegistrar,
            final Properties nodeNormalizationProperties, final CuriePrefixCatalog curiePrefixCatalog) {
        this.serviceRegistrar = serviceRegistrar;
        this.nodeNormalizationProperties = nodeNormalizationProperties;
        this.curiePrefixCatalog = curiePrefixCatalog;
    }

    @Override
    public TaskIterator createTaskIterator(final CyColumn column) {
        if (column == null)
            throw new IllegalStateException("No table column was selected");
        return new TaskIterator(new NormalizeIdentifiersTask(column, nodeNormalizationProperties, null,
                curiePrefixCatalog));
    }

    @Override
    public TaskIterator createTaskIterator() {
        return new TaskIterator(new NormalizeIdentifiersCommandTask(serviceRegistrar, nodeNormalizationProperties,
                curiePrefixCatalog));
    }

    @Override
    public boolean isReady() {
        return true;
    }
}
