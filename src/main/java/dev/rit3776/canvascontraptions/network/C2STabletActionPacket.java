package dev.rit3776.canvascontraptions.network;

import dev.rit3776.canvascontraptions.CanvasContraptions;
import dev.rit3776.canvascontraptions.CCDataComponents;
import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record C2STabletActionPacket(Action action, int index, Optional<CCDataComponents.DraftingLayout> layout) implements CustomPacketPayload {
    public static final Type<C2STabletActionPacket> TYPE = new Type<>(CanvasContraptions.asResource("tablet_action"));

    public enum Action {
        SAVE, DELETE, SELECT
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, C2STabletActionPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(i -> Action.values()[i], a -> a.ordinal()), C2STabletActionPacket::action,
            ByteBufCodecs.VAR_INT, C2STabletActionPacket::index,
            ByteBufCodecs.optional(CCDataComponents.DraftingLayout.STREAM_CODEC), C2STabletActionPacket::layout,
            C2STabletActionPacket::new
    );
    
    // Wait, ByteBufCodecs.fromCodec for enum might be easier with ByteBufCodecs.idMapper
    // But let's use a simpler way for now if possible.
    // Actually, CCDataComponents.DraftingLayout.STREAM_CODEC is already defined.

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ItemStack stack = player.getMainHandItem();
            
            List<CCDataComponents.DraftingLayout> library = new ArrayList<>(stack.getOrDefault(CCDataComponents.TABLET_LIBRARY, List.of()));

            switch (action) {
                case SAVE -> {
                    layout.ifPresent(l -> {
                        if (library.size() < 16) {
                            library.add(l);
                        }
                    });
                }
                case DELETE -> {
                    if (index >= 0 && index < library.size()) {
                        library.remove(index);
                    }
                }
                case SELECT -> {
                    if (index >= 0 && index < library.size()) {
                        stack.set(CCDataComponents.DRAFTING_LAYOUT, library.get(index));
                    }
                }
            }
            stack.set(CCDataComponents.TABLET_LIBRARY, library);
        });
    }
}
