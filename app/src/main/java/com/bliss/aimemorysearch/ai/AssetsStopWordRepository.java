package com.bliss.aimemorysearch.ai;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class AssetsStopWordRepository implements StopWordRepository {

    private static final String STOP_WORDS_ASSET_ROOT =
            "nlp/stopwords/";

    private final Context context;
    private final Map<String, Set<String>> cache;

    public AssetsStopWordRepository(
            Context context
    ) {
        this.context =
                context.getApplicationContext();
        cache =
                new HashMap<>();
    }

    @Override
    public synchronized Set<String> getStopWords(
            String languageCode
    ) {
        String normalizedLanguageCode =
                normalizeLanguageCode(
                        languageCode
                );

        if (normalizedLanguageCode.isEmpty()) {
            return Collections.emptySet();
        }

        if (cache.containsKey(normalizedLanguageCode)) {
            return cache.get(
                    normalizedLanguageCode
            );
        }

        Set<String> stopWords =
                loadStopWords(
                        normalizedLanguageCode
                );

        cache.put(
                normalizedLanguageCode,
                stopWords
        );

        return stopWords;
    }

    private Set<String> loadStopWords(
            String languageCode
    ) {
        String assetPath =
                STOP_WORDS_ASSET_ROOT
                        + languageCode
                        + ".txt";

        try (
                InputStream inputStream =
                        context.getAssets().open(
                                assetPath
                        );
                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        inputStream,
                                        StandardCharsets.UTF_8
                                )
                        )
        ) {
            Set<String> stopWords =
                    new HashSet<>();
            String line;

            while (
                    (line = reader.readLine()) != null
            ) {
                String value =
                        line.trim();

                if (!value.isEmpty()) {
                    stopWords.add(
                            value
                    );
                }
            }

            return Collections.unmodifiableSet(
                    stopWords
            );
        } catch (IOException e) {
            return Collections.emptySet();
        }
    }

    private static String normalizeLanguageCode(
            String languageCode
    ) {
        if (languageCode == null) {
            return "";
        }

        return languageCode
                .trim()
                .toLowerCase(
                        Locale.ROOT
                );
    }
}
