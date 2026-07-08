package dev.rit3776.canvascontraptions;

import net.neoforged.neoforge.common.ModConfigSpec;

public class CCServerConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue CONSUME_DYES;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("general");
        CONSUME_DYES = builder.comment("Whether to consume dyes when placing images in survival mode.")
                .define("consumeDyes", true);
        builder.pop();
        SPEC = builder.build();
    }
}
