package net.efn.client.gui;

import com.mojang.blaze3d.textures.GpuTexture;

import imgui.ImDrawList;
import imgui.ImFont;

import net.efn.client.config.HudTheme;
import net.efn.client.config.Surface;
import net.efn.client.imgui.ImGuiManager;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.client.HudModule;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.util.Identifier;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

@Environment(EnvType.CLIENT)
public final class HudPanel {

    public static final int LABEL = -6052949;

    public static final int VALUE = -1;

    public static final int WARN = -47814;

    public static final int GOOD = -13445301;

    public static final int CAUTION = -19648;

    private static final float GRIP = 14.0F;

    private static final float HEADER_H = 20.0F;

    private static final float ROW_H = 22.0F;

    private static final float PAD = 8.0F;

    private static final float FACE = 16.0F;

    private static final float GAP_FACE = 6.0F;

    private static final float GAP_COUNT = 5.0F;

    private static final float GAP_RIGHT = 14.0F;

    private static final float ROWS_RADIUS = 6.0F;

    private static final float SKIN_FACE_U = 0.125F;

    private static final float SKIN_HAT_U = 0.625F;

    private static final float SKIN_TOP_V = 0.125F;

    private static final float SKIN_PART = 0.125F;

    private static final Map<String, ProfileComponent> PROFILES = new HashMap<>();

    private static final Set<GpuTexture> NEAREST = Collections.newSetFromMap(new WeakHashMap<>());

    private final NumberSetting x;

    private final NumberSetting y;

    private final NumberSetting scale;

    private final NumberSetting opacity;

    private boolean dragging;

    private boolean resizing;

    private boolean wasDown;

    private float grabX;

    private float grabY;

    private float scaleAtGrab;

    private float widthAtGrab;

    private float alpha = 1.0F;

    public HudPanel(
            NumberSetting setting,
            NumberSetting setting2,
            NumberSetting scale2,
            NumberSetting opacity2) {
        this.x = setting;
        this.y = setting2;
        this.scale = scale2;
        this.opacity = opacity2;
        setting.visibleWhen(() -> false);
        setting2.visibleWhen(() -> false);
    }

    public static NumberSetting coordinate(String string2) {
        return new NumberSetting(string2, -1.0F, -1.0F, 10000.0F).step(1.0F);
    }

    public static NumberSetting opacitySetting() {
        return new NumberSetting("Opacity", 100.0F, 10.0F, 100.0F).step(5.0F);
    }

    public float[] place(
            ImDrawList imDrawList, float f, float g, float h, float f2, float g2, float h2) {
        MinecraftClient client = MinecraftClient.getInstance();
        this.alpha = Math.max(0.0F, Math.min(1.0F, this.opacity.get() / 100.0F));
        float f3 = this.x.get() < 0.0F ? g2 : this.x.get();
        float g3 = this.y.get() < 0.0F ? h2 : this.y.get();
        boolean bl = client.currentScreen instanceof ChatScreen;
        boolean bl2 = bl && GLFW.glfwGetMouseButton(client.getWindow().getHandle(), 0) == 1;
        float h3 = (float) (client.mouse.getX() * h / Math.max(1, client.getWindow().getWidth()));
        float f4 = (float) (client.mouse.getY() * f2 / Math.max(1, client.getWindow().getHeight()));
        if (bl2 && !this.wasDown) {
            if (this.scale != null
                    && h3 >= f3 + f - 14.0F
                    && h3 <= f3 + f
                    && f4 >= g3 + g - 14.0F
                    && f4 <= g3 + g) {
                this.resizing = true;
                this.scaleAtGrab = this.scale.get();
                this.widthAtGrab = Math.max(1.0F, f);
                this.grabX = f3 + f - h3;
            } else if (h3 >= f3 && h3 <= f3 + f && f4 >= g3 && f4 <= g3 + g) {
                this.dragging = true;
                this.grabX = h3 - f3;
                this.grabY = f4 - g3;
            }
        }
        if (this.resizing) {
            this.scale.set(
                    Math.max(
                            this.scale.min(),
                            Math.min(
                                    this.scale.max(),
                                    this.scaleAtGrab * (h3 + this.grabX - f3) / this.widthAtGrab)));
        }
        if (this.dragging) {
            f3 = h3 - this.grabX;
            g3 = f4 - this.grabY;
        }
        f3 = Math.max(0.0F, Math.min(f3, h - f));
        g3 = Math.max(0.0F, Math.min(g3, f2 - g));
        if (!bl2) {
            if (this.dragging) {
                this.x.set(Math.round(f3));
                this.y.set(Math.round(g3));
            }
            this.dragging = false;
            this.resizing = false;
        }
        this.wasDown = bl2;
        if (bl) {
            imDrawList.addRect(
                    f3 - 1.0F,
                    g3 - 1.0F,
                    f3 + f + 1.0F,
                    g3 + g + 1.0F,
                    GuiPalette.abgr(!this.dragging && !this.resizing ? 1627389951 : -1056964609),
                    8.0F,
                    0,
                    1.0F);
            if (this.scale != null) {
                for (int m = 1; m <= 3; m++) {
                    float g4 = m * 4.0F;
                    imDrawList.addLine(
                            f3 + f - g4,
                            g3 + g - 2.0F,
                            f3 + f - 2.0F,
                            g3 + g - g4,
                            GuiPalette.abgr(-788529153),
                            1.5F);
                }
            }
        }
        return new float[] {f3, g3};
    }

