package dev.rit3776.canvascontraptions;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerMapManager {
    private static final Map<String, Integer> hashToId = new ConcurrentHashMap<>();
    private static boolean initialized = false;

    //Gets an existing map ID for the given pixel data, or creates a new one if it doesn't exist.
    public static int getOrCreateMapId(ServerLevel level, byte[] colors) {
        if (!initialized) {
            initialize(level);
        }

        String hash = DigestUtils.sha256Hex(colors);
        Integer existingId = hashToId.get(hash);

        if (existingId != null) {
            // Verify the map still exists in the world
            if (level.getMapData("map_" + existingId) != null) {
                return existingId;
            } else {
                hashToId.remove(hash);
            }
        }

        // Create new map
        int newId = level.getFreeMapId();
        MapItemSavedData data = MapItemSavedData.createFresh(0.0, 0.0, (byte) 3, false, false, level.dimension());
        System.arraycopy(colors, 0, data.colors, 0, Math.min(colors.length, data.colors.length));
        level.setMapData("map_" + newId, data);

        hashToId.put(hash, newId);
        return newId;
    }

    private static synchronized void initialize(ServerLevel level) {
        if (initialized) return;
        initialized = true;

        CanvasContraptions.LOGGER.info("Scanning existing maps for de-duplication...");
        long start = System.currentTimeMillis();
        int count = 0;

        java.nio.file.Path dataFolder = level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data");
        if (java.nio.file.Files.exists(dataFolder)) {
            try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.list(dataFolder)) {
                stream.filter(path -> path.getFileName().toString().startsWith("map_") && path.getFileName().toString().endsWith(".dat"))
                        .forEach(path -> {
                            String fileName = path.getFileName().toString();
                            try {
                                int id = Integer.parseInt(fileName.substring(4, fileName.length() - 4));
                                MapItemSavedData data = level.getMapData("map_" + id);
                                if (data != null) {
                                    String hash = DigestUtils.sha256Hex(data.colors);
                                    hashToId.putIfAbsent(hash, id);
                                }
                            } catch (Exception ignored) {
                            }
                        });
            } catch (java.io.IOException e) {
                CanvasContraptions.LOGGER.error("Failed to scan maps for de-duplication", e);
            }
        }

        CanvasContraptions.LOGGER.info("Scan complete. Indexed {} maps in {}ms", hashToId.size(), System.currentTimeMillis() - start);
    }

    //Clears the cache. Should be called when the server stops or world unloads to prevent memory leaks or stale data in singleplayer world swapping.
    public static void clear() {
        hashToId.clear();
    }
}
