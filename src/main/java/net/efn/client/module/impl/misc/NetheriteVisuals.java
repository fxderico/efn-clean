package net.efn.client.module.impl.misc;

import imgui.ImDrawList;

import net.efn.client.gui.WorldLabel;
import net.efn.client.util.FoglessLines;
import net.efn.client.util.TracerEngine;
import net.efn.client.util.glyph.GlyphCanvas;
import net.efn.client.util.glyph.GlyphShapes;
import net.efn.client.util.glyph.GlyphSwarm;
import net.efn.client.util.glyph.GlyphTint;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

@Environment(EnvType.CLIENT)
final class NetheriteVisuals {

    static final String DOTS = "Dots";

    static final String CUBES = "Cubes";

    static final String PULSES = "Pulses";

    private static final long PULSE_NANOS = 1400000000L;

    private static final float WAVE_SECONDS = 0.9F;

    private static final int MAX_WAVES = 12;

    private static final int GLOW_LIMIT = 32;

    private static final int DEBRIS_TRACERS = 3;

    private static final int LABEL_LIMIT = 8;

    private static final double PROBE_RADIUS = 12.0;

    private static final int DOT_LIMIT = 1800;

    private static final int CUBE_LIMIT = 450;

    private static final double MISSING_RADIUS = 32.0;

    private static final int MISSING_LIMIT = 1600;

    private static final float PROBE_FRESH_TICKS = 14.0F;

    private static final float[] SHELL_INFLATE = new float[] {0.06F, 0.16F, 0.3F};

    private static final float[] SHELL_ALPHA = new float[] {0.16F, 0.08F, 0.04F};

    private final GlyphCanvas canvas = new GlyphCanvas();

    private final GlyphSwarm swarm = new GlyphSwarm(128);

    private final Random random = Random.create();

    private final List<NetheriteVisuals.Wave> waves = new ArrayList<>();

    private final List<NetheriteVisuals.Label> labels = new ArrayList<>();

    private long lastFrame;

    private double pixel;

    NetheriteVisuals() {}

    void clear() {
        this.swarm.clear();
        this.waves.clear();
        this.labels.clear();
        this.lastFrame = 0L;
    }

    void burst(BlockPos pos2, int i) {
        double d = pos2.getX() + 0.5;
        double e = pos2.getY() + 0.5;
        double d2 = pos2.getZ() + 0.5;
        if (this.waves.size() >= 12) {
            this.waves.removeFirst();
        }
        this.waves.add(new NetheriteVisuals.Wave(d, e, d2, System.nanoTime()));
        int lift = GlyphTint.lift(i, 0.55F);
        for (int k = 0; k < 2; k++) {
            this.swarm.spawn(
                    (byte) 0,
                    d + this.jitter(0.3F),
                    e + 0.6,
                    d2 + this.jitter(0.3F),
                    this.jitter(0.2F),
                    1.1F,
                    this.jitter(0.2F),
                    1.8F,
                    0.22F,
                    this.jitter(0.3F),
                    this.jitter(0.5F),
                    this.random.nextFloat(),
                    lift,
                    128);
        }
        for (int m = 0; m < 10; m++) {
            float f = this.random.nextFloat() * 6.2831855F;
            float g = 2.2F + this.random.nextFloat() * 1.6F;
            int j = GlyphTint.mix(i, -1, this.random.nextFloat() * 0.5F);
            this.swarm.spawn(
                    (byte) 3,
                    d,
                    e,
                    d2,
                    MathHelper.cos(f) * g,
                    1.2F + this.random.nextFloat() * 1.5F,
                    MathHelper.sin(f) * g,
                    0.8F,
                    0.09F,
                    f,
                    2.8F,
                    this.random.nextFloat(),
                    j,
                    128);
        }
        MinecraftClient.getInstance()
                .getSoundManager()
                .play(
                        PositionedSoundInstance.ui(
                                SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.3F, 0.9F));
    }

    void render(
            MinecraftClient client,
            float f,
            Vec3d vec,
            List<NetheriteVisuals.Section> list,
            List<BlockPos> list2,
            AdvancedFinder advancedFinder,
            NetheriteVisuals.Options options) {
        long l = System.nanoTime();
        float g =
                this.lastFrame == 0L ? 0.0F : Math.min(0.1F, (float) (l - this.lastFrame) / 1.0E9F);
        this.lastFrame = l;
        this.swarm.step(g);
        this.canvas.begin(client.gameRenderer.getCamera());
        this.pixel = FoglessLines.pixelScale(f);
        int alpha = GlyphTint.alpha(options.near(), 1.0F);
        int lift = GlyphTint.lift(alpha, 0.55F);
        float h = 0.5F + 0.5F * MathHelper.sin((float) (l % 1400000000L) / 1.4E9F * 6.2831855F);
        this.labels.clear();
        this.drawRevealed(vec, list2, options, alpha, lift, h);
        this.drawWaves(l, alpha, lift);
        if (options.showProbes()) {
            this.drawProbes(vec, advancedFinder, options, alpha, lift, h);
        }
        this.drawSections(list, options, lift, h);
        this.drawTracers(list, options);
        this.swarm.draw(this.canvas, 1.0F, true);
        this.canvas.submit(true);
    }

