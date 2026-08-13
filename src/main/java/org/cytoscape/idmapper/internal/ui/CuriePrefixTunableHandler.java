package org.cytoscape.idmapper.internal.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.lang.reflect.Field;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.cytoscape.idmapper.normalization.CuriePrefixCatalog;
import org.cytoscape.idmapper.normalization.CuriePrefixSuggestion;
import org.cytoscape.idmapper.normalization.CuriePrefixValue;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.swing.AbstractGUITunableHandler;

public class CuriePrefixTunableHandler extends AbstractGUITunableHandler {

    private final JComboBox<Object> comboBox;
    private boolean updating;

    public CuriePrefixTunableHandler(final Field field, final Object instance, final Tunable tunable,
            final CuriePrefixCatalog catalog) {
        super(field, instance, tunable);
        panel = new JPanel(new BorderLayout(8, 0));
        comboBox = new JComboBox<Object>();
        comboBox.setEditable(true);
        comboBox.setPreferredSize(new Dimension(320, comboBox.getPreferredSize().height));
        comboBox.addItem("");

        final JLabel label = new JLabel(getDescription());
        label.setToolTipText(getTooltip());
        comboBox.setToolTipText(getTooltip());
        panel.add(label, BorderLayout.WEST);
        panel.add(comboBox, BorderLayout.CENTER);

        update();
        comboBox.addActionListener(e -> handle());
        loadSuggestions(catalog);
    }

    @Override
    public void handle() {
        if (updating)
            return;
        try {
            setValue(new CuriePrefixValue(getSelectedPrefix()));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to set CURIE prefix", e);
        }
    }

    @Override
    public void update() {
        updating = true;
        try {
            final Object value = getValue();
            if (value instanceof CuriePrefixValue)
                comboBox.getEditor().setItem(((CuriePrefixValue) value).getPrefix());
        } catch (Exception e) {
            comboBox.getEditor().setItem("");
        } finally {
            updating = false;
        }
    }

    @Override
    public String getState() {
        return getSelectedPrefix();
    }

    private String getSelectedPrefix() {
        final Object item = comboBox.getEditor().getItem();
        if (item instanceof CuriePrefixSuggestion)
            return ((CuriePrefixSuggestion) item).getPrefix();
        if (item == null)
            return "";
        return item.toString().trim();
    }

    private void loadSuggestions(final CuriePrefixCatalog catalog) {
        final Thread loader = new Thread(() -> {
            final java.util.List<CuriePrefixSuggestion> suggestions = catalog.getSuggestions();
            SwingUtilities.invokeLater(() -> {
                final String typedValue = getSelectedPrefix();
                updating = true;
                try {
                    for (final CuriePrefixSuggestion suggestion : suggestions)
                        comboBox.addItem(suggestion);
                    comboBox.getEditor().setItem(typedValue);
                } finally {
                    updating = false;
                }
            });
        }, "idmapper-curie-prefix-loader");
        loader.setDaemon(true);
        loader.start();
    }
}
