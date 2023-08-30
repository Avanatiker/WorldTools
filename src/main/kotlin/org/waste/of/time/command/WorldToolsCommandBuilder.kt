package org.waste.of.time.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.BoolArgumentType.bool
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import org.waste.of.time.WorldTools
import org.waste.of.time.storage.StorageManager

object WorldToolsCommandBuilder {
    fun register(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        dispatcher.register(
            literal("worldtools")
                .then(literal("capture")
                    .then(literal("start").executes {
                        WorldTools.startCapture()
                        0
                    }
                    ).then(literal("stop").executes {
                        WorldTools.stopCapture()
                        0
                    }
                    )
                    .then(literal("flush").executes {
                        WorldTools.flush()
                        0
                    }
                    )
                )
                .then(literal("save")
                    .executes {
                        StorageManager.save()
                    }
                    .then(argument("freezeEntities", bool()).executes {
                        StorageManager.save(BoolArgumentType.getBool(it, "freezeEntities"))
                    })
                )
        )
    }
}
