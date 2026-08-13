package com.bliss.aimemorysearch.email.gmail;

import com.bliss.aimemorysearch.email.EmailAttachment;
import com.bliss.aimemorysearch.email.EmailMessage;
import com.bliss.aimemorysearch.email.EmailSource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Gmail REST implementation. Authorization tokens are supplied by Google Identity Services. */
public final class GmailEmailSource implements EmailSource {
    private static final String API = "https://gmail.googleapis.com/gmail/v1/users/me";
    private final String accessToken;

    public GmailEmailSource(String accessToken) {
        if (accessToken == null || accessToken.trim().isEmpty()) {
            throw new IllegalArgumentException("accessToken must not be empty");
        }
        this.accessToken = accessToken;
    }

    @Override public String getSourceId() { return "gmail"; }

    public Profile getProfile() throws IOException {
        JSONObject json = get(API + "/profile");
        return new Profile(json.optString("emailAddress"), json.optString("historyId"));
    }

    @Override public Page fetchMessages(String continuationToken, int limit) throws IOException {
        int bounded = Math.max(1, Math.min(500, limit));
        String url = API + "/messages?maxResults=" + bounded;
        if (continuationToken != null && !continuationToken.isEmpty()) {
            url += "&pageToken=" + encode(continuationToken);
        }
        JSONObject response = get(url);
        JSONArray summaries = response.optJSONArray("messages");
        List<EmailMessage> messages = new ArrayList<>();
        if (summaries != null) {
            for (int i = 0; i < summaries.length(); i++) {
                messages.add(fetchMessage(summaries.optJSONObject(i).optString("id")));
            }
        }
        return new Page(messages, emptyToNull(response.optString("nextPageToken")));
    }

    public EmailMessage fetchMessage(String messageId) throws IOException {
        JSONObject message = get(API + "/messages/" + encode(messageId) + "?format=full");
        hydrateExternalTextBodies(messageId, message.optJSONObject("payload"));
        return parseMessage(message);
    }

    static EmailMessage parseMessage(JSONObject json) {
        JSONObject payload = json.optJSONObject("payload");
        String subject = header(payload, "Subject");
        String from = header(payload, "From");
        List<String> to = addresses(header(payload, "To"));
        List<String> cc = addresses(header(payload, "Cc"));
        Bodies bodies = new Bodies();
        collectBodies(payload, bodies);
        String body = !bodies.plain.isEmpty() ? join(bodies.plain) : join(bodies.html);
        return new EmailMessage(
                json.optString("id"), json.optString("threadId"), subject, from,
                to, cc, body, Collections.emptyList(), json.optLong("internalDate"));
    }

    private void hydrateExternalTextBodies(String messageId, JSONObject part) throws IOException {
        if (part == null) return;
        String mime = part.optString("mimeType");
        JSONObject body = part.optJSONObject("body");
        if (("text/plain".equalsIgnoreCase(mime) || "text/html".equalsIgnoreCase(mime))
                && body != null && body.optString("data").isEmpty()
                && !body.optString("attachmentId").isEmpty()) {
            JSONObject external = get(API + "/messages/" + encode(messageId)
                    + "/attachments/" + encode(body.optString("attachmentId")));
            try { body.put("data", external.optString("data")); }
            catch (JSONException invalid) { throw new IOException("Invalid Gmail text body", invalid); }
        }
        JSONArray parts = part.optJSONArray("parts");
        if (parts != null) for (int i = 0; i < parts.length(); i++) {
            hydrateExternalTextBodies(messageId, parts.optJSONObject(i));
        }
    }

    public HistoryPage fetchHistory(String startHistoryId, String pageToken)
            throws IOException {
        String url = API + "/history?startHistoryId=" + encode(startHistoryId)
                + "&maxResults=500";
        if (pageToken != null && !pageToken.isEmpty()) {
            url += "&pageToken=" + encode(pageToken);
        }
        JSONObject response = get(url);
        Set<String> added = new LinkedHashSet<>();
        Set<String> deleted = new LinkedHashSet<>();
        Set<String> modified = new LinkedHashSet<>();
        JSONArray history = response.optJSONArray("history");
        if (history != null) {
            for (int i = 0; i < history.length(); i++) {
                JSONObject record = history.optJSONObject(i);
                collectIds(record.optJSONArray("messagesAdded"), added);
                collectIds(record.optJSONArray("messagesDeleted"), deleted);
                collectIds(record.optJSONArray("labelsAdded"), modified);
                collectIds(record.optJSONArray("labelsRemoved"), modified);
            }
        }
        added.removeAll(deleted);
        modified.removeAll(deleted);
        added.addAll(modified);
        return new HistoryPage(new ArrayList<>(added), new ArrayList<>(deleted),
                emptyToNull(response.optString("nextPageToken")),
                response.optString("historyId", startHistoryId));
    }

