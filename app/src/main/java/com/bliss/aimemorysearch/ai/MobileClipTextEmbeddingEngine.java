package com.bliss.aimemorysearch.ai;

import android.content.Context;
import android.util.Log;

import java.io.InputStream;
import java.io.File;
import java.nio.LongBuffer;
import java.util.Collections;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;

public class MobileClipTextEmbeddingEngine {

    private static final int MAX_LEN = 77;

    private static MobileClipTextEmbeddingEngine instance;

    private OrtEnvironment environment;
    private OrtSession session;

    public static synchronized MobileClipTextEmbeddingEngine getInstance() {

        if (instance == null) {
            instance =
                    new MobileClipTextEmbeddingEngine();
        }

        return instance;
    }

    public void initialize(
            Context context
    ) {
        initialize(context, ModelPackageRuntime.requireDirectory(
                context, ModelPackageRuntime.CLIP_TEXT));
    }

    public void initialize(Context context, File packageDirectory) {
        android.util.Log.e(
                "CLIP_TEXT_DEBUG",
                "INITIALIZE CALLED"
        );
        android.util.Log.e(
                "CLIP_TEXT_DEBUG",
                "SESSION = " + session
        );
        if (session != null) {
            android.util.Log.e(
                    "CLIP_TEXT_DEBUG",
                    "ALREADY INITIALIZED"
            );
            return;
        }

        try {

            environment =
                    OrtEnvironment.getEnvironment();

            File modelFile = new File(packageDirectory, "text_model_uint8.onnx");

            session =
                    environment.createSession(
                            modelFile.getAbsolutePath(),
                            new OrtSession.SessionOptions()
                    );
            ClipTokenizer
                    .getInstance()
                    .initialize(packageDirectory);
            
            android.util.Log.e(
                    "CLIP_TEXT_META",
                    session.getInputInfo().toString()
            );

            android.util.Log.e(
                    "CLIP_TEXT_META",
                    session.getOutputInfo().toString()
            );
            android.util.Log.e(
                    "CLIP_TEXT_DEBUG",
                    "SESSION CREATED"
            );
            android.util.Log.e(
                    "CLIP_TEXT_INPUTS",
                    session.getInputNames().toString()
            );

            android.util.Log.e(
                    "CLIP_TEXT_OUTPUTS",
                    session.getOutputNames().toString()
            );

            android.util.Log.e(
                    "CLIP_TEXT_META",
                    session.getInputInfo().toString()
            );

            android.util.Log.e(
                    "CLIP_TEXT_META",
                    session.getOutputInfo().toString()
            );
            android.util.Log.e(
                    "CLIP_TEXT_META",
                    session.getInputInfo()
                            .toString()
            );

            android.util.Log.e(
                    "CLIP_TEXT_META",
                    session.getOutputInfo()
                            .toString()
            );
        } catch (Exception e) {

            Log.e(
                    "MOBILECLIP_TEXT",
                    "INITIALIZE FAILED",
                    e
            );
        }
    }

    public synchronized boolean isInitialized() {
        return environment != null
                && session != null
                && ClipTokenizer.getInstance().isInitialized();
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
        ClipTokenizer.getInstance().deactivate();
    }

    public float[] generateEmbedding(
            String text
    ) {

        android.util.Log.d("MULTILINGUAL_PIPELINE", "MobileCLIP received text: " + text
                + " | initialized=" + (environment != null && session != null));

        try {

            long[] inputIds =
                    ClipTokenizer
                            .getInstance()
                            .encode(text);

            StringBuilder sb =
                    new StringBuilder();

            for (
                    int i = 0;
                    i < Math.min(
                            20,
                            inputIds.length
                    );
                    i++
            ) {

                sb.append(
                        inputIds[i]
                ).append(" ");
            }

            android.util.Log.e(
                    "CLIP_TOKEN_DEBUG",
                    text + " -> " + sb
            );
            String decoded = "";

            for (int i = 0; i < Math.min(10, inputIds.length); i++) {

                decoded += inputIds[i] + " ";
            }

            android.util.Log.e(
                    "QUERY_TEST",
                    text
            );
            float[] emb =
                    generateEmbedding(
                            inputIds
                    );

            android.util.Log.e(
                    "CLIP_COMPARE",
                    text
                            + " => "
                            + emb[0]
                            + ", "
                            + emb[1]
                            + ", "
                            + emb[2]
                            + ", "
                            + emb[3]
            );
            android.util.Log.e(
                    "QUERY_VECTOR",
                    text
                            + " => "
                            + emb.length
                            + " dims"
            );

            android.util.Log.e(
                    "QUERY_VECTOR",
                    emb[0]
                            + " "
                            + emb[1]
                            + " "
                            + emb[2]
                            + " "
                            + emb[3]
                            + " "
                            + emb[4]
            );
            return emb;

        } catch (Exception e) {

            Log.e(
                    "MOBILECLIP_TEXT",
                    "TEXT EMBEDDING FAILED",
                    e
            );
        }

        return new float[512];
    }

    public float[] generateEmbedding(
            long[] inputIds
    ) {
        android.util.Log.e(
                "CLIP_TEXT_CALL",
                "TOKEN COUNT = " + inputIds.length
        );
        try {

            if (
                    environment == null
                            ||
                            session == null
            ) {

                return new float[512];
            }

            OnnxTensor tensor =
                    OnnxTensor.createTensor(
                            environment,
                            LongBuffer.wrap(
                                    inputIds
                            ),
                            new long[]{
                                    1,
                                    inputIds.length
                            }
                    );

            OrtSession.Result result =
                    session.run(
                            Collections.singletonMap(
                                    "input_ids",
                                    tensor
                            )
                    );
            android.util.Log.e(
                    "CLIP_TEXT_CLASS",
                    result.get(0)
                            .getValue()
                            .getClass()
                            .getName()
            );
            float[][] output =
                    (float[][])
                            result.get(0)
                                    .getValue();

            android.util.Log.e(
                    "CLIP_TEXT_DIM",
                    output.length
                            + " x "
                            + output[0].length
            );
            android.util.Log.e(
                    "CLIP_TEXT_VALUES",
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

            tensor.close();
            result.close();

            return embedding;

        } catch (Exception e) {

            Log.e(
                    "MOBILECLIP_TEXT",
                    "ONNX TEXT FAILED",
                    e
            );
        }

        return new float[512];
    }

    private long[] fixLength77(
            long[] original
    ) {

        long[] fixed =
                new long[MAX_LEN];

        for (int i = 0; i < MAX_LEN; i++) {

            if (
                    original != null
                            &&
                            i < original.length
            ) {

                fixed[i] =
                        original[i];

            } else {

                fixed[i] = 0;
            }
        }

        fixed[0] = 101;
        fixed[MAX_LEN - 1] = 102;

        return fixed;
    }

    private float[] normalize(
            float[] vector
    ) {

        if (vector == null) {
            return new float[512];
        }

        float sum = 0f;

        for (float v : vector) {
            sum += v * v;
        }

        float norm =
                (float) Math.sqrt(sum);

        if (norm == 0f) {
            return vector;
        }

        for (int i = 0; i < vector.length; i++) {
            vector[i] /= norm;
        }

        return vector;
    }
}
