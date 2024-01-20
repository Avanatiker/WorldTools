package org.waste.of.time.storage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import net.minecraft.util.path.SymlinkValidationException
import org.waste.of.time.WorldTools.LOG
import org.waste.of.time.WorldTools.mc
import org.waste.of.time.manager.MessageManager
import org.waste.of.time.manager.MessageManager.sendInfo
import org.waste.of.time.manager.StatisticManager
import org.waste.of.time.storage.serializable.EndFlow
import java.io.IOException
import java.util.concurrent.CancellationException
import kotlin.time.Duration
import kotlin.time.measureTime

object StorageFlow {
    private const val MAX_BUFFER_SIZE = 1000
    var lastStoredTimestamp: Long = 0
    var lastStored: Storeable? = null
    var lastStoredTimeNeeded: Duration = Duration.ZERO

    private val sharedFlow = MutableSharedFlow<Storeable>(
        extraBufferCapacity = MAX_BUFFER_SIZE,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    fun emit(storeable: Storeable) {
        sharedFlow.tryEmit(storeable)
    }

    fun launch(levelName: String) = CoroutineScope(Dispatchers.IO).launch {
        StatisticManager.reset()
        val cachedStorages = mutableMapOf<String, CustomRegionBasedStorage>()

        try {
            LOG.info("Started caching")
            mc.levelStorage.createSession(levelName).use { openSession ->
                sharedFlow.collect { storeable ->
                    if (!storeable.shouldStore()) {
                        return@collect
                    }

                    val time = measureTime {
                        storeable.store(openSession, cachedStorages)
                    }

                    lastStored = storeable
                    lastStoredTimestamp = System.currentTimeMillis()
                    lastStoredTimeNeeded = time

                    if (storeable is EndFlow) {
                        throw StopCollectingException()
                    }
                }
            }
        } catch (e: StopCollectingException) {
            LOG.info("Canceled caching flow")
        } catch (e: IOException) {
            LOG.error("IOException: Failed to create session for $levelName", e)
            MessageManager.sendError("worldtools.log.error.failed_to_create_session", levelName, e.localizedMessage)
        } catch (e: SymlinkValidationException) {
            LOG.error("SymlinkValidationException: Failed to create session for $levelName", e)
            MessageManager.sendError("worldtools.log.error.failed_to_create_session", levelName, e.localizedMessage)
        } catch (e: CancellationException) {
            LOG.info("Canceled caching thread")
        }

        cachedStorages.values.forEach { it.close() }
        LOG.info("Finished caching")
    }

    class StopCollectingException : Exception()
}
