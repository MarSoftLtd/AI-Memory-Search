package com.bliss.aimemorysearch.ai;

import android.content.Context;

import com.bliss.aimemorysearch.ai.model.AIPackageBundleInfo;
import com.bliss.aimemorysearch.ai.model.AIPackageInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AiPackageBundleCoordinator {
    public static final String AI_SEARCH_BUNDLE_ID = "ai-search";

    private final Context context;
    private final AssetsTranslationPackageRepository repository;
    private final AiPackageLifecycleManager lifecycleManager;
    private final AiPackageManager packageManager;
    private boolean bundleActive;

    public AiPackageBundleCoordinator(Context context) {
        this.context = context.getApplicationContext();
        repository = new AssetsTranslationPackageRepository(this.context);
        lifecycleManager = AiPlatform.createPackageLifecycleManager(this.context, null);
        packageManager = AiPlatform.getPackageManager();
    }

    public List<AIPackageInfo> getPackages(String bundleId) {
        AIPackageBundleInfo bundle = repository.findBundleById(bundleId);
        if (bundle == null) {
            return Collections.emptyList();
        }
        List<AIPackageInfo> result = new ArrayList<>();
        for (String packageId : bundle.getPackageIds()) {
            AIPackageInfo info = repository.findByPackageId(packageId);
            if (info == null || info.getPackageType() != AiPackageType.MODEL) {
                return Collections.emptyList();
            }
            result.add(info);
        }
        return Collections.unmodifiableList(result);
    }

    public AIPackageInfo createPresentationInfo(String bundleId) {
        AIPackageBundleInfo bundle = repository.findBundleById(bundleId);
        List<AIPackageInfo> packages = getPackages(bundleId);
        if (bundle == null || packages.isEmpty()) {
            return null;
        }
        long downloadBytes = 0L;
        long installedBytes = 0L;
        for (AIPackageInfo info : packages) {
            downloadBytes += info.getDownloadSizeBytes();
            installedBytes += info.getInstalledSizeBytes();
        }
        return new AIPackageInfo(
                bundle.getBundleId(), bundle.getDisplayNameKey(), bundle.getDescriptionKey(),
                Collections.emptyList(), "1.0.0", "1.0.0", downloadBytes,
                installedBytes, AiPackageType.MODEL, AiPackageLifecycleState.NOT_INSTALLED, ""
        );
    }

    public boolean isInstalled(String bundleId) {
        List<AIPackageInfo> packages = getPackages(bundleId);
        if (packages.isEmpty()) {
            return false;
        }
        for (AIPackageInfo info : packages) {
            if (!ModelPackageRuntime.isInstalled(context, info.getPackageId())) {
                return false;
            }
        }
        return true;
    }

    public List<AIPackageInfo> getMissingPackages(String bundleId) {
        List<AIPackageInfo> missing = new ArrayList<>();
        for (AIPackageInfo info : getPackages(bundleId)) {
            if (!ModelPackageRuntime.isInstalled(context, info.getPackageId())) {
                missing.add(info);
            }
        }
        return missing;
    }

    public synchronized boolean activate(String bundleId) {
        List<AIPackageInfo> packages = getPackages(bundleId);
        if (packages.isEmpty() || !isInstalled(bundleId)) {
            rollbackActivation();
            return false;
        }
        if (bundleActive && allRuntimesActive(packages)) {
            return true;
        }
        rollbackActivation();
        try {
            for (AIPackageInfo info : packages) {
                File directory = ModelPackageRuntime.requireDirectory(
                        context, info.getPackageId());
                String runtimeRole = ModelPackageRuntime.runtimeRole(directory);
                activateRuntime(runtimeRole, directory);
                if (!isRuntimeActive(runtimeRole)) {
                    throw new IllegalStateException(
                            "MODEL runtime failed to initialize: " + info.getPackageId());
                }
                AiPackageLifecycleResult result =
                        lifecycleManager.activateInstalledModelPackage(info, directory);
                if (!result.isSuccess()) {
                    throw new IllegalStateException(
                            "MODEL active state failed: " + info.getPackageId());
                }
            }
            if (!allRuntimesActive(packages)) {
                throw new IllegalStateException("AI Search bundle activation is incomplete");
            }
            bundleActive = true;
            return true;
        } catch (Exception failure) {
            rollbackActivation();
            android.util.Log.e("AI_SEARCH_BUNDLE", "Activation failed", failure);
            return false;
        }
    }

    public synchronized boolean isActive(String bundleId) {
        List<AIPackageInfo> packages = getPackages(bundleId);
        return bundleActive && !packages.isEmpty() && allRuntimesActive(packages);
    }

    private void activateRuntime(String runtimeRole, File directory) {
        switch (runtimeRole) {
                    case "EMBEDDING_CORE":
                        EmbeddingEngine.getInstance().initialize(context, directory);
                        break;
                    case "E5":
                        new E5EmbeddingEngine(context).initialize();
                        break;
                    case "CLIP_TEXT":
                        MobileClipTextEmbeddingEngine.getInstance().initialize(context, directory);
                        break;
                    case "CLIP_VISION":
                        ImageEmbeddingEngine.getInstance().initialize(directory);
                        break;
                    default:
                throw new IllegalArgumentException("Unknown MODEL runtime role: " + runtimeRole);
        }
    }

    private boolean isRuntimeActive(String runtimeRole) {
        switch (runtimeRole) {
            case "EMBEDDING_CORE":
                return EmbeddingEngine.getInstance().isInitialized();
            case "E5":
                return E5EmbeddingEngine.isInitialized();
            case "CLIP_TEXT":
                return MobileClipTextEmbeddingEngine.getInstance().isInitialized();
            case "CLIP_VISION":
                return ImageEmbeddingEngine.getInstance().isInitialized();
            default:
                return false;
        }
    }

    private boolean allRuntimesActive(List<AIPackageInfo> packages) {
        for (AIPackageInfo info : packages) {
            try {
                File directory = ModelPackageRuntime.requireDirectory(
                        context, info.getPackageId());
                if (!isRuntimeActive(ModelPackageRuntime.runtimeRole(directory))
                        || !packageManager.isModelPackageActive(info.getPackageId())) {
                    return false;
                }
            } catch (Exception failure) {
                return false;
            }
        }
        return true;
    }

    private void rollbackActivation() {
        ImageEmbeddingEngine.getInstance().deactivate();
        MobileClipTextEmbeddingEngine.getInstance().deactivate();
        E5EmbeddingEngine.deactivate();
        EmbeddingEngine.getInstance().deactivate();
        packageManager.deactivateAllModelPackages();
        bundleActive = false;
    }
}