    public void card(
            ImDrawList imDrawList,
            String string2,
            String string3,
            int j,
            List<HudPanel.Line> list,
            String string4,
            float f,
            float g,
            float h,
            float f2) {
        ImGuiManager imGuiManager = ImGuiManager.getInstance();
        float g2 = (this.scale == null ? 1.0F : this.scale.get()) * imGuiManager.uiScale();
        ImFont imFont = imGuiManager.hudLabelFont();
        ImFont imFont2 = imGuiManager.hudTextFont();
        int px = HudChrome.px(14.0F, g2);
        int px2 = HudChrome.px(17.0F, g2);
        float h2 = 8.0F * g2;
        float f3 = 20.0F * g2;
        float g3 = 22.0F * g2;
        float faceSize = faceSize(g2);
        float h3 = 6.0F * g2;
        boolean bl = false;
        for (HudPanel.Line line : list) {
            bl |= line.face() != 0;
        }
        float f4 = bl ? faceSize + h3 : 0.0F;
        float width = HudChrome.width(imFont, px, string2);
        float g4 =
                width
                        + (string3.isEmpty()
                                ? 0.0F
                                : 5.0F * g2 + HudChrome.width(imFont2, px2, string3));
        for (HudPanel.Line line2 : list) {
            float h4 = f4 + HudChrome.width(imFont2, px2, line2.text());
            if (!line2.right().isEmpty()) {
                h4 += 14.0F * g2 + HudChrome.width(imFont, px, line2.right());
            }
            g4 = Math.max(g4, h4);
        }
        float f5 = (float) Math.ceil(g4 + h2 * 2.0F);
        float g5 = Math.round(f3 + g3 * list.size());
        float h5 = 10.0F * g2;
        float[] floats = this.place(imDrawList, f5, g5, f, g, h5 + (f - f5 - h5 * 2.0F) * h, f2);
        float f6 = Math.round(floats[0]);
        float g6 = Math.round(floats[1]);
        Surface surface = HudTheme.get().forElementId(string4);
        float h6 = Math.min(HudTheme.get().rounding.get(), 6.0F) * g2;
        HudModule module = ModuleRegistry.get(HudModule.class);
        if (module == null || module.shadow()) {
            HudChrome.shadow(imDrawList, f6, g6, f5, g5, h6, this.alpha, g2);
        }
        if (surface != null) {
            imDrawList.addRectFilled(
                    f6,
                    g6,
                    f6 + f5,
                    g6 + g5,
                    HudChrome.scaleAlpha(GuiPalette.fill(surface), this.alpha),
                    h6);
            SurfaceBackground.draw(imDrawList, surface, f6, g6, f5, g5, h6, this.alpha);
        } else {
            imDrawList.addRectFilled(
                    f6,
                    g6,
                    f6 + f5,
                    g6 + g5,
                    HudChrome.scaleAlpha(HudChrome.CHIP_FILL, this.alpha),
                    h6);
        }
        float baselineOffset = HudChrome.baselineOffset(imFont2, px2, imFont, px);
        float f7 = g6 + (float) Math.floor((f3 - px2) * 0.5F);
        HudChrome.text(
                imDrawList,
                imFont,
                px,
                f6 + h2,
                f7 + baselineOffset,
                this.abgr(-6052949),
                string2,
                true);
        if (!string3.isEmpty()) {
            HudChrome.text(
                    imDrawList,
                    imFont2,
                    px2,
                    f6 + h2 + width + 5.0F * g2,
                    f7,
                    this.abgr(j),
                    string3,
                    true);
        }
        float g7 = (float) Math.floor((g3 - px2) * 0.5F);
        for (int n = 0; n < list.size(); n++) {
            HudPanel.Line line3 = (HudPanel.Line) list.get(n);
            float h7 = Math.round(g6 + f3 + g3 * n);
            float f8 = h7 + g7;
            if (line3.face() != 0) {
                this.drawFace(
                        imDrawList,
                        line3.face(),
                        Math.round(f6 + h2),
                        Math.round(h7 + (g3 - faceSize) * 0.5F),
                        faceSize,
                        g2);
            }
            HudChrome.text(
                    imDrawList,
                    imFont2,
                    px2,
                    f6 + h2 + f4,
                    f8,
                    this.abgr(line3.color()),
                    line3.text(),
                    true);
            if (!line3.right().isEmpty()) {
                float g8 = f6 + f5 - h2 - HudChrome.width(imFont, px, line3.right());
                HudChrome.text(
                        imDrawList,
                        imFont,
                        px,
                        g8,
                        f8 + baselineOffset,
                        this.abgr(line3.rightColor()),
                        line3.right(),
                        true);
            }
        }
    }

