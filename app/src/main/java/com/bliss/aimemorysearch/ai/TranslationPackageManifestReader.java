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
import java.util.List;

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

        return new TranslationPackageManifest(
                jsonObject.getString(
                        "id"
                ),
                jsonObject.getString(
                        "displayName"
                ),
                jsonObject.getString(
                        "family"
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
                        "modelDirectory"
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
