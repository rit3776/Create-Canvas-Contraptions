package dev.rit3776.canvascontraptions;

import com.mojang.blaze3d.platform.NativeImage;
import dev.rit3776.canvascontraptions.network.C2SImageUploadPacket;
import dev.rit3776.canvascontraptions.network.C2SSelectTilePacket;
import dev.rit3776.canvascontraptions.network.CCNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DraftingGUI extends Screen {
    private BufferedImage selectedImage;
    private String selectedFileName = "";
    private int columns = 1;
    private int rows = 1;
    private boolean dither = true;
    private boolean keepAspectRatio = true;

    private DynamicTexture previewTexture;
    private ResourceLocation previewLocation;

    private final InteractionHand activeHand;
    private final Mode mode;
    private int[] mapIds;
    private int selectedIndexOnStack = 0;

    private enum Mode {
        BLANK, FILLED, TABLET
    }

    public DraftingGUI(Component title, InteractionHand hand, Mode mode) {
        super(title);
        this.activeHand = hand;
        this.mode = mode;
        if (Minecraft.getInstance().player != null) {
            ItemStack stack = Minecraft.getInstance().player.getItemInHand(hand);
            if (stack.hasTag()) {
                this.mapIds = stack.getTag().getIntArray("MapIDs");
                this.selectedIndexOnStack = stack.getTag().getInt("SelectedIndex");

                if (mode == Mode.FILLED) {
                    if (stack.getTag().contains("Width")) {
                        this.columns = stack.getTag().getInt("Width");
                    }
                    if (stack.getTag().contains("Height")) {
                        this.rows = stack.getTag().getInt("Height");
                    }
                }
            }
        }
    }

    public static void openBlank(InteractionHand hand) {
        Minecraft.getInstance().setScreen(
                new DraftingGUI(Component.translatable("gui.canvascontraptions.drafting_paper.new"), hand, Mode.BLANK));
    }

    public static void openFilled(InteractionHand hand) {
        Minecraft.getInstance()
                .setScreen(new DraftingGUI(Component.translatable("gui.canvascontraptions.drafting_paper.selection"),
                        hand, Mode.FILLED));
    }

    public static void openTablet(InteractionHand hand) {
        Minecraft.getInstance().setScreen(new DraftingGUI(
                Component.translatable("gui.canvascontraptions.drafting_tablet.title"), hand, Mode.TABLET));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        super.init();

        int bx = 10;
        int bw = 100;

        if (mode == Mode.BLANK || mode == Mode.TABLET) {
            if (selectedImage == null) {
                this.addRenderableWidget(Button
                        .builder(Component.translatable("gui.canvascontraptions.button.select_file"), b -> selectFile())
                        .bounds(bx, 40, bw, 20)
                        .build());
                this.addRenderableWidget(Button
                        .builder(Component.translatable("gui.canvascontraptions.button.close"), b -> this.onClose())
                        .bounds(bx, 65, bw, 20).build());
            } else {
                this.addRenderableWidget(
                        Button.builder(Component.translatable("gui.canvascontraptions.button.cols_minus"), b -> {
                            columns = Math.max(1, columns - 1);
                            init();
                        }).bounds(bx, 40, bw / 2 - 2, 20).build());
                this.addRenderableWidget(
                        Button.builder(Component.translatable("gui.canvascontraptions.button.cols_plus"), b -> {
                            columns++;
                            init();
                        }).bounds(bx + bw / 2 + 2, 40, bw / 2 - 2, 20).build());

                this.addRenderableWidget(
                        Button.builder(Component.translatable("gui.canvascontraptions.button.rows_minus"), b -> {
                            rows = Math.max(1, rows - 1);
                            init();
                        }).bounds(bx, 65, bw / 2 - 2, 20).build());
                this.addRenderableWidget(
                        Button.builder(Component.translatable("gui.canvascontraptions.button.rows_plus"), b -> {
                            rows++;
                            init();
                        }).bounds(bx + bw / 2 + 2, 65, bw / 2 - 2, 20).build());

                this.addRenderableWidget(Button.builder(Component.translatable("gui.canvascontraptions.button.dither",
                        Component
                                .translatable(
                                        dither ? "gui.canvascontraptions.label.on" : "gui.canvascontraptions.label.off")
                                .getString()),
                        b -> {
                            dither = !dither;
                            init();
                        }).bounds(bx, 95, bw, 20).build());

                this.addRenderableWidget(Button.builder(Component.translatable("gui.canvascontraptions.button.aspect",
                        Component.translatable(keepAspectRatio ? "gui.canvascontraptions.label.fit"
                                : "gui.canvascontraptions.label.stretch").getString()),
                        b -> {
                            keepAspectRatio = !keepAspectRatio;
                            init();
                        }).bounds(bx, 120, bw, 20).build());

                this.addRenderableWidget(
                        Button.builder(Component.translatable("gui.canvascontraptions.button.import"), b -> submit())
                                .bounds(bx, height - 55, bw, 20).build());
                this.addRenderableWidget(
                        Button.builder(Component.translatable("gui.canvascontraptions.button.cancel"), b -> {
                            selectedImage = null;
                            init();
                        }).bounds(bx, height - 30, bw, 20).build());
            }
        }
    }

    private void selectFile() {
        ImageImportHelper.selectFile((img, file) -> {
            this.selectedImage = img;
            this.selectedFileName = file.getName();
            setupPreviewTexture(img);
            init();
        });
    }

    private void setupPreviewTexture(BufferedImage img) {
        this.previewTexture = ImageImportHelper.setupPreviewTexture(img, this.previewTexture);
        this.previewLocation = Minecraft.getInstance().getTextureManager().register("canvascontraptions_preview", previewTexture);
    }

    @Override
    public void onClose() {
        if (previewTexture != null) {
            previewTexture.close();
            previewTexture = null;
        }
        super.onClose();
    }

    private void submit() {
        if (selectedImage == null)
            return;
        ImageImportHelper.uploadImage(selectedImage, columns, rows, keepAspectRatio, dither, activeHand, selectedFileName);
        this.onClose();
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mode == Mode.FILLED && mapIds != null) {
            int size = 200;
            int tileSize = Math.min(size / Math.max(1, columns), size / Math.max(1, rows));
            int gridWidth = columns * tileSize;
            int gridHeight = rows * tileSize;
            int startX = width / 2 - gridWidth / 2;
            int startY = height / 2 - gridHeight / 2;

            if (mouseX >= startX && mouseX < startX + gridWidth && mouseY >= startY && mouseY < startY + gridHeight) {
                int col = (int) ((mouseX - startX) / tileSize);
                int row = (int) ((mouseY - startY) / tileSize);
                if (col >= 0 && col < columns && row >= 0 && row < rows) {
                    int index = row * columns + col;
                    if (index >= 0 && index < mapIds.length) {
                        CCNetwork.sendToServer(new C2SSelectTilePacket(index, activeHand));
                        this.onClose();
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        guiGraphics.drawCenteredString(font, this.title, width / 2, 10, 0xFFFFFF);

        if (mode == Mode.BLANK || mode == Mode.TABLET) {
            int sidebarW = 120;
            int areaY = 30;
            int areaW = width - sidebarW - 10;
            int areaH = height - areaY - 20;

            double gridAspect = (double) columns / rows;
            int gridW, gridH;
            if ((double) areaW / areaH > gridAspect) {
                gridH = areaH;
                gridW = (int) (gridH * gridAspect);
            } else {
                gridW = areaW;
                gridH = (int) (gridW / gridAspect);
            }

            int startX = sidebarW + (areaW - gridW) / 2;
            int startY = areaY + (areaH - gridH) / 2;

            guiGraphics.fill(startX, startY, startX + gridW, startY + gridH, 0xFF111111);
            drawRectOutline(guiGraphics, startX, startY, gridW, gridH, 0xFFFFFFFF);

            if (selectedImage != null && previewLocation != null) {
                if (keepAspectRatio) {
                    double imgAspect = (double) selectedImage.getWidth() / selectedImage.getHeight();
                    int iw, ih;
                    if (gridAspect > imgAspect) {
                        ih = gridH;
                        iw = (int) (ih * imgAspect);
                    } else {
                        iw = gridW;
                        ih = (int) (iw / imgAspect);
                    }
                    int ix = startX + (gridW - iw) / 2;
                    int iy = startY + (gridH - ih) / 2;
                    guiGraphics.blit(previewLocation, ix, iy, 0, 0, iw, ih, iw, ih);
                } else {
                    guiGraphics.blit(previewLocation, startX, startY, 0, 0, gridW, gridH, gridW, gridH);
                }
            }

            for (int i = 1; i < columns; i++) {
                int lx = startX + i * gridW / columns;
                guiGraphics.fill(lx, startY, lx + 1, startY + gridH, 0x80FFFFFF);
            }
            for (int i = 1; i < rows; i++) {
                int ly = startY + i * gridH / rows;
                guiGraphics.fill(startX, ly, startX + gridW, ly + 1, 0x80FFFFFF);
            }

            if (selectedImage != null) {
                guiGraphics.drawString(font,
                        Component.translatable("gui.canvascontraptions.label.image", selectedFileName), 10, height - 85,
                        0xAAAAAA);
                guiGraphics.drawString(font, Component.translatable("gui.canvascontraptions.label.size",
                        selectedImage.getWidth() + "x" + selectedImage.getHeight()), 10, height - 74, 0x00FF00);
            }
        } else if (mode == Mode.FILLED && mapIds != null) {
            int size = 200;
            int tileSize = Math.min(size / Math.max(1, columns), size / Math.max(1, rows));
            int gridWidth = columns * tileSize;
            int gridHeight = rows * tileSize;
            int startX = width / 2 - gridWidth / 2;
            int startY = height / 2 - gridHeight / 2;

            for (int i = 0; i < mapIds.length; i++) {
                int r = i / columns;
                int c = i % columns;
                int tx = startX + c * tileSize;
                int ty = startY + r * tileSize;
                ResourceLocation loc = ClientMapCache.getTextureLocation(mapIds[i], Minecraft.getInstance().level);
                guiGraphics.blit(loc, tx, ty, 0, 0, tileSize, tileSize, tileSize, tileSize);
                int borderColor = (i == selectedIndexOnStack) ? 0xFFFF0000 : 0xFF444444;
                drawRectOutline(guiGraphics, tx, ty, tileSize, tileSize, borderColor);
                if (mouseX >= tx && mouseX < tx + tileSize && mouseY >= ty && mouseY < ty + tileSize) {
                    guiGraphics.fill(tx, ty, tx + tileSize, ty + tileSize, 0x40FFFFFF);
                    drawRectOutline(guiGraphics, tx, ty, tileSize, tileSize, 0xFFFFFFFF);
                }
            }
        }
    }

    private void drawRectOutline(GuiGraphics guiGraphics, int x, int y, int w, int h, int color) {
        guiGraphics.fill(x, y, x + w, y + 1, color);
        guiGraphics.fill(x, y + h - 1, x + w, y + h, color);
        guiGraphics.fill(x, y, x + 1, y + h, color);
        guiGraphics.fill(x + w - 1, y, x + w, y + h, color);
    }
}
