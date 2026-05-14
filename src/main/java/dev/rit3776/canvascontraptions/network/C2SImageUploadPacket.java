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
import java.util.Map;
import java.util.function.Supplier;

public class C2SImageUploadPacket {
    private static final Map<java.util.UUID, List<Integer>> uploadSessions = new java.util.concurrent.ConcurrentHashMap<>();

    public final int sliceIndex;
    public final int totalSlices;
    public final byte[] data;
    public final int width;
    public final int height;
    public final InteractionHand hand;
    public final String fileName;

    public C2SImageUploadPacket(int sliceIndex, int totalSlices, byte[] data, int width, int height,
            InteractionHand hand, String fileName) {
        this.sliceIndex = sliceIndex;
        this.totalSlices = totalSlices;
        this.data = data;
        this.width = width;
        this.height = height;
        this.hand = hand;
        this.fileName = fileName;
    }

    public static void encode(C2SImageUploadPacket msg, FriendlyByteBuf buffer) {
        buffer.writeInt(msg.sliceIndex);
        buffer.writeInt(msg.totalSlices);
        buffer.writeByteArray(msg.data);
        buffer.writeInt(msg.width);
        buffer.writeInt(msg.height);
        buffer.writeEnum(msg.hand);
        buffer.writeUtf(msg.fileName);
    }

    public static C2SImageUploadPacket decode(FriendlyByteBuf buffer) {
        return new C2SImageUploadPacket(
                buffer.readInt(),
                buffer.readInt(),
                buffer.readByteArray(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readEnum(InteractionHand.class),
                buffer.readUtf());
    }

    public static void handle(C2SImageUploadPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null)
                return;
            java.util.UUID uuid = player.getUUID();

            if (msg.sliceIndex == 0) {
                uploadSessions.put(uuid, new ArrayList<>(java.util.Collections.nCopies(msg.totalSlices, -1)));
            }

            List<Integer> ids = uploadSessions.get(uuid);
            if (ids == null || ids.size() != msg.totalSlices) {
                return;
            }

            ServerLevel level = player.serverLevel();
            int mapId = dev.rit3776.canvascontraptions.ServerMapManager.getOrCreateMapId(level, msg.data);
            ids.set(msg.sliceIndex, mapId);
            CCNetwork.sendToClient(new S2CMapDataPacket(mapId, msg.data), player);

            if (!ids.contains(-1)) {
                uploadSessions.remove(uuid);

                int[] finalIds = new int[ids.size()];
                for (int i = 0; i < ids.size(); i++)
                    finalIds[i] = ids.get(i);

                ItemStack heldStack = player.getItemInHand(msg.hand);
                if (heldStack.getItem() instanceof DraftingTabletItem) {
                    CompoundTag tag = heldStack.getOrCreateTag();
                    tag.putIntArray("MapIDs", finalIds);
                    tag.putInt("Width", msg.width);
                    tag.putInt("Height", msg.height);
                    tag.putInt("SelectedIndex", 0);
                    tag.putString("FileName", msg.fileName);
                } else {
                    ItemStack result = new ItemStack(CCItems.FILLED_DRAFTING_PAPER.get());
                    CompoundTag tag = result.getOrCreateTag();
                    tag.putIntArray("MapIDs", finalIds);
                    tag.putInt("Width", msg.width);
                    tag.putInt("Height", msg.height);
                    tag.putInt("SelectedIndex", 0);
                    tag.putString("FileName", msg.fileName);

                    if (heldStack.getCount() > 1) {
                        heldStack.shrink(1);
                        if (!player.getInventory().add(result)) {
                            player.drop(result, false);
                        }
                    } else {
                        player.setItemInHand(msg.hand, result);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