    @Override public InputStream openAttachment(String sourceMessageId,
                                                 EmailAttachment attachment) {
        throw new UnsupportedOperationException("Attachments are not enabled in this stage");
    }

    private JSONObject get(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);
        connection.setRequestProperty("Accept", "application/json");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        int status = connection.getResponseCode();
        InputStream stream = status >= 200 && status < 300
                ? connection.getInputStream() : connection.getErrorStream();
        String response = stream == null ? "" : new String(readAll(stream), StandardCharsets.UTF_8);
        connection.disconnect();
        if (status < 200 || status >= 300) throw new GmailApiException(status, response);
        try { return new JSONObject(response); }
        catch (JSONException invalid) { throw new IOException("Invalid Gmail response", invalid); }
    }

    private static void collectBodies(JSONObject part, Bodies bodies) {
        if (part == null) return;
        String fileName = part.optString("filename");
        String mime = part.optString("mimeType");
        if (fileName.isEmpty()) {
            JSONObject body = part.optJSONObject("body");
            String data = body == null ? "" : body.optString("data");
            if (!data.isEmpty()) {
                String text = new String(java.util.Base64.getUrlDecoder().decode(data),
                        StandardCharsets.UTF_8);
                if ("text/plain".equalsIgnoreCase(mime)) bodies.plain.add(text.trim());
                else if ("text/html".equalsIgnoreCase(mime)) bodies.html.add(htmlToText(text));
            }
        }
        JSONArray parts = part.optJSONArray("parts");
        if (parts != null) for (int i = 0; i < parts.length(); i++) {
            collectBodies(parts.optJSONObject(i), bodies);
        }
    }

    private static String header(JSONObject payload, String name) {
        if (payload == null) return "";
        JSONArray headers = payload.optJSONArray("headers");
        if (headers != null) for (int i = 0; i < headers.length(); i++) {
            JSONObject header = headers.optJSONObject(i);
            if (name.equalsIgnoreCase(header.optString("name"))) return header.optString("value");
        }
        return "";
    }

    private static List<String> addresses(String header) {
        if (header == null || header.trim().isEmpty()) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        boolean quoted = false;
        int start = 0;
        for (int i = 0; i <= header.length(); i++) {
            if (i < header.length() && header.charAt(i) == '"') quoted = !quoted;
            if (i == header.length() || (header.charAt(i) == ',' && !quoted)) {
                String value = header.substring(start, i).trim();
                if (!value.isEmpty()) result.add(value);
                start = i + 1;
            }
        }
        return result;
    }

    private static void collectIds(JSONArray changes, Set<String> target) {
        if (changes == null) return;
        for (int i = 0; i < changes.length(); i++) {
            JSONObject wrapper = changes.optJSONObject(i);
            JSONObject message = wrapper == null ? null : wrapper.optJSONObject("message");
            if (message != null && !message.optString("id").isEmpty()) target.add(message.optString("id"));
        }
    }

    private static String htmlToText(String html) {
        return html.replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", " ")
                .replaceAll("(?i)<br\\s*/?>|</p\\s*>|</div\\s*>", "\n")
                .replaceAll("(?s)<[^>]+>", " ").replace("&nbsp;", " ")
                .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&#39;", "'")
                .replaceAll("[ \\t\\r]+", " ").replaceAll(" *\\n+ *", "\n").trim();
    }

    private static String join(List<String> values) {
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (value == null || value.trim().isEmpty()) continue;
            if (result.length() > 0) result.append('\n');
            result.append(value.trim());
        }
        return result.toString();
    }

    private static String encode(String value) throws IOException {
        return URLEncoder.encode(value, "UTF-8");
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    private static byte[] readAll(InputStream input) throws IOException {
        try (InputStream source = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = source.read(buffer)) != -1) output.write(buffer, 0, read);
            return output.toByteArray();
        }
    }

    private static final class Bodies {
        final List<String> plain = new ArrayList<>();
        final List<String> html = new ArrayList<>();
    }

    public static final class Profile {
        public final String emailAddress;
        public final String historyId;
        Profile(String emailAddress, String historyId) {
            this.emailAddress = emailAddress;
            this.historyId = historyId;
        }
    }

    public static final class HistoryPage {
        public final List<String> addedMessageIds;
        public final List<String> deletedMessageIds;
        public final String nextPageToken;
        public final String historyId;
        HistoryPage(List<String> added, List<String> deleted, String next, String historyId) {
            this.addedMessageIds = added;
            this.deletedMessageIds = deleted;
            this.nextPageToken = next;
            this.historyId = historyId;
        }
    }

    public static final class GmailApiException extends IOException {
        public final int statusCode;
        GmailApiException(int statusCode, String response) {
            super("Gmail API HTTP " + statusCode + ": " + response);
            this.statusCode = statusCode;
        }
    }
}
