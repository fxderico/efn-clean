package net.efn.client.module.impl.render;

import net.efn.client.config.ThemeStore;
import net.efn.client.gui.ThemeTint;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.client.HudModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class CosmeticAccent {

    private CosmeticAccent() {}

    static int argb() {
        HudModule module = ModuleRegistry.get(HudModule.class);
        int i =
                module != null && module.customAccent()
                        ? module.accentArgb()
                        : ThemeStore.get().accent.argb();
        return ThemeTint.accent(i) | 0xFF000000;
    }
}
