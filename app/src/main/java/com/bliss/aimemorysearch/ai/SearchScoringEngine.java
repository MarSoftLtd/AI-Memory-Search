package com.bliss.aimemorysearch.ai;

import com.bliss.aimemorysearch.db.FileEntity;

import java.text.Normalizer;
import java.util.Locale;

public class SearchScoringEngine {

    public static float calculateFinalScore(
            String query,
            FileEntity entity,
            float semanticScore
    ) {

        if (entity == null) {
            return 0f;
        }

        String searchableText =
                normalize(
                        (
                                safe(entity.name)
                                        + " "
                                        + safe(entity.ocrText)
                                        + " "
                                        + safe(entity.path)
                        )
                );

        String normalizedQuery =
                normalize(query);

        if (
                normalizedQuery.trim().isEmpty()
        ) {
            return 0f;
        }

        String[] queryTokens =
                normalizedQuery.split("\\s+");

        int validTokens = 0;

        int matchedTokens = 0;

        for (String token : queryTokens) {

            if (
                    token == null
                            ||
                            token.trim().isEmpty()
            ) {
                continue;
            }

            validTokens++;

            if (
                    searchableText.contains(token)
            ) {

                matchedTokens++;
            }
        }

        if (validTokens == 0) {
            return 0f;
        }

        float coverage =
                (float) matchedTokens
                        /
                        (float) validTokens;

        /*
         * HARD FILTER
         */

        if (
                validTokens >= 2
                        &&
                        coverage < 0.60f
        ) {

            return 0f;
        }

        float ocrScore =
                calculateOcrScore(
                        normalizedQuery,
                        entity
                );

        float filenameScore =
                calculateFilenameScore(
                        normalizedQuery,
                        entity
                );

        float pathScore =
                calculatePathScore(
                        normalizedQuery,
                        entity
                );

        float finalScore =
                semanticScore * 1.10f +
                        ocrScore * 1.30f +
                        filenameScore * 1.45f +
                        pathScore * 0.40f;

        /*
         * COVERAGE BOOST
         */

        finalScore +=
                coverage * 1.40f;

        /*
         * PARTIAL COVERAGE PENALTY
         */

        if (coverage < 1.0f) {

            finalScore *= 0.45f;
        }

        /*
         * EXACT QUERY BOOST
         */

        if (
                searchableText.contains(
                        normalizedQuery
                )
        ) {

            finalScore += 3.5f;
        }

        /*
         * DOCUMENT BOOST
         */

        if (entity.type != null) {

            String type =
                    entity.type.toLowerCase();

            if (
                    type.contains("pdf")
                            ||
                            type.contains("document")
                            ||
                            type.contains("doc")
                            ||
                            type.contains("txt")
                            ||
                            type.contains("xlsx")
            ) {

                finalScore += 0.35f;
            }
        }

        /*
         * IDENTITY DOCUMENT BOOST
         */

        if (
                normalizedQuery.contains("ci")
                        ||
                        normalizedQuery.contains("carte identitate")
        ) {

            if (
                    searchableText.contains("serie")
                            ||
                            searchableText.contains("cnp")
                            ||
                            searchableText.contains("identity")
                            ||
                            searchableText.contains("identitate")
            ) {

                finalScore += 2.5f;
            }
        }

        /*
         * INVOICE BOOST
         */

        if (
                normalizedQuery.contains("factura")
        ) {

            if (
                    searchableText.contains("factura")
                            ||
                            searchableText.contains("invoice")
            ) {

                finalScore += 1.8f;
            }
        }

        return Math.max(
                0f,
                finalScore
        );
    }

    private static float calculateOcrScore(
            String query,
            FileEntity entity
    ) {

        if (entity.ocrText == null) {
            return 0f;
        }

        String normalizedOcr =
                normalize(entity.ocrText);

        if (
                normalizedOcr.contains(query)
        ) {

            return 4.0f;
        }

        String[] queryTokens =
                query.split("\\s+");

        float matchedTokens = 0f;

        for (String token : queryTokens) {

            if (
                    token == null
                            ||
                            token.trim().isEmpty()
            ) {
                continue;
            }

            if (
                    normalizedOcr.contains(token)
            ) {

                matchedTokens += 1f;
            }
        }

        return matchedTokens
                /
                queryTokens.length;
    }

    private static float calculateFilenameScore(
            String query,
            FileEntity entity
    ) {

        if (entity.name == null) {
            return 0f;
        }

        String normalizedName =
                normalize(entity.name);

        if (
                normalizedName.contains(query)
        ) {

            return 5.0f;
        }

        String[] queryTokens =
                query.split("\\s+");

        float matchedTokens = 0f;

        for (String token : queryTokens) {

            if (
                    token == null
                            ||
                            token.trim().isEmpty()
            ) {
                continue;
            }

            if (
                    normalizedName.contains(token)
            ) {

                matchedTokens += 1f;
            }
        }

        return matchedTokens
                /
                queryTokens.length;
    }

    private static float calculatePathScore(
            String query,
            FileEntity entity
    ) {

        if (entity.path == null) {
            return 0f;
        }

        String normalizedPath =
                normalize(entity.path);

        if (
                normalizedPath.contains(query)
        ) {

            return 2.0f;
        }

        return 0f;
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

        normalized =
                normalized.replaceAll(
                        "[^a-z0-9 ]",
                        " "
                );

        normalized =
                normalized.replaceAll(
                        "\\s+",
                        " "
                );

        return normalized.trim();
    }

    private static String safe(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value;
    }
}