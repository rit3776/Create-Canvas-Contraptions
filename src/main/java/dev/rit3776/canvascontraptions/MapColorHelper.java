package dev.rit3776.canvascontraptions;

import net.minecraft.world.level.material.MapColor;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class MapColorHelper {
    private static final int LUT_SIZE = 32;
    private static final byte[] COLOR_LUT = new byte[LUT_SIZE * LUT_SIZE * LUT_SIZE];
    private static boolean lutInitialized = false;

    public static byte getNearestMapColor(int r, int g, int b) {
        if (!lutInitialized) {
            initLUT();
        }
        r = Math.clamp(r, 0, 255);
        g = Math.clamp(g, 0, 255);
        b = Math.clamp(b, 0, 255);
        int ir = (r * (LUT_SIZE - 1)) / 255;
        int ig = (g * (LUT_SIZE - 1)) / 255;
        int ib = (b * (LUT_SIZE - 1)) / 255;
        return COLOR_LUT[ir * LUT_SIZE * LUT_SIZE + ig * LUT_SIZE + ib];
    }

    private static int fixColor(int val) {
        // Minecraft 1.21.1's calculateRGBColor returns colors in a format where R and B are swapped compared to standard ARGB
        int a = (val >> 24) & 0xFF;
        int b = (val >> 16) & 0xFF; // This is actually Blue in the returned value
        int g = (val >> 8) & 0xFF;
        int r = val & 0xFF;         // This is actually Red in the returned value
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static synchronized void initLUT() {
        if (lutInitialized) return;

        List<MapColorData> mapColors = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            MapColor mapColor = MapColor.byId(i);
            if (mapColor != MapColor.NONE) {
                for (int j = 0; j < 4; j++) {
                    if (i == 0 && j == 0) continue; // Skip transparency
                    int rgb = fixColor(mapColor.calculateRGBColor(MapColor.Brightness.values()[j]));
                    mapColors.add(new MapColorData((byte) (i * 4 + j), new Color(rgb)));
                }
            }
        }

        for (int r = 0; r < LUT_SIZE; r++) {
            for (int g = 0; g < LUT_SIZE; g++) {
                for (int b = 0; b < LUT_SIZE; b++) {
                    int cr = (r * 255) / (LUT_SIZE - 1);
                    int cg = (g * 255) / (LUT_SIZE - 1);
                    int cb = (b * 255) / (LUT_SIZE - 1);
                    COLOR_LUT[r * LUT_SIZE * LUT_SIZE + g * LUT_SIZE + b] = findNearest(cr, cg, cb, mapColors);
                }
            }
        }
        lutInitialized = true;
    }

    private static byte findNearest(int r, int g, int b, List<MapColorData> mapColors) {
        double minDiff = Double.MAX_VALUE;
        byte bestId = 0;

        double[] lab1 = rgbToLab(r, g, b);

        for (MapColorData data : mapColors) {
            double[] lab2 = rgbToLab(data.color.getRed(), data.color.getGreen(), data.color.getBlue());
            double diff = deltaE(lab1, lab2);
            if (diff < minDiff) {
                minDiff = diff;
                bestId = data.id;
            }
        }
        return bestId;
    }

    private record MapColorData(byte id, Color color) {}

    // LAB color space conversion and DeltaE for better color matching
    private static double[] rgbToLab(int r, int g, int b) {
        double lr = pivotRgb(r / 255.0);
        double lg = pivotRgb(g / 255.0);
        double lb = pivotRgb(b / 255.0);

        double x = lr * 0.4124 + lg * 0.3576 + lb * 0.1805;
        double y = lr * 0.2126 + lg * 0.7152 + lb * 0.0722;
        double z = lr * 0.0193 + lg * 0.1192 + lb * 0.9505;

        // D65 illuminant
        x /= 0.95047;
        y /= 1.00000;
        z /= 1.08883;

        x = pivotXyz(x);
        y = pivotXyz(y);
        z = pivotXyz(z);

        return new double[]{
                Math.max(0, 116 * y - 16),
                500 * (x - y),
                200 * (y - z)
        };
    }

    private static double pivotRgb(double n) {
        return (n > 0.04045) ? Math.pow((n + 0.055) / 1.055, 2.4) : (n / 12.92);
    }

    private static double pivotXyz(double n) {
        return (n > 0.008856) ? Math.pow(n, 1.0 / 3.0) : (7.787 * n + 16.0 / 116.0);
    }

    private static double deltaE(double[] lab1, double[] lab2) {
        return Math.sqrt(Math.pow(lab1[0] - lab2[0], 2) + Math.pow(lab1[1] - lab2[1], 2) + Math.pow(lab1[2] - lab2[2], 2));
    }

    public static Color getColorFromMapByte(byte colorByte) {
        int id = (colorByte & 255) / 4;
        int shade = colorByte & 3;
        MapColor mapColor = MapColor.byId(id);
        return new Color(fixColor(mapColor.calculateRGBColor(MapColor.Brightness.values()[shade])));
    }
}
