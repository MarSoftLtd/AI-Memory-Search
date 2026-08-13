package com.bliss.aimemorysearch.email;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;

public final class EmailIdentity {
    private EmailIdentity() {}

    public static String stableId(String provider, String account, String messageId) {
        String normalizedProvider = required(provider, "provider").toLowerCase(Locale.ROOT);
        return "email://" + component(normalizedProvider) + "/"
                + component(required(account, "account")) + "/"
                + component(required(messageId, "messageId"));
    }

    private static String component(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        return value.trim();
    }
}
