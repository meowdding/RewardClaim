package tech.thatgravyboat.rewardclaim

import org.polyfrost.oneconfig.api.config.v1.KtConfig

object RewardClaimOneConfig : KtConfig(
    id = "rewardclaim.json",
    title = "Reward Claim",
    category = Category.UTILITY,
) {
    var autoAdSkip by switch(
        name = "Ad Skip",
        def = false,
        description = "Automatically skip ad when it can be skipped.",
    )

    var noConfirm by switch(
        name = "No Confirmation",
        def = false,
        description = "Claim the selected reward without showing a confirmation.",
    )
}
