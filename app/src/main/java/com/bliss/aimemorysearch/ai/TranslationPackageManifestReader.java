package com.bliss.aimemorysearch.ai;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class TranslationPackageManifestReader {

    public TranslationPackageManifest read(
            TranslationPackageLayout layout
    ) throws IOException, JSONException {

        File manifestFile =
                layout.getManifestFile();

        if (
                !manifestFile.exists()
                        ||
                        !manifestFile.isFile()
        ) {
            throw new IOException(
                    "Translation package manifest is missing"
            );
        }

        JSONObject jsonObject =
                new JSONObject(
                        readText(
                                manifestFile
                        )
                );

        validateSchema(jsonObject);

        return new TranslationPackageManifest(
                jsonObject.getString(
                        "packageId"
                ),
                jsonObject.getString(
                        "displayName"
                ),
                jsonObject.getString(
                        "languageFamily"
                ),
                jsonObject.getString(
                        "version"
                ),
                jsonObject.getString(
                        "minimumAppVersion"
                ),
                jsonObject.getString(
                        "translatorEngine"
                ),
                jsonObject.getString(
                        "tokenizer"
                ),
                readSupportedLanguages(
                        jsonObject.getJSONArray(
                                "supportedLanguages"
                        )
                )
        );
    }

    private static void validateSchema(
            JSONObject jsonObject
    ) throws JSONException {

        Set<String> actualFields =
                new HashSet<>();
        Iterator<String> keys =
                jsonObject.keys();

        while (keys.hasNext()) {
            actualFields.add(keys.next());
        }

        if (
                !actualFields.equals(
                        TranslationPackageManifestSchema.getFields()
                )
        ) {
            throw new JSONException(
                    "Translation package manifest schema is invalid"
            );
        }

        requireNonEmptyString(jsonObject, "packageId");
        requireNonEmptyString(jsonObject, "displayName");
        if (!"TRANSLATION".equals(jsonObject.getString("packageType"))) {
            throw new JSONException(
                    "Translation package type is invalid"
            );
        }
        requireNonEmptyString(jsonObject, "languageFamily");
        if (jsonObject.getJSONArray("supportedLanguages").length() == 0) {
            throw new JSONException(
                    "Translation package languages are missing"
            );
        }
        jsonObject.getInt("packageFormatVersion");
        requireNonEmptyString(jsonObject, "modelVersion");
        requireNonEmptyString(jsonObject, "translatorEngine");
        requireNonEmptyString(jsonObject, "tokenizer");
        requireNonEmptyString(jsonObject, "version");
        requireNonEmptyString(jsonObject, "minimumAppVersion");
        requireNonEmptyString(jsonObject, "runtime");
        requireNonEmptyString(jsonObject, "architecture");
        requireNonEmptyString(jsonObject, "createdBy");
        requireNonEmptyString(jsonObject, "buildDate");
        requireNonEmptyString(jsonObject, "checksum");
        jsonObject.getLong("compressedSize");
        jsonObject.getLong("uncompressedSize");
    }

    private static void requireNonEmptyString(
            JSONObject jsonObject,
            String field
    ) throws JSONException {

        if (jsonObject.getString(field).trim().isEmpty()) {
            throw new JSONException(
                    "Translation package manifest field is empty: "
                            + field
            );
        }
    }

    private static List<String> readSupportedLanguages(
            JSONArray jsonArray
    ) throws JSONException {

        List<String> languages =
                new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            languages.add(
                    jsonArray.getString(
                            i
                    )
            );
        }

        return languages;
    }

    private static String readText(
            File file
    ) throws IOException {

        StringBuilder builder =
                new StringBuilder();

        try (
                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        new FileInputStream(
                                                file
                                        ),
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
