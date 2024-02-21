package org.waste.of.time.maps

import com.ibm.icu.impl.locale.XCldrStub.FileUtilities.UTF8
import org.waste.of.time.storage.WorldStorage
import java.nio.file.Files
import kotlin.io.path.isRegularFile
import kotlin.io.path.notExists

object MapRemapSerializer {

    // Serialize the maps we've found
    fun serialize(ctx: MapScanContext) {
        val uniqueMapIds = ctx.foundMaps
            .map { it.mapId }
            .distinct()
        val outputFilePath = ctx.storage.path.resolve("map_ids.txt")
//        PathUtil.createDirectories(outputFilePath)
        Files.newOutputStream(outputFilePath).bufferedWriter(UTF8).use { writer ->
            uniqueMapIds.forEach {
                writer.write(it)
                writer.newLine()
            }
        }
    }

    // deserialize the remaps for this world
    fun deserializeRemaps(worldStorage: WorldStorage): Map<Int, Int> {
        val inputFile = worldStorage.path.resolve("map_remaps.txt")
        if (inputFile.notExists() || !inputFile.isRegularFile()) throw RuntimeException("map_remaps.txt not found")
        val map = mutableMapOf<Int, Int>()
        Files.newInputStream(inputFile).bufferedReader(UTF8).use { reader ->
            reader.forEachLine { line ->
                if (!line.contains(":")) return@forEachLine
                val split = line.split(":")
                map[split.first().toInt()] = split.last().toInt()
            }
        }
        return map
    }
}
