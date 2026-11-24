package net.efn.client.module.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;

import net.efn.client.config.FriendStore;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.ShaderOverlay;
import net.efn.client.util.combat.CombatTarget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

import org.joml.Matrix4f;

import java.util.Arrays;

@Environment(EnvType.CLIENT)
public final class ShaderEspModule extends ClientModule {

    public static volatile ShaderEspModule active;

    private static final String COLOR_THEME = "Theme";

    private static final String COLOR_MANUAL = "Manual";

    private static final int OPAQUE = -16777216;

    private static final int WHITE = -1;

    private static final float FRIEND_LIFT = 0.45F;

    private static final float TARGET_LIFT = 0.8F;

    private static final float SHEEN_LIFT = 0.6F;

    private static final int LIMIT_CAP = 96;

    private static final long TARGET_MEMORY_MS = 3500L;

    private static final long BREATH_NANOS = 2600000000L;

    private static final long SHEEN_NANOS = 5200000000L;

    private static final double CULL_MARGIN = 0.75;

    private final BooleanSetting players =
            this.addSetting("players", new BooleanSetting("Players", true));

    private final BooleanSetting passives =
            this.addSetting("passives", new BooleanSetting("Passives", false));

    private final BooleanSetting items =
            this.addSetting("items", new BooleanSetting("Items", false));

    private final BooleanSetting crystals =
            this.addSetting("crystals", new BooleanSetting("Crystals", true));

    private final BooleanSetting markTarget =
            this.addSetting("mark-target", new BooleanSetting("Mark Target", true));

    private final ModeSetting colorMode =
            this.addSetting("color", new ModeSetting("Color", "Theme", "Theme", "Manual"));

    private final ColorSetting playerColor =
            this.addSetting("player-color", new ColorSetting("Player Color", -13338378));

    private final ColorSetting friendColor =
            this.addSetting("friend-color", new ColorSetting("Friend Color", -7358982));

    private final ColorSetting passiveColor =
            this.addSetting("passive-color", new ColorSetting("Passive Color", -13061400));

    private final ColorSetting itemColor =
            this.addSetting("item-color", new ColorSetting("Item Color", -11744808));

    private final ColorSetting crystalColor =
            this.addSetting("crystal-color", new ColorSetting("Crystal Color", -6128394));

    private final ColorSetting targetColor =
            this.addSetting("target-color", new ColorSetting("Target Color", -2693891));

    private final NumberSetting stroke =
            this.addSetting("stroke", new NumberSetting("Stroke", 2.0F, 0.5F, 6.0F).step(0.5F));

    private final NumberSetting aura =
            this.addSetting("aura", new NumberSetting("Aura", 55.0F, 0.0F, 100.0F).step(5.0F));

    private final NumberSetting auraReach =
            this.addSetting(
                    "aura-reach", new NumberSetting("Aura Reach", 10.0F, 3.0F, 24.0F).step(1.0F));

    private final NumberSetting fill =
            this.addSetting("fill", new NumberSetting("Fill", 10.0F, 0.0F, 60.0F).step(5.0F));

    private final NumberSetting sheen =
            this.addSetting("sheen", new NumberSetting("Sheen", 30.0F, 0.0F, 100.0F).step(5.0F));

    private final ColorSetting sheenColor =
            this.addSetting("sheen-color", new ColorSetting("Sheen Color", -2299649));

    private final BooleanSetting breathe =
            this.addSetting("breathe", new BooleanSetting("Breathe", false));

    private final NumberSetting distance =
            this.addSetting(
                    "distance", new NumberSetting("Distance", 64.0F, 8.0F, 192.0F).step(4.0F));

    private final NumberSetting limit =
            this.addSetting("limit", new NumberSetting("Limit", 48.0F, 4.0F, 96.0F).step(1.0F));

    private final Entity[] picked = new Entity[96];

    private final int[] tints = new int[96];

    private final double[] spans = new double[96];

    private int count;

    private long pickedFrame = -9223372036854775808L;

