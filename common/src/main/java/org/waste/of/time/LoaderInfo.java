package org.waste.of.time;

import dev.architectury.injectables.annotations.ExpectPlatform;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class LoaderInfo {
    @Contract(pure = true)
    @ExpectPlatform
    public static @NotNull String getVersion() {
        return "DEV";
    }
}
