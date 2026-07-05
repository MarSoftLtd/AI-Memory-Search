package com.bliss.aimemorysearch.ai;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.Collections;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;

public class ImageEmbeddingEngine {

    private static final String TAG_INIT = "MOBILECLIP_INIT";
    private static final String TAG_IMAGE = "MOBILECLIP_IMAGE";

    private static final String ASSET_MODEL_PATH =
            "models/clip/vision_model_q4f16.onnx";

    private static final String LOCAL_MODEL_DIR =
            "models/clip";

    private static final String LOCAL_MODEL_NAME =
            "vision_model_q4f16.onnx";

    private static ImageEmbeddingEngine instance;

    private OrtEnvironment environment;
    private OrtSession session;

    public static synchronized ImageEmbeddingEngine getInstance() {

        if (instance == null) {
            instance = new ImageEmbeddingEngine();
        }

        return instance;
    }

    public synchronized void initialize(
            Context context
    ) {

        if (session != null) {
            return;
        }

        try {

            environment =
                    OrtEnvironment.getEnvironment();

            File modelFile =
                    copyAssetModelToInternalFile(
                            context
                    );

            OrtSession.SessionOptions options =
                    new OrtSession.SessionOptions();

            session =
                    environment.createSession(
                            modelFile.getAbsolutePath(),
                            options
                    );

        } catch (Throwable e) {
            
            session = null;
        }
    }

    public float[] generateEmbedding(
            Bitmap bitmap
    ) {

        try {

            if (environment == null) {
                return null;
            }

            if (session == null) {

                Log.e(
                        TAG_IMAGE,
                        "SESSION IS NULL"
                );

                return null;
            }

            if (bitmap == null) {
                return null;
            }

            Bitmap resized =
                    Bitmap.createScaledBitmap(
                            bitmap,
                            224,
                            224,
                            true
                    );

            float[] input =
                    preprocessImage(
                            resized
                    );

            OnnxTensor tensor =
                    OnnxTensor.createTensor(
                            environment,
                            FloatBuffer.wrap(input),
                            new long[]{
                                    1,
                                    3,
                                    224,
                                    224
                            }
                    );

            OrtSession.Result result =
                    session.run(
                            Collections.singletonMap(
                                    "pixel_values",
                                    tensor
                            )
                    );

            float[][] output =
                    (float[][])
                            result.get(0)
                                    .getValue();

            android.util.Log.e(
                    "CLIP_IMAGE_VALUES",
                    output[0][0]
                            + " | "
                            + output[0][1]
                            + " | "
                            + output[0][2]
                            + " | "
                            + output[0][3]
                            + " | "
                            + output[0][4]
            );

            float[] embedding =
                    normalize(
                            output[0]
                    );

            result.close();
            tensor.close();

            resized.recycle();

            return embedding;

        } catch (Throwable e) {

            Log.e(
                    TAG_IMAGE,
                    "EMBEDDING FAILED",
                    e
            );
        }

        return null;
    }

    private File copyAssetModelToInternalFile(
            Context context
    ) throws Exception {

        File modelDir =
                new File(
                        context.getFilesDir(),
                        LOCAL_MODEL_DIR
                );

        if (!modelDir.exists()) {
            modelDir.mkdirs();
        }

        File modelFile =
                new File(
                        modelDir,
                        LOCAL_MODEL_NAME
                );
        if (
                modelFile.exists()
                        &&
                        modelFile.length() > 10 * 1024 * 1024
        ) {
            return modelFile;
        }
        InputStream inputStream =
                context.getAssets()
                        .open(
                                ASSET_MODEL_PATH
                        );
        FileOutputStream outputStream =
                new FileOutputStream(
                        modelFile,
                        false
                );

        byte[] buffer =
                new byte[1024 * 1024];
        int read;

        while (
                (read = inputStream.read(buffer)) != -1
        ) {

            outputStream.write(
                    buffer,
                    0,
                    read
            );
        }
        outputStream.flush();
        outputStream.close();
        inputStream.close();
        return modelFile;
    }

    private float[] preprocessImage(
            Bitmap bitmap
    ) {

        int width =
                bitmap.getWidth();

        int height =
                bitmap.getHeight();

        float[] data =
                new float[
                        3 * width * height
                        ];

        int redOffset = 0;
        int greenOffset =
                width * height;
        int blueOffset =
                width * height * 2;

        for (int y = 0; y < height; y++) {

            for (int x = 0; x < width; x++) {

                int pixel =
                        bitmap.getPixel(
                                x,
                                y
                        );

                int index =
                        y * width + x;

                float r =
                        Color.red(pixel)
                                / 255f;

                float g =
                        Color.green(pixel)
                                / 255f;

                float b =
                        Color.blue(pixel)
                                / 255f;

                r =
                        (r - 0.48145466f)
                                / 0.26862954f;

                g =
                        (g - 0.4578275f)
                                / 0.26130258f;

                b =
                        (b - 0.40821073f)
                                / 0.27577711f;

                data[
                        redOffset + index
                        ] = r;

                data[
                        greenOffset + index
                        ] = g;

                data[
                        blueOffset + index
                        ] = b;
            }
        }

        return data;
    }

    private float[] normalize(
            float[] vector
    ) {

        if (vector == null) {
            return null;
        }

        float sum = 0f;

        for (float v : vector) {

            if (
                    Float.isNaN(v)
                            ||
                            Float.isInfinite(v)
            ) {
                return null;
            }

            sum += v * v;
        }

        float norm =
                (float) Math.sqrt(sum);

        if (
                norm == 0f
                        ||
                        Float.isNaN(norm)
                        ||
                        Float.isInfinite(norm)
        ) {
            return null;
        }

        for (int i = 0; i < vector.length; i++) {
            vector[i] /= norm;
        }

        return vector;
    }
}