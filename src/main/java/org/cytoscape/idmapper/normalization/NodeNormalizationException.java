package org.cytoscape.idmapper.normalization;

public class NodeNormalizationException extends Exception {
    private static final long serialVersionUID = 1L;

    public NodeNormalizationException(final String message) {
        super(message);
    }

    public NodeNormalizationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
