package org.cytoscape.idmapper.normalization;

/**
 * Represents the result of a node normalization operation.
 */
public final class NodeNormalizationResult {
    private final String inputCurie;
    private final String canonicalCurie;

    /**
     * Creates a new instance of NodeNormalizationResult.
     * 
     * @param inputCurie
     * @param canonicalCurie
     */
    public NodeNormalizationResult(final String inputCurie, final String canonicalCurie) {
        this.inputCurie = inputCurie;
        this.canonicalCurie = canonicalCurie;
    }

    /**
     * Returns the input CURIE that was normalized.
     * 
     * @return
     */
    public String getInputCurie() {
        return inputCurie;
    }

    /**
     * Returns the canonical CURIE that was resolved from the input CURIE.
     * 
     * @return
     */
    public String getCanonicalCurie() {
        return canonicalCurie;
    }

    /**
     * Returns true if the input CURIE was successfully resolved to a canonical
     * CURIE.
     * 
     * @return
     */
    public boolean isResolved() {
        return canonicalCurie != null && !canonicalCurie.trim().isEmpty();
    }
}
