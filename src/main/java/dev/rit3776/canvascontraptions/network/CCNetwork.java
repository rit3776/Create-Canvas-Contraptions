package dev.rit3776.canvascontraptions.network;

import dev.rit3776.canvascontraptions.CanvasContraptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class CCNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CanvasContraptions.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int id = 0;

    public static void register() {
        INSTANCE.registerMessage(id++, C2SImageUploadPacket.class, C2SImageUploadPacket::encode, C2SImageUploadPacket::decode, C2SImageUploadPacket::handle);
        INSTANCE.registerMessage(id++, C2SSelectTilePacket.class, C2SSelectTilePacket::encode, C2SSelectTilePacket::decode, C2SSelectTilePacket::handle);
        INSTANCE.registerMessage(id++, S2CMapDataPacket.class, S2CMapDataPacket::encode, S2CMapDataPacket::decode, S2CMapDataPacket::handle);
        INSTANCE.registerMessage(id++, C2SRequestMapDataPacket.class, C2SRequestMapDataPacket::encode, C2SRequestMapDataPacket::decode, C2SRequestMapDataPacket::handle);
        INSTANCE.registerMessage(id++, C2STabletActionPacket.class, C2STabletActionPacket::encode, C2STabletActionPacket::decode, C2STabletActionPacket::handle);
    }

    public static <MSG> void sendToServer(MSG msg) {
        INSTANCE.sendToServer(msg);
    }

    public static <MSG> void sendToClient(MSG msg, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    public static <MSG> void broadcastToAllInRange(MSG msg, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            INSTANCE.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                    pos.getX(), pos.getY(), pos.getZ(), 64.0, serverLevel.dimension())), msg);
        }
    }
}