    public ShaderEspModule() {
        super(
                "glow-esp",
                "Glow ESP",
                "Outlines entities through walls with a glow.",
                ModuleCategory.RENDER);
        this.playerColor.visibleWhen(() -> this.players.get() && this.manual());
        this.friendColor.visibleWhen(() -> this.players.get() && this.manual());
        this.passiveColor.visibleWhen(() -> this.passives.get() && this.manual());
        this.itemColor.visibleWhen(() -> this.items.get() && this.manual());
        this.crystalColor.visibleWhen(() -> this.crystals.get() && this.manual());
        this.targetColor.visibleWhen(() -> this.markTarget.get() && this.manual());
        this.auraReach.visibleWhen(() -> this.aura.get() > 0.0F);
        this.sheenColor.visibleWhen(() -> this.sheen.get() > 0.0F && this.manual());
    }

    @Override
    protected void onEnable() {
        active = this;
        this.on(Render3DEvent.TYPE, this::onRender3D);
    }

    @Override
    protected void onDisable() {
        active = null;
        this.forget();
        if (RenderSystem.isOnRenderThread()) {
            ShaderEspPass.release();
        } else {
            MinecraftClient.getInstance().execute(ShaderEspModule::releaseIfOff);
        }
    }

    private static void releaseIfOff() {
        if (active == null) {
            ShaderEspPass.release();
        }
    }

    public static void afterLevel(Camera camera, Matrix4f matrix, Matrix4f matrix2) {
        ShaderEspModule module = active;
        if (module != null) {
            module.present(camera, matrix, matrix2);
        }
    }

    public static void extracted(Entity entity, EntityRenderState entityRenderState) {
        if (active != null) {
            ShaderEspPass.keep(entity, entityRenderState, Render3DEvent.frame());
        }
    }

    private void onRender3D(Render3DEvent event) {
        this.pick(event.frustum);
        this.pickedFrame = Render3DEvent.frame();
        if (this.count > 0 && ShaderOverlay.shaderPackInUse()) {
            this.sketch(event.partialTick);
        }
    }

    private void present(Camera camera, Matrix4f matrix, Matrix4f matrix2) {
        int count2 = this.count;
        try {
            if (count2 == 0
                    || this.pickedFrame + 1L != Render3DEvent.frame()
                    || ShaderOverlay.shaderPackInUse()) {
                return;
            }
            if (ShaderEspPass.paint(
                    camera, matrix, matrix2, this.picked, this.tints, count2, this.pickedFrame)) {
                long l = System.nanoTime();
                float f =
                        Math.max(
                                1.0F,
                                MinecraftClient.getInstance().getFramebuffer().textureHeight
                                        / 1080.0F);
                float g = (float) (l % 5200000000L / 5.2E9);
                ShaderEspPass.compose(
                        this.stroke.get() * f,
                        this.auraStrength(l),
                        this.auraReach.get() * f,
                        this.fill.get() / 100.0F,
                        g,
                        this.sheenTint(),
                        this.sheen.get() / 100.0F);
                return;
            }
        } finally {
            this.forget();
            ShaderEspPass.dropKept();
        }
    }

    private boolean manual() {
        return this.colorMode.is("Manual");
    }

    private int sheenTint() {
        return this.manual()
                ? this.sheenColor.argb() | 0xFF000000
                : ColorHelper.lerp(0.6F, CosmeticAccent.argb(), -1);
    }

    private float auraStrength(long l) {
        float f = this.aura.get() / 100.0F;
        if (!this.breathe.get()) {
            return f;
        } else {
            double d = l % 2600000000L / 2.6E9;
            float g = (float) (0.5 - 0.5 * Math.cos(d * 3.141592653589793 * 2.0));
            g = g * g * (3.0F - 2.0F * g);
            return f * (0.45F + 0.55F * g);
        }
    }

