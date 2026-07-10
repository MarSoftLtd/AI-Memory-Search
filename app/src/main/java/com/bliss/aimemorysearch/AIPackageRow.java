package com.bliss.aimemorysearch;

import androidx.annotation.DimenRes;
import androidx.annotation.Nullable;

public class AIPackageRow {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_SECTION_HEADER = 1;
    public static final int TYPE_AI_PACKAGE = 2;
    public static final int TYPE_STORAGE_SUMMARY = 3;

    private final int type;
    @Nullable
    private final String title;
    @Nullable
    private final AIPackageUiModel aiPackage;
    private final int topMargin;

    private AIPackageRow(
            int type,
            @Nullable String title,
            @Nullable AIPackageUiModel aiPackage,
            @DimenRes int topMargin
    ) {
        this.type = type;
        this.title = title;
        this.aiPackage = aiPackage;
        this.topMargin = topMargin;
    }

    public static AIPackageRow header() {
        return new AIPackageRow(
                TYPE_HEADER,
                null,
                null,
                0
        );
    }

    public static AIPackageRow sectionHeader(
            String title,
            @DimenRes int topMargin
    ) {
        return new AIPackageRow(
                TYPE_SECTION_HEADER,
                title,
                null,
                topMargin
        );
    }

    public static AIPackageRow aiPackage(
            AIPackageUiModel aiPackage
    ) {
        return new AIPackageRow(
                TYPE_AI_PACKAGE,
                null,
                aiPackage,
                0
        );
    }

    public static AIPackageRow storageSummary() {
        return new AIPackageRow(
                TYPE_STORAGE_SUMMARY,
                null,
                null,
                0
        );
    }

    public int getType() {
        return type;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    @Nullable
    public AIPackageUiModel getAiPackage() {
        return aiPackage;
    }

    @DimenRes
    public int getTopMargin() {
        return topMargin;
    }
}
