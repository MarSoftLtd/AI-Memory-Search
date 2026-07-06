#include <jni.h>
#include <sentencepiece_processor.h>

#include <vector>

namespace {
sentencepiece::SentencePieceProcessor processor;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_bliss_aimemorysearch_ai_E5SentencePieceNative_version(
        JNIEnv *env,
        jclass clazz
) {
    (void) clazz;
    return env->NewStringUTF("SentencePiece OK");
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_bliss_aimemorysearch_ai_E5SentencePieceNative_loadModel(
        JNIEnv *env,
        jclass clazz,
        jstring path
) {
    (void) clazz;

    if (path == nullptr) {
        return JNI_FALSE;
    }

    const char *model_path =
            env->GetStringUTFChars(
                    path,
                    nullptr
            );

    if (model_path == nullptr) {
        return JNI_FALSE;
    }

    const auto status =
            processor.Load(
                    model_path
            );

    env->ReleaseStringUTFChars(
            path,
            model_path
    );

    return status.ok()
           ? JNI_TRUE
           : JNI_FALSE;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_bliss_aimemorysearch_ai_E5SentencePieceNative_loadModelPieceCount(
        JNIEnv *env,
        jclass clazz,
        jstring path
) {
    (void) clazz;

    if (path == nullptr) {
        return -1;
    }

    const char *model_path =
            env->GetStringUTFChars(
                    path,
                    nullptr
            );

    if (model_path == nullptr) {
        return -1;
    }

    sentencepiece::SentencePieceProcessor localProcessor;

    const auto status =
            localProcessor.Load(
                    model_path
            );

    env->ReleaseStringUTFChars(
            path,
            model_path
    );

    if (!status.ok()) {
        return -1;
    }

    return static_cast<jint>(
            localProcessor.GetPieceSize()
    );
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_bliss_aimemorysearch_ai_E5SentencePieceNative_encode(
        JNIEnv *env,
        jclass clazz,
        jstring text
) {
    (void) clazz;

    if (
            text == nullptr
                    ||
                    !processor.status()
                            .ok()
    ) {
        return env->NewIntArray(0);
    }

    const char *input_text =
            env->GetStringUTFChars(
                    text,
                    nullptr
            );

    if (input_text == nullptr) {
        return env->NewIntArray(0);
    }

    std::vector<int> ids;

    const auto status =
            processor.Encode(
                    input_text,
                    &ids
            );

    env->ReleaseStringUTFChars(
            text,
            input_text
    );

    if (!status.ok()) {
        return env->NewIntArray(0);
    }

    auto result =
            env->NewIntArray(
                    static_cast<jsize>(ids.size())
            );

    if (result == nullptr) {
        return nullptr;
    }

    env->SetIntArrayRegion(
            result,
            0,
            static_cast<jsize>(ids.size()),
            reinterpret_cast<const jint *>(ids.data())
    );

    return result;
}
