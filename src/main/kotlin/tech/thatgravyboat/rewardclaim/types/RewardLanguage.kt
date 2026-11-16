package tech.thatgravyboat.rewardclaim.types

private val TRANSLATION_REGEX = Regex("\"(?<key>(?:\\\\.|[^\"\\\\])+)\"\\s*:\\s*\"(?<text>(?:\\\\.|[^\"\\\\])*)\"")

class RewardLanguage(translationDataFromHtml: String) {

    private val translations = hashMapOf<String, String>()

    fun translate(key: String) = translations[key] ?: key
    fun translateOrNull(key: String) = translations[key]

    init {
        TRANSLATION_REGEX.findAll(translationDataFromHtml.replace("\\'", "'")).forEach { result ->
            translations[result.groups["key"]!!.value] = result.groups["text"]!!.value
        }
    }
}
