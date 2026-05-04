package dev.rit3776.canvascontraptions.network;

import dev.rit3776.canvascontraptions.CanvasContraptions;
import dev.rit3776.canvascontraptions.ClientMapCache;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record S2CMapDataPacket(int mapId, byte[] data) implements CustomPacketPayload {
    public static final Type<S2CMapDataPacket> TYPE = new Type<>(CanvasContraptions.asResource("map_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMapDataPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, S2CMapDataPacket::mapId,
            ByteBufCodecs.BYTE_ARRAY, S2CMapDataPacket::data,
            S2CMapDataPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientMapCache.updateMapData(mapId, data);
        });
    }
}
