package org.waste.of.time.maps

import com.google.common.io.Files
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtList
import net.minecraft.util.math.Vec3d
import net.minecraft.world.storage.RegionFile
import org.waste.of.time.Utils.chunkPosList
import org.waste.of.time.WorldTools.LOG
import org.waste.of.time.WorldTools.mc
import org.waste.of.time.manager.MessageManager
import org.waste.of.time.storage.WorldStorage
import java.nio.file.Path

object MapsRemapper {

    fun remapMaps(worldName: String, remap: Map<Int, Int>, force: Boolean = false) {
        val worldDirectoryPath = mc.levelStorage.savesDirectory.resolve(worldName)
        if (!verifyWorld(worldName, worldDirectoryPath)) return
        MessageManager.sendInfo("Remapping maps in $worldName")
        CoroutineScope(Dispatchers.IO).launch(mapScanErrorHandler) {
            val storage = WorldStorage(worldDirectoryPath)
            if (!force && !verifyRemap(worldName, storage, remap)) return@launch
            val ctx = MapScanContext(worldName, storage, remap)
            scanMaps(ctx)
            val remapCount = ctx.foundMaps.count { ctx.remap.containsKey(it.mapId) }
            MessageManager.sendInfo("Remapped $remapCount maps")
        }
    }

    fun remapMapsWithRemapFile(worldName: String) {
        val worldDirectoryPath = mc.levelStorage.savesDirectory.resolve(worldName)
        if (!verifyWorld(worldName, worldDirectoryPath)) return
        val storage = WorldStorage(worldDirectoryPath)
        val remap = MapRemapSerializer.deserializeRemaps(storage)
        MessageManager.sendInfo("Loaded ${remap.size} remaps from disk")
        remapMaps(worldName, remap)
    }

    fun findMaps(worldName: String) {
        val worldDirectoryPath = mc.levelStorage.savesDirectory.resolve(worldName)
        if (!verifyWorld(worldName, worldDirectoryPath)) return
        MessageManager.sendInfo("Finding maps in $worldName")
        val ctx = MapScanContext(worldName, WorldStorage(worldDirectoryPath))
        CoroutineScope(Dispatchers.IO).launch(mapScanErrorHandler) {
            scanMaps(ctx)
            val foundMaps = ctx.foundMaps
            val uniqueMaps = foundMaps
                .map { it.mapId }
                .distinct()
                .sorted()
            val uniqueMapCount = uniqueMaps
                .count()
            MessageManager.sendInfo("Found $uniqueMapCount map ID's: $uniqueMaps")
//            foundMaps.groupingBy { it.source.src }.eachCount().forEach {
//                MessageManager.sendInfo("Count: [${it.key}] ${it.value}")
//            }
            foundMaps.groupingBy { it.source.src }.aggregate { key, accumulator: StringBuilder?, element, first ->
                if (first)
                    StringBuilder().append("[$key] ").append(element.mapId)
                else
                    accumulator!!.append(", ${element.mapId}")
            }.forEach { (_, maps) ->
                MessageManager.sendInfo("$maps")
            }
            MapRemapSerializer.serialize(ctx)
        }
    }

    private val mapScanErrorHandler = CoroutineExceptionHandler { _, exception ->
        MessageManager.sendError("Error during map scan: $exception")
        LOG.error("Error during map scan", exception)
    }

    private fun verifyWorld(worldName: String, worldDirectoryPath: Path): Boolean {
        if (!worldDirectoryPath.toFile().exists()) {
            LOG.error("World $worldName does not exist")
            MessageManager.sendInfo("World $worldName does not exist")
            return false
        }
        if (isWorldCurrentlyOpen(worldName)) {
            MessageManager.sendError("World: $worldName is currently open")
            return false
        }
        return true
    }

