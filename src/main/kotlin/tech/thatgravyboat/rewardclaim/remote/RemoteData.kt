package tech.thatgravyboat.rewardclaim.remote

import com.google.gson.Gson
import java.net.URI
import org.slf4j.LoggerFactory
import tech.thatgravyboat.rewardclaim.types.ImageType
import tech.thatgravyboat.rewardclaim.types.RewardImage

private val GSON = Gson()
private val DEFAULT_IMAGE_TYPE = ImageType(142, 100, false)
private val LOGGER = LoggerFactory.getLogger("Reward Claim")

object RemoteData {
    private var imageTypes: HashMap<String, ImageType> = hashMapOf()
    var textures: HashMap<String, RewardImage> = hashMapOf()
    var rewardMessageRegex: Regex = Regex("Click the link to visit our website and claim your reward: https://rewards\\.hypixel\\.net/claim-reward/(?<id>[A-Za-z0-9]{8})")
    var rewardMissedMessageRegex: Regex = Regex("We noticed you haven't claimed your free Daily Reward yet!\\nTo choose your reward you have to click the link to visit our website! As a reminder, here's your link for today:  https://rewards\\.hypixel\\.net/claim-reward/(?<id>[A-Za-z0-9]{8})")
    var userAgent: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.54 Safari/537.36"
    @JvmField
    var disabled: Boolean = false
    var disabledMessage: String = "Reward Claim was disabled by the mod author for an unknown reason."

    fun getImageType(type: String?) = imageTypes.getOrDefault(type, DEFAULT_IMAGE_TYPE)

    fun loadData() {
        try {
            val url = URI("https://raw.githubusercontent.com/ThatGravyBoat/RewardClaim/master/data.json").toURL()
            val content = url.readText()
            val config = GSON.fromJson(content, JsonConfig::class.java)
            textures = config.textures
            imageTypes = config.imageTypes
            rewardMessageRegex = Regex(config.rewardRegex)
            rewardMissedMessageRegex = Regex(config.missedRewardRegex)
            userAgent = config.userAgent
            disabled = config.disabled
            disabledMessage = if (config.disabledMessage2.isNullOrBlank()) config.disabledMessage else config.disabledMessage2
        } catch (e: Exception) {
            LOGGER.warn("Could not load Reward Claim configuration; using defaults.", e)
        }
    }

    private data class JsonConfig(
        val imageTypes: HashMap<String, ImageType> = hashMapOf(),
        val textures: HashMap<String, RewardImage> = hashMapOf(),
        val rewardRegex: String = "Click the link to visit our website and claim your reward: https://rewards\\.hypixel\\.net/claim-reward/(?<id>[A-Za-z0-9]{8})",
        val missedRewardRegex: String = "We noticed you haven't claimed your free Daily Reward yet!\\nTo choose your reward you have to click the link to visit our website! As a reminder, here's your link for today:  https://rewards\\.hypixel\\.net/claim-reward/(?<id>[A-Za-z0-9]{8})",
        val userAgent: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.54 Safari/537.36",
        val disabled: Boolean = false,
        val disabledMessage: String = "Reward Claim was disabled by the mod author for an unknown reason.",
        val disabledMessage2: String? = null
    )
}
