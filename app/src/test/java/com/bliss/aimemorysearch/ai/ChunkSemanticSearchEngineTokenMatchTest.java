package com.bliss.aimemorysearch.ai;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ChunkSemanticSearchEngineTokenMatchTest {

    @Test
    public void requiresWholeTokensInsteadOfWordPrefixes() {
        assertTrue(ChunkSemanticSearchEngine.matchesRequiredToken(
                "red car parked", "car"));
        assertTrue(ChunkSemanticSearchEngine.matchesRequiredToken(
                "apa rece", "apa"));
        assertFalse(ChunkSemanticSearchEngine.matchesRequiredToken(
                "take care", "car"));
        assertFalse(ChunkSemanticSearchEngine.matchesRequiredToken(
                "aparat electronic", "apa"));
        assertFalse(ChunkSemanticSearchEngine.matchesRequiredToken(
                "aparat electronic", "water"));
    }
}
