package com.bliss.aimemorysearch.ai;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;

public class E5EmbeddingEngine {

    private static final String TAG = "E5_EMBEDDING";
    private static final String MODEL_ASSET_PATH =
            "models/e5/model.onnx";
    private static final String LOCAL_MODEL_DIR =
            "models/e5";
    private static final String LOCAL_MODEL_NAME =
            "model.onnx";
    private static final String LOCAL_SENTENCEPIECE_NAME =
            "sentencepiece.bpe.model";

    private static final Object INIT_LOCK =
            new Object();

    private static boolean sentencePieceLoadAttempted = false;
    private static boolean sentencePieceLoaded = false;
    private static OrtEnvironment environment;
    private static OrtSession session;

    private final Context appContext;

    public E5EmbeddingEngine(
            Context context
    ) {

        appContext =
                context.getApplicationContext();
    }

    public void initialize() {

        synchronized (INIT_LOCK) {

            if (
                    sentencePieceLoaded
                            &&
                            environment != null
                            &&
                            session != null
            ) {
                return;
            }

            try {

                if (!sentencePieceLoadAttempted) {
                    sentencePieceLoadAttempted =
                            true;

                    File sentencePieceFile =
                            new File(
                                    new File(
                                            appContext.getFilesDir(),
                                            LOCAL_MODEL_DIR
                                    ),
                                    LOCAL_SENTENCEPIECE_NAME
                            );

                    sentencePieceLoaded =
                            E5SentencePieceNative.loadModel(
                                    sentencePieceFile.getAbsolutePath()
                            );

                    if (sentencePieceLoaded) {
                        Log.i(
                                TAG,
                                "sentencepiece loaded: "
                                        + sentencePieceFile.getAbsolutePath()
                        );

                    } else {
                        Log.e(
                                TAG,
                                "sentencepiece load failed: "
                                        + sentencePieceFile.getAbsolutePath()
                        );
                    }
                }

                if (environment == null) {
                    environment =
                            OrtEnvironment.getEnvironment();
                }

                if (session == null) {
                    File modelFile =
                            copyAssetModelToInternalFile(
                                    appContext
                            );

                    OrtSession.SessionOptions options =
                            new OrtSession.SessionOptions();

                    session =
                            environment.createSession(
                                    modelFile.getAbsolutePath(),
                                    options
                            );

                    validateRequiredInputs();

                    Log.i(
                            TAG,
                            "model loaded: "
                                    + modelFile.getAbsolutePath()
                    );
                }

            } catch (Exception e) {

                if (e instanceof IllegalStateException) {
                    throw (IllegalStateException) e;
                }

                Log.e(
                        TAG,
                        "initialize failed",
                        e
                );
            }
        }
    }

