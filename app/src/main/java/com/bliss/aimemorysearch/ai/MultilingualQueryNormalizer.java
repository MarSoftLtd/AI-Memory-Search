package com.bliss.aimemorysearch.ai;

import android.content.Context;

public class MultilingualQueryNormalizer {

    public interface Callback {

        void onReady(String originalQuery,
                     String englishClipQuery);

        void onError(String originalQuery);
    }

    private static MultilingualQueryNormalizer instance;

    private final Context context;

    private MultilingualQueryNormalizer(Context context) {
        this.context =
                context.getApplicationContext();
    }

    public static synchronized MultilingualQueryNormalizer getInstance(Context context) {

        if (instance == null) {
            instance = new MultilingualQueryNormalizer(context);
        }

        return instance;
    }

    public void normalizeForClip(String query,
                                 Callback callback) {
        normalizeForClip(
                query,
                "",
                callback
        );
    }

    public void normalizeForClip(
            String query,
            String selectedLanguageFamily,
            Callback callback
    ) {

        android.util.Log.d("MULTILINGUAL_PIPELINE", "Normalizer input/original query: " + query);

        if (callback == null) {
            android.util.Log.w("MULTILINGUAL_PIPELINE", "Normalizer early return: callback is null");
            return;
        }

        if (query == null) {
            android.util.Log.w("MULTILINGUAL_PIPELINE", "Normalizer early return: query is null; invoking onError");
            callback.onError("");
            return;
        }

        String englishQuery =
                query.trim();

        try {

            englishQuery =
                    TranslationEngine
                            .getInstance(context)
                            .translate(
                                    query,
                                    selectedLanguageFamily
                            );

            android.util.Log.d("MULTILINGUAL_PIPELINE", "Translated English query: " + englishQuery);

            if (
                    englishQuery == null
                            ||
                            englishQuery.trim().isEmpty()
            ) {
                englishQuery =
                        query.trim();
                android.util.Log.w("MULTILINGUAL_PIPELINE", "Translation fallback: empty output; using original query: " + englishQuery);
            }

        } catch (Exception e) {
            englishQuery =
                    query.trim();
            android.util.Log.e("MULTILINGUAL_PIPELINE", "Translation exception; fallback to original query: " + englishQuery, e);
        }

        android.util.Log.d("MULTILINGUAL_PIPELINE", "Normalizer output/final CLIP query: " + englishQuery);

        callback.onReady(
                query,
                englishQuery
        );
    }

    public String normalize(String query) {

        if (query == null) {
            return "";
        }

        return query.trim();
    }
}
