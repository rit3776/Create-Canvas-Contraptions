package dev.rit3776.canvascontraptions.network;

import dev.rit3776.canvascontraptions.ClientMapCache;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CMapDataPacket {
    private final int mapId;
    private final byte[] colors;

    public S2CMapDataPacket(int mapId, byte[] colors) {
        this.mapId = mapId;
        this.colors = colors;
    }

    public static void encode(S2CMapDataPacket msg, FriendlyByteBuf buffer) {
        buffer.writeInt(msg.mapId);
        buffer.writeByteArray(msg.colors);
    }

    public static S2CMapDataPacket decode(FriendlyByteBuf buffer) {
        return new S2CMapDataPacket(buffer.readInt(), buffer.readByteArray());
    }

    public static void handle(S2CMapDataPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            dev.rit3776.canvascontraptions.CanvasContraptions.LOGGER.info("Received S2CMapDataPacket for map ID: " + msg.mapId);
            if (Minecraft.getInstance().level != null) {
                ClientMapCache.update(msg.mapId, msg.colors, Minecraft.getInstance().level);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
