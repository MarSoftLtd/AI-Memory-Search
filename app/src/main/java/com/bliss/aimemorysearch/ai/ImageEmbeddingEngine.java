package com.bliss.aimemorysearch.ai;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import java.io.InputStream;
import java.io.File;
import java.nio.FloatBuffer;
import java.util.Collections;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;

public class ImageEmbeddingEngine {

    private static final String TAG_INIT = "MOBILECLIP_INIT";
    private static final String TAG_IMAGE = "MOBILECLIP_IMAGE";

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
        initialize(ModelPackageRuntime.requireDirectory(
                context, ModelPackageRuntime.CLIP_VISION));
    }

    public synchronized void initialize(File packageDirectory) {

        if (session != null) {
            return;
        }

        try {

            environment =
                    OrtEnvironment.getEnvironment();

            File modelFile = new File(packageDirectory, "vision_model_q4f16.onnx");

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

    public synchronized boolean isInitialized() {
        return environment != null && session != null;
    }

    public synchronized void deactivate() {
        if (session != null) {
            try {
                session.close();
            } catch (Exception ignored) {
            }
        }
        session = null;
        environment = null;
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

            Bitmap resized = resizeShortEdge(bitmap);
            int left = (resized.getWidth() - 224) / 2;
            int top = (resized.getHeight() - 224) / 2;
            Bitmap cropped = Bitmap.createBitmap(
                    resized,
                    left,
                    top,
                    224,
                    224
            );

            float[] input =
                    preprocessImage(
                            cropped
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

            if (cropped != resized) {
                cropped.recycle();
            }
            if (resized != bitmap) {
                resized.recycle();
            }

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

    private Bitmap resizeShortEdge(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int resizedWidth;
        int resizedHeight;

        if (width <= height) {
            resizedWidth = 224;
            resizedHeight = Math.max(224, 224 * height / width);
        } else {
            resizedHeight = 224;
            resizedWidth = Math.max(224, 224 * width / height);
        }

        return Bitmap.createScaledBitmap(
                bitmap,
                resizedWidth,
                resizedHeight,
                true
        );
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
