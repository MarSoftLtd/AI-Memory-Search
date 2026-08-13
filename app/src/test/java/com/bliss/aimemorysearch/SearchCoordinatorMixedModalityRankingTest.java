package com.bliss.aimemorysearch;

import static org.junit.Assert.assertEquals;

import com.bliss.aimemorysearch.db.FileEntity;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SearchCoordinatorMixedModalityRankingTest {

    @Test
    public void mixedModalitiesUseRelativeStrengthAndPreserveInternalOrder() {
        FileEntity documentBest = file("document-best", "DOCUMENT");
        FileEntity documentSecond = file("document-second", "DOCUMENT");
        FileEntity imageBest = file("image-best", "IMAGE");
        FileEntity imageSecond = file("image-second", "IMAGE");
        List<FileEntity> results = new ArrayList<>(Arrays.asList(
                documentBest, documentSecond, imageBest, imageSecond));
        Map<String, Float> scores = scores(
                documentBest, 40f, documentSecond, 30f,
                imageBest, 5f, imageSecond, 4f);
        Set<String> images = new HashSet<>(Arrays.asList(
                imageBest.path, imageSecond.path));

        SearchCoordinator.sortFinalResults(
                results, scores, images, 40f, 5f);

        assertEquals(Arrays.asList(
                documentBest, imageBest, imageSecond, documentSecond), results);
    }

    @Test
    public void documentOnlyRetainsRawScoreOrder() {
        assertSingleModalityOrder("DOCUMENT", false);
    }

    @Test
    public void imageOnlyRetainsRawScoreOrder() {
        assertSingleModalityOrder("IMAGE", true);
    }

    private static void assertSingleModalityOrder(
            String type,
            boolean imagesOnly
    ) {
        FileEntity lower = file("lower", type);
        FileEntity higher = file("higher", type);
        List<FileEntity> results = new ArrayList<>(Arrays.asList(lower, higher));
        Map<String, Float> scores = scores(lower, 2f, higher, 3f);
        Set<String> images = imagesOnly
                ? new HashSet<>(Arrays.asList(lower.path, higher.path))
                : new HashSet<>();

        SearchCoordinator.sortFinalResults(
                results, scores, images, imagesOnly ? 0f : 3f,
                imagesOnly ? 3f : 0f);

        assertEquals(Arrays.asList(higher, lower), results);
    }

    private static Map<String, Float> scores(Object... values) {
        Map<String, Float> output = new HashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            output.put(((FileEntity) values[index]).path,
                    (Float) values[index + 1]);
        }
        return output;
    }

    private static FileEntity file(String path, String type) {
        return new FileEntity(
                path, path, type, null, null,
                0L, 0L, 0L, null, null, null, null);
    }
}
