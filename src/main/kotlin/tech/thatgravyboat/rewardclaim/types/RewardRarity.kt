package tech.thatgravyboat.rewardclaim.types

enum class RewardRarity(val translationKey: String, val colorCode: Char) {
    COMMON("rarity.common", 'f'),
    RARE("rarity.rare", 'b'),
    EPIC("rarity.epic", '5'),
    LEGENDARY("rarity.legendary", '6');

    val color: String
        get() = "§$colorCode"
}
