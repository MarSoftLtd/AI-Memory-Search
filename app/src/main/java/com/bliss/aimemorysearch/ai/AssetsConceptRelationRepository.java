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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AssetsConceptRelationRepository implements ConceptRelationRepository {

    private static final String RELATIONS_ASSET_PATH =
            "nlp/relations/relations.json";

    private final Context context;
    private Map<String, List<SemanticRelation>> relationCache;
    private List<SemanticRelation> allRelations;

    public AssetsConceptRelationRepository(
            Context context
    ) {
        this.context =
                context.getApplicationContext();
    }

    @Override
    public synchronized List<SemanticRelation> getRelations(
            String conceptId
    ) {
        if (conceptId == null) {
            return Collections.emptyList();
        }

        ensureLoaded();

        List<SemanticRelation> relations =
                relationCache.get(
                        conceptId.trim()
                );

        if (relations == null) {
            return Collections.emptyList();
        }

        return relations;
    }

    @Override
    public synchronized List<SemanticRelation> getAllRelations() {
        ensureLoaded();

        return allRelations;
    }

    private void ensureLoaded() {
        if (relationCache != null) {
            return;
        }

        relationCache =
                new HashMap<>();
        allRelations =
                new ArrayList<>();

        try {
            JSONObject root =
                    new JSONObject(
                            readAssetText()
                    );

            JSONArray relations =
                    root.optJSONArray(
                            "relations"
                    );

            if (relations == null) {
                freezeCache();
                return;
            }

            for (
                    int i = 0;
                    i < relations.length();
                    i++
            ) {
                JSONObject relationObject =
                        relations.optJSONObject(
                                i
                        );

                if (relationObject == null) {
                    continue;
                }

                addRelation(
                        relationObject
                );
            }

            freezeCache();
        } catch (Exception e) {
            relationCache =
                    Collections.emptyMap();
            allRelations =
                    Collections.emptyList();
        }
    }

    private void addRelation(
            JSONObject relationObject
    ) {
        String sourceConceptId =
                relationObject.optString(
                        "source",
                        ""
                ).trim();

        if (sourceConceptId.isEmpty()) {
            return;
        }

        SemanticRelationType relationType =
                parseRelationType(
                        relationObject.optString(
                                "type",
                                ""
                        )
                );

        if (relationType == null) {
            return;
        }

        String targetConceptId =
                relationObject.optString(
                        "target",
                        ""
                ).trim();

        if (targetConceptId.isEmpty()) {
            return;
        }

        List<SemanticRelation> relations =
                relationCache.get(
                        sourceConceptId
                );

        if (relations == null) {
            relations =
                    new ArrayList<>();
            relationCache.put(
                    sourceConceptId,
                    relations
            );
        }

        SemanticRelation relation =
                new SemanticRelation(
                        sourceConceptId,
                        relationType,
                        targetConceptId
                );

        relations.add(
                relation
        );
        allRelations.add(
                relation
        );
    }

    private void freezeCache() {
        for (Map.Entry<String, List<SemanticRelation>> entry : relationCache.entrySet()) {
            entry.setValue(
                    Collections.unmodifiableList(
                            entry.getValue()
                    )
            );
        }

        allRelations =
                Collections.unmodifiableList(
                        allRelations
                );
    }

    private static SemanticRelationType parseRelationType(
            String value
    ) {
        if (value == null) {
            return null;
        }

        try {
            return SemanticRelationType.valueOf(
                    value.trim()
            );
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String readAssetText() throws IOException {
        try (
                InputStream inputStream =
                        context.getAssets().open(
                                RELATIONS_ASSET_PATH
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
