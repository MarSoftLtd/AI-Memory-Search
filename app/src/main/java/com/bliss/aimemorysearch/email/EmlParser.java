package com.bliss.aimemorysearch.email;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Local RFC 5322/MIME parser for imported .eml messages. */
public final class EmlParser {
    private static final Pattern PARAMETER = Pattern.compile(
            "(?i)(?:^|;)\\s*([a-z0-9_-]+)\\s*=\\s*(?:\"([^\"]*)\"|([^;\\s]*))");
    private static final Pattern ENCODED_WORD = Pattern.compile(
            "=\\?([^?]+)\\?([bBqQ])\\?([^?]*)\\?=");

    public EmailMessage parse(InputStream input) throws IOException {
        if (input == null) throw new IllegalArgumentException("input == null");
        return parse(readAll(input));
    }

    public EmailMessage parse(byte[] emlBytes) throws IOException {
        if (emlBytes == null) throw new IllegalArgumentException("emlBytes == null");
        Part root = parsePart(emlBytes);
        List<String> plainBodies = new ArrayList<>();
        List<String> htmlBodies = new ArrayList<>();
        List<EmailAttachment> attachments = new ArrayList<>();
        collect(root, plainBodies, htmlBodies, attachments);
        String body = join(!plainBodies.isEmpty() ? plainBodies : htmlBodies);
        return new EmailMessage(
                trimBrackets(decodeHeader(root.header("message-id"))),
                decodeHeader(root.header("subject")),
                decodeHeader(root.header("from")),
                addresses(root.header("to")),
                addresses(root.header("cc")),
                body,
                attachments);
    }

    private static void collect(Part part, List<String> plain, List<String> html,
                                List<EmailAttachment> attachments) throws IOException {
        String type = baseType(part.header("content-type"));
        String dispositionHeader = part.header("content-disposition");
        String disposition = baseType(dispositionHeader);
        String fileName = parameter(dispositionHeader, "filename");
        if (fileName.isEmpty()) fileName = parameter(part.header("content-type"), "name");
        boolean attachment = "attachment".equals(disposition) || !fileName.isEmpty();

        if (type.startsWith("multipart/")) {
            String boundary = parameter(part.header("content-type"), "boundary");
            if (!boundary.isEmpty()) {
                for (byte[] child : splitMultipart(part.body, boundary)) {
                    collect(parsePart(child), plain, html, attachments);
                }
            }
            return;
        }

        byte[] decoded = decodeBody(part.body, part.header("content-transfer-encoding"));
        if (attachment) {
            attachments.add(new EmailAttachment(
                    decodeHeader(fileName), type, trimBrackets(part.header("content-id")),
                    disposition, decoded.length));
            return;
        }

        Charset charset = charset(parameter(part.header("content-type"), "charset"));
        String text = new String(decoded, charset).trim();
        if ("text/plain".equals(type)) plain.add(text);
        else if ("text/html".equals(type)) html.add(htmlToText(text));
    }

    private static Part parsePart(byte[] bytes) {
        int separator = headerSeparator(bytes);
        int bodyStart = separator < 0 ? bytes.length : separator;
        int separatorLength = separator < 0 ? 0
                : (bodyStart + 3 < bytes.length && bytes[bodyStart] == '\r' ? 4 : 2);
        String rawHeaders = new String(bytes, 0, bodyStart, StandardCharsets.ISO_8859_1);
        Map<String, String> headers = parseHeaders(rawHeaders);
        byte[] body = new byte[Math.max(0, bytes.length - bodyStart - separatorLength)];
        System.arraycopy(bytes, bodyStart + separatorLength, body, 0, body.length);
        return new Part(headers, body);
    }

    private static int headerSeparator(byte[] bytes) {
        for (int i = 0; i < bytes.length - 1; i++) {
            if (i + 3 < bytes.length && bytes[i] == '\r' && bytes[i + 1] == '\n'
                    && bytes[i + 2] == '\r' && bytes[i + 3] == '\n') return i;
            if (bytes[i] == '\n' && bytes[i + 1] == '\n') return i;
        }
        return -1;
    }

    private static Map<String, String> parseHeaders(String raw) {
        Map<String, String> headers = new LinkedHashMap<>();
        String unfolded = raw.replaceAll("\\r?\\n[ \\t]+", " ");
        for (String line : unfolded.split("\\r?\\n")) {
            int colon = line.indexOf(':');
            if (colon <= 0) continue;
            String name = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String value = line.substring(colon + 1).trim();
            headers.merge(name, value, (left, right) -> left + ", " + right);
        }
        return headers;
    }

    private static List<byte[]> splitMultipart(byte[] body, String boundary) {
        String source = new String(body, StandardCharsets.ISO_8859_1)
                .replace("\r\n", "\n");
        String delimiter = "--" + boundary;
        List<byte[]> result = new ArrayList<>();
        int cursor = source.indexOf(delimiter);
        while (cursor >= 0) {
            int start = cursor + delimiter.length();
            if (source.startsWith("--", start)) break;
            if (source.startsWith("\n", start)) start++;
            int next = source.indexOf("\n" + delimiter, start);
            if (next < 0) break;
            String part = source.substring(start, next);
            result.add(part.getBytes(StandardCharsets.ISO_8859_1));
            cursor = next + 1;
        }
        return result;
    }

