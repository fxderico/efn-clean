package net.efn.client.gui;

import com.example.client.mixin.AccessorGameRenderer;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBuffer.MappedView;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;

import imgui.ImDrawList;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

import net.efn.client.imgui.ImGuiManager;
import net.efn.client.module.impl.client.PerformanceModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.BlockRenderLayers;
import net.minecraft.client.render.DiffuseLighting.Type;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.ProjectionMatrix2;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.render.command.RenderDispatcher;
import net.minecraft.client.render.fog.FogRenderer.FogType;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.client.texture.NativeImage.Format;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Atlases;

import org.joml.Matrix4fStack;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class ItemIcons {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final int SHEET_SIDE = 512;

    private static final int MIN_PX = 8;

    private static final int MAX_PX = 96;

    private static final int MAX_SHEETS = 8;

    private static final int BUSY_FRAMES = 2;

    private static final int IDLE_FRAMES = 600;

    private static final int PAINTS_PER_FRAME = 6;

    private static final int CHECK_SIDE = 32;

    static final int AUDIT_FRAMES = 30;

    private static final ItemIcons.Cell BLANK = new ItemIcons.Cell();

    private static final Int2ObjectOpenHashMap<ItemIcons.Sheet> SHEETS =
            new Int2ObjectOpenHashMap<>();

    private static final List<ItemIcons.Sheet> RETIRED = new ArrayList<>();

    private static final Reference2ObjectOpenHashMap<Item, ItemStack> PLAIN =
            new Reference2ObjectOpenHashMap<>();

    private static final ReferenceOpenHashSet<Item> BROKEN = new ReferenceOpenHashSet<>();

    private static final ItemLook MODEL = new ItemLook();

    private static final int[] VIEWPORT = new int[4];

    private static GpuTexture depth;

    private static GpuTextureView depthView;

    private static ProjectionMatrix2 projection;

    private static OrderedRenderCommandQueueImpl submits;

    private static RenderDispatcher features;

    private static Immediate buffers;

    private static GpuBuffer readback;

    private static ByteBuffer straightened;

    private static boolean straightenFailed;

    private static int verdict;

    private static GpuTexture itemAtlas;

    private static int frame = -2147483648;

    private static int retiredFrame;

    private static int auditedFrame;

    private static int paintsLeft;

    private static boolean disabled;

    private ItemIcons() {}

    static float pixels(float f) {
        float g = f < 16.0F ? Math.round(f) : 16.0F * (float) Math.floor(f / 16.0F + 0.25F);
        return Math.max(8.0F, Math.min(96.0F, g));
    }

    static void draw(ImDrawList imDrawList, Item item, float f, float g, float h, float f2) {
        if (item != null && item != Items.AIR) {
            ItemStack stack = PLAIN.get(item);
            if (stack == null) {
                stack = item.getDefaultStack();
                PLAIN.put(item, stack);
            }
            draw(imDrawList, stack, f, g, h, f2);
        }
    }

    public static void draw(
            ImDrawList imDrawList, ItemStack stack, float f, float g, float h, float f2) {
        if (stack != null && !stack.isEmpty() && !(f2 <= 0.004F)) {
            startFrame();
            int pixels = (int) pixels(h);
            float g2 = Math.round(f);
            float h2 = Math.round(g);
            ItemIcons.Cell cell;
            try {
                cell = cell(stack, pixels);
            } finally {
                MODEL.clear();
            }
            if (cell != null) {
                if (cell.texture == 0) {
                    float f3 = Math.max(1.0F, pixels * 0.18F);
                    imDrawList.addRectFilled(
                            g2 + 1.0F,
                            h2 + 1.0F,
                            g2 + pixels - 1.0F,
                            h2 + pixels - 1.0F,
                            HudChrome.scaleAlpha(452984831, f2),
                            f3);
                } else {
                    imDrawList.addImage(
                            cell.texture,
                            g2,
                            h2,
                            g2 + pixels,
                            h2 + pixels,
                            cell.u0,
                            cell.v0,
                            cell.u1,
                            cell.v1,
                            HudChrome.scaleAlpha(-1, f2));
                }
            }
        }
    }

    private static void startFrame() {
        int frameCount = ImGuiManager.getInstance().frameCount();
        if (frameCount != frame) {
            frame = frameCount;
            paintsLeft = PerformanceModule.optimizationsEnabled() ? 2 : 6;
            if (!RETIRED.isEmpty() && frameCount != retiredFrame) {
                for (ItemIcons.Sheet sheet : RETIRED) {
                    sheet.release();
                }
                RETIRED.clear();
            }
            GpuTexture gpuTexture = currentAtlas();
            boolean bl = frameCount - auditedFrame >= 30 && audit();
            if (gpuTexture == itemAtlas && !bl) {
                ObjectIterator objectIterator = SHEETS.values().iterator();
                while (objectIterator.hasNext()) {
                    ItemIcons.Sheet sheet2 = (ItemIcons.Sheet) objectIterator.next();
                    if (frameCount - sheet2.lastUsed > 600) {
                        sheet2.release();
                        objectIterator.remove();
                    }
                }
            } else {
                itemAtlas = gpuTexture;
                for (ItemIcons.Sheet sheet3 : SHEETS.values()) {
                    sheet3.release();
                }
                SHEETS.clear();
                BROKEN.clear();
            }
        }
    }

    private static boolean audit() {
        auditedFrame = frame;
        boolean bl = false;
        if (depth != null && !alive(depth)) {
            depthView.close();
            depth = null;
            depthView = null;
            bl = true;
        }
        for (ItemIcons.Sheet sheet : SHEETS.values()) {
            bl |= sheet.color != null && !alive(sheet.color);
        }
        if (bl) {
            LOGGER.warn("HUD item icon textures were lost; drawing them again");
        }
        return bl;
    }

    private static GpuTexture currentAtlas() {
        try {
            return MinecraftClient.getInstance()
                    .getAtlasManager()
                    .getAtlasTexture(Atlases.ITEMS)
                    .getGlTexture();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static ItemIcons.Cell cell(ItemStack stack, int i) {
        ItemIcons.Sheet sheet = sheet(i);
        if (sheet == null) {
            return null;
        } else {
            sheet.lastUsed = frame;
            if (!resolve(stack)) {
                return BLANK;
            } else {
                ItemIcons.Cell cell = sheet.cells.get(MODEL.identity);
                if (cell != null) {
                    return cell;
                } else if (disabled) {
                    return BLANK;
                } else if (paintsLeft <= 0) {
                    return null;
                } else {
                    paintsLeft--;
                    if (sheet.full()) {
                        retire(sheet);
                        sheet = new ItemIcons.Sheet(i);
                        sheet.lastUsed = frame;
                        SHEETS.put(i, sheet);
                    }
                    cell = paint(sheet, seeThrough(stack.getItem()));
                    sheet.cells.put(Arrays.asList(MODEL.identity.toArray()), cell);
                    return cell;
                }
            }
        }
    }

    private static ItemIcons.Sheet sheet(int i) {
        ItemIcons.Sheet sheet = SHEETS.get(i);
        if (sheet != null) {
            return sheet;
        } else {
            if (SHEETS.size() >= 8) {
                ItemIcons.Sheet sheet2 = stalest();
                if (sheet2 == null || frame - sheet2.lastUsed < 2) {
                    return null;
                }
                retire(sheet2);
            }
            sheet = new ItemIcons.Sheet(i);
            SHEETS.put(i, sheet);
            return sheet;
        }
    }

    private static ItemIcons.Sheet stalest() {
        ItemIcons.Sheet sheet = null;
        for (ItemIcons.Sheet sheet2 : SHEETS.values()) {
            if (sheet == null || sheet2.lastUsed < sheet.lastUsed) {
                sheet = sheet2;
            }
        }
        return sheet;
    }

    private static void retire(ItemIcons.Sheet sheet) {
        SHEETS.remove(sheet.px);
        RETIRED.add(sheet);
        retiredFrame = frame;
    }

    private static boolean resolve(ItemStack stack) {
        if (BROKEN.contains(stack.getItem())) {
            return false;
        } else if (MODEL.resolve(stack)) {
            return true;
        } else {
            BROKEN.add(stack.getItem());
            return false;
        }
    }

    private static boolean seeThrough(Item item) {
        return !(item instanceof BlockItem blockItem)
                ? false
                : BlockRenderLayers.getBlockLayer(blockItem.getBlock().getDefaultState())
                        == BlockRenderLayer.TRANSLUCENT;
    }

    private static ItemIcons.Cell paint(ItemIcons.Sheet sheet, boolean bl) {
        if (MODEL.isEmpty()) {
            return BLANK;
        } else {
            try {
                if (sheet.color == null) {
                    allocate(sheet);
                }
                int px2 = sheet.px;
                int i = 512 / px2;
                int j = sheet.filled++;
                int k = j % i * px2;
                int m = j / i * px2;
                int n = 512 - m - px2;
                render(MODEL, sheet.color, sheet.colorView, depth, depthView, k, m, px2);
                if (bl) {
                    straighten(sheet.color, k, n, px2);
                }
                ItemIcons.Cell cell = new ItemIcons.Cell();
                cell.texture = ((GlTexture) sheet.color).getGlId();
                cell.u0 = k / 512.0F;
                cell.u1 = (k + px2) / 512.0F;
                cell.v0 = (512 - m) / 512.0F;
                cell.v1 = n / 512.0F;
                return cell;
            } catch (RuntimeException exception) {
                disabled = true;
                LOGGER.warn(
                        "HUD item icons could not be drawn; showing plain slots instead",
                        (Throwable) exception);
                return BLANK;
            }
        }
    }

    private static void allocate(ItemIcons.Sheet sheet) {
        GpuDevice device = RenderSystem.getDevice();
        if (depth == null) {
            depth = create("EFN HUD icon depth", 8, TextureFormat.DEPTH32, 512);
            depthView = device.createTextureView(depth);
        }
        sheet.color = create("EFN HUD icons " + sheet.px, 15, TextureFormat.RGBA8, 512);
        sheet.colorView = device.createTextureView(sheet.color);
        device.createCommandEncoder().clearColorAndDepthTextures(sheet.color, 0, depth, 1.0);
    }

    public static void render(
            ItemRenderState itemRenderState,
            GpuTexture gpuTexture,
            GpuTextureView gpuTextureView,
            GpuTexture gpuTexture2,
            GpuTextureView gpuTextureView2,
            int i,
            int j,
            int k) {
        MinecraftClient client = MinecraftClient.getInstance();
        int width = gpuTexture.getWidth(0);
        int m = width - j - k;
        RenderSystem.getDevice()
                .createCommandEncoder()
                .clearColorAndDepthTextures(gpuTexture, 0, gpuTexture2, 1.0, i, m, k, k);
        if (features == null) {
            buffers = VertexConsumerProvider.immediate(new BufferAllocator(262144));
            submits = new OrderedRenderCommandQueueImpl();
            features =
                    new RenderDispatcher(
                            submits,
                            client.getBlockRenderManager(),
                            buffers,
                            client.getAtlasManager(),
                            client.getBufferBuilders().getOutlineVertexConsumers(),
                            client.getBufferBuilders().getEffectVertexConsumers(),
                            client.textRenderer);
        }
        GpuTextureView outputColorTextureOverride = RenderSystem.outputColorTextureOverride;
        GpuTextureView outputDepthTextureOverride = RenderSystem.outputDepthTextureOverride;
        GpuBufferSlice shaderLights = RenderSystem.getShaderLights();
        GpuBufferSlice shaderFog = RenderSystem.getShaderFog();
        GpuBufferSlice projectionMatrixBuffer = RenderSystem.getProjectionMatrixBuffer();
        ProjectionType projectionType = RenderSystem.getProjectionType();
        GL11.glGetIntegerv(2978, VIEWPORT);
        GlStateManager._viewport(VIEWPORT[0], VIEWPORT[1], VIEWPORT[2], VIEWPORT[3]);
        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.identity();
        try {
            RenderSystem.outputColorTextureOverride = gpuTextureView;
            RenderSystem.outputDepthTextureOverride = gpuTextureView2;
            if (projection == null) {
                projection = new ProjectionMatrix2("EFN icons", -1000.0F, 1000.0F, true);
            }
            RenderSystem.setProjectionMatrix(
                    projection.set(width, width), ProjectionType.ORTHOGRAPHIC);
            Type type = itemRenderState.isSideLit() ? Type.ITEMS_3D : Type.ITEMS_FLAT;
            client.gameRenderer.getDiffuseLighting().setShaderLights(type);
            RenderSystem.setShaderFog(
                    ((AccessorGameRenderer) client.gameRenderer)
                            .efn$getFogRenderer()
                            .getFogBuffer(FogType.NONE));
            RenderSystem.enableScissorForRenderTypeDraws(i, m, k, k);
            MatrixStack matrices = new MatrixStack();
            matrices.translate(i + k * 0.5F, j + k * 0.5F, 0.0F);
            matrices.scale(k, -k, k);
            try {
                itemRenderState.render(matrices, submits, 15728880, OverlayTexture.DEFAULT_UV, 0);
                features.render();
            } finally {
                submits.clear();
                buffers.draw();
            }
        } finally {
            RenderSystem.disableScissorForRenderTypeDraws();
            RenderSystem.outputColorTextureOverride = outputColorTextureOverride;
            RenderSystem.outputDepthTextureOverride = outputDepthTextureOverride;
            RenderSystem.setShaderLights(shaderLights);
            RenderSystem.setShaderFog(shaderFog);
            RenderSystem.setProjectionMatrix(projectionMatrixBuffer, projectionType);
            modelViewStack.popMatrix();
            GlStateManager._viewport(VIEWPORT[0], VIEWPORT[1], VIEWPORT[2], VIEWPORT[3]);
        }
    }

    static void straighten(GpuTexture gpuTexture, int i, int j, int k) {
        if (!straightenFailed) {
            int m = k * k * 4;
            long l = (long) gpuTexture.getWidth(0) * gpuTexture.getHeight(0) * 4L;
            try {
                GpuDevice device = RenderSystem.getDevice();
                if (readback == null || readback.size() < l) {
                    if (readback != null) {
                        readback.close();
                    }
                    readback = device.createBuffer(() -> "EFN icon readback", 9, l);
                }
                if (straightened == null || straightened.capacity() < m) {
                    straightened = BufferUtils.createByteBuffer(m);
                }
                CommandEncoder commandEncoder = device.createCommandEncoder();
                commandEncoder.copyTextureToBuffer(
                        gpuTexture, readback, 0L, () -> {}, 0, i, j, k, k);
                try (GpuFence fence = commandEncoder.createFence()) {
                    GL11.glFlush();
                    if (!fence.awaitCompletion(1000000000L)) {
                        throw new IllegalStateException("icon read-back timed out");
                    }
                }
                straightened.clear().limit(m);
                try (MappedView mappedView =
                        commandEncoder.mapBuffer(readback.slice(0L, m), true, false)) {
                    ByteBuffer byteBuffer = mappedView.data();
                    for (int n = 0; n < m; n += 4) {
                        int o = byteBuffer.get(n + 3) & 255;
                        for (int p = 0; p < 3; p++) {
                            int q =
                                    o == 0
                                            ? 0
                                            : Math.min(
                                                    255,
                                                    ((byteBuffer.get(n + p) & 255) * 255 + o / 2)
                                                            / o);
                            straightened.put(n + p, (byte) q);
                        }
                        straightened.put(n + 3, (byte) o);
                    }
                }
                commandEncoder.writeToTexture(
                        gpuTexture, straightened, Format.RGBA, 0, 0, i, j, k, k);
            } catch (RuntimeException exception) {
                straightenFailed = true;
                LOGGER.warn(
                        "Couldn't read icons back; see-through ones stay darker",
                        (Throwable) exception);
            } finally {
                GlStateManager._pixelStore(3330, 0);
            }
        }
    }

    static void forgetVerdict() {
        verdict = 0;
    }

    static boolean works() {
        if (verdict == 0) {
            verdict = check() ? 1 : -1;
        }
        return verdict > 0;
    }

    private static boolean check() {
        MinecraftClient client = MinecraftClient.getInstance();
        GpuDevice device = RenderSystem.getDevice();
        ItemRenderState itemRenderState = new ItemRenderState();
        GpuTexture gpuTexture = null;
        GpuTexture gpuTexture2 = null;
        GpuTextureView gpuTextureView = null;
        GpuTextureView gpuTextureView2 = null;
        GpuBuffer gpuBuffer = null;
        try {
            client.getItemModelManager()
                    .clearAndUpdate(
                            itemRenderState,
                            new ItemStack(Items.STONE),
                            ItemDisplayContext.GUI,
                            client.world,
                            null,
                            0);
            if (itemRenderState.isEmpty()) {
                return true;
            } else {
                gpuTexture = create("EFN icon check", 10, TextureFormat.RGBA8, 32);
                gpuTexture2 = create("EFN icon check depth", 8, TextureFormat.DEPTH32, 32);
                gpuTextureView = device.createTextureView(gpuTexture);
                gpuTextureView2 = device.createTextureView(gpuTexture2);
                render(
                        itemRenderState,
                        gpuTexture,
                        gpuTextureView,
                        gpuTexture2,
                        gpuTextureView2,
                        0,
                        0,
                        32);
                gpuBuffer = device.createBuffer(() -> "EFN icon check", 9, 4096L);
                CommandEncoder commandEncoder = device.createCommandEncoder();
                commandEncoder.copyTextureToBuffer(
                        gpuTexture, gpuBuffer, 0L, () -> {}, 0, 0, 0, 32, 32);
                try (GpuFence fence = commandEncoder.createFence()) {
                    GL11.glFlush();
                    if (!fence.awaitCompletion(1000000000L)) {
                        return true;
                    }
                }
                int i = 0;
                long l = 0L;
                try (MappedView mappedView = commandEncoder.mapBuffer(gpuBuffer, true, false)) {
                    ByteBuffer byteBuffer = mappedView.data();
                    for (int j = 0; j < 4096; j += 4) {
                        if ((byteBuffer.get(j + 3) & 255) >= 128) {
                            i++;
                            l +=
                                    (byteBuffer.get(j) & 255)
                                            + (byteBuffer.get(j + 1) & 255)
                                            + (byteBuffer.get(j + 2) & 255);
                        }
                    }
                }
                long l2 = i == 0 ? 0L : l / i / 3L;
                boolean bl = i >= 128 && l2 >= 24L;
                if (!bl) {
                    LOGGER.warn(
                            "Item icons come out blank or black here (covered {}, brightness {});"
                                    + " block lists use flat textures",
                            i,
                            l2);
                }
                return bl;
            }
        } catch (RuntimeException exception) {
            LOGGER.warn("Couldn't check item icons", (Throwable) exception);
            return true;
        } finally {
            itemRenderState.clear();
            GlStateManager._pixelStore(3330, 0);
            if (gpuBuffer != null) {
                gpuBuffer.close();
            }
            if (gpuTextureView != null) {
                gpuTextureView.close();
            }
            if (gpuTextureView2 != null) {
                gpuTextureView2.close();
            }
            if (gpuTexture != null) {
                gpuTexture.close();
            }
            if (gpuTexture2 != null) {
                gpuTexture2.close();
            }
        }
    }

    static GpuTexture create(String string, int i, TextureFormat textureFormat, int j) {
        GlStateManager._activeTexture(GL11.glGetInteger(34016));
        GlStateManager._bindTexture(GL11.glGetInteger(32873));
        GpuTexture texture2 =
                RenderSystem.getDevice().createTexture(string, i, textureFormat, j, j, 1, 1);
        GlTexture glTexture;
        if (texture2 instanceof GlTexture && !prepare(glTexture = (GlTexture) texture2)) {
            texture2.close();
            throw new IllegalStateException(string + " got no storage");
        } else {
            return texture2;
        }
    }

    private static boolean prepare(GlTexture glTexture) {
        int glGetInteger = GL11.glGetInteger(32873);
        GL11.glBindTexture(3553, glTexture.getGlId());
        boolean bl;
        try {
            if (!holds(glTexture)) {
                int glGetInteger2 = GL11.glGetInteger(35055);
                GL15.glBindBuffer(35052, 0);
                TextureFormat format = glTexture.getFormat();
                GL11.glTexImage2D(
                        3553,
                        0,
                        GlConst.toGlInternalId(format),
                        glTexture.getWidth(0),
                        glTexture.getHeight(0),
                        0,
                        GlConst.toGlExternalId(format),
                        GlConst.toGlType(format),
                        (ByteBuffer) null);
                GL15.glBindBuffer(35052, glGetInteger2);
            }
            GL11.glTexParameteri(3553, 33085, 0);
            GL11.glTexParameteri(3553, 10241, 9728);
            GL11.glTexParameteri(3553, 10240, 9728);
            boolean holds = holds(glTexture);
            bl = holds;
        } finally {
            GL11.glBindTexture(3553, glGetInteger);
        }
        return bl;
    }

    static boolean alive(GpuTexture gpuTexture) {
        if (gpuTexture.isClosed()) {
            return false;
        } else if (!(gpuTexture instanceof GlTexture glTexture)) {
            return true;
        } else if (!GL11.glIsTexture(glTexture.getGlId())) {
            return false;
        } else {
            int glGetInteger = GL11.glGetInteger(32873);
            GL11.glBindTexture(3553, glTexture.getGlId());
            boolean bl = GL11.glGetInteger(32873) == glTexture.getGlId() && holds(glTexture);
            GL11.glBindTexture(3553, glGetInteger);
            if (!bl) {
                GlStateManager.clearGlErrors();
            }
            return bl;
        }
    }

    private static boolean holds(GpuTexture gpuTexture) {
        return GL11.glGetTexLevelParameteri(3553, 0, 4096) == gpuTexture.getWidth(0)
                && GL11.glGetTexLevelParameteri(3553, 0, 4097) == gpuTexture.getHeight(0);
    }

    static void dispose(GpuTexture gpuTexture) {
        if (gpuTexture != null && alive(gpuTexture)) {
            gpuTexture.close();
        }
    }

    static void crisp(GlTexture glTexture) {
        int glGetInteger = GL11.glGetInteger(32873);
        GL11.glBindTexture(3553, glTexture.getGlId());
        GL11.glTexParameteri(3553, 10240, 9728);
        GL11.glBindTexture(3553, glGetInteger);
    }

    @Environment(EnvType.CLIENT)
    private static final class Cell {

        int texture;

        float u0;

        float v0;

        float u1;

        float v1;

        private Cell() {}
    }

    @Environment(EnvType.CLIENT)
    private static final class Sheet {

        final int px;

        final Object2ObjectOpenHashMap<Object, ItemIcons.Cell> cells =
                new Object2ObjectOpenHashMap<>();

        GpuTexture color;

        GpuTextureView colorView;

        int filled;

        int lastUsed;

        Sheet(int px2) {
            this.px = px2;
        }

        boolean full() {
            int i = 512 / this.px;
            return this.filled >= i * i;
        }

        void release() {
            if (this.colorView != null) {
                this.colorView.close();
            }
            ItemIcons.dispose(this.color);
            this.colorView = null;
            this.color = null;
            this.cells.clear();
        }
    }
}
