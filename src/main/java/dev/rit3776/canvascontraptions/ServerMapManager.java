package dev.rit3776.canvascontraptions;

import dev.rit3776.canvascontraptions.network.S2CMapDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.network.PacketDistributor;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class ServerMapManager {
    private static final Map<String, Integer> hashToId = new ConcurrentHashMap<>();
    private static boolean initialized = false;

    public static int getOrCreateMapId(ServerLevel level, byte[] colors) {
        if (!initialized) {
            initialize(level);
        }

        String hash = DigestUtils.sha256Hex(colors);
        Integer existingId = hashToId.get(hash);

        if (existingId != null) {
            if (level.getMapData(new MapId(existingId)) != null) {
                return existingId;
            } else {
                hashToId.remove(hash);
            }
        }

        MapId newId = level.getFreeMapId();
        MapItemSavedData data = MapItemSavedData.createFresh(0.0, 0.0, (byte) 3, false, false, level.dimension());
        System.arraycopy(colors, 0, data.colors, 0, Math.min(colors.length, data.colors.length));
        level.setMapData(newId, data);

        hashToId.put(hash, newId.id());
        return newId.id();
    }

    public static void sendMapData(ServerPlayer player, int mapId) {
        MapItemSavedData data = player.serverLevel().getMapData(new MapId(mapId));
        if (data != null) {
            PacketDistributor.sendToPlayer(player, new S2CMapDataPacket(mapId, data.colors));
        }
    }

    private static synchronized void initialize(ServerLevel level) {
        if (initialized) return;
        initialized = true;

        CanvasContraptions.LOGGER.info("Scanning existing maps for de-duplication...");
        long start = System.currentTimeMillis();

        Path dataFolder = level.getServer().getWorldPath(LevelResource.ROOT).resolve("data");
        if (Files.exists(dataFolder)) {
            try (Stream<Path> stream = Files.list(dataFolder)) {
                stream.filter(path -> path.getFileName().toString().startsWith("map_") && path.getFileName().toString().endsWith(".dat"))
                        .forEach(path -> {
                            String fileName = path.getFileName().toString();
                            try {
                                int id = Integer.parseInt(fileName.substring(4, fileName.length() - 4));
                                MapItemSavedData data = level.getMapData(new MapId(id));
                                if (data != null) {
                                    String hash = DigestUtils.sha256Hex(data.colors);
                                    hashToId.putIfAbsent(hash, id);
                                }
                            } catch (Exception ignored) {
                            }
                        });
            } catch (IOException e) {
                CanvasContraptions.LOGGER.error("Failed to scan maps for de-duplication", e);
            }
        }

        CanvasContraptions.LOGGER.info("Scan complete. Indexed {} maps in {}ms", hashToId.size(), System.currentTimeMillis() - start);
    }

    public static void clear() {
        hashToId.clear();
        initialized = false;
    }
}
