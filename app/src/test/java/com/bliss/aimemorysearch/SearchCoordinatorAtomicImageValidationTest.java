package com.bliss.aimemorysearch;

import static org.junit.Assert.assertEquals;

import com.bliss.aimemorysearch.ai.concepts.MatchTier;
import com.bliss.aimemorysearch.db.FileEntity;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SearchCoordinatorAtomicImageValidationTest {

    @Test
    public void simpleQueryKeepsExistingImageResults() {
        FileEntity first = file("first", "IMAGE");
        FileEntity second = file("second", "IMAGE");
        List<FileEntity> results = new ArrayList<>(Arrays.asList(first, second));

        SearchCoordinator.applyAtomicImageValidation(
                results,
                false,
                tiers(second, MatchTier.RELATED_MATCH)
        );

        assertEquals(Arrays.asList(first, second), results);
    }

    @Test
    public void multiConceptKeepsOnlyAtomicConjunctionBestMatches() {
        FileEntity wholeQueryFirst = file("whole-first", "IMAGE");
        FileEntity wholeQuerySecond = file("whole-second", "IMAGE");
        FileEntity wholeQueryThird = file("whole-third", "IMAGE");
        List<FileEntity> results = new ArrayList<>(Arrays.asList(
                wholeQueryFirst,
                wholeQuerySecond,
                wholeQueryThird
        ));
        Map<String, MatchTier> tiers = new LinkedHashMap<>();
        tiers.put(wholeQueryFirst.path, MatchTier.PARTIAL_MATCH);
        tiers.put(wholeQuerySecond.path, MatchTier.BEST_MATCH);
        tiers.put(wholeQueryThird.path, MatchTier.RELATED_MATCH);

        SearchCoordinator.applyAtomicImageValidation(results, true, tiers);

        assertEquals(Arrays.asList(wholeQuerySecond), results);
    }

    @Test
    public void atomicValidationDoesNotRerankWholeQueryImages() {
        FileEntity first = file("first", "IMAGE");
        FileEntity second = file("second", "IMAGE");
        FileEntity third = file("third", "IMAGE");
        List<FileEntity> results = new ArrayList<>(Arrays.asList(
                first, second, third
        ));
        Map<String, MatchTier> tiers = new LinkedHashMap<>();
        tiers.put(first.path, MatchTier.BEST_MATCH);
        tiers.put(second.path, MatchTier.PARTIAL_MATCH);
        tiers.put(third.path, MatchTier.BEST_MATCH);

        SearchCoordinator.applyAtomicImageValidation(results, true, tiers);

        assertEquals(Arrays.asList(first, third), results);
    }

    @Test
    public void multiConceptCanValidlyReturnZeroImages() {
        FileEntity image = file("image", "IMAGE");
        List<FileEntity> results = new ArrayList<>(Arrays.asList(image));

        SearchCoordinator.applyAtomicImageValidation(
                results,
                true,
                tiers(image, MatchTier.PARTIAL_MATCH)
        );

        assertEquals(0, results.size());
    }

    @Test
    public void mixedModalityDocumentsAreNeverRemoved() {
        FileEntity document = file("document", "DOCUMENT");
        FileEntity rejectedImage = file("image", "IMAGE");
        List<FileEntity> results = new ArrayList<>(Arrays.asList(
                document,
                rejectedImage
        ));

        SearchCoordinator.applyAtomicImageValidation(
                results,
                true,
                tiers(rejectedImage, MatchTier.RELATED_MATCH)
        );

        assertEquals(Arrays.asList(document), results);
    }

    private static Map<String, MatchTier> tiers(
            FileEntity file,
            MatchTier tier
    ) {
        Map<String, MatchTier> output = new LinkedHashMap<>();
        output.put(file.path, tier);
        return output;
    }

    private static FileEntity file(String path, String type) {
        return new FileEntity(
                path, path, type, null, null,
                0L, 0L, 0L, null, null, null, null
        );
    }
}
