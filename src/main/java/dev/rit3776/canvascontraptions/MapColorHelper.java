package dev.rit3776.canvascontraptions;

import net.minecraft.world.level.material.MapColor;

public class MapColorHelper {
    private static byte[] lut = null;

    private static void initLut() {
        if (lut != null) return;
        lut = new byte[32 * 32 * 32];

        // Cache all available map colors in Lab space
        int colorCount = 64 * 4;
        float[][] mapColorLabs = new float[colorCount][3];
        boolean[] valid = new boolean[colorCount];

        for (int i = 0; i < 64; i++) {
            MapColor mc = MapColor.byId(i);
            if (mc == MapColor.NONE && i > 0) continue;
            for (int shade = 0; shade < 4; shade++) {
                int idx = i * 4 + shade;
                int argb = 0xFF000000 | getVariant(mc.col, shade);
                mapColorLabs[idx] = rgbToLab(argb);
                valid[idx] = true;
            }
        }

        // Fill 3D-LUT
        for (int r = 0; r < 32; r++) {
            for (int g = 0; g < 32; g++) {
                for (int b = 0; b < 32; b++) {
                    float[] targetLab = rgbToLab(0xFF000000 | (r << 3 | 4) << 16 | (g << 3 | 4) << 8 | (b << 3 | 4));
                    
                    float bestDist = Float.MAX_VALUE;
                    byte bestIdx = 0;

                    for (int i = 0; i < colorCount; i++) {
                        if (!valid[i]) continue;
                        float dist = labDistanceSq(targetLab, mapColorLabs[i]);
                        if (dist < bestDist) {
                            bestDist = dist;
                            bestIdx = (byte) i;
                        }
                    }
                    lut[r * 1024 + g * 32 + b] = bestIdx;
                }
            }
        }
    }

    public static byte findNearestIndex(int argb) {
        int a = (argb >> 24) & 0xFF;
        if (a < 1) return 0;
        
        initLut();
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        
        return lut[(r >> 3) * 1024 + (g >> 3) * 32 + (b >> 3)];
    }

    // Direct RGB access for dithering without bit shifting if needed
    public static byte findNearestIndex(int r, int g, int b) {
        initLut();
        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));
        return lut[(r >> 3) * 1024 + (g >> 3) * 32 + (b >> 3)];
    }

    private static float labDistanceSq(float[] lab1, float[] lab2) {
        float dl = lab1[0] - lab2[0];
        float da = lab1[1] - lab2[1];
        float db = lab1[2] - lab2[2];
        return dl * dl + da * da + db * db;
    }

    private static float[] rgbToLab(int argb) {
        float r = ((argb >> 16) & 0xFF) / 255.0f;
        float g = ((argb >> 8) & 0xFF) / 255.0f;
        float b = (argb & 0xFF) / 255.0f;

        // Gamma correction (sRGB inverse)
        r = (r > 0.04045f) ? (float) Math.pow((r + 0.055f) / 1.055f, 2.4) : r / 12.92f;
        g = (g > 0.04045f) ? (float) Math.pow((g + 0.055f) / 1.055f, 2.4) : g / 12.92f;
        b = (b > 0.04045f) ? (float) Math.pow((b + 0.055f) / 1.055f, 2.4) : b / 12.92f;

        // Linear RGB to XYZ (D65)
        float x = r * 0.4124f + g * 0.3576f + b * 0.1805f;
        float y = r * 0.2126f + g * 0.7152f + b * 0.0722f;
        float z = r * 0.0193f + g * 0.1192f + b * 0.9505f;

        // XYZ to Lab
        x /= 0.95047f;
        y /= 1.00000f;
        z /= 1.08883f;

        x = (x > 0.008856f) ? (float) Math.pow(x, 1.0/3.0) : (7.787f * x) + (16.0f/116.0f);
        y = (y > 0.008856f) ? (float) Math.pow(y, 1.0/3.0) : (7.787f * y) + (16.0f/116.0f);
        z = (z > 0.008856f) ? (float) Math.pow(z, 1.0/3.0) : (7.787f * z) + (16.0f/116.0f);

        return new float[] {
            (116.0f * y) - 16.0f,
            500.0f * (x - y),
            200.0f * (y - z)
        };
    }

    private static int getVariant(int rgb, int shade) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        
        int mult = switch (shade) {
            case 0 -> 180;
            case 1 -> 220;
            case 2 -> 255;
            case 3 -> 135;
            default -> 220;
        };
        
        r = r * mult / 255;
        g = g * mult / 255;
        b = b * mult / 255;
        
        return (r << 16) | (g << 8) | b;
    }

    public static int getRgbColor(byte index) {
        int i = (index & 255) / 4;
        int shade = index & 3;
        MapColor mc = MapColor.byId(i);
        if (mc == MapColor.NONE) return 0;
        
        return 0xFF000000 | getVariant(mc.col, shade);
    }
}
