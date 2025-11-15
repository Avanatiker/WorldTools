package org.waste.of.time.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Selectable;

@Mixin(Screen.class)
public interface ScreenAccessor {
    @Invoker("addDrawableChild")
    <T extends Element & Drawable & Selectable> T wt$addDrawableChild(T widget);
}