    private void pick(Frustum frustum) {
        this.forget();
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        ClientPlayerEntity player = client.player;
        if (world != null && player != null) {
            Camera camera = client.gameRenderer.getCamera();
            Vec3d cameraPos = camera.getCameraPos();
            Entity entity = camera.isThirdPerson() ? null : client.getCameraEntity();
            double d = (double) this.distance.get() * this.distance.get();
            int i = Math.max(1, Math.min(96, this.limit.getInt()));
            LivingEntity entity2 = this.markTarget.get() ? CombatTarget.current(3500L) : null;
            boolean bl = !this.manual();
            int j = bl ? CosmeticAccent.argb() : 0;
            for (Entity entity3 : world.getEntities()) {
                double squaredDistance;
                int tint;
                if (entity3 != player
                        && entity3 != entity
                        && !entity3.isRemoved()
                        && (tint = this.tintOf(entity3, entity2, bl, j)) != 0
                        && !((squaredDistance = entity3.squaredDistanceTo(cameraPos)) > d)
                        && (frustum == null
                                || frustum.isVisible(entity3.getBoundingBox().expand(0.75)))) {
                    this.admit(entity3, tint, squaredDistance, i);
                }
            }
        }
    }

    private int tintOf(Entity entity, LivingEntity entity2, boolean bl, int i) {
        LivingEntity entity3;
        if (entity instanceof LivingEntity && (entity3 = (LivingEntity) entity).isDead()) {
            return 0;
        } else {
            boolean bl2 = false;
            ColorSetting playerColor2;
            if (entity instanceof PlayerEntity player) {
                if (!this.players.get() || player.isSpectator()) {
                    return 0;
                }
                bl2 = FriendStore.getInstance().isFriend(player.getGameProfile().name());
                playerColor2 = this.playerColor;
            } else if (entity instanceof EndCrystalEntity) {
                if (!this.crystals.get()) {
                    return 0;
                }
                playerColor2 = this.crystalColor;
            } else if (entity instanceof ItemEntity) {
                if (!this.items.get()) {
                    return 0;
                }
                playerColor2 = this.itemColor;
            } else {
                if (entity instanceof Monster) {
                    return 0;
                }
                if (!(entity instanceof MobEntity)) {
                    return 0;
                }
                if (!this.passives.get()) {
                    return 0;
                }
                playerColor2 = this.passiveColor;
            }
            if (entity2 != null && entity == entity2) {
                return bl ? ColorHelper.lerp(0.8F, i, -1) : this.targetColor.argb() | 0xFF000000;
            } else if (bl2) {
                return bl ? ColorHelper.lerp(0.45F, i, -1) : this.friendColor.argb() | 0xFF000000;
            } else {
                return bl ? i : playerColor2.argb() | 0xFF000000;
            }
        }
    }

    private void admit(Entity entity, int i, double d, int j) {
        int count2 = this.count;
        if (this.count < j) {
            this.count++;
        } else {
            count2 = 0;
            for (int m = 1; m < this.count; m++) {
                if (this.spans[m] > this.spans[count2]) {
                    count2 = m;
                }
            }
            if (this.spans[count2] <= d) {
                return;
            }
        }
        this.picked[count2] = entity;
        this.tints[count2] = i;
        this.spans[count2] = d;
    }

    private void sketch(float f) {
        float g = Math.max(1.0F, this.stroke.get());
        for (int i = 0; i < this.count; i++) {
            Entity entity = this.picked[i];
            Vec3d entityPos = entity.getEntityPos();
            Vec3d lerpedPos = entity.getLerpedPos(f);
            Box box =
                    entity.getBoundingBox()
                            .offset(
                                    lerpedPos.x - entityPos.x,
                                    lerpedPos.y - entityPos.y,
                                    lerpedPos.z - entityPos.z);
            GizmoDrawing.box(box, DrawStyle.stroked(this.tints[i], g)).ignoreOcclusion();
        }
    }

    private void forget() {
        Arrays.fill(this.picked, 0, Math.min(this.count, 96), null);
        this.count = 0;
    }
}
