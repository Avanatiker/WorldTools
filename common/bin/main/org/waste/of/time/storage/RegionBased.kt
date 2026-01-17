package org.waste.of.time.storage

import net.minecraft.nbt.NbtCompound
import net.minecraft.util.WorldSavePath
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.World
import net.minecraft.world.level.storage.LevelStorage
import org.waste.of.time.WorldTools.LOG

abstract class RegionBased(
    val chunkPos: ChunkPos,
    val world: World,
    private val suffix: String
) : Storeable() {
    val dimension: String = world.registryKey.value.path

    private val dimensionPath
        get() = when (dimension) {
            "overworld" -> ""
            "the_nether" -> "DIM-1/"
            "the_end" -> "DIM1/"
            else -> "dimensions/minecraft/$dimension/"
        }

    abstract fun compound(): NbtCompound

    abstract fun incrementStats()

    // can be overridden but super should be called after
    open fun writeToStorage(
        session: LevelStorage.Session,
        storage: CustomRegionBasedStorage,
        cachedStorages: MutableMap<String, CustomRegionBasedStorage>
    ) {
        try {
            storage.write(
                chunkPos,
                compound()
            )
            incrementStats()
        } catch (e: Exception) {
            LOG.error("Failed to store $this", e)
        }
    }

    override fun store(
        session: LevelStorage.Session,
        cachedStorages: MutableMap<String, CustomRegionBasedStorage>
    ) {
        if (!shouldStore()) return
        val storage = generateStorage(session, cachedStorages)
        writeToStorage(session, storage, cachedStorages)
    }

    fun generateStorage(
        session: LevelStorage.Session,
        cachedStorages: MutableMap<String, CustomRegionBasedStorage>
    ): CustomRegionBasedStorage {
        val path = session.getDirectory(WorldSavePath.ROOT)
            .resolve(dimensionPath)
            .resolve(suffix)
        return cachedStorages.getOrPut(path.toString()) {
            CustomRegionBasedStorage(path, false)
        }
    }
}
