package org.cytoscape.idmapper.internal;

import static org.cytoscape.work.ServiceProperties.COMMAND;
import static org.cytoscape.work.ServiceProperties.COMMAND_DESCRIPTION;
import static org.cytoscape.work.ServiceProperties.COMMAND_NAMESPACE;
import static org.cytoscape.work.ServiceProperties.TITLE;

import java.util.Properties;

import org.cytoscape.idmapper.task.MapColumnTaskFactory;
import org.cytoscape.idmapper.task.MapColumnTaskFactoryImpl;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.TableColumnTaskFactory;
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
        mapColumnTaskFactoryProps.setProperty(TITLE, "Map column...");
//        mapColumnTaskFactoryProps.setProperty(TOOL_BAR_GRAVITY,"2.0");
//        mapColumnTaskFactoryProps.setProperty(LARGE_ICON_URL, getClass().getResource("/images/icons/table_map.png").toString());
//        mapColumnTaskFactoryProps.setProperty(IN_TOOL_BAR,"true");
        mapColumnTaskFactoryProps.setProperty(COMMAND, "map column");
        mapColumnTaskFactoryProps.setProperty(COMMAND_NAMESPACE, "table");
        mapColumnTaskFactoryProps.setProperty(COMMAND_DESCRIPTION,  "Map a column contents to another id format");
        registerService(bc, mapColumnTaskFactory, TableColumnTaskFactory.class, mapColumnTaskFactoryProps);
        registerService(bc, mapColumnTaskFactory, MapColumnTaskFactory.class, mapColumnTaskFactoryProps);
    }
}
