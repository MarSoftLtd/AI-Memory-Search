package com.bliss.aimemorysearch.ai;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class PrefixVocabularyCache {

    private static final HashSet<String> vocabulary =
            new HashSet<>();

    public static void addText(
            String text
    ) {

        if (text == null) {
            return;
        }

        String[] words =
                text.split("\\s+");

        for (String word : words) {

            if (
                    word == null
                            ||
                            word.length() < 3
            ) {

                continue;
            }

            vocabulary.add(word);
        }
    }

    public static List<String> expandPrefix(
            String prefix
    ) {

        List<String> results =
                new ArrayList<>();

        if (
                prefix == null
                        ||
                        prefix.length() < 2
        ) {

            return results;
        }

        for (String word : vocabulary) {

            if (
                    word != null
                            &&
                            word.startsWith(prefix)
            ) {

                results.add(word);
            }
        }

        results.sort(
                (a, b) ->
                        Integer.compare(
                                a.length(),
                                b.length()
                        )
        );
        android.util.Log.e(
                "PREFIX_RESULT",
                prefix + " => " + results
        );
        if (results.size() > 10) {

            return new ArrayList<>(
                    results.subList(
                            0,
                            10
                    )
            );
        }

        return results;
    }
    public static int getVocabularySize() {

        return vocabulary.size();
    }
    public static void clear() {

        vocabulary.clear();
    }
}