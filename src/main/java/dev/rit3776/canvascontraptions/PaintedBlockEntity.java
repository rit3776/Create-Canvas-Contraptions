package dev.rit3776.canvascontraptions;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import dev.rit3776.canvascontraptions.network.CCNetwork;
import dev.rit3776.canvascontraptions.network.S2CMapDataPacket;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class PaintedBlockEntity extends BlockEntity {
    private int mapId = -1;
    private int rotation = 0;

    public PaintedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void setMapId(int id) {
        this.mapId = id;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);

            MapItemSavedData data = level.getMapData("map_" + id);
            if (data != null) {
                CCNetwork.broadcastToAllInRange(new S2CMapDataPacket(id, data.colors), level, worldPosition);
            }
        }
    }

    public int getMapId() {
        return mapId;
    }

    public void rotate() {
        this.rotation = (this.rotation + 1) % 4;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public int getRotation() {
        return rotation;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("MapID", mapId);
        tag.putInt("Rotation", rotation);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.mapId = tag.getInt("MapID");
        this.rotation = tag.getInt("Rotation");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putInt("MapID", mapId);
        tag.putInt("Rotation", rotation);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
