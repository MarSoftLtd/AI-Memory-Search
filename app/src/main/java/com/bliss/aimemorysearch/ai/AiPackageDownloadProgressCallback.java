package com.bliss.aimemorysearch.ai;

public interface AiPackageDownloadProgressCallback {

    void onProgress(
            long downloadedBytes,
            long totalBytes
    );
}
