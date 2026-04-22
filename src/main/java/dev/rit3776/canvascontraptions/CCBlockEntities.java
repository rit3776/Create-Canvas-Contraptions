package dev.rit3776.canvascontraptions;

import com.tterrag.registrate.util.entry.BlockEntityEntry;

import static dev.rit3776.canvascontraptions.CanvasContraptions.REGISTRATE;

public class CCBlockEntities {
    public static final BlockEntityEntry<PaintedBlockEntity> PAINTED_BLOCK_ENTITY = REGISTRATE
            .blockEntity("painted_block_entity", PaintedBlockEntity::new)
            .validBlocks(CCBlocks.PAINTED_BLOCK)
            .renderer(() -> PaintedBlockRenderer::new)
            .register();

    public static void register() {}
}
