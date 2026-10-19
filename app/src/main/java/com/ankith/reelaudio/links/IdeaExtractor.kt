package com.ankith.reelaudio.links

/**
 * Finds websites mentioned in a reel's caption or spoken words, including spoken forms
 * like "notion dot so" and well-known tools named without a website (see [KnownTools]).
 */
object IdeaExtractor {
    private const val TLDS = "com|ai|io|app|dev|org|net|co|in|so|xyz|me|tools|tool|gg|tv|ly|to|site|tech|design|studio|space|page|club|pro|info|cc|us|uk"
    private val FULL_URL = Regex("""https?://[^\s"'<>()\[\]]+""", RegexOption.IGNORE_CASE)
    private val BARE_DOMAIN = Regex(
        """(?<![@\w.-])((?:[a-z0-9][a-z0-9-]{0,40}\.)+(?:$TLDS))(/[^\s"'<>()\[\]]*)?(?![\w-])""",
        RegexOption.IGNORE_CASE,
    )
    private val SPOKEN_DOMAIN = Regex("""\b([a-z0-9][a-z0-9-]{1,30})\s+dot\s+($TLDS)\b""", RegexOption.IGNORE_CASE)

    /** Hosts that are never the "idea" itself: social apps, mail and link shorteners for them. */
    private val IGNORED = setOf(
        "instagram.com", "facebook.com", "fb.com", "fb.watch", "threads.net", "gmail.com", "yahoo.com",
        "hotmail.com", "outlook.com", "icloud.com", "tiktok.com", "whatsapp.com", "wa.me",
    )

    fun findLinks(text: String, exclude: String? = null): List<String> {
        if (text.isBlank()) return emptyList()
        val found = linkedSetOf<String>()
        FULL_URL.findAll(text).forEach { found += clean(it.value) }
        // Bare domains, skipping ones that are part of a full URL already found.
        val withoutUrls = FULL_URL.replace(text, " ")
        BARE_DOMAIN.findAll(withoutUrls).forEach { m -> found += clean("https://" + m.value) }
        SPOKEN_DOMAIN.findAll(text).forEach { m ->
            found += "https://${m.groupValues[1].lowercase()}.${m.groupValues[2].lowercase()}/"
        }
        // Tools named without a website ("make slides with Gamma AI"), unless their site was found above.
        val hosts = found.mapNotNull { host(it) }.toSet()
        KnownTools.find(text).filter { host(it) !in hosts }.forEach { found += it }
        val excluded = exclude?.trimEnd('/')?.lowercase()
        return found.filter { url ->
            val host = host(url) ?: return@filter false
            host.contains('.') && IGNORED.none { host == it || host.endsWith(".$it") } &&
                url.trimEnd('/').lowercase() != excluded
        }.distinctBy { it.trimEnd('/').lowercase() }
    }

    private fun clean(url: String): String {
        val trimmed = url.trimEnd('.', ',', ';', ':', '!', '?', ')', '"', '\'')
        val scheme = trimmed.substringBefore("://")
        val rest = trimmed.substringAfter("://")
        val host = rest.substringBefore('/').lowercase()
        val path = rest.drop(host.length)
        return "$scheme://$host${path.ifEmpty { "/" }}"
    }

    fun host(url: String): String? =
        url.substringAfter("://", "").substringBefore('/').substringBefore('?').lowercase()
            .removePrefix("www.").takeIf { it.isNotBlank() }
}
