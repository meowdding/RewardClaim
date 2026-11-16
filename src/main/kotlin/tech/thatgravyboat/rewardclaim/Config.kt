package tech.thatgravyboat.rewardclaim

import net.fabricmc.loader.api.FabricLoader

object Config {
    var checkingTimer: Long = 3000
    private var oneConfigLoaded = false

    val autoAdSkip: Boolean
        get() = oneConfigLoaded && RewardClaimOneConfig.autoAdSkip

    val showConfirmation: Boolean
        get() = !oneConfigLoaded || !RewardClaimOneConfig.noConfirm

    fun initializeOneConfig() {
        oneConfigLoaded = FabricLoader.getInstance().isModLoaded("oneconfig")
        if (oneConfigLoaded) RewardClaimOneConfig.preload()
    }
}
