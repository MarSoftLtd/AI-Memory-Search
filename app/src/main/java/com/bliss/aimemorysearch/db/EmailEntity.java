package com.bliss.aimemorysearch.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "emails",
        indices = {
                @Index(value = {"provider", "account", "messageId"}, unique = true),
                @Index("threadId"),
                @Index("timestamp")
        }
)
public class EmailEntity {
    @PrimaryKey
    @NonNull
    public String id;
    @NonNull
    public String provider;
    @NonNull
    public String account;
    @NonNull
    public String messageId;
    public String threadId;
    public String subject;
    public String sender;
    public String toRecipients;
    public String ccRecipients;
    public String body;
    public long timestamp;
    public int attachmentCount;
    public long indexedAt;

    public EmailEntity(@NonNull String id, @NonNull String provider,
                       @NonNull String account, @NonNull String messageId,
                       String threadId, String subject, String sender,
                       String toRecipients, String ccRecipients, String body,
                       long timestamp, int attachmentCount, long indexedAt) {
        this.id = id;
        this.provider = provider;
        this.account = account;
        this.messageId = messageId;
        this.threadId = threadId;
        this.subject = subject;
        this.sender = sender;
        this.toRecipients = toRecipients;
        this.ccRecipients = ccRecipients;
        this.body = body;
        this.timestamp = timestamp;
        this.attachmentCount = attachmentCount;
        this.indexedAt = indexedAt;
    }
}
