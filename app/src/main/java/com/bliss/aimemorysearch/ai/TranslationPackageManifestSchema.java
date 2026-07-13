package com.bliss.aimemorysearch.ai;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

final class TranslationPackageManifestSchema {

    private static final Set<String> FIELDS =
            Collections.unmodifiableSet(
                    new HashSet<>(
                            Arrays.asList(
                                    "packageId",
                                    "displayName",
                                    "packageType",
                                    "languageFamily",
                                    "supportedLanguages",
                                    "packageFormatVersion",
                                    "modelVersion",
                                    "translatorEngine",
                                    "tokenizer",
                                    "version",
                                    "minimumAppVersion",
                                    "runtime",
                                    "architecture",
                                    "createdBy",
                                    "buildDate",
                                    "checksum",
                                    "compressedSize",
                                    "uncompressedSize"
                            )
                    )
            );

    private TranslationPackageManifestSchema() {
    }

    static Set<String> getFields() {
        return FIELDS;
    }

    static boolean hasExactFields(
            File manifestFile
    ) throws IOException {
        return readTopLevelFields(
                readText(manifestFile)
        ).equals(FIELDS);
    }

    private static Set<String> readTopLevelFields(
            String json
    ) throws IOException {

        Set<String> fields =
                new HashSet<>();
        int depth = 0;
        int index = 0;

        while (index < json.length()) {
            char current = json.charAt(index);

            if (current == '{' || current == '[') {
                depth++;
                index++;
                continue;
            }

            if (current == '}' || current == ']') {
                depth--;
                index++;
                continue;
            }

            if (current != '"') {
                index++;
                continue;
            }

            StringBuilder value =
                    new StringBuilder();
            index++;
            boolean escaped = false;

            while (index < json.length()) {
                current = json.charAt(index++);

                if (escaped) {
                    value.append(current);
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    break;
                } else {
                    value.append(current);
                }
            }

            if (current != '"') {
                throw new IOException(
                        "Translation package manifest contains an unterminated string"
                );
            }

            int separator = index;
            while (
                    separator < json.length()
                            &&
                            Character.isWhitespace(
                                    json.charAt(separator)
                            )
            ) {
                separator++;
            }

            if (
                    depth == 1
                            &&
                            separator < json.length()
                            &&
                            json.charAt(separator) == ':'
            ) {
                if (!fields.add(value.toString())) {
                    throw new IOException(
                            "Translation package manifest contains a duplicate field"
                    );
                }
            }
        }

        if (depth != 0) {
            throw new IOException(
                    "Translation package manifest structure is invalid"
            );
        }

        return fields;
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
                                        new FileInputStream(file),
                                        "UTF-8"
                                )
                        )
        ) {
            char[] buffer = new char[4096];
            int count;

            while ((count = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, count);
            }
        }

        return builder.toString();
    }
}
