package org.waste.of.time.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.BoolArgumentType.bool
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import org.waste.of.time.WorldTools

object WorldToolsCommandBuilder {
    fun register(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        dispatcher.register(
            literal("worldtools")
                .then(
                    literal("capture")
                        .then(
                            literal("start")
                                .executes {
                                    WorldTools.capturing = true
                                    0
                                }
                        )
                        .then(
                            literal("stop")
                                .executes {
                                    WorldTools.capturing = false
                                    0
                                }
                        )
                        .then(
                            literal("flush")
                                .executes {
                                    WorldTools.flush()
                                    0
                                }
                        )
                )
                .then(
                    literal("save")
                        .then(argument("freezeEntities", bool()).executes(SaveCommand()))
                )
        )
    }
}