package org.waste.of.time.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.waste.of.time.extension.IPalettedContainerExtension;

@Mixin(PalettedContainer.class)
public class PalettedContainerMixin implements IPalettedContainerExtension {
    @Unique
    private boolean ignoreLock = false;

    @WrapOperation(method = "serialize", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/PalettedContainer;lock()V"
    ))
    public void disableChunkContainerLock(PalettedContainer instance, Operation<Void> original) {
        if (ignoreLock) return;
        original.call(instance);
    }

    @WrapOperation(method = "serialize", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/PalettedContainer;unlock()V"
    ))
    public void disableChunkContainerUnlock(PalettedContainer instance, Operation<Void> original) {
        if (ignoreLock) return;
        original.call(instance);
    }

    @Override
    public void setWTIgnoreLock(boolean b) {
        ignoreLock = b;
    }
}