    private suspend fun verifyRemap(worldName: String, storage: WorldStorage, remap: Map<Int, Int>): Boolean {
        if (remap.values.distinct().size != remap.size) {
            MessageManager.sendError("Duplicate remap ID")
            return false
        }

        //  we have to find existing maps to determine if remap is valid T.T
        val ctx = MapScanContext(worldName, storage)
        scanMaps(ctx)

        // verify the existing map ID's actually exist
        val notFoundExistingIds = remap.keys.filter { ctx.foundMaps.all { foundMap -> foundMap.mapId != it } }

        if (notFoundExistingIds.isNotEmpty()) {
            MessageManager.sendError("Map ID's not found: $notFoundExistingIds")
            return false
        }
        // verify there are no maps with new ID's
        val newIdsFound = remap.values.filter { ctx.foundMaps.any { foundMap -> foundMap.mapId == it } }
        if (newIdsFound.isNotEmpty()) {
            MessageManager.sendError("Map ID's already exist: $newIdsFound")
            return false
        }
        return true
    }

    private suspend fun scanMaps(ctx: MapScanContext) {
        scanMapsFromWorldData(ctx)
        ctx.storage.regionStorageRegionFileFlow().collect { regionFile ->
            regionFile.use {
                scanMapsInContainers(it, ctx)
            }
        }
        ctx.storage.entityStorageRegionFileFlow().collect { regionFile ->
            regionFile.use {
                scanMapsInEntityRegion(it, ctx)
            }
        }
        ctx.storage.playerDataStorageFlow().collect {
            scanPlayerDataMaps(it, ctx)
        }
        scanLevelDatMaps(ctx)
    }

    private fun isWorldCurrentlyOpen(worldName: String): Boolean {
        return mc.isInSingleplayer && mc.server?.saveProperties?.levelName.equals(worldName)
    }

    private fun scanLevelDatMaps(ctx: MapScanContext) {
        val levelDatPath = ctx.storage.getLevelDatPath()
        val levelDatNbt = NbtIo.readCompressed(levelDatPath.toFile()) ?: return
        val playerDataNbt = levelDatNbt.getCompound("Data").getCompound("Player")
        if (searchPlayerDataMapsBase(playerDataNbt, MapSourceId.LEVEL_DAT, ctx)) {
            NbtIo.writeCompressed(levelDatNbt, levelDatPath.toFile())
        }
    }

    private fun scanPlayerDataMaps(nbtPath: Path, ctx: MapScanContext) {
        val nbt = NbtIo.readCompressed(nbtPath.toFile()) ?: return
        if (searchPlayerDataMapsBase(nbt, MapSourceId.PLAYER_DATA, ctx)) {
            NbtIo.writeCompressed(nbt, nbtPath.toFile())
        }
    }

    private fun searchPlayerDataMapsBase(nbt: NbtCompound, mapSourceId: MapSourceId, ctx: MapScanContext): Boolean {
        return scanInventory(nbt.getList("Inventory", 10), MapSource(mapSourceId, "Inventory"), ctx) or
                scanInventory(nbt.getList("EnderItems", 10), MapSource(mapSourceId, "Ender Chest"), ctx)
    }

    private suspend fun scanMapsFromWorldData(ctx: MapScanContext) {
        ctx.storage.worldDataStorageFlow().collect {
            val fileName = it.fileName.toString()
            if (fileName.startsWith("map_") && fileName.endsWith(".dat")) {
                val mapId = fileName.substring( // map_123.dat -> 123
                    fileName.indexOfFirst { c -> c == '_' } + 1,
                    fileName.indexOfLast { c -> c == '.' })
                    .toInt()
                ctx.foundMaps.add(FoundMap(mapId, MapSource(MapSourceId.WORLD_DATA)))
                if (ctx.remap.containsKey(mapId)) {
                    Files.move(it.toFile(), it.resolveSibling("map_${ctx.remap[mapId]!!}.dat").toFile())
                }
            }
        }
    }

    private fun scanMapsInEntityRegion(regionFile: RegionFile, ctx: MapScanContext) {
        regionFile.chunkPosList().forEach { chunkPos ->
            var dirty = false
            var chunkNbt: NbtCompound?
            regionFile.getChunkInputStream(chunkPos).use {
                val nbt = NbtIo.read(it)
                chunkNbt = nbt
                val entities = nbt.getList("Entities", 10)
                entities.forEach { entity ->
                    val entityNbt = entity as NbtCompound
                    val id = entityNbt.getString("id")
                    val posNbt = entityNbt.getList("Pos", 6)
                    val pos = Vec3d(
                        posNbt.getDouble(0),
                        posNbt.getDouble(1),
                        posNbt.getDouble(2)
                    )
                    dirty = dirty or
                            scanEntityNbtForMapsInItemFrames(entityNbt, id, pos, ctx) or
                            scanEntityNbtForMapsInItemEntities(entityNbt, id, pos, ctx) or
                            scanEntityNbtForMapsInEntityInventories(entityNbt, id, pos, ctx)
                }
            }
            if (dirty) {
                chunkNbt?.let { nbt ->
                    regionFile.getChunkOutputStream(chunkPos).use { chunkOutputStream ->
                        NbtIo.write(nbt, chunkOutputStream)
                    }
                }
            }
        }
    }

