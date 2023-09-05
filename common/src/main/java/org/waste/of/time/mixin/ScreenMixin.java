package org.waste.of.time.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.waste.of.time.event.Events;

@Mixin(Screen.class)
public class ScreenMixin {
    @Inject(method = "init(Lnet/minecraft/client/MinecraftClient;II)V", at = @At("TAIL"))
    private void afterInitScreen(MinecraftClient client, int width, int height, CallbackInfo ci) {
        Events.INSTANCE.onAfterInitScreen((Screen) (Object) this);
    }

    @Inject(method = "removed", at = @At("RETURN"))
    public void onRemoved(final CallbackInfo ci) {
        Events.INSTANCE.onScreenRemoved((Screen) (Object) this);
    }
}