    void overlay(ImDrawList imDrawList, float f, float g) {
        MinecraftClient client = MinecraftClient.getInstance();
        for (NetheriteVisuals.Label label : this.labels) {
            float[] floats = WorldLabel.project(client, label.pos(), f, g);
            if (floats != null) {
                WorldLabel.drawText(
                        imDrawList, floats[0], floats[1], label.primary(), label.secondary(), 1.0F);
            }
        }
    }

    boolean hasLabels() {
        return !this.labels.isEmpty();
    }

    private static int tint(NetheriteVisuals.Options options, double d) {
        float f = (float) MathHelper.clamp(d / options.fade(), 0.0, 1.0);
        return GlyphTint.alpha(GlyphTint.mix(options.near(), options.far(), f), 1.0F);
    }

    private void drawSections(
            List<NetheriteVisuals.Section> list, NetheriteVisuals.Options options, int i, float f) {
        for (NetheriteVisuals.Section section : list) {
            if (section.progress() >= 0.0F) {
                this.drawScanning(section, i, f);
            }
        }
        for (NetheriteVisuals.Section section2 : list) {
            if (section2.progress() < 0.0F) {
                this.drawCandidate(section2, options);
            }
        }
    }

    private void drawCandidate(NetheriteVisuals.Section section, NetheriteVisuals.Options options) {
        double d = MathHelper.clamp(section.distance() / options.fade(), 0.0, 1.0);
        int tint = tint(options, section.distance());
        this.box(
                section.box(),
                GlyphTint.alpha(tint, (float) (0.95 - 0.3 * d)),
                (float) (2.4 - 0.8 * d));
        this.faces(section.box(), GlyphTint.alpha(tint, (float) (0.07 - 0.03 * d)));
    }

    private void drawScanning(NetheriteVisuals.Section section, int i, float f) {
        Box box2 = section.box();
        this.box(box2, GlyphTint.alpha(i, 0.6F + 0.4F * f), 3.2F);
        this.faces(box2, GlyphTint.alpha(i, 0.05F));
        double d = box2.minY + (box2.maxY - box2.minY) * section.progress();
        int alpha = GlyphTint.alpha(i, 0.2F);
        this.canvas.quad(
                box2.minX, d, box2.minZ, box2.maxX, d, box2.minZ, box2.maxX, d, box2.maxZ,
                box2.minX, d, box2.maxZ, alpha);
        this.canvas.quad(
                box2.minX, d, box2.minZ, box2.minX, d, box2.maxZ, box2.maxX, d, box2.maxZ,
                box2.maxX, d, box2.minZ, alpha);
        int alpha2 = GlyphTint.alpha(i, 0.95F);
        this.line(box2.minX, d, box2.minZ, box2.maxX, d, box2.minZ, alpha2, 2.4F);
        this.line(box2.maxX, d, box2.minZ, box2.maxX, d, box2.maxZ, alpha2, 2.4F);
        this.line(box2.maxX, d, box2.maxZ, box2.minX, d, box2.maxZ, alpha2, 2.4F);
        this.line(box2.minX, d, box2.maxZ, box2.minX, d, box2.minZ, alpha2, 2.4F);
    }

    private void drawTracers(
            List<NetheriteVisuals.Section> list, NetheriteVisuals.Options options) {
        int i = 0;
        for (NetheriteVisuals.Section section : list) {
            if (i >= options.tracerCount()) {
                break;
            }
            if (!(section.distance() < 1.0)) {
                Vec3d center = section.box().getCenter();
                TracerEngine.draw(
                        center, GlyphTint.alpha(tint(options, section.distance()), 0.85F));
                String string =
                        section.progress() >= 0.0F
                                ? Math.round(section.progress() * 100.0F) + "%"
                                : null;
                this.labels.add(
                        new NetheriteVisuals.Label(
                                center,
                                String.format(Locale.ROOT, "%.0fm", section.distance()),
                                string));
                i++;
            }
        }
    }

