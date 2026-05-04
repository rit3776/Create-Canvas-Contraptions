package dev.rit3776.canvascontraptions.network;

import dev.rit3776.canvascontraptions.CanvasContraptions;
import dev.rit3776.canvascontraptions.CCDataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record C2SSelectTilePacket(int selectedIndex) implements CustomPacketPayload {
    public static final Type<C2SSelectTilePacket> TYPE = new Type<>(CanvasContraptions.asResource("select_tile"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SSelectTilePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, C2SSelectTilePacket::selectedIndex,
            C2SSelectTilePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ItemStack stack = player.getMainHandItem();
            
            CCDataComponents.DraftingLayout layout = stack.get(CCDataComponents.DRAFTING_LAYOUT);
            if (layout != null) {
                stack.set(CCDataComponents.DRAFTING_LAYOUT, new CCDataComponents.DraftingLayout(
                        layout.mapIds(), layout.width(), layout.height(), layout.fileName(), selectedIndex
                ));
            }
        });
    }
}
