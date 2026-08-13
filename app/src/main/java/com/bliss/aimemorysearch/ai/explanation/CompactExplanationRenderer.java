package com.bliss.aimemorysearch.ai.explanation;

import com.bliss.aimemorysearch.SearchExplanationHolder;
import com.bliss.aimemorysearch.ai.concepts.InterpretationExecutionResult;
import com.bliss.aimemorysearch.ai.concepts.MatchTier;

import java.util.Locale;
import java.util.Map;

/** Deterministic compact user-facing text for an immutable Explanation. */
public final class CompactExplanationRenderer {

    public String render(Explanation explanation) {
        Locale locale = Locale.getDefault();
        boolean romanian = "ro".equalsIgnoreCase(locale.getLanguage());
        if (explanation == null) {
            return romanian
                    ? "Explicația nu este disponibilă."
                    : "The explanation is unavailable.";
        }

        StringBuilder output = new StringBuilder();
        appendSearch(output, explanation.getQuerySummary(), romanian);
        appendFound(output, explanation, romanian);

        TierCounts tiers = tierCounts(
                explanation,
                SearchExplanationHolder.getImageMatchTiers()
        );
        if (tiers.available) {
            appendTierCounts(output, tiers, romanian);
            appendTierMeaning(output, tiers, romanian);
        } else {
            output.append(romanian
                    ? " Aceste rezultate sunt afișate deoarece sunt cele mai apropiate potriviri disponibile."
                    : " These results are shown because they are the closest available matches.");
        }
        return output.toString();
    }

    private static void appendSearch(
            StringBuilder output,
            QuerySummary query,
            boolean romanian
    ) {
        if (query != null
                && query.isAvailable()
                && query.getNormalizedQuery() != null
                && !query.getNormalizedQuery().trim().isEmpty()) {
            if (romanian) {
                output.append("Ai căutat „")
                        .append(query.getNormalizedQuery())
                        .append("”.");
            } else {
                output.append("You searched for “")
                        .append(query.getNormalizedQuery())
                        .append("”.");
            }
            return;
        }
        output.append(romanian
                ? "Ai căutat în fișierele tale."
                : "You searched your files.");
    }

    private static void appendFound(
            StringBuilder output,
            Explanation explanation,
            boolean romanian
    ) {
        int count = explanation.getResultExplanations().size();
        if (romanian) {
            output.append(" Am găsit ")
                    .append(count)
                    .append(count == 1 ? " rezultat." : " rezultate.");
        } else {
            output.append(" Found ")
                    .append(count)
                    .append(count == 1 ? " result." : " results.");
        }
    }

    private static void appendTierCounts(
            StringBuilder output,
            TierCounts tiers,
            boolean romanian
    ) {
        if (romanian) {
            output.append(" Dintre imagini, ")
                    .append(romanianComplete(tiers.best))
                    .append(", ")
                    .append(romanianPartial(tiers.partial))
                    .append(", iar ")
                    .append(romanianRelated(tiers.related))
                    .append('.');
        } else {
            output.append(" Among the images, ")
                    .append(englishCount(tiers.best, "complete match"))
                    .append(", ")
                    .append(englishCount(tiers.partial, "partial match"))
                    .append(", and ")
                    .append(englishCount(tiers.related, "related match"))
                    .append('.');
        }
    }

    private static String romanianComplete(int count) {
        return count == 1
                ? "1 este o potrivire completă"
                : count + " sunt potriviri complete";
    }

    private static String romanianPartial(int count) {
        return count == 1
                ? "1 este o potrivire parțială"
                : count + " sunt potriviri parțiale";
    }

    private static String romanianRelated(int count) {
        return count == 1
                ? "1 este un rezultat asociat"
                : count + " sunt rezultate asociate";
    }

    private static String englishCount(int count, String singular) {
        return count == 1
                ? "1 is a " + singular
                : count + " are " + singular + "es";
    }

    private static void appendTierMeaning(
            StringBuilder output,
            TierCounts tiers,
            boolean romanian
    ) {
        if (tiers.best > 0) {
            if (tiers.partial + tiers.related == 0) {
                output.append(romanian
                        ? " Toate imaginile afișate acoperă întreaga căutare."
                        : " All displayed images cover the whole search.");
            } else {
                output.append(romanian
                        ? " Potrivirile complete acoperă întreaga căutare; celelalte corespund doar unei părți."
                        : " Complete matches cover the whole search; the others match only part of it.");
            }
        } else if (tiers.partial > 0) {
            output.append(romanian
                    ? " Nicio imagine afișată nu acoperă întreaga căutare; potrivirile parțiale corespund doar unei părți."
                    : " No displayed image covers the whole search; partial matches cover only part of it.");
        } else {
            output.append(romanian
                    ? " Nicio imagine afișată nu acoperă întreaga căutare; sunt afișate doar rezultate asociate."
                    : " No displayed image covers the whole search; only related results are shown.");
        }
    }

    private static TierCounts tierCounts(
            Explanation explanation,
            Map<String, MatchTier> matchTiers
    ) {
        int best = 0;
        int partial = 0;
        int related = 0;
        boolean available = false;
        for (ResultExplanation result : explanation.getResultExplanations()) {
            if (result.getModality()
                    != InterpretationExecutionResult.Modality.IMAGE) {
                continue;
            }
            MatchTier tier = matchTiers.get(result.getResultId());
            if (tier == null) continue;
            available = true;
            if (tier == MatchTier.BEST_MATCH) best++;
            else if (tier == MatchTier.PARTIAL_MATCH) partial++;
            else related++;
        }
        return new TierCounts(available && best + partial > 0,
                best, partial, related);
    }

    private static final class TierCounts {
        final boolean available;
        final int best;
        final int partial;
        final int related;

        TierCounts(boolean available, int best, int partial, int related) {
            this.available = available;
            this.best = best;
            this.partial = partial;
            this.related = related;
        }
    }
}
