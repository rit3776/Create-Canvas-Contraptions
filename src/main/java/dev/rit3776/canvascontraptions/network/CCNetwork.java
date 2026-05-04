package dev.rit3776.canvascontraptions.network;

import dev.rit3776.canvascontraptions.CanvasContraptions;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class CCNetwork {
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(CanvasContraptions.MODID).versioned("1.0.0");

        registrar.playToServer(C2SImageUploadPacket.TYPE, C2SImageUploadPacket.STREAM_CODEC, C2SImageUploadPacket::handle);
        registrar.playToServer(C2SRequestMapDataPacket.TYPE, C2SRequestMapDataPacket.STREAM_CODEC, C2SRequestMapDataPacket::handle);
        registrar.playToServer(C2SSelectTilePacket.TYPE, C2SSelectTilePacket.STREAM_CODEC, C2SSelectTilePacket::handle);
        registrar.playToServer(C2STabletActionPacket.TYPE, C2STabletActionPacket.STREAM_CODEC, C2STabletActionPacket::handle);

        registrar.playToClient(S2CMapDataPacket.TYPE, S2CMapDataPacket.STREAM_CODEC, S2CMapDataPacket::handle);
    }
}
