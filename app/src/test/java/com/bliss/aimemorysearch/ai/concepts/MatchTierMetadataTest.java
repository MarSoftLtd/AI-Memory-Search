package com.bliss.aimemorysearch.ai.concepts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.bliss.aimemorysearch.SearchExplanationHolder;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

public class MatchTierMetadataTest {

    @Test
    public void retrievedResultCarriesOptionalTierWithoutChangingScore() {
        InterpretationExecutionResult.RetrievedResult tiered =
                new InterpretationExecutionResult.RetrievedResult(
                        "/image.jpg",
                        InterpretationExecutionResult.Modality.IMAGE,
                        0.75f,
                        MatchTier.BEST_MATCH
                );
        InterpretationExecutionResult.RetrievedResult legacy =
                new InterpretationExecutionResult.RetrievedResult(
                        "/document.pdf",
                        InterpretationExecutionResult.Modality.DOCUMENT,
                        0.5f
                );

        assertEquals(0.75f, tiered.getScore(), 0f);
        assertEquals(MatchTier.BEST_MATCH, tiered.getMatchTier());
        assertNull(legacy.getMatchTier());
    }

    @Test
    public void holderDefensivelyCopiesTierMetadata() {
        Map<String, MatchTier> source = new LinkedHashMap<>();
        source.put("/one.jpg", MatchTier.PARTIAL_MATCH);

        SearchExplanationHolder.setImageMatchTiers(source);
        source.clear();

        assertEquals(
                MatchTier.PARTIAL_MATCH,
                SearchExplanationHolder.getImageMatchTiers().get("/one.jpg")
        );
        try {
            SearchExplanationHolder.getImageMatchTiers().clear();
            throw new AssertionError("Expected immutable tier metadata");
        } catch (UnsupportedOperationException expected) {
            // Expected.
        } finally {
            SearchExplanationHolder.clear();
        }
    }
}
