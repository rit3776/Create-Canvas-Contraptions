package dev.rit3776.canvascontraptions;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class PaintedBlockRenderer implements BlockEntityRenderer<PaintedBlockEntity> {
    public PaintedBlockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(PaintedBlockEntity be, float partialTicks, PoseStack poseStack,
            MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        int id = be.getMapId();
        if (id < 0)
            return;

        if (be.getLevel() == null)
            return;
        MapId mapId = new MapId(id);
        MapItemSavedData data = ClientMapCache.getOrCreate(id, be.getLevel());

        if (!ClientMapCache.hasData(id))
            return;

        Direction facing = be.getBlockState().getValue(PaintedBlock.FACING);

        poseStack.pushPose();

        poseStack.translate(0.5, 0.5, 0.5);

        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(270));
            case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(-90));
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
            default -> {
            } // SOUTH
        }

        float angle = be.getRotation() * 90.0f;
        poseStack.mulPose(Axis.ZP.rotationDegrees(angle));

        poseStack.translate(-0.5, 0.5, -0.485);
        poseStack.scale(1f / 128f, -1f / 128f, 1f);

        Minecraft.getInstance().gameRenderer.getMapRenderer()
                .render(poseStack, buffer, mapId, data, false, combinedLight);

        poseStack.popPose();
    }
}
