package org.cytoscape.idmapper.normalization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.simple.JSONObject;

/**
 * Represents the summary of a normalize identifiers operation.
 */
public final class NormalizeIdentifiersSummary {
    private int rowsExamined;
    private int uniqueCuriesSubmitted;
    private int successfullyNormalized;
    private int unresolved;
    private int failedBatches;
    private boolean cancelled;
    private String outputColumnName;
    private final List<String> errors = new ArrayList<String>();

    /**
     * Returns the number of rows examined during the normalization operation.
     * 
     * @return
     */
    public int getRowsExamined() {
        return rowsExamined;
    }

    /**
     * Sets the number of rows examined during the normalization operation.
     * 
     * @param rowsExamined
     */
    public void setRowsExamined(final int rowsExamined) {
        this.rowsExamined = rowsExamined;
    }

    /**
     * Returns the number of unique CURIEs submitted for normalization.
     * 
     * @return
     */
    public int getUniqueCuriesSubmitted() {
        return uniqueCuriesSubmitted;
    }

    /**
     * Sets the number of unique CURIEs submitted for normalization.
     * 
     * @param uniqueCuriesSubmitted
     */
    public void setUniqueCuriesSubmitted(final int uniqueCuriesSubmitted) {
        this.uniqueCuriesSubmitted = uniqueCuriesSubmitted;
    }

    /**
     * Returns the number of CURIEs that were successfully normalized.
     * 
     * @return
     */
    public int getSuccessfullyNormalized() {
        return successfullyNormalized;
    }

    /**
     * Sets the number of CURIEs that were successfully normalized.
     * 
     * @param successfullyNormalized
     */
    public void setSuccessfullyNormalized(final int successfullyNormalized) {
        this.successfullyNormalized = successfullyNormalized;
    }

    /**
     * Returns the number of CURIEs that could not be resolved to a canonical CURIE.
     * 
     * @return
     */
    public int getUnresolved() {
        return unresolved;
    }

    /**
     * Sets the number of CURIEs that could not be resolved to a canonical CURIE.
     * 
     * @param unresolved
     */
    public void setUnresolved(final int unresolved) {
        this.unresolved = unresolved;
    }

    /**
     * Returns the number of batches that failed during the normalization operation.
     * 
     * @return
     */
    public int getFailedBatches() {
        return failedBatches;
    }

    /**
     * Increments the number of batches that failed during the normalization
     * operation.
     */
    public void incrementFailedBatches() {
        failedBatches++;
    }

    /**
     * Returns true if the normalization operation was cancelled before all batches
     * completed.
     * 
     * @return
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Sets whether the normalization operation was cancelled before all batches
     * 
     * @param cancelled
     */
    public void setCancelled(final boolean cancelled) {
        this.cancelled = cancelled;
    }

    /**
     * Returns the name of the output column where normalized identifiers were
     * written.
     * 
     * @return
     */
    public String getOutputColumnName() {
        return outputColumnName;
    }

    /**
     * Sets the name of the output column where normalized identifiers were written.
     * 
     * @param outputColumnName
     */
    public void setOutputColumnName(final String outputColumnName) {
        this.outputColumnName = outputColumnName;
    }

    /**
     * Adds an error message to the list of errors encountered during the
     * normalization operation.
     * 
     * @param error
     */
    public void addError(final String error) {
        if (error != null && !error.trim().isEmpty())
            errors.add(error);
    }

    /**
     * Returns an unmodifiable list of error messages encountered during the
     * normalization operation.
     * 
     * @return
     */
    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    /**
     * Returns a JSON representation of the summary of the normalization operation.
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    public String toJson() {
        final JSONObject object = new JSONObject();
        object.put("rowsExamined", rowsExamined);
        object.put("uniqueCuriesSubmitted", uniqueCuriesSubmitted);
        object.put("successfullyNormalized", successfullyNormalized);
        object.put("unresolved", unresolved);
        object.put("failedBatches", failedBatches);
        object.put("cancelled", cancelled);
        object.put("outputColumnName", outputColumnName);
        object.put("errors", new ArrayList<String>(errors));
        return object.toJSONString();
    }

    /**
     * Returns a human-readable string representation of the summary of the
     * normalization operation.
     * 
     * @return
     */
    public String toHumanString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Normalize Identifiers complete.\n");
        sb.append("Rows examined: ").append(rowsExamined).append('\n');
        sb.append("Unique CURIEs submitted: ").append(uniqueCuriesSubmitted).append('\n');
        sb.append("Successfully normalized: ").append(successfullyNormalized).append('\n');
        sb.append("Unresolved: ").append(unresolved).append('\n');
        sb.append("Failed batches: ").append(failedBatches).append('\n');
        if (cancelled)
            sb.append("Cancelled before all batches completed.\n");
        if (outputColumnName != null)
            sb.append("Output column: ").append(outputColumnName);
        if (!errors.isEmpty())
            sb.append("\nErrors: ").append(errors);
        return sb.toString();
    }
}
