package tech.thatgravyboat.rewardclaim.types

import java.net.URI
import java.net.URL

data class RewardImage(val url: String, val imageType: String) {
    fun toUrl(): URL? {
        return try {
            URI(url).toURL()
        } catch (e: Exception) {
            null
        }
    }
}
