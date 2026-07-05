package com.bliss.aimemorysearch;

public class FileItem {

    private final String name;
    private final String path;
    private final String type;
    private final String imagePath;

    private String ocrText = "";

    public FileItem(
            String name,
            String path,
            String type,
            String imagePath
    ) {

        this.name = name;
        this.path = path;
        this.type = type;
        this.imagePath = imagePath;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getType() {
        return type;
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getOcrText() {
        return ocrText;
    }

    public void setOcrText(String ocrText) {
        this.ocrText = ocrText;
    }
}