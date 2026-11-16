package tech.thatgravyboat.rewardclaim

import net.fabricmc.api.ClientModInitializer
import tech.thatgravyboat.rewardclaim.remote.RemoteData

object RewardClaimClient : ClientModInitializer {

    override fun onInitializeClient() {
        Config.initializeOneConfig()
        RemoteData.loadData()
    }
}
