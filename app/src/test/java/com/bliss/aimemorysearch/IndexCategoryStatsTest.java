package com.bliss.aimemorysearch;

import static org.junit.Assert.assertEquals;

import com.bliss.aimemorysearch.db.FileEntity;
import org.junit.Test;
import java.util.Arrays;

public class IndexCategoryStatsTest {
    @Test public void classifiesPersistentRowsAndIgnoresUnfinishedRows() {
        IndexCategoryStats stats = IndexCategoryStats.from(Arrays.asList(
                file("/DCIM/a.jpg", "IMAGE", "DONE"),
                file("email://gmail/a", "EMAIL", "DONE"),
                file("/mail/attachment.pdf", "DOCUMENT", "DONE"),
                file("/docs/a.docx", "DOCUMENT", "DONE"),
                file("/docs/a.xls", "DOCUMENT", "DONE"),
                file("/docs/a.pptx", "DOCUMENT", "DONE"),
                file("/docs/a.txt", "DOCUMENT", "DONE"),
                file("/docs/a.odt", "DOCUMENT", "DONE"),
                file("/docs/pending.pdf", "PDF", "PENDING")));
        assertEquals(8, stats.total);
        for (IndexCategoryStats.Category category : IndexCategoryStats.Category.values()) {
            assertEquals(1, stats.count(category));
        }
    }

    @Test public void attachmentImageUsesItsRealExtension() {
        assertEquals(IndexCategoryStats.Category.IMAGES,
                IndexCategoryStats.classify(file(
                        "/email_attachment/photo.png", "DOCUMENT", "DONE")));
    }

    private static FileEntity file(String path, String type, String status) {
        String name = path.substring(path.lastIndexOf('/') + 1);
        return new FileEntity(path, name, type, null, null,
                1L, 1L, 1L, status, "", "test", null);
    }
}
