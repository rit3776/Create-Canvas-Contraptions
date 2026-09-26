package dev.rit3776.canvascontraptions;

import net.neoforged.neoforge.common.ModConfigSpec;

public class CCClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue PRELOAD_LUT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("general");
        PRELOAD_LUT = builder.comment("Whether to generate the color conversion LUT on startup rather than on the first image conversion.")
                .define("preloadLutOnStartup", true);
        builder.pop();
        SPEC = builder.build();
    }
}
