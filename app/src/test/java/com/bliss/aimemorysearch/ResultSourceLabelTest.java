package com.bliss.aimemorysearch;

import static org.junit.Assert.assertEquals;

import com.bliss.aimemorysearch.db.FileEntity;

import org.junit.Test;

public class ResultSourceLabelTest {
    @Test public void derivesKnownStorageSourcesFromDirectories() {
        assertEquals("DCIM", label("/storage/emulated/0/DCIM/Camera/dog.jpg"));
        assertEquals("SCREENSHOTS", label(
                "/storage/emulated/0/Pictures/Screenshots/shot.png"));
        assertEquals("WHATSAPP", label(
                "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/x.jpg"));
        assertEquals("DOWNLOADS", label("/storage/emulated/0/Download/a.pdf"));
        assertEquals("DOCUMENTS", label("/storage/emulated/0/Documents/a.docx"));
    }

    @Test public void emailAndRealAttachmentMarkersRemainExplicit() {
        FileEntity email = file("email://gmail/a/b", "EMAIL");
        assertEquals("EMAIL", ResultSourceLabel.from(email));
        assertEquals("EMAIL ATTACHMENT", label(
                "/cache/email_attachment/report.pdf"));
    }

    private static String label(String path) {
        return ResultSourceLabel.from(file(path, "DOCUMENT"));
    }

    private static FileEntity file(String path, String type) {
        return new FileEntity(path, "name", type, null, null,
                0L, 0L, 0L, "DONE", "", "test", null);
    }
}
