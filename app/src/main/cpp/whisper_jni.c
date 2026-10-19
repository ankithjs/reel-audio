#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "whisper.h"

#define CLS(name) Java_com_ankith_reelaudio_transcribe_WhisperLib_##name

JNIEXPORT jlong JNICALL CLS(initContext)(JNIEnv *env, jclass clazz, jstring modelPath) {
    const char *path = (*env)->GetStringUTFChars(env, modelPath, NULL);
    struct whisper_context_params cparams = whisper_context_default_params();
    cparams.use_gpu = false;
    struct whisper_context *ctx = whisper_init_from_file_with_params(path, cparams);
    (*env)->ReleaseStringUTFChars(env, modelPath, path);
    return (jlong) ctx;
}

JNIEXPORT void JNICALL CLS(freeContext)(JNIEnv *env, jclass clazz, jlong ctx) {
    if (ctx) whisper_free((struct whisper_context *) ctx);
}

/*
 * Runs speech-to-text on 16 kHz mono samples. Returns one line per segment:
 * "<start ms>\t<end ms>\t<text>", preceded by a first line "lang\t<code>".
 */
JNIEXPORT jstring JNICALL CLS(transcribe)(JNIEnv *env, jclass clazz, jlong handle, jfloatArray samples,
                                          jint threads, jboolean translate) {
    struct whisper_context *ctx = (struct whisper_context *) handle;
    if (!ctx) return NULL;

    jsize n = (*env)->GetArrayLength(env, samples);
    jfloat *data = (*env)->GetFloatArrayElements(env, samples, NULL);

    struct whisper_full_params p = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    p.n_threads = threads;
    p.translate = translate;
    p.language = "auto";
    p.no_context = true;
    p.print_progress = false;
    p.print_realtime = false;
    p.print_special = false;
    p.print_timestamps = false;
    p.suppress_blank = true;

    int rc = whisper_full(ctx, p, data, n);
    (*env)->ReleaseFloatArrayElements(env, samples, data, JNI_ABORT);
    if (rc != 0) return NULL;

    int segments = whisper_full_n_segments(ctx);
    size_t cap = 256, len = 0;
    char *out = malloc(cap);
    len += snprintf(out, cap, "lang\t%s\n", whisper_lang_str(whisper_full_lang_id(ctx)));
    for (int i = 0; i < segments; i++) {
        const char *text = whisper_full_get_segment_text(ctx, i);
        long long t0 = (long long) whisper_full_get_segment_t0(ctx, i) * 10;
        long long t1 = (long long) whisper_full_get_segment_t1(ctx, i) * 10;
        size_t need = len + strlen(text) + 64;
        if (need > cap) {
            while (cap < need) cap *= 2;
            out = realloc(out, cap);
        }
        len += snprintf(out + len, cap - len, "%lld\t%lld\t%s\n", t0, t1, text);
    }
    jstring result = (*env)->NewStringUTF(env, out);
    free(out);
    return result;
}
