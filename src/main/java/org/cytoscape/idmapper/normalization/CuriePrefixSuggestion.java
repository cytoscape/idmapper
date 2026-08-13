package org.cytoscape.idmapper.normalization;

public final class CuriePrefixSuggestion implements Comparable<CuriePrefixSuggestion> {

    private final String prefix;
    private final long count;

    public CuriePrefixSuggestion(final String prefix, final long count) {
        this.prefix = prefix;
        this.count = count;
    }

    public String getPrefix() {
        return prefix;
    }

    public long getCount() {
        return count;
    }

    public String getDisplayName() {
        return prefix;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    @Override
    public int compareTo(final CuriePrefixSuggestion other) {
        final int countCompare = Long.compare(other.count, count);
        if (countCompare != 0)
            return countCompare;
        return prefix.compareToIgnoreCase(other.prefix);
    }
}
