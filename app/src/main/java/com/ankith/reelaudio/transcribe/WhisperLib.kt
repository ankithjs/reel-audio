package com.ankith.reelaudio.transcribe

/** JNI bridge to whisper.cpp, compiled from app/src/main/cpp. */
object WhisperLib {
    init {
        System.loadLibrary("reelaudio_whisper")
    }

    @JvmStatic external fun initContext(modelPath: String): Long
    @JvmStatic external fun freeContext(context: Long)
    @JvmStatic external fun transcribe(context: Long, samples: FloatArray, threads: Int, translate: Boolean): String?
}
