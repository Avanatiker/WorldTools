package org.waste.of.time

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.scanner.NbtScanner
import net.minecraft.util.PathUtil
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.storage.RegionFile
import java.io.DataOutput
import java.io.IOException
import java.nio.file.Path


open class CustomRegionBasedStorage internal constructor(
    private val directory: Path,
    private val dsync: Boolean
) : AutoCloseable {
    private val cachedRegionFiles: Long2ObjectLinkedOpenHashMap<RegionFile?> = Long2ObjectLinkedOpenHashMap()

    @Throws(IOException::class)
    fun getRegionFile(pos: ChunkPos): RegionFile {
        val longPos = ChunkPos.toLong(pos.regionX, pos.regionZ)
        cachedRegionFiles.getAndMoveToFirst(longPos)?.let { return it }

        if (cachedRegionFiles.size >= 256) {
            cachedRegionFiles.removeLast()?.close()
        }

        PathUtil.createDirectories(directory)
        val path = directory.resolve("r." + pos.regionX + "." + pos.regionZ + MCA_EXTENSION)
        val regionFile = RegionFile(path, directory, dsync)
        cachedRegionFiles.putAndMoveToFirst(longPos, regionFile)
        return regionFile
    }

    @Throws(IOException::class)
    fun getTagAt(pos: ChunkPos): NbtCompound? {
        getRegionFile(pos).getChunkInputStream(pos).use { dataInputStream ->
            return if (dataInputStream == null) {
                null
            } else NbtIo.read(dataInputStream)
        }
    }

    @Throws(IOException::class)
    fun scanChunk(chunkPos: ChunkPos, scanner: NbtScanner?) {
        getRegionFile(chunkPos).getChunkInputStream(chunkPos).use { dataInputStream ->
            dataInputStream?.let {
                NbtIo.scan(it, scanner)
            }
        }
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

    @Throws(IOException::class)
    override fun close() {
        // ToDo: find out why the closing causes a crash
//        val throwableDeliverer = ThrowableDeliverer<IOException>()
//
//        cachedRegionFiles.values.filterNotNull().forEach { regionFile ->
//            try {
//                regionFile.close()
//            } catch (iOException: IOException) {
//                throwableDeliverer.add(iOException)
//            }
//        }
//
//        throwableDeliverer.deliver()
    }

    @Throws(IOException::class)
    fun sync() {
        cachedRegionFiles.values.filterNotNull().forEach { it.sync() }
    }

    companion object {
        const val MCA_EXTENSION = ".mca"
    }
}
