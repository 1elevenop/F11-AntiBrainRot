package com.f11.app.data

import java.text.Normalizer
import kotlin.random.Random

enum class FrictionLanguage(val code: String, val speechTag: String, val label: String) {
    ENGLISH("en", "en-US", "English"),
    HINDI("hi", "hi-IN", "हिन्दी");

    companion object {
        fun fromCode(code: String?): FrictionLanguage = entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}

/** Hindi first-person verbs are gendered; the user picks the form that fits them. */
enum class HindiForm(val code: String, val label: String) {
    MASCULINE("m", "रहा हूँ"),
    FEMININE("f", "रही हूँ");

    companion object {
        fun fromCode(code: String?): HindiForm = entries.firstOrNull { it.code == code } ?: MASCULINE
    }
}

/**
 * Hard-hitting lines shown on the shield and typed/spoken to loosen a rule.
 * Hindi templates use {masculine|feminine} for gendered words.
 */
object FrictionLines {

    private val english = listOf(
        "Someone believes in my potential, and I am wasting it here.",
        "My future self will remember what I chose to do with this moment.",
        "I was given one life, and I am choosing to watch other people live theirs.",
        "I know what I should be doing, yet I choose distraction.",
        "Every scroll is a moment of my life I will never get back.",
        "I am voluntarily becoming a slave to my impulses.",
        "I am not tired. I am avoiding the life I need to build.",
        "I am sacrificing discipline for dopamine.",
        "I am watching my potential decay, one scroll at a time.",
    )

    private val hindi = listOf(
        "कोई मेरी क्षमता पर भरोसा करता है, और मैं उसे यहाँ बर्बाद कर {रहा|रही} हूँ।",
        "मेरा भविष्य याद रखेगा कि मैंने इस पल के साथ क्या किया।",
        "मुझे एक ही ज़िंदगी मिली है, और मैं दूसरों को अपनी ज़िंदगी जीते देख {रहा|रही} हूँ।",
        "मुझे पता है मुझे क्या करना चाहिए, फिर भी मैं ध्यान भटकाना चुन {रहा|रही} हूँ।",
        "हर स्क्रॉल मेरी ज़िंदगी का एक पल है जो कभी वापस नहीं आएगा।",
        "मैं अपनी मर्ज़ी से अपनी आदतों का गुलाम बन {रहा|रही} हूँ।",
        "मैं {थका|थकी} नहीं हूँ। मैं उस ज़िंदगी से भाग {रहा|रही} हूँ जो मुझे बनानी है।",
        "मैं डोपामिन के लिए अनुशासन कुर्बान कर {रहा|रही} हूँ।",
        "मैं अपनी क्षमता को एक-एक स्क्रॉल में खत्म होते देख {रहा|रही} हूँ।",
    )

    private val genderToken = Regex("\\{([^|}]*)\\|([^}]*)\\}")

    fun all(language: FrictionLanguage, form: HindiForm = HindiForm.MASCULINE): List<String> = when (language) {
        FrictionLanguage.ENGLISH -> english
        FrictionLanguage.HINDI -> hindi.map { line ->
            genderToken.replace(line) { if (form == HindiForm.FEMININE) it.groupValues[2] else it.groupValues[1] }
        }
    }

    /** Distinct random lines, e.g. one to type and another to speak. */
    fun pick(
        count: Int,
        language: FrictionLanguage,
        form: HindiForm = HindiForm.MASCULINE,
        random: Random = Random.Default,
    ): List<String> = all(language, form).shuffled(random).take(count)

    /** Lowercase, punctuation-free, single-spaced (works for Latin and Devanagari). */
    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFC)
            .lowercase()
            .replace(Regex("[\\p{P}\\p{S}।॥]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    /** Typing must be exact, ignoring case, punctuation and spacing. */
    fun typedMatches(expected: String, typed: String): Boolean =
        normalize(typed).isNotEmpty() && normalize(typed) == normalize(expected)

    /**
     * Speech recognition is imperfect, so a spoken line passes when most of its words were heard
     * in order-independent fashion.
     */
    fun spokenMatches(expected: String, heard: String, threshold: Double = 0.75): Boolean {
        val want = normalize(expected).split(' ').filter { it.isNotEmpty() }
        if (want.isEmpty()) return false
        val got = normalize(heard).split(' ').filter { it.isNotEmpty() }.toMutableList()
        var hits = 0
        for (word in want) {
            if (got.remove(word)) hits++
        }
        return hits.toDouble() / want.size >= threshold
    }
}
