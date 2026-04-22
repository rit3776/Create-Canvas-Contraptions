package dev.rit3776.canvascontraptions;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class DraftingPaperCopyRecipe extends CustomRecipe {
    public DraftingPaperCopyRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        int filledCount = 0;
        int blankCount = 0;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.getItem() instanceof FilledDraftingPaperItem) {
                    if (stack.hasTag() && stack.getTag().contains("MapIDs")) {
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
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        int blankCount = 0;
        ItemStack filledSource = ItemStack.EMPTY;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
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
            if (filledSource.hasTag()) {
                result.setTag(filledSource.getTag().copy());
            }
            return result;
        }

        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3 || width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CanvasContraptions.DRAFTING_PAPER_COPY_SERIALIZER.get();
    }
}
