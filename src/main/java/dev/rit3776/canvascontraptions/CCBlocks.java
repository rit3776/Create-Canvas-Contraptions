package dev.rit3776.canvascontraptions;

import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.client.renderer.RenderType;

import static dev.rit3776.canvascontraptions.CanvasContraptions.REGISTRATE;

public class CCBlocks {
    public static final BlockEntry<PaintedBlock> PAINTED_BLOCK = REGISTRATE.block("painted_block", PaintedBlock::new)
            .properties(p -> p.mapColor(MapColor.COLOR_GRAY)
                    .instabreak()
                    .noOcclusion())
            .addLayer(() -> RenderType::cutout)
            .blockstate((c, p) -> p.directionalBlock(c.get(), p.models().withExistingParent(c.getName(), p.mcLoc("block/air"))))
            .register();

    public static void register() {}
}
