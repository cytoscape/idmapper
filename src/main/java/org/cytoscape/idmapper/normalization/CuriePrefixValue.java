package org.cytoscape.idmapper.normalization;

public final class CuriePrefixValue {

    private String prefix;

    public CuriePrefixValue() {
        this("");
    }

    public CuriePrefixValue(final String prefix) {
        setPrefix(prefix);
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(final String prefix) {
        this.prefix = prefix == null ? "" : prefix;
    }

    @Override
    public String toString() {
        return prefix;
    }
}
