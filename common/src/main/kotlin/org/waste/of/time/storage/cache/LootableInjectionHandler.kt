package org.waste.of.time.storage.cache

import net.minecraft.block.ChestBlock
import net.minecraft.block.entity.*
import net.minecraft.block.enums.ChestType
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.ingame.*
import net.minecraft.inventory.EnderChestInventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.registry.Registries
import org.waste.of.time.WorldTools.LOG
import org.waste.of.time.WorldTools.config
import org.waste.of.time.WorldTools.mc
import org.waste.of.time.manager.StatisticManager
import org.waste.of.time.storage.cache.HotCache.scannedBlockEntities

object LootableInjectionHandler {
    fun onScreenRemoved(screen: Screen) {
        val blockEntity = HotCache.lastInteractedBlockEntity ?: return

        when (screen) {
            is GenericContainerScreen -> {
                when (blockEntity) {
                    is ChestBlockEntity -> blockEntity.injectDataToChest(screen)
                    is BarrelBlockEntity -> blockEntity.injectDataToBarrelBlock(screen)
                    is EnderChestBlockEntity -> injectDataToEnderChest(screen)
                }
            }
            is Generic3x3ContainerScreen -> {
                (blockEntity as? DispenserBlockEntity)?.injectDataToDispenserOrDropper(screen)
            }
            is AbstractFurnaceScreen<*> -> {
                (blockEntity as? AbstractFurnaceBlockEntity)?.injectDataToFurnace(screen)
            }
            is BrewingStandScreen -> {
                (blockEntity as? BrewingStandBlockEntity)?.injectDataToBrewingStand(screen)
            }
            is HopperScreen -> {
                (blockEntity as? HopperBlockEntity)?.injectDataToHopper(screen)
            }
            is ShulkerBoxScreen -> {
                (blockEntity as? ShulkerBoxBlockEntity)?.injectDataToShulkerBox(screen)
            }
            is LecternScreen -> {
                (blockEntity as? LecternBlockEntity)?.injectDataToLectern(screen)
            }
        }

        // ToDo: Add support for entity containers like chest boat and minecart

        // ToDo: Find out if its possible to get the map state update (currently has no effect)
//        screen.getContainerSlots().filter {
//            it.stack.item == Items.FILLED_MAP
//        }.forEach {
//            it.stack.components.get(DataComponentTypes.MAP_ID)?.let { id ->
//                HotCache.mapIDs.add(id.id)
//            }
//        }

        scannedBlockEntities[blockEntity.pos] = blockEntity
        blockEntity.world?.registryKey?.value?.path?.let {
            StatisticManager.dimensions.add(it)
        }
        if (config.debug.logSavedContainers) {
            LOG.info("Saved block entity: ${Registries.BLOCK_ENTITY_TYPE.getId(blockEntity.type)?.path} at ${blockEntity.pos}")
        }
    }

    private fun injectDataToEnderChest(screen: GenericContainerScreen) {
        if (mc.isInSingleplayer) return
        val inventory = screen.screenHandler.inventory as? SimpleInventory ?: return
        if (inventory.size() != 27) return
        mc.player?.enderChestInventory = EnderChestInventory().apply {
            repeat(inventory.size()) { i ->
                setStack(i, inventory.getStack(i))
            }
        }
    }

    private fun AbstractFurnaceBlockEntity.injectDataToFurnace(screen: AbstractFurnaceScreen<*>) {
        screen.getContainerSlots().forEach {
            setStack(it.index, it.stack)
        }
    }

    private fun BarrelBlockEntity.injectDataToBarrelBlock(screen: GenericContainerScreen) {
        screen.getContainerSlots().forEach {
            setStack(it.index, it.stack)
        }
    }

    private fun BrewingStandBlockEntity.injectDataToBrewingStand(screen: BrewingStandScreen) {
        screen.getContainerSlots().forEach {
            setStack(it.index, it.stack)
        }
    }

    private fun ChestBlockEntity.injectDataToChest(screen: GenericContainerScreen) {
        val facing = cachedState[ChestBlock.FACING] ?: return
        val chestType = cachedState[ChestBlock.CHEST_TYPE] ?: return
        val containerSlots = screen.getContainerSlots()
        val inventories = containerSlots.partition { it.index < 27 }

        when (chestType) {
            ChestType.SINGLE -> {
                containerSlots.forEach {
                    setStack(it.index, it.stack)
                }
            }

            ChestType.LEFT -> {
                val pos = pos.offset(facing.rotateYClockwise())
                val otherChest = world?.getBlockEntity(pos)
                if (otherChest !is ChestBlockEntity) return

                inventories.first.forEach {
                    otherChest.setStack(it.index, it.stack)
                }
                inventories.second.forEach {
                    setStack(it.index - 27, it.stack)
                }

                scannedBlockEntities[otherChest.pos] = otherChest
            }

            ChestType.RIGHT -> {
                val pos = pos.offset(facing.rotateYCounterclockwise())
                val otherChest = world?.getBlockEntity(pos)
                if (otherChest !is ChestBlockEntity) return

                inventories.first.forEach {
                    setStack(it.index, it.stack)
                }
                inventories.second.forEach {
                    otherChest.setStack(it.index - 27, it.stack)
                }

                scannedBlockEntities[otherChest.pos] = otherChest
            }
        }
    }

    private fun DispenserBlockEntity.injectDataToDispenserOrDropper(screen: Generic3x3ContainerScreen) {
        screen.getContainerSlots().forEach {
            setStack(it.index, it.stack)
        }
    }

    private fun HopperBlockEntity.injectDataToHopper(screen: HopperScreen) {
        screen.getContainerSlots().forEach {
            setStack(it.index, it.stack)
        }
    }

    private fun ShulkerBoxBlockEntity.injectDataToShulkerBox(screen: ShulkerBoxScreen) {
        screen.getContainerSlots().forEach {
            setStack(it.index, it.stack)
        }
    }

    private fun LecternBlockEntity.injectDataToLectern(screen: LecternScreen) {
        book = screen.screenHandler.bookItem
    }

    private fun HandledScreen<*>.getContainerSlots() = screenHandler.slots.filter { it.inventory is SimpleInventory }
}
