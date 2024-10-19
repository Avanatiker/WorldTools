package org.waste.of.time.storage.cache

import net.minecraft.block.ChestBlock
import net.minecraft.block.entity.*
import net.minecraft.block.enums.ChestType
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.ingame.*
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.entity.vehicle.HopperMinecartEntity
import net.minecraft.entity.vehicle.VehicleInventory
import net.minecraft.inventory.EnderChestInventory
import net.minecraft.inventory.SimpleInventory
import org.waste.of.time.WorldTools.mc
import org.waste.of.time.storage.cache.HotCache.markScanned
import org.waste.of.time.storage.cache.HotCache.scannedBlockEntities

object DataInjectionHandler {
    fun onScreenRemoved(screen: Screen) {
        HotCache.lastInteractedBlockEntity?.let {
            handleBlockEntity(screen, it)
        }
        HotCache.lastInteractedEntity?.let {
            handleEntity(screen, it)
        }
    }

    private fun handleEntity(screen: Screen, entity: Entity) {
        when (screen) {
            is GenericContainerScreen -> {
                (entity as? VehicleInventory)?.dataToVehicle(screen)
            }
            is HopperScreen -> {
                (entity as? HopperMinecartEntity)?.dataToHopperMinecart(screen)
            }
        }

        entity.markScanned()
    }

    private fun VehicleInventory.dataToVehicle(screen: GenericContainerScreen) {
        screen.getContainerSlots().forEach {
            setStack(it.index, it.stack)
        }
    }

    private fun HopperMinecartEntity.dataToHopperMinecart(screen: HopperScreen) {
        screen.getContainerSlots().forEach {
            setStack(it.index, it.stack)
        }
    }

    private fun handleBlockEntity(screen: Screen, blockEntity: BlockEntity, ) {
        when (screen) {
            is GenericContainerScreen -> {
                when (blockEntity) {
                    is ChestBlockEntity -> blockEntity.dataToChest(screen)
                    is BarrelBlockEntity -> blockEntity.dataToBarrelBlock(screen)
                    is EnderChestBlockEntity -> dataToEnderChest(screen)
                }
            }

            is Generic3x3ContainerScreen -> {
                (blockEntity as? DispenserBlockEntity)?.dataToDispenserOrDropper(screen)
            }

            is AbstractFurnaceScreen<*> -> {
                (blockEntity as? AbstractFurnaceBlockEntity)?.dataToFurnace(screen)
            }

            is BrewingStandScreen -> {
                (blockEntity as? BrewingStandBlockEntity)?.dataToBrewingStand(screen)
            }

            is HopperScreen -> {
                (blockEntity as? HopperBlockEntity)?.dataToHopper(screen)
            }

            is ShulkerBoxScreen -> {
                (blockEntity as? ShulkerBoxBlockEntity)?.dataToShulkerBox(screen)
            }

            is LecternScreen -> {
                (blockEntity as? LecternBlockEntity)?.dataToLectern(screen)
            }

            is CrafterScreen -> {
                (blockEntity as? CrafterBlockEntity)?.dataToCrafter(screen)
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

        blockEntity.markScanned()
    }

    private fun dataToEnderChest(screen: GenericContainerScreen) {
        if (mc.isInSingleplayer) return
        val inventory = screen.screenHandler.inventory as? SimpleInventory ?: return
        if (inventory.size() != 27) return
        mc.player?.enderChestInventory = EnderChestInventory().apply {
            repeat(inventory.size()) { i ->
                setStack(i, inventory.getStack(i))
            }
        }
    }

    private fun AbstractFurnaceBlockEntity.dataToFurnace(screen: AbstractFurnaceScreen<*>) {
        screen.getContainerSlots().forEach {
            setStack(it.index, it.stack)
        }
    }

    private fun BarrelBlockEntity.dataToBarrelBlock(screen: GenericContainerScreen) {
        screen.getContainerSlots().forEach {
            setStack(it.index, it.stack)
        }
    }

    private fun BrewingStandBlockEntity.dataToBrewingStand(screen: BrewingStandScreen) {
        screen.getContainerSlots().forEach {
            setStack(it.index, it.stack)
        }
    }

    private fun ChestBlockEntity.dataToChest(screen: GenericContainerScreen) {
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

    private fun DispenserBlockEntity.dataToDispenserOrDropper(screen: Generic3x3ContainerScreen) {
        screen.getContainerSlots().forEach {
            setStack(it.index, it.stack)
        }
    }

    private fun HopperBlockEntity.dataToHopper(screen: HopperScreen) {
        screen.getContainerSlots().forEach {
            setStack(it.index, it.stack)
        }
    }

    private fun ShulkerBoxBlockEntity.dataToShulkerBox(screen: ShulkerBoxScreen) {
        screen.getContainerSlots().forEach {
            setStack(it.index, it.stack)
        }
    }

    private fun LecternBlockEntity.dataToLectern(screen: LecternScreen) {
        book = screen.screenHandler.bookItem
    }

    private fun CrafterBlockEntity.dataToCrafter(screen: CrafterScreen) {
        screen.getContainerSlots().forEach {
            setStack(it.index, it.stack)
            setSlotEnabled(it.index, !isSlotDisabled(it.index))
        }
    }

    private fun HandledScreen<*>.getContainerSlots() = screenHandler.slots.filter { it.inventory !is PlayerInventory }
}
