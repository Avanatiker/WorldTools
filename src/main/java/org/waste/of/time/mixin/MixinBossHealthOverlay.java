package org.waste.of.time.mixin;

import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.entity.boss.BossBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.waste.of.time.BarManager;

import java.util.*;

@Mixin(BossBarHud.class)
public class MixinBossHealthOverlay {


    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Ljava/util/Map;values()Ljava/util/Collection;"))
    public Collection<BossBar> modifyValues(Map<UUID, ClientBossBar> bossBars) {
        List<BossBar> list = new ArrayList<>(bossBars.size() + 2);
        BarManager.INSTANCE.getProgressBar().ifPresent(list::add);
        BarManager.INSTANCE.getCaptureBar().ifPresent(list::add);
        list.addAll(bossBars.values());
        return list;
    }


}
