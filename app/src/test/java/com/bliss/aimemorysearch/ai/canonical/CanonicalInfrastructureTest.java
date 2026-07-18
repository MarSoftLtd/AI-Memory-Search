package com.bliss.aimemorysearch.ai.canonical;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CanonicalInfrastructureTest {

    @Test
    public void canonicalHashIsStableAndVersioned() {
        CanonicalHash first = CanonicalHash.fromCanonicalText(
                "  Water   Bill  ",
                "norm-v1"
        );
        CanonicalHash second = CanonicalHash.fromCanonicalText(
                "water bill",
                "norm-v1"
        );
        CanonicalHash differentVersion =
                CanonicalHash.fromCanonicalText(
                        "water bill",
                        "norm-v2"
                );

        assertEquals(first, second);
        assertNotEquals(first, differentVersion);
        assertEquals(32, first.toString().length());
        assertEquals(first, CanonicalHash.fromBytes(first.toBytes(), 0));
    }

    @Test
    public void vocabularyCachePersistsAndScopesEntries() throws Exception {
        File directory = Files.createTempDirectory(
                "canonical-cache-test"
        ).toFile();
        File cacheFile = new File(directory, "vocabulary.bin");
        CanonicalTranslationMetadata german = metadata(
                "de",
                "germanic",
                0.96f,
                "model-de-v1"
        );
        CanonicalTranslationMetadata french = metadata(
                "fr",
                "romance",
                0.91f,
                "model-fr-v1"
        );
        CanonicalHash water = CanonicalHash.fromCanonicalText(
                "water bill",
                "norm-v1"
        );

        try (CanonicalVocabularyCache cache =
                new CanonicalVocabularyCache(cacheFile)) {
            cache.insert(
                    "Wasserrechnung",
                    Arrays.asList(water),
                    german
            );
            cache.insert(
                    "facture d'eau",
                    Arrays.asList(water),
                    french
            );
            assertEquals(2, cache.size());
            assertEquals(2L, cache.version());
            assertNotNull(cache.lookup("Wasserrechnung", german));
            assertNull(cache.lookup("Wasserrechnung", french));
        }

        try (CanonicalVocabularyCache reopened =
                new CanonicalVocabularyCache(cacheFile)) {
            assertEquals(2, reopened.size());
            assertEquals(2L, reopened.version());
            CanonicalVocabularyEntry entry = reopened.lookup(
                    "Wasserrechnung",
                    german
            );
            assertNotNull(entry);
            assertEquals(water, entry.getCanonicalHashes().get(0));
            assertEquals(0.96f,
                    entry.getMetadata().getConfidence(),
                    0.0001f);

            reopened.update(
                    "Wasserrechnung",
                    Arrays.asList(
                            water,
                            CanonicalHash.fromCanonicalText(
                                    "utility bill",
                                    "norm-v1"
                            )
                    ),
                    german
            );
            assertEquals(3L, reopened.version());
            assertEquals(1, reopened.invalidate(
                    "de",
                    "germanic",
                    "norm-v1",
                    "model-de-v1"
            ));
            assertEquals(4L, reopened.version());
            assertNull(reopened.lookup("Wasserrechnung", german));
            assertNotNull(reopened.lookup("facture d'eau", french));
        }

        assertTrue(cacheFile.isFile());
        assertTrue(cacheFile.length() > 0L);
    }

    @Test
    public void postingCodecRoundTripsSortedChunkPostings()
            throws Exception {
        List<CanonicalPosting> input = Arrays.asList(
                new CanonicalPosting("/b.pdf", 9, 2, 3),
                new CanonicalPosting("/a.pdf", 5, 1, 1),
                new CanonicalPosting("/a.pdf", 140, 300, 255)
        );

        byte[] encoded = CanonicalPostingCodec.encode(input);
        List<CanonicalPosting> decoded =
                CanonicalPostingCodec.decode(encoded);

        assertTrue(encoded.length > 0);
        assertEquals(3, decoded.size());
        assertPosting(decoded.get(0), "/a.pdf", 5, 1, 1);
        assertPosting(decoded.get(1), "/a.pdf", 140, 300, 255);
        assertPosting(decoded.get(2), "/b.pdf", 9, 2, 3);
    }

    @Test
    public void postingCodecRejectsDuplicateChunkIdentities() {
        try {
            CanonicalPostingCodec.encode(Arrays.asList(
                    new CanonicalPosting("/a.pdf", 10, 1, 0),
                    new CanonicalPosting("/a.pdf", 10, 2, 0)
            ));
            fail("Duplicate chunk identities must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("unique"));
        }
    }

    @Test
    public void vocabularyExtractorKeepsTermsAndContextualBigrams() {
        Map<CanonicalHash, Integer> evidence =
                new CanonicalVocabularyExtractor().extract(
                        "Water bill and gas bill"
                );

        assertTrue(evidence.containsKey(CanonicalHash.fromCanonicalText(
                "water",
                CanonicalVocabularyExtractor.NORMALIZATION_VERSION
        )));
        assertTrue(evidence.containsKey(CanonicalHash.fromCanonicalText(
                "water bill",
                CanonicalVocabularyExtractor.NORMALIZATION_VERSION
        )));
        assertTrue(evidence.containsKey(CanonicalHash.fromCanonicalText(
                "gas bill",
                CanonicalVocabularyExtractor.NORMALIZATION_VERSION
        )));
        assertEquals(Integer.valueOf(1), evidence.get(
                CanonicalHash.fromCanonicalText(
                        "bill",
                        CanonicalVocabularyExtractor.NORMALIZATION_VERSION
                )));
    }

    private static CanonicalTranslationMetadata metadata(
            String language,
            String family,
            float confidence,
            String modelVersion
    ) {
        return new CanonicalTranslationMetadata(
                language,
                family,
                confidence,
                "norm-v1",
                modelVersion
        );
    }

    private static void assertPosting(
            CanonicalPosting posting,
            String filePath,
            int chunkIndex,
            int frequency,
            int flags
    ) {
        assertEquals(filePath, posting.getFilePath());
        assertEquals(chunkIndex, posting.getChunkIndex());
        assertEquals(frequency, posting.getTermFrequency());
        assertEquals(flags, posting.getFieldFlags());
    }
}
