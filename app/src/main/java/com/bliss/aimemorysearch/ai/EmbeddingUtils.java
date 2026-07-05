package com.bliss.aimemorysearch.ai;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class EmbeddingUtils {

    public static byte[] floatArrayToBytes(
            float[] values
    ) {

        ByteBuffer buffer =
                ByteBuffer.allocate(
                        values.length * 4
                );

        buffer.order(
                ByteOrder.nativeOrder()
        );

        for (float value : values) {

            buffer.putFloat(value);
        }

        return buffer.array();
    }

    public static float[] bytesToFloatArray(
            byte[] bytes
    ) {

        if (bytes == null) {
            return new float[0];
        }

        ByteBuffer buffer =
                ByteBuffer.wrap(bytes);

        buffer.order(
                ByteOrder.nativeOrder()
        );

        float[] result =
                new float[
                        bytes.length / 4
                        ];

        for (
                int i = 0;
                i < result.length;
                i++
        ) {

            result[i] =
                    buffer.getFloat();
        }

        return result;
    }
}