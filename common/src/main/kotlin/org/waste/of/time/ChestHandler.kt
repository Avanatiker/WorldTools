package org.waste.of.time

import net.minecraft.block.ChestBlock
import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.block.enums.ChestType
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.inventory.SimpleInventory
import org.waste.of.time.WorldTools.cachedBlockEntities
import org.waste.of.time.WorldTools.checkCache

object ChestHandler {

    fun onScreenRemoved(screen: Screen) {
        if (screen !is GenericContainerScreen) return

        val container = WorldTools.lastOpenedContainer ?: return
        val facing = container.cachedState[ChestBlock.FACING]
        val chestType = container.cachedState[ChestBlock.CHEST_TYPE]
        val containerSlots = screen.screenHandler.slots.filter { it.inventory is SimpleInventory }

        cachedBlockEntities.add(container)
        val inventories = containerSlots.partition { it.index < 27 }

        when (chestType) {
            ChestType.SINGLE -> {
                containerSlots.forEach {
                    container.setStack(it.index, it.stack)
                }
            }
            ChestType.LEFT -> {
                val pos = container.pos.offset(facing.rotateYClockwise())
                val otherChest = container.world?.getBlockEntity(pos)
                if (otherChest !is ChestBlockEntity) return

                inventories.first.forEach {
                    otherChest.setStack(it.index, it.stack)
                }
                inventories.second.forEach {
                    container.setStack(it.index - 27, it.stack)
                }

                cachedBlockEntities.add(otherChest)
            }
            ChestType.RIGHT -> {
                val pos = container.pos.offset(facing.rotateYCounterclockwise())
                val otherChest = container.world?.getBlockEntity(pos)
                if (otherChest !is ChestBlockEntity) return

                inventories.first.forEach {
                    container.setStack(it.index, it.stack)
                }
                inventories.second.forEach {
                    otherChest.setStack(it.index - 27, it.stack)
                }

                cachedBlockEntities.add(otherChest)
            }
            else -> return
        }

        checkCache()
    }

}
