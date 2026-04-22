package dev.rit3776.canvascontraptions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SSelectTilePacket {
    private final int index;
    private final InteractionHand hand;

    public C2SSelectTilePacket(int index, InteractionHand hand) {
        this.index = index;
        this.hand = hand;
    }

    public static void encode(C2SSelectTilePacket msg, FriendlyByteBuf buffer) {
        buffer.writeInt(msg.index);
        buffer.writeEnum(msg.hand);
    }

    public static C2SSelectTilePacket decode(FriendlyByteBuf buffer) {
        return new C2SSelectTilePacket(buffer.readInt(), buffer.readEnum(InteractionHand.class));
    }

    public static void handle(C2SSelectTilePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ItemStack stack = player.getItemInHand(msg.hand);
            if (stack.hasTag()) {
                stack.getTag().putInt("SelectedIndex", msg.index);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
