package com.bliss.aimemorysearch.ai;

public final class E5SentencePieceNative {

    static {
        System.loadLibrary("e5_sentencepiece_jni");
    }

    private E5SentencePieceNative() {
    }

    public static native String version();

    public static native boolean loadModel(
            String path
    );

    public static native int loadModelPieceCount(
            String path
    );

    public static native int[] encode(
            String text
    );
}
