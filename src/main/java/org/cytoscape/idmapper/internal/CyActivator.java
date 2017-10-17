package org.cytoscape.idmapper.internal;

import java.util.Properties;

import org.cytoscape.idmapper.task.MapColumnTaskFactory;
import org.cytoscape.idmapper.task.MapColumnTaskFactoryImpl;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.TableColumnTaskFactory;
import org.cytoscape.work.ServiceProperties;
import org.cytoscape.work.TunableSetter;
import org.cytoscape.work.undo.UndoSupport;
import org.osgi.framework.BundleContext;

public class CyActivator extends AbstractCyActivator {

    @Override
    public void start(BundleContext bc) {

        final UndoSupport undo = getService(bc, UndoSupport.class);
        final TunableSetter tunable = getService(bc, TunableSetter.class);
        final CyServiceRegistrar reg = getService(bc, CyServiceRegistrar.class);
        final MapColumnTaskFactoryImpl mapColumnTaskFactory = new MapColumnTaskFactoryImpl( undo, tunable, reg);

        final Properties mapColumnTaskFactoryProps = new Properties();
        mapColumnTaskFactoryProps.setProperty(ServiceProperties.TITLE, "Map column...");
        mapColumnTaskFactoryProps.setProperty(ServiceProperties.COMMAND, "map column");
        mapColumnTaskFactoryProps.setProperty(ServiceProperties.COMMAND_NAMESPACE, "idmapper");
        mapColumnTaskFactoryProps.setProperty(ServiceProperties.COMMAND_DESCRIPTION,  "Map a column contents to another id format");

      mapColumnTaskFactoryProps.setProperty(ServiceProperties.COMMAND_SUPPORTS_JSON, "true");
    mapColumnTaskFactoryProps.setProperty(ServiceProperties.COMMAND_EXAMPLE_JSON, JSON_EXAMPLE);
    mapColumnTaskFactoryProps.setProperty(ServiceProperties.COMMAND_LONG_DESCRIPTION,  "Uses the BridgeDB service to look up analogous identifiers from a wide selection of other databases");

         registerService(bc, mapColumnTaskFactory, TableColumnTaskFactory.class, mapColumnTaskFactoryProps);
        registerService(bc, mapColumnTaskFactory, MapColumnTaskFactory.class, mapColumnTaskFactoryProps);
    }
	String JSON_EXAMPLE = "{\"SUID\":1234}";

}
