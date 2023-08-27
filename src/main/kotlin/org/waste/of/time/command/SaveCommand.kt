package org.waste.of.time.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import org.waste.of.time.storage.StorageManager


class SaveCommand : Command<FabricClientCommandSource> {
    override fun run(context: CommandContext<FabricClientCommandSource>?): Int {
        val noAi = try {
            BoolArgumentType.getBool(context, "freezeEntities")
        } catch (e: IllegalArgumentException) {
             false
        }
        StorageManager.save(noAi)
        return 0
    }
}
