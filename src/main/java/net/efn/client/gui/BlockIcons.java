package net.efn.client.gui;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;

import imgui.ImDrawList;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

import net.efn.client.imgui.ImGuiManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.BlockRenderLayers;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.Sprite;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Atlases;
import net.minecraft.util.math.BlockPos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
final class BlockIcons {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final float SPRITE_TEXELS = 16.0F;

    private static final int PAGE_SIDE = 1024;

    private static final int RENDERS_PER_FRAME = 48;

    private static final int MAX_SHEETS = 4;

    private static final int MAX_LOSSES = 3;

    private static final int WATER_RGB = 4159204;

    private static final Int2ObjectOpenHashMap<BlockIcons.Sheet> SHEETS =
            new Int2ObjectOpenHashMap<>();

    private static final ReferenceOpenHashSet<GpuTexture> CRISP = new ReferenceOpenHashSet<>();

    private static GpuTexture atlas;

    private static final ItemRenderState ITEM = new ItemRenderState();

    private static int budgetFrame = -1;

    private static int rendersLeft;

    private static boolean itemsFailed;

    private static int auditedFrame;

    private static int losses;

    private BlockIcons() {}

    static void validate() {
        GpuTexture glTexture;
        try {
            glTexture =
                    MinecraftClient.getInstance()
                            .getAtlasManager()
                            .getAtlasTexture(Atlases.BLOCKS)
                            .getGlTexture();
        } catch (RuntimeException exception) {
            glTexture = null;
        }
        if (glTexture != atlas) {
            atlas = glTexture;
            clearSheets();
        } else {
            int frameCount = ImGuiManager.getInstance().frameCount();
            if (frameCount - auditedFrame >= 30) {
                auditedFrame = frameCount;
                if (!intact()) {
                    clearSheets();
                    if (++losses < 3) {
                        LOGGER.warn("Block icon pages were lost; drawing them again");
                    } else {
                        itemsFailed = true;
                        LOGGER.warn(
                                "Block icon pages keep getting lost; using block textures instead");
                    }
                }
            }
        }
    }

    private static boolean intact() {
        for (BlockIcons.Sheet sheet : SHEETS.values()) {
            if (sheet.depth != null && !ItemIcons.alive(sheet.depth)) {
                return false;
            }
            for (GpuTexture page : sheet.pages) {
                if (!ItemIcons.alive(page)) {
                    return false;
                }
            }
        }
        return true;
    }

    static void forget() {
        clearSheets();
    }

    static float size(float f) {
        return Math.max(1, (int) (f / 16.0F)) * 16.0F;
    }

    static void draw(ImDrawList imDrawList, Block block, float f, float g, float h) {
        int i = Math.round(h);
        BlockIcons.Sheet sheet = sheet(i);
        BlockIcons.Icon icon = sheet.icons.get(block);
        if (icon == null) {
            icon = resolve(sheet, block);
            if (icon == null) {
                return;
            }
            sheet.icons.put(block, icon);
        }
        float f2 = Math.round(f);
        float g2 = Math.round(g);
        if (icon.texture != 0) {
            imDrawList.addImage(
                    icon.texture,
                    f2,
                    g2,
                    f2 + i,
                    g2 + i,
                    icon.u0,
                    icon.v0,
                    icon.u1,
                    icon.v1,
                    EfnWidgets.c(icon.tint));
        } else {
            float h2 = i * 0.2F;
            imDrawList.addRectFilled(f2, g2, f2 + i, g2 + i, EfnWidgets.c(-12240588), h2);
            imDrawList.addRect(f2, g2, f2 + i, g2 + i, EfnWidgets.c(536870911), h2, 0, 1.0F);
        }
    }

    private static BlockIcons.Sheet sheet(int i) {
        BlockIcons.Sheet sheet = SHEETS.get(i);
        if (sheet == null) {
            if (SHEETS.size() >= 4) {
                clearSheets();
            }
            sheet = new BlockIcons.Sheet(i);
            SHEETS.put(i, sheet);
        }
        return sheet;
    }

