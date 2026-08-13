package com.bliss.aimemorysearch.ai.concepts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import org.junit.Test;

public class BatchAtomicImageRetrieverTest {

    @Test
    public void emptyInputDoesNotScanCorpus() {
        BatchAtomicImageRetriever.BatchResult result =
                BatchAtomicImageRetriever.search(
                        null,
                        Collections.emptyMap(),
                        10
                );

        assertEquals(0, result.getCorpusScans());
        assertEquals(0, result.getIndexedImageCount());
        assertTrue(result.resultsFor("cat").isEmpty());
    }
}
