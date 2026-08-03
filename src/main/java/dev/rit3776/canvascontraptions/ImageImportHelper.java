package dev.rit3776.canvascontraptions;

import com.mojang.blaze3d.platform.NativeImage;
import dev.rit3776.canvascontraptions.network.C2SImageUploadPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.function.BiConsumer;

public class ImageImportHelper {

    public enum DitherMode {
        NONE,
        ATKINSON,
        STUCKI
    }

    public static String formatImageName(String rawFileName, MapColorHelper.ColorMode colorMode, DitherMode ditherMode, int columns, int rows) {
        if (rawFileName == null) rawFileName = "";
        String truncated = rawFileName;
        if (truncated.length() > 16) {
            truncated = truncated.substring(0, 16) + "...";
        }
        String ditherStr = switch (ditherMode) {
            case NONE -> Component.translatable("gui.canvascontraptions.dither.none").getString();
            case ATKINSON -> Component.translatable("gui.canvascontraptions.dither.atkinson").getString();
            case STUCKI -> Component.translatable("gui.canvascontraptions.dither.stucki").getString();
        };
        String colorModeStr = switch (colorMode) {
            case LAB -> Component.translatable("gui.canvascontraptions.color_mode.lab").getString();
            case RGB -> Component.translatable("gui.canvascontraptions.color_mode.rgb").getString();
        };
        String sizeStr = columns + "x" + rows;
        return truncated + " - " + ditherStr + " - " + colorModeStr + " - " + sizeStr;
    }

    public static void selectFile(BiConsumer<BufferedImage, File> callback) {
        new Thread(() -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer filters = stack.mallocPointer(3);
                filters.put(stack.UTF8("*.png"));
                filters.put(stack.UTF8("*.jpg"));
                filters.put(stack.UTF8("*.jpeg"));
                filters.flip();

                String path = TinyFileDialogs.tinyfd_openFileDialog(
                        Component.translatable("gui.canvascontraptions.dialog.select_image").getString(),
                        "", filters,
                        Component.translatable("gui.canvascontraptions.dialog.image_files").getString(),
                        false);
                if (path != null) {
                    try {
                        File file = new File(path);
                        BufferedImage img = ImageIO.read(file);
                        Minecraft.getInstance().execute(() -> callback.accept(img, file));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public static DynamicTexture setupPreviewTexture(BufferedImage img, DynamicTexture oldTexture) {
        if (oldTexture != null) {
            oldTexture.close();
        }
        NativeImage nativeImage = new NativeImage(img.getWidth(), img.getHeight(), false);
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int argb = img.getRGB(x, y);
                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                nativeImage.setPixelRGBA(x, y, (a << 24) | (b << 16) | (g << 8) | r);
            }
        }
        return new DynamicTexture(nativeImage);
    }

    public static void uploadImage(BufferedImage selectedImage, int columns, int rows, boolean keepAspectRatio, MapColorHelper.ColorMode colorMode, DitherMode ditherMode, InteractionHand activeHand, String fileName) {
        if (selectedImage == null)
            return;

        int canvasWidth = columns * 128;
        int canvasHeight = rows * 128;
        BufferedImage canvas = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        if (keepAspectRatio) {
            double scale = Math.min((double) canvasWidth / selectedImage.getWidth(),
                    (double) canvasHeight / selectedImage.getHeight());
            int sw = (int) (selectedImage.getWidth() * scale);
            int sh = (int) (selectedImage.getHeight() * scale);
            int ox = (canvasWidth - sw) / 2;
            int oy = (canvasHeight - sh) / 2;
            g.drawImage(selectedImage, ox, oy, sw, sh, null);
        } else {
            g.drawImage(selectedImage, 0, 0, canvasWidth, canvasHeight, null);
        }
        g.dispose();

        // 1. Convert and dither the entire canvas BEFORE splitting into tiles
        byte[] fullMapColors = convertToMapColors(canvas, colorMode, ditherMode);

        // 2. Slice full map color array into 128x128 tile arrays
        int totalSlices = rows * columns;
        int sliceIndex = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                byte[] sliceData = new byte[128 * 128];
                for (int y = 0; y < 128; y++) {
                    int fullY = r * 128 + y;
                    for (int x = 0; x < 128; x++) {
                        int fullX = c * 128 + x;
                        sliceData[y * 128 + x] = fullMapColors[fullY * canvasWidth + fullX];
                    }
                }
                PacketDistributor.sendToServer(new C2SImageUploadPacket(sliceIndex, totalSlices, sliceData, columns, rows,
                        activeHand, fileName));
                sliceIndex++;
            }
        }
    }

