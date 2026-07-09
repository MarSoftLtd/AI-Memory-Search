package com.bliss.aimemorysearch.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DocumentTypeDefinition {

    private final String id;
    private final String canonicalValue;
    private final Map<String, List<String>> labelsByLanguage;

    public DocumentTypeDefinition(
            String id,
            String canonicalValue,
            Map<String, List<String>> labelsByLanguage
    ) {
        this.id =
                nonNullString(
                        id
                );
        this.canonicalValue =
                nonNullString(
                        canonicalValue
                );
        this.labelsByLanguage =
                immutableLabelMap(
                        labelsByLanguage
                );
    }

    public String getId() {
        return id;
    }

    public String getCanonicalValue() {
        return canonicalValue;
    }

    public Map<String, List<String>> getLabelsByLanguage() {
        return labelsByLanguage;
    }

    public List<String> getLabels(
            String languageCode
    ) {
        if (languageCode == null) {
            return Collections.emptyList();
        }

        List<String> labels =
                labelsByLanguage.get(
                        languageCode.trim()
                );

        if (labels == null) {
            return Collections.emptyList();
        }

        return labels;
    }

    private static String nonNullString(
            String value
    ) {
        return value != null
                ? value
                : "";
    }

    private static Map<String, List<String>> immutableLabelMap(
            Map<String, List<String>> values
    ) {
        if (values == null) {
            return Collections.emptyMap();
        }

        Map<String, List<String>> result =
                new LinkedHashMap<>();

        for (Map.Entry<String, List<String>> entry : values.entrySet()) {
            result.put(
                    entry.getKey(),
                    immutableStringList(
                            entry.getValue()
                    )
            );
        }

        return Collections.unmodifiableMap(
                result
        );
    }

    private static List<String> immutableStringList(
            List<String> values
    ) {
        if (values == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(
                new ArrayList<>(
                        values
                )
        );
    }
}
