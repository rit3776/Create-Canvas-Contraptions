package dev.rit3776.canvascontraptions;

import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

import static dev.rit3776.canvascontraptions.CanvasContraptions.REGISTRATE;

public class CCBlocks {
    public static final BlockEntry<PaintedBlock> PAINTED_BLOCK = REGISTRATE.block("painted_block", PaintedBlock::new)
            .initialProperties(SharedProperties::wooden)
            .properties(p -> p.mapColor(MapColor.WOOD).sound(SoundType.WOOD).noOcclusion().strength(0.0f))
            .onRegister(p -> {
                CanvasContraptions.LOGGER.info("Registering MovementBehaviour for Painted Block");
                MovementBehaviour.REGISTRY.register(p, new PaintedMovementBehaviour());
            })
            .register();

    public static void register() {}
}
