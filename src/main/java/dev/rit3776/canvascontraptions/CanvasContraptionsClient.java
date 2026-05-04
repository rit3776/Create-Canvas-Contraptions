package dev.rit3776.canvascontraptions;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = CanvasContraptions.MODID, dist = Dist.CLIENT)
public class CanvasContraptionsClient {
    public CanvasContraptionsClient(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.register(CCClientEvents.class);
    }
}
