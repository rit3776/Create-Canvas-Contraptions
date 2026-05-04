package dev.rit3776.canvascontraptions;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BlankDraftingPaperItem extends Item {
    public BlankDraftingPaperItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            DraftingGUI.open();
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
