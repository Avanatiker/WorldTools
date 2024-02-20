package org.waste.of.time.storage.cache

import net.minecraft.block.ChestBlock
import net.minecraft.block.entity.*
import net.minecraft.block.enums.ChestType
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.ingame.*
import net.minecraft.inventory.EnderChestInventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.screen.GenericContainerScreenHandler
import org.waste.of.time.WorldTools
import org.waste.of.time.extension.IBlockEntityContainerExtension
import org.waste.of.time.manager.StatisticManager

object LootableInjectionHandler {
    fun onScreenRemoved(screen: Screen) {
        // ToDo: Add support for entity containers like chest boat and minecart
        if (screen !is HandledScreen<*>) return
        if (HotCache.lastInteractedBlockEntity is EnderChestBlockEntity
            && !WorldTools.mc.isInSingleplayer) { // SinglePlayer populates contents automatically
            cacheEnderChestContents(screen)
            return
        }
        val container = HotCache.lastInteractedBlockEntity as? LockableContainerBlockEntity ?: return

        when (container) {
            is AbstractFurnaceBlockEntity -> {
                container.injectDataToFurnace(screen)
                if (WorldTools.config.debug.logSavedContainers)
                    WorldTools.LOG.info("Saving furnace contents: ${container.pos}")
            }
            is BarrelBlockEntity -> {
                container.injectDataToBarrelBlock(screen)
                if (WorldTools.config.debug.logSavedContainers)
                    WorldTools.LOG.info("Saving barrel contents: ${container.pos}")
            }
            is BrewingStandBlockEntity -> {
                container.injectDataToBrewingStand(screen)
                if (WorldTools.config.debug.logSavedContainers)
                    WorldTools.LOG.info("Saving brewing stand contents: ${container.pos}")
            }
            is ChestBlockEntity -> {
                container.injectDataToChest(screen)
                if (WorldTools.config.debug.logSavedContainers)
                    WorldTools.LOG.info("Saving chest contents: ${container.pos}")
            }
            is DispenserBlockEntity -> {
                container.injectDataToDispenserOrDropper(screen)
                if (WorldTools.config.debug.logSavedContainers)
                    WorldTools.LOG.info("Saving dispenser/dropper: ${container.pos}")
            }
            is HopperBlockEntity -> {
                container.injectDataToHopper(screen)
                if (WorldTools.config.debug.logSavedContainers)
                    WorldTools.LOG.info("Saving hopper: ${container.pos}")
            }
            is ShulkerBoxBlockEntity -> {
                container.injectDataToShulkerBox(screen)
                if (WorldTools.config.debug.logSavedContainers)
                    WorldTools.LOG.info("Saving shulker: ${container.pos}")
            }
        }

        HotCache.blockEntities.add(container)
        StatisticManager.containers++
        container.world?.registryKey?.value?.path?.let {
            StatisticManager.dimensions.add(it)
        }
    }

    private fun cacheEnderChestContents(screen: HandledScreen<*>) {
        val screenHandler = screen.screenHandler as? GenericContainerScreenHandler ?: return
        val inventory = screenHandler.inventory as? SimpleInventory ?: return
        if (inventory.size() != 27) return
        val enderChestInv = EnderChestInventory()
        for (i in 0..<inventory.size()) {
            enderChestInv.setStack(i, inventory.getStack(i))
        }
        MinecraftClient.getInstance().player?.enderChestInventory = enderChestInv
        StatisticManager.containers++
    }

    private fun AbstractFurnaceBlockEntity.injectDataToFurnace(screen: HandledScreen<*>) {
        if (screen !is AbstractFurnaceScreen) return
        screen.getContainerSlots().forEach {
            setStack(it.index, it.stack)
        }
        (this as IBlockEntityContainerExtension).wtContentsRead = true;
    }

    private fun BarrelBlockEntity.injectDataToBarrelBlock(screen: HandledScreen<*>) {
        if (screen !is GenericContainerScreen) return
        screen.getContainerSlots().forEach {
            setStack(it.index, it.stack)
        }
        (this as IBlockEntityContainerExtension).wtContentsRead = true;
    }

    private fun BrewingStandBlockEntity.injectDataToBrewingStand(screen: HandledScreen<*>) {
        if (screen !is BrewingStandScreen) return
        screen.getContainerSlots().forEach {
            setStack(it.index, it.stack)
        }
        (this as IBlockEntityContainerExtension).wtContentsRead = true;
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
        (this as IBlockEntityContainerExtension).wtContentsRead = true;
    }

    private fun DispenserBlockEntity.injectDataToDispenserOrDropper(screen: HandledScreen<*>) {
        if (screen !is Generic3x3ContainerScreen) return
        screen.getContainerSlots().forEach {
            setStack(it.index, it.stack)
        }
        (this as IBlockEntityContainerExtension).wtContentsRead = true;
    }

    private fun HopperBlockEntity.injectDataToHopper(screen: HandledScreen<*>) {
        if (screen !is HopperScreen) return
        screen.getContainerSlots().forEach {
            setStack(it.index, it.stack)
        }
        (this as IBlockEntityContainerExtension).wtContentsRead = true;
    }

    private fun ShulkerBoxBlockEntity.injectDataToShulkerBox(screen: HandledScreen<*>) {
        if (screen !is ShulkerBoxScreen) return
        screen.getContainerSlots().forEach {
            setStack(it.index, it.stack)
        }
        (this as IBlockEntityContainerExtension).wtContentsRead = true;
    }

    private fun HandledScreen<*>.getContainerSlots() = screenHandler.slots.filter { it.inventory is SimpleInventory }

}
