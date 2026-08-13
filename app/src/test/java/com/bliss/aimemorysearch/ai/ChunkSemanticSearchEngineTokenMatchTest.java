package com.bliss.aimemorysearch.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class ChunkSemanticSearchEngineTokenMatchTest {

    @Test
    public void requiresWholeTokensInsteadOfWordPrefixes() {
        assertTrue(ChunkSemanticSearchEngine.matchesRequiredToken(
                "red car parked", "car", false));
        assertTrue(ChunkSemanticSearchEngine.matchesRequiredToken(
                "apa rece", "apa", false));
        assertFalse(ChunkSemanticSearchEngine.matchesRequiredToken(
                "take care", "car", false));
        assertFalse(ChunkSemanticSearchEngine.matchesRequiredToken(
                "aparat electronic", "apa", false));
        assertFalse(ChunkSemanticSearchEngine.matchesRequiredToken(
                "aparat electronic", "water", false));
    }

    @Test
    public void normalThreeLetterQueryDoesNotMatchAccidentalAcronym() {
        assertFalse(ChunkSemanticSearchEngine.isExplicitAcronymQuery(
                "car", "car"));
        assertFalse(ChunkSemanticSearchEngine.matchesRequiredToken(
                "contractul a ramas", "car", false));
    }

    @Test
    public void explicitUppercaseAcronymKeepsExistingInitialsMatch() {
        assertTrue(ChunkSemanticSearchEngine.isExplicitAcronymQuery(
                "CAR", "car"));
        assertTrue(ChunkSemanticSearchEngine.matchesRequiredToken(
                "contractul a ramas", "car", true));
    }

    @Test
    public void originalTokenIsNotDuplicatedBySemanticExpansion() {
        List<String> merged = ChunkSemanticSearchEngine.mergeExpansionTokens(
                Arrays.asList("car"),
                Arrays.asList("car", "cart")
        );

        assertEquals(Arrays.asList("car", "cart"), merged);
        assertEquals("car", ChunkSemanticSearchEngine.appendDistinctExpansion(
                "car", "car", new String[]{"car"}));
        assertEquals("car cart",
                ChunkSemanticSearchEngine.appendDistinctExpansion(
                        "car", "cart", new String[]{"car"}));
    }
}
