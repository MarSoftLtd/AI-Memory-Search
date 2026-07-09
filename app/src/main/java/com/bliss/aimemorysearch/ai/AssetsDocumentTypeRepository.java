package com.bliss.aimemorysearch.ai;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AssetsDocumentTypeRepository implements DocumentTypeRepository {

    private static final String DOCUMENT_TYPES_ASSET_PATH =
            "nlp/lexicons/document_types.json";

    private final Context context;
    private Map<String, DocumentTypeDefinition> documentTypesById;
    private List<DocumentTypeDefinition> allDocumentTypes;

    public AssetsDocumentTypeRepository(
            Context context
    ) {
        this.context =
                context.getApplicationContext();
    }

    @Override
    public synchronized DocumentTypeDefinition findDocumentTypeById(
            String documentTypeId
    ) {
        if (documentTypeId == null) {
            return null;
        }

        ensureLoaded();

        return documentTypesById.get(
                documentTypeId.trim()
        );
    }

    @Override
    public synchronized List<DocumentTypeDefinition> getAllDocumentTypes() {
        ensureLoaded();

        return allDocumentTypes;
    }

    private void ensureLoaded() {
        if (documentTypesById != null) {
            return;
        }

        documentTypesById =
                new LinkedHashMap<>();
        allDocumentTypes =
                new ArrayList<>();

        loadDocumentTypes();

        freezeCaches();
    }

    private void loadDocumentTypes() {
        try {
            JSONObject root =
                    new JSONObject(
                            readAssetText()
                    );

            JSONArray documentTypes =
                    root.optJSONArray(
                            "documentTypes"
                    );

            if (documentTypes == null) {
                return;
            }

            for (
                    int i = 0;
                    i < documentTypes.length();
                    i++
            ) {
                JSONObject documentTypeObject =
                        documentTypes.optJSONObject(
                                i
                        );

                if (documentTypeObject == null) {
                    continue;
                }

                DocumentTypeDefinition definition =
                        new DocumentTypeDefinition(
                                documentTypeObject.optString(
                                        "id",
                                        ""
                                ),
                                documentTypeObject.optString(
                                        "canonicalValue",
                                        ""
                                ),
                                readLabelsByLanguage(
                                        documentTypeObject
                                )
                        );

                if (!definition.getId().isEmpty()) {
                    allDocumentTypes.add(
                            definition
                    );

                    if (!documentTypesById.containsKey(definition.getId())) {
                        documentTypesById.put(
                                definition.getId(),
                                definition
                        );
                    }
                }
            }
        } catch (Exception e) {
            return;
        }
    }

    private static Map<String, List<String>> readLabelsByLanguage(
            JSONObject documentTypeObject
    ) {
        Map<String, List<String>> result =
                new LinkedHashMap<>();

        JSONObject labels =
                documentTypeObject.optJSONObject(
                        "labels"
                );

        if (labels == null) {
            return result;
        }

        JSONArray languageCodes =
                labels.names();

        if (languageCodes == null) {
            return result;
        }

        for (
                int i = 0;
                i < languageCodes.length();
                i++
        ) {
            String languageCode =
                    languageCodes.optString(
                            i,
                            ""
                    ).trim();

            if (languageCode.isEmpty()) {
                continue;
            }

            JSONArray labelArray =
                    labels.optJSONArray(
                            languageCode
                    );

            if (labelArray == null) {
                continue;
            }

            List<String> labelValues =
                    readStringArray(
                            labelArray
                    );

            result.put(
                    languageCode,
                    labelValues
            );
        }

        return result;
    }

    private static List<String> readStringArray(
            JSONArray array
    ) {
        List<String> result =
                new ArrayList<>();

        for (
                int i = 0;
                i < array.length();
                i++
        ) {
            String value =
                    array.optString(
                            i,
                            ""
                    ).trim();

            if (!value.isEmpty()) {
                result.add(
                        value
                );
            }
        }

        return result;
    }

    private void freezeCaches() {
        documentTypesById =
                Collections.unmodifiableMap(
                        documentTypesById
                );
        allDocumentTypes =
                Collections.unmodifiableList(
                        allDocumentTypes
                );
    }

    private String readAssetText()
            throws IOException {
        try (
                InputStream inputStream =
                        context.getAssets().open(
                                DOCUMENT_TYPES_ASSET_PATH
                        );
                ByteArrayOutputStream outputStream =
                        new ByteArrayOutputStream()
        ) {
            byte[] buffer =
                    new byte[8192];
            int read;

            while (
                    (read = inputStream.read(buffer)) != -1
            ) {
                outputStream.write(
                        buffer,
                        0,
                        read
                );
            }

            return outputStream.toString(
                    StandardCharsets.UTF_8.name()
            );
        }
    }
}
