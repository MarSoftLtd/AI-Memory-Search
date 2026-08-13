package com.bliss.aimemorysearch;

import com.bliss.aimemorysearch.db.FileEntity;

import java.util.List;
import java.util.Locale;

/** Deterministic dashboard projection over the persistent files_index table. */
public final class IndexCategoryStats {
    public enum Category { IMAGES, EMAIL, PDF, WORD, EXCEL, POWERPOINT, TEXT, OTHER_DOCUMENTS }

    private final int[] counts = new int[Category.values().length];
    public final int total;

    private IndexCategoryStats(int total) { this.total = total; }

    public int count(Category category) { return counts[category.ordinal()]; }

    public static IndexCategoryStats from(List<FileEntity> files) {
        IndexCategoryStats stats = new IndexCategoryStats(countIndexed(files));
        if (files == null) return stats;
        for (FileEntity file : files) {
            if (!indexed(file)) continue;
            stats.counts[classify(file).ordinal()]++;
        }
        return stats;
    }

    static Category classify(FileEntity file) {
        String type = upper(file == null ? null : file.type);
        if ("EMAIL".equals(type)) return Category.EMAIL;
        String extension = extension(file);
        if ("IMAGE".equals(type) || imageExtension(extension)) return Category.IMAGES;
        if ("PDF".equals(type) || "pdf".equals(extension)) return Category.PDF;
        if ("doc".equals(extension) || "docx".equals(extension)) return Category.WORD;
        if ("xls".equals(extension) || "xlsx".equals(extension)) return Category.EXCEL;
        if ("ppt".equals(extension) || "pptx".equals(extension)) return Category.POWERPOINT;
        if ("txt".equals(extension)) return Category.TEXT;
        return Category.OTHER_DOCUMENTS;
    }

    private static int countIndexed(List<FileEntity> files) {
        if (files == null) return 0;
        int total = 0;
        for (FileEntity file : files) if (indexed(file)) total++;
        return total;
    }

    private static boolean indexed(FileEntity file) {
        return file != null && "DONE".equalsIgnoreCase(file.indexStatus);
    }

    private static String extension(FileEntity file) {
        if (file == null) return "";
        String value = file.name == null || file.name.isEmpty() ? file.path : file.name;
        if (value == null) return "";
        int slash = Math.max(value.lastIndexOf('/'), value.lastIndexOf('\\'));
        int dot = value.lastIndexOf('.');
        return dot > slash && dot < value.length() - 1
                ? value.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }

    private static boolean imageExtension(String extension) {
        return "jpg".equals(extension) || "jpeg".equals(extension)
                || "png".equals(extension) || "webp".equals(extension)
                || "gif".equals(extension) || "bmp".equals(extension)
                || "heic".equals(extension) || "heif".equals(extension);
    }

    private static String upper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
