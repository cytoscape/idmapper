package org.cytoscape.idmapper.internal.ui;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.cytoscape.idmapper.normalization.CuriePrefix;
import org.cytoscape.idmapper.normalization.CuriePrefixCatalog;
import org.cytoscape.idmapper.normalization.CuriePrefixValue;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.swing.GUITunableHandlerFactory;

public class CuriePrefixTunableHandlerFactory implements GUITunableHandlerFactory<CuriePrefixTunableHandler> {

    private final CuriePrefixCatalog catalog;

    public CuriePrefixTunableHandlerFactory(final CuriePrefixCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public CuriePrefixTunableHandler createTunableHandler(final Field field, final Object instance,
            final Tunable tunable) {
        if (field == null || field.getType() != CuriePrefixValue.class || !field.isAnnotationPresent(CuriePrefix.class))
            return null;
        return new CuriePrefixTunableHandler(field, instance, tunable, catalog);
    }

    @Override
    public CuriePrefixTunableHandler createTunableHandler(final Method getter, final Method setter,
            final Object instance, final Tunable tunable) {
        return null;
    }
}