    private void drawProbes(
            Vec3d vec,
            AdvancedFinder advancedFinder,
            NetheriteVisuals.Options options,
            int i,
            int j,
            float f) {
        int n = GlyphTint.mix(j, -1, 0.5F);
        advancedFinder.forEachMissing(
                vec,
                32.0,
                1600,
                options.missingFrom(),
                (jx, k, value) ->
                        this.canvas.sprite(
                                GlyphShapes.DIAMOND,
                                jx + 0.5,
                                k + 0.5,
                                value + 0.5,
                                0.11F + 0.05F * f,
                                0.0F,
                                GlyphTint.alpha(n, 0.5F + 0.45F * f)));
        int o = GlyphTint.mix(j, -1, 0.6F);
        String probeStyle2 = options.probeStyle();
        int p = "Cubes".equals(probeStyle2) ? 450 : 1800;
        advancedFinder.forEachProbe(
                vec,
                12.0,
                p,
                options.missingFrom(),
                (k, value2, m, value3) -> {
                    float g = Math.min(1.0F, value3 / 14.0F);
                    int q = GlyphTint.mix(o, i, g);
                    double d = k + 0.5;
                    double e = value2 + 0.5;
                    double d2 = m + 0.5;
                    if ("Cubes".equals(probeStyle2)) {
                        double e2 = 0.2 - 0.1 * g;
                        this.faces(
                                new Box(d - e2, e - e2, d2 - e2, d + e2, e + e2, d2 + e2),
                                GlyphTint.alpha(q, 0.65F - 0.3F * g));
                    } else {
                        this.canvas.sprite(
                                GlyphShapes.DIAMOND,
                                d,
                                e,
                                d2,
                                0.13F - 0.06F * g,
                                0.0F,
                                GlyphTint.alpha(q, 0.95F - 0.5F * g));
                        if ("Pulses".equals(probeStyle2) && g < 1.0F) {
                            this.canvas.sprite(
                                    GlyphShapes.HALO,
                                    d,
                                    e,
                                    d2,
                                    0.15F + 0.55F * g,
                                    0.0F,
                                    GlyphTint.alpha(o, 0.55F * (1.0F - g) * (1.0F - g)));
                        }
                    }
                });
    }

    private void drawRevealed(
            Vec3d vec,
            List<BlockPos> list,
            NetheriteVisuals.Options options,
            int i,
            int j,
            float f) {
        ArrayList list2 = new ArrayList(list);
        list2.sort(Comparator.comparingDouble((BlockPos pos2) -> pos2.getSquaredDistance(vec)));
        BlockPos pos3;
        double d;
        for (int k = 0;
                k < list2.size()
                        && !((d =
                                        Math.sqrt(
                                                (pos3 = (BlockPos) list2.get(k))
                                                        .getSquaredDistance(vec)))
                                > options.range());
                k++) {
            Box box2 = new Box(pos3);
            if (k < 32) {
                for (int m = 0; m < SHELL_INFLATE.length; m++) {
                    this.faces(
                            box2.expand(SHELL_INFLATE[m]),
                            GlyphTint.alpha(i, SHELL_ALPHA[m] * (0.7F + 0.3F * f)));
                }
            }
            this.faces(box2, GlyphTint.alpha(i, 0.3F));
            this.box(box2, GlyphTint.alpha(j, 0.95F), 2.6F);
            Vec3d center = box2.getCenter();
            if (k < 3) {
                TracerEngine.draw(center, GlyphTint.alpha(i, 0.8F));
            }
            if (k < 8) {
                this.labels.add(
                        new NetheriteVisuals.Label(
                                center.add(0.0, 0.9, 0.0),
                                String.format(Locale.ROOT, "%.0fm", d),
                                null));
            }
        }
    }

    private void drawWaves(long l, int i, int j) {
        Iterator iterator = this.waves.iterator();
        while (iterator.hasNext()) {
            NetheriteVisuals.Wave wave = (NetheriteVisuals.Wave) iterator.next();
            float f = (float) (l - wave.born()) / 1.0E9F / 0.9F;
            if (f >= 1.0F) {
                iterator.remove();
            } else {
                float g = 1.0F - (1.0F - f) * (1.0F - f) * (1.0F - f);
                float h = 0.4F + 2.6F * g;
                float f2 = (1.0F - f) * (1.0F - f);
                this.canvas.ring(
                        wave.x(),
                        wave.y(),
                        wave.z(),
                        h * 0.86F,
                        h,
                        48,
                        GlyphTint.alpha(j, f2 * 0.9F));
                this.canvas.ring(
                        wave.x(),
                        wave.y(),
                        wave.z(),
                        h,
                        h * 1.25F,
                        48,
                        GlyphTint.alpha(i, f2 * 0.3F));
            }
        }
    }

