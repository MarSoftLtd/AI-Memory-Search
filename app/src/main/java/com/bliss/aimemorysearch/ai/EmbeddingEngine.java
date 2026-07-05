package com.bliss.aimemorysearch.ai;
import android.content.Context;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import java.io.InputStream;
public class EmbeddingEngine implements TextEmbeddingEngine {

    private static EmbeddingEngine instance;
    private final java.util.Map<String, float[]> embeddingCache =
            new java.util.concurrent.ConcurrentHashMap<>();
    private OrtEnvironment ortEnvironment;
    private OrtSession ortSession;

    public static synchronized EmbeddingEngine getInstance() {

        if (instance == null) {
            instance = new EmbeddingEngine();
        }

        return instance;
    }

    public void initialize(Context context) {

        try {
            MiniLMTokenizer
                    .getInstance()
                    .initialize(context);

            ortEnvironment =
                    OrtEnvironment.getEnvironment();

            InputStream inputStream =
                    context.getAssets()
                            .open("models/model.onnx");

            byte[] modelBytes =
                    new byte[inputStream.available()];

            inputStream.read(modelBytes);

            inputStream.close();

            OrtSession.SessionOptions options =
                    new OrtSession.SessionOptions();

            ortSession =
                    ortEnvironment.createSession(
                            modelBytes,
                            options
                    );

        } catch (Exception e) {

            android.util.Log.e(
                    "EMBEDDING_INIT",
                    "FAILED",
                    e
            );
        }
    }
    public float[] generateEmbedding(
            String text
    ) {

        try {
            if (text == null) {
                return new float[384];
            }

            text = text.trim();

            if (text.isEmpty()) {
                return new float[384];
            }

            if (embeddingCache.containsKey(text)) {

                return embeddingCache.get(text);
            }
            MiniLMTokenizer.TokenizedInput encoded =
                    MiniLMTokenizer
                            .getInstance()
                            .encode(text);
            android.util.Log.e(
                    "TOKEN_TEST",
                    text
            );

            android.util.Log.e(
                    "TOKEN_TEST",
                    java.util.Arrays.toString(encoded.inputIds)
            );

            android.util.Log.e(
                    "TOKEN_TEST",
                    java.util.Arrays.toString(encoded.attentionMask)
            );
            long[] inputIds =
                    encoded.inputIds;

            long[] attentionMask =
                    encoded.attentionMask;

            long[] tokenTypeIds =
                    encoded.tokenTypeIds;

            OnnxTensor inputTensor =
                    OnnxTensor.createTensor(
                            ortEnvironment,
                            java.nio.LongBuffer.wrap(inputIds),
                            new long[]{1, 128}
                    );

            OnnxTensor attentionTensor =
                    OnnxTensor.createTensor(
                            ortEnvironment,
                            java.nio.LongBuffer.wrap(attentionMask),
                            new long[]{1, 128}
                    );

            OnnxTensor tokenTypeTensor =
                    OnnxTensor.createTensor(
                            ortEnvironment,
                            java.nio.LongBuffer.wrap(tokenTypeIds),
                            new long[]{1, 128}
                    );

            java.util.Map<String, OnnxTensor> inputs =
                    new java.util.HashMap<>();

            inputs.put(
                    "input_ids",
                    inputTensor
            );

            inputs.put(
                    "attention_mask",
                    attentionTensor
            );

            inputs.put(
                    "token_type_ids",
                    tokenTypeTensor
            );

            OrtSession.Result result =
                    ortSession.run(inputs);

            float[][][] output =
                    (float[][][]) result
                            .get(0)
                            .getValue();

            float[] embedding =
                    meanPooling(
                            output,
                            attentionMask
                    );

            embedding =
                    normalizeVector(
                            embedding
                    );

            embeddingCache.put(
                    text,
                    embedding
            );

            return embedding;

        } catch (Exception e) {

            android.util.Log.e(
                    "EMBEDDING",
                    "FAILED",
                    e
            );
        }

        return new float[384];
    }
    private float[] meanPooling(
            float[][][] tokenEmbeddings,
            long[] attentionMask
    ) {

        int tokens =
                tokenEmbeddings[0].length;

        int dimensions =
                tokenEmbeddings[0][0].length;

        float[] pooled =
                new float[dimensions];

        float validTokens = 0f;

        for (
                int token = 0;
                token < tokens;
                token++
        ) {

            if (
                    attentionMask[token] == 0
            ) {
                continue;
            }

            validTokens++;

            for (
                    int dim = 0;
                    dim < dimensions;
                    dim++
            ) {

                pooled[dim] +=
                        tokenEmbeddings[0][token][dim];
            }
        }

        if (validTokens == 0f) {
            return pooled;
        }

        for (
                int dim = 0;
                dim < dimensions;
                dim++
        ) {

            pooled[dim] /=
                    validTokens;
        }

        return pooled;
    }
    private float[] normalizeVector(
            float[] vector
    ) {

        float sum =
                0f;

        for (float value : vector) {

            sum +=
                    value * value;
        }

        float norm =
                (float) Math.sqrt(sum);

        if (norm == 0f) {
            return vector;
        }

        for (
                int i = 0;
                i < vector.length;
                i++
        ) {

            vector[i] =
                    vector[i] / norm;
        }

        return vector;
    }
}