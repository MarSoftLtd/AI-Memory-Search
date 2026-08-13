package com.bliss.aimemorysearch;

import java.util.Locale;

/** One source-to-color mapping shared by result cards and the technical report. */
public final class SourceVisuals {
    private SourceVisuals() {}

    public static int colorResource(String sourceType) {
        String type = sourceType == null
                ? "DOCUMENT" : sourceType.trim().toUpperCase(Locale.ROOT);
        if ("EMAIL".equals(type)) return R.color.color_source_email;
        if ("IMAGE".equals(type)) return R.color.color_source_image;
        if ("PDF".equals(type)) return R.color.color_source_pdf;
        return R.color.color_source_document;
    }
}