    private void line(
            double d, double e, double d2, double e2, double d3, double e3, int i, float f) {
        this.canvas.ribbon(d, e, d2, e2, d3, e3, i, f, this.pixel);
    }

    private void box(Box box2, int i, float f) {
        this.line(box2.minX, box2.minY, box2.minZ, box2.maxX, box2.minY, box2.minZ, i, f);
        this.line(box2.maxX, box2.minY, box2.minZ, box2.maxX, box2.minY, box2.maxZ, i, f);
        this.line(box2.maxX, box2.minY, box2.maxZ, box2.minX, box2.minY, box2.maxZ, i, f);
        this.line(box2.minX, box2.minY, box2.maxZ, box2.minX, box2.minY, box2.minZ, i, f);
        this.line(box2.minX, box2.maxY, box2.minZ, box2.maxX, box2.maxY, box2.minZ, i, f);
        this.line(box2.maxX, box2.maxY, box2.minZ, box2.maxX, box2.maxY, box2.maxZ, i, f);
        this.line(box2.maxX, box2.maxY, box2.maxZ, box2.minX, box2.maxY, box2.maxZ, i, f);
        this.line(box2.minX, box2.maxY, box2.maxZ, box2.minX, box2.maxY, box2.minZ, i, f);
        this.line(box2.minX, box2.minY, box2.minZ, box2.minX, box2.maxY, box2.minZ, i, f);
        this.line(box2.maxX, box2.minY, box2.minZ, box2.maxX, box2.maxY, box2.minZ, i, f);
        this.line(box2.maxX, box2.minY, box2.maxZ, box2.maxX, box2.maxY, box2.maxZ, i, f);
        this.line(box2.minX, box2.minY, box2.maxZ, box2.minX, box2.maxY, box2.maxZ, i, f);
    }

    private void faces(Box box2, int i) {
        this.face(
                box2.minX, box2.minY, box2.minZ, box2.maxX, box2.minY, box2.minZ, box2.maxX,
                box2.minY, box2.maxZ, box2.minX, box2.minY, box2.maxZ, i);
        this.face(
                box2.minX, box2.maxY, box2.minZ, box2.maxX, box2.maxY, box2.minZ, box2.maxX,
                box2.maxY, box2.maxZ, box2.minX, box2.maxY, box2.maxZ, i);
        this.face(
                box2.minX, box2.minY, box2.minZ, box2.maxX, box2.minY, box2.minZ, box2.maxX,
                box2.maxY, box2.minZ, box2.minX, box2.maxY, box2.minZ, i);
        this.face(
                box2.minX, box2.minY, box2.maxZ, box2.maxX, box2.minY, box2.maxZ, box2.maxX,
                box2.maxY, box2.maxZ, box2.minX, box2.maxY, box2.maxZ, i);
        this.face(
                box2.minX, box2.minY, box2.minZ, box2.minX, box2.minY, box2.maxZ, box2.minX,
                box2.maxY, box2.maxZ, box2.minX, box2.maxY, box2.minZ, i);
        this.face(
                box2.maxX, box2.minY, box2.minZ, box2.maxX, box2.minY, box2.maxZ, box2.maxX,
                box2.maxY, box2.maxZ, box2.maxX, box2.maxY, box2.minZ, i);
    }

    private void face(
            double d,
            double e,
            double d2,
            double e2,
            double d3,
            double e3,
            double d4,
            double e4,
            double d5,
            double e5,
            double d6,
            double e6,
            int i) {
        this.canvas.quad(d, e, d2, e2, d3, e3, d4, e4, d5, e5, d6, e6, i);
        this.canvas.quad(d, e, d2, e5, d6, e6, d4, e4, d5, e2, d3, e3, i);
    }

    private float jitter(float f) {
        return (this.random.nextFloat() * 2.0F - 1.0F) * f;
    }

    @Environment(EnvType.CLIENT)
    private record Label(Vec3d pos, String primary, String secondary) {}

    @Environment(EnvType.CLIENT)
    record Options(
            int near,
            int far,
            double fade,
            double range,
            String probeStyle,
            float missingFrom,
            int tracerCount,
            boolean showProbes) {}

    @Environment(EnvType.CLIENT)
    record Section(Box box, double distance, float progress) {}

    @Environment(EnvType.CLIENT)
    private record Wave(double x, double y, double z, long born) {}
}
