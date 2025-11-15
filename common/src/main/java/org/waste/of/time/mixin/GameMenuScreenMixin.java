package org.waste.of.time.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.waste.of.time.Events;
import org.waste.of.time.gui.ManagerScreen;
import org.waste.of.time.manager.CaptureManager;
import org.waste.of.time.manager.MessageManager;

@Mixin(GameMenuScreen.class)
public class GameMenuScreenMixin {

    @Inject(method = "initWidgets", at = @At("TAIL"))
    public void onInitWidgets(final CallbackInfo ci) {
        GameMenuScreen self = (GameMenuScreen)(Object)this;
        MinecraftClient client = MinecraftClient.getInstance();
        Text label = CaptureManager.INSTANCE.getCapturing()
                ? MessageManager.INSTANCE.translateHighlight("worldtools.gui.escape.button.finish_download", CaptureManager.INSTANCE.getCurrentLevelName())
                : MessageManager.INSTANCE.getBrand();
        ButtonWidget button = ButtonWidget.builder(label, b -> {
            if (CaptureManager.INSTANCE.getCapturing()) {
                CaptureManager.INSTANCE.stop();
                client.setScreen(null);
            } else {
                client.setScreen(ManagerScreen.INSTANCE);
            }
        }).width(204).build();
        button.setX(10);
        button.setY(self.height - 30);
        ((ScreenAccessor) self).wt$addDrawableChild(button);
    }
}
