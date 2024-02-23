package org.waste.of.time.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import net.minecraft.world.storage.RegionFile
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isDirectory

class WorldStorage(val path: Path) {
    private val dimensionPaths: List<String>

    init {
        val dimRegex = Regex("^(DIM-?[0-9]+)$")
        Files.newDirectoryStream(path).use { stream ->
            val dims = stream
                .filter { it.exists() && it.isDirectory() }
                .map { it.fileName.toString() }
                .filter { dimRegex.matches(it) }
                .toMutableList()
                .also { it.add("") } // base case for top level dir (overworld)
            this.dimensionPaths = dims
        }
    }

    // flow on all dimensions
    fun regionStorageRegionFileFlow(): Flow<RegionFile> = flow {
        dimensionPaths.forEach { dimensionPath ->
            getRegionStorage(dimensionPath).regionFileFlow().collect { emit(it) }
        }
    }.flowOn(Dispatchers.IO)

    private fun getRegionStorage(dimensionPath: String): CustomRegionBasedStorage =
        CustomRegionBasedStorage(path.resolve(dimensionPath).resolve("region"), false)

    // flow on all dimensions
    fun entityStorageRegionFileFlow(): Flow<RegionFile> = flow {
        dimensionPaths.forEach { dimensionPath ->
            getEntityStorage(dimensionPath).regionFileFlow().collect { emit(it) }
        }
    }.flowOn(Dispatchers.IO)

    private fun getEntityStorage(dimensionPath: String): CustomRegionBasedStorage =
        CustomRegionBasedStorage(path.resolve(dimensionPath).resolve("entities"), false)

    fun getLevelDatPath(): Path = path.resolve("level.dat")

    fun worldDataStorageFlow(): Flow<Path> = flow<Path> {
        Files.newDirectoryStream(path.resolve("data")).use { stream ->
            stream
                .filter { it.fileName.extension == "dat" }
                .forEach { emit(it) }
        }
    }.flowOn(Dispatchers.IO)

    fun playerDataStorageFlow(): Flow<Path> = flow<Path> {
        Files.newDirectoryStream(path.resolve("playerdata")).use { stream ->
            stream
                .filter { it.fileName.extension == "dat" }
                .forEach { emit(it) }
        }
    }.flowOn(Dispatchers.IO)
}
