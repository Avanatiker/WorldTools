package org.waste.of.time.mixin

import net.minecraft.client.gui.hud.BossBarHud
import net.minecraft.client.gui.hud.ClientBossBar
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor
import java.util.*

@Mixin(BossBarHud::class)
interface BossBarHudAccessor {
    @Accessor("bossBars")
    fun getBossBars(): MutableMap<UUID, ClientBossBar>
}