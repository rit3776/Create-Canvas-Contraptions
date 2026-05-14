package dev.rit3776.canvascontraptions;

import dev.rit3776.canvascontraptions.network.C2SRequestMapDataPacket;
import dev.rit3776.canvascontraptions.network.CCNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.Map;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

public class ClientMapCache {
    private static final Map<Integer, MapItemSavedData> mapCache = new ConcurrentHashMap<>();
    private static final Map<Integer, Long> lastRequestTime = new ConcurrentHashMap<>();

    private static final Map<Integer, DynamicTexture> textureCache = new ConcurrentHashMap<>();
    private static final Map<Integer, ResourceLocation> textureLocations = new ConcurrentHashMap<>();
    private static final Map<Integer, byte[]> colorCache = new ConcurrentHashMap<>();
    private static final int MAX_TEXTURES = 256;
    private static final Map<Integer, ResourceLocation> textureLru = Collections
            .synchronizedMap(new LinkedHashMap<>(16, 0.75f, true));
    private static final Map<Integer, Boolean> hasDataMap = new ConcurrentHashMap<>();

    public static MapItemSavedData getOrCreate(int mapId, Level level) {
        MapItemSavedData data = mapCache.computeIfAbsent(mapId, id -> {
            requestIfMissing(id);
            hasDataMap.put(id, false);
            return MapItemSavedData.createForClient((byte) 3, false, level.dimension());
        });
        colorCache.computeIfAbsent(mapId, id -> new byte[data.colors.length]);
        return data;
    }

    public static boolean hasData(int mapId) {
        return hasDataMap.getOrDefault(mapId, false);
    }

    public static ResourceLocation getTextureLocation(int mapId, Level level) {
        getOrCreate(mapId, level);

        ResourceLocation existing = textureLocations.get(mapId);
        if (existing != null) {
            textureLru.put(mapId, existing);
            return existing;
        }

        synchronized (textureLru) {
            if (textureLru.size() >= MAX_TEXTURES) {
                Iterator<Integer> it = textureLru.keySet().iterator();
                if (it.hasNext()) {
                    Integer oldest = it.next();
                    ResourceLocation oldLoc = textureLru.remove(oldest);
                    textureLocations.remove(oldest);
                    DynamicTexture oldTex = textureCache.remove(oldest);
                    if (oldLoc != null) {
                        try {
                            Minecraft.getInstance().getTextureManager().release(oldLoc);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }

            DynamicTexture texture = new DynamicTexture(128, 128, true);
            textureCache.put(mapId, texture);
            updateTexture(mapId);
            ResourceLocation loc = Minecraft.getInstance().getTextureManager().register("canvas_map_" + mapId, texture);
            textureLocations.put(mapId, loc);
            textureLru.put(mapId, loc);
            return loc;
        }
    }

    private static void updateTexture(int mapId) {
        DynamicTexture texture = textureCache.get(mapId);
        if (texture == null)
            return;

        byte[] colors = colorCache.get(mapId);
        if (colors == null) {
            MapItemSavedData data = mapCache.get(mapId);
            if (data == null)
                return;
            colors = Arrays.copyOf(data.colors, data.colors.length);
            colorCache.put(mapId, colors);
        }

        for (int i = 0; i < 128 * 128 && i < colors.length; i++) {
            int color = MapColorHelper.getRgbColor(colors[i]);
            texture.getPixels().setPixelRGBA(i % 128, i / 128, colorToAbgr(color));
        }
        texture.upload();
    }

    private static int colorToAbgr(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    public static void update(int mapId, byte[] colors, Level level) {
        MapItemSavedData data = getOrCreate(mapId, level);
        if (data.colors.length == colors.length) {
            byte[] copy = Arrays.copyOf(colors, colors.length);

            colorCache.put(mapId, copy);

            boolean any = false;
            for (byte b : copy) {
                if (b != 0) {
                    any = true;
                    break;
                }
            }
            hasDataMap.put(mapId, any);

            Minecraft.getInstance().execute(() -> {
                MapItemSavedData d = getOrCreate(mapId, level);
                if (d != null && d.colors.length == copy.length) {
                    System.arraycopy(copy, 0, d.colors, 0, copy.length);
                }
                updateTexture(mapId);
            });
        }
    }

    public static void reset() {
        int count = textureLocations.size();
        if (count > 0) {
            CanvasContraptions.LOGGER.info("Clearing ClientMapCache and releasing {} textures from VRAM", count);
            Minecraft mc = Minecraft.getInstance();
            textureLocations.values().forEach(location -> {
                mc.getTextureManager().release(location);
            });
        }

        mapCache.clear();
        textureCache.clear();
        textureLocations.clear();
        colorCache.clear();
        lastRequestTime.clear();
        hasDataMap.clear();
        textureLru.clear();
    }

    private static void requestIfMissing(int mapId) {
        long now = System.currentTimeMillis();
        if (now - lastRequestTime.getOrDefault(mapId, 0L) > 10000) {
            lastRequestTime.put(mapId, now);
            CCNetwork.sendToServer(new C2SRequestMapDataPacket(mapId));
        }
    }
}
