package net.efn.client.gui.mainmenu;

import com.mojang.blaze3d.textures.GpuTexture;

import imgui.ImDrawList;

import net.efn.client.account.Account;
import net.efn.client.account.AccountType;
import net.efn.client.gui.HudDraw;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.client.texture.PlayerSkinCache.Entry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.Identifier;

import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
final class PlayerFaces {

    private static final float FACE_U = 0.125F;

    private static final float HAT_U = 0.625F;

    private static final float TOP_V = 0.125F;

    private static final float PART = 0.125F;

    private static final Map<UUID, Supplier<SkinTextures>> SKINS = new HashMap<>();

    private static final Set<GpuTexture> NEAREST = Collections.newSetFromMap(new WeakHashMap<>());

    private PlayerFaces() {}

    static void draw(
            ImDrawList imDrawList, Account account, float f, float g, float h, float f2, float g2) {
        UUID uuid;
        try {
            uuid = UUID.fromString(account.uuid());
        } catch (IllegalArgumentException exception) {
            placeholder(imDrawList, f, g, h, f2, g2);
            return;
        }
        draw(imDrawList, uuid, account.type() == AccountType.CRACKED, f, g, h, f2, g2);
    }

    static void draw(
            ImDrawList imDrawList,
            UUID uuid,
            boolean bl,
            float f,
            float g,
            float h,
            float f2,
            float g2) {
        int texture = texture(skin(uuid, bl).body().texturePath());
        if (texture == 0) {
            placeholder(imDrawList, f, g, h, f2, g2);
        } else {
            float h2 = Math.max(1.0F, (float) Math.round(3.0F * f2));
            int col = HudDraw.col(-1, g2);
            imDrawList.addImageRounded(
                    texture, f, g, f + h, g + h, 0.125F, 0.125F, 0.25F, 0.25F, col, h2, 240);
            imDrawList.addImageRounded(
                    texture, f, g, f + h, g + h, 0.625F, 0.125F, 0.75F, 0.25F, col, h2, 240);
        }
    }

    private static void placeholder(
            ImDrawList imDrawList, float f, float g, float h, float f2, float g2) {
        imDrawList.addRectFilled(
                f, g, f + h, g + h, HudDraw.col(-7693914, 0.3F * g2), Math.round(3.0F * f2));
    }

    private static SkinTextures skin(UUID uuid, boolean bl) {
        return bl
                ? DefaultSkinHelper.getSkinTextures(uuid)
                : SKINS.computeIfAbsent(
                                uuid,
                                uUIDx -> {
                                    Supplier supplier =
                                            MinecraftClient.getInstance()
                                                    .getPlayerSkinCache()
                                                    .getSupplier(ProfileComponent.ofDynamic(uUIDx));
                                    return () -> ((Entry) supplier.get()).getTextures();
                                })
                        .get();
    }

    private static int texture(Identifier id) {
        GpuTexture glTexture;
        try {
            glTexture =
                    MinecraftClient.getInstance().getTextureManager().getTexture(id).getGlTexture();
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
}
