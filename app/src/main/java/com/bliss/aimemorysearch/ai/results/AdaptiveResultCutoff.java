package com.bliss.aimemorysearch.ai.results;

import com.bliss.aimemorysearch.ai.concepts.MatchTier;
import com.bliss.aimemorysearch.ai.evidence.DocumentConfidence;
import com.bliss.aimemorysearch.db.FileEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Selects a display prefix from existing ordered result observations. */
public final class AdaptiveResultCutoff {

    private AdaptiveResultCutoff() {}

    public static Result preserveOriginalResults(
            List<FileEntity> orderedResults
    ) {
        int count = orderedResults == null ? 0 : orderedResults.size();
        return new Result(count, 0, Reason.ORIGINAL_PRODUCTION_PIPELINE);
    }

    public static Result evaluate(
            List<FileEntity> orderedResults,
            Map<String, Float> scores,
            Map<String, DocumentConfidence> confidences,
            Map<String, MatchTier> matchTiers,
            Map<String, Float> agreementByPath
    ) {
        return evaluate(
                orderedResults,
                scores,
                confidences,
                matchTiers,
                agreementByPath,
                false
        );
    }

    public static Result evaluate(
            List<FileEntity> orderedResults,
            Map<String, Float> scores,
            Map<String, DocumentConfidence> confidences,
            Map<String, MatchTier> matchTiers,
            Map<String, Float> agreementByPath,
            boolean bestMatchOnly
    ) {
        if (bestMatchOnly) {
            int bestMatches = 0;
            int total = orderedResults == null ? 0 : orderedResults.size();
            if (orderedResults != null) {
                for (FileEntity result : orderedResults) {
                    if (matchTiers.get(result.path) == MatchTier.BEST_MATCH) {
                        bestMatches++;
                    }
                }
            }
            return new Result(
                    bestMatches,
                    total - bestMatches,
                    Reason.BEST_MATCH_ONLY
            );
        }
        if (orderedResults == null || orderedResults.size() <= 2) {
            int count = orderedResults == null ? 0 : orderedResults.size();
            return new Result(count, 0, Reason.ALL_RESULTS_MEANINGFUL);
        }

        int tierBoundary = cleanTierBoundary(orderedResults, matchTiers);
        if (tierBoundary > 0) {
            return new Result(
                    tierBoundary,
                    orderedResults.size() - tierBoundary,
                    Reason.MATCH_TIER_BOUNDARY
            );
        }

        List<Float> utilities = utilities(
                orderedResults,
                scores,
                confidences,
                matchTiers,
                agreementByPath
        );
        int elbow = distributionElbow(utilities);
        if (elbow > 0) {
            return new Result(
                    elbow,
                    orderedResults.size() - elbow,
                    Reason.DISTRIBUTION_ELBOW
            );
        }
        return new Result(
                orderedResults.size(),
                0,
                Reason.ALL_RESULTS_MEANINGFUL
        );
    }

    private static int cleanTierBoundary(
            List<FileEntity> results,
            Map<String, MatchTier> matchTiers
    ) {
        for (FileEntity result : results) {
            if (!isImage(result) || !matchTiers.containsKey(result.path)) {
                return -1;
            }
        }
        int leadingTier = tierRank(matchTiers.get(results.get(0).path));
        if (leadingTier <= tierRank(MatchTier.RELATED_MATCH)) return -1;
        for (int boundary = 2; boundary < results.size(); boundary++) {
            if (tierRank(matchTiers.get(results.get(boundary).path))
                    < leadingTier) {
                return boundary;
            }
        }
        return -1;
    }

