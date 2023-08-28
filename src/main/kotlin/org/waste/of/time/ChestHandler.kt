package org.waste.of.time

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.minecraft.block.ChestBlock
import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.block.enums.ChestType
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.inventory.SimpleInventory
import net.minecraft.screen.slot.Slot
import net.minecraft.util.math.Direction

object ChestHandler {
    fun register(screen: Screen) {
        ScreenEvents.remove(screen).register(ScreenEvents.Remove { screen2 ->
            if (screen2 !is GenericContainerScreen) return@Remove
            val container = WorldTools.lastOpenedContainer ?: return@Remove
            val facing = container.cachedState[ChestBlock.FACING]
            val chestType = container.cachedState[ChestBlock.CHEST_TYPE]

            val containerSlots = screen2.screenHandler.slots.filter { it.inventory is SimpleInventory }

            WorldTools.cachedBlockEntities.add(container)

            when (chestType) {
                ChestType.SINGLE -> {
                    containerSlots.forEach {
                        container.setStack(it.index, it.stack)
                    }
                }
                ChestType.LEFT -> {
                    populateOtherChest(container, facing, containerSlots, false)
                }
                ChestType.RIGHT -> {
                    populateOtherChest(container, facing, containerSlots, true)
                }
                else -> return@Remove
            }

            WorldTools.checkCache()
        })
    }

    private fun populateOtherChest(container: ChestBlockEntity, facing: Direction, containerSlots: List<Slot>, isRight: Boolean) {
        val facingOffset = facing.rotateYClockwise()
        val pos = container.pos.offset(facingOffset) ?: return
        val otherChest = container.world?.getBlockEntity(pos) ?: return
        if (otherChest !is ChestBlockEntity) return

        val inventories = containerSlots.partition { it.index < 27 }
        inventories.first.forEach {
            if (isRight) container.setStack(it.index, it.stack)
            else otherChest.setStack(it.index, it.stack)
        }
        inventories.second.forEach {
            if (isRight) otherChest.setStack(it.index - 27, it.stack)
            else container.setStack(it.index - 27, it.stack)
        }

        WorldTools.cachedBlockEntities.add(otherChest)
    }
}