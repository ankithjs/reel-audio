package com.ankith.reelaudio

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

    @Test fun skipsTheReelItselfAndPlainWords() {
        assertEquals(
            emptyList<String>(),
            IdeaExtractor.findLinks("Watch https://www.instagram.com/reel/abc/ e.g. version 1.5 is out", null),
        )
    }
}
