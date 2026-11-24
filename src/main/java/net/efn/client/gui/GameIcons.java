package net.efn.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Environment(EnvType.CLIENT)
final class GameIcons {

    private static final Map<Identifier, ThemeImages.Texture> TEXTURES = new HashMap<>();

    private static final Set<Identifier> MISSING = new HashSet<>();

    private GameIcons() {}

    static ThemeImages.Texture effect(RegistryEntry<StatusEffect> entry) {
        Identifier id = entry.getKey().map(RegistryKey::getValue).orElse(null);
        return id == null
                ? null
                : texture(
                        Identifier.of(
                                id.getNamespace(), "textures/mob_effect/" + id.getPath() + ".png"));
    }

    static ThemeImages.Texture block(String string) {
        return texture(Identifier.ofVanilla("textures/block/" + string + ".png"));
    }

    private static ThemeImages.Texture texture(Identifier id) {
        ThemeImages.Texture texture = TEXTURES.get(id);
        if (texture == null && !MISSING.contains(id)) {
            try (InputStream inValue =
                    MinecraftClient.getInstance().getResourceManager().open(id)) {
                texture = ThemeImages.decode(inValue.readAllBytes(), id.toString(), true);
            } catch (RuntimeException | IOException exception) {
                texture = null;
            }
            if (texture == null) {
                MISSING.add(id);
            } else {
                TEXTURES.put(id, texture);
            }
            return texture;
        } else {
            return texture;
        }
    }
}
