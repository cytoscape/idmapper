package org.cytoscape.idmapper.normalization;

import java.util.List;
import java.util.Map;

public interface NodeNormalizationClient {
    Map<String, NodeNormalizationResult> normalize(List<String> curies) throws NodeNormalizationException;
}
