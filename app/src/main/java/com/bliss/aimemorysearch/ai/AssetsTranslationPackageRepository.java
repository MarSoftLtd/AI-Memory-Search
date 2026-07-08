package com.bliss.aimemorysearch.ai;

import android.content.Context;

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

    private final Context context;

    public AssetsTranslationPackageRepository(
            Context context
    ) {
        this.context =
                context.getApplicationContext();
    }

    @Override
    public List<TranslationPackageInfo> getPackages() {
        try {
            JSONArray packages =
                    readPackagesArray();

            List<TranslationPackageInfo> result =
                    new ArrayList<>();

            for (int i = 0; i < packages.length(); i++) {
                result.add(
                        readPackageInfo(
                                packages.getJSONObject(
                                        i
                                )
                        )
                );
            }

            return Collections.unmodifiableList(
                    result
            );
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private JSONArray readPackagesArray() throws Exception {

        String json =
                readAssetText();

        String trimmedJson =
                json.trim();

        if (trimmedJson.startsWith("[")) {
            return new JSONArray(
                    trimmedJson
            );
        }

        JSONObject root =
                new JSONObject(
                        trimmedJson
                );

        return root.getJSONArray(
                "packages"
        );
    }

    private TranslationPackageInfo readPackageInfo(
            JSONObject jsonObject
    ) throws Exception {

        return new TranslationPackageInfo(
                jsonObject.getString(
                        "packageId"
                ),
                jsonObject.getString(
                        "displayName"
                ),
                jsonObject.optString(
                        "description",
                        ""
                ),
                jsonObject.getString(
                        "version"
                ),
                jsonObject.optLong(
                        "sizeBytes",
                        0L
                ),
                jsonObject.optLong(
                        "requiredSpaceBytes",
                        0L
                ),
                jsonObject.optString(
                        "downloadUrl",
                        ""
                ),
                jsonObject.optString(
                        "checksumSha256",
                        ""
                ),
                jsonObject.getString(
                        "minAppVersion"
                )
        );
    }

    private String readAssetText() throws Exception {

        StringBuilder builder =
                new StringBuilder();

        try (
                InputStream inputStream =
                        context.getAssets().open(
                                REPOSITORY_ASSET_PATH
                        );
                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        inputStream,
                                        "UTF-8"
                                )
                        )
        ) {
            String line;

            while (
                    (line = reader.readLine()) != null
            ) {
                builder.append(
                        line
                );
                builder.append(
                        '\n'
                );
            }
        }

        return builder.toString();
    }
}
