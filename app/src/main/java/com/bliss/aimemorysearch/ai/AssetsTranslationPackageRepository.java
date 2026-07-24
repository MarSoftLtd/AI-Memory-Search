package com.bliss.aimemorysearch.ai;

import android.content.Context;

import com.bliss.aimemorysearch.ai.model.AIPackageInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import com.bliss.aimemorysearch.ai.model.AIPackageBundleInfo;

public final class AssetsTranslationPackageRepository
        implements TranslationPackageRepository {

    private static final String REPOSITORY_ASSET_PATH =
            "translation_packages/repository.json";
    private static final int SUPPORTED_SCHEMA_VERSION = 1;

    private final Context context;
    private volatile List<AIPackageInfo> cachedPackages;
    private volatile Map<String, AIPackageBundleInfo> cachedBundles;

    public AssetsTranslationPackageRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public List<AIPackageInfo> getAIPackages() {
        List<AIPackageInfo> packages = cachedPackages;
        if (packages != null) {
            return packages;
        }
        synchronized (this) {
            if (cachedPackages == null) {
                cachedPackages = loadPackages();
            }
            return cachedPackages;
        }
    }

    public AIPackageInfo findByPackageId(String packageId) {
        if (packageId == null) {
            return null;
        }
        for (AIPackageInfo packageInfo : getAIPackages()) {
            if (packageId.equals(packageInfo.getPackageId())) {
                return packageInfo;
            }
        }
        return null;
    }

    public AIPackageBundleInfo findBundleById(String bundleId) {
        if (bundleId == null) {
            return null;
        }
        ensureLoaded();
        return cachedBundles.get(bundleId);
    }

    public List<AIPackageInfo> getBundlePackages(String bundleId) {
        AIPackageBundleInfo bundle = findBundleById(bundleId);
        if (bundle == null) {
            return Collections.emptyList();
        }
        List<AIPackageInfo> packages = new ArrayList<>();
        for (String packageId : bundle.getPackageIds()) {
            AIPackageInfo packageInfo = findByPackageId(packageId);
            if (packageInfo == null) {
                return Collections.emptyList();
            }
            packages.add(packageInfo);
        }
        return Collections.unmodifiableList(packages);
    }

    private synchronized void ensureLoaded() {
        if (cachedPackages != null && cachedBundles != null) {
            return;
        }
        try {
            JSONObject root = new JSONObject(readAssetText());
            if (root.getInt("schemaVersion") != SUPPORTED_SCHEMA_VERSION) {
                cachedPackages = Collections.emptyList();
                cachedBundles = Collections.emptyMap();
                return;
            }
            JSONArray entries = root.getJSONArray("packages");
            List<AIPackageInfo> packages = new ArrayList<>(entries.length());
            for (int index = 0; index < entries.length(); index++) {
                packages.add(readPackage(entries.getJSONObject(index)));
            }
            Map<String, AIPackageBundleInfo> bundles = new LinkedHashMap<>();
            JSONArray bundleEntries = root.optJSONArray("bundles");
            if (bundleEntries != null) {
                for (int index = 0; index < bundleEntries.length(); index++) {
                    JSONObject entry = bundleEntries.getJSONObject(index);
                    JSONArray ids = entry.getJSONArray("packageIds");
                    List<String> packageIds = new ArrayList<>(ids.length());
                    for (int item = 0; item < ids.length(); item++) {
                        packageIds.add(ids.getString(item));
                    }
                    AIPackageBundleInfo bundle = new AIPackageBundleInfo(
                            entry.getString("bundleId"),
                            entry.getString("displayNameKey"),
                            entry.getString("descriptionKey"),
                            packageIds
                    );
                    bundles.put(bundle.getBundleId(), bundle);
                }
            }
            cachedPackages = Collections.unmodifiableList(packages);
            cachedBundles = Collections.unmodifiableMap(bundles);
        } catch (Exception ignored) {
            cachedPackages = Collections.emptyList();
            cachedBundles = Collections.emptyMap();
        }
    }

    @Override
    public List<TranslationPackageInfo> getPackages() {
        return Collections.emptyList();
    }

    private List<AIPackageInfo> loadPackages() {
        ensureLoaded();
        return cachedPackages;
    }

    private AIPackageInfo readPackage(JSONObject entry) throws Exception {
        JSONArray languageArray = entry.getJSONArray("supportedLanguages");
        List<String> supportedLanguages = new ArrayList<>(languageArray.length());
        for (int index = 0; index < languageArray.length(); index++) {
            supportedLanguages.add(languageArray.getString(index));
        }
        return new AIPackageInfo(
                entry.getString("packageId"),
                entry.getString("displayNameKey"),
                entry.getString("descriptionKey"),
                supportedLanguages,
                entry.getString("version"),
                entry.getString("modelVersion"),
                entry.getLong("downloadSizeBytes"),
                entry.getLong("installedSizeBytes"),
                AiPackageType.valueOf(entry.getString("packageType")),
                AiPackageLifecycleState.NOT_INSTALLED,
                entry.getString("sha256")
        );
    }

    private String readAssetText() throws Exception {
        StringBuilder builder = new StringBuilder();
        try (
                InputStream inputStream = context.getAssets().open(REPOSITORY_ASSET_PATH);
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream, "UTF-8")
                )
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
        }
        return builder.toString();
    }
}
