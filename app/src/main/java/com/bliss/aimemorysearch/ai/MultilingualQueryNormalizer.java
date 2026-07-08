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

        if (callback == null) {
            return;
        }

        if (query == null) {
            callback.onError("");
            return;
        }

        String englishQuery =
                query.trim();

        try {

            englishQuery =
                    RomanceTranslator
                            .getInstance(context)
                            .translate(query);

            if (
                    englishQuery == null
                            ||
                            englishQuery.trim().isEmpty()
            ) {
                englishQuery =
                        query.trim();
            }

        } catch (Exception e) {
            englishQuery =
                    query.trim();
        }

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