    public static byte[] convertToMapColors(BufferedImage image, MapColorHelper.ColorMode colorMode, DitherMode ditherMode) {
        int width = image.getWidth();
        int height = image.getHeight();
        byte[] colors = new byte[width * height];

        if (ditherMode == DitherMode.NONE) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int argb = image.getRGB(x, y);
                    if (((argb >> 24) & 0xFF) < 1) {
                        colors[y * width + x] = 0;
                    } else {
                        colors[y * width + x] = MapColorHelper.getNearestMapColor(
                                (argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, colorMode);
                    }
                }
            }
        } else {
            float[][][] errorBuf = new float[height][width][3];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int argb = image.getRGB(x, y);
                    if (((argb >> 24) & 0xFF) < 1) {
                        colors[y * width + x] = 0;
                        continue;
                    }

                    float r = ((argb >> 16) & 0xFF) + errorBuf[y][x][0];
                    float g = ((argb >> 8) & 0xFF) + errorBuf[y][x][1];
                    float b = (argb & 0xFF) + errorBuf[y][x][2];

                    byte idx = MapColorHelper.getNearestMapColor((int) r, (int) g, (int) b, colorMode);
                    colors[y * width + x] = idx;

                    Color actual = MapColorHelper.getColorFromMapByte(idx);
                    float er = r - actual.getRed();
                    float eg = g - actual.getGreen();
                    float eb = b - actual.getBlue();

                    if (ditherMode == DitherMode.ATKINSON) {
                        // Atkinson dithering: 6 neighbors, 1/8 factor each
                        diffuse(errorBuf, width, height, x + 1, y,     er, eg, eb, 1 / 8f);
                        diffuse(errorBuf, width, height, x + 2, y,     er, eg, eb, 1 / 8f);
                        diffuse(errorBuf, width, height, x - 1, y + 1, er, eg, eb, 1 / 8f);
                        diffuse(errorBuf, width, height, x,     y + 1, er, eg, eb, 1 / 8f);
                        diffuse(errorBuf, width, height, x + 1, y + 1, er, eg, eb, 1 / 8f);
                        diffuse(errorBuf, width, height, x,     y + 2, er, eg, eb, 1 / 8f);
                    } else if (ditherMode == DitherMode.STUCKI) {
                        // Stucki dithering: 12 neighbors, divisor 42
                        diffuse(errorBuf, width, height, x + 1, y,     er, eg, eb, 8 / 42f);
                        diffuse(errorBuf, width, height, x + 2, y,     er, eg, eb, 4 / 42f);

                        diffuse(errorBuf, width, height, x - 2, y + 1, er, eg, eb, 2 / 42f);
                        diffuse(errorBuf, width, height, x - 1, y + 1, er, eg, eb, 4 / 42f);
                        diffuse(errorBuf, width, height, x,     y + 1, er, eg, eb, 8 / 42f);
                        diffuse(errorBuf, width, height, x + 1, y + 1, er, eg, eb, 4 / 42f);
                        diffuse(errorBuf, width, height, x + 2, y + 1, er, eg, eb, 2 / 42f);

                        diffuse(errorBuf, width, height, x - 2, y + 2, er, eg, eb, 1 / 42f);
                        diffuse(errorBuf, width, height, x - 1, y + 2, er, eg, eb, 2 / 42f);
                        diffuse(errorBuf, width, height, x,     y + 2, er, eg, eb, 4 / 42f);
                        diffuse(errorBuf, width, height, x + 1, y + 2, er, eg, eb, 2 / 42f);
                        diffuse(errorBuf, width, height, x + 2, y + 2, er, eg, eb, 1 / 42f);
                    }
                }
            }
        }
        return colors;
    }

    private static void diffuse(float[][][] buf, int width, int height, int x, int y, float er, float eg, float eb, float w) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            buf[y][x][0] += er * w;
            buf[y][x][1] += eg * w;
            buf[y][x][2] += eb * w;
        }
    }
}

