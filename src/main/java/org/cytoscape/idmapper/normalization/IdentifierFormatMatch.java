package org.cytoscape.idmapper.normalization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class IdentifierFormatMatch {

    private final String ruleName;
    private final int confidence;
    private final List<String> suggestedPrefixes;

    public IdentifierFormatMatch(final String ruleName, final int confidence, final List<String> suggestedPrefixes) {
        this.ruleName = ruleName;
        this.confidence = confidence;
        this.suggestedPrefixes = new ArrayList<String>(suggestedPrefixes);
    }

    public String getRuleName() {
        return ruleName;
    }

    public int getConfidence() {
        return confidence;
    }

    public List<String> getSuggestedPrefixes() {
        return Collections.unmodifiableList(suggestedPrefixes);
    }
}
