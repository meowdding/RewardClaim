package tech.thatgravyboat.rewardclaim.ui

import java.awt.Desktop
import java.net.CookieHandler
import java.net.CookieManager
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.concurrent.TimeUnit
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import org.lwjgl.opengl.GL11
import org.slf4j.LoggerFactory
import tech.thatgravyboat.rewardclaim.Config
import tech.thatgravyboat.rewardclaim.Threading
import tech.thatgravyboat.rewardclaim.remote.RemoteData
import tech.thatgravyboat.rewardclaim.types.WebData

private val SECURITY_REGEX = Regex("window\\.securityToken = \"(?<token>.*)\";")
private val DATA_REGEX = Regex("window\\.appData = '(?<data>\\{.*})';")
private val LOGGER = LoggerFactory.getLogger(RewardClaimScreen::class.java)

enum class GuiState { LOADING, FAILED, SUCCESSFUL, FAILED_REWARDS }

class RewardClaimScreen(private val id: String) : Screen() {

    private var state: GuiState = GuiState.LOADING
    private var selected: Int = -1
    private var data: WebData? = null
    private var popupType: PopupType = PopupType.NONE
    private var lastClickTime: Long = 0
    private var lastClickedReward: Int = -1

    private val rewardCards = arrayOf<RewardCard?>(null, null, null)
    private var streakProgress: Int = 0

    private val mc: Minecraft = Minecraft.getInstance()

    enum class PopupType { NONE, ERROR, CONFIRM, AD_SKIP, AD_NO_SKIP }

    override fun init() {
        super.init()
        buttons.clear()

        val leftCenter = (width * 0.295f).toInt()
        buttons.add(object : ButtonWidget(-1, leftCenter - 77, (height * 0.62f).toInt(), 154, 28, "Claim Reward") {
            override fun mouseClicked(mc: Minecraft, mouseX: Int, mouseY: Int): Boolean {
                if (selected != -1) {
                    if (Config.showConfirmation) {
                        popupType = PopupType.CONFIRM
                    } else {
                        claimReward()
                    }
                }
                return super.mouseClicked(mc, mouseX, mouseY)
            }

            override fun render(mc: Minecraft, mouseX: Int, mouseY: Int) {
                hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height
                val color = if (hovered) 0xFF16A31B.toInt() else 0xFF008A08.toInt()
                RewardCard.drawRect(x, y, x + width, y + height, color)
                RewardCard.drawBorder(x, y, x + width, y + height, 0xFF74B877.toInt())
                val labelWidth = mc.textRenderer.getWidth(message)
                mc.textRenderer.draw(message, (x + (width - labelWidth) / 2f), (y + height / 2f - 4f), 0xFFFFFF, false)
            }
        })

        val rewardWidth = (width * 0.282f).toInt()
        val rewardHeight = maxOf((height * 0.15f).toInt(), 44)
        val startX = (width * 0.565f).toInt()
        val startY = (height * 0.29f).toInt()
        val spacing = maxOf((height * 0.175f).toInt(), rewardHeight + 6)
        for (i in 0..2) {
            rewardCards[i] = RewardCard(startX, startY + i * spacing, rewardWidth, rewardHeight)
        }

        fetchData()
    }

    private fun fetchData() {
        Threading.runAsync {
            try {
                CookieHandler.setDefault(CookieManager())
                val url = URI("https://rewards.hypixel.net/claim-reward/$id").toURL()
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.useCaches = true
                conn.addRequestProperty("User-Agent", RemoteData.userAgent)
                conn.readTimeout = 15000
                conn.connectTimeout = 15000
                conn.doOutput = true

                conn.inputStream.use { `is` ->
                    val html = `is`.bufferedReader().readText()
                    val securityMatcher = SECURITY_REGEX.find(html)
                    val dataMatcher = DATA_REGEX.find(html)
                    val translations = extractTranslations(html)

                    if (securityMatcher != null && dataMatcher != null && translations != null) {
                        val webData = WebData(securityMatcher, dataMatcher, translations)

                        mc.executeTask(Runnable {
                            data = webData

                            if (webData.rewards.isEmpty()) {
                                state = GuiState.FAILED_REWARDS
                                popupType = PopupType.ERROR
                            } else {
                                state = GuiState.SUCCESSFUL
                                updateRewardCards()
                            }

                            if (webData.skippable || webData.duration == 0) {
                                popupType = if (Config.autoAdSkip) PopupType.NONE else PopupType.AD_SKIP
                            } else {
                                Threading.schedule({ showAdPopup(true) }, webData.duration.toLong(), TimeUnit.SECONDS)
                            }
                        })
                    } else {
                        mc.executeTask(Runnable {
                            state = GuiState.FAILED_REWARDS
                            popupType = PopupType.ERROR
                        })
                    }
                }
            } catch (e: Exception) {
                LOGGER.warn("Could not load reward claim data.", e)
                mc.executeTask(Runnable {
                    state = GuiState.FAILED
                    popupType = PopupType.ERROR
                })
            }
        }
    }

