package tech.thatgravyboat.rewardclaim

import com.teamresourceful.resourcefullib.common.utils.Scheduling
import earth.terrarium.olympus.client.components.compound.LayoutWidget
import earth.terrarium.olympus.client.pipelines.RoundedRectangle
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

internal val IO_EXECUTOR = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors().coerceAtLeast(1) * 2) { runnable ->
    Thread(runnable, "RewardClaim-IO").apply { isDaemon = true }
}

val mc: Minecraft get() = Minecraft.getInstance()

fun schedule(duration: Duration, block: () -> Unit) {
    Scheduling.schedule({
        mc.schedule { block() }
    }, duration.inWholeMilliseconds, TimeUnit.MILLISECONDS)
}

object Utils {

    inline fun GuiGraphics.translated(x: Number, y: Number, block: () -> Unit) {
        this.pose().pushMatrix()
        this.pose().translate(x.toFloat(), y.toFloat())
        block()
        this.pose().popMatrix()
    }

    fun GuiGraphics.drawRoundedRec(
        x: Int, y: Int, width: Int, height: Int,
        backgroundColor: Int, borderColor: Int = backgroundColor,
        borderSize: Int = 0, radius: Int = 0,
    ) {
        RoundedRectangle.draw(
            this@drawRoundedRec, x, y, width, height,
            backgroundColor, borderColor, width.coerceAtMost(height) * (radius / 100f), borderSize,
        )
    }

    fun LayoutWidget<*>.withVerticalLayout(margin: Int = 0, gap: Int = 0) {
        this.withContentMargin(margin)
        this.withLayoutCallback { widget, layout ->
            var y = widget.y
            layout.visitWidgets {
                it.y = y
                it.x = widget.x + margin
                y += it.height + gap
            }
        }
    }
}