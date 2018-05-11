package org.cytoscape.idmapper.internal;

import java.util.Properties;

import org.cytoscape.idmapper.task.MapColumnTaskFactory;
import org.cytoscape.idmapper.task.MapColumnTaskFactoryImpl;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.TableColumnTaskFactory;
import org.cytoscape.work.ServiceProperties;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TunableSetter;
import org.cytoscape.work.undo.UndoSupport;
import org.osgi.framework.BundleContext;

public class CyActivator extends AbstractCyActivator {

    @Override
    public void start(BundleContext bc) {

        final UndoSupport undo = getService(bc, UndoSupport.class);
        final TunableSetter tunable = getService(bc, TunableSetter.class);
        final CyServiceRegistrar reg = getService(bc, CyServiceRegistrar.class);
        final MapColumnTaskFactory mapColumnTaskFactory = new MapColumnTaskFactoryImpl( undo, tunable, reg);

        final Properties props = new Properties();
        props.setProperty(ServiceProperties.TITLE, "Map column...");
        props.setProperty(ServiceProperties.COMMAND, "map column");
        props.setProperty(ServiceProperties.ENABLE_FOR, "true");
        
        props.setProperty(ServiceProperties.COMMAND_NAMESPACE, "idmapper");
        props.setProperty(ServiceProperties.COMMAND_DESCRIPTION,  "Map a column contents to another id format");

        props.setProperty(ServiceProperties.COMMAND_SUPPORTS_JSON, "true");
      	props.setProperty(ServiceProperties.COMMAND_EXAMPLE_JSON, JSON_EXAMPLE);
    	props.setProperty(ServiceProperties.COMMAND_LONG_DESCRIPTION,  "Uses the BridgeDB service to look up analogous identifiers from a wide selection of other databases");

        registerService(bc, mapColumnTaskFactory, TableColumnTaskFactory.class, props);
        registerService(bc, mapColumnTaskFactory, MapColumnTaskFactory.class, props);
        registerService(bc, mapColumnTaskFactory, TaskFactory.class, props);
    }
	String JSON_EXAMPLE = "{ \"new column\": \"mappedIDs\" }";

}