    public float[] generateEmbedding(
            String text
    ) {

        initialize();

        if (
                !sentencePieceLoaded
                        ||
                        environment == null
                        ||
                        session == null
        ) {
            return new float[0];
        }

        if (text == null) {
            return new float[0];
        }

        String normalizedText =
                text.trim();

        if (normalizedText.isEmpty()) {
            return new float[0];
        }

        OrtSession.Result result = null;
        List<OnnxTensor> tensors =
                new ArrayList<>();

        try {

            if (!sentencePieceLoaded) {
                Log.e(TAG, "SentencePiece not loaded");
                return new float[0];
            }

            int[] rawIds =
                    E5SentencePieceNative.encode(normalizedText);

            if (rawIds == null || rawIds.length == 0) {
                Log.e(TAG, "SentencePiece returned empty tokens");
                return new float[0];
            }

            int[] withSpecialTokens =
                    wrapE5(rawIds);

            long[] inputIds =
                    toLongArray(withSpecialTokens);

            if (inputIds.length == 0) {
                return new float[0];
            }

            long[] attentionMask =
                    createFullAttentionMask(
                            inputIds.length
                    );

            Log.i(
                    TAG,
                    "token count: "
                            + inputIds.length
                            + ", attention tokens: "
                            + sumAttentionMask(attentionMask)
            );

            Map<String, OnnxTensor> inputs =
                    new HashMap<>();

            OnnxTensor inputIdsTensor =
                    OnnxTensor.createTensor(
                            environment,
                            LongBuffer.wrap(inputIds),
                            new long[]{
                                    1,
                                    inputIds.length
                            }
                    );
            tensors.add(inputIdsTensor);

            OnnxTensor attentionMaskTensor =
                    OnnxTensor.createTensor(
                            environment,
                            LongBuffer.wrap(attentionMask),
                            new long[]{
                                    1,
                                    attentionMask.length
                            }
                    );
            tensors.add(attentionMaskTensor);

            if (
                    session.getInputNames()
                            .contains("input_ids")
            ) {
                inputs.put(
                        "input_ids",
                        inputIdsTensor
                );
            }

            if (
                    session.getInputNames()
                            .contains("attention_mask")
            ) {
                inputs.put(
                        "attention_mask",
                        attentionMaskTensor
                );
            }

            if (
                    session.getInputNames()
                            .contains("token_type_ids")
            ) {
                long[] tokenTypeIds =
                        new long[inputIds.length];

                OnnxTensor tokenTypeIdsTensor =
                        OnnxTensor.createTensor(
                                environment,
                                LongBuffer.wrap(tokenTypeIds),
                                new long[]{
                                        1,
                                        tokenTypeIds.length
                                }
                        );
                tensors.add(tokenTypeIdsTensor);

                inputs.put(
                        "token_type_ids",
                        tokenTypeIdsTensor
                );
            }

            Log.i(
                    TAG,
                    "onnx inputs: "
                            + session.getInputInfo()
            );
            Log.i(
                    TAG,
                    "onnx outputs: "
                            + session.getOutputInfo()
            );

            long startTime =
                    System.nanoTime();

            result =
                    session.run(inputs);

            long inferenceMs =
                    (System.nanoTime() - startTime)
                            / 1_000_000L;

            Log.i(
                    TAG,
                    "inference time: " + inferenceMs + " ms"
            );

            Object outputValue =
                    result.get(0)
                            .getValue();

            float[] embedding =
                    poolOutput(
                            outputValue,
                            attentionMask
                    );

            normalizeL2(
                    embedding
            );

            Log.i(
                    TAG,
                    "embedding dimension: " + embedding.length
            );

            return embedding;

        } catch (Exception e) {

            if (e instanceof IllegalStateException) {
                throw (IllegalStateException) e;
            }

            Log.e(
                    TAG,
                    "generate embedding failed",
                    e
            );

            return new float[0];

        } finally {

            if (result != null) {
                result.close();
            }

            for (OnnxTensor tensor : tensors) {
                tensor.close();
            }
        }
    }

    public float cosineSimilarity(
            float[] a,
            float[] b
    ) {

        if (
                a == null
                        ||
                        b == null
                        ||
                        a.length == 0
                        ||
                        b.length == 0
                        ||
                        a.length != b.length
        ) {
            throw new IllegalArgumentException(
                    "Embedding vectors must be non-empty and have the same length"
            );
        }

        float dot =
                0f;
        float normA =
                0f;
        float normB =
                0f;

        for (
                int i = 0;
                i < a.length;
                i++
        ) {
            dot +=
                    a[i] * b[i];
            normA +=
                    a[i] * a[i];
            normB +=
                    b[i] * b[i];
        }

        if (
                normA == 0f
                        ||
                        normB == 0f
        ) {
            return 0f;
        }

        return (float) (
                dot
                        /
                        (
                                Math.sqrt(normA)
                                        *
                                        Math.sqrt(normB)
                        )
        );
    }

    public void selfTest() {

        float[] dog = generateEmbedding("dog");
        float[] dogAgain = generateEmbedding("dog");
        float[] caine = generateEmbedding("caine");
        float[] hund = generateEmbedding("hund");
        float[] perro = generateEmbedding("perro");

        if (dog.length == 0
                || caine.length == 0
                || hund.length == 0
                || perro.length == 0) {

            Log.e(TAG, "selfTest skipped - embeddings not ready");
            return;
        }

        Log.i(TAG, "selfTest cosine dog vs dog: "
                + cosineSimilarity(dog, dogAgain));

        Log.i(TAG, "selfTest cosine dog vs caine: "
                + cosineSimilarity(dog, caine));

        Log.i(TAG, "selfTest cosine dog vs hund: "
                + cosineSimilarity(dog, hund));

        Log.i(TAG, "selfTest cosine dog vs perro: "
                + cosineSimilarity(dog, perro));
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

        try (
                InputStream inputStream =
                        context.getAssets()
                                .open(MODEL_ASSET_PATH);
                FileOutputStream outputStream =
                        new FileOutputStream(
                                modelFile
                        )
        ) {

            byte[] buffer =
                    new byte[16 * 1024];

            int read;

            while (
                    (read = inputStream.read(buffer))
                            != -1
            ) {
                outputStream.write(
                        buffer,
                        0,
                        read
                );
            }
        }

        return modelFile;
    }

