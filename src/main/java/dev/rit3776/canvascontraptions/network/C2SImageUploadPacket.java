package dev.rit3776.canvascontraptions.network;

import dev.rit3776.canvascontraptions.CCItems;
import dev.rit3776.canvascontraptions.DraftingTabletItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class C2SImageUploadPacket {
    public final List<byte[]> mapDatas;
    public final int width;
    public final int height;
    public final InteractionHand hand;
    public final String fileName;

    public C2SImageUploadPacket(List<byte[]> mapDatas, int width, int height, InteractionHand hand, String fileName) {
        this.mapDatas = mapDatas;
        this.width = width;
        this.height = height;
        this.hand = hand;
        this.fileName = fileName;
    }

    public static void encode(C2SImageUploadPacket msg, FriendlyByteBuf buffer) {
        buffer.writeInt(msg.mapDatas.size());
        for (byte[] data : msg.mapDatas) {
            buffer.writeByteArray(data);
        }
        buffer.writeInt(msg.width);
        buffer.writeInt(msg.height);
        buffer.writeEnum(msg.hand);
        buffer.writeUtf(msg.fileName);
    }

    public static C2SImageUploadPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readInt();
        List<byte[]> datas = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            datas.add(buffer.readByteArray());
        }
        return new C2SImageUploadPacket(datas, buffer.readInt(), buffer.readInt(), buffer.readEnum(InteractionHand.class), buffer.readUtf());
    }

    public static void handle(C2SImageUploadPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            ServerLevel level = player.serverLevel();

            int[] ids = new int[msg.mapDatas.size()];
            for (int i = 0; i < msg.mapDatas.size(); i++) {
                byte[] pixels = msg.mapDatas.get(i);
                int mapId = dev.rit3776.canvascontraptions.ServerMapManager.getOrCreateMapId(level, pixels);
                ids[i] = mapId;
                CCNetwork.sendToClient(new S2CMapDataPacket(mapId, pixels), player);
            }

            ItemStack heldStack = player.getItemInHand(msg.hand);
            if (heldStack.getItem() instanceof DraftingTabletItem) {
                CompoundTag tag = heldStack.getOrCreateTag();
                tag.putIntArray("MapIDs", ids);
                tag.putInt("Width", msg.width);
                tag.putInt("Height", msg.height);
                tag.putInt("SelectedIndex", 0);
                tag.putString("FileName", msg.fileName);
            } else {
                ItemStack result = new ItemStack(CCItems.FILLED_DRAFTING_PAPER.get());
                CompoundTag tag = result.getOrCreateTag();
                tag.putIntArray("MapIDs", ids);
                tag.putInt("Width", msg.width);
                tag.putInt("Height", msg.height);
                tag.putInt("SelectedIndex", 0);
                tag.putString("FileName", msg.fileName);

                // Correctly handle stack shrinking to only consume one empty paper
                if (heldStack.getCount() > 1) {
                    heldStack.shrink(1);
                    if (!player.getInventory().add(result)) {
                        player.drop(result, false);
                    }
                } else {
                    player.setItemInHand(msg.hand, result);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
