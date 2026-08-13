package com.bliss.aimemorysearch;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EmailOpenUriTest {
    @Test public void convertsGmailApiHexThreadIdForConversationUri() {
        assertEquals("127", EmailOpenUri.gmailConversationId("7f"));
        assertEquals("1234567890123456789",
                EmailOpenUri.gmailConversationId("112210f47de98115"));
    }

    @Test public void preservesNonHexProviderIdSafely() {
        assertEquals("thread-id", EmailOpenUri.gmailConversationId("thread-id"));
    }
}
