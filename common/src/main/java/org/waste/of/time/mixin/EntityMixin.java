package org.waste.of.time.mixin;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.waste.of.time.WorldTools;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "setRemoved", at = @At("HEAD"), cancellable = true)
    private void cancelMarkRemoved(Entity.RemovalReason reason, CallbackInfo ci) {
        if (WorldTools.INSTANCE.getCaching() && reason == Entity.RemovalReason.DISCARDED) ci.cancel();
    }
}
