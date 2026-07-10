package com.bliss.aimemorysearch;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

public class AIPackageUiModel {

    public enum State {
        READY,
        INSTALLED,
        AVAILABLE,
        UPDATE_AVAILABLE,
        DOWNLOADING,
        DISABLED
    }

    private final String id;
    private final int icon;
    private final String title;
    private final String description;
    private final State state;
    private final boolean installed;
    private final boolean updateAvailable;
    @Nullable
    private final Integer progress;

    public AIPackageUiModel(
            String id,
            @DrawableRes int icon,
            String title,
            String description,
            State state,
            boolean installed,
            boolean updateAvailable,
            @Nullable Integer progress
    ) {
        this.id = id;
        this.icon = icon;
        this.title = title;
        this.description = description;
        this.state = state;
        this.installed = installed;
        this.updateAvailable = updateAvailable;
        this.progress = progress;
    }

    public String getId() {
        return id;
    }

    @DrawableRes
    public int getIcon() {
        return icon;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public State getState() {
        return state;
    }

    public boolean isInstalled() {
        return installed;
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    @Nullable
    public Integer getProgress() {
        return progress;
    }
}
