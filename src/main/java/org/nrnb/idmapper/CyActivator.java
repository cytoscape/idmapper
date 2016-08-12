package org.nrnb.idmapper;

/*
 * %%
 * Copyright (C) 2006 - 2016 The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

import static org.cytoscape.work.ServiceProperties.COMMAND;
import static org.cytoscape.work.ServiceProperties.COMMAND_DESCRIPTION;
import static org.cytoscape.work.ServiceProperties.COMMAND_NAMESPACE;
import static org.cytoscape.work.ServiceProperties.TITLE;

import java.util.Properties;

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
        mapColumnTaskFactoryProps.setProperty(TITLE, "Map colum...");
        mapColumnTaskFactoryProps.setProperty(COMMAND, "map column");
        mapColumnTaskFactoryProps.setProperty(COMMAND_NAMESPACE, "table");
        mapColumnTaskFactoryProps.setProperty(COMMAND_DESCRIPTION,  "Map a column contents to another id format");
        registerService(bc, mapColumnTaskFactory, TableColumnTaskFactory.class, mapColumnTaskFactoryProps);
        registerService(bc, mapColumnTaskFactory, MapColumnTaskFactory.class, mapColumnTaskFactoryProps);
    }
}
