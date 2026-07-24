package com.bliss.aimemorysearch.ai;

import android.content.Context;

import org.json.JSONObject;
import org.json.JSONArray;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public final class ModelPackageRuntime {
    public static final String EMBEDDING_CORE = "core-model-embedding";
    public static final String E5 = "core-model-e5";
    public static final String CLIP_TEXT = "core-model-clip-text";
    public static final String CLIP_VISION = "core-model-clip-vision";

    private ModelPackageRuntime() {}

    public static File requireDirectory(Context context, String packageId) {
        File directory = new File(
                new File(context.getApplicationContext().getFilesDir(), "ai_packages"),
                packageId
        );
        if (!directory.isDirectory()) {
            throw new IllegalStateException("AI model package is not installed: " + packageId);
        }
        try {
            JSONObject config = readConfig(directory);
            JSONArray requiredFiles = config.getJSONArray("requiredFiles");
            if (requiredFiles.length() == 0) {
                throw new IllegalStateException("AI model package has no runtime files: " + packageId);
            }
            for (int index = 0; index < requiredFiles.length(); index++) {
                String fileName = requiredFiles.getString(index);
                if (fileName.isEmpty() || fileName.contains("/") || fileName.contains("\\")
                        || !new File(directory, fileName).isFile()) {
                    throw new IllegalStateException(
                            "AI model package runtime file is missing: " + packageId + "/" + fileName);
                }
            }
        } catch (IllegalStateException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new IllegalStateException("AI model package config is invalid: " + packageId, failure);
        }
        return directory;
    }

    public static boolean isInstalled(Context context, String packageId) {
        try {
            requireDirectory(context, packageId);
            return true;
        } catch (IllegalStateException notInstalled) {
            return false;
        }
    }

    public static String runtimeRole(File directory) throws Exception {
        return readConfig(directory).getString("runtimeRole");
    }

    private static JSONObject readConfig(File directory) throws Exception {
        File config = new File(directory, ManifestFileNames.CONFIG);
        try (InputStream input = new FileInputStream(config)) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[16 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return new JSONObject(output.toString(StandardCharsets.UTF_8.name()));
        }
    }
}
