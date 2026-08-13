package com.bliss.aimemorysearch.ai;

import static org.junit.Assert.assertEquals;

import com.bliss.aimemorysearch.db.FileEntity;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ImageSemanticSearchEngineCutoffTest {

    @Test
    public void bestScoreBelowPointTwentySixIsNotAutomaticallyRejected() {
        List<ImageSemanticSearchEngine.SearchResult> filtered =
                ImageSemanticSearchEngine.filterByAdaptiveThreshold(
                        results(0.2500f, 0.2300f, 0.2199f),
                        0.2500f
                );

        assertEquals(2, filtered.size());
    }

    @Test
    public void adaptiveThresholdStillFiltersNormally() {
        List<ImageSemanticSearchEngine.SearchResult> filtered =
                ImageSemanticSearchEngine.filterByAdaptiveThreshold(
                        results(0.3000f, 0.2600f, 0.2549f),
                        0.3000f
                );

        assertEquals(2, filtered.size());
    }

    @Test
    public void weakDistributionCanBecomeEmptyOnlyThroughAdaptiveFiltering() {
        List<ImageSemanticSearchEngine.SearchResult> filtered =
                ImageSemanticSearchEngine.filterByAdaptiveThreshold(
                        results(0.2199f, 0.2100f, 0.2000f),
                        0.2199f
                );

        assertEquals(0, filtered.size());
    }

    @Test
    public void veryLargeGapAfterFirstKeepsTopGroup() {
        assertEquals(1, cutoff(0.3009f, 0.1914f, 0.1796f, 0.1771f, 0.1751f));
    }

    @Test
    public void twoCloseResultsThenLargeGapKeepsFirstTwo() {
        assertEquals(2, cutoff(0.2815f, 0.2722f, 0.2087f, 0.2034f, 0.1971f));
    }

    @Test
    public void compactDistributionIsNotCutAggressively() {
        assertEquals(5, cutoff(0.2603f, 0.2582f, 0.2540f, 0.2501f, 0.2462f));
    }

    @Test
    public void nearlyEqualScoresAreAllKept() {
        assertEquals(5, cutoff(0.2504f, 0.2503f, 0.2502f, 0.2501f, 0.2500f));
    }

    @Test
    public void imageCutoffDoesNotRemoveMixedModalityDocuments() {
        FileEntity firstDocument = file("document-1", "DOCUMENT");
        FileEntity secondDocument = file("document-2", "DOCUMENT");
        List<FileEntity> mixed = new ArrayList<>(Arrays.asList(
                firstDocument,
                secondDocument
        ));
        List<ImageSemanticSearchEngine.SearchResult> images = results(
                0.3009f, 0.1914f, 0.1796f, 0.1771f, 0.1751f);

        int imageCutoff = ImageSemanticSearchEngine.adaptiveImageCutoff(images);
        for (int index = 0; index < imageCutoff; index++) {
            mixed.add(images.get(index).file);
        }

        assertEquals(3, mixed.size());
        assertEquals(firstDocument, mixed.get(0));
        assertEquals(secondDocument, mixed.get(1));
        assertEquals("IMAGE", mixed.get(2).type);
    }

    private static int cutoff(float... scores) {
        return ImageSemanticSearchEngine.adaptiveImageCutoff(results(scores));
    }

    private static List<ImageSemanticSearchEngine.SearchResult> results(
            float... scores
    ) {
        List<ImageSemanticSearchEngine.SearchResult> output = new ArrayList<>();
        for (int index = 0; index < scores.length; index++) {
            output.add(new ImageSemanticSearchEngine.SearchResult(
                    file("image-" + index, "IMAGE"),
                    scores[index]
            ));
        }
        return output;
    }

    private static FileEntity file(String path, String type) {
        return new FileEntity(
                path, path, type, null, null,
                0L, 0L, 0L, null, null, null, null
        );
    }
}
