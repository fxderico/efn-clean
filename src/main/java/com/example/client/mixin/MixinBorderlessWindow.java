/*
 * Decompiled with CFR 0.152.
 */
package com.example.client.mixin;

import net.efn.client.gui.BorderlessFullscreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.Monitor;
import net.minecraft.client.util.MonitorTracker;
import net.minecraft.client.util.VideoMode;
import net.minecraft.client.util.Window;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(value = EnvType.CLIENT)
@Mixin(value = {Window.class})
public abstract class MixinBorderlessWindow implements BorderlessFullscreen.Switchable {

    @Shadow @Final private long handle;

    @Shadow @Final private MonitorTracker monitorTracker;

    @Shadow private boolean fullscreen;

    @Shadow private boolean currentFullscreen;

    @Shadow private int windowedX;

    @Shadow private int windowedY;

    @Shadow private int windowedWidth;

    @Shadow private int windowedHeight;

    @Shadow private int x;

    @Shadow private int y;

    @Shadow private int width;

    @Shadow private int height;

    @Unique private boolean efn$borderless;

    @Inject(
            method = {"updateWindowRegion()V"},
            at = {@At(value = "HEAD")},
            cancellable = true)
    private void efn$setMode(CallbackInfo ci) {
        if (!this.fullscreen || !BorderlessFullscreen.wanted()) {
            if (this.efn$borderless) {
                this.efn$leaveBorderless();
            }
            return;
        }
        Monitor monitor = this.monitorTracker.getMonitor((Window) ((Object) this));
        if (monitor == null) {
            return;
        }
        boolean bl = GLFW.glfwGetWindowMonitor(this.handle) != 0L;
        boolean bl2 = bl;
        if (!bl && !this.efn$borderless) {
            this.windowedX = this.x;
            this.windowedY = this.y;
            this.windowedWidth = this.width;
            this.windowedHeight = this.height;
        }
        VideoMode currentVideoMode = monitor.getCurrentVideoMode();
        this.x = monitor.getViewportX();
        this.y = monitor.getViewportY();
        this.width = currentVideoMode.getWidth();
        this.height = currentVideoMode.getHeight();
        GLFW.glfwSetWindowAttrib(this.handle, 131077, 0);
        GLFW.glfwSetWindowMonitor(this.handle, 0L, this.x, this.y, this.width, this.height, -1);
        if (bl) {
            GLFW.glfwSetWindowAttrib(this.handle, 131077, 0);
        }
        this.efn$borderless = true;
        ci.cancel();
    }

    @Unique
    private void efn$leaveBorderless() {
        this.efn$borderless = false;
        GLFW.glfwSetWindowAttrib(this.handle, 131077, 1);
        this.x = this.windowedX;
        this.y = this.windowedY;
        this.width = this.windowedWidth;
        this.height = this.windowedHeight;
    }

    @Override
    public boolean efn$isBorderless() {
        return this.efn$borderless;
    }

    @Override
    public void efn$reapplyMode() {
        this.currentFullscreen = !this.fullscreen;
    }
}
