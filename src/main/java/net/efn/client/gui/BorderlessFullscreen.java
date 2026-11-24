/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.gui;

import com.sun.jna.Platform;

import net.efn.client.module.impl.misc.NoInvLeakModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;

@Environment(value = EnvType.CLIENT)
public final class BorderlessFullscreen {

    private BorderlessFullscreen() {}

    public static boolean wanted() {
        return Platform.isWindows() && NoInvLeakModule.wantsBorderless();
    }

    public static void tick() {
        Window window = MinecraftClient.getInstance().getWindow();
        if (!window.isFullscreen()) {
            return;
        }
        Switchable switchable = (Switchable) ((Object) window);
        if (BorderlessFullscreen.wanted() != switchable.efn$isBorderless()) {
            switchable.efn$reapplyMode();
        }
    }

    @Environment(value = EnvType.CLIENT)
    public static interface Switchable {

        public boolean efn$isBorderless();

        public void efn$reapplyMode();
    }
}
