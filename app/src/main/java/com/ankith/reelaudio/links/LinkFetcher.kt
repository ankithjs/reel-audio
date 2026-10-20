package com.ankith.reelaudio.links

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Reads a web page and keeps what matters: name, description, preview image and readable text. */
object LinkFetcher {
    data class Page(
        val url: String,
        val title: String,
        val siteName: String,
        val description: String,
        val content: String,
        val imageUrl: String,
    )

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"
    private const val MAX_CONTENT = 20_000

    fun fetch(url: String): Page {
        val doc = Jsoup.connect(url)
            .userAgent(USER_AGENT)
            .timeout(20_000)
            .followRedirects(true)
            .maxBodySize(4_000_000)
            .get()
        return parse(doc)
    }

    fun parse(doc: Document): Page {
        fun meta(vararg keys: String): String = keys.firstNotNullOfOrNull { k ->
            doc.selectFirst("meta[property=$k], meta[name=$k]")?.attr("content")?.trim()?.takeIf { it.isNotEmpty() }
        }.orEmpty()
        fun metaUrl(vararg keys: String): String = keys.firstNotNullOfOrNull { k ->
            doc.selectFirst("meta[property=$k], meta[name=$k]")?.absUrl("content")?.takeIf { it.isNotEmpty() }
        }.orEmpty()

        val pageUrl = doc.location()
        val host = runCatching { URL(pageUrl).host.removePrefix("www.") }.getOrDefault("")
        val title = meta("og:title", "twitter:title").ifBlank { doc.title().trim() }.ifBlank { host }
        val description = meta("og:description", "description", "twitter:description")
        val siteName = meta("og:site_name", "application-name").ifBlank { host }
        val image = metaUrl("og:image", "og:image:url", "twitter:image")

        doc.select("script, style, noscript, nav, footer, header, aside, form, svg, iframe, button").remove()
        if (host.endsWith("wikipedia.org")) {
            // Keep the article text, not citations, edit links and info tables.
            doc.select("sup, table, .mw-editsection, .hatnote, .reflist, .references, .navbox, #toc, .toc, .mw-empty-elt").remove()
        }
        val root = doc.selectFirst("article") ?: doc.selectFirst("main") ?: doc.body()
        val lines = root?.select("h1, h2, h3, h4, p, li, pre, blockquote")
            ?.map { el ->
                val text = el.text().trim()
                if (el.tagName().matches(Regex("h[1-4]")) && text.isNotEmpty()) "\n$text" else text
            }
            ?.filter { it.isNotBlank() && it.trim().length > 2 }
            ?.distinct()
            .orEmpty()
        val content = lines.joinToString("\n").trim().take(MAX_CONTENT)
        // Pages without a summary (like Wikipedia) get their first real paragraph instead.
        val summary = description.ifBlank {
            lines.firstOrNull { it.length >= 80 && !it.startsWith("\n") }?.let {
                if (it.length > 400) it.take(400).substringBeforeLast(' ') + "…" else it
            }.orEmpty()
        }
        val cleanTitle = if (host.endsWith("wikipedia.org")) title.removeSuffix(" - Wikipedia") else title

        return Page(pageUrl, cleanTitle, siteName, summary, content, image)
    }

    /** Saves the preview image; skips anything that isn't a reasonably sized image. */
    fun downloadImage(url: String, target: File): Boolean = runCatching {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", USER_AGENT)
        conn.connectTimeout = 15_000
        conn.readTimeout = 20_000
        conn.instanceFollowRedirects = true
        if (conn.responseCode != 200 || conn.contentType?.startsWith("image/") != true) return@runCatching false
        conn.inputStream.use { input ->
            target.outputStream().use { out ->
                val buf = ByteArray(64 * 1024)
                var total = 0
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    total += n
                    if (total > 5_000_000) error("too big")
                    out.write(buf, 0, n)
                }
            }
        }
        true
    }.getOrElse { target.delete(); false }
}
