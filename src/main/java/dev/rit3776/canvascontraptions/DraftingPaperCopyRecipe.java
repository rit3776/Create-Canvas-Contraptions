package dev.rit3776.canvascontraptions;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class DraftingPaperCopyRecipe extends CustomRecipe {
    public DraftingPaperCopyRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int filledCount = 0;
        int blankCount = 0;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.getItem() instanceof FilledDraftingPaperItem) {
                    if (stack.has(CCDataComponents.DRAFTING_LAYOUT)) {
                        filledCount++;
                    } else {
                        return false;
                    }
                } else if (stack.getItem() instanceof BlankDraftingPaperItem) {
                    blankCount++;
                } else {
                    return false;
                }
            }
        }

        return filledCount == 1 && blankCount >= 1;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        int blankCount = 0;
        ItemStack filledSource = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.getItem() instanceof FilledDraftingPaperItem) {
                    filledSource = stack;
                } else if (stack.getItem() instanceof BlankDraftingPaperItem) {
                    blankCount++;
                }
            }
        }

        if (!filledSource.isEmpty() && blankCount > 0) {
            ItemStack result = new ItemStack(CCItems.FILLED_DRAFTING_PAPER.get(), blankCount + 1);
            CCDataComponents.DraftingLayout layout = filledSource.get(CCDataComponents.DRAFTING_LAYOUT);
            if (layout != null) {
                result.set(CCDataComponents.DRAFTING_LAYOUT, layout);
            }
            return result;
        }

        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 2 && height >= 2 || width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CanvasContraptions.DRAFTING_PAPER_COPY_SERIALIZER.get();
    }
}
