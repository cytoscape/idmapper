package org.cytoscape.idmapper.internal;

import java.util.Properties;

import org.cytoscape.idmapper.internal.ui.CuriePrefixTunableHandlerFactory;
import org.cytoscape.idmapper.normalization.ApacheCuriePrefixCatalogClient;
import org.cytoscape.idmapper.normalization.CuriePrefixCatalog;
import org.cytoscape.idmapper.normalization.NodeNormalizationProperties;
import org.cytoscape.idmapper.task.MapColumnTaskFactory;
import org.cytoscape.idmapper.task.MapColumnTaskFactoryImpl;
import org.cytoscape.idmapper.task.NormalizeIdentifiersTaskFactory;
import org.cytoscape.idmapper.task.NormalizeIdentifiersTaskFactoryImpl;
import org.cytoscape.property.CyProperty;
import org.cytoscape.property.SimpleCyProperty;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.TableColumnTaskFactory;
import org.cytoscape.work.ServiceProperties;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TunableSetter;
import org.cytoscape.work.swing.GUITunableHandlerFactory;
import org.cytoscape.work.undo.UndoSupport;
import org.osgi.framework.BundleContext;

public class CyActivator extends AbstractCyActivator {

        @Override
        public void start(BundleContext bc) {

                final UndoSupport undo = getService(bc, UndoSupport.class);
                final TunableSetter tunable = getService(bc, TunableSetter.class);
                final CyServiceRegistrar reg = getService(bc, CyServiceRegistrar.class);
                final MapColumnTaskFactory mapColumnTaskFactory = new MapColumnTaskFactoryImpl(undo, tunable, reg);
                final Properties nodeNormalizationProperties = NodeNormalizationProperties.defaults();
                final SimpleCyProperty<Properties> nodeNormalizationCyProperty = new SimpleCyProperty<Properties>(
                                NodeNormalizationProperties.PROPERTY_NAME, nodeNormalizationProperties,
                                Properties.class,
                                CyProperty.SavePolicy.CONFIG_DIR);
                final CuriePrefixCatalog curiePrefixCatalog = new CuriePrefixCatalog(
                                new ApacheCuriePrefixCatalogClient(
                                                NodeNormalizationProperties.getBaseUrl(nodeNormalizationProperties),
                                                NodeNormalizationProperties
                                                                .getConnectTimeout(nodeNormalizationProperties),
                                                NodeNormalizationProperties
                                                                .getRequestTimeout(nodeNormalizationProperties)),
                                NodeNormalizationProperties.getCuriePrefixesCacheTtl(nodeNormalizationProperties));
                final NormalizeIdentifiersTaskFactory normalizeIdentifiersTaskFactory = new NormalizeIdentifiersTaskFactoryImpl(
                                reg, nodeNormalizationProperties);

                final Properties props = new Properties();
                props.setProperty(ServiceProperties.TITLE, "Map column...");
                props.setProperty(ServiceProperties.COMMAND, "map column");
                props.setProperty(ServiceProperties.ENABLE_FOR, "true");

                props.setProperty(ServiceProperties.COMMAND_NAMESPACE, "idmapper");
                props.setProperty(ServiceProperties.COMMAND_DESCRIPTION, "Map a column contents to another id format");

                props.setProperty(ServiceProperties.COMMAND_SUPPORTS_JSON, "true");
                props.setProperty(ServiceProperties.COMMAND_EXAMPLE_JSON, JSON_EXAMPLE);
                props.setProperty(ServiceProperties.COMMAND_LONG_DESCRIPTION,
                                "Uses the BridgeDB service to look up analogous identifiers from a wide selection of other databases");

                registerService(bc, mapColumnTaskFactory, TableColumnTaskFactory.class, props);
                registerService(bc, mapColumnTaskFactory, MapColumnTaskFactory.class, props);
                registerService(bc, mapColumnTaskFactory, TaskFactory.class, props);

                final Properties normalizeProps = new Properties();
                normalizeProps.setProperty(ServiceProperties.TITLE, "Normalize Identifiers...");
                normalizeProps.setProperty(ServiceProperties.COMMAND, "normalize");
                normalizeProps.setProperty(ServiceProperties.ENABLE_FOR, "true");
                normalizeProps.setProperty(ServiceProperties.COMMAND_NAMESPACE, "idmapper");
                normalizeProps.setProperty(ServiceProperties.COMMAND_DESCRIPTION,
                                "Normalize column identifiers using the NCATS Translator Node Normalization Service");
                normalizeProps.setProperty(ServiceProperties.COMMAND_SUPPORTS_JSON, "true");
                normalizeProps.setProperty(ServiceProperties.COMMAND_EXAMPLE_JSON, NORMALIZE_JSON_EXAMPLE);
                normalizeProps.setProperty(ServiceProperties.COMMAND_LONG_DESCRIPTION,
                                "Normalizes CURIE identifiers to canonical primary identifiers using POST /get_normalized_nodes");

                registerService(bc, nodeNormalizationCyProperty, CyProperty.class, new Properties());
                registerService(bc, new CuriePrefixTunableHandlerFactory(curiePrefixCatalog),
                                GUITunableHandlerFactory.class,
                                new Properties());
                registerService(bc, normalizeIdentifiersTaskFactory, TableColumnTaskFactory.class, normalizeProps);
                registerService(bc, normalizeIdentifiersTaskFactory, NormalizeIdentifiersTaskFactory.class,
                                normalizeProps);
                registerService(bc, normalizeIdentifiersTaskFactory, TaskFactory.class, normalizeProps);
        }

        String JSON_EXAMPLE = "{ \"new column\": \"mappedIDs\" }";
        String NORMALIZE_JSON_EXAMPLE = "{ \"outputColumnName\": \"idmapper::normalized::name\" }";

}
