package org.cytoscape.idmapper.normalization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.cytoscape.idmapper.normalization.CurieNormalizer.PreparedIdentifiers;
import org.junit.Test;

public class CurieNormalizerTest {

    @Test
    public void recognizesAlreadyPrefixedCuries() {
        assertTrue(CurieNormalizer.hasCuriePrefix("NCBIGene:7157"));
        assertTrue(CurieNormalizer.hasCuriePrefix(" HGNC:11998 "));
        assertFalse(CurieNormalizer.hasCuriePrefix("7157"));
    }

    @Test
    public void appliesUserPrefixOnlyWhenMissing() {
        assertEquals("NCBIGene:7157", CurieNormalizer.toCurie("7157", "NCBIGene"));
        assertEquals("NCBIGene::7157", CurieNormalizer.toCurie("7157", "NCBIGene:"));
        assertEquals("HGNC:11998", CurieNormalizer.toCurie("HGNC:11998", "NCBIGene"));
    }

    @Test
    public void trimsAndIgnoresBlankValues() {
        assertEquals("HGNC:11998", CurieNormalizer.toCurie(" HGNC:11998 ", null));
        assertNull(CurieNormalizer.toCurie(null, "HGNC"));
        assertNull(CurieNormalizer.toCurie("   ", "HGNC"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMalformedPrefix() {
        CurieNormalizer.normalizePrefix("bad prefix");
    }

    @Test
    public void deduplicatesIdentifiersAndRetainsRows() {
        final PreparedIdentifiers prepared = CurieNormalizer.prepare(
                Arrays.asList("7157", " NCBIGene:7157 ", "", null, "11998"), "NCBIGene");

        assertEquals(5, prepared.getRowsExamined());
        assertEquals(Arrays.asList("NCBIGene:7157", "NCBIGene:11998"), prepared.getUniqueCuries());
        assertEquals(Arrays.asList(Integer.valueOf(0), Integer.valueOf(1)),
                prepared.getRowsByCurie().get("NCBIGene:7157"));
        assertEquals(Arrays.asList(Integer.valueOf(4)), prepared.getRowsByCurie().get("NCBIGene:11998"));
    }

    @Test
    public void splitsBatches() {
        final List<List<String>> batches = CurieNormalizer.splitBatches(Arrays.asList("a", "b", "c", "d", "e"), 2);
        assertEquals(3, batches.size());
        assertEquals(Arrays.asList("a", "b"), batches.get(0));
        assertEquals(Arrays.asList("c", "d"), batches.get(1));
        assertEquals(Arrays.asList("e"), batches.get(2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonPositiveBatchSize() {
        CurieNormalizer.splitBatches(Arrays.asList("a"), 0);
    }
}
