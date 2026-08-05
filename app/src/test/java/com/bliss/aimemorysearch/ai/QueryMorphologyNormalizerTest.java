package com.bliss.aimemorysearch.ai;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class QueryMorphologyNormalizerTest {

    @Test
    public void normalizesSupportedRomanianFormsToStableLemmas() {
        assertEquivalent("masina", "masini");
        assertEquivalent("floare", "flori");
        assertEquivalent("trandafir", "trandafiri");
        assertEquivalent("copil", "copii");
    }

    @Test
    public void normalizesTokensWithoutChangingUnrelatedWords() {
        assertEquals(
                "masina rosie",
                QueryMorphologyNormalizer.normalize("masini rosie")
        );
        assertEquals(
                "invoice apa",
                QueryMorphologyNormalizer.normalize("invoice apa")
        );
    }

    private static void assertEquivalent(String singular, String plural) {
        assertEquals(
                QueryMorphologyNormalizer.normalize(singular),
                QueryMorphologyNormalizer.normalize(plural)
        );
    }
}
