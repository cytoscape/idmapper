package org.cytoscape.idmapper.normalization;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CurieCandidatePlanner {

    private final IdentifierFormatClassifier classifier;

    public CurieCandidatePlanner(final IdentifierFormatClassifier classifier) {
        this.classifier = classifier;
    }

    public CurieCandidatePlan plan(final List<String> values, final String prefix, final boolean guessPrefix,
            final List<CuriePrefixSuggestion> prefixSuggestions, final int maxPrefixGuesses,
            final boolean useIdentifierFormatFilters) {
        if (maxPrefixGuesses <= 0)
            throw new IllegalArgumentException("Maximum prefix guesses must be a positive integer");

        final String normalizedPrefix = CurieNormalizer.normalizePrefix(prefix);
        final Map<String, Integer> observedPrefixCounts = observedPrefixCounts(values);
        final List<String> catalogPrefixes = catalogPrefixes(prefixSuggestions);
        final Set<String> catalogPrefixSet = new LinkedHashSet<String>(catalogPrefixes);
        final List<PreparedIdentifier> prepared = new ArrayList<PreparedIdentifier>();
        final Set<String> uniqueCandidates = new LinkedHashSet<String>();
        int candidateCount = 0;
        int rowsWithPrefixGuesses = 0;
        int rowsWithFormatFilteredGuesses = 0;

        if (values != null) {
            for (int i = 0; i < values.size(); i++) {
                final String value = values.get(i);
                final String trimmed = value == null ? null : value.trim();
                if (trimmed == null || trimmed.isEmpty())
                    continue;

                final boolean alreadyPrefixed = CurieNormalizer.hasCuriePrefix(trimmed);
                final List<String> candidates = new ArrayList<String>();
                String ruleName = null;
                boolean formatFiltered = false;

                if (alreadyPrefixed) {
                    candidates.add(trimmed);
                } else if (!guessPrefix) {
                    candidates.add(CurieNormalizer.toCurie(trimmed, normalizedPrefix));
                } else {
                    addPrefixCandidate(candidates, normalizedPrefix, trimmed, maxPrefixGuesses);
                    addObservedPrefixCandidates(candidates, observedPrefixCounts, trimmed, maxPrefixGuesses);

                    final IdentifierFormatMatch match = useIdentifierFormatFilters && classifier != null
                            ? classifier.classify(trimmed)
                            : null;
                    if (match != null) {
                        ruleName = match.getRuleName();
                        final int before = candidates.size();
                        for (final String suggestedPrefix : match.getSuggestedPrefixes()) {
                            if (catalogPrefixSet.isEmpty() || catalogPrefixSet.contains(suggestedPrefix)
                                    || observedPrefixCounts.containsKey(suggestedPrefix))
                                addPrefixCandidate(candidates, suggestedPrefix, trimmed, maxPrefixGuesses);
                        }
                        formatFiltered = candidates.size() > before;
                    }

                    if (!formatFiltered)
                        addCatalogPrefixCandidates(candidates, catalogPrefixes, trimmed, maxPrefixGuesses);

                    if (candidates.size() > 1)
                        rowsWithPrefixGuesses++;
                    if (formatFiltered)
                        rowsWithFormatFilteredGuesses++;
                }

                if (candidates.isEmpty())
                    throw new IllegalArgumentException("No CURIE prefix candidates are available for value: " + trimmed);

                candidateCount += candidates.size();
                uniqueCandidates.addAll(candidates);
                prepared.add(new PreparedIdentifier(i, trimmed, alreadyPrefixed, ruleName, formatFiltered, candidates));
            }
        }

        final int rowsExamined = values == null ? 0 : values.size();
        return new CurieCandidatePlan(rowsExamined, candidateCount, rowsWithPrefixGuesses, rowsWithFormatFilteredGuesses,
                prepared, new ArrayList<String>(uniqueCandidates));
    }

    private static Map<String, Integer> observedPrefixCounts(final List<String> values) {
        final Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
        if (values == null)
            return counts;
        for (final String value : values) {
            if (!CurieNormalizer.hasCuriePrefix(value))
                continue;
            final String trimmed = value.trim();
            final int colon = trimmed.indexOf(':');
            if (colon <= 0)
                continue;
            final String prefix = trimmed.substring(0, colon);
            final Integer count = counts.get(prefix);
            counts.put(prefix, Integer.valueOf(count == null ? 1 : count.intValue() + 1));
        }
        return counts;
    }

    private static List<String> catalogPrefixes(final List<CuriePrefixSuggestion> prefixSuggestions) {
        final List<String> prefixes = new ArrayList<String>();
        if (prefixSuggestions == null)
            return prefixes;
        for (final CuriePrefixSuggestion suggestion : prefixSuggestions) {
            if (suggestion != null && suggestion.getPrefix() != null)
                addUnique(prefixes, suggestion.getPrefix());
        }
        return prefixes;
    }

    private static void addObservedPrefixCandidates(final List<String> candidates, final Map<String, Integer> counts,
            final String value, final int maxPrefixGuesses) {
        final List<Map.Entry<String, Integer>> entries = new ArrayList<Map.Entry<String, Integer>>(counts.entrySet());
        entries.sort((a, b) -> {
            final int countCompare = b.getValue().compareTo(a.getValue());
            if (countCompare != 0)
                return countCompare;
            return a.getKey().compareToIgnoreCase(b.getKey());
        });
        for (final Map.Entry<String, Integer> entry : entries)
            addPrefixCandidate(candidates, entry.getKey(), value, maxPrefixGuesses);
    }

    private static void addCatalogPrefixCandidates(final List<String> candidates, final List<String> catalogPrefixes,
            final String value, final int maxPrefixGuesses) {
        for (final String catalogPrefix : catalogPrefixes)
            addPrefixCandidate(candidates, catalogPrefix, value, maxPrefixGuesses);
    }

    private static void addPrefixCandidate(final List<String> candidates, final String prefix, final String value,
            final int maxPrefixGuesses) {
        if (prefix == null || candidates.size() >= maxPrefixGuesses)
            return;
        final String normalizedPrefix = CurieNormalizer.normalizePrefix(prefix);
        if (normalizedPrefix == null)
            return;
        addUnique(candidates, normalizedPrefix + ":" + value);
    }

    private static void addUnique(final List<String> values, final String value) {
        if (value != null && !values.contains(value))
            values.add(value);
    }
}