    private static void clearSheets() {
        for (BlockIcons.Sheet sheet : SHEETS.values()) {
            sheet.close();
        }
        SHEETS.clear();
        CRISP.clear();
        ItemIcons.forgetVerdict();
    }

    private static BlockIcons.Icon resolve(BlockIcons.Sheet sheet, Block block) {
        Item item = block.asItem();
        Block block2 = block;
        if (item == Items.AIR && (item = standIn(block)) instanceof BlockItem) {
            BlockItem blockItem = (BlockItem) item;
            block2 = blockItem.getBlock();
        }
        if (item == Items.AIR || itemsFailed || sheet.cell > 256 || !ItemIcons.works()) {
            return sprite(block);
        } else if (!takeRender()) {
            return null;
        } else {
            MinecraftClient client = MinecraftClient.getInstance();
            try {
                client.getItemModelManager()
                        .clearAndUpdate(
                                ITEM,
                                new ItemStack(item),
                                ItemDisplayContext.GUI,
                                client.world,
                                null,
                                0);
            } catch (RuntimeException exception) {
                ITEM.clear();
                return sprite(block);
            }
            if (ITEM.isEmpty()) {
                return sprite(block);
            } else {
                BlockIcons.Icon icon = new BlockIcons.Icon();
                boolean bl =
                        BlockRenderLayers.getBlockLayer(block2.getDefaultState())
                                == BlockRenderLayer.TRANSLUCENT;
                try {
                    renderItem(sheet, icon, bl);
                    return icon;
                } catch (RuntimeException exception2) {
                    itemsFailed = true;
                    LOGGER.warn(
                            "Block item icons failed, using block textures instead",
                            (Throwable) exception2);
                    return sprite(block);
                }
            }
        }
    }

