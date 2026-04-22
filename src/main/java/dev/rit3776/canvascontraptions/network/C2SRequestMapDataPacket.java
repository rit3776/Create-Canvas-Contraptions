package dev.rit3776.canvascontraptions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SRequestMapDataPacket {
    private final int mapId;

    public C2SRequestMapDataPacket(int mapId) {
        this.mapId = mapId;
    }

    public static void encode(C2SRequestMapDataPacket msg, FriendlyByteBuf buffer) {
        buffer.writeInt(msg.mapId);
    }

    public static C2SRequestMapDataPacket decode(FriendlyByteBuf buffer) {
        return new C2SRequestMapDataPacket(buffer.readInt());
    }

    public static void handle(C2SRequestMapDataPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            MapItemSavedData data = player.level().getMapData("map_" + msg.mapId);
            if (data != null) {
                CCNetwork.sendToClient(new S2CMapDataPacket(msg.mapId, data.colors), player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
