package org.waste.of.time.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import org.waste.of.time.storage.StorageManager


class SaveCommand : Command<FabricClientCommandSource> {
    override fun run(context: CommandContext<FabricClientCommandSource>?): Int {
        StorageManager.save(BoolArgumentType.getBool(context, "freezeEntities"))

        return 0
    }
}