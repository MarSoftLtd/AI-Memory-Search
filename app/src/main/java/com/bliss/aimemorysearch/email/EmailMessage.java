package com.bliss.aimemorysearch.email;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Provider-neutral email content used by parsing and future source adapters. */
public final class EmailMessage {
    private final String sourceMessageId;
    private final String threadId;
    private final String subject;
    private final String from;
    private final List<String> to;
    private final List<String> cc;
    private final String body;
    private final List<EmailAttachment> attachments;
    private final long timestamp;

    public EmailMessage(String sourceMessageId, String subject, String from,
                        List<String> to, List<String> cc, String body,
                        List<EmailAttachment> attachments) {
        this(sourceMessageId, "", subject, from, to, cc, body, attachments, 0L);
    }

    public EmailMessage(String sourceMessageId, String threadId, String subject, String from,
                        List<String> to, List<String> cc, String body,
                        List<EmailAttachment> attachments, long timestamp) {
        this.sourceMessageId = value(sourceMessageId);
        this.threadId = value(threadId);
        this.subject = value(subject);
        this.from = value(from);
        this.to = immutable(to);
        this.cc = immutable(cc);
        this.body = value(body);
        this.attachments = attachments == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(attachments));
        this.timestamp = Math.max(0L, timestamp);
    }

    public String getSourceMessageId() { return sourceMessageId; }
    public String getThreadId() { return threadId; }
    public String getSubject() { return subject; }
    public String getFrom() { return from; }
    public List<String> getTo() { return to; }
    public List<String> getCc() { return cc; }
    public String getBody() { return body; }
    public List<EmailAttachment> getAttachments() { return attachments; }
    public long getTimestamp() { return timestamp; }

    private static List<String> immutable(List<String> values) {
        return values == null ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(values));
    }

    private static String value(String input) { return input == null ? "" : input; }
}
