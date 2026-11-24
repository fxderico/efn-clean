package net.efn.client.module.impl.combat.crystal;

import imgui.ImDrawList;

import net.efn.client.gui.WorldLabel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

import java.util.Locale;

@Environment(EnvType.CLIENT)
public final class ShotView {

    private static final long SLIDE_MS = 120L;

    private static final long HOLD_MS = 150L;

    private static final long FADE_MS = 350L;

    private static final float FILL_ALPHA = 0.22F;

    private static final float LINE_WIDTH = 1.5F;

    private static final double LABEL_RISE = 0.25;

    private Box from;

    private Box to;

    private long movedAt;

    private long shownAt;

    private int color;

    private float damage;

    private float selfDamage;

    public ShotView() {}

    public void show(Box to2, int color2, float damage2, float selfDamage2) {
        long l = System.currentTimeMillis();
        if (!this.showing(l)) {
            this.from = to2;
            this.movedAt = l;
        } else if (!to2.equals(this.to)) {
            this.from = this.box(l);
            this.movedAt = l;
        }
        this.to = to2;
        this.shownAt = l;
        this.color = color2;
        this.damage = damage2;
        this.selfDamage = selfDamage2;
    }

    public void clear() {
        this.to = null;
    }

    public boolean showing() {
        return this.showing(System.currentTimeMillis());
    }

    public void draw() {
        long l = System.currentTimeMillis();
        if (this.showing(l)) {
            float alpha = this.alpha(l);
            GizmoDrawing.box(
                            this.box(l),
                            DrawStyle.filledAndStroked(
                                    fade(this.color, alpha), 1.5F, fade(this.color, alpha * 0.22F)))
                    .ignoreOcclusion();
        }
    }

    public void label(ImDrawList imDrawList, float f, float g) {
        long l = System.currentTimeMillis();
        if (this.showing(l)) {
            Box box = this.box(l);
            Vec3d vec = new Vec3d(box.getCenter().x, box.maxY + 0.25, box.getCenter().z);
            float[] floats = WorldLabel.project(MinecraftClient.getInstance(), vec, f, g);
            if (floats != null) {
                WorldLabel.drawText(
                        imDrawList,
                        floats[0],
                        floats[1],
                        String.format(Locale.ROOT, "%.1f", this.damage),
                        String.format(Locale.ROOT, "self %.1f", this.selfDamage),
                        this.alpha(l));
            }
        }
    }

    private boolean showing(long l) {
        return this.to != null && this.alpha(l) > 0.0F;
    }

    private float alpha(long l) {
        long l2 = l - this.shownAt - 150L;
        return l2 <= 0L ? 1.0F : Math.max(0.0F, 1.0F - (float) l2 / 350.0F);
    }

    private Box box(long l) {
        float f = Math.min(1.0F, (float) (l - this.movedAt) / 120.0F);
        float g = 1.0F - (1.0F - f) * (1.0F - f) * (1.0F - f);
        return new Box(
                this.from.getMinPos().lerp(this.to.getMinPos(), g),
                this.from.getMaxPos().lerp(this.to.getMaxPos(), g));
    }

    private static int fade(int i, float f) {
        return Math.round((i >>> 24) * f) << 24 | i & 16777215;
    }
}
