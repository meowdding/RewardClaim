package tech.thatgravyboat.rewardclaim

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screen.Screen
import tech.thatgravyboat.rewardclaim.remote.RemoteData
import tech.thatgravyboat.rewardclaim.ui.RewardClaimScreen

object Initializer {

    private var rewardClaimTime: Long = 0

    fun onMessage(message: String): Boolean {
        val stripped = stripFormatting(message.trim())
        val rewardId = sequenceOf(
            RemoteData.rewardMessageRegex,
            RemoteData.rewardMissedMessageRegex
        ).firstNotNullOfOrNull { regex ->
            regex.matchEntire(stripped)?.groups?.get("id")?.value
        } ?: return false

        if (!RemoteData.disabled) {
            rewardClaimTime = System.currentTimeMillis()
            Minecraft.getInstance().openScreen(RewardClaimScreen(rewardId))
        }
        return true
    }

    fun shouldCancelScreen(current: Screen?, next: Screen?): Boolean {
        if (current !is RewardClaimScreen) return false
        if (System.currentTimeMillis() - rewardClaimTime > Config.checkingTimer) return false
        return next != null
    }

    private fun stripFormatting(input: String): String {
        return input.replace(Regex("\u00A7."), "").replace("\n", "")
    }
}
