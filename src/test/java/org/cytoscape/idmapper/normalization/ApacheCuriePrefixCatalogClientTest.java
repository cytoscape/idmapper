package org.cytoscape.idmapper.normalization;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class ApacheCuriePrefixCatalogClientTest {

    @Test
    public void parsesAndSortsPrefixSuggestions() throws Exception {
        final String body = "{"
                + "\"biolink:Gene\":{\"curie_prefix\":{\"HGNC\":\"45130\",\"NCBIGene\":\"70201057\"}},"
                + "\"biolink:Protein\":{\"curie_prefix\":{\"UniProtKB\":\"149881839\",\"HGNC\":\"100\"}}"
                + "}";

        final List<CuriePrefixSuggestion> suggestions =
                ApacheCuriePrefixCatalogClient.parsePrefixSuggestions(body);

        assertEquals("UniProtKB", suggestions.get(0).getPrefix());
        assertEquals(149881839L, suggestions.get(0).getCount());
        assertEquals("NCBIGene", suggestions.get(1).getPrefix());
        assertEquals(70201057L, suggestions.get(1).getCount());
        assertEquals("HGNC", suggestions.get(2).getPrefix());
        assertEquals(45130L, suggestions.get(2).getCount());
    }

    @Test(expected = NodeNormalizationException.class)
    public void rejectsMalformedJson() throws Exception {
        ApacheCuriePrefixCatalogClient.parsePrefixSuggestions("{not-json");
    }

    @Test
    public void cacheReusesSuggestionsWithinTtl() {
        final CountingCatalogClient client = new CountingCatalogClient();
        final CuriePrefixCatalog catalog = new CuriePrefixCatalog(client, 60000L);

        assertEquals(1, catalog.getSuggestions().size());
        assertEquals(1, catalog.getSuggestions().size());
        assertEquals(1, client.calls);
    }

    private static final class CountingCatalogClient implements CuriePrefixCatalogClient {
        int calls;

        @Override
        public List<CuriePrefixSuggestion> fetchPrefixSuggestions() {
            calls++;
            return Arrays.asList(new CuriePrefixSuggestion("HGNC", 1L));
        }
    }
}
