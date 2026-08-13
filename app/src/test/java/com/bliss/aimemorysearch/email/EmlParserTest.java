package com.bliss.aimemorysearch.email;

import com.bliss.aimemorysearch.ai.TextChunker;

import org.junit.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EmlParserTest {
    private final EmlParser parser = new EmlParser();

    @Test public void extractsHeadersAddressesAndPlainBody() throws Exception {
        EmailMessage message = parse("plain.eml");
        assertEquals("plain-1@example.com", message.getSourceMessageId());
        assertEquals("Quarterly planning", message.getSubject());
        assertEquals("Alice Example <alice@example.com>", message.getFrom());
        assertEquals(2, message.getTo().size());
        assertEquals("Dave <dave@example.com>", message.getCc().get(0));
        assertEquals("Meeting notes for the Q3 plan – approved.", message.getBody());
        assertTrue(message.getAttachments().isEmpty());
    }

    @Test public void convertsHtmlToTextWhenPlainBodyIsAbsent() throws Exception {
        EmailMessage message = parse("html-only.eml");
        assertTrue(message.getBody().contains("Project update"));
        assertTrue(message.getBody().contains("The build is ready & verified."));
        assertFalse(message.getBody().contains("<b>"));
    }

    @Test public void prioritizesPlainBodyAndExtractsAttachmentMetadata() throws Exception {
        EmailMessage message = parse("multipart-with-attachments.eml");
        assertEquals("Raport lunar", message.getSubject());
        assertEquals("Preferred plain body.", message.getBody());
        assertEquals(2, message.getCc().size());
        assertEquals(2, message.getAttachments().size());

        EmailAttachment pdf = message.getAttachments().get(0);
        assertEquals("report.pdf", pdf.getFileName());
        assertEquals("application/pdf", pdf.getMimeType());
        assertEquals("attachment", pdf.getDisposition());
        assertEquals(8L, pdf.getSize());

        EmailAttachment image = message.getAttachments().get(1);
        assertEquals("chart.png", image.getFileName());
        assertEquals("image/png", image.getMimeType());
        assertEquals("inline", image.getDisposition());
        assertEquals("chart-1", image.getContentId());
    }

    @Test public void buildsDeterministicIndexTextInRequiredOrder() throws Exception {
        EmailMessage message = parse("plain.eml");
        String expected = "Quarterly planning\n"
                + "Alice Example <alice@example.com>\n"
                + "Bob Example <bob@example.com>\n"
                + "Carol <carol@example.com>\n"
                + "Dave <dave@example.com>\n"
                + "Meeting notes for the Q3 plan – approved.";
        assertEquals(expected, EmailIndexTextBuilder.build(message));
        assertEquals(expected, EmailIndexTextBuilder.build(message));
    }

    @Test public void indexTextCanEnterExistingTextChunker() throws Exception {
        String indexText = EmailIndexTextBuilder.build(parse("plain.eml"));
        List<String> chunks = TextChunker.chunkText(indexText);
        assertFalse(chunks.isEmpty());
        assertTrue(chunks.get(0).contains("Quarterly planning"));
        assertTrue(chunks.get(0).contains("alice@example.com"));
        assertTrue(chunks.get(0).contains("Meeting notes"));
    }

    private EmailMessage parse(String fixture) throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/email/" + fixture)) {
            if (input == null) throw new AssertionError("Missing fixture: " + fixture);
            return parser.parse(input);
        }
    }
}
