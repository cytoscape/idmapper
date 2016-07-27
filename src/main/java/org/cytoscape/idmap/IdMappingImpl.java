package org.cytoscape.idmap;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A basic implementation of a IdMapping.
 *
 *
 * @author cmzmasek
 *
 */
public class IdMappingImpl implements IdMapping {

    private final SortedSet<String> source_ids;
    private String                  source_type;
    private String                  source_species;
    private final SortedSet<String> target_ids;
    private String                  target_type;
    private String                  target_species;
    private String                  relationship_type;

    public IdMappingImpl() {
        source_ids = new TreeSet<String>();
        target_ids = new TreeSet<String>();
    }

    @Override
    public final Set<String> getSourceIds() {
        return source_ids;
    }

    @Override
    public final Set<String> getTargetIds() {
        return target_ids;
    }

    public final void addSourceId(final String source_id) {
        source_ids.add(source_id);
    }

    public final void addTargetId(final String target_id) {
        target_ids.add(target_id);
    }

    @Override
    public String getSourceType() {
        return source_type;
    }

    public void setSourceType(final String source_type) {
        this.source_type = source_type;
    }

    @Override
    public String getSourceSpecies() {
        return source_species;
    }

    public void setSourceSpecies(final String source_species) {
        this.source_species = source_species;
    }

    @Override
    public String getTargetType() {
        return target_type;
    }

    public void setTargetType(final String target_type) {
        this.target_type = target_type;
    }

    @Override
    public String getTargetSpecies() {
        return target_species;
    }

    public void setTargetSpecies(final String target_species) {
        this.target_species = target_species;
    }

    @Override
    public String getRelationshipType() {
        return relationship_type;
    }

    public void setRelationshipType(final String relationship_type) {
        this.relationship_type = relationship_type;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("source ids    : " + setToSB(getSourceIds()) + "\n");
        sb.append("target ids    : " + setToSB(getTargetIds()) + "\n");
        sb.append("source id type: " + getSourceType() + "\n");
        sb.append("target id type: " + getTargetType() + "\n");
        sb.append("source species: " + getSourceSpecies() + "\n");
        sb.append("target species: " + getTargetSpecies());
        if (getRelationshipType() != null) {
            sb.append("\n");
            sb.append("type          : " + getRelationshipType() + "\n");
        }
        return sb.toString();
    }

    private static StringBuilder setToSB(final Set<String> set) {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (final String str : set) {
            if (first)          first = false;
            else                sb.append(", ");
            sb.append(str);
        }
        return sb;
    }

}