    private static byte[] decodeBody(byte[] body, String encoding) throws IOException {
        String normalized = encoding == null ? "" : encoding.trim().toLowerCase(Locale.ROOT);
        if ("base64".equals(normalized)) {
            try { return Base64.getMimeDecoder().decode(body); }
            catch (IllegalArgumentException invalid) {
                throw new IOException("Invalid base64 MIME body", invalid);
            }
        }
        if ("quoted-printable".equals(normalized)) return decodeQuotedPrintable(body, false);
        return body;
    }

    private static byte[] decodeQuotedPrintable(byte[] input, boolean headerMode) {
        ByteArrayOutputStream output = new ByteArrayOutputStream(input.length);
        for (int i = 0; i < input.length; i++) {
            int current = input[i] & 0xff;
            if (headerMode && current == '_') { output.write(' '); continue; }
            if (current == '=' && i + 1 < input.length) {
                if (input[i + 1] == '\n') { i++; continue; }
                if (i + 2 < input.length && input[i + 1] == '\r' && input[i + 2] == '\n') {
                    i += 2; continue;
                }
                if (i + 2 < input.length) {
                    int high = Character.digit((char) input[i + 1], 16);
                    int low = Character.digit((char) input[i + 2], 16);
                    if (high >= 0 && low >= 0) { output.write((high << 4) + low); i += 2; continue; }
                }
            }
            output.write(current);
        }
        return output.toByteArray();
    }

    private static String decodeHeader(String value) {
        if (value == null || value.isEmpty()) return "";
        Matcher matcher = ENCODED_WORD.matcher(value);
        StringBuffer decoded = new StringBuffer();
        while (matcher.find()) {
            try {
                byte[] bytes = matcher.group(2).equalsIgnoreCase("B")
                        ? Base64.getDecoder().decode(matcher.group(3))
                        : decodeQuotedPrintable(matcher.group(3).getBytes(StandardCharsets.ISO_8859_1), true);
                matcher.appendReplacement(decoded, Matcher.quoteReplacement(
                        new String(bytes, charset(matcher.group(1)))));
            } catch (RuntimeException ignored) {
                matcher.appendReplacement(decoded, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(decoded);
        return decoded.toString().replaceAll("\\?=\\s+=\\?", "?==?").trim();
    }

    private static List<String> addresses(String header) {
        List<String> result = new ArrayList<>();
        if (header == null || header.trim().isEmpty()) return result;
        String decoded = decodeHeader(header);
        boolean quoted = false;
        int start = 0;
        for (int i = 0; i <= decoded.length(); i++) {
            if (i < decoded.length() && decoded.charAt(i) == '"') quoted = !quoted;
            if (i == decoded.length() || (decoded.charAt(i) == ',' && !quoted)) {
                String address = decoded.substring(start, i).trim();
                if (!address.isEmpty()) result.add(address);
                start = i + 1;
            }
        }
        return result;
    }

    private static String htmlToText(String html) {
        return html.replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", " ")
                .replaceAll("(?i)<br\\s*/?>|</p\\s*>|</div\\s*>|</li\\s*>", "\n")
                .replaceAll("(?s)<[^>]+>", " ")
                .replace("&nbsp;", " ").replace("&amp;", "&")
                .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
                .replace("&#39;", "'").replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .replaceAll(" *\\n+ *", "\n").trim();
    }

    private static String parameter(String header, String name) {
        if (header == null) return "";
        Matcher matcher = PARAMETER.matcher(header);
        while (matcher.find()) {
            if (name.equalsIgnoreCase(matcher.group(1))) {
                return matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
            }
        }
        return "";
    }

    private static String baseType(String header) {
        if (header == null || header.trim().isEmpty()) return "text/plain";
        int semicolon = header.indexOf(';');
        return (semicolon < 0 ? header : header.substring(0, semicolon))
                .trim().toLowerCase(Locale.ROOT);
    }

    private static Charset charset(String name) {
        try { return name == null || name.isEmpty() ? StandardCharsets.UTF_8 : Charset.forName(name); }
        catch (RuntimeException ignored) { return StandardCharsets.UTF_8; }
    }

    private static String trimBrackets(String value) {
        if (value == null) return "";
        String result = value.trim();
        if (result.startsWith("<") && result.endsWith(">") && result.length() > 1)
            return result.substring(1, result.length() - 1);
        return result;
    }

    private static String join(List<String> parts) {
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            String value = part == null ? "" : part.trim();
            if (value.isEmpty()) continue;
            if (result.length() > 0) result.append('\n');
            result.append(value);
        }
        return result.toString();
    }

    private static byte[] readAll(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
        return output.toByteArray();
    }

    private static final class Part {
        final Map<String, String> headers;
        final byte[] body;
        Part(Map<String, String> headers, byte[] body) { this.headers = headers; this.body = body; }
        String header(String name) { return headers.get(name); }
    }
}
