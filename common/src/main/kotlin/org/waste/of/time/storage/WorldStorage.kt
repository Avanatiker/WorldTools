package org.waste.of.time.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.nio.file.Files
import java.nio.file.Path

class WorldStorage {
    val path: Path
    val dimensionPaths: List<String>
    constructor(path: Path) {
        this.path = path
        // todo: handle custom dimensions
        this.dimensionPaths = listOf("", "DIM-1/", "DIM1/")
            .filter { path.resolve(it).toFile().exists() }
    }

    fun getRegionStorage(dimensionPath: String): CustomRegionBasedStorage {
        return CustomRegionBasedStorage(path.resolve(dimensionPath).resolve("region"), false)
    }

    fun getEntityStorage(dimensionPath: String): CustomRegionBasedStorage {
        return CustomRegionBasedStorage(path.resolve(dimensionPath).resolve("entities"), false)
    }

    fun getWorldDataStoragePath(): Path {
        return path.resolve("data")
    }

    fun getPlayerDataStoragePath(): Path {
        return path.resolve("playerdata")
    }

    fun getLevelDatPath(): Path {
        return path.resolve("level.dat")
    }

    fun worldDataStorageFlow(): Flow<Path> = flow<Path> {
        Files.newDirectoryStream(getWorldDataStoragePath()).use { stream ->
            stream.forEach { path ->
                emit(path)
            }
        }
    }.flowOn(Dispatchers.IO)

    fun playerDataStorageFlow(): Flow<Path> = flow<Path> {
        Files.newDirectoryStream(getPlayerDataStoragePath()).use { stream ->
            stream.forEach { path ->
                emit(path)
            }
        }
    }.flowOn(Dispatchers.IO)
}
