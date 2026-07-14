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

public final class AssetsTranslationPackageRepository
        implements TranslationPackageRepository {

    private static final String REPOSITORY_ASSET_PATH =
            "translation_packages/repository.json";
    private static final int SUPPORTED_SCHEMA_VERSION = 1;

    private final Context context;
    private volatile List<AIPackageInfo> cachedPackages;

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

    @Override
    public List<TranslationPackageInfo> getPackages() {
        return Collections.emptyList();
    }

    private List<AIPackageInfo> loadPackages() {
        try {
            JSONObject root = new JSONObject(readAssetText());
            if (root.getInt("schemaVersion") != SUPPORTED_SCHEMA_VERSION) {
                return Collections.emptyList();
            }
            JSONArray entries = root.getJSONArray("packages");
            List<AIPackageInfo> packages = new ArrayList<>(entries.length());
            for (int index = 0; index < entries.length(); index++) {
                packages.add(readPackage(entries.getJSONObject(index)));
            }
            return Collections.unmodifiableList(packages);
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
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
