package com.ankith.reelaudio

import com.ankith.reelaudio.links.ClaudeSkills
import com.ankith.reelaudio.links.IdeaExtractor
import org.junit.Assert.assertEquals
import org.junit.Test

class IdeaExtractorTest {
    @Test fun findsFullBareAndSpokenLinks() {
        val text = """
            Try https://www.perplexity.ai/discover?x=1 for research.
            Also notion.so/templates is great, and Excalidraw.com for sketches.
            My favourite is gamma dot app. Mail me at me@gmail.com.
            Follow instagram.com/someone for more.
        """.trimIndent()
        assertEquals(
            listOf(
                "https://www.perplexity.ai/discover?x=1",
                "https://notion.so/templates",
                "https://excalidraw.com/",
                "https://gamma.app/",
            ),
            IdeaExtractor.findLinks(text),
        )
    }

    @Test fun findsToolsNamedWithoutAWebsite() {
        val text = """
            I plan my week in Notion and make slides with gamma ai.
            Then I ask chatgpt to check it. Perplexity is my search engine: perplexity.ai
            Move the cursor, it's a linear notion of time.
        """.trimIndent()
        assertEquals(
            listOf(
                "https://perplexity.ai/",
                "https://www.notion.so/",
                "https://gamma.app/",
                "https://chatgpt.com/",
            ),
            IdeaExtractor.findLinks(text),
        )
    }

    @Test fun findsClaudeSkills() {
        val text = """
            5 Claude skills you need. First, the frontend design skill makes your sites look great.
            Then a skill called Superpowers for planning. Run /skill-creator to make your own.
            The Changelog Writer skill drafts release notes. It's a useful skill.
        """.trimIndent()
        assertEquals(
            listOf(
                "frontend-design" to true,
                "superpowers" to true,
                "skill-creator" to true,
                "changelog-writer" to false,
            ),
            ClaudeSkills.find(text).map { it.name to it.readPage },
        )
        assertEquals(
            "Then a skill called Superpowers for planning.",
            ClaudeSkills.find(text)[1].fromReel,
        )
    }

    @Test fun ignoresSkillTalkThatIsNotAboutClaude() {
        assertEquals(emptyList<ClaudeSkills.Found>(), ClaudeSkills.find("Public speaking is the Best Life skill."))
    }

    @Test fun skipsTheReelItselfAndPlainWords() {
        assertEquals(
            emptyList<String>(),
            IdeaExtractor.findLinks("Watch https://www.instagram.com/reel/abc/ e.g. version 1.5 is out", null),
        )
    }
}
