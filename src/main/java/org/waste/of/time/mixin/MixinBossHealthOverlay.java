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

    /**
     * This mixin is used to add the capture bar and the progress bar to the boss bar overlay and to make sure that
     * it is always rendered on top of the other boss bars.
     *
     * @param bossBars The boss bars that are currently being rendered
     * @return A collection of boss bars that includes the capture bar and the progress bar
     */
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Ljava/util/Map;values()Ljava/util/Collection;"))
    public Collection<BossBar> modifyValues(Map<UUID, ClientBossBar> bossBars) {
        List<BossBar> newBossBars = new ArrayList<>(bossBars.size() + 2);
        BarManager.INSTANCE.getCaptureBar().ifPresent(newBossBars::add);
        BarManager.INSTANCE.getProgressBar().ifPresent(newBossBars::add);
        newBossBars.addAll(bossBars.values());
        return newBossBars;
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Ljava/util/Map;isEmpty()Z"))
    public boolean modifyIsEmpty(Map<UUID, ClientBossBar> bossBars) {
        return bossBars.isEmpty()
                && BarManager.INSTANCE.getCaptureBar().isEmpty()
                && BarManager.INSTANCE.getProgressBar().isEmpty();
    }
}
