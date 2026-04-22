package dev.rit3776.canvascontraptions;

import com.mojang.blaze3d.platform.NativeImage;
import dev.rit3776.canvascontraptions.network.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
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

public class DraftingTabletGUI extends Screen {
    private final InteractionHand activeHand;
    private int columns = 1;
    private int rows = 1;
    private int[] mapIds;
    private int selectedIndexOnStack = 0;
    private String fileName = "";

    private boolean showLibrary = false;
    private boolean dither = true;
    private boolean keepAspectRatio = true;
    private List<String> savedNames = new ArrayList<>();
    private double scrollOffset = 0;
    private String statusMessage = "";
    private int statusTimer = 0;

    // Import temporary state
    private BufferedImage selectedImage;
    private String tempFileName = "";
    private int tempRows = 1;
    private int tempCols = 1;
    private DynamicTexture previewTexture;
    private ResourceLocation previewLocation;

    public DraftingTabletGUI(Component title, InteractionHand hand) {
        super(title);
        this.activeHand = hand;
        loadFromItem();
    }

    private void loadFromItem() {
        if (Minecraft.getInstance().player == null) return;
        ItemStack stack = Minecraft.getInstance().player.getItemInHand(activeHand);
        if (stack.hasTag()) {
            CompoundTag tag = stack.getTag();
            this.mapIds = tag.contains("MapIDs") ? tag.getIntArray("MapIDs") : null;
            this.selectedIndexOnStack = tag.getInt("SelectedIndex");
            this.fileName = tag.getString("FileName");
            this.columns = tag.getInt("Width");
            this.rows = tag.getInt("Height");

            this.savedNames.clear();
            ListTag list = tag.getList("SavedImages", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                savedNames.add(list.getCompound(i).getString("FileName"));
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        super.init();
        loadFromItem();

        int bx = 10;
        int bw = 100;

        if (selectedImage == null) {
            // Management Sidebar
            this.addRenderableWidget(Button.builder(Component.translatable("gui.canvascontraptions.button.add_new"), b -> selectFile())
                    .bounds(bx, 40, bw, 20).build());
            this.addRenderableWidget(Button.builder(Component.translatable("gui.canvascontraptions.button.saved_list"), b -> {
                        showLibrary = !showLibrary;
                        scrollOffset = 0;
                        init();
                    })
                    .bounds(bx, 65, bw, 20).build());
            
            if (mapIds != null && !fileName.isEmpty()) {
                this.addRenderableWidget(Button.builder(Component.translatable("gui.canvascontraptions.button.save_active"), b -> saveToLibrary())
                        .bounds(bx, 90, bw, 20).build());
            }
            
            this.addRenderableWidget(Button.builder(Component.translatable("gui.canvascontraptions.button.close"), b -> this.onClose())
                    .bounds(bx, 115, bw, 20).build());
        } else {
            // Import Configuration Sidebar
            this.addRenderableWidget(Button.builder(Component.translatable("gui.canvascontraptions.button.cols_minus"), b -> {
                tempCols = Math.max(1, tempCols - 1);
                init();
            }).bounds(bx, 40, bw / 2 - 2, 20).build());
            this.addRenderableWidget(Button.builder(Component.translatable("gui.canvascontraptions.button.cols_plus"), b -> {
                tempCols++;
                init();
            }).bounds(bx + bw / 2 + 2, 40, bw / 2 - 2, 20).build());

            this.addRenderableWidget(Button.builder(Component.translatable("gui.canvascontraptions.button.rows_minus"), b -> {
                tempRows = Math.max(1, tempRows - 1);
                init();
            }).bounds(bx, 65, bw / 2 - 2, 20).build());
            this.addRenderableWidget(Button.builder(Component.translatable("gui.canvascontraptions.button.rows_plus"), b -> {
                tempRows++;
                init();
            }).bounds(bx + bw / 2 + 2, 65, bw / 2 - 2, 20).build());
            
            this.addRenderableWidget(Button.builder(Component.translatable("gui.canvascontraptions.button.dither", 
                    Component.translatable(dither ? "gui.canvascontraptions.label.on" : "gui.canvascontraptions.label.off").getString()), b -> {
                dither = !dither;
                init();
            }).bounds(bx, 95, bw, 20).build());

            this.addRenderableWidget(Button.builder(Component.translatable("gui.canvascontraptions.button.aspect", 
                    Component.translatable(keepAspectRatio ? "gui.canvascontraptions.label.fit" : "gui.canvascontraptions.label.stretch").getString()), b -> {
                keepAspectRatio = !keepAspectRatio;
                init();
            }).bounds(bx, 120, bw, 20).build());
            
            this.addRenderableWidget(Button.builder(Component.translatable("gui.canvascontraptions.button.import"), b -> submitImport())
                    .bounds(bx, height - 55, bw, 20).build());
            this.addRenderableWidget(Button.builder(Component.translatable("gui.canvascontraptions.button.cancel"), b -> {
                selectedImage = null;
                init();
            }).bounds(bx, height - 30, bw, 20).build());
        }
    }

    private void selectFile() {
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
                        Minecraft.getInstance().execute(() -> {
                            this.selectedImage = img;
                            this.tempFileName = file.getName();
                            this.showLibrary = false;
                            this.statusMessage = Component.translatable("message.canvascontraptions.image_loaded").getString();
                            this.statusTimer = 60;
                            setupPreviewTexture(img);
                            init();
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void setupPreviewTexture(BufferedImage img) {
        if (previewTexture != null) {
            previewTexture.close();
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
        previewTexture = new DynamicTexture(nativeImage);
        previewLocation = Minecraft.getInstance().getTextureManager().register("canvascontraptions_tablet_preview", previewTexture);
    }

    @Override
    public void onClose() {
        if (previewTexture != null) {
            previewTexture.close();
            previewTexture = null;
        }
        super.onClose();
    }

    private void submitImport() {
        if (selectedImage == null) return;
        
        int canvasWidth = tempCols * 128;
        int canvasHeight = tempRows * 128;
        BufferedImage canvas = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        if (keepAspectRatio) {
            double scale = Math.min((double) canvasWidth / selectedImage.getWidth(), (double) canvasHeight / selectedImage.getHeight());
            int sw = (int) (selectedImage.getWidth() * scale);
            int sh = (int) (selectedImage.getHeight() * scale);
            int ox = (canvasWidth - sw) / 2;
            int oy = (canvasHeight - sh) / 2;
            g.drawImage(selectedImage, ox, oy, sw, sh, null);
        } else {
            g.drawImage(selectedImage, 0, 0, canvasWidth, canvasHeight, null);
        }
        g.dispose();

        List<byte[]> slices = new ArrayList<>();
        for (int r = 0; r < tempRows; r++) {
            for (int c = 0; c < tempCols; c++) {
                BufferedImage slice = canvas.getSubimage(c * 128, r * 128, 128, 128);
                slices.add(convertToMapColors(slice));
            }
        }

        CCNetwork.sendToServer(new C2SImageUploadPacket(slices, tempCols, tempRows, activeHand, tempFileName));
        this.selectedImage = null;
        this.statusMessage = Component.translatable("message.canvascontraptions.import_success").getString();
        this.statusTimer = 60;
        init();
    }

    private byte[] convertToMapColors(BufferedImage image) {
        byte[] colors = new byte[128 * 128];
        if (dither) {
            float[][][] errorBuf = new float[128][128][3];
            for (int y = 0; y < 128; y++) {
                for (int x = 0; x < 128; x++) {
                    int rgb = image.getRGB(x, y);
                    float r = ((rgb >> 16) & 0xFF) + errorBuf[x][y][0];
                    float g = ((rgb >> 8) & 0xFF) + errorBuf[x][y][1];
                    float b = (rgb & 0xFF) + errorBuf[x][y][2];

                    byte idx = MapColorHelper.findNearestIndex((int)r, (int)g, (int)b);
                    colors[y * 128 + x] = idx;

                    int actual = MapColorHelper.getRgbColor(idx);
                    float er = r - ((actual >> 16) & 0xFF);
                    float eg = g - ((actual >> 8) & 0xFF);
                    float eb = b - (actual & 0xFF);

                    if (x + 1 < 128) diffuse(errorBuf, x + 1, y, er, eg, eb, 7 / 16f);
                    if (y + 1 < 128) {
                        if (x > 0) diffuse(errorBuf, x - 1, y + 1, er, eg, eb, 3 / 16f);
                        diffuse(errorBuf, x, y + 1, er, eg, eb, 5 / 16f);
                        if (x + 1 < 128) diffuse(errorBuf, x + 1, y + 1, er, eg, eb, 1 / 16f);
                    }
                }
            }
        } else {
            for (int y = 0; y < 128; y++) {
                for (int x = 0; x < 128; x++) {
                    colors[y * 128 + x] = MapColorHelper.findNearestIndex(image.getRGB(x, y));
                }
            }
        }
        return colors;
    }

    private void diffuse(float[][][] buf, int x, int y, float er, float eg, float eb, float w) {
        buf[x][y][0] += er * w;
        buf[x][y][1] += eg * w;
        buf[x][y][2] += eb * w;
    }

    private void saveToLibrary() {
        if (savedNames.size() >= 16) {
            statusMessage = ChatFormatting.RED + Component.translatable("message.canvascontraptions.library_full").getString();
            statusTimer = 100;
            return;
        }
        if (mapIds == null || fileName.isEmpty()) {
            statusMessage = ChatFormatting.RED + Component.translatable("message.canvascontraptions.nothing_to_save").getString();
            statusTimer = 60;
            return;
        }
        CCNetwork.sendToServer(new C2STabletActionPacket(C2STabletActionPacket.Action.SAVE, -1, activeHand));
        statusMessage = ChatFormatting.GREEN + Component.translatable("message.canvascontraptions.saved_to_library").getString();
        statusTimer = 60;
    }

    private void deleteFromLibrary(int index) {
        CCNetwork.sendToServer(new C2STabletActionPacket(C2STabletActionPacket.Action.DELETE, index, activeHand));
        statusMessage = ChatFormatting.YELLOW + Component.translatable("message.canvascontraptions.deleted_from_library").getString();
        statusTimer = 60;
        init();
    }

    private void selectFromLibrary(int index) {
        CCNetwork.sendToServer(new C2STabletActionPacket(C2STabletActionPacket.Action.SELECT, index, activeHand));
        this.showLibrary = false;
        statusMessage = Component.translatable("message.canvascontraptions.switched_to_saved_image").getString();
        statusTimer = 60;
        init();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (showLibrary) {
            scrollOffset = Mth.clamp(scrollOffset - delta * 15, 0, Math.max(0, (savedNames.size() - 5) * 15));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showLibrary) {
            int sidebarW = 120;
            int lx = sidebarW + 10;
            int ly = 35;
            for (int i = 0; i < savedNames.size(); i++) {
                double dy = ly + i * 15 - scrollOffset;
                if (dy < ly - 5 || dy > ly + 140) continue;

                if (mouseX >= lx && mouseX < lx + 200 && mouseY >= dy && mouseY < dy + 15) {
                    if (button == 0) {
                        selectFromLibrary(i);
                        return true;
                    } else if (button == 1) {
                        deleteFromLibrary(i);
                        return true;
                    }
                }
            }
        } else if (mapIds != null && selectedImage == null) {
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

            if (mouseX >= startX && mouseX < startX + gridW && mouseY >= startY && mouseY < startY + gridH) {
                int col = (int) ((mouseX - startX) / (gridW / (double)columns));
                int row = (int) ((mouseY - startY) / (gridH / (double)rows));
                int index = (int)row * columns + (int)col;
                if (index >= 0 && index < mapIds.length) {
                    CCNetwork.sendToServer(new C2SSelectTilePacket(index, activeHand));
                    this.onClose();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        loadFromItem();
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        guiGraphics.drawCenteredString(font, this.title, width / 2, 10, 0xFFFFFF);
        
        if (statusTimer > 0) {
            guiGraphics.drawCenteredString(font, statusMessage, width / 2, height - 30, 0xEEEEEE);
            statusTimer--;
        }

        int sidebarW = 120;
        int areaY = 30;
        int areaW = width - sidebarW - 10;
        int areaH = height - areaY - 20;

        if (showLibrary && selectedImage == null) {
            guiGraphics.drawString(font, Component.translatable("gui.canvascontraptions.saved_images_header"), sidebarW + 10, 20, 0xFFFF00);
            int lx = sidebarW + 10;
            int ly = 35;
            guiGraphics.enableScissor(lx, ly, lx + 200, ly + 150);
            for (int i = 0; i < savedNames.size(); i++) {
                double dy = ly + i * 15 - scrollOffset;
                int color = (mouseX >= lx && mouseX < lx + 200 && mouseY >= dy && mouseY < dy + 15) ? 0xFFFFFF : 0xAAAAAA;
                guiGraphics.drawString(font, (i + 1) + ". " + savedNames.get(i), lx, (int)dy, color);
            }
            guiGraphics.disableScissor();
            if (savedNames.isEmpty()) guiGraphics.drawString(font, Component.translatable("gui.canvascontraptions.library_empty"), lx, ly, 0x666666);
        } else if (selectedImage != null) {
            // Draw import info in sidebar bottom
            guiGraphics.drawString(font, Component.translatable("gui.canvascontraptions.label.image", tempFileName), 10, height - 85, 0xAAAAAA);
            guiGraphics.drawString(font, Component.translatable("gui.canvascontraptions.label.size", selectedImage.getWidth() + "x" + selectedImage.getHeight()), 10, height - 74, 0x00FF00);

            double gridAspect = (double) tempCols / tempRows;
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
            
            if (previewLocation != null) {
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

            for (int i = 1; i < tempCols; i++) {
                int lx = startX + i * gridW / tempCols;
                guiGraphics.fill(lx, startY, lx + 1, startY + gridH, 0x80FFFFFF);
            }
            for (int i = 1; i < tempRows; i++) {
                int ly = startY + i * gridH / tempRows;
                guiGraphics.fill(startX, ly, startX + gridW, ly + 1, 0x80FFFFFF);
            }
        } else if (mapIds != null) {
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

            for (int i = 0; i < mapIds.length; i++) {
                int r = i / columns;
                int c = i % columns;
                int tw = gridW / columns;
                int th = gridH / rows;
                int tx = startX + c * tw;
                int ty = startY + r * th;
                ResourceLocation loc = ClientMapCache.getTextureLocation(mapIds[i], Minecraft.getInstance().level);
                guiGraphics.blit(loc, tx, ty, 0, 0, tw, th, tw, th);
                int borderColor = (i == selectedIndexOnStack) ? 0xFFFF0000 : 0xFF444444;
                drawRectOutline(guiGraphics, tx, ty, tw, th, borderColor);
                if (mouseX >= tx && mouseX < tx + tw && mouseY >= ty && mouseY < ty + th) {
                    guiGraphics.fill(tx, ty, tx + tw, ty + th, 0x40FFFFFF);
                    drawRectOutline(guiGraphics, tx, ty, tw, th, 0xFFFFFFFF);
                }
            }
        }

        if (selectedImage == null) {
            guiGraphics.drawString(font, Component.translatable("gui.canvascontraptions.label.active", (fileName.isEmpty() ? Component.translatable("gui.canvascontraptions.label.none").getString() : fileName)), 10, height - 20, 0xAAAAAA);
        }
    }

    private void drawRectOutline(GuiGraphics guiGraphics, int x, int y, int w, int h, int color) {
        guiGraphics.fill(x, y, x + w, y + 1, color);
        guiGraphics.fill(x, y + h - 1, x + w, y + h, color);
        guiGraphics.fill(x, y, x + 1, y + h, color);
        guiGraphics.fill(x + w - 1, y, x + w, y + h, color);
    }
}