    private static float faceSize(float f) {
        int j = Math.round(Math.round(16.0F * f) / 8.0F) * 8;
        if (j > 22.0F * f - 4.0F * f) {
            j -= 8;
        }
        return Math.max(8, j);
    }

    public void drawFace(ImDrawList imDrawList, int j, float f, float g, float h, float f2) {
        float g2 = Math.max(1.0F, (float) Math.round(2.0F * f2));
        int abgr = this.abgr(-1);
        imDrawList.addImageRounded(
                j, f, g, f + h, g + h, 0.125F, 0.125F, 0.25F, 0.25F, abgr, g2, 240);
        imDrawList.addImageRounded(
                j, f, g, f + h, g + h, 0.625F, 0.125F, 0.75F, 0.25F, abgr, g2, 240);
        imDrawList.addRect(
                f, g, f + h, g + h, HudChrome.scaleAlpha(402653183, this.alpha), g2, 0, 1.0F);
    }

    public static int face(String string2) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        PlayerListEntry listEntry =
                networkHandler == null ? null : networkHandler.getPlayerListEntry(string2);
        Identifier id =
                listEntry != null
                        ? listEntry.getSkinTextures().body().texturePath()
                        : client.getPlayerSkinCache()
                                .get(
                                        PROFILES.computeIfAbsent(
                                                string2.toLowerCase(Locale.ROOT),
                                                value -> ProfileComponent.ofDynamic(string2)))
                                .getTextures()
                                .body()
                                .texturePath();
        GpuTexture glTexture;
        try {
            glTexture = client.getTextureManager().getTexture(id).getGlTexture();
        } catch (IllegalStateException exception) {
            return 0;
        }
        GlTexture glTexture2;
        if (glTexture instanceof GlTexture && !(glTexture2 = (GlTexture) glTexture).isClosed()) {
            if (NEAREST.add(glTexture)) {
                int glGetInteger = GL11.glGetInteger(32873);
                GL11.glBindTexture(3553, glTexture2.getGlId());
                GL11.glTexParameteri(3553, 10241, 9728);
                GL11.glTexParameteri(3553, 10240, 9728);
                GL11.glBindTexture(3553, glGetInteger);
            }
            return glTexture2.getGlId();
        } else {
            return 0;
        }
    }

    public int abgr(int j) {
        int k = Math.round((j >>> 24 & 0xFF) * this.alpha);
        return GuiPalette.abgr(k << 24 | j & 16777215);
    }

    @Environment(EnvType.CLIENT)
    public record Line(int face, String text, int color, String right, int rightColor) {

        public Line(String string, int i) {
            this(0, string, i, "", -6052949);
        }
    }
}
