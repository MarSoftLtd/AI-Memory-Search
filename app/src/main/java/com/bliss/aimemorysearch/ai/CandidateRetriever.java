package com.bliss.aimemorysearch.ai;

import com.bliss.aimemorysearch.db.FileEntity;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CandidateRetriever {

    public static List<FileEntity> filterCandidates(
            List<FileEntity> allFiles,
            String query
    ) {

        List<FileEntity> candidates =
                new ArrayList<>();

        if (allFiles == null) {
            return candidates;
        }

        String normalizedQuery =
                normalize(query);

        String[] queryTokens =
                normalizedQuery.split("\\s+");

        for (FileEntity entity : allFiles) {

            if (entity == null) {
                continue;
            }

            float score = 0f;

            String name =
                    normalize(entity.name);

            String ocr =
                    normalize(entity.ocrText);

            String path =
                    normalize(entity.path);

            String type =
                    normalize(entity.type);

            for (String token : queryTokens) {

                if (token.trim().isEmpty()) {
                    continue;
                }

                if (name.contains(token)) {
                    score += 6f;
                }

                if (ocr.contains(token)) {
                    score += 8f;
                }

                if (path.contains(token)) {
                    score += 2f;
                }

                if (
                        type.contains("pdf")
                                ||
                                type.contains("doc")
                                ||
                                type.contains("text")
                ) {

                    score += 0.5f;
                }
            }

            if (
                    score > 0f
                            ||
                            (
                                    entity.embedding != null
                                            &&
                                            entity.embedding.length > 0
                            )
            ) {

                candidates.add(entity);
            }
        }

        return candidates;
    }

    private static String normalize(
            String text
    ) {

        if (text == null) {
            return "";
        }

        String normalized =
                Normalizer.normalize(
                        text,
                        Normalizer.Form.NFD
                );

        normalized =
                normalized.replaceAll(
                        "\\p{InCombiningDiacriticalMarks}+",
                        ""
                );

        normalized =
                normalized.toLowerCase(
                        Locale.ROOT
                );

        return normalized;
    }
}