package tech.thatgravyboat.rewardclaim.data

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.Strictness
import com.mojang.serialization.JsonOps

data class RewardState(
    val token: String,
    val data: Data,
    val language: Language,
) {

    companion object {

        private val GSON = GsonBuilder()
            .setStrictness(Strictness.LENIENT)
            .create()

        fun get(security: MatchResult, data: MatchResult, i18n: MatchResult): RewardState {
            val token = security.groups["token"]?.value ?: error("Security token not found")
            val jsonData = data.groups["data"]?.value?.replace("\\'", "'") ?: error("Data not found")
            val translations = i18n.groups["translations"]?.value ?: error("Translations not found")

            return RewardState(
                token,
                DataCodec.CODEC.parse(JsonOps.INSTANCE, GSON.fromJson(jsonData, JsonElement::class.java)).orThrow,
                Language(translations)
            )
        }
    }
}
