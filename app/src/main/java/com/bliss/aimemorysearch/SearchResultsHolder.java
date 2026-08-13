package com.bliss.aimemorysearch;

import com.bliss.aimemorysearch.db.FileEntity;
import com.bliss.aimemorysearch.ai.concepts.MatchTier;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

public class SearchResultsHolder {

    public static List<FileEntity> results;
    private static List<FileEntity> allResults = Collections.emptyList();
    private static List<FileEntity> hiddenResults = Collections.emptyList();
    private static List<FileEntity> hiddenPartialMatches =
            Collections.emptyList();
    private static List<FileEntity> hiddenRelatedMatches =
            Collections.emptyList();

    public static void setResults(
            List<FileEntity> orderedResults,
            int displayedCount
    ) {
        List<FileEntity> all = new ArrayList<>(orderedResults);
        int cutoff = Math.max(0, Math.min(displayedCount, all.size()));
        allResults = Collections.unmodifiableList(all);
        results = Collections.unmodifiableList(
                new ArrayList<>(all.subList(0, cutoff))
        );
        hiddenResults = Collections.unmodifiableList(
                new ArrayList<>(all.subList(cutoff, all.size()))
        );
        hiddenPartialMatches = Collections.emptyList();
        hiddenRelatedMatches = Collections.emptyList();
    }

    public static void setBestMatchResults(
            List<FileEntity> orderedResults,
            Map<String, MatchTier> matchTiers
    ) {
        List<FileEntity> all = new ArrayList<>(orderedResults);
        List<FileEntity> best = new ArrayList<>();
        List<FileEntity> hidden = new ArrayList<>();
        List<FileEntity> partial = new ArrayList<>();
        List<FileEntity> related = new ArrayList<>();
        for (FileEntity result : all) {
            MatchTier tier = matchTiers.get(result.path);
            if (tier == MatchTier.BEST_MATCH) {
                best.add(result);
            } else {
                hidden.add(result);
                if (tier == MatchTier.PARTIAL_MATCH) partial.add(result);
                else related.add(result);
            }
        }
        allResults = Collections.unmodifiableList(all);
        results = Collections.unmodifiableList(best);
        hiddenResults = Collections.unmodifiableList(hidden);
        hiddenPartialMatches = Collections.unmodifiableList(partial);
        hiddenRelatedMatches = Collections.unmodifiableList(related);
    }

    public static List<FileEntity> getAllResults() { return allResults; }
    public static List<FileEntity> getHiddenResults() { return hiddenResults; }
    public static List<FileEntity> getHiddenPartialMatches() {
        return hiddenPartialMatches;
    }
    public static List<FileEntity> getHiddenRelatedMatches() {
        return hiddenRelatedMatches;
    }
}
