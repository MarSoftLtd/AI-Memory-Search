package com.bliss.aimemorysearch.email.gmail;

import com.bliss.aimemorysearch.email.EmailMessage;

import org.json.JSONObject;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GmailEmailSourceTest {
    @Test public void mapsGmailFullMessageAndPrefersPlainBody() throws Exception {
        String plain = encoded("Preferred plain body");
        String html = encoded("<p>HTML fallback</p>");
        JSONObject json = new JSONObject("{"
                + "\"id\":\"gmail-id\",\"threadId\":\"thread-id\",\"internalDate\":\"12345\","
                + "\"payload\":{\"mimeType\":\"multipart/alternative\",\"headers\":["
                + "{\"name\":\"Subject\",\"value\":\"Status\"},"
                + "{\"name\":\"From\",\"value\":\"Sender <sender@example.com>\"},"
                + "{\"name\":\"To\",\"value\":\"One <one@example.com>, Two <two@example.com>\"},"
                + "{\"name\":\"Cc\",\"value\":\"Copy <copy@example.com>\"}],"
                + "\"parts\":["
                + "{\"mimeType\":\"text/html\",\"filename\":\"\",\"body\":{\"data\":\"" + html + "\"}},"
                + "{\"mimeType\":\"text/plain\",\"filename\":\"\",\"body\":{\"data\":\"" + plain + "\"}}]}}" );

        EmailMessage message = GmailEmailSource.parseMessage(json);
        assertEquals("gmail-id", message.getSourceMessageId());
        assertEquals("thread-id", message.getThreadId());
        assertEquals(12345L, message.getTimestamp());
        assertEquals("Status", message.getSubject());
        assertEquals("Sender <sender@example.com>", message.getFrom());
        assertEquals(2, message.getTo().size());
        assertEquals(1, message.getCc().size());
        assertEquals("Preferred plain body", message.getBody());
        assertTrue(message.getAttachments().isEmpty());
    }

    @Test public void convertsHtmlWhenPlainBodyIsAbsent() throws Exception {
        String html = encoded("<h1>Update</h1><p>Ready &amp; local.</p>");
        JSONObject json = new JSONObject("{\"id\":\"id\",\"threadId\":\"t\","
                + "\"internalDate\":\"5\",\"payload\":{\"mimeType\":\"text/html\","
                + "\"headers\":[],\"body\":{\"data\":\"" + html + "\"}}}");
        EmailMessage message = GmailEmailSource.parseMessage(json);
        assertTrue(message.getBody().contains("Update"));
        assertTrue(message.getBody().contains("Ready & local."));
        assertFalse(message.getBody().contains("<p>"));
    }

    private static String encoded(String text) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }
}
