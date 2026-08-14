package org.cytoscape.idmapper.normalization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CurieCandidatePlan {

    private final int rowsExamined;
    private final int candidateCuriesSubmitted;
    private final int rowsWithPrefixGuesses;
    private final int rowsWithFormatFilteredGuesses;
    private final List<PreparedIdentifier> preparedIdentifiers;
    private final List<String> uniqueCandidateCuries;

    public CurieCandidatePlan(final int rowsExamined, final int candidateCuriesSubmitted,
            final int rowsWithPrefixGuesses, final int rowsWithFormatFilteredGuesses,
            final List<PreparedIdentifier> preparedIdentifiers, final List<String> uniqueCandidateCuries) {
        this.rowsExamined = rowsExamined;
        this.candidateCuriesSubmitted = candidateCuriesSubmitted;
        this.rowsWithPrefixGuesses = rowsWithPrefixGuesses;
        this.rowsWithFormatFilteredGuesses = rowsWithFormatFilteredGuesses;
        this.preparedIdentifiers = new ArrayList<PreparedIdentifier>(preparedIdentifiers);
        this.uniqueCandidateCuries = new ArrayList<String>(uniqueCandidateCuries);
    }

    public int getRowsExamined() {
        return rowsExamined;
    }

    public int getCandidateCuriesSubmitted() {
        return candidateCuriesSubmitted;
    }

    public int getRowsWithPrefixGuesses() {
        return rowsWithPrefixGuesses;
    }

    public int getRowsWithFormatFilteredGuesses() {
        return rowsWithFormatFilteredGuesses;
    }

    public List<PreparedIdentifier> getPreparedIdentifiers() {
        return Collections.unmodifiableList(preparedIdentifiers);
    }

    public List<String> getUniqueCandidateCuries() {
        return Collections.unmodifiableList(uniqueCandidateCuries);
    }
}
