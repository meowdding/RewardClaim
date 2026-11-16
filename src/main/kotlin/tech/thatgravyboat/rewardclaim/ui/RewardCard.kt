package tech.thatgravyboat.rewardclaim.ui

import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO
import net.minecraft.client.Minecraft
import net.minecraft.client.render.texture.DynamicTexture
import net.minecraft.resource.Identifier
import org.lwjgl.opengl.GL11
import org.slf4j.LoggerFactory
import tech.thatgravyboat.rewardclaim.Threading
import tech.thatgravyboat.rewardclaim.remote.RemoteData
import tech.thatgravyboat.rewardclaim.types.RewardData
import tech.thatgravyboat.rewardclaim.types.RewardImage
import tech.thatgravyboat.rewardclaim.types.RewardLanguage

private val LOGGER = LoggerFactory.getLogger(RewardCard::class.java)

class RewardCard(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
) {

    private var rewardData: RewardData? = null
    private var language: RewardLanguage? = null
    private var selected: Boolean = false
    private var hovered: Boolean = false

    fun setData(reward: RewardData?, lang: RewardLanguage?) {
        rewardData = reward
        language = lang
        reward?.image?.let(::loadImage)
    }

    fun setSelected(isSelected: Boolean) {
        selected = isSelected
    }

    fun containsPoint(mouseX: Int, mouseY: Int): Boolean =
        mouseX in x until x + width && mouseY in y until y + height

    fun renderCard(mc: Minecraft, mouseX: Int, mouseY: Int) {
        val reward = rewardData ?: return
        hovered = containsPoint(mouseX, mouseY)

        val background = when {
            selected -> 0xFF006B08.toInt()
            hovered -> 0xFF303A32.toInt()
            else -> 0xE51A1A1A.toInt()
        }
        drawRect(x, y, x + width, y + height, background)
        drawBorder(
            x, y, x + width, y + height,
            if (selected) 0xFF71B978.toInt() else if (hovered) 0xFFAAAAAA.toInt() else 0xFF050505.toInt()
        )

        val iconSize = minOf(64, height - 16)
        reward.image?.let {
            drawImage(mc, it, x + 28f, y + (height - iconSize) / 2f, iconSize.toFloat(), iconSize.toFloat())
        }

        val title = reward.getDisplayName(language ?: RewardLanguage(""))
        val textX = x + 93
        val lang = language ?: RewardLanguage("")
        val textWidth = (x + width - 8 - textX).coerceAtLeast(1)
        drawFittedText(mc, title, textX, y + 4f, textWidth)
        drawHorizontalLine(textX, x + width - 8, y + 16, 0xFFBDBDBD.toInt())
        val rarity = "Rarity: ${reward.rarity.color}${lang.translate(reward.rarity.translationKey)}"
        drawFittedText(mc, rarity, textX, y + 20f, textWidth)
        reward.amount?.let { amount ->
            drawFittedText(mc, "Amount: §6$amount", textX, y + 32f, textWidth)
        }
    }

    private fun drawFittedText(mc: Minecraft, text: String, x: Int, y: Float, maxWidth: Int) {
        val measuredWidth = mc.textRenderer.getWidth(text)
        val scale = if (measuredWidth > maxWidth) maxWidth.toFloat() / measuredWidth else 1f
        GL11.glPushMatrix()
        GL11.glTranslatef(x.toFloat(), y, 0f)
        GL11.glScalef(scale, 1f, 1f)
        mc.textRenderer.draw(text, 0f, 0f, 0xFFFFFF, false)
        GL11.glPopMatrix()
    }

    private fun loadImage(image: RewardImage) {
        val url = image.toUrl() ?: return
        val key = image.url
        if (textures.containsKey(key) || !loading.add(key)) return

        Threading.runAsync {
            try {
                val connection = url.openConnection()
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                val bufferedImage = connection.getInputStream().use { ImageIO.read(it) }
                    ?: throw IllegalStateException("Unsupported reward image: $key")
                Minecraft.getInstance().executeTask(Runnable {
                    try {
                        val identifier = Minecraft.getInstance().textureManager.register(
                            "rewardclaim/" + key.hashCode().toUInt().toString(16),
                            DynamicTexture(bufferedImage)
                        )
                        textures[key] = identifier
                    } catch (e: Exception) {
                        LOGGER.warn("Could not register reward image texture: {}.", key, e)
                    } finally {
                        loading.remove(key)
                    }
                })
            } catch (e: Exception) {
                loading.remove(key)
                LOGGER.warn("Could not load reward image: {}.", key, e)
            }
        }
    }

    companion object {
        internal val textures = ConcurrentHashMap<String, Identifier>()
        internal val loading = ConcurrentHashMap.newKeySet<String>()

        internal fun drawImage(mc: Minecraft, image: RewardImage, x: Float, y: Float, maxWidth: Float, maxHeight: Float) {
            val texture = textures[image.url] ?: return
            val imageType = RemoteData.getImageType(image.imageType)
            val scale = minOf(maxWidth / imageType.width, maxHeight / imageType.height)
            val imageWidth = imageType.width * scale
            val imageHeight = imageType.height * scale
            val imageX = x + (maxWidth - imageWidth) / 2f
            val imageY = y + (maxHeight - imageHeight) / 2f

            mc.textureManager.bind(texture)
            GL11.glColor4f(1f, 1f, 1f, 1f)
            GL11.glBegin(GL11.GL_QUADS)
            GL11.glTexCoord2f(0f, 0f)
            GL11.glVertex2f(imageX, imageY)
            GL11.glTexCoord2f(0f, 1f)
            GL11.glVertex2f(imageX, imageY + imageHeight)
            GL11.glTexCoord2f(1f, 1f)
            GL11.glVertex2f(imageX + imageWidth, imageY + imageHeight)
            GL11.glTexCoord2f(1f, 0f)
            GL11.glVertex2f(imageX + imageWidth, imageY)
            GL11.glEnd()
        }

        internal fun drawRect(left: Int, top: Int, right: Int, bottom: Int, color: Int) {
            val alpha = (color shr 24 and 0xFF) / 255.0f
            val red = (color shr 16 and 0xFF) / 255.0f
            val green = (color shr 8 and 0xFF) / 255.0f
            val blue = (color and 0xFF) / 255.0f

            GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glEnable(GL11.GL_BLEND)
            GL11.glDisable(GL11.GL_DEPTH_TEST)
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
            GL11.glColor4f(red, green, blue, alpha)
            GL11.glLineWidth(1f)
            GL11.glBegin(GL11.GL_QUADS)
            GL11.glVertex2d(left.toDouble(), bottom.toDouble())
            GL11.glVertex2d(right.toDouble(), bottom.toDouble())
            GL11.glVertex2d(right.toDouble(), top.toDouble())
            GL11.glVertex2d(left.toDouble(), top.toDouble())
            GL11.glEnd()
            GL11.glEnable(GL11.GL_TEXTURE_2D)
            GL11.glDisable(GL11.GL_BLEND)
            GL11.glEnable(GL11.GL_DEPTH_TEST)
        }

        internal fun drawBorder(left: Int, top: Int, right: Int, bottom: Int, color: Int) {
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glColor4f(
                (color shr 16 and 0xFF) / 255f,
                (color shr 8 and 0xFF) / 255f,
                (color and 0xFF) / 255f,
                (color shr 24 and 0xFF) / 255f
            )
            GL11.glLineWidth(1.5f)
            GL11.glBegin(GL11.GL_LINE_LOOP)
            GL11.glVertex2d(left.toDouble(), top.toDouble())
            GL11.glVertex2d(right.toDouble(), top.toDouble())
            GL11.glVertex2d(right.toDouble(), bottom.toDouble())
            GL11.glVertex2d(left.toDouble(), bottom.toDouble())
            GL11.glEnd()
            GL11.glColor4f(1f, 1f, 1f, 1f)
            GL11.glEnable(GL11.GL_TEXTURE_2D)
        }

        private fun drawHorizontalLine(startX: Int, endX: Int, y: Int, color: Int) {
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glColor4f(
                (color shr 16 and 0xFF) / 255f,
                (color shr 8 and 0xFF) / 255f,
                (color and 0xFF) / 255f,
                (color shr 24 and 0xFF) / 255f
            )
            GL11.glBegin(GL11.GL_LINES)
            GL11.glVertex2i(startX, y)
            GL11.glVertex2i(endX, y)
            GL11.glEnd()
            GL11.glColor4f(1f, 1f, 1f, 1f)
            GL11.glEnable(GL11.GL_TEXTURE_2D)
        }
    }
}
