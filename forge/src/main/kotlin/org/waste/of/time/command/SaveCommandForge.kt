package org.waste.of.time.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.server.command.ServerCommandSource
import org.waste.of.time.storage.StorageManager

class SaveCommandForge : Command<ServerCommandSource> {
    override fun run(context: CommandContext<ServerCommandSource>?): Int {
        val noAi = try {
            BoolArgumentType.getBool(context, "freezeEntities")
        } catch (e: IllegalArgumentException) {
            false
        }
        StorageManager.save(noAi)
        return 0
    }
}
