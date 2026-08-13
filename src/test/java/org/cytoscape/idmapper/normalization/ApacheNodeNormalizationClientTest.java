package org.cytoscape.idmapper.normalization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;

import org.junit.Test;

public class ApacheNodeNormalizationClientTest {

    @Test
    public void buildsEndpointWithAndWithoutTrailingSlash() throws Exception {
        final URI noSlash = ApacheNodeNormalizationClient.buildEndpoint("https://example.org");
        final URI slash = ApacheNodeNormalizationClient.buildEndpoint("https://example.org/");

        assertEquals("https://example.org/get_normalized_nodes", noSlash.toString());
        assertEquals("https://example.org/get_normalized_nodes", slash.toString());
    }

    @Test
    public void buildsPostBodyWithCuriesArray() {
        final String body = ApacheNodeNormalizationClient.buildRequestBody(Arrays.asList("HGNC:11998", "NCBIGene:7157"));

        assertTrue(body.contains("\"curies\""));
        assertTrue(body.contains("\"HGNC:11998\""));
        assertTrue(body.contains("\"NCBIGene:7157\""));
    }

    @Test
    public void parsesPrimaryIdentifiers() throws Exception {
        final String body = "{"
                + "\"HGNC:11998\":{\"id\":{\"identifier\":\"NCBIGene:7157\",\"label\":\"TP53\"}},"
                + "\"UniProtKB:P04637\":{\"id\":{\"identifier\":\"UniProtKB:P04637\"}}"
                + "}";

        final Map<String, NodeNormalizationResult> results = ApacheNodeNormalizationClient.parseResponse(
                Arrays.asList("HGNC:11998", "UniProtKB:P04637"), body);

        assertEquals("NCBIGene:7157", results.get("HGNC:11998").getCanonicalCurie());
        assertEquals("UniProtKB:P04637", results.get("UniProtKB:P04637").getCanonicalCurie());
    }

    @Test
    public void treatsPartialResponsesAsUnresolved() throws Exception {
        final String body = "{\"HGNC:11998\":{\"id\":{\"identifier\":\"NCBIGene:7157\"}}}";

        final Map<String, NodeNormalizationResult> results = ApacheNodeNormalizationClient.parseResponse(
                Arrays.asList("HGNC:11998", "MISSING:1"), body);

        assertTrue(results.get("HGNC:11998").isResolved());
        assertFalse(results.get("MISSING:1").isResolved());
    }

    @Test(expected = NodeNormalizationException.class)
    public void reportsMalformedJson() throws Exception {
        ApacheNodeNormalizationClient.parseResponse(Arrays.asList("HGNC:11998"), "{not-json");
    }
}
