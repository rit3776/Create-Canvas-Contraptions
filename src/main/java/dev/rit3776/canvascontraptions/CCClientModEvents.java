package dev.rit3776.canvascontraptions;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = CanvasContraptions.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class CCClientModEvents {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(CCBlockEntities.PAINTED_BLOCK_ENTITY.get(), PaintedBlockRenderer::new);
    }
}
