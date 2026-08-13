package org.cytoscape.idmapper.normalization;

import java.util.Collections;
import java.util.List;

public class CuriePrefixCatalog {

    private final CuriePrefixCatalogClient client;
    private final long ttlMillis;
    private long lastLoadTimeMillis;
    private List<CuriePrefixSuggestion> cachedSuggestions = Collections.emptyList();

    public CuriePrefixCatalog(final CuriePrefixCatalogClient client, final long ttlMillis) {
        this.client = client;
        this.ttlMillis = ttlMillis;
    }

    public synchronized List<CuriePrefixSuggestion> getSuggestions() {
        final long now = System.currentTimeMillis();
        if (cachedSuggestions.isEmpty() || now - lastLoadTimeMillis > ttlMillis) {
            try {
                cachedSuggestions = client.fetchPrefixSuggestions();
                lastLoadTimeMillis = now;
            } catch (NodeNormalizationException e) {
                if (cachedSuggestions == null)
                    cachedSuggestions = Collections.emptyList();
            }
        }
        return cachedSuggestions;
    }
}
