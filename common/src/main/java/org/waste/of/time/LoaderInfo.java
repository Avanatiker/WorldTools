package org.waste.of.time;

import dev.architectury.injectables.annotations.ExpectPlatform;

public class LoaderInfo {
    @ExpectPlatform
    public static String getVersion() {
        return "DEV";
    }
}
