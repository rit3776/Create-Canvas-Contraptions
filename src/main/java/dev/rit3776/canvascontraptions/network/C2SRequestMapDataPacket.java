package dev.rit3776.canvascontraptions.network;

import dev.rit3776.canvascontraptions.CanvasContraptions;
import dev.rit3776.canvascontraptions.ServerMapManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record C2SRequestMapDataPacket(int mapId) implements CustomPacketPayload {
    public static final Type<C2SRequestMapDataPacket> TYPE = new Type<>(CanvasContraptions.asResource("request_map_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRequestMapDataPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, C2SRequestMapDataPacket::mapId,
            C2SRequestMapDataPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerMapManager.sendMapData(player, mapId);
        });
    }
}
