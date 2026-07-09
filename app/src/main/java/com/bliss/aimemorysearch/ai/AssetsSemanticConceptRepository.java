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

public final class AssetsSemanticConceptRepository implements SemanticConceptRepository {

    private static final String CONCEPTS_ASSET_ROOT =
            "nlp/concepts";

    private final Context context;
    private Map<String, SemanticConcept> conceptsByToken;
    private Map<String, SemanticConcept> conceptsById;
    private List<SemanticConcept> allConcepts;

    public AssetsSemanticConceptRepository(
            Context context
    ) {
        this.context =
                context.getApplicationContext();
    }

    @Override
    public synchronized SemanticConcept findConcept(
            String token
    ) {
        return findConceptByToken(
                token
        );
    }

    @Override
    public synchronized SemanticConcept findConceptByToken(
            String token
    ) {
        if (token == null) {
            return null;
        }

        ensureLoaded();

        return conceptsByToken.get(
                token.trim()
        );
    }

    @Override
    public synchronized SemanticConcept findConceptById(
            String conceptId
    ) {
        if (conceptId == null) {
            return null;
        }

        ensureLoaded();

        return conceptsById.get(
                conceptId.trim()
        );
    }

    @Override
    public synchronized List<SemanticConcept> getAllConcepts() {
        ensureLoaded();

        return allConcepts;
    }

    private void ensureLoaded() {
        if (conceptsByToken != null) {
            return;
        }

        conceptsByToken =
                new LinkedHashMap<>();
        conceptsById =
                new LinkedHashMap<>();
        allConcepts =
                new ArrayList<>();

        String[] files =
                listConceptFiles();

        for (String file : files) {
            if (
                    file != null
                            &&
                            file.endsWith(
                                    ".json"
                            )
            ) {
                loadConceptFile(
                        CONCEPTS_ASSET_ROOT
                                + "/"
                                + file
                );
            }
        }

        freezeCaches();
    }

    private String[] listConceptFiles() {
        try {
            String[] files =
                    context.getAssets().list(
                            CONCEPTS_ASSET_ROOT
                    );

            return files != null
                    ? files
                    : new String[0];
        } catch (IOException e) {
            return new String[0];
        }
    }

    private void loadConceptFile(
            String assetPath
    ) {
        try {
            JSONObject root =
                    new JSONObject(
                            readAssetText(
                                    assetPath
                            )
                    );

            String language =
                    root.optString(
                            "language",
                            ""
                    );

            JSONArray concepts =
                    root.optJSONArray(
                            "concepts"
                    );

            if (concepts == null) {
                return;
            }

            for (
                    int i = 0;
                    i < concepts.length();
                    i++
            ) {
                JSONObject conceptObject =
                        concepts.optJSONObject(
                                i
                        );

                if (conceptObject == null) {
                    continue;
                }

                List<String> tokens =
                        readTokens(
                                conceptObject
                        );

                SemanticConcept concept =
                        new SemanticConcept(
                                conceptObject.optString(
                                        "id",
                                        ""
                                ),
                                conceptObject.optString(
                                        "label",
                                        ""
                                ),
                                language,
                                tokens
                        );

                if (!concept.getId().isEmpty()) {
                    allConcepts.add(
                            concept
                    );

                    if (!conceptsById.containsKey(concept.getId())) {
                        conceptsById.put(
                                concept.getId(),
                                concept
                        );
                    }
                }

                for (String token : concept.getTokens()) {
                    if (!token.isEmpty()) {
                        conceptsByToken.put(
                                token,
                                concept
                        );
                    }
                }
            }
        } catch (Exception e) {
            return;
        }
    }

    private void freezeCaches() {
        conceptsByToken =
                Collections.unmodifiableMap(
                        conceptsByToken
                );
        conceptsById =
                Collections.unmodifiableMap(
                        conceptsById
                );
        allConcepts =
                Collections.unmodifiableList(
                        allConcepts
                );
    }

    private static List<String> readTokens(
            JSONObject conceptObject
    ) {
        List<String> tokens =
                new ArrayList<>();

        JSONArray tokenArray =
                conceptObject.optJSONArray(
                        "tokens"
                );

        if (tokenArray == null) {
            return tokens;
        }

        for (
                int i = 0;
                i < tokenArray.length();
                i++
        ) {
            String token =
                    tokenArray.optString(
                            i,
                            ""
                    ).trim();

            if (!token.isEmpty()) {
                tokens.add(
                        token
                );
            }
        }

        return tokens;
    }

    private String readAssetText(
            String assetPath
    ) throws IOException {
        try (
                InputStream inputStream =
                        context.getAssets().open(
                                assetPath
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
