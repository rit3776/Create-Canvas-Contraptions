package dev.rit3776.canvascontraptions;

import net.minecraft.world.level.material.MapColor;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class MapColorHelper {
    public enum ColorMode {
        LAB,
        RGB
    }

    private static final int LUT_SIZE = 32;
    private static final byte[] COLOR_LUT_LAB = new byte[LUT_SIZE * LUT_SIZE * LUT_SIZE];
    private static final byte[] COLOR_LUT_RGB = new byte[LUT_SIZE * LUT_SIZE * LUT_SIZE];
    private static boolean lutInitialized = false;

    public static byte getNearestMapColor(int r, int g, int b, ColorMode colorMode) {
        if (!lutInitialized) {
            initLUT();
        }
        r = Math.clamp(r, 0, 255);
        g = Math.clamp(g, 0, 255);
        b = Math.clamp(b, 0, 255);
        int ir = (r * (LUT_SIZE - 1)) / 255;
        int ig = (g * (LUT_SIZE - 1)) / 255;
        int ib = (b * (LUT_SIZE - 1)) / 255;
        int index = ir * LUT_SIZE * LUT_SIZE + ig * LUT_SIZE + ib;
        return (colorMode == ColorMode.RGB) ? COLOR_LUT_RGB[index] : COLOR_LUT_LAB[index];
    }

    public static byte getNearestMapColor(int r, int g, int b) {
        return getNearestMapColor(r, g, b, ColorMode.LAB);
    }

    private static int fixColor(int val) {
        int a = (val >> 24) & 0xFF;
        int b = (val >> 16) & 0xFF;
        int g = (val >> 8) & 0xFF;
        int r = val & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static synchronized void initLUT() {
        if (lutInitialized)
            return;

        List<MapColorData> mapColors = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            MapColor mapColor = MapColor.byId(i);
            if (mapColor != MapColor.NONE) {
                for (int j = 0; j < 4; j++) {
                    if (i == 0 && j == 0)
                        continue;
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
                    int idx = r * LUT_SIZE * LUT_SIZE + g * LUT_SIZE + b;
                    COLOR_LUT_LAB[idx] = findNearestLab2000(cr, cg, cb, mapColors);
                    COLOR_LUT_RGB[idx] = findNearestRGB(cr, cg, cb, mapColors);
                }
            }
        }
        lutInitialized = true;
    }

    private static byte findNearestLab2000(int r, int g, int b, List<MapColorData> mapColors) {
        double minDiff = Double.MAX_VALUE;
        byte bestId = 0;
        double[] lab1 = rgbToLab(r, g, b);

        for (MapColorData data : mapColors) {
            double[] lab2 = rgbToLab(data.color.getRed(), data.color.getGreen(), data.color.getBlue());
            double diff = deltaE2000(lab1, lab2);
            if (diff < minDiff) {
                minDiff = diff;
                bestId = data.id;
            }
        }
        return bestId;
    }

    private static byte findNearestRGB(int r, int g, int b, List<MapColorData> mapColors) {
        double minDiff = Double.MAX_VALUE;
        byte bestId = 0;
        for (MapColorData data : mapColors) {
            int dr = r - data.color.getRed();
            int dg = g - data.color.getGreen();
            int db = b - data.color.getBlue();
            double diff = dr * dr + dg * dg + db * db;
            if (diff < minDiff) {
                minDiff = diff;
                bestId = data.id;
            }
        }
        return bestId;
    }

    private record MapColorData(byte id, Color color) {
    }

    private static double[] rgbToLab(int r, int g, int b) {
        double lr = pivotRgb(r / 255.0);
        double lg = pivotRgb(g / 255.0);
        double lb = pivotRgb(b / 255.0);

        double x = lr * 0.4124 + lg * 0.3576 + lb * 0.1805;
        double y = lr * 0.2126 + lg * 0.7152 + lb * 0.0722;
        double z = lr * 0.0193 + lg * 0.1192 + lb * 0.9505;

        x /= 0.95047;
        y /= 1.00000;
        z /= 1.08883;

        x = pivotXyz(x);
        y = pivotXyz(y);
        z = pivotXyz(z);

        return new double[] {
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

    private static double deltaE2000(double[] lab1, double[] lab2) {
        double l1 = lab1[0], a1 = lab1[1], b1 = lab1[2];
        double l2 = lab2[0], a2 = lab2[1], b2 = lab2[2];

        double c1 = Math.hypot(a1, b1);
        double c2 = Math.hypot(a2, b2);
        double cBar = (c1 + c2) / 2.0;

        double cBar7 = Math.pow(cBar, 7);
        double g = 0.5 * (1.0 - Math.sqrt(cBar7 / (cBar7 + 6103515625.0))); // 25^7 = 6103515625

        double a1Prime = (1.0 + g) * a1;
        double a2Prime = (1.0 + g) * a2;

        double c1Prime = Math.hypot(a1Prime, b1);
        double c2Prime = Math.hypot(a2Prime, b2);

        double h1Prime = Math.toDegrees(Math.atan2(b1, a1Prime));
        if (h1Prime < 0) h1Prime += 360.0;
        double h2Prime = Math.toDegrees(Math.atan2(b2, a2Prime));
        if (h2Prime < 0) h2Prime += 360.0;

        double deltaLPrime = l2 - l1;
        double deltaCPrime = c2Prime - c1Prime;

        double deltahPrime;
        if (c1Prime * c2Prime == 0) {
            deltahPrime = 0;
        } else if (Math.abs(h2Prime - h1Prime) <= 180.0) {
            deltahPrime = h2Prime - h1Prime;
        } else if (h2Prime - h1Prime > 180.0) {
            deltahPrime = h2Prime - h1Prime - 360.0;
        } else {
            deltahPrime = h2Prime - h1Prime + 360.0;
        }

        double deltaHPrime = 2.0 * Math.sqrt(c1Prime * c2Prime) * Math.sin(Math.toRadians(deltahPrime / 2.0));

        double lBarPrime = (l1 + l2) / 2.0;
        double cBarPrime = (c1Prime + c2Prime) / 2.0;

        double hBarPrime;
        if (c1Prime * c2Prime == 0) {
            hBarPrime = h1Prime + h2Prime;
        } else if (Math.abs(h1Prime - h2Prime) <= 180.0) {
            hBarPrime = (h1Prime + h2Prime) / 2.0;
        } else if (h1Prime + h2Prime < 360.0) {
            hBarPrime = (h1Prime + h2Prime + 360.0) / 2.0;
        } else {
            hBarPrime = (h1Prime + h2Prime - 360.0) / 2.0;
        }

        double t = 1.0 - 0.17 * Math.cos(Math.toRadians(hBarPrime - 30.0))
                       + 0.24 * Math.cos(Math.toRadians(2.0 * hBarPrime))
                       + 0.32 * Math.cos(Math.toRadians(3.0 * hBarPrime + 6.0))
                       - 0.20 * Math.cos(Math.toRadians(4.0 * hBarPrime - 63.0));

        double deltaTheta = 30.0 * Math.exp(-Math.pow((hBarPrime - 275.0) / 25.0, 2));
        double cBarPrime7 = Math.pow(cBarPrime, 7);
        double rC = 2.0 * Math.sqrt(cBarPrime7 / (cBarPrime7 + 6103515625.0));

        double l50 = lBarPrime - 50.0;
        double sL = 1.0 + (0.015 * l50 * l50) / Math.sqrt(20.0 + l50 * l50);
        double sC = 1.0 + 0.045 * cBarPrime;
        double sH = 1.0 + 0.015 * cBarPrime * t;
        double rT = -Math.sin(Math.toRadians(2.0 * deltaTheta)) * rC;

        double termL = deltaLPrime / sL;
        double termC = deltaCPrime / sC;
        double termH = deltaHPrime / sH;

        return Math.sqrt(termL * termL + termC * termC + termH * termH + rT * termC * termH);
    }

    public static Color getColorFromMapByte(byte colorByte) {
        int id = (colorByte & 255) / 4;
        int shade = colorByte & 3;
        MapColor mapColor = MapColor.byId(id);
        return new Color(fixColor(mapColor.calculateRGBColor(MapColor.Brightness.values()[shade])));
    }
}

