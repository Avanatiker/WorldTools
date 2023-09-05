package org.waste.of.time.mixin;

import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.waste.of.time.WorldTools;

@Mixin(WorldChunk.class)
public class WorldChunkMixin {
    @Inject(method = "clear", at = @At(value = "INVOKE", target = "Ljava/util/Map;values()Ljava/util/Collection;", shift = At.Shift.AFTER, ordinal = 0), cancellable = true)
    private void cancelMarkRemoved(CallbackInfo ci) {
        if (WorldTools.INSTANCE.getCaching()) ci.cancel();
    }
}
