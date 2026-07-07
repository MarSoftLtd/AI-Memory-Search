#include <jni.h>
#include <android/log.h>
#include <exception>
#include <memory>
#include <string>
#include <vector>

#include <sentencepiece_processor.h>

#include "ctranslate2/translator.h"

#define LOG_TAG "CTRANSLATE2"


static std::unique_ptr<ctranslate2::Translator> translator;
static sentencepiece::SentencePieceProcessor sourceProcessor;
static sentencepiece::SentencePieceProcessor targetProcessor;


extern "C"
JNIEXPORT void JNICALL
Java_com_bliss_aimemorysearch_ai_CTranslate2Native_nativeInit(
        JNIEnv *env,
        jobject thiz,
        jstring modelPath
) {

    const char *path = env->GetStringUTFChars(modelPath, nullptr);

    ctranslate2::ComputeType computeType =
            ctranslate2::ComputeType::DEFAULT;
    std::string modelPathString(path);

    try {

        auto sourceStatus =
                sourceProcessor.Load(
                        modelPathString + "/source.spm"
                );

        if (!sourceStatus.ok()) {
            throw std::runtime_error(
                    sourceStatus.ToString()
            );
        }

        auto targetStatus =
                targetProcessor.Load(
                        modelPathString + "/target.spm"
                );

        if (!targetStatus.ok()) {
            throw std::runtime_error(
                    targetStatus.ToString()
            );
        }

        translator = std::make_unique<ctranslate2::Translator>(
                path,
                ctranslate2::Device::CPU,
                computeType
        );

    } catch (const std::exception& e) {

        __android_log_print(
                ANDROID_LOG_ERROR,
                LOG_TAG,
                "Translator initialization failed: %s",
                e.what()
        );

        env->ReleaseStringUTFChars(modelPath, path);

        jclass exceptionClass =
                env->FindClass(
                        "java/lang/RuntimeException"
                );

        env->ThrowNew(
                exceptionClass,
                e.what()
        );

        return;
    }

    env->ReleaseStringUTFChars(modelPath, path);
}



extern "C"
JNIEXPORT jstring JNICALL
Java_com_bliss_aimemorysearch_ai_CTranslate2Native_nativeTranslate(
        JNIEnv *env,
        jobject thiz,
        jstring input
) {

    const char *text = env->GetStringUTFChars(input, nullptr);

    std::string inputText(text);
    env->ReleaseStringUTFChars(input, text);

    std::vector<std::string> tokens;

    try {

        auto encodeStatus =
                sourceProcessor.Encode(
                        inputText,
                        &tokens
                );

        if (!encodeStatus.ok()) {
            throw std::runtime_error(
                    encodeStatus.ToString()
            );
        }

        tokens.push_back("</s>");

        auto results = translator->translate_batch(
                {tokens}
        );

        std::string output;

        auto decodeStatus =
                targetProcessor.Decode(
                        results[0].hypotheses[0],
                        &output
                );

        if (!decodeStatus.ok()) {
            throw std::runtime_error(
                    decodeStatus.ToString()
            );
        }

        return env->NewStringUTF(
                output.c_str()
        );

    } catch (const std::exception& e) {

        __android_log_print(
                ANDROID_LOG_ERROR,
                LOG_TAG,
                "Translator translation failed: %s",
                e.what()
        );

        jclass exceptionClass =
                env->FindClass(
                        "java/lang/RuntimeException"
                );

        env->ThrowNew(
                exceptionClass,
                e.what()
        );

        return nullptr;
    }
}


extern "C"
JNIEXPORT void JNICALL
Java_com_bliss_aimemorysearch_ai_CTranslate2Native_nativeClose(
        JNIEnv *env,
        jobject thiz
) {

    translator.reset();
}
