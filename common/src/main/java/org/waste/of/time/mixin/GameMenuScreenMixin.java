package org.waste.of.time.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.GridWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.waste.of.time.Events;

@Mixin(GameMenuScreen.class)
public class GameMenuScreenMixin {

    @Inject(method = "initWidgets", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/MinecraftClient;isInSingleplayer()Z"
    ))
    public void onInitWidgets(final CallbackInfo ci,
                              @Local GridWidget.Adder adder) {
        Events.INSTANCE.onGameMenuScreenInitWidgets(adder);
    }
}