    private fun updateRewardCards() {
        data?.let { d ->
            for (i in 0..2) {
                rewardCards[i]?.setData(d.rewards.getOrNull(i), d.language)
            }
        }
    }

    private fun extractTranslations(html: String): String? {
        val assignment = "window.i18n = "
        val start = html.indexOf(assignment)
        if (start == -1) return null

        val objectStart = html.indexOf('{', start + assignment.length)
        if (objectStart == -1) return null

        var depth = 0
        var quote: Char? = null
        var escaped = false
        for (index in objectStart until html.length) {
            val char = html[index]
            if (quote != null) {
                if (escaped) {
                    escaped = false
                } else if (char == '\\') {
                    escaped = true
                } else if (char == quote) {
                    quote = null
                }
                continue
            }

            when (char) {
                '"', '\'' -> quote = char
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return html.substring(objectStart, index + 1)
                }
            }
        }
        return null
    }

    private fun showAdPopup(skip: Boolean) {
        mc.executeTask(Runnable {
            popupType = when {
                skip && Config.autoAdSkip -> PopupType.NONE
                skip -> PopupType.AD_SKIP
                else -> PopupType.AD_NO_SKIP
            }
        })
    }

    override fun render(mouseX: Int, mouseY: Int, partialTicks: Float) {
        renderBackground()
        streakProgress = data?.streak?.progress?.coerceIn(0, 9) ?: 0
        super.render(mouseX, mouseY, partialTicks)

        val leftCenter = (width * 0.295f).toInt()
        val panelX = (width * 0.128f).toInt()
        val panelY = (height * 0.29f).toInt()
        val panelWidth = (width * 0.334f).toInt()
        val panelHeight = (height * 0.30f).toInt()
        rewardCards.forEach { it?.renderCard(mc, mouseX, mouseY) }

        drawCenteredString("Select 1 of the 3 rewards", leftCenter, (height * 0.225f).toInt(), 0xFFFFFF)
        drawCenteredString("Reward Options", (width * 0.706f).toInt(), (height * 0.225f).toInt(), 0xFFFFFF)

        data?.let { d ->
            drawCenteredString("Daily Streak", leftCenter, (height * 0.72f).toInt(), 0xFFFFFF)
            val streakText = "Current: ${d.streak.current}  Highest: ${d.streak.highest}"
            drawCenteredString(streakText, leftCenter, (height * 0.755f).toInt(), 0xFFFFFF)
        }

        for (index in 0 until 9) {
            val centerX = leftCenter - 4 * 21 + index * 21
            RewardClaimScreenCompanion.drawCircle(
                centerX.toFloat(),
                height * 0.805f,
                7.6f,
                if (index < streakProgress) 0xFF3C8527.toInt() else 0xFFAAAAAA.toInt()
            )
        }

        if (selected != -1 && data != null) {
            val reward = data!!.rewards[selected]
            val lang = data!!.language
            RewardClaimScreenCompanion.drawRect(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xE5262827.toInt())
            RewardClaimScreenCompanion.drawBorder(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF737A76.toInt())

            val iconSize = (height * 0.09f).toInt()
            reward.image?.let {
                RewardCard.drawImage(mc, it, panelX + 43f, panelY + 12f, iconSize.toFloat(), iconSize.toFloat())
            }
            val infoX = panelX + 109f
            val displayName = reward.getDisplayName(lang).replaceFirst(Regex("^§."), "§b")
            drawFittedString(displayName, infoX, panelY + 14f, panelX + panelWidth - 14f - infoX, 0xFFFFFF)
            RewardClaimScreenCompanion.drawHLine(
                infoX.toInt(), panelX + panelWidth - 8, panelY + 28, 0xFFBDBDBD.toInt()
            )

            val rarityText = "Rarity: ${reward.rarity.color}${lang.translate(reward.rarity.translationKey)}"
            drawString(rarityText, infoX, panelY + 32f, 0xFFFFFF)

            reward.amount?.let { amt ->
                drawString("Amount: §6$amt", infoX, panelY + 46f, 0xFFFFFF)
            }

            val desc = reward.getDescription(lang)
            val descriptionY = maxOf(panelHeight * 0.5f, 65f)
            wrapText(desc, panelWidth - 28).forEachIndexed { index, line ->
                drawString(line, panelX + 14f, panelY + descriptionY + index * 12f, 0xFF44E9EF.toInt())
            }
        }

        when (popupType) {
            PopupType.ERROR -> drawErrorPopup()
            PopupType.CONFIRM -> drawConfirmPopup()
            PopupType.AD_SKIP -> drawAdPopup(true)
            PopupType.AD_NO_SKIP -> drawAdPopup(false)
            else -> {}
        }
    }

    private fun drawErrorPopup() {
        val w = 200
        val h = 120
        val x = (width - w) / 2
        val y = (height - h) / 2

        RewardClaimScreenCompanion.drawRect(x, y, x + w, y + h, 0xCC000000.toInt())
        RewardClaimScreenCompanion.drawRect(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF333333.toInt())

        drawCenteredString("§cAn Error Occurred!", x + w / 2, y + 10, 0xFFFFFF)
        val errorMsg = data?.let { "Rewards were empty." } ?: "Error loading rewards."
        drawCenteredString(errorMsg, x + w / 2, y + 30, 0xFFFFFF)

        RewardClaimScreenCompanion.drawRect(x + w / 2 - 30, y + h - 40, x + w / 2 + 30, y + h - 20, 0xFF00D469.toInt())
        drawCenteredString("Close", x + w / 2, y + h - 35, 0xFFFFFF)
    }

    private fun drawConfirmPopup() {
        val w = 250
        val h = 100
        val x = (width - w) / 2
        val y = (height - h) / 2

        RewardClaimScreenCompanion.drawRect(x, y, x + w, y + h, 0xCC000000.toInt())
        RewardClaimScreenCompanion.drawRect(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF333333.toInt())

        drawCenteredString("Confirmation", x + w / 2, y + 10, 0xFFFFFF)
        drawCenteredString("Are you sure you want to claim this reward?", x + w / 2, y + 30, 0xFFFFFF)

        RewardClaimScreenCompanion.drawRect(x + w / 2 - 60, y + h - 40, x + w / 2 - 10, y + h - 20, 0xFF00D469.toInt())
        drawCenteredString("Back", x + w / 2 - 35, y + h - 35, 0xFFFFFF)

        RewardClaimScreenCompanion.drawRect(x + w / 2 + 10, y + h - 40, x + w / 2 + 60, y + h - 20, 0xFF00D469.toInt())
        drawCenteredString("Continue", x + w / 2 + 35, y + h - 35, 0xFFFFFF)
    }

    private fun drawAdPopup(skip: Boolean) {
        val w = adPopupWidth()
        val lines = wrapText("Hypixel is a great server and as such we don't want to remove their ad.", w - 20)
        val h = (if (skip) 100 else 80) + (lines.size - 1) * 12
        val x = (width - w) / 2
        val y = (height - h) / 2

        RewardClaimScreenCompanion.drawRect(x, y, x + w, y + h, 0xCC000000.toInt())
        RewardClaimScreenCompanion.drawRect(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF333333.toInt())

        drawCenteredString("Reward Claim AD", x + w / 2, y + 10, 0xFFFFFF)
        lines.forEachIndexed { index, line ->
            drawCenteredString(line, x + w / 2, y + 30 + index * 12, 0xFFFFFF)
        }

        RewardClaimScreenCompanion.drawRect(x + w / 2 - 60, y + h - 40, x + w / 2 - 10, y + h - 20, 0xFF00D469.toInt())
        drawCenteredString("Store", x + w / 2 - 35, y + h - 35, 0xFFFFFF)

        if (skip) {
            RewardClaimScreenCompanion.drawRect(x + w / 2 + 10, y + h - 40, x + w / 2 + 60, y + h - 20, 0xFF00D469.toInt())
            drawCenteredString("Skip", x + w / 2 + 35, y + h - 35, 0xFFFFFF)
        }
    }

    private fun adPopupWidth(): Int = minOf(420, width - 40).coerceAtLeast(1)

    private fun adPopupHeight(skip: Boolean, popupWidth: Int): Int {
        val lines = wrapText(
            "Hypixel is a great server and as such we don't want to remove their ad.",
            popupWidth - 20
        )
        return (if (skip) 100 else 80) + (lines.size - 1) * 12
    }

    private fun wrapText(text: String, maxWidth: Int): List<String> {
        val lines = mutableListOf<String>()
        var line = ""
        for (word in text.split(' ')) {
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (line.isNotEmpty() && textRenderer.getWidth(candidate) > maxWidth) {
                lines.add(line)
                line = word
            } else {
                line = candidate
            }
        }
        if (line.isNotEmpty()) lines.add(line)
        return lines
    }

    private fun drawCenteredString(text: String, x: Int, y: Int, color: Int) {
        val textWidth = textRenderer.getWidth(text)
        textRenderer.draw(text, (x - textWidth / 2).toFloat(), y.toFloat(), color, false)
    }

    private fun drawString(text: String, x: Float, y: Float, color: Int) {
        textRenderer.draw(text, x, y, color, false)
    }

    private fun drawFittedString(text: String, x: Float, y: Float, maxWidth: Float, color: Int) {
        val textWidth = textRenderer.getWidth(text)
        val scale = if (textWidth > maxWidth) maxWidth / textWidth else 1f
        GL11.glPushMatrix()
        GL11.glTranslatef(x, y, 0f)
        GL11.glScalef(scale, 1f, 1f)
        textRenderer.draw(text, 0f, 0f, color, false)
        GL11.glPopMatrix()
    }

    companion object RewardClaimScreenCompanion {
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

        internal fun drawVLine(x: Int, startY: Int, endY: Int, color: Int) {
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glColor4d(
                (color shr 16 and 0xFF) / 255.0,
                (color shr 8 and 0xFF) / 255.0,
                (color and 0xFF) / 255.0,
                (color shr 24 and 0xFF) / 255.0
            )
            GL11.glLineWidth(1f)
            GL11.glBegin(GL11.GL_LINES)
            GL11.glVertex2d(x.toDouble(), startY.toDouble())
            GL11.glVertex2d(x.toDouble(), endY.toDouble())
            GL11.glEnd()
            GL11.glEnable(GL11.GL_TEXTURE_2D)
        }

        internal fun drawBorder(left: Int, top: Int, right: Int, bottom: Int, color: Int) {
            RewardCard.drawBorder(left, top, right, bottom, color)
        }

        internal fun drawHLine(startX: Int, endX: Int, y: Int, color: Int) {
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glColor4d(
                (color shr 16 and 0xFF) / 255.0,
                (color shr 8 and 0xFF) / 255.0,
                (color and 0xFF) / 255.0,
                (color shr 24 and 0xFF) / 255.0
            )
            GL11.glLineWidth(1f)
            GL11.glBegin(GL11.GL_LINES)
            GL11.glVertex2d(startX.toDouble(), y.toDouble())
            GL11.glVertex2d(endX.toDouble(), y.toDouble())
            GL11.glEnd()
            GL11.glEnable(GL11.GL_TEXTURE_2D)
        }

        internal fun drawCircle(cx: Float, cy: Float, radius: Float, color: Int) {
            val top = kotlin.math.ceil(cy - radius - 0.5f).toInt()
            val bottom = kotlin.math.ceil(cy + radius - 0.5f).toInt()
            for (y in top until bottom) {
                val offsetY = y + 0.5f - cy
                val remaining = radius * radius - offsetY * offsetY
                if (remaining <= 0f) continue
                val halfWidth = kotlin.math.sqrt(remaining.toDouble()).toFloat()
                val left = kotlin.math.ceil(cx - halfWidth - 0.5f).toInt()
                val right = kotlin.math.ceil(cx + halfWidth - 0.5f).toInt()
                drawRect(
                    left,
                    y,
                    right,
                    y + 1,
                    color
                )
            }
        }
    }

    override fun buttonClicked(button: ButtonWidget) {
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        if (popupType != PopupType.NONE && mouseButton == 0) {
            val popupW = when (popupType) {
                PopupType.ERROR -> 200
                PopupType.CONFIRM -> 250
                PopupType.AD_SKIP, PopupType.AD_NO_SKIP -> adPopupWidth()
                else -> 0
            }
            val popupH = when (popupType) {
                PopupType.ERROR -> 120
                PopupType.CONFIRM -> 100
                PopupType.AD_SKIP -> adPopupHeight(true, popupW)
                PopupType.AD_NO_SKIP -> adPopupHeight(false, popupW)
                else -> 0
            }
            val x = (width - popupW) / 2
            val y = (height - popupH) / 2

            if (popupType == PopupType.ERROR && mouseX in x + popupW / 2 - 30 until x + popupW / 2 + 30 && mouseY in y + popupH - 40 until y + popupH - 20) {
                mc.openScreen(null)
                return
            }

            if (popupType == PopupType.CONFIRM) {
                if (mouseX in x + popupW / 2 - 60 until x + popupW / 2 - 10 && mouseY in y + popupH - 40 until y + popupH - 20) {
                    popupType = PopupType.NONE
                    return
                }
                if (mouseX in x + popupW / 2 + 10 until x + popupW / 2 + 60 && mouseY in y + popupH - 40 until y + popupH - 20) {
                    claimReward()
                    return
                }
            }

            if (popupType == PopupType.AD_SKIP || popupType == PopupType.AD_NO_SKIP) {
                if (mouseX in x + popupW / 2 - 60 until x + popupW / 2 - 10 && mouseY in y + popupH - 40 until y + popupH - 20) {
                    try {
                        Desktop.getDesktop().browse(URI("https://store.hypixel.net/"))
                    } catch (e: Exception) {
                        LOGGER.warn("Could not open the Hypixel store.", e)
                    }
                    return
                }
                if (popupType == PopupType.AD_SKIP && mouseX in x + popupW / 2 + 10 until x + popupW / 2 + 60 && mouseY in y + popupH - 40 until y + popupH - 20) {
                    popupType = PopupType.NONE
                    return
                }
            }
        }

        if (state == GuiState.SUCCESSFUL && mouseButton == 0) {
            for (i in rewardCards.indices) {
                val card = rewardCards[i] ?: continue
                if (card.containsPoint(mouseX, mouseY)) {
                    val now = System.currentTimeMillis()
                    val isDoubleClick = i == lastClickedReward && now - lastClickTime < 500
                    selected = i
                    rewardCards.forEachIndexed { index, rewardCard ->
                        rewardCard?.setSelected(index == i)
                    }

                    if (isDoubleClick) {
                        if (Config.showConfirmation) {
                            popupType = PopupType.CONFIRM
                        } else {
                            claimReward()
                        }
                    }
                    lastClickedReward = i
                    lastClickTime = now
                    return
                }
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    private fun claimReward() {
        Threading.runAsync {
            try {
                val d = data ?: return@runAsync
                val url = URI("https://rewards.hypixel.net/claim-reward/claim?option=$selected&id=$id&activeAd=${d.activeAd}&_csrf=${d.securityToken}&watchedFallback=false").toURL()
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.useCaches = true
                conn.addRequestProperty("User-Agent", RemoteData.userAgent)
                conn.readTimeout = 15000
                conn.connectTimeout = 15000
                conn.responseCode
                CookieHandler.setDefault(null)
                mc.executeTask(Runnable {
                    mc.openScreen(null)
                })
            } catch (e: Exception) {
                LOGGER.warn("Could not claim the selected reward.", e)
            }
        }
    }

    override fun keyPressed(charCode: Char, keyCode: Int) {
        if (keyCode == 1) {
            mc.openScreen(null)
        }
    }

    override fun removed() {

    }
}
