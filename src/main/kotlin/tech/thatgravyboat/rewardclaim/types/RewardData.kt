package tech.thatgravyboat.rewardclaim.types

import com.google.gson.annotations.SerializedName
import tech.thatgravyboat.rewardclaim.remote.RemoteData

private val ARMOR_REGEX = Regex("(^[a-z0-9_]+)_([a-z]+)$", RegexOption.IGNORE_CASE)

data class RewardData(
    val rarity: RewardRarity,
    private val reward: String,
    val amount: Int?,
    val intlist: Array<Int>?,
    @SerializedName("gameType")
    private val gameType: String?,
    @SerializedName("package")
    private val rewardPackage: String?,
    @SerializedName("key")
    private val rewardKey: String?
) {

    fun getDisplayName(language: RewardLanguage): String {
        rewardPackage?.let { item ->
            if (reward.equals("housing_package", ignoreCase = true)) {
                val packageName = item.removePrefix("specialoccasion_reward_card_skull_")
                return "${rarity.color}${language.translate("housing.skull.$packageName")}"
            }
        }
        if (reward.equals("add_vanity", ignoreCase = true)) {
            val key = rewardKey.orEmpty()
            val armorMatcher = ARMOR_REGEX.find(key)
            if ("suit" in key && armorMatcher != null) {
                val typeName = language.translateOrNull("vanity." + armorMatcher.groups[1]!!.value)
                val suitName = language.translateOrNull("vanity.armor." + armorMatcher.groups[2]!!.value)
                if (typeName != null && suitName != null) {
                    return "${rarity.color}$typeName $suitName"
                }
                return "${rarity.color}${language.translate("type.add_vanity")}"
            } else if ("emote" in key || "taunt" in key) {
                val name = language.translateOrNull("vanity.$key") ?: language.translate("type.add_vanity")
                return "${rarity.color}$name"
            }
            return "${rarity.color}${language.translate("type.add_vanity")}"
        }
        return if (reward.equals("tokens", ignoreCase = true) || reward.equals("coins", ignoreCase = true)) {
            val mode = GameMode.getModeFromId(gameType)
            "${rarity.color}${language.translate("type.$reward").replace("{\$game}", mode.displayName)}"
        } else {
            "${rarity.color}${language.translate("type.$reward")}"
        }
    }

    fun getDescription(language: RewardLanguage): String {
        if (reward.equals("add_vanity", ignoreCase = true)) {
            val descriptionKey = when {
                rewardKey?.contains("suit") == true -> "vanity.suits.description"
                rewardKey?.contains("emote") == true -> "vanity.emotes.description"
                rewardKey?.contains("taunt") == true -> "vanity.gestures.description"
                else -> null
            }
            return descriptionKey?.let(language::translateOrNull).orEmpty()
        }
        val description = language.translateOrNull("type.$reward.description") ?: return ""
        return if (reward.equals("tokens", ignoreCase = true) || reward.equals("coins", ignoreCase = true)) {
            val mode = GameMode.getModeFromId(gameType)
            description.replace("{\$game}", mode.displayName)
        } else description
    }

    val image: RewardImage?
        get() {
            var id = reward.lowercase()
            rewardKey?.let { key ->
                if (id == "add_vanity") {
                    when {
                        "suit" in key -> id += "_suit"
                        "emote" in key -> id += "_emote"
                        "taunt" in key -> id += "_taunt"
                    }
                }
            }
            rewardPackage?.let { item ->
                if (id == "housing_package") {
                    val packageId = item.removePrefix("specialoccasion_reward_card_skull_")
                    RemoteData.textures[id + "_" + packageId]?.let { image -> return image }
                }
            }
            return RemoteData.textures[id]
        }
}
