package com.ankith.reelaudio.links

/**
 * Well-known tools and apps that reels often mention by name only ("use Notion for this"),
 * with their websites.
 *
 * Names that are also everyday words (Notion, Cursor, Linear) only match when written with a
 * capital letter, as in captions and transcripts; the rest match in any case.
 */
object KnownTools {
    private class Tool(val names: List<String>, val url: String, val anyCase: Boolean)

    private val TOOLS = listOf(
        // AI assistants and writing
        Tool(listOf("ChatGPT", "Chat GPT"), "https://chatgpt.com/", true),
        Tool(listOf("Claude AI"), "https://claude.ai/", true),
        Tool(listOf("Gemini"), "https://gemini.google.com/", false),
        Tool(listOf("Perplexity"), "https://www.perplexity.ai/", false),
        Tool(listOf("Copilot"), "https://copilot.microsoft.com/", false),
        Tool(listOf("DeepSeek", "Deep Seek"), "https://www.deepseek.com/", true),
        Tool(listOf("Grok"), "https://grok.com/", false),
        Tool(listOf("Grammarly"), "https://www.grammarly.com/", true),
        Tool(listOf("QuillBot"), "https://quillbot.com/", true),
        Tool(listOf("Jasper AI"), "https://www.jasper.ai/", true),
        Tool(listOf("Character AI", "Character.AI"), "https://character.ai/", true),
        Tool(listOf("NotebookLM", "Notebook LM"), "https://notebooklm.google.com/", true),
        Tool(listOf("Poe"), "https://poe.com/", false),
        Tool(listOf("Hugging Face", "HuggingFace"), "https://huggingface.co/", true),
        // Images, video and voice
        Tool(listOf("Midjourney", "Mid Journey"), "https://www.midjourney.com/", true),
        Tool(listOf("DALL-E", "DALL·E", "Dall-E"), "https://openai.com/dall-e-3/", true),
        Tool(listOf("Leonardo AI", "Leonardo.ai"), "https://leonardo.ai/", true),
        Tool(listOf("Ideogram"), "https://ideogram.ai/", true),
        Tool(listOf("Runway ML", "RunwayML"), "https://runwayml.com/", true),
        Tool(listOf("Pika Labs", "Pika AI"), "https://pika.art/", true),
        Tool(listOf("Sora"), "https://sora.com/", false),
        Tool(listOf("HeyGen", "Hey Gen"), "https://www.heygen.com/", true),
        Tool(listOf("Synthesia"), "https://www.synthesia.io/", true),
        Tool(listOf("ElevenLabs", "Eleven Labs"), "https://elevenlabs.io/", true),
        Tool(listOf("Suno AI", "Suno"), "https://suno.com/", false),
        Tool(listOf("Udio"), "https://www.udio.com/", true),
        Tool(listOf("CapCut", "Cap Cut"), "https://www.capcut.com/", true),
        Tool(listOf("Descript"), "https://www.descript.com/", false),
        Tool(listOf("Opus Clip", "OpusClip"), "https://www.opus.pro/", true),
        Tool(listOf("InVideo"), "https://invideo.io/", true),
        Tool(listOf("Canva"), "https://www.canva.com/", true),
        Tool(listOf("Figma"), "https://www.figma.com/", true),
        Tool(listOf("Photopea"), "https://www.photopea.com/", true),
        Tool(listOf("Remove.bg", "remove bg"), "https://www.remove.bg/", true),
        Tool(listOf("Krea AI", "Krea"), "https://www.krea.ai/", false),
        Tool(listOf("Adobe Firefly"), "https://firefly.adobe.com/", true),
        Tool(listOf("Lightroom"), "https://lightroom.adobe.com/", true),
        Tool(listOf("Spline"), "https://spline.design/", false),
        // Notes, productivity and slides
        Tool(listOf("Notion"), "https://www.notion.so/", false),
        Tool(listOf("Obsidian"), "https://obsidian.md/", false),
        Tool(listOf("Evernote"), "https://evernote.com/", true),
        Tool(listOf("Todoist"), "https://todoist.com/", true),
        Tool(listOf("TickTick"), "https://ticktick.com/", true),
        Tool(listOf("Trello"), "https://trello.com/", true),
        Tool(listOf("ClickUp", "Click Up"), "https://clickup.com/", true),
        Tool(listOf("Asana"), "https://asana.com/", false),
        Tool(listOf("Miro"), "https://miro.com/", false),
        Tool(listOf("Excalidraw"), "https://excalidraw.com/", true),
        Tool(listOf("Gamma AI", "Gamma app"), "https://gamma.app/", true),
        Tool(listOf("Tome AI"), "https://tome.app/", true),
        Tool(listOf("Beautiful.ai", "Beautiful AI"), "https://www.beautiful.ai/", true),
        Tool(listOf("Otter AI", "Otter.ai"), "https://otter.ai/", true),
        Tool(listOf("Calendly"), "https://calendly.com/", true),
        Tool(listOf("Zapier"), "https://zapier.com/", true),
        Tool(listOf("Make.com"), "https://www.make.com/", true),
        Tool(listOf("n8n"), "https://n8n.io/", true),
        Tool(listOf("Airtable"), "https://www.airtable.com/", true),
        Tool(listOf("Forest app"), "https://www.forestapp.cc/", true),
        Tool(listOf("Raycast"), "https://www.raycast.com/", true),
        Tool(listOf("Readwise"), "https://readwise.io/", true),
        Tool(listOf("Pocket app"), "https://getpocket.com/", true),
        // Building, coding and websites
        Tool(listOf("Cursor AI", "Cursor"), "https://www.cursor.com/", false),
        Tool(listOf("GitHub", "Git Hub"), "https://github.com/", true),
        Tool(listOf("Replit"), "https://replit.com/", true),
        Tool(listOf("Lovable"), "https://lovable.dev/", false),
        Tool(listOf("Bolt.new", "Bolt new"), "https://bolt.new/", true),
        Tool(listOf("v0.dev", "v0 by Vercel"), "https://v0.dev/", true),
        Tool(listOf("Vercel"), "https://vercel.com/", true),
        Tool(listOf("Netlify"), "https://www.netlify.com/", true),
        Tool(listOf("Framer"), "https://www.framer.com/", false),
        Tool(listOf("Webflow"), "https://webflow.com/", true),
        Tool(listOf("Wix"), "https://www.wix.com/", false),
        Tool(listOf("Squarespace"), "https://www.squarespace.com/", true),
        Tool(listOf("WordPress"), "https://wordpress.com/", true),
        Tool(listOf("Shopify"), "https://www.shopify.com/", true),
        Tool(listOf("Supabase"), "https://supabase.com/", true),
        Tool(listOf("Firebase"), "https://firebase.google.com/", true),
        Tool(listOf("Stack Overflow", "StackOverflow"), "https://stackoverflow.com/", true),
        Tool(listOf("LeetCode", "Leet Code"), "https://leetcode.com/", true),
        Tool(listOf("roadmap.sh"), "https://roadmap.sh/", true),
        // Learning
        Tool(listOf("Coursera"), "https://www.coursera.org/", true),
        Tool(listOf("Udemy"), "https://www.udemy.com/", true),
        Tool(listOf("Khan Academy"), "https://www.khanacademy.org/", true),
        Tool(listOf("Duolingo"), "https://www.duolingo.com/", true),
        Tool(listOf("Skillshare"), "https://www.skillshare.com/", true),
        Tool(listOf("edX"), "https://www.edx.org/", false),
        Tool(listOf("freeCodeCamp", "Free Code Camp"), "https://www.freecodecamp.org/", true),
        Tool(listOf("Brilliant.org"), "https://brilliant.org/", true),
        Tool(listOf("MasterClass", "Master Class"), "https://www.masterclass.com/", false),
        Tool(listOf("Blinkist"), "https://www.blinkist.com/", true),
        Tool(listOf("Goodreads"), "https://www.goodreads.com/", true),
        Tool(listOf("Anki"), "https://apps.ankiweb.net/", false),
        Tool(listOf("Quizlet"), "https://quizlet.com/", true),
        Tool(listOf("Wolfram Alpha", "WolframAlpha"), "https://www.wolframalpha.com/", true),
        Tool(listOf("Headspace"), "https://www.headspace.com/", true),
        Tool(listOf("Calm app"), "https://www.calm.com/", true),
        Tool(listOf("Medium.com"), "https://medium.com/", true),
        Tool(listOf("Substack"), "https://substack.com/", true),
        Tool(listOf("LinkedIn Learning"), "https://www.linkedin.com/learning/", true),
        Tool(listOf("Pinterest"), "https://www.pinterest.com/", true),
    )

    private class Matcher(val regex: Regex, val tool: Tool)

    private val MATCHERS = TOOLS.flatMap { tool ->
        tool.names.map { name ->
            val pattern = """(?<![\w.@/-])${Regex.escape(name)}(?![\w-]|\.\w)"""
            Matcher(if (tool.anyCase) Regex(pattern, RegexOption.IGNORE_CASE) else Regex(pattern), tool)
        }
    }

    /** Websites of the known tools named in [text], in the order they are first mentioned. */
    fun find(text: String): List<String> =
        MATCHERS.mapNotNull { m -> m.regex.find(text)?.let { it.range.first to m.tool.url } }
            .sortedBy { it.first }
            .map { it.second }
            .distinct()
}