    private static Item standIn(Block block) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null) {
            try {
                ItemStack stack =
                        block.getDefaultState().getPickStack(client.world, BlockPos.ORIGIN, false);
                if (!stack.isEmpty()) {
                    return stack.getItem();
                }
            } catch (RuntimeException exception) {
            }
        }
        if (block == Blocks.MOVING_PISTON) {
            return Items.PISTON;
        } else if (block == Blocks.END_PORTAL) {
            return Items.ENDER_EYE;
        } else {
            return block == Blocks.END_GATEWAY ? Items.ENDER_PEARL : Items.AIR;
        }
    }

    private static boolean takeRender() {
        int frameCount = ImGuiManager.getInstance().frameCount();
        if (frameCount != budgetFrame) {
            budgetFrame = frameCount;
            rendersLeft = 48;
        }
        if (rendersLeft <= 0) {
            return false;
        } else {
            rendersLeft--;
            return true;
        }
    }

    private static void renderItem(BlockIcons.Sheet sheet, BlockIcons.Icon icon, boolean bl) {
        int cell2 = sheet.cell;
        int i = 1024 / cell2;
        if (sheet.pages.isEmpty() || sheet.used >= i * i) {
            addPage(sheet);
        }
        int j = sheet.used++;
        int k = j % i * cell2;
        int m = j / i * cell2;
        int n = 1024 - m - cell2;
        GpuTexture page = sheet.pages.get(sheet.pages.size() - 1);
        GpuTextureView view = sheet.views.get(sheet.views.size() - 1);
        try {
            ItemIcons.render(ITEM, page, view, sheet.depth, sheet.depthView, k, m, cell2);
        } finally {
            ITEM.clear();
        }
        if (bl) {
            ItemIcons.straighten(page, k, n, cell2);
        }
        icon.texture = ((GlTexture) page).getGlId();
        icon.u0 = k / 1024.0F;
        icon.u1 = (k + cell2) / 1024.0F;
        icon.v0 = (1024 - m) / 1024.0F;
        icon.v1 = n / 1024.0F;
    }

    private static void addPage(BlockIcons.Sheet sheet) {
        GpuDevice device = RenderSystem.getDevice();
        if (sheet.depth == null) {
            sheet.depth = ItemIcons.create("EFN block icons depth", 8, TextureFormat.DEPTH32, 1024);
            sheet.depthView = device.createTextureView(sheet.depth);
        }
        GpuTexture gpuTexture =
                ItemIcons.create(
                        "EFN block icons " + sheet.cell + "px", 15, TextureFormat.RGBA8, 1024);
        sheet.pages.add(gpuTexture);
        sheet.views.add(device.createTextureView(gpuTexture));
        sheet.used = 0;
        device.createCommandEncoder().clearColorAndDepthTextures(gpuTexture, 0, sheet.depth, 1.0);
    }

    private static BlockIcons.Icon sprite(Block block) {
        BlockIcons.Icon icon = new BlockIcons.Icon();
        MinecraftClient client = MinecraftClient.getInstance();
        BlockState defaultState = block.getDefaultState();
        try {
            Sprite modelParticleSprite =
                    client.getBakedModelManager()
                            .getBlockModels()
                            .getModelParticleSprite(defaultState);
            if (MissingSprite.getMissingSpriteId()
                    .equals(modelParticleSprite.getContents().getId())) {
                return icon;
            }
            GpuTexture glTexture =
                    client.getTextureManager()
                            .getTexture(modelParticleSprite.getAtlasId())
                            .getGlTexture();
            GlTexture glTexture2;
            if (!(glTexture instanceof GlTexture)
                    || (glTexture2 = (GlTexture) glTexture).isClosed()) {
                return icon;
            }
            if (CRISP.add(glTexture2)) {
                ItemIcons.crisp(glTexture2);
            }
            icon.texture = glTexture2.getGlId();
            icon.u0 = modelParticleSprite.getMinU();
            icon.v0 = modelParticleSprite.getMinV();
            icon.u1 = modelParticleSprite.getMaxU();
            icon.v1 = modelParticleSprite.getMaxV();
        } catch (RuntimeException exception) {
            return icon;
        }
        if (!defaultState.isOf(Blocks.GRASS_BLOCK)) {
            int i = -1;
            try {
                i = client.getBlockColors().getColor(defaultState, null, null, 0);
            } catch (RuntimeException exception2) {
            }
            if (i == -1
                    && (defaultState.isOf(Blocks.WATER)
                            || defaultState.isOf(Blocks.BUBBLE_COLUMN))) {
                i = 4159204;
            }
            if (i != -1) {
                icon.tint = 0xFF000000 | (i & 0xFF) << 16 | i & 0xFF00 | i >> 16 & 0xFF;
            }
        }
        return icon;
    }

    @Environment(EnvType.CLIENT)
    private static final class Icon {

        int texture;

        float u0;

        float v0;

        float u1;

        float v1;

        int tint = -1;

        private Icon() {}
    }

    @Environment(EnvType.CLIENT)
    private static final class Sheet {

        final int cell;

        final Reference2ObjectOpenHashMap<Block, BlockIcons.Icon> icons =
                new Reference2ObjectOpenHashMap<>();

        final List<GpuTexture> pages = new ArrayList<>();

        final List<GpuTextureView> views = new ArrayList<>();

        GpuTexture depth;

        GpuTextureView depthView;

        int used;

        Sheet(int cell2) {
            this.cell = cell2;
        }

        void close() {
            for (GpuTextureView view : this.views) {
                view.close();
            }
            for (GpuTexture page : this.pages) {
                ItemIcons.dispose(page);
            }
            if (this.depthView != null) {
                this.depthView.close();
            }
            ItemIcons.dispose(this.depth);
            this.views.clear();
            this.pages.clear();
            this.depth = null;
            this.depthView = null;
        }
    }
}
