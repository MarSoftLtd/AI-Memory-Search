package com.bliss.aimemorysearch.email;

import java.util.Objects;

/** Metadata for an attachment; attachment bytes remain owned by the EmailSource. */
public final class EmailAttachment {
    private final String fileName;
    private final String mimeType;
    private final String contentId;
    private final String disposition;
    private final long size;

    public EmailAttachment(String fileName, String mimeType, String contentId,
                           String disposition, long size) {
        this.fileName = value(fileName);
        this.mimeType = value(mimeType);
        this.contentId = value(contentId);
        this.disposition = value(disposition);
        this.size = Math.max(0L, size);
    }

    public String getFileName() { return fileName; }
    public String getMimeType() { return mimeType; }
    public String getContentId() { return contentId; }
    public String getDisposition() { return disposition; }
    public long getSize() { return size; }

    private static String value(String input) {
        return input == null ? "" : input;
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof EmailAttachment)) return false;
        EmailAttachment that = (EmailAttachment) other;
        return size == that.size && fileName.equals(that.fileName)
                && mimeType.equals(that.mimeType) && contentId.equals(that.contentId)
                && disposition.equals(that.disposition);
    }

    @Override public int hashCode() {
        return Objects.hash(fileName, mimeType, contentId, disposition, size);
    }
}
