package com.bliss.aimemorysearch;

import com.bliss.aimemorysearch.db.FileEntity;

import java.util.Locale;

/** Derives a display provenance from real type/path metadata. */
public final class ResultSourceLabel {
    private ResultSourceLabel() {}

    public static String from(FileEntity file) {
        if (file == null) return "DOCUMENT";
        if ("EMAIL".equalsIgnoreCase(file.type)) return "EMAIL";
        String path = file.path == null ? "" : file.path.replace('\\', '/');
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.contains("email_attachment")
                || lower.contains("email-attachment")) {
            return "EMAIL ATTACHMENT";
        }
        String[] parts = path.split("/");
        for (int index = parts.length - 2; index >= 0; index--) {
            String part = clean(parts[index]);
            if (part.contains("SCREENSHOT")) return "SCREENSHOTS";
            if (part.contains("WHATSAPP")) return "WHATSAPP";
            if (part.contains("TIKTOK")) return "TIKTOK";
        }
        for (String partValue : parts) {
            String part = clean(partValue);
            if (part.equals("DCIM")) return "DCIM";
            if (part.equals("DOWNLOAD") || part.equals("DOWNLOADS")) {
                return "DOWNLOADS";
            }
            if (part.equals("DOCUMENT") || part.equals("DOCUMENTS")) {
                return "DOCUMENTS";
            }
        }
        for (int index = parts.length - 2; index >= 0; index--) {
            String part = clean(parts[index]);
            if (part.isEmpty() || numeric(part) || infrastructure(part)) continue;
            return part;
        }
        return file.type == null || file.type.trim().isEmpty()
                ? "DOCUMENT" : clean(file.type);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim()
                .replace('_', ' ').replace('-', ' ')
                .toUpperCase(Locale.ROOT);
    }

    private static boolean numeric(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (!Character.isDigit(value.charAt(index))) return false;
        }
        return !value.isEmpty();
    }

    private static boolean infrastructure(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.equals("storage") || lower.equals("emulated")
                || lower.equals("primary") || lower.equals("media")
                || lower.equals("data") || lower.equals("files");
    }
}