    private fun scanEntityNbtForMapsInItemFrames(
        entityNbt: NbtCompound,
        id: String,
        pos: Vec3d,
        ctx: MapScanContext
    ) : Boolean {
        if ("minecraft:item_frame" != id) return false
        return scanItemNbtForMap(entityNbt.getCompound("Item"), MapSource(MapSourceId.ITEM_FRAME, id, pos), ctx)
    }

    private fun scanItemNbtForMap(nbt: NbtCompound, mapSource: MapSource, ctx: MapScanContext): Boolean {
        if ("minecraft:filled_map" != nbt.getString("id")) return false
        val mapTag = nbt.getCompound("tag")
        val mapId = mapTag.getInt("map")
        ctx.foundMaps.add(FoundMap(mapId, mapSource))
        if (ctx.remap.containsKey(mapId)) {
            mapTag.putInt("map", ctx.remap[mapId]!!)
            return true
        }
        return false
    }

    private fun scanEntityNbtForMapsInItemEntities(
        entityNbt: NbtCompound,
        id: String,
        pos: Vec3d,
        ctx: MapScanContext
    ) : Boolean {
        if ("minecraft:item" != id) return false
        return scanItemNbtForMap(entityNbt.getCompound("Item"), MapSource(MapSourceId.ITEM_ENTITY, id, pos), ctx)
    }

    private fun scanEntityNbtForMapsInEntityInventories(
        entityNbt: NbtCompound,
        id: String,
        pos: Vec3d,
        ctx: MapScanContext
    ) : Boolean {
        return scanInventory(
            // todo: there may be other nbt tags to search on different entity types
            entityNbt.getList("Inventory", 10), // entities that implement InventoryOwner
            MapSource(MapSourceId.ENTITY_INVENTORY, id, pos),
            ctx
        )
    }

    private fun scanInventory(
        inventoryTagList: NbtList,
        mapSource: MapSource,
        ctx: MapScanContext
    ) : Boolean {
        var dirty = false
        inventoryTagList.forEach { inventorySlotTag ->
            dirty = dirty or scanItemNbtForMap(inventorySlotTag as NbtCompound, mapSource, ctx)
        }
        return dirty
    }

    private fun scanMapsInContainers(regionFile: RegionFile, ctx: MapScanContext) {
        regionFile.chunkPosList().forEach { chunkPos ->
            var dirty = false
            var chunkNbt: NbtCompound?
            regionFile.getChunkInputStream(chunkPos).use {
                val nbt = NbtIo.read(it)
                chunkNbt = nbt
                val blockEntities = nbt.getList("block_entities", 10)
                blockEntities.forEach blockEntityLoop@ { blockEntity ->
                    val blockEntityNbt = blockEntity as NbtCompound
                    if (!blockEntityNbt.contains("Items")) return@blockEntityLoop
                    val containerBlockId = blockEntityNbt.getString("id")
                    val pos = Vec3d(
                        blockEntityNbt.getInt("x").toDouble(),
                        blockEntityNbt.getInt("y").toDouble(),
                        blockEntityNbt.getInt("z").toDouble()
                    )
                    val itemsListTag = blockEntityNbt.getList("Items", 10)
                    dirty = dirty or
                            scanInventory(itemsListTag, MapSource(MapSourceId.CONTAINER, containerBlockId, pos), ctx)
                }
            }
            if (dirty) {
                chunkNbt?.let { nbt ->
                    regionFile.getChunkOutputStream(chunkPos).use { chunkOutputStream ->
                        NbtIo.write(nbt, chunkOutputStream)
                    }
                }
            }
        }
    }
}
