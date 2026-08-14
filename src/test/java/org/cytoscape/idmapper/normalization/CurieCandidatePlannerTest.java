package org.cytoscape.idmapper.normalization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class CurieCandidatePlannerTest {

    private final CurieCandidatePlanner planner = new CurieCandidatePlanner(new IdentifierFormatClassifier());
    private final List<CuriePrefixSuggestion> suggestions = Arrays.asList(
            new CuriePrefixSuggestion("UniProtKB", 1000L),
            new CuriePrefixSuggestion("NCBIGene", 900L),
            new CuriePrefixSuggestion("ENSEMBL", 800L),
            new CuriePrefixSuggestion("HGNC", 700L),
            new CuriePrefixSuggestion("RGD", 600L));

    @Test
    public void existingPrefixedValuesAreNotExpanded() {
        final CurieCandidatePlan plan = planner.plan(Arrays.asList("HGNC:11998"), null, true, suggestions, 25, true);

        assertEquals(Arrays.asList("HGNC:11998"), plan.getPreparedIdentifiers().get(0).getCandidateCuries());
        assertEquals(0, plan.getRowsWithPrefixGuesses());
    }

    @Test
    public void guessOffPreservesCurrentSingleCandidateBehavior() {
        final CurieCandidatePlan plan = planner.plan(Arrays.asList("7157"), "NCBIGene", false, suggestions, 25, true);

        assertEquals(Arrays.asList("NCBIGene:7157"), plan.getPreparedIdentifiers().get(0).getCandidateCuries());
        assertEquals(Arrays.asList("NCBIGene:7157"), plan.getUniqueCandidateCuries());
    }

    @Test
    public void userPrefixIsFirstEvenWhenFormatSuggestsAnotherPrefix() {
        final CurieCandidatePlan plan = planner.plan(Arrays.asList("ENSG00000141510"), "HGNC", true, suggestions, 25,
                true);

        assertEquals("HGNC:ENSG00000141510", plan.getPreparedIdentifiers().get(0).getCandidateCuries().get(0));
        assertEquals("ENSEMBL:ENSG00000141510", plan.getPreparedIdentifiers().get(0).getCandidateCuries().get(1));
        assertEquals(1, plan.getRowsWithFormatFilteredGuesses());
    }

    @Test
    public void uniprotLikeValuesPreferUniprot() {
        final CurieCandidatePlan plan = planner.plan(Arrays.asList("P04637"), null, true, suggestions, 25, true);

        assertEquals(Arrays.asList("UniProtKB:P04637"), plan.getPreparedIdentifiers().get(0).getCandidateCuries());
        assertEquals(1, plan.getRowsWithFormatFilteredGuesses());
    }

    @Test
    public void numericValuesUseNumericCompatiblePrefixes() {
        final CurieCandidatePlan plan = planner.plan(Arrays.asList("7157"), null, true, suggestions, 25, true);

        final List<String> candidates = plan.getPreparedIdentifiers().get(0).getCandidateCuries();
        assertTrue(candidates.contains("NCBIGene:7157"));
        assertTrue(candidates.contains("HGNC:7157"));
        assertTrue(candidates.contains("RGD:7157"));
        assertFalse(candidates.contains("UniProtKB:7157"));
    }

    @Test
    public void observedColumnPrefixesArePromoted() {
        final CurieCandidatePlan plan = planner.plan(Arrays.asList("NCBIGene:1", "NCBIGene:2", "7157"), null, true,
                suggestions, 25, true);

        assertEquals("NCBIGene:7157", plan.getPreparedIdentifiers().get(2).getCandidateCuries().get(0));
    }

    @Test
    public void maxPrefixGuessesIsEnforced() {
        final CurieCandidatePlan plan = planner.plan(Arrays.asList("7157"), "HGNC", true, suggestions, 2, true);

        assertEquals(2, plan.getPreparedIdentifiers().get(0).getCandidateCuries().size());
    }

    @Test
    public void formatMissFallsBackToCatalogOrder() {
        final CurieCandidatePlan plan = planner.plan(Arrays.asList("notARecognizedShape"), null, true, suggestions, 3,
                true);

        assertEquals("UniProtKB:notARecognizedShape", plan.getPreparedIdentifiers().get(0).getCandidateCuries().get(0));
        assertEquals("NCBIGene:notARecognizedShape", plan.getPreparedIdentifiers().get(0).getCandidateCuries().get(1));
    }
}
