package org.waste.of.time.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.server.integrated.IntegratedServerLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.waste.of.time.WorldTools;

@Mixin(IntegratedServerLoader.class)
public class IntegratedServerLoaderMixin {

    @WrapOperation(method = "start(Lnet/minecraft/client/gui/screen/Screen;Ljava/lang/String;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/integrated/IntegratedServerLoader;start(Lnet/minecraft/client/gui/screen/Screen;Ljava/lang/String;ZZ)V"
            ))
    public void disableExperimentalWorldSettingsScreen(IntegratedServerLoader instance, Screen parent, String levelName, boolean safeMode, boolean canShowBackupPrompt, Operation<Void> original) {
        if (WorldTools.INSTANCE.getConfig().getAdvanced().getHideExperimentalWorldGui())
            canShowBackupPrompt = false;
        original.call(instance, parent, levelName, safeMode, canShowBackupPrompt);
    }
}
