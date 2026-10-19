package com.ankith.reelaudio.ui

data class Cue(val startMs: Long, val endMs: Long, val text: String)

/** Minimal WebVTT parser: enough to show the current line while a clip plays. */
fun parseVtt(vtt: String): List<Cue> {
    if (vtt.isBlank()) return emptyList()
    val cues = mutableListOf<Cue>()
    val lines = vtt.replace("\r", "").lines()
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        if ("-->" in line) {
            val (a, b) = line.split("-->").map { it.trim().substringBefore(' ') }
            val text = StringBuilder()
            i++
            while (i < lines.size && lines[i].isNotBlank()) {
                if (text.isNotEmpty()) text.append(' ')
                text.append(lines[i].replace(Regex("<[^>]+>"), "").trim())
                i++
            }
            val start = parseTime(a)
            val end = parseTime(b)
            if (start != null && end != null && text.isNotBlank()) {
                // Auto-generated captions repeat the previous line; keep only the new part.
                val prev = cues.lastOrNull()
                if (prev == null || prev.text != text.toString()) cues += Cue(start, end, text.toString())
            }
        } else i++
    }
    return cues
}

private fun parseTime(t: String): Long? {
    val parts = t.split(':')
    return runCatching {
        val (h, m, s) = when (parts.size) {
            3 -> Triple(parts[0].toLong(), parts[1].toLong(), parts[2])
            2 -> Triple(0L, parts[0].toLong(), parts[1])
            else -> return null
        }
        val sec = s.replace(',', '.').toDouble()
        (h * 3600 + m * 60) * 1000 + (sec * 1000).toLong()
    }.getOrNull()
}

fun formatTime(ms: Long): String {
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}
