package tech.thatgravyboat.rewardclaim

import com.teamresourceful.resourcefullib.common.utils.Scheduling
import earth.terrarium.olympus.client.components.compound.LayoutWidget
import earth.terrarium.olympus.client.pipelines.RoundedRectangle
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

val mc: Minecraft get() = Minecraft.getInstance()

object Threading {
    private val executor = Executors.newSingleThreadExecutor {
        val thread = Thread(it, "Reward Claim")
        thread.isDaemon = true
        thread
    }

    fun <T> supply(block: () -> T): CompletableFuture<T> {
        return CompletableFuture.supplyAsync(block, executor)
    }

    fun run(block: () -> Unit): CompletableFuture<Void> {
        return CompletableFuture.runAsync(block, executor)
    }

    fun schedule(duration: Duration, block: () -> Unit) {
        Scheduling.schedule({
            mc.schedule { block() }
        }, duration.inWholeMilliseconds, TimeUnit.MILLISECONDS)
    }
}

object Utils {

    inline fun GuiGraphicsExtractor.translated(x: Number, y: Number, block: () -> Unit) {
        this.pose().pushMatrix()
        this.pose().translate(x.toFloat(), y.toFloat())
        block()
        this.pose().popMatrix()
    }

    fun GuiGraphicsExtractor.drawRoundedRec(
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