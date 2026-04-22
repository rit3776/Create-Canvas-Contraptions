package dev.rit3776.canvascontraptions.network;

import dev.rit3776.canvascontraptions.DraftingTabletItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2STabletActionPacket {
    public enum Action {
        SAVE, DELETE, SELECT
    }

    private final Action action;
    private final int index;
    private final InteractionHand hand;

    public C2STabletActionPacket(Action action, int index, InteractionHand hand) {
        this.action = action;
        this.index = index;
        this.hand = hand;
    }

    public static void encode(C2STabletActionPacket msg, FriendlyByteBuf buffer) {
        buffer.writeEnum(msg.action);
        buffer.writeInt(msg.index);
        buffer.writeEnum(msg.hand);
    }

    public static C2STabletActionPacket decode(FriendlyByteBuf buffer) {
        return new C2STabletActionPacket(
                buffer.readEnum(Action.class),
                buffer.readInt(),
                buffer.readEnum(InteractionHand.class)
        );
    }

    public static void handle(C2STabletActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ItemStack stack = player.getItemInHand(msg.hand);
            if (!(stack.getItem() instanceof DraftingTabletItem)) return;

            CompoundTag tag = stack.getOrCreateTag();
            ListTag savedList = tag.getList("SavedImages", Tag.TAG_COMPOUND);

            switch (msg.action) {
                case SAVE:
                    if (savedList.size() < 16 && tag.contains("MapIDs")) {
                        CompoundTag newEntry = new CompoundTag();
                        newEntry.putIntArray("MapIDs", tag.getIntArray("MapIDs"));
                        newEntry.putInt("Width", tag.getInt("Width"));
                        newEntry.putInt("Height", tag.getInt("Height"));
                        newEntry.putString("FileName", tag.getString("FileName"));
                        newEntry.putInt("SelectedIndex", 0);
                        savedList.add(newEntry);
                        tag.put("SavedImages", savedList);
                    }
                    break;
                case DELETE:
                    if (msg.index >= 0 && msg.index < savedList.size()) {
                        savedList.remove(msg.index);
                        tag.put("SavedImages", savedList);
                    }
                    break;
                case SELECT:
                    if (msg.index >= 0 && msg.index < savedList.size()) {
                        CompoundTag entry = savedList.getCompound(msg.index);
                        tag.putIntArray("MapIDs", entry.getIntArray("MapIDs"));
                        tag.putInt("Width", entry.getInt("Width"));
                        tag.putInt("Height", entry.getInt("Height"));
                        tag.putString("FileName", entry.getString("FileName"));
                        tag.putInt("SelectedIndex", 0);
                    }
                    break;
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
