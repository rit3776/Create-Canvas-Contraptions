package dev.rit3776.canvascontraptions;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class ClientHooks {

    @OnlyIn(Dist.CLIENT)
    public static void openBlankDraftingPaper(InteractionHand hand) {
        DraftingGUI.openBlank(hand);
    }

    @OnlyIn(Dist.CLIENT)
    public static void openFilledDraftingPaper(InteractionHand hand) {
        DraftingGUI.openFilled(hand);
    }

    @OnlyIn(Dist.CLIENT)
    public static void openDraftingTablet(Player player, InteractionHand hand) {
        Minecraft.getInstance().setScreen(new DraftingTabletGUI(player.getItemInHand(hand).getHoverName(), hand));
    }

    @OnlyIn(Dist.CLIENT)
    public static void handleMapData(int mapId, byte[] colors) {
        if (Minecraft.getInstance().level != null) {
            ClientMapCache.update(mapId, colors, Minecraft.getInstance().level);
        }
    }
}
