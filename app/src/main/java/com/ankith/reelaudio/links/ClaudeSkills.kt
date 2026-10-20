package com.ankith.reelaudio.links

import java.net.URLEncoder

/**
 * Finds Claude skills a reel talks about ("install the frontend-design skill", "a skill called
 * Superpowers", "/skill-creator").
 *
 * Skills from Anthropic's public skills repo link to their GitHub folder, so the app can read
 * the page. Any other skill name links to a web search, and its details are the reel's own
 * words about it.
 */
object ClaudeSkills {
    data class Found(val name: String, val url: String, val fromReel: String, val readPage: Boolean)

    private const val REPO = "https://github.com/anthropics/skills/tree/main/skills/"

    /** Skills in github.com/anthropics/skills. */
    private val OFFICIAL = setOf(
        "algorithmic-art", "brand-guidelines", "canvas-design", "doc-coauthoring", "docx",
        "frontend-design", "internal-comms", "mcp-builder", "pdf", "pptx", "skill-creator",
        "slack-gif-creator", "theme-factory", "web-artifacts-builder", "webapp-testing", "xlsx",
    )

    /** Well-known community skill collections and where they live. */
    private val COMMUNITY = mapOf(
        "superpowers" to "https://github.com/obra/superpowers",
        "taste-skill" to "https://github.com/Leonxlnx/taste-skill",
    )

    /** Install commands shown in captions or pasted in: "npx skills add owner/repo". */
    private val INSTALL = Regex("""\bnpx\s+(?:-y\s+)?skills(?:@[\w.]+)?\s+add\s+(?:https://github\.com/)?([\w.-]+)/([\w.-]+)""")

    /** Skills named by an install command, linked to their GitHub repo. Works without any mention of Claude. */
    fun fromInstallCommands(text: String): List<Found> =
        INSTALL.findAll(text).map { m ->
            val repo = m.groupValues[2].removeSuffix(".git")
            Found(repo.lowercase(), "https://github.com/${m.groupValues[1]}/$repo", m.value, readPage = true)
        }.distinctBy { it.url.lowercase() }.toList()

    /** Only look for skill names in reels that are about Claude or agent skills. */
    private val TOPIC = Regex("""\bclaude\b|skill\.md|agent skills?\b|skills? for claude""", RegexOption.IGNORE_CASE)

    private val PATTERNS = listOf(
        // "a skill called Superpowers", "the skill named frontend-design"
        Regex("""\bskill\s+(?:called|named)\s+["“'`]?([A-Za-z][A-Za-z0-9]*(?:-[A-Za-z0-9]+)*)""", RegexOption.IGNORE_CASE),
        // "the frontend-design skill", "`skill-creator` skill"
        Regex("""["“'`]?\b([a-z][a-z0-9]*(?:-[a-z0-9]+)+)["”'`]?\s+skill\b"""),
        // "the Frontend Design skill", "this Superpowers skill"
        Regex("""\b(?i:the|this|a|an|my|its)\s+([A-Z][A-Za-z0-9]*(?:[ -][A-Z][A-Za-z0-9]*){0,2})\s+skill\b"""),
        // Slash commands in captions: "/skill-creator"
        Regex("""(?<![\w/:.])/([a-z][a-z0-9]*(?:-[a-z0-9]+)+)\b"""),
    )

    private val NOT_NAMES = setOf(
        "claude", "claude code", "agent", "new", "custom", "useful", "best", "one", "this", "that",
        "the", "skill", "skills", "code", "simple", "same", "first", "next", "other", "special",
    )

    /** Known skills said out loud, where speech-to-text writes "skill creator" for "skill-creator". */
    private val SPOKEN = (OFFICIAL + COMMUNITY.keys).map { name ->
        val words = name.replace("-", "[ -]")
        Regex(if ('-' in name) """\b($words)\b""" else """\b($words)\s+skills?\b""", RegexOption.IGNORE_CASE)
    }

    private const val SEARCH = "https://www.google.com/search?q="

    /** The skill's name when [url] is one this finder made, so the app can label it. */
    fun nameFor(url: String): String? = when {
        url.startsWith(REPO) -> url.removePrefix(REPO).trimEnd('/')
        else -> COMMUNITY.entries.firstOrNull { it.value == url }?.key
    }

    /** Skills without a page of their own link to a search; there's nothing to read there. */
    fun isSearch(url: String): Boolean = url.startsWith(SEARCH)

    fun find(text: String): List<Found> {
        val found = linkedMapOf<String, Found>()
        fromInstallCommands(text).forEach { found[it.name] = it }
        if (!TOPIC.containsMatchIn(text)) return found.values.toList()
        val official = OFFICIAL.associateBy { it }
        val hits = (PATTERNS + SPOKEN).flatMap { p -> p.findAll(text).map { it.range.first to it.groupValues[1] }.toList() }
            .sortedBy { it.first }
        for ((at, raw) in hits) {
            val name = raw.trim().trimEnd('.', ',', '!', '?')
            val key = name.lowercase().replace(' ', '-')
            if (key in NOT_NAMES || name.lowercase() in NOT_NAMES || key.length < 3 || key in found) continue
            val url = when {
                key in official -> REPO + key
                key in COMMUNITY -> COMMUNITY.getValue(key)
                else -> SEARCH + URLEncoder.encode("Claude skill \"$name\"", "UTF-8")
            }
            val readPage = key in official || key in COMMUNITY
            found[key] = Found(key, url, sentenceAround(text, at), readPage)
        }
        return found.values.toList()
    }

    private fun sentenceAround(text: String, at: Int): String {
        val start = text.lastIndexOfAny(charArrayOf('.', '!', '?', '\n'), (at - 1).coerceAtLeast(0)).let {
            if (it < 0 || it >= at) 0 else it + 1
        }
        val end = text.indexOfAny(charArrayOf('.', '!', '?', '\n'), at).let { if (it < 0) text.length else it + 1 }
        val sentence = text.substring(start, end).trim()
        return if (sentence.length > 300) sentence.take(300).substringBeforeLast(' ') + "…" else sentence
    }
}
