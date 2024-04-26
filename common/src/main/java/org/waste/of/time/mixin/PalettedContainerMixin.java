package org.waste.of.time.mixin;

import net.minecraft.world.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.waste.of.time.extension.IPalettedContainerExtension;

@Mixin(PalettedContainer.class)
public class PalettedContainerMixin implements IPalettedContainerExtension {
    @Unique
    private boolean ignoreLock = false;

    @Inject(method = "lock", at = @At("HEAD"), cancellable = true)
    public void disableChunkContainerLock(CallbackInfo ci) {
        if (ignoreLock) ci.cancel();
    }

    @Inject(method = "unlock", at = @At("HEAD"), cancellable = true)
    public void disableChunkContainerUnLock(CallbackInfo ci) {
        if (ignoreLock) ci.cancel();
    }

    @Override
    public void setWTIgnoreLock(boolean b) {
        ignoreLock = b;
    }
}
