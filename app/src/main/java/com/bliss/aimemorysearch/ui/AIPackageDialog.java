package com.bliss.aimemorysearch.ui;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.View;
import android.widget.TextView;

import com.bliss.aimemorysearch.R;
import com.bliss.aimemorysearch.ai.AiPackageLifecycleState;
import com.bliss.aimemorysearch.ai.AiPackageType;
import com.bliss.aimemorysearch.ai.model.AIPackageInfo;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

public final class AIPackageDialog {
    private final View root;
    private final Context context;
    private final TextView typeText;
    private final TextView titleText;
    private final TextView descriptionText;
    private final TextView stateText;
    private final View infoContainer;
    private final TextView languagesText;
    private final TextView versionText;
    private final TextView downloadSizeText;
    private final View installedSizeRow;
    private final TextView installedSizeText;
    private final View progressContainer;
    private final TextView progressStatusText;
    private final LinearProgressIndicator progressBar;
    private final TextView progressBytesText;
    private final TextView progressPercentText;
    private final TextView errorText;
    private final MaterialButton primaryButton;
    private final TextView secondaryButton;
    private boolean closingEnabled = true;

    public AIPackageDialog(View root) {
        if (root == null) {
            throw new IllegalArgumentException("root must not be null");
        }
        this.root = root;
        context = root.getContext();
        typeText = requireView(R.id.aiPackageTypeText);
        titleText = requireView(R.id.aiPackageTitleText);
        descriptionText = requireView(R.id.aiPackageDescriptionText);
        stateText = requireView(R.id.aiPackageStateText);
        infoContainer = requireView(R.id.aiPackageInfoContainer);
        languagesText = requireView(R.id.aiPackageLanguagesText);
        versionText = requireView(R.id.aiPackageVersionText);
        downloadSizeText = requireView(R.id.aiPackageDownloadSizeText);
        installedSizeRow = requireView(R.id.aiPackageInstalledSizeRow);
        installedSizeText = requireView(R.id.aiPackageInstalledSizeText);
        progressContainer = requireView(R.id.aiPackageProgressContainer);
        progressStatusText = requireView(R.id.aiPackageProgressStatusText);
        progressBar = requireView(R.id.aiPackageProgressBar);
        progressBytesText = requireView(R.id.aiPackageProgressBytesText);
        progressPercentText = requireView(R.id.aiPackageProgressPercentText);
        errorText = requireView(R.id.aiPackageErrorText);
        primaryButton = requireView(R.id.aiPackagePrimaryButton);
        secondaryButton = requireView(R.id.aiPackageSecondaryButton);
    }

    public void show() {
        root.setAlpha(1f);
        root.setVisibility(View.VISIBLE);
    }

    public void hide() {
        if (!closingEnabled) {
            return;
        }
        root.setVisibility(View.GONE);
        root.setAlpha(0f);
    }

    public void bind(AIPackageInfo info) {
        if (info == null) {
            throw new IllegalArgumentException("info must not be null");
        }
        typeText.setText(getPackageTypeLabel(info.getPackageType()));
        titleText.setText(resolveString(info.getDisplayNameKey()));
        descriptionText.setText(resolveString(info.getDescriptionKey()));
        languagesText.setText(TextUtils.join(
                context.getString(R.string.ai_package_list_separator),
                info.getSupportedLanguages()
        ));
        versionText.setText(info.getVersion());
        downloadSizeText.setText(formatBytes(info.getDownloadSizeBytes()));
        installedSizeText.setText(formatBytes(info.getInstalledSizeBytes()));
        setVisible(installedSizeRow, info.getInstalledSizeBytes() > 0L);
        bindState(info.getState());
    }

    public void showReadyState() {
        closingEnabled = true;
        applyState(R.string.ai_package_ready, false, false, true, true,
                R.string.ai_package_download, R.string.ai_package_later);
    }

    public void showDownloadingState(int progress, long downloadedBytes, long totalBytes) {
        closingEnabled = false;
        int boundedProgress = Math.max(0, Math.min(100, progress));
        applyState(R.string.ai_package_downloading, true, false, true, false,
                R.string.ai_package_cancel, 0);
        progressStatusText.setText(R.string.ai_package_downloading_status);
        progressBar.setIndeterminate(false);
        progressBar.setProgressCompat(boundedProgress, true);
        progressBytesText.setText(context.getString(
                R.string.ai_package_progress_bytes,
                formatBytes(downloadedBytes),
                formatBytes(totalBytes)
        ));
        progressPercentText.setText(context.getString(
                R.string.ai_package_progress_percent,
                boundedProgress
        ));
    }

    public void showVerifyingState() {
        showIndeterminateState(
                R.string.ai_package_verifying,
                R.string.ai_package_verifying_status
        );
    }

