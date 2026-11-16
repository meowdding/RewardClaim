package tech.thatgravyboat.rewardclaim

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

object Threading {

    private val counter = AtomicInteger(0)

    private val scheduledPool: ScheduledExecutorService = Executors.newScheduledThreadPool(10) { target: Runnable? ->
        Thread(target, "RewardClaim Thread " + counter.incrementAndGet())
    }

    private val POOL = ThreadPoolExecutor(
        10, 30,
        0L, TimeUnit.SECONDS,
        LinkedBlockingQueue()
    ) { target: Runnable? -> Thread(target, "RewardClaim Async " + counter.incrementAndGet()) }

    @JvmStatic
    fun runAsync(runnable: () -> Unit) {
        POOL.execute(runnable)
    }

    fun schedule(r: Runnable, delay: Long, unit: TimeUnit): ScheduledFuture<*> {
        return scheduledPool.schedule(r, delay, unit)
    }

    inline fun supply(crossinline block: () -> Any): CompletableFuture<Any> {
        val future = CompletableFuture<Any>()
        runAsync {
            try {
                future.complete(block())
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }
        return future
    }

    inline fun run(crossinline block: () -> Unit): CompletableFuture<Void> {
        val future = CompletableFuture<Void>()
        runAsync {
            try {
                block()
                future.complete(null)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }
        return future
    }
}
