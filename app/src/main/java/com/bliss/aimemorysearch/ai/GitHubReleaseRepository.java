package com.bliss.aimemorysearch.ai;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GitHubReleaseRepository implements AiPackageRepository {

    public interface MetadataSource {

        String loadMetadata() throws Exception;
    }

    private final MetadataSource metadataSource;

    public GitHubReleaseRepository(
            MetadataSource metadataSource
    ) {
        this.metadataSource =
                metadataSource;
    }

    @Override
    public List<AiPackageInfo> getPackages() {
        try {
            if (metadataSource == null) {
                return Collections.emptyList();
            }

            JSONArray packages =
                    readPackagesArray(
                            metadataSource.loadMetadata()
                    );

            List<AiPackageInfo> result =
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

    public List<String> getPackageVersions(
            String packageId
    ) {
        if (
                packageId == null
                        ||
                        packageId.trim().isEmpty()
        ) {
            return Collections.emptyList();
        }

        List<String> versions =
                new ArrayList<>();

        for (AiPackageInfo packageInfo : getPackages()) {
            if (
                    packageId.equals(
                            packageInfo.getPackageId()
                    )
            ) {
                versions.add(
                        packageInfo.getVersion()
                );
            }
        }

        return Collections.unmodifiableList(
                versions
        );
    }

    private static JSONArray readPackagesArray(
            String json
    ) throws Exception {
        String trimmedJson =
                json == null
                        ? ""
                        : json.trim();

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

    private static AiPackageInfo readPackageInfo(
            JSONObject jsonObject
    ) throws Exception {
        return new AiPackageInfo(
                jsonObject.getString(
                        "packageId"
                ),
                AiPackageType.valueOf(
                        jsonObject.getString(
                                "packageType"
                        )
                ),
                jsonObject.getString(
                        "capabilityKey"
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
                jsonObject.optString(
                        "minAppVersion",
                        ""
                )
        );
    }
}
