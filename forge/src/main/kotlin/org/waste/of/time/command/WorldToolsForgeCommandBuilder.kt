package org.waste.of.time.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.BoolArgumentType.bool
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import org.waste.of.time.WorldTools

object WorldToolsForgeCommandBuilder {
    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            literal("worldtools")
                .then(
                    literal("capture")
                        .then(
                            literal("start")
                                .executes {
                                    WorldTools.startCapture()
                                    0
                                }
                        )
                        .then(
                            literal("stop")
                                .executes {
                                    WorldTools.stopCapture()
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
                        .executes(SaveCommandForge())
                        .then(argument("freezeEntities", bool()).executes(SaveCommandForge()))

                )
        )
    }
}
