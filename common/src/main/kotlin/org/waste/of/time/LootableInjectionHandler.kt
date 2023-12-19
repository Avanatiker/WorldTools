package org.waste.of.time

import net.minecraft.block.ChestBlock
import net.minecraft.block.entity.AbstractFurnaceBlockEntity
import net.minecraft.block.entity.BarrelBlockEntity
import net.minecraft.block.entity.BrewingStandBlockEntity
import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.block.entity.DispenserBlockEntity
import net.minecraft.block.entity.HopperBlockEntity
import net.minecraft.block.entity.ShulkerBoxBlockEntity
import net.minecraft.block.enums.ChestType
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.ingame.*
import net.minecraft.inventory.SimpleInventory
import org.waste.of.time.event.HotCache

object LootableInjectionHandler {
    fun onScreenRemoved(screen: Screen) {
        // ToDo: Add support for entity containers like chest boat and minecart
        if (screen !is HandledScreen<*>) return
        val container = HotCache.lastOpenedContainer ?: return

        when (container) {
            is AbstractFurnaceBlockEntity -> {
                container.injectDataToFurnace(screen)
            }
            is BarrelBlockEntity -> {
                container.injectDataToBarrelBlock(screen)
            }
            is BrewingStandBlockEntity -> {
                container.injectDataToBrewingStand(screen)
            }
            is ChestBlockEntity -> {
                container.injectDataToChest(screen)
            }
            is DispenserBlockEntity -> {
                container.injectDataToDispenserOrDropper(screen)
            }
            is HopperBlockEntity -> {
                container.injectDataToHopper(screen)
            }
            is ShulkerBoxBlockEntity -> {
                container.injectDataToShulkerBox(screen)
            }
        }

        HotCache.blockEntities.add(container)
        StatisticManager.containers++
        container.world?.registryKey?.value?.path?.let {
            StatisticManager.dimensions.add(it)
        }
        HotCache.lastOpenedContainer = null
    }

    private fun AbstractFurnaceBlockEntity.injectDataToFurnace(screen: HandledScreen<*>) {
        if (screen !is AbstractFurnaceScreen) return
        screen.getContainerSlots().forEach {
            setStack(it.index, it.stack)
        }
    }

    private fun BarrelBlockEntity.injectDataToBarrelBlock(screen: HandledScreen<*>) {
        if (screen !is GenericContainerScreen) return
        screen.getContainerSlots().forEach {
            setStack(it.index, it.stack)
        }
    }

    private fun BrewingStandBlockEntity.injectDataToBrewingStand(screen: HandledScreen<*>) {
        if (screen !is BrewingStandScreen) return
        screen.getContainerSlots().forEach {
            setStack(it.index, it.stack)
        }
    }

    private fun ChestBlockEntity.injectDataToChest(screen: HandledScreen<*>) {
        if (screen !is GenericContainerScreen) return
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

                HotCache.blockEntities.add(otherChest)
                StatisticManager.containers++
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

                HotCache.blockEntities.add(otherChest)
            }
        }
    }

    private fun DispenserBlockEntity.injectDataToDispenserOrDropper(screen: HandledScreen<*>) {
        if (screen !is Generic3x3ContainerScreen) return
        screen.getContainerSlots().forEach {
            setStack(it.index, it.stack)
        }
    }

    private fun HopperBlockEntity.injectDataToHopper(screen: HandledScreen<*>) {
        if (screen !is HopperScreen) return
        screen.getContainerSlots().forEach {
            setStack(it.index, it.stack)
        }
    }

    private fun ShulkerBoxBlockEntity.injectDataToShulkerBox(screen: HandledScreen<*>) {
        if (screen !is ShulkerBoxScreen) return
        screen.getContainerSlots().forEach {
            setStack(it.index, it.stack)
        }
    }

    private fun HandledScreen<*>.getContainerSlots() = screenHandler.slots.filter { it.inventory is SimpleInventory }

}
