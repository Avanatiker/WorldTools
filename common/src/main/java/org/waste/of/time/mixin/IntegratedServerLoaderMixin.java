package org.waste.of.time.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.integrated.IntegratedServerLoader;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.waste.of.time.WorldTools;

@Mixin(IntegratedServerLoader.class)
public class IntegratedServerLoaderMixin {

    @WrapOperation(method = "checkBackupAndStart",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/integrated/IntegratedServerLoader;showBackupPromptScreen(Lnet/minecraft/world/level/storage/LevelStorage$Session;ZLjava/lang/Runnable;Ljava/lang/Runnable;)V"
            ))
    public void disableExperimentalWorldSettingsScreen(IntegratedServerLoader instance, LevelStorage.Session session, boolean customized, Runnable callback, Runnable onCancel, Operation<Void> original) {
        if (WorldTools.INSTANCE.getConfig().getAdvanced().getHideExperimentalWorldGui()) {
            callback.run();
        } else {
            original.call(instance, session, customized, callback, onCancel);
        }
    }
}
