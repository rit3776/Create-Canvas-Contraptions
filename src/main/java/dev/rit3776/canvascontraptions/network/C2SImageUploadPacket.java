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
import java.util.List;

public record C2SImageUploadPacket(List<byte[]> mapDatas, int width, int height, InteractionHand hand, String fileName) implements CustomPacketPayload {
    public static final Type<C2SImageUploadPacket> TYPE = new Type<>(CanvasContraptions.asResource("image_upload"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SImageUploadPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE_ARRAY.apply(ByteBufCodecs.list()), C2SImageUploadPacket::mapDatas,
            ByteBufCodecs.VAR_INT, C2SImageUploadPacket::width,
            ByteBufCodecs.VAR_INT, C2SImageUploadPacket::height,
            ByteBufCodecs.VAR_INT.map(i -> InteractionHand.values()[i], InteractionHand::ordinal), C2SImageUploadPacket::hand,
            ByteBufCodecs.STRING_UTF8, C2SImageUploadPacket::fileName,
            C2SImageUploadPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            
            List<Integer> ids = new ArrayList<>();
            for (byte[] pixels : mapDatas) {
                int mapId = ServerMapManager.getOrCreateMapId(player.serverLevel(), pixels);
                ids.add(mapId);
                PacketDistributor.sendToPlayer(player, new S2CMapDataPacket(mapId, pixels));
            }

            ItemStack heldStack = player.getItemInHand(hand);
            CCDataComponents.DraftingLayout layout = new CCDataComponents.DraftingLayout(ids, width, height, fileName, 0);
            
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
        });
    }
}
