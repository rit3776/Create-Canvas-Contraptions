package dev.rit3776.canvascontraptions;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class PaintedMovementBehaviour implements MovementBehaviour {

    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld, ContraptionMatrices matrices, MultiBufferSource buffer) {
        if (!context.world.isClientSide) return;

        if (context.blockEntityData == null) return;

        int mapId = context.blockEntityData.getInt("MapID");
        if (mapId < 0) return;

        MapItemSavedData data = ClientMapCache.getOrCreate(mapId, context.world);
        
        Direction facing = context.state.getValue(PaintedBlock.FACING);
        
        if (!ClientMapCache.hasData(mapId)) return;

        PoseStack poseStack = matrices.getModel();
        poseStack.pushPose();

        poseStack.translate(0.5, 0.5, 0.5);

        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
            case EAST  -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
            case WEST  -> poseStack.mulPose(Axis.YP.rotationDegrees(270));
            case UP    -> poseStack.mulPose(Axis.XP.rotationDegrees(-90));
            case DOWN  -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
            default    -> {}
        }

        poseStack.translate(-0.5, 0.5, -0.485);
        poseStack.scale(1f / 128f, -1f / 128f, 1f);

        BlockPos lightPos = context.position != null ? BlockPos.containing(context.position) : context.localPos;
        int light = LevelRenderer.getLightColor(context.world, lightPos);

        Minecraft.getInstance().gameRenderer.getMapRenderer()
                .render(poseStack, buffer, mapId, data, true, light);

        poseStack.popPose();
    }
}
