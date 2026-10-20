package com.ankith.reelaudio.links

/**
 * Skills that reels talk about ("learn public speaking", "start journaling"), each saved as
 * its Wikipedia article so the Links entry carries a plain explanation of the skill.
 *
 * Only names that are clearly a skill are listed; everyday words like "writing" or "running"
 * would match far too often.
 */
object KnownSkills {
    private class Skill(val names: List<String>, val article: String)

    private val SKILLS = listOf(
        // Mind and self-improvement
        Skill(listOf("meditation", "meditating"), "Meditation"),
        Skill(listOf("mindfulness"), "Mindfulness"),
        Skill(listOf("journaling", "journalling"), "Journaling"),
        Skill(listOf("stoicism"), "Stoicism"),
        Skill(listOf("critical thinking"), "Critical_thinking"),
        Skill(listOf("first principles thinking", "first-principles thinking"), "First_principle"),
        Skill(listOf("emotional intelligence"), "Emotional_intelligence"),
        Skill(listOf("self-discipline", "self discipline"), "Self-discipline"),
        Skill(listOf("delayed gratification"), "Delayed_gratification"),
        Skill(listOf("time management"), "Time_management"),
        Skill(listOf("pomodoro technique", "pomodoro method"), "Pomodoro_Technique"),
        Skill(listOf("deep work"), "Deep_work"),
        Skill(listOf("goal setting", "goal-setting"), "Goal_setting"),
        Skill(listOf("habit building", "habit formation"), "Habit_formation"),
        Skill(listOf("speed reading"), "Speed_reading"),
        Skill(listOf("memory palace", "method of loci"), "Method_of_loci"),
        Skill(listOf("feynman technique"), "Learning_by_teaching"),
        Skill(listOf("spaced repetition"), "Spaced_repetition"),
        Skill(listOf("active recall"), "Testing_effect"),
        Skill(listOf("lucid dreaming"), "Lucid_dream"),
        Skill(listOf("breathwork", "box breathing"), "Breathwork"),
        Skill(listOf("cold exposure", "cold showers", "cold plunge"), "Cold_water_immersion"),
        Skill(listOf("intermittent fasting"), "Intermittent_fasting"),
        Skill(listOf("yoga"), "Yoga"),
        Skill(listOf("calisthenics"), "Calisthenics"),
        // Speaking and people
        Skill(listOf("public speaking"), "Public_speaking"),
        Skill(listOf("storytelling"), "Storytelling"),
        Skill(listOf("active listening"), "Active_listening"),
        Skill(listOf("negotiation", "negotiating"), "Negotiation"),
        Skill(listOf("persuasion"), "Persuasion"),
        Skill(listOf("body language"), "Body_language"),
        Skill(listOf("networking skills"), "Business_networking"),
        Skill(listOf("leadership skills", "leadership"), "Leadership"),
        Skill(listOf("conflict resolution"), "Conflict_resolution"),
        Skill(listOf("communication skills"), "Communication"),
        Skill(listOf("debating", "debate skills"), "Debate"),
        Skill(listOf("improv", "improvisation"), "Improvisational_theatre"),
        // Writing and creative
        Skill(listOf("copywriting"), "Copywriting"),
        Skill(listOf("creative writing"), "Creative_writing"),
        Skill(listOf("poetry writing", "writing poetry"), "Poetry"),
        Skill(listOf("songwriting"), "Songwriting"),
        Skill(listOf("calligraphy"), "Calligraphy"),
        Skill(listOf("sketching"), "Sketch_(drawing)"),
        Skill(listOf("photography"), "Photography"),
        Skill(listOf("videography"), "Videography"),
        Skill(listOf("video editing"), "Video_editing"),
        Skill(listOf("graphic design"), "Graphic_design"),
        Skill(listOf("ui design", "ux design", "ui/ux", "ui ux design"), "User_experience_design"),
        Skill(listOf("3d modeling", "3d modelling"), "3D_modeling"),
        Skill(listOf("animation"), "Animation"),
        Skill(listOf("music production"), "Record_producer"),
        Skill(listOf("touch typing"), "Touch_typing"),
        // Work and money
        Skill(listOf("personal finance"), "Personal_finance"),
        Skill(listOf("financial literacy"), "Financial_literacy"),
        Skill(listOf("investing"), "Investment"),
        Skill(listOf("stock market"), "Stock_market"),
        Skill(listOf("budgeting"), "Budget"),
        Skill(listOf("compound interest"), "Compound_interest"),
        Skill(listOf("entrepreneurship"), "Entrepreneurship"),
        Skill(listOf("digital marketing"), "Digital_marketing"),
        Skill(listOf("content marketing"), "Content_marketing"),
        Skill(listOf("affiliate marketing"), "Affiliate_marketing"),
        Skill(listOf("email marketing"), "Email_marketing"),
        Skill(listOf("social media marketing"), "Social_media_marketing"),
        Skill(listOf("personal branding"), "Personal_branding"),
        Skill(listOf("search engine optimization", "seo"), "Search_engine_optimization"),
        Skill(listOf("sales skills", "salesmanship"), "Sales"),
        Skill(listOf("project management"), "Project_management"),
        Skill(listOf("product management"), "Product_management"),
        Skill(listOf("freelancing"), "Freelancer"),
        Skill(listOf("dropshipping", "drop shipping"), "Drop_shipping"),
        Skill(listOf("prompt engineering"), "Prompt_engineering"),
        Skill(listOf("data analysis", "data analytics"), "Data_analysis"),
        Skill(listOf("data science"), "Data_science"),
        Skill(listOf("excel skills", "microsoft excel"), "Microsoft_Excel"),
        // Tech
        Skill(listOf("coding", "programming"), "Computer_programming"),
        Skill(listOf("python programming", "learn python", "python language"), "Python_(programming_language)"),
        Skill(listOf("javascript"), "JavaScript"),
        Skill(listOf("web development"), "Web_development"),
        Skill(listOf("app development"), "Mobile_app_development"),
        Skill(listOf("machine learning"), "Machine_learning"),
        Skill(listOf("artificial intelligence"), "Artificial_intelligence"),
        Skill(listOf("cybersecurity", "cyber security", "ethical hacking"), "Computer_security"),
        Skill(listOf("cloud computing"), "Cloud_computing"),
        Skill(listOf("blockchain"), "Blockchain"),
        Skill(listOf("sql"), "SQL"),
        // Everyday life
        Skill(listOf("cooking skills", "meal prep", "meal prepping"), "Meal_preparation"),
        Skill(listOf("first aid"), "First_aid"),
        Skill(listOf("self-defense", "self defense", "self-defence"), "Self-defense"),
        Skill(listOf("chess"), "Chess"),
        Skill(listOf("swimming"), "Swimming"),
        Skill(listOf("sign language"), "Sign_language"),
        Skill(listOf("language learning"), "Language_acquisition"),
    )

    private const val WIKI = "https://en.wikipedia.org/wiki/"

    private class Matcher(val regex: Regex, val url: String)

    private val MATCHERS = SKILLS.flatMap { skill ->
        skill.names.map { name ->
            Matcher(
                Regex("""(?<![\w@/.-])${Regex.escape(name)}(?![\w-])""", RegexOption.IGNORE_CASE),
                WIKI + skill.article,
            )
        }
    }

    /** True when [url] is a skill saved by this list, so the app can label it as a skill. */
    fun isSkill(url: String): Boolean = url.startsWith(WIKI) && SKILLS.any { url == WIKI + it.article }

    /** Wikipedia pages of the skills named in [text], in the order they are first mentioned. */
    fun find(text: String): List<String> =
        MATCHERS.mapNotNull { m -> m.regex.find(text)?.let { it.range.first to m.url } }
            .sortedBy { it.first }
            .map { it.second }
            .distinct()
}
