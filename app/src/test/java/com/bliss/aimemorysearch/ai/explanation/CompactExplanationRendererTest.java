package com.bliss.aimemorysearch.ai.explanation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.bliss.aimemorysearch.SearchExplanationHolder;
import com.bliss.aimemorysearch.ai.concepts.Interpretation;
import com.bliss.aimemorysearch.ai.concepts.InterpretationExecutionResult;
import com.bliss.aimemorysearch.ai.concepts.MatchTier;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CompactExplanationRendererTest {

    private final CompactExplanationRenderer renderer =
            new CompactExplanationRenderer();
    private Locale originalLocale;

    @Before
    public void saveLocale() {
        originalLocale = Locale.getDefault();
        SearchExplanationHolder.clear();
    }

    @After
    public void restoreLocale() {
        Locale.setDefault(originalLocale);
        SearchExplanationHolder.clear();
    }

    @Test
    public void romanianUsesUserLanguageAndTierMetadata() {
        Locale.setDefault(Locale.forLanguageTag("ro-RO"));
        setTiers(
                MatchTier.BEST_MATCH,
                MatchTier.BEST_MATCH,
                MatchTier.PARTIAL_MATCH,
                MatchTier.RELATED_MATCH
        );

        String output = renderer.render(explanation("trandafiri rosii", 4));

        assertEquals(
                "Ai căutat „trandafiri rosii”. Am găsit 4 rezultate. "
                        + "Dintre imagini, 2 sunt potriviri complete, 1 este o potrivire parțială, "
                        + "iar 1 este un rezultat asociat. Potrivirile complete acoperă întreaga "
                        + "căutare; celelalte corespund doar unei părți.",
                output
        );
        assertAtMostFourSentences(output);
        assertNoInternalTerms(output);
    }

    @Test
    public void englishReportsWhenOnlyPartialMatchesExist() {
        Locale.setDefault(Locale.US);
        setTiers(
                MatchTier.PARTIAL_MATCH,
                MatchTier.PARTIAL_MATCH,
                MatchTier.RELATED_MATCH
        );

        String output = renderer.render(explanation("white cat", 3));

        assertTrue(output.contains("You searched for “white cat”."));
        assertTrue(output.contains("0 are complete matches"));
        assertTrue(output.contains("2 are partial matches"));
        assertTrue(output.contains("No displayed image covers the whole search"));
        assertAtMostFourSentences(output);
        assertNoInternalTerms(output);
    }

    @Test
    public void requiredQueriesRenderDeterministically() {
        Locale.setDefault(Locale.US);
        setTiers(MatchTier.PARTIAL_MATCH);
        List<String> queries = Arrays.asList(
                "white cat",
                "pisica alba",
                "red roses",
                "factura apa",
                "contract Engie"
        );

        for (String query : queries) {
            String first = renderer.render(explanation(query, 1));
            String second = renderer.render(explanation(query, 1));
            assertEquals(first, second);
            assertTrue(first.contains(query));
            assertAtMostFourSentences(first);
            assertNoInternalTerms(first);
        }
    }

    @Test
    public void absentTierMetadataUsesConservativeFallback() {
        Locale.setDefault(Locale.US);

        String output = renderer.render(explanation("contract Engie", 1));

        assertEquals(
                "You searched for “contract Engie”. Found 1 result. "
                        + "These results are shown because they are the closest "
                        + "available matches.",
                output
        );
        assertAtMostFourSentences(output);
        assertNoInternalTerms(output);
    }

    @Test
    public void unavailableExplanationUsesCurrentLocale() {
        Locale.setDefault(Locale.forLanguageTag("ro"));
        assertEquals("Explicația nu este disponibilă.", renderer.render(null));
        Locale.setDefault(Locale.US);
        assertEquals("The explanation is unavailable.", renderer.render(null));
    }

    private static Explanation explanation(String query, int resultCount) {
        List<ResultExplanation> results = new ArrayList<>(resultCount);
        for (int index = 0; index < resultCount; index++) {
            results.add(new ResultExplanation(
                    "image-" + index,
                    "/image-" + index + ".jpg",
                    InterpretationExecutionResult.Modality.IMAGE,
                    index + 1,
                    1f - index * 0.01f,
                    "i1",
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    ResultExplanation.Completeness.COMPLETE
            ));
        }
        return new Explanation(
                "e1",
                "s1",
                "1",
                Explanation.Availability.AVAILABLE,
                new QuerySummary("q1", query, true),
                new InterpretationSummary(
                        "i1",
                        Interpretation.Kind.ATOMIC_CONJUNCTION,
                        Collections.emptyList(),
                        "atomic",
                        false,
                        true,
                        true,
                        null,
                        resultCount,
                        0,
                        resultCount
                ),
                Collections.emptyList(),
                new SelectionSummary(
                        resultCount, 0, resultCount, "i1", null, true
                ),
                results,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                new GenerationMetadata(0L, "1", null)
        );
    }

    private static void setTiers(MatchTier... tiers) {
        Map<String, MatchTier> values = new LinkedHashMap<>();
        for (int index = 0; index < tiers.length; index++) {
            values.put("image-" + index, tiers[index]);
        }
        SearchExplanationHolder.setImageMatchTiers(values);
    }

    private static void assertAtMostFourSentences(String output) {
        int sentences = 0;
        for (int index = 0; index < output.length(); index++) {
            if (output.charAt(index) == '.') sentences++;
        }
        assertTrue("Too many sentences: " + output, sentences <= 4);
    }

    private static void assertNoInternalTerms(String output) {
        String normalized = output.toLowerCase(Locale.ROOT);
        for (String forbidden : Arrays.asList(
                "interpretation",
                "evidence",
                "production order",
                "missing evidence",
                "interpretare",
                "dovezi",
                "ordinea de producție"
        )) {
            assertFalse(forbidden + " in: " + output,
                    normalized.contains(forbidden));
        }
    }
}
