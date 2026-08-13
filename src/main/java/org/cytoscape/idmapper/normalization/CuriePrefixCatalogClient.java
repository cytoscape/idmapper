package org.cytoscape.idmapper.normalization;

import java.util.List;

public interface CuriePrefixCatalogClient {
    List<CuriePrefixSuggestion> fetchPrefixSuggestions() throws NodeNormalizationException;
}
