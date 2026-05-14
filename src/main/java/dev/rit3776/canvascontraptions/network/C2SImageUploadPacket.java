package dev.rit3776.canvascontraptions.network;

import dev.rit3776.canvascontraptions.CanvasContraptions;
import dev.rit3776.canvascontraptions.CCDataComponents;
import dev.rit3776.canvascontraptions.CCItems;
import dev.rit3776.canvascontraptions.DraftingTabletItem;
import dev.rit3776.canvascontraptions.ServerMapManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public record C2SImageUploadPacket(int sliceIndex, int totalSlices, byte[] data, int width, int height,
        InteractionHand hand, String fileName) implements CustomPacketPayload {
    public static final Type<C2SImageUploadPacket> TYPE = new Type<>(CanvasContraptions.asResource("image_upload"));
    private static final Map<UUID, List<Integer>> uploadSessions = new ConcurrentHashMap<>();

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SImageUploadPacket> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, C2SImageUploadPacket>() {
        @Override
        public C2SImageUploadPacket decode(RegistryFriendlyByteBuf buf) {
            return new C2SImageUploadPacket(
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readByteArray(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    InteractionHand.values()[buf.readVarInt()],
                    buf.readUtf());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, C2SImageUploadPacket packet) {
            buf.writeVarInt(packet.sliceIndex);
            buf.writeVarInt(packet.totalSlices);
            buf.writeByteArray(packet.data);
            buf.writeVarInt(packet.width);
            buf.writeVarInt(packet.height);
            buf.writeVarInt(packet.hand.ordinal());
            buf.writeUtf(packet.fileName);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            UUID uuid = player.getUUID();

            if (sliceIndex == 0) {
                uploadSessions.put(uuid, new ArrayList<>(Collections.nCopies(totalSlices, -1)));
            }

            List<Integer> ids = uploadSessions.get(uuid);
            if (ids == null || ids.size() != totalSlices) {
                return;
            }

            int mapId = ServerMapManager.getOrCreateMapId(player.serverLevel(), data);
            ids.set(sliceIndex, mapId);
            PacketDistributor.sendToPlayer(player, new S2CMapDataPacket(mapId, data));

            if (!ids.contains(-1)) {
                uploadSessions.remove(uuid);
                ItemStack heldStack = player.getItemInHand(hand);
                CCDataComponents.DraftingLayout layout = new CCDataComponents.DraftingLayout(ids, width, height,
                        fileName, 0);

                if (heldStack.getItem() instanceof DraftingTabletItem) {
                    heldStack.set(CCDataComponents.DRAFTING_LAYOUT, layout);
                } else {
                    ItemStack result = new ItemStack(CCItems.FILLED_DRAFTING_PAPER.get());
                    result.set(CCDataComponents.DRAFTING_LAYOUT, layout);

                    if (heldStack.getCount() > 1) {
                        heldStack.shrink(1);
                        if (!player.getInventory().add(result)) {
                            player.drop(result, false);
                        }
                    } else {
                        player.setItemInHand(hand, result);
                    }
                }
            }
        });
    }
}