    private void validateRequiredInputs() {

        Set<String> inputNames =
                session.getInputNames();

        if (!inputNames.contains("input_ids")) {
            throw new IllegalStateException(
                    "E5 model missing required ONNX input: input_ids"
            );
        }

        if (!inputNames.contains("attention_mask")) {
            throw new IllegalStateException(
                    "E5 model missing required ONNX input: attention_mask"
            );
        }
    }

    private float[] poolOutput(
            Object outputValue,
            long[] attentionMask
    ) {

        if (outputValue == null) {
            throw new IllegalStateException(
                    "Expected E5 hidden-state output tensor float[][][], got null"
            );
        }

        if (outputValue instanceof float[][][]) {
            return meanPool(
                    (float[][][]) outputValue,
                    attentionMask
            );
        }

        throw new IllegalStateException(
                "Expected E5 hidden-state output tensor float[][][], got "
                        + outputValue.getClass()
                        .getName()
        );
    }

    private float[] meanPool(
            float[][][] hiddenState,
            long[] attentionMask
    ) {

        if (
                hiddenState.length == 0
                        ||
                        hiddenState[0].length == 0
        ) {
            throw new IllegalStateException(
                    "Expected non-empty E5 hidden-state output tensor"
            );
        }

        int tokenCount =
                hiddenState[0].length;

        int dimension =
                hiddenState[0][0].length;

        float[] pooled =
                new float[dimension];

        float attentionSum =
                0f;

        int usableTokens =
                Math.min(
                        tokenCount,
                        attentionMask.length
                );

        for (
                int token = 0;
                token < usableTokens;
                token++
        ) {

            float mask =
                    attentionMask[token];

            if (mask == 0f) {
                continue;
            }

            attentionSum +=
                    mask;

            for (
                    int dim = 0;
                    dim < dimension;
                    dim++
            ) {
                pooled[dim] +=
                        hiddenState[0][token][dim]
                                * mask;
            }
        }

        if (attentionSum == 0f) {
            return pooled;
        }

        for (
                int dim = 0;
                dim < dimension;
                dim++
        ) {
            pooled[dim] /=
                    attentionSum;
        }

        return pooled;
    }

    private long[] createFullAttentionMask(
            int length
    ) {

        long[] attentionMask =
                new long[length];

        for (
                int i = 0;
                i < attentionMask.length;
                i++
        ) {
            attentionMask[i] =
                    1L;
        }

        return attentionMask;
    }

    private long[] toLongArray(
            int[] values
    ) {

        if (values == null) {
            return new long[0];
        }

        long[] output =
                new long[values.length];

        for (
                int i = 0;
                i < values.length;
                i++
        ) {
            output[i] =
                    values[i];
        }

        return output;
    }
    private int[] wrapE5(int[] tokens) {

        if (tokens == null || tokens.length == 0) {
            return new int[0];
        }

        int CLS = 101;
        int SEP = 102;

        int[] out = new int[tokens.length + 2];

        out[0] = CLS;
        System.arraycopy(tokens, 0, out, 1, tokens.length);
        out[out.length - 1] = SEP;

        return out;
    }
    private long sumAttentionMask(
            long[] attentionMask
    ) {

        long sum =
                0L;

        for (long value : attentionMask) {
            sum +=
                    value;
        }

        return sum;
    }

    private void normalizeL2(
            float[] embedding
    ) {

        float sum =
                0f;

        for (float value : embedding) {
            sum +=
                    value * value;
        }

        float norm =
                (float) Math.sqrt(sum);

        if (norm == 0f) {
            return;
        }

        for (
                int i = 0;
                i < embedding.length;
                i++
        ) {
            embedding[i] /=
                    norm;
        }
    }
}
