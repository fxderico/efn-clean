package net.efn.client.module.impl.render;

import imgui.ImDrawList;

import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.gui.WorldLabel;
import net.efn.client.module.ClientModule;
import net.efn.client.module.HudOverlayProvider;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.misc.NameProtectModule;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.PearlTrajectorySimulator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class PearlTrajectoryModule extends ClientModule implements HudOverlayProvider {

    private static final int MAX_TRAIL_POINTS = 200;

    private static final double MIN_TRAIL_DISTANCE_SQ = 1.0E-4;

    private static final double PIECE_LENGTH = 0.45;

    private static final double FADE_IN_DISTANCE = 1.8;

    private static final int RING_SEGMENTS = 48;

    private static final float POP_IN_SECONDS = 0.22F;

    private static final float WAVE_PERIOD_SECONDS = 1.4F;

    private static final long LABEL_MAX_AGE_NANOS = 250000000L;

    private static final long EPOCH = System.nanoTime();

    private static final String LANDING_RING = "Ring";

    private static final String LANDING_BOX = "Box";

    private static final String LANDING_NONE = "None";

    private final Map<Integer, List<Vec3d>> trails = new HashMap<>();

    private final List<PearlTrajectoryModule.Label> labels = new ArrayList<>();

    private long labelsAt;

    private long predictionShownAt = -1L;

    private final BooleanSetting sPredict =
            this.addSetting("predict", new BooleanSetting("Predict", true));

    private final BooleanSetting sTrail =
            this.addSetting("trail", new BooleanSetting("Trail", true));

    private final BooleanSetting sOthers =
            this.addSetting("others-pearls", new BooleanSetting("Others' Pearls", false));

    private final ModeSetting sLanding =
            this.addSetting("landing", new ModeSetting("Landing", "Ring", "Ring", "Box", "None"));

    private final BooleanSetting sInfo =
            this.addSetting("landing-info", new BooleanSetting("Landing Info", true));

    private final BooleanSetting sGlow = this.addSetting("glow", new BooleanSetting("Glow", true));

    private final BooleanSetting sGradient =
            this.addSetting("gradient", new BooleanSetting("Gradient", true));

    private final ColorSetting sColor =
            this.addSetting("color", new ColorSetting("Color", -9524994));

    private final ColorSetting sEndColor =
            this.addSetting("end-color", new ColorSetting("End Color", -3697942));

    private final ColorSetting sHitColor =
            this.addSetting("hit-color", new ColorSetting("Hit Color", -42406));

    private final NumberSetting sLineWidth =
            this.addSetting(
                    "line-width", new NumberSetting("Line Width", 2.0F, 0.5F, 5.0F).step(0.5F));

    public PearlTrajectoryModule() {
        super(
                "pearl-trajectory",
                "Pearl Trajectory",
                "Predicts where a held pearl lands and previews the flight of thrown pearls.",
                ModuleCategory.RENDER);
        this.sEndColor.visibleWhen(this.sGradient::get);
    }

    @Override
    protected void onDisable() {
        this.trails.clear();
        this.labels.clear();
        this.predictionShownAt = -1L;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null && client.player != null) {
            HashSet set = new HashSet();
            for (Entity entity : client.world.getEntities()) {
                EnderPearlEntity enderPearlEntity;
                if (entity instanceof EnderPearlEntity
                        && this.isTracked(enderPearlEntity = (EnderPearlEntity) entity, client)) {
                    set.add(enderPearlEntity.getId());
                    List list =
                            this.trails.computeIfAbsent(
                                    enderPearlEntity.getId(), integer -> new ArrayList<>());
                    Vec3d entityPos = enderPearlEntity.getEntityPos();
                    if (list.isEmpty()
                            || !(((Vec3d) list.getLast()).squaredDistanceTo(entityPos) < 1.0E-4)) {
                        list.add(entityPos);
                        if (list.size() > 200) {
                            list.removeFirst();
                        }
                    }
                }
            }
            this.trails.keySet().retainAll(set);
        } else {
            this.trails.clear();
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        this.labels.clear();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.world != null) {
            long l;
            this.labelsAt = l = System.nanoTime();
            float f = (float) (l - EPOCH) / 1.0E9F;
            float sLineWidth2 = this.sLineWidth.get();
            Vec3d cameraPosVec = client.player.getCameraPosVec(event.partialTick);
            if (this.sPredict.get() && PearlTrajectorySimulator.isHoldingPearl(client.player)) {
                if (this.predictionShownAt < 0L) {
                    this.predictionShownAt = l;
                }
                float easeOutBack =
                        easeOutBack(
                                Math.min(
                                        1.0F,
                                        (float) (l - this.predictionShownAt) / 1.0E9F / 0.22F));
                PearlTrajectorySimulator.SimulationResult simulationResult =
                        PearlTrajectorySimulator.simulate(
                                client.world, client.player, event.partialTick);
                this.drawPath(simulationResult.points(), sLineWidth2, true, false, 1.0F);
                this.drawLanding(simulationResult, easeOutBack, f, sLineWidth2, 1.0F);
                this.addLabel(simulationResult, cameraPosVec, null);
            } else {
                this.predictionShownAt = -1L;
            }
            for (Entity entity : client.world.getEntities()) {
                EnderPearlEntity enderPearlEntity;
                if (entity instanceof EnderPearlEntity
                        && this.isTracked(enderPearlEntity = (EnderPearlEntity) entity, client)) {
                    Vec3d lerpedPos = enderPearlEntity.getLerpedPos(event.partialTick);
                    if (this.sTrail.get()) {
                        this.drawTrail(
                                this.trails.get(enderPearlEntity.getId()), lerpedPos, sLineWidth2);
                    }
                    PearlTrajectorySimulator.SimulationResult simulationResult2 =
                            PearlTrajectorySimulator.simulate(client.world, enderPearlEntity);
                    ArrayList list = new ArrayList<>(simulationResult2.points());
                    list.set(0, lerpedPos);
                    this.drawPath(list, sLineWidth2 * 0.8F, false, true, 0.7F);
                    this.drawLanding(simulationResult2, 1.0F, f, sLineWidth2, 0.75F);
                    Entity owner = enderPearlEntity.getOwner();
                    boolean bl = owner == null || owner == client.player;
                    this.addLabel(
                            simulationResult2,
                            cameraPosVec,
                            bl ? null : NameProtectModule.protect(owner.getName()).getString());
                }
            }
        }
    }

    private void drawPath(List<Vec3d> list, float f, boolean bl, boolean bl2, float g) {
        if (list.size() >= 2) {
            double d = 0.0;
            for (int i = 1; i < list.size(); i++) {
                d += ((Vec3d) list.get(i - 1)).distanceTo((Vec3d) list.get(i));
            }
            if (!(d <= 1.0E-6)) {
                int j = this.sColor.argb();
                int k = this.sGradient.get() ? this.sEndColor.argb() : j;
                boolean sGlow2 = this.sGlow.get();
                double e = 0.0;
                int m = 0;
                for (int n = 1; n < list.size(); n++) {
                    Vec3d vec = (Vec3d) list.get(n - 1);
                    Vec3d vec2 = (Vec3d) list.get(n);
                    double distance = vec.distanceTo(vec2);
                    int o = Math.max(1, (int) Math.ceil(distance / 0.45));
                    for (int p = 0; p < o; m++) {
                        if (!bl2 || (m & 1) == 0) {
                            double d2 = (double) p / o;
                            double e2 = (double) (p + 1) / o;
                            double d3 = e + distance * (d2 + e2) * 0.5;
                            float h = g;
                            if (bl) {
                                h = g * smoothstep((float) (d3 / 1.8));
                            }
                            if (!(h < 0.02F)) {
                                int scaleAlpha = scaleAlpha(lerpArgb(j, k, (float) (d3 / d)), h);
                                Vec3d vec3 = vec.lerp(vec2, d2);
                                Vec3d vec4 = vec.lerp(vec2, e2);
                                if (sGlow2) {
                                    GizmoDrawing.line(
                                                    vec3,
                                                    vec4,
                                                    scaleAlpha(scaleAlpha, 0.2F),
                                                    f * 3.2F)
                                            .ignoreOcclusion();
                                }
                                GizmoDrawing.line(vec3, vec4, scaleAlpha, f).ignoreOcclusion();
                            }
                        }
                        p++;
                    }
                    e += distance;
                }
            }
        }
    }

    private void drawTrail(List<Vec3d> list, Vec3d vec, float f) {
        if (list != null && !list.isEmpty()) {
            int i = this.sColor.argb();
            int j = list.size() + 1;
            for (int k = 1; k < j; k++) {
                Vec3d vec2 = (Vec3d) list.get(k - 1);
                Vec3d vec3 = k < list.size() ? (Vec3d) list.get(k) : vec;
                float g = 0.15F + 0.85F * k / (j - 1.0F);
                GizmoDrawing.line(vec2, vec3, scaleAlpha(i, g), f).ignoreOcclusion();
            }
        }
    }

    private void drawLanding(
            PearlTrajectorySimulator.SimulationResult simulationResult,
            float f,
            float g,
            float h,
            float f2) {
        int scaleAlpha = scaleAlpha(this.sHitColor.argb(), f2);
        if (simulationResult.entityHit() != null) {
            Box box = simulationResult.entityHit().getBoundingBox().expand(0.05);
            GizmoDrawing.box(
                            box,
                            DrawStyle.filledAndStroked(
                                    scaleAlpha, h, scaleAlpha(scaleAlpha, 0.14F)))
                    .ignoreOcclusion();
        } else {
            BlockHitResult blockHitResult = simulationResult.blockHit();
            String sLanding2 = this.sLanding.get();
            if (blockHitResult != null && !"None".equals(sLanding2)) {
                int scaleAlpha2 =
                        scaleAlpha(
                                this.sGradient.get() ? this.sEndColor.argb() : this.sColor.argb(),
                                f2);
                Direction side = blockHitResult.getSide();
                Vec3d vec = new Vec3d(side.getOffsetX(), side.getOffsetY(), side.getOffsetZ());
                if ("Box".equals(sLanding2)) {
                    Box box2 = new Box(blockHitResult.getBlockPos()).expand(0.002);
                    GizmoDrawing.face(
                                    box2.getMinPos(),
                                    box2.getMaxPos(),
                                    side,
                                    DrawStyle.filledAndStroked(
                                            scaleAlpha2, h, scaleAlpha(scaleAlpha2, 0.22F)))
                            .ignoreOcclusion();
                } else {
                    Vec3d vec2 = simulationResult.landing().add(vec.multiply(0.02));
                    float g2 = 1.0F + 0.05F * (float) Math.sin(g * 5.0F);
                    drawRing(vec2, vec, 0.38F * f * g2, scaleAlpha2, h);
                    drawCross(vec2, vec, 0.09F * f, scaleAlpha2, h);
                    float h2 = g % 1.4F / 1.4F;
                    float f3 = lerp(0.3F, 0.95F, easeOutCubic(h2)) * f;
                    drawRing(vec2, vec, f3, scaleAlpha(scaleAlpha2, 0.75F * (1.0F - h2)), h * 0.8F);
                }
            }
        }
    }

    private static void drawRing(Vec3d vec, Vec3d vec2, float f, int i, float g) {
        if ((i >>> 24 & 0xFF) >= 4 && !(f <= 0.0F)) {
            Vec3d[] vecs = planeBasis(vec2);
            Vec3d vec3 = null;
            for (int j = 0; j <= 48; j++) {
                double d = 6.283185307179586 * j / 48.0;
                Vec3d vec4 =
                        vec.add(vecs[0].multiply(Math.cos(d) * f))
                                .add(vecs[1].multiply(Math.sin(d) * f));
                if (vec3 != null) {
                    GizmoDrawing.line(vec3, vec4, i, g).ignoreOcclusion();
                }
                vec3 = vec4;
            }
        }
    }

    private static void drawCross(Vec3d vec, Vec3d vec2, float f, int i, float g) {
        Vec3d[] vecs = planeBasis(vec2);
        GizmoDrawing.line(vec.add(vecs[0].multiply(-f)), vec.add(vecs[0].multiply(f)), i, g)
                .ignoreOcclusion();
        GizmoDrawing.line(vec.add(vecs[1].multiply(-f)), vec.add(vecs[1].multiply(f)), i, g)
                .ignoreOcclusion();
    }

    private static Vec3d[] planeBasis(Vec3d vec) {
        Vec3d vec2 =
                Math.abs(vec.y) > 0.9
                        ? new Vec3d(1.0, 0.0, 0.0)
                        : vec.crossProduct(new Vec3d(0.0, 1.0, 0.0)).normalize();
        return new Vec3d[] {vec2, vec.crossProduct(vec2).normalize()};
    }

    private void addLabel(
            PearlTrajectorySimulator.SimulationResult simulationResult, Vec3d vec, String string) {
        if (this.sInfo.get() && simulationResult.landed()) {
            String string2 = String.format(Locale.ROOT, "%.1fs", simulationResult.ticks() / 20.0F);
            String string3 =
                    String.format(Locale.ROOT, "%.1fm", vec.distanceTo(simulationResult.landing()));
            String string4 =
                    simulationResult.entityHit() != null
                            ? NameProtectModule.protect(simulationResult.entityHit().getName())
                                            .getString()
                                    + "  \u00b7  "
                                    + string2
                            : (string != null ? string + "  \u00b7  " + string2 : string2);
            this.labels.add(
                    new PearlTrajectoryModule.Label(simulationResult.landing(), string3, string4));
        }
    }

    private boolean isTracked(EnderPearlEntity enderPearlEntity, MinecraftClient client) {
        if (enderPearlEntity.isRemoved()) {
            return false;
        } else {
            Entity owner = enderPearlEntity.getOwner();
            return owner == null || owner == client.player || this.sOthers.get();
        }
    }

    @Override
    public boolean isHudOverlayEnabled() {
        return this.isEnabled() && this.sInfo.get();
    }

    @Override
    public void renderHudOverlay(ImDrawList imDrawList, float f, float g) {
        if (!this.labels.isEmpty() && System.nanoTime() - this.labelsAt <= 250000000L) {
            MinecraftClient client = MinecraftClient.getInstance();
            for (PearlTrajectoryModule.Label label : this.labels) {
                float[] floats = WorldLabel.project(client, label.pos().add(0.0, 0.35, 0.0), f, g);
                if (floats != null) {
                    WorldLabel.drawText(
                            imDrawList,
                            floats[0],
                            floats[1],
                            label.primary(),
                            label.secondary(),
                            1.0F);
                }
            }
        }
    }

    private static int lerpArgb(int i, int j, float f) {
        f = Math.max(0.0F, Math.min(1.0F, f));
        int k = i >>> 24;
        int m = i >> 16 & 0xFF;
        int n = i >> 8 & 0xFF;
        int o = i & 0xFF;
        int p = j >>> 24;
        int q = j >> 16 & 0xFF;
        int r = j >> 8 & 0xFF;
        int t = j & 0xFF;
        return Math.round(k + (p - k) * f) << 24
                | Math.round(m + (q - m) * f) << 16
                | Math.round(n + (r - n) * f) << 8
                | Math.round(o + (t - o) * f);
    }

    private static int scaleAlpha(int i, float f) {
        int j = Math.round((i >>> 24) * Math.max(0.0F, Math.min(1.0F, f)));
        return j << 24 | i & 16777215;
    }

    private static float lerp(float f, float g, float h) {
        return f + (g - f) * h;
    }

    private static float smoothstep(float f) {
        f = Math.max(0.0F, Math.min(1.0F, f));
        return f * f * (3.0F - 2.0F * f);
    }

    private static float easeOutCubic(float f) {
        float g = 1.0F - f;
        return 1.0F - g * g * g;
    }

    private static float easeOutBack(float f) {
        float g = 1.70158F;
        float h = g + 1.0F;
        float f2 = f - 1.0F;
        return 1.0F + h * f2 * f2 * f2 + g * f2 * f2;
    }

    @Environment(EnvType.CLIENT)
    private record Label(Vec3d pos, String primary, String secondary) {}
}
