package org.waste.of.time

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import org.waste.of.time.command.WorldToolsFabricCommandBuilder

object WorldToolsFabric : ClientModInitializer {
    override fun onInitializeClient() {
        WorldTools.initialize()
        ClientCommandRegistrationCallback.EVENT.register(ClientCommandRegistrationCallback { dispatcher, _ ->
            WorldToolsFabricCommandBuilder.register(dispatcher)
        })
    }
}
