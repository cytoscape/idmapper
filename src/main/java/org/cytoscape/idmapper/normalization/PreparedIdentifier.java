package org.cytoscape.idmapper.normalization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PreparedIdentifier {

    private final int rowIndex;
    private final String originalValue;
    private final boolean alreadyPrefixed;
    private final String formatRuleName;
    private final boolean formatFiltered;
    private final List<String> candidateCuries;

    public PreparedIdentifier(final int rowIndex, final String originalValue, final boolean alreadyPrefixed,
            final String formatRuleName, final boolean formatFiltered, final List<String> candidateCuries) {
        this.rowIndex = rowIndex;
        this.originalValue = originalValue;
        this.alreadyPrefixed = alreadyPrefixed;
        this.formatRuleName = formatRuleName;
        this.formatFiltered = formatFiltered;
        this.candidateCuries = new ArrayList<String>(candidateCuries);
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public String getOriginalValue() {
        return originalValue;
    }

    public boolean isAlreadyPrefixed() {
        return alreadyPrefixed;
    }

    public String getFormatRuleName() {
        return formatRuleName;
    }

    public boolean isFormatFiltered() {
        return formatFiltered;
    }

    public List<String> getCandidateCuries() {
        return Collections.unmodifiableList(candidateCuries);
    }
}