    private static List<Float> utilities(
            List<FileEntity> results,
            Map<String, Float> scores,
            Map<String, DocumentConfidence> confidences,
            Map<String, MatchTier> matchTiers,
            Map<String, Float> agreementByPath
    ) {
        float minimumScore = Float.POSITIVE_INFINITY;
        float maximumScore = Float.NEGATIVE_INFINITY;
        for (FileEntity result : results) {
            Float score = scores.get(result.path);
            if (score == null || !Float.isFinite(score)) continue;
            minimumScore = Math.min(minimumScore, score);
            maximumScore = Math.max(maximumScore, score);
        }
        boolean scoreAvailable = Float.isFinite(minimumScore)
                && Float.isFinite(maximumScore);
        float scoreRange = maximumScore - minimumScore;

        List<Float> output = new ArrayList<>(results.size());
        for (FileEntity result : results) {
            float weightedValue = 0f;
            float totalWeight = 0f;
            Float score = scores.get(result.path);
            if (scoreAvailable && score != null && Float.isFinite(score)) {
                float normalized = scoreRange == 0f
                        ? 1f : (score - minimumScore) / scoreRange;
                weightedValue += normalized * 2f;
                totalWeight += 2f;
            }
            DocumentConfidence confidence = confidences.get(result.path);
            if (confidence != null
                    && confidence.getConfidence() != null
                    && confidence.getConfidence().isAvailable()) {
                weightedValue += confidence.getConfidence().getValue();
                totalWeight += 1f;
            }
            MatchTier tier = matchTiers.get(result.path);
            if (tier != null) {
                weightedValue += tierValue(tier) * 3f;
                totalWeight += 3f;
            }
            Float agreement = agreementByPath.get(result.path);
            if (agreement != null && Float.isFinite(agreement)) {
                weightedValue += clamp(agreement) * 2f;
                totalWeight += 2f;
            }
            output.add(totalWeight == 0f ? 0f : weightedValue / totalWeight);
        }
        return output;
    }

    private static int distributionElbow(List<Float> utilities) {
        List<Float> positiveDrops = new ArrayList<>();
        float largestDrop = 0f;
        int largestBoundary = -1;
        for (int index = 1; index < utilities.size(); index++) {
            float drop = utilities.get(index - 1) - utilities.get(index);
            if (drop > 0f) positiveDrops.add(drop);
            if (index >= 2 && drop > largestDrop) {
                largestDrop = drop;
                largestBoundary = index;
            }
        }
        if (largestBoundary < 0 || positiveDrops.isEmpty()) return -1;
        float median = median(positiveDrops);
        List<Float> deviations = new ArrayList<>(positiveDrops.size());
        for (float drop : positiveDrops) {
            deviations.add(Math.abs(drop - median));
        }
        float medianDeviation = median(deviations);
        return largestDrop > median + medianDeviation
                ? largestBoundary : -1;
    }

    private static float median(List<Float> values) {
        List<Float> ordered = new ArrayList<>(values);
        Collections.sort(ordered);
        int middle = ordered.size() / 2;
        if ((ordered.size() & 1) == 1) return ordered.get(middle);
        return (ordered.get(middle - 1) + ordered.get(middle)) / 2f;
    }

    private static boolean isImage(FileEntity result) {
        return result != null
                && "IMAGE".equalsIgnoreCase(result.type);
    }

    private static int tierRank(MatchTier tier) {
        if (tier == MatchTier.BEST_MATCH) return 3;
        if (tier == MatchTier.PARTIAL_MATCH) return 2;
        return 1;
    }

    private static float tierValue(MatchTier tier) {
        if (tier == MatchTier.BEST_MATCH) return 1f;
        if (tier == MatchTier.PARTIAL_MATCH) return 0.5f;
        return 0f;
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    public static final class Result {
        private final int displayedCount;
        private final int hiddenCount;
        private final Reason reason;

        Result(int displayedCount, int hiddenCount, Reason reason) {
            this.displayedCount = displayedCount;
            this.hiddenCount = hiddenCount;
            this.reason = reason;
        }

        public int getDisplayedCount() { return displayedCount; }
        public int getHiddenCount() { return hiddenCount; }
        public Reason getReason() { return reason; }
    }

    public enum Reason {
        ORIGINAL_PRODUCTION_PIPELINE,
        BEST_MATCH_ONLY,
        MATCH_TIER_BOUNDARY,
        DISTRIBUTION_ELBOW,
        ALL_RESULTS_MEANINGFUL
    }
}
