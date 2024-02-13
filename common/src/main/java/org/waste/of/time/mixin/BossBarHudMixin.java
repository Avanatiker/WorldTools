package org.waste.of.time.mixin;

import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.waste.of.time.manager.BarManager;
import org.waste.of.time.manager.CaptureManager;

import java.util.*;

@Mixin(BossBarHud.class)
public class BossBarHudMixin {

    /**
     * This mixin is used to add the capture bar and the progress bar to the boss bar overlay and to make sure that
     * it is always rendered on top of the other boss bars.
     *
     * @param bossBars The boss bars that are currently being rendered
     * @return A collection of boss bars that includes the capture bar and the progress bar
     */

    // todo: remove redirects to avoid mod conflicts
    //  either replace them with injects or use MixinExtras
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Ljava/util/Map;values()Ljava/util/Collection;"))
    public Collection<ClientBossBar> modifyValues(Map<UUID, ClientBossBar> bossBars) {
        if (!CaptureManager.INSTANCE.getCapturing()) return bossBars.values();
        List<ClientBossBar> newBossBars = new ArrayList<>(bossBars.size() + 2);
        BarManager.INSTANCE.getCaptureBar().ifPresent(newBossBars::add);
        BarManager.INSTANCE.progressBar().ifPresent(newBossBars::add);
        newBossBars.addAll(bossBars.values());
        return newBossBars;
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Ljava/util/Map;isEmpty()Z"))
    public boolean modifyIsEmpty(Map<UUID, ClientBossBar> bossBars) {
        if (!CaptureManager.INSTANCE.getCapturing()) return bossBars.isEmpty();
        return bossBars.isEmpty()
                && BarManager.INSTANCE.getCaptureBar().isEmpty()
                && BarManager.INSTANCE.progressBar().isEmpty();
    }
}
