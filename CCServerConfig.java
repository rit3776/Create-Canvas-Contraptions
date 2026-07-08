package dev.rit3776.canvascontraptions;

import net.minecraftforge.common.ForgeConfigSpec;

public class CCServerConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue CONSUME_DYES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("general");
        CONSUME_DYES = builder.comment("Whether to consume dyes when placing images in survival mode.")
                .define("consumeDyes", true);
        builder.pop();
        SPEC = builder.build();
    }
}
