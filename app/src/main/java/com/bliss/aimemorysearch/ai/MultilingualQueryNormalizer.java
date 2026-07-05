package com.bliss.aimemorysearch.ai;

import android.content.Context;

public class MultilingualQueryNormalizer {

    public interface Callback {

        void onReady(String originalQuery,
                     String englishClipQuery);

        void onError(String originalQuery);
    }

    private static MultilingualQueryNormalizer instance;

    private MultilingualQueryNormalizer(Context context) {
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

        callback.onReady(
                query,
                query.trim()
        );
    }

    public String normalize(String query) {

        if (query == null) {
            return "";
        }

        return query.trim();
    }
}