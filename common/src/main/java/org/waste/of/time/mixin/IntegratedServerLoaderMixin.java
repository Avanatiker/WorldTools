package org.waste.of.time.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.serialization.Dynamic;
import net.minecraft.server.integrated.IntegratedServerLoader;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.waste.of.time.WorldTools;

@Mixin(IntegratedServerLoader.class)
public class IntegratedServerLoaderMixin {

    @WrapOperation(method = "start(Lnet/minecraft/world/level/storage/LevelStorage$Session;Ljava/lang/Runnable;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/integrated/IntegratedServerLoader;start(Lnet/minecraft/world/level/storage/LevelStorage$Session;Lcom/mojang/serialization/Dynamic;ZZLjava/lang/Runnable;)V"
            ))
    public void disableExperimentalWorldSettingsScreen(IntegratedServerLoader instance, LevelStorage.Session session, Dynamic<?> levelProperties, boolean safeMode, boolean canShowBackupPrompt, Runnable onCancel, Operation<Void> original) {
        if (WorldTools.INSTANCE.getConfig().getAdvanced().getHideExperimentalWorldGui())
            canShowBackupPrompt = false;
        original.call(instance, session, levelProperties, safeMode, canShowBackupPrompt, onCancel);
    }
}
