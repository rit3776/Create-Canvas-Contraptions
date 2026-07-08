package dev.rit3776.canvascontraptions;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FilledDraftingPaperItem extends Item {
    public FilledDraftingPaperItem(Properties properties) {
        super(properties.stacksTo(64));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null)
            return InteractionResult.PASS;

        ItemStack stack = context.getItemInHand();
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains("MapIDs"))
            return InteractionResult.FAIL;

        int selectedIndex = tag.getInt("SelectedIndex");
        int[] ids = tag.getIntArray("MapIDs");
        if (selectedIndex < 0 || selectedIndex >= ids.length)
            return InteractionResult.FAIL;

        int mapId = ids[selectedIndex];

        BlockPos pos = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockPos placePos = pos.relative(face);

        if (level.getBlockState(placePos).isAir() || level.getBlockState(placePos).canBeReplaced()) {
            if (!player.getAbilities().instabuild && CCServerConfig.CONSUME_DYES.get()) {
                if (!consumeDyes(player)) {
                    player.displayClientMessage(Component.translatable("message.canvascontraptions.missing_dyes"),
                            true);
                    return InteractionResult.FAIL;
                }
            }

            if (!level.isClientSide) {
                BlockState state = CCBlocks.PAINTED_BLOCK.getDefaultState().setValue(PaintedBlock.FACING, face);
                level.setBlock(placePos, state, 3);
                if (level.getBlockEntity(placePos) instanceof PaintedBlockEntity be) {
                    be.setMapId(mapId);
                }

                if (!player.getAbilities().instabuild && !(stack.getItem() instanceof DraftingTabletItem)) {
                    stack.shrink(1);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.FAIL;
    }

    private boolean consumeDyes(Player player) {
        List<Item> dyes = List.of(Items.CYAN_DYE, Items.MAGENTA_DYE, Items.YELLOW_DYE, Items.BLACK_DYE);
        for (Item dye : dyes) {
            boolean found = false;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack invStack = player.getInventory().getItem(i);
                if (invStack.is(dye)) {
                    found = true;
                    break;
                }
            }
            if (!found)
                return false;
        }

        for (Item dye : dyes) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack invStack = player.getInventory().getItem(i);
                if (invStack.is(dye)) {
                    invStack.shrink(1);
                    break;
                }
            }
        }
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            ClientHooks.openFilledDraftingPaper(hand);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            if (tag.contains("FileName")) {
                tooltip.add(Component.translatable("tooltip.canvascontraptions.file")
                        .append(Component.literal(tag.getString("FileName")).withStyle(ChatFormatting.BLUE)));
            }
            if (tag.contains("MapIDs")) {
                int[] ids = tag.getIntArray("MapIDs");
                int width = tag.getInt("Width");
                int height = tag.getInt("Height");
                int index = tag.getInt("SelectedIndex");
                tooltip.add(Component.translatable("tooltip.canvascontraptions.layout")
                        .append(Component.literal(width + "x" + height).withStyle(ChatFormatting.GOLD)));
                tooltip.add(Component.translatable("tooltip.canvascontraptions.selected_tile")
                        .append(Component.literal(String.valueOf(index)).withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(" / " + (ids.length - 1)).withStyle(ChatFormatting.DARK_GRAY)));
            }
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
