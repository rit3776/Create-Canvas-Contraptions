package dev.rit3776.canvascontraptions;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import dev.rit3776.canvascontraptions.network.S2CMapDataPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.neoforged.neoforge.network.PacketDistributor;

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
            
            // Sync map data to nearby players
            MapItemSavedData data = level.getMapData(new MapId(id));
            if (data != null) {
                PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(worldPosition), new S2CMapDataPacket(id, data.colors));
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("MapID", mapId);
        tag.putInt("Rotation", rotation);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.mapId = tag.getInt("MapID");
        this.rotation = tag.getInt("Rotation");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("MapID", mapId);
        tag.putInt("Rotation", rotation);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
