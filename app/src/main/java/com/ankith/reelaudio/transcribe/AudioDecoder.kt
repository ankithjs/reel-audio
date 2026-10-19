package com.ankith.reelaudio.transcribe

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import java.nio.ByteOrder

/** Decodes any audio/video file Android can play into 16 kHz mono floats, the input Whisper expects. */
object AudioDecoder {
    private const val TARGET_RATE = 16_000

    fun decode(path: String, maxSeconds: Int = 600): FloatArray {
        val extractor = MediaExtractor()
        extractor.setDataSource(path)
        val track = (0 until extractor.trackCount).firstOrNull {
            extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        } ?: error("No audio track in this clip.")
        extractor.selectTrack(track)
        val format = extractor.getTrackFormat(track)
        val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        codec.configure(format, null, null, 0)
        codec.start()

        var rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        var floatPcm = false
        var mono = FloatArray(rate * 60)
        var count = 0
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        try {
            while (true) {
                if (!inputDone) {
                    val i = codec.dequeueInputBuffer(10_000)
                    if (i >= 0) {
                        val n = extractor.readSampleData(codec.getInputBuffer(i)!!, 0)
                        if (n < 0) {
                            codec.queueInputBuffer(i, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(i, 0, n, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val o = codec.dequeueOutputBuffer(info, 10_000)
                if (o == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val f = codec.outputFormat
                    rate = f.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    channels = f.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    floatPcm = Build.VERSION.SDK_INT >= 24 && f.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
                        f.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT
                } else if (o >= 0) {
                    val buf = codec.getOutputBuffer(o)!!.order(ByteOrder.nativeOrder())
                    buf.position(info.offset)
                    buf.limit(info.offset + info.size)
                    val frames = if (floatPcm) info.size / 4 / channels else info.size / 2 / channels
                    if (count + frames > mono.size) mono = mono.copyOf(maxOf(mono.size * 2, count + frames))
                    if (floatPcm) {
                        val fb = buf.asFloatBuffer()
                        for (f in 0 until frames) {
                            var sum = 0f
                            for (c in 0 until channels) sum += fb.get()
                            mono[count++] = sum / channels
                        }
                    } else {
                        val sb = buf.asShortBuffer()
                        for (f in 0 until frames) {
                            var sum = 0f
                            for (c in 0 until channels) sum += sb.get() / 32768f
                            mono[count++] = sum / channels
                        }
                    }
                    codec.releaseOutputBuffer(o, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                    if (count >= maxSeconds * rate) break
                }
            }
        } finally {
            codec.stop()
            codec.release()
            extractor.release()
        }
        return resample(mono, count, rate)
    }

    private fun resample(input: FloatArray, length: Int, fromRate: Int): FloatArray {
        if (fromRate == TARGET_RATE) return input.copyOf(length)
        val outLength = (length.toLong() * TARGET_RATE / fromRate).toInt()
        val ratio = fromRate.toDouble() / TARGET_RATE
        return FloatArray(outLength) { i ->
            val pos = i * ratio
            val j = pos.toInt()
            val frac = (pos - j).toFloat()
            val a = input[j.coerceAtMost(length - 1)]
            val b = input[(j + 1).coerceAtMost(length - 1)]
            a + (b - a) * frac
        }
    }
}
