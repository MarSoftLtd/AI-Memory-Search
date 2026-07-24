package com.bliss.aimemorysearch.ai;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DocumentTextExtractor {

    public static String extractText(File file) {

        if (file == null || !file.exists()) {
            return "";
        }

        String name =
                file.getName().toLowerCase();

        try {

            if (name.endsWith(".txt") || name.endsWith(".csv")
                    || name.endsWith(".json") || name.endsWith(".xml")
                    || name.endsWith(".md") || name.endsWith(".html")
                    || name.endsWith(".htm")) {
                return readPlainText(file);
            }

            if (name.endsWith(".docx")) {
                return readDocx(file);
            }

            if (name.endsWith(".xlsx")) {
                return readXlsx(file);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    private static String readPlainText(File file) throws Exception {

        StringBuilder builder =
                new StringBuilder();

        BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                new FileInputStream(file)
                        )
                );

        String line;

        while ((line = reader.readLine()) != null) {
            builder.append(line).append(" ");
        }

        reader.close();

        return builder.toString().trim();
    }

    private static String readDocx(File file) throws Exception {

        ZipInputStream zip =
                new ZipInputStream(
                        new FileInputStream(file)
                );

        ZipEntry entry;

        StringBuilder builder =
                new StringBuilder();

        while ((entry = zip.getNextEntry()) != null) {

            if (entry.getName().equals("word/document.xml")) {

                String xml =
                        readZipEntry(zip);

                builder.append(
                        xmlToText(xml)
                );
            }
        }

        zip.close();

        return builder.toString().trim();
    }

    private static String readXlsx(File file) throws Exception {

        ZipInputStream zip =
                new ZipInputStream(
                        new FileInputStream(file)
                );

        ZipEntry entry;

        Map<Integer, String> sharedStrings =
                new HashMap<>();

        StringBuilder sheetsText =
                new StringBuilder();

        while ((entry = zip.getNextEntry()) != null) {

            String entryName =
                    entry.getName();

            String xml =
                    readZipEntry(zip);

            if (entryName.equals("xl/sharedStrings.xml")) {

                String[] parts =
                        xml.split("<si>");

                int index = 0;

                for (String part : parts) {

                    if (part.contains("</si>")) {

                        sharedStrings.put(
                                index,
                                xmlToText(part)
                        );

                        index++;
                    }
                }
            }

            if (
                    entryName.startsWith("xl/worksheets/")
                            &&
                            entryName.endsWith(".xml")
            ) {

                sheetsText.append(" ")
                        .append(xmlToText(xml));
            }
        }

        zip.close();

        String result =
                sheetsText.toString();

        for (Map.Entry<Integer, String> entrySet : sharedStrings.entrySet()) {

            result =
                    result.replace(
                            " " + entrySet.getKey() + " ",
                            " " + entrySet.getValue() + " "
                    );
        }

        return result.trim();
    }

    private static String readZipEntry(
            ZipInputStream zip
    ) throws Exception {

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        byte[] buffer =
                new byte[4096];

        int read;

        while ((read = zip.read(buffer)) != -1) {

            output.write(
                    buffer,
                    0,
                    read
            );
        }

        return output.toString("UTF-8");
    }

    private static String xmlToText(String xml) {

        if (xml == null) {
            return "";
        }

        return xml
                .replaceAll("<[^>]+>", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
