package org.waste.of.time.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isDirectory

class WorldStorage(val path: Path) {
    val dimensionPaths: List<String>

    init {
        val dimRegex = Regex("^(DIM-?[0-9]+)$")
        Files.newDirectoryStream(path).use { stream ->
            val dims = stream
                .filter {
                    it.exists() && it.isDirectory()
                }
                .map { it.fileName.toString() }
                .filter { dimRegex.matches(it) }
                .toMutableList()
            dims.add("") // base case for top level dir (overworld)
            this.dimensionPaths = dims
        }
    }

    fun getRegionStorage(dimensionPath: String): CustomRegionBasedStorage {
        return CustomRegionBasedStorage(path.resolve(dimensionPath).resolve("region"), false)
    }

    fun getEntityStorage(dimensionPath: String): CustomRegionBasedStorage {
        return CustomRegionBasedStorage(path.resolve(dimensionPath).resolve("entities"), false)
    }

    private fun getWorldDataStoragePath(): Path {
        return path.resolve("data")
    }

    private fun getPlayerDataStoragePath(): Path {
        return path.resolve("playerdata")
    }

    fun getLevelDatPath(): Path {
        return path.resolve("level.dat")
    }

    fun worldDataStorageFlow(): Flow<Path> = flow<Path> {
        Files.newDirectoryStream(getWorldDataStoragePath()).use { stream ->
            stream
                .filter { it.fileName.extension == "dat" }
                .forEach { emit(it) }
        }
    }.flowOn(Dispatchers.IO)

    fun playerDataStorageFlow(): Flow<Path> = flow<Path> {
        Files.newDirectoryStream(getPlayerDataStoragePath()).use { stream ->
            stream
                .filter { it.fileName.extension == "dat" }
                .forEach { emit(it) }
        }
    }.flowOn(Dispatchers.IO)
}
