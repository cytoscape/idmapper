package org.nrnb.idmapper;

import java.util.Set;

/**
 * This is used to express one-to-one, one-to-many, many-to-one, and
 * many-to-many relationships of biological identifiers from different database
 * source and/or species.
 *
 * @author cmzmasek
 *
 */
public interface IdMapping {

    /**
     * This returns the source id(s) of this id mapping relationship.
     *
     * @return a set of source ids
     */
    public Set<String> getSourceIds();

    /**
     * This returns the target id(s) of this id mapping relationship.
     *
     * @return a set of target ids
     */
    public Set<String> getTargetIds();

    /**
     * This returns the source id type (e.g. "UniProt") of this id mapping
     * relationship.
     *
     * @return the source type
     */
    public MappingSource getSourceType();

    /**
     * This returns the species of the source ids (e.g. "Homo sapiens") of this
     * id mapping relationship. In most cases, source and target species are the
     * same.
     *
     * @return the source species.
     */
    public String getSourceSpecies();

    /**
     * This returns the target id type (e.g. "Ensembl") of this id mapping
     * relationship.
     *
     * @return the target type
     */
    public MappingSource getTargetType();

    /**
     * This returns the species of the target ids (e.g. "Homo sapiens") of this
     * id mapping relationship. In most cases, source and target species are the
     * same.
     *
     * @return the target species.
     */
//    public String getTargetSpecies();

    /**
     * This returns the type of relationship between source and target ids (e.g.
     * "xrefs", "orthologs", "paralogs", "homologs").
     *
     * @return the relationship type
     */
//    public String getRelationshipType();

}
