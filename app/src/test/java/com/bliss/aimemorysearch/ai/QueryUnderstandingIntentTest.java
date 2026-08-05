package com.bliss.aimemorysearch.ai;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

public class QueryUnderstandingIntentTest {

    @Test
    public void classifiesDocumentQueries() {
        assertIntent(true, false, "invoice");
        assertIntent(true, false, "invoice", "apa");
        assertIntent(true, false, "factura", "engie");
    }

    @Test
    public void classifiesImageQueries() {
        assertIntent(false, true, "poza", "cu", "pisica", "alba");
        assertIntent(false, true, "pisica", "alba");
        assertIntent(false, true, "white", "cat");
        assertIntent(false, true, "red", "roses");
    }

    @Test
    public void keepsAmbiguousQueriesMixed() {
        assertIntent(true, true, "daniela");
        assertIntent(true, true, "church");
        assertIntent(true, true, "biserica");
        assertIntent(true, true, "house");
        assertIntent(true, true, "casa");
    }

    private static void assertIntent(
            boolean documentIntent,
            boolean imageIntent,
            String... tokens
    ) {
        assertEquals(
                documentIntent,
                QueryUnderstandingEngine.detectDocumentIntent(
                        Arrays.asList(tokens)
                )
        );
        assertEquals(
                imageIntent,
                QueryUnderstandingEngine.detectImageIntent(
                        Arrays.asList(tokens)
                )
        );
    }
}
