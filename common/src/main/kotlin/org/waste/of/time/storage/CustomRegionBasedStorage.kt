package org.waste.of.time.storage

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap
import net.minecraft.block.entity.BlockEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIo
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import java.nio.file.Files
import net.minecraft.util.ThrowableDeliverer
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.storage.RegionFile
import net.minecraft.world.storage.StorageKey
import net.minecraft.world.World
import org.waste.of.time.WorldTools.MCA_EXTENSION
import org.waste.of.time.WorldTools.MOD_NAME
import org.waste.of.time.WorldTools.mc
import java.io.DataOutput
import java.io.IOException
import java.nio.file.Path


open class CustomRegionBasedStorage internal constructor(
    private val directory: Path,
    private val dsync: Boolean
) : AutoCloseable {
    private val cachedRegionFiles: Long2ObjectLinkedOpenHashMap<RegionFile?> = Long2ObjectLinkedOpenHashMap()

    companion object {
        val defaultStorageKey: StorageKey = StorageKey(MOD_NAME, World.OVERWORLD, "chunk")
    }

    @Throws(IOException::class)
    fun getRegionFile(pos: ChunkPos): RegionFile {
        val longPos = ChunkPos.toLong(pos.regionX, pos.regionZ)
        cachedRegionFiles.getAndMoveToFirst(longPos)?.let { return it }

        if (cachedRegionFiles.size >= 256) {
            cachedRegionFiles.removeLast()?.close()
        }

        Files.createDirectories(directory)
        val path = directory.resolve("r." + pos.regionX + "." + pos.regionZ + MCA_EXTENSION)
        val regionFile = RegionFile(defaultStorageKey, path, directory, dsync)
        cachedRegionFiles.putAndMoveToFirst(longPos, regionFile)
        return regionFile
    }

    @Throws(IOException::class)
    fun write(pos: ChunkPos, nbt: NbtCompound?) {
        val regionFile = getRegionFile(pos)
        if (nbt == null) {
            regionFile.delete(pos)
        } else {
            regionFile.getChunkOutputStream(pos).use { dataOutputStream ->
                NbtIo.write(nbt, dataOutputStream as DataOutput)
            }
        }
    }

    private fun getNbtAt(chunkPos: ChunkPos) =
        getRegionFile(chunkPos).getChunkInputStream(chunkPos)?.use { dataInputStream ->
            NbtIo.readCompound(dataInputStream)
        }

    fun getBlockEntities(chunkPos: ChunkPos): List<BlockEntity> {
        val nbt = getNbtAt(chunkPos) ?: return emptyList()
        val list = nbt.getList("block_entities").orElse(null) ?: return emptyList()
        val world = mc.world ?: return emptyList()
        val result = mutableListOf<BlockEntity>()
        for (i in 0 until list.size) {
            val element = list.get(i)
            val compoundTag = (element as? NbtCompound) ?: continue
            val blockPos = BlockPos(
                compoundTag.getInt("x").orElse(0),
                compoundTag.getInt("y").orElse(0),
                compoundTag.getInt("z").orElse(0)
            )
            val blockEntityId = Identifier.of(compoundTag.getString("id").orElse(""))
            runCatching {
                val block = Registries.BLOCK.get(blockEntityId)
                BlockEntity.createFromNbt(blockPos, block.defaultState, compoundTag, world.registryManager)
            }.getOrNull()?.let { result.add(it) }
        }
        return result
    }

    @Throws(IOException::class)
    override fun close() {
        val throwableDeliverer = ThrowableDeliverer<IOException>()

        cachedRegionFiles.values.filterNotNull().forEach { regionFile ->
            try {
                regionFile.close()
            } catch (iOException: IOException) {
                throwableDeliverer.add(iOException)
            }
        }

        throwableDeliverer.deliver()
    }
}
