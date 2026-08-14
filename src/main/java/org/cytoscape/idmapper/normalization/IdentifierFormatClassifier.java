package org.cytoscape.idmapper.normalization;

import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Pattern;

public final class IdentifierFormatClassifier {

    private static final Pattern ENSEMBL = Pattern.compile("^ENS[GPT][0-9]+(\\.[0-9]+)?$");
    private static final Pattern UNIPROT_PRIMARY = Pattern.compile("^[OPQ][0-9][A-Z0-9]{3}[0-9]$");
    private static final Pattern UNIPROT_SECONDARY = Pattern.compile("^[A-NR-Z][0-9][A-Z][A-Z0-9]{2}[0-9]$");
    private static final Pattern NUMERIC = Pattern.compile("^[0-9]+$");

    public IdentifierFormatMatch classify(final String value) {
        if (value == null)
            return null;

        final String trimmed = value.trim();
        if (trimmed.isEmpty() || CurieNormalizer.hasCuriePrefix(trimmed))
            return null;

        if (ENSEMBL.matcher(trimmed).matches())
            return new IdentifierFormatMatch("ensembl", 100, Collections.singletonList("ENSEMBL"));
        if (UNIPROT_PRIMARY.matcher(trimmed).matches() || UNIPROT_SECONDARY.matcher(trimmed).matches())
            return new IdentifierFormatMatch("uniprot", 100, Collections.singletonList("UniProtKB"));
        if (NUMERIC.matcher(trimmed).matches()) {
            return new IdentifierFormatMatch("numeric", 50,
                    Arrays.asList("NCBIGene", "HGNC", "RGD", "MGI", "ZFIN", "SGD", "OMIM", "PMID", "RXCUI",
                            "UMLS"));
        }

        return null;
    }
}
