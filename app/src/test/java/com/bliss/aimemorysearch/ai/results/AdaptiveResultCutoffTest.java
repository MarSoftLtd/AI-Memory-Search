package com.bliss.aimemorysearch.ai.results;

import static org.junit.Assert.assertEquals;

import com.bliss.aimemorysearch.SearchResultsHolder;
import com.bliss.aimemorysearch.ai.concepts.MatchTier;
import com.bliss.aimemorysearch.db.FileEntity;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AdaptiveResultCutoffTest {

    @Test
    public void cleanTierBoundaryDisplaysBestPrefix() {
        List<FileEntity> results = images(5);
        Map<String, MatchTier> tiers = new LinkedHashMap<>();
        tiers.put("/0.jpg", MatchTier.BEST_MATCH);
        tiers.put("/1.jpg", MatchTier.BEST_MATCH);
        tiers.put("/2.jpg", MatchTier.PARTIAL_MATCH);
        tiers.put("/3.jpg", MatchTier.PARTIAL_MATCH);
        tiers.put("/4.jpg", MatchTier.RELATED_MATCH);

        AdaptiveResultCutoff.Result cutoff = AdaptiveResultCutoff.evaluate(
                results,
                descendingScores(results),
                Collections.emptyMap(),
                tiers,
                Collections.emptyMap()
        );

        assertEquals(2, cutoff.getDisplayedCount());
        assertEquals(3, cutoff.getHiddenCount());
        assertEquals(AdaptiveResultCutoff.Reason.MATCH_TIER_BOUNDARY,
                cutoff.getReason());
    }

    @Test
    public void equalObservationsKeepAllResults() {
        List<FileEntity> results = images(4);
        Map<String, Float> scores = new LinkedHashMap<>();
        Map<String, MatchTier> tiers = new LinkedHashMap<>();
        for (FileEntity result : results) {
            scores.put(result.path, 1f);
            tiers.put(result.path, MatchTier.PARTIAL_MATCH);
        }

        AdaptiveResultCutoff.Result cutoff = AdaptiveResultCutoff.evaluate(
                results,
                scores,
                Collections.emptyMap(),
                tiers,
                Collections.emptyMap()
        );

        assertEquals(4, cutoff.getDisplayedCount());
        assertEquals(0, cutoff.getHiddenCount());
        assertEquals(AdaptiveResultCutoff.Reason.ALL_RESULTS_MEANINGFUL,
                cutoff.getReason());
    }

    @Test
    public void lowerTierStartsCutoffEvenWhenLaterFallbackHasHigherTier() {
        List<FileEntity> results = images(5);
        Map<String, MatchTier> tiers = new LinkedHashMap<>();
        tiers.put("/0.jpg", MatchTier.PARTIAL_MATCH);
        tiers.put("/1.jpg", MatchTier.PARTIAL_MATCH);
        tiers.put("/2.jpg", MatchTier.RELATED_MATCH);
        tiers.put("/3.jpg", MatchTier.RELATED_MATCH);
        tiers.put("/4.jpg", MatchTier.PARTIAL_MATCH);

        AdaptiveResultCutoff.Result cutoff = AdaptiveResultCutoff.evaluate(
                results,
                descendingScores(results),
                Collections.emptyMap(),
                tiers,
                Collections.emptyMap()
        );

        assertEquals(2, cutoff.getDisplayedCount());
        assertEquals(3, cutoff.getHiddenCount());
        assertEquals(AdaptiveResultCutoff.Reason.MATCH_TIER_BOUNDARY,
                cutoff.getReason());
    }

    @Test
    public void hiddenResultsRemainAvailableInOriginalOrder() {
        List<FileEntity> results = images(5);

        SearchResultsHolder.setResults(results, 2);

        assertEquals(2, SearchResultsHolder.results.size());
        assertEquals(5, SearchResultsHolder.getAllResults().size());
        assertEquals("/2.jpg",
                SearchResultsHolder.getHiddenResults().get(0).path);
        assertEquals("/4.jpg",
                SearchResultsHolder.getHiddenResults().get(2).path);
    }

    @Test
    public void bestOnlyModeNeverPromotesPartialOrRelatedMatches() {
        List<FileEntity> results = images(4);
        Map<String, MatchTier> tiers = new LinkedHashMap<>();
        tiers.put("/0.jpg", MatchTier.PARTIAL_MATCH);
        tiers.put("/1.jpg", MatchTier.RELATED_MATCH);
        tiers.put("/2.jpg", MatchTier.BEST_MATCH);
        tiers.put("/3.jpg", MatchTier.PARTIAL_MATCH);

        AdaptiveResultCutoff.Result cutoff = AdaptiveResultCutoff.evaluate(
                results,
                descendingScores(results),
                Collections.emptyMap(),
                tiers,
                Collections.emptyMap(),
                true
        );
        SearchResultsHolder.setBestMatchResults(results, tiers);

        assertEquals(1, cutoff.getDisplayedCount());
        assertEquals(3, cutoff.getHiddenCount());
        assertEquals(AdaptiveResultCutoff.Reason.BEST_MATCH_ONLY,
                cutoff.getReason());
        assertEquals("/2.jpg", SearchResultsHolder.results.get(0).path);
        assertEquals(2,
                SearchResultsHolder.getHiddenPartialMatches().size());
        assertEquals(1,
                SearchResultsHolder.getHiddenRelatedMatches().size());
        assertEquals("/0.jpg",
                SearchResultsHolder.getHiddenResults().get(0).path);
        assertEquals("/3.jpg",
                SearchResultsHolder.getHiddenResults().get(2).path);
    }

    @Test
    public void singleConceptImagePipelineKeepsEveryOriginalResult() {
        List<FileEntity> results = images(10);

        AdaptiveResultCutoff.Result cutoff =
                AdaptiveResultCutoff.preserveOriginalResults(results);

        assertEquals(10, cutoff.getDisplayedCount());
        assertEquals(0, cutoff.getHiddenCount());
        assertEquals(
                AdaptiveResultCutoff.Reason.ORIGINAL_PRODUCTION_PIPELINE,
                cutoff.getReason()
        );
    }

    private static List<FileEntity> images(int count) {
        List<FileEntity> output = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            FileEntity file = new FileEntity(
                    "/" + index + ".jpg",
                    index + ".jpg",
                    "IMAGE",
                    "/" + index + ".jpg",
                    null,
                    0L,
                    0L,
                    0L,
                    null,
                    null,
                    null,
                    null
            );
            output.add(file);
        }
        return output;
    }

    private static Map<String, Float> descendingScores(
            List<FileEntity> results
    ) {
        Map<String, Float> output = new LinkedHashMap<>();
        for (int index = 0; index < results.size(); index++) {
            output.put(results.get(index).path, (float) (results.size() - index));
        }
        return output;
    }
}
