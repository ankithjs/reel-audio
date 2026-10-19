package com.ankith.reelaudio.transcribe

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

sealed interface ModelState {
    data object Missing : ModelState
    data class Downloading(val percent: Int) : ModelState
    data object Ready : ModelState
    data class Failed(val reason: String) : ModelState
}

data class Transcript(val language: String, val vtt: String, val text: String)

/**
 * Whisper "small" speech model, downloaded once (~190 MB) and run fully on the phone.
 * Translate mode turns Hindi and other languages into English text.
 */
class SpeechModel(private val context: Context) {
    private val modelFile get() = File(context.filesDir, "models/$MODEL_NAME")
    private val _state = MutableStateFlow<ModelState>(if (modelFile.exists()) ModelState.Ready else ModelState.Missing)
    val state: StateFlow<ModelState> = _state

    private val lock = Mutex()
    private var handle = 0L

    val isReady get() = modelFile.exists()

    suspend fun download() = withContext(Dispatchers.IO) {
        if (modelFile.exists()) return@withContext
        val tmp = File(modelFile.parentFile, "$MODEL_NAME.part").apply { parentFile?.mkdirs() }
        try {
            _state.value = ModelState.Downloading(0)
            var url = URL(MODEL_URL)
            var conn: HttpURLConnection
            // Follow redirects by hand; HuggingFace hops to a CDN on another host.
            while (true) {
                conn = url.openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = false
                conn.connectTimeout = 20_000
                conn.readTimeout = 30_000
                val code = conn.responseCode
                if (code in 300..399) {
                    url = URL(url, conn.getHeaderField("Location"))
                    conn.disconnect()
                    continue
                }
                if (code != 200) error("Download failed (HTTP $code)")
                break
            }
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                tmp.outputStream().use { out ->
                    val buf = ByteArray(256 * 1024)
                    var done = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        if (total > 0) _state.value = ModelState.Downloading((done * 100 / total).toInt())
                    }
                }
            }
            if (total > 0 && tmp.length() != total) error("Download was cut off. Try again.")
            tmp.renameTo(modelFile)
            _state.value = ModelState.Ready
        } catch (t: Throwable) {
            tmp.delete()
            _state.value = ModelState.Failed(t.message ?: "Download failed")
        }
    }

    suspend fun delete() = lock.withLock {
        if (handle != 0L) WhisperLib.freeContext(handle)
        handle = 0L
        modelFile.delete()
        _state.value = ModelState.Missing
    }

    /** Speech in [audioPath] as English text with timings. Runs one clip at a time. */
    suspend fun transcribe(audioPath: String): Transcript = withContext(Dispatchers.Default) {
        lock.withLock {
            check(modelFile.exists()) { "Download the speech model in Settings first." }
            if (handle == 0L) {
                handle = WhisperLib.initContext(modelFile.absolutePath)
                check(handle != 0L) { "Couldn't load the speech model. Remove it in Settings and download again." }
            }
            val samples = AudioDecoder.decode(audioPath)
            val threads = Runtime.getRuntime().availableProcessors().coerceIn(2, 4)
            val raw = WhisperLib.transcribe(handle, samples, threads, translate = true)
                ?: error("Speech-to-text failed on this clip.")
            parse(raw)
        }
    }

    private fun parse(raw: String): Transcript {
        var lang = ""
        val vtt = StringBuilder("WEBVTT\n\n")
        val text = StringBuilder()
        for (line in raw.lines()) {
            val parts = line.split('\t', limit = 3)
            if (parts.size == 2 && parts[0] == "lang") lang = parts[1]
            if (parts.size != 3) continue
            val t0 = parts[0].toLongOrNull() ?: continue
            val t1 = parts[1].toLongOrNull() ?: continue
            val words = parts[2].trim()
            // Whisper marks music/silence like "[Music]"; keep it out of the lyrics.
            if (words.isEmpty() || (words.startsWith("[") && words.endsWith("]")) ||
                (words.startsWith("(") && words.endsWith(")"))) continue
            vtt.append(stamp(t0)).append(" --> ").append(stamp(t1)).append('\n').append(words).append("\n\n")
            text.append(words).append('\n')
        }
        return Transcript(lang, vtt.toString(), text.toString().trim())
    }

    private fun stamp(ms: Long) = "%02d:%02d:%02d.%03d".format(ms / 3_600_000, ms / 60_000 % 60, ms / 1000 % 60, ms % 1000)

    companion object {
        const val MODEL_NAME = "ggml-small-q5_1.bin"
        const val MODEL_URL = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/$MODEL_NAME"
        const val MODEL_SIZE_MB = 190
    }
}