    public void showInstallingState() {
        showIndeterminateState(
                R.string.ai_package_installing,
                R.string.ai_package_installing_status
        );
    }

    public void showInstalledState() {
        closingEnabled = true;
        applyState(R.string.ai_package_installed, false, false, true, true,
                R.string.ai_package_done, R.string.ai_package_close);
    }

    public void showReconcilingState() {
        closingEnabled = false;
        applyState(R.string.ai_package_installed, true, false, false, false,
                0, 0);
        progressStatusText.setText(R.string.ai_package_reconciling_status);
        progressBar.setIndeterminate(true);
        progressBytesText.setText(null);
        progressPercentText.setText(null);
    }

    public void showErrorState(String message) {
        closingEnabled = true;
        applyState(R.string.ai_package_error, false, true, true, true,
                R.string.ai_package_retry, R.string.ai_package_close);
        errorText.setText(message);
    }

    public void showDownloadCompletedState() {
        closingEnabled = true;
        applyState(R.string.ai_package_download_completed, false, false, true, false,
                R.string.ai_package_close, 0);
    }

    public void showDownloadErrorState(String message) {
        closingEnabled = true;
        applyState(R.string.ai_package_error, false, true, true, false,
                R.string.ai_package_close, 0);
        errorText.setText(message);
    }

    public void setPrimaryActionListener(View.OnClickListener listener) {
        primaryButton.setOnClickListener(listener);
    }

    public void setSecondaryActionListener(View.OnClickListener listener) {
        secondaryButton.setOnClickListener(listener);
    }

    private void bindState(AiPackageLifecycleState state) {
        if (state == null) {
            showReadyState();
            return;
        }
        switch (state) {
            case DOWNLOADING:
                showDownloadingState(0, 0L, 0L);
                break;
            case VERIFYING:
                showVerifyingState();
                break;
            case INSTALLING:
                showInstallingState();
                break;
            case INSTALLED:
            case ACTIVE:
            case INACTIVE:
                showInstalledState();
                break;
            case FAILED:
                showErrorState(context.getString(R.string.ai_package_error_default));
                break;
            case NOT_INSTALLED:
            case DOWNLOADED:
            case UPDATE_AVAILABLE:
            default:
                showReadyState();
                break;
        }
    }

    private void showIndeterminateState(int stateTextResId, int statusTextResId) {
        applyState(stateTextResId, true, false, false, true,
                0, R.string.ai_package_cancel);
        progressStatusText.setText(statusTextResId);
        progressBar.setIndeterminate(true);
        progressBytesText.setText(null);
        progressPercentText.setText(null);
    }

    private void applyState(
            int stateTextResId,
            boolean showProgress,
            boolean showError,
            boolean showPrimary,
            boolean showSecondary,
            int primaryTextResId,
            int secondaryTextResId
    ) {
        stateText.setText(stateTextResId);
        setVisible(infoContainer, true);
        setVisible(progressContainer, showProgress);
        setVisible(errorText, showError);
        setVisible(primaryButton, showPrimary);
        setVisible(secondaryButton, showSecondary);
        if (showPrimary) primaryButton.setText(primaryTextResId);
        if (showSecondary) secondaryButton.setText(secondaryTextResId);
    }

    private void setVisible(View view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private String formatBytes(long bytes) {
        return Formatter.formatFileSize(context, Math.max(0L, bytes));
    }

    private String resolveString(String resourceKey) {
        int resourceId = context.getResources().getIdentifier(
                resourceKey,
                "string",
                context.getPackageName()
        );
        if (resourceId == 0) {
            throw new IllegalArgumentException(
                    "Missing string resource: " + resourceKey
            );
        }
        return context.getString(resourceId);
    }

    private String getPackageTypeLabel(AiPackageType packageType) {
        if (packageType == null) {
            return context.getString(R.string.ai_package_type_unknown);
        }
        switch (packageType) {
            case DOCUMENT:
                return context.getString(R.string.ai_package_type_document);
            case IMAGE:
                return context.getString(R.string.ai_package_type_image);
            case TRANSLATION:
                return context.getString(R.string.ai_package_type_translation);
            case SPEECH:
                return context.getString(R.string.ai_package_type_speech);
            case VOICE:
                return context.getString(R.string.ai_package_type_voice);
            case OCR:
                return context.getString(R.string.ai_package_type_ocr);
            case KNOWLEDGE:
                return context.getString(R.string.ai_package_type_knowledge);
            case FUTURE:
            default:
                return context.getString(R.string.ai_package_type_other);
        }
    }

    private <T extends View> T requireView(int id) {
        T view = root.findViewById(id);
        if (view == null) {
            throw new IllegalArgumentException("Missing required dialog view: " + id);
        }
        return view;
    }
}
