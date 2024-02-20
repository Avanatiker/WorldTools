package org.waste.of.time

import net.minecraft.nbt.NbtCompound
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.storage.RegionFile
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

object Utils {
    // Why cant I use the std lib?
    fun Boolean.toByte(): Byte = if (this) 1 else 0
    fun Vec3d.asString() = "(%.2f, %.2f, %.2f)".format(x, y, z)
    fun NbtCompound.addAuthor() = apply { putString("Author", WorldTools.CREDIT_MESSAGE) }

    fun getTime(): String {
        val localDateTime = LocalDateTime.now()
        val zoneId = ZoneId.systemDefault()

        val zonedDateTime = ZonedDateTime.of(localDateTime, zoneId)
        val formatter = DateTimeFormatter.RFC_1123_DATE_TIME

        return zonedDateTime.format(formatter)
    }

    fun Vec3d.manhattanDistance(other: Vec3d): Double {
        return abs(this.x - other.x) + abs(this.y - other.y) + abs(this.z - other.z)
    }

    fun Vec3d.manhattanDistance2d(other: Vec3d): Double {
        return abs(this.x - other.x) + abs(this.z - other.z)
    }

    // All possible "relative" ChunkPos positions in a RegionFile
    private val possibleChunkPosList: List<ChunkPos> by lazy { genPossibleChunkPosList() }
    private fun genPossibleChunkPosList(): List<ChunkPos> {
        val list = mutableListOf<ChunkPos>()
        for (x in 0 until 32) {
            for (z in 0 until 32) {
                list.add(ChunkPos(x, z))
            }
        }
        return list
    }
    fun RegionFile.chunkPosList(): List<ChunkPos> {
        val list = mutableListOf<ChunkPos>()
        for (chunkPos in possibleChunkPosList) {
            if (hasChunk(chunkPos)) {
                list.add(chunkPos)
            }
        }
        return list
    }
}
