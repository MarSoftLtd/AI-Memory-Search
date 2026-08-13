package com.bliss.aimemorysearch.email;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Provider-neutral boundary. Implementations own authentication and transport. */
public interface EmailSource {
    String getSourceId();

    Page fetchMessages(String continuationToken, int limit) throws IOException;

    InputStream openAttachment(String sourceMessageId, EmailAttachment attachment)
            throws IOException;

    final class Page {
        private final List<EmailMessage> messages;
        private final String continuationToken;

        public Page(List<EmailMessage> messages, String continuationToken) {
            this.messages = messages == null ? Collections.emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(messages));
            this.continuationToken = continuationToken;
        }

        public List<EmailMessage> getMessages() { return messages; }
        public String getContinuationToken() { return continuationToken; }
        public boolean hasMore() {
            return continuationToken != null && !continuationToken.isEmpty();
        }
    }
}
