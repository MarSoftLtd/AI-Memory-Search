package com.bliss.aimemorysearch.email;

import java.util.List;

/** Builds stable searchable text without provider-specific fields. */
public final class EmailIndexTextBuilder {
    private EmailIndexTextBuilder() {}

    public static String build(EmailMessage message) {
        if (message == null) return "";
        StringBuilder result = new StringBuilder();
        append(result, message.getSubject());
        append(result, message.getFrom());
        appendAll(result, message.getTo());
        appendAll(result, message.getCc());
        append(result, message.getBody());
        return result.toString();
    }

    private static void appendAll(StringBuilder result, List<String> values) {
        for (String value : values) append(result, value);
    }

    private static void append(StringBuilder result, String value) {
        if (value == null) return;
        String normalized = value.trim();
        if (normalized.isEmpty()) return;
        if (result.length() > 0) result.append('\n');
        result.append(normalized);
    }
}
