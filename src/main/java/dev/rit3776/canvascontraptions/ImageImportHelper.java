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

    public static void uploadImage(BufferedImage selectedImage, int columns, int rows, boolean keepAspectRatio, boolean dither, InteractionHand activeHand, String fileName) {
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

        int totalSlices = rows * columns;
        int sliceIndex = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                BufferedImage slice = canvas.getSubimage(c * 128, r * 128, 128, 128);
                byte[] data = convertToMapColors(slice, dither);
                PacketDistributor.sendToServer(new C2SImageUploadPacket(sliceIndex, totalSlices, data, columns, rows,
                        activeHand, fileName));
                sliceIndex++;
            }
        }
    }

    public static byte[] convertToMapColors(BufferedImage image, boolean dither) {
        byte[] colors = new byte[128 * 128];
        if (dither) {
            float[][][] errorBuf = new float[128][128][3];
            for (int y = 0; y < 128; y++) {
                for (int x = 0; x < 128; x++) {
                    int argb = image.getRGB(x, y);
                    if (((argb >> 24) & 0xFF) < 1) {
                        colors[y * 128 + x] = 0;
                        continue;
                    }

                    int rgb = argb;
                    float r = ((rgb >> 16) & 0xFF) + errorBuf[x][y][0];
                    float g = ((rgb >> 8) & 0xFF) + errorBuf[x][y][1];
                    float b = (rgb & 0xFF) + errorBuf[x][y][2];

                    byte idx = MapColorHelper.getNearestMapColor((int) r, (int) g, (int) b);
                    colors[y * 128 + x] = idx;

                    Color actual = MapColorHelper.getColorFromMapByte(idx);
                    float er = r - actual.getRed();
                    float eg = g - actual.getGreen();
                    float eb = b - actual.getBlue();

                    if (x + 1 < 128)
                        diffuse(errorBuf, x + 1, y, er, eg, eb, 7 / 16f);
                    if (y + 1 < 128) {
                        if (x > 0)
                            diffuse(errorBuf, x - 1, y + 1, er, eg, eb, 3 / 16f);
                        diffuse(errorBuf, x, y + 1, er, eg, eb, 5 / 16f);
                        if (x + 1 < 128)
                            diffuse(errorBuf, x + 1, y + 1, er, eg, eb, 1 / 16f);
                    }
                }
            }
        } else {
            for (int y = 0; y < 128; y++) {
                for (int x = 0; x < 128; x++) {
                    int argb = image.getRGB(x, y);
                    if (((argb >> 24) & 0xFF) < 1) {
                        colors[y * 128 + x] = 0;
                    } else {
                        colors[y * 128 + x] = MapColorHelper.getNearestMapColor((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF);
                    }
                }
            }
        }
        return colors;
    }

    private static void diffuse(float[][][] buf, int x, int y, float er, float eg, float eb, float w) {
        buf[x][y][0] += er * w;
        buf[x][y][1] += eg * w;
        buf[x][y][2] += eb * w;
    }
}
