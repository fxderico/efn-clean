package net.efn.client.module.impl.render;

import imgui.ImDrawList;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import net.efn.client.event.Stage;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.gui.WorldLabel;
import net.efn.client.module.ClientModule;
import net.efn.client.module.HudOverlayProvider;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.misc.NameProtectModule;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.Setting;
import net.efn.client.util.Ballistics;
import net.efn.client.util.combat.CombatTarget;
import net.efn.client.util.glyph.GlyphCanvas;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.EggItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.item.SnowballItem;
import net.minecraft.item.ThrowablePotionItem;
import net.minecraft.item.TridentItem;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import net.minecraft.world.debug.gizmo.VisibilityConfigurable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Environment(EnvType.CLIENT)
public final class TrajectoriesModule extends ClientModule implements HudOverlayProvider {

    private static final String PATH_BEADS = "Beads";

    private static final String PATH_DASHES = "Dashes";

    private static final String PATH_SOLID = "Solid";

    private static final String COLOR_THEME = "Theme";

    private static final String COLOR_MANUAL = "Manual";

    private static final String MARKER_BOLT = "Bolt";

    private static final String MARKER_SUN = "Ring";

    private static final String MARKER_NONE = "None";

    private static final int WHITE = -1;

    private static final float HEAD_LIFT = 0.35F;

    private static final float SHEEN_LIFT = 0.8F;

    private static final float PLATE_LIFT = 0.55F;

    private static final float HIT_LIFT = 0.5F;

    private static final int VOLLEY_CAP = 3;

    private static final int TRACK_CAP = 24;

    private static final int TRACK_LIFETIME = 200;

    private static final double RESTING_SPEED_SQR = 1.0E-4;

    private static final double AIM_DRIFT = 0.001;

    private static final double AIM_SWAY = 5.0E-4;

    private static final float BOW_MIN_PULL = 0.1F;

    private static final float BOW_SPEED = 3.0F;

    private static final int TRIDENT_WINDUP = 10;

    private static final float CROSSBOW_ARROW_SPEED = 3.15F;

    private static final float CROSSBOW_ROCKET_SPEED = 1.6F;

    private static final float MULTISHOT_FAN = 10.0F;

    private static final float BOTTLE_SPEED = 0.7F;

    private static final float LOB_TILT = -20.0F;

    private static final double MUZZLE_DROP = 0.1;

    private static final double ROCKET_DROP = 0.15;

    private static final int ROCKET_FUSE_SLACK = 6;

    private static final double BEAD_PERIOD = 0.42;

    private static final double BEAD_ANGLE = 0.011;

    private static final double BEAD_MIN = 0.035;

    private static final double BEAD_MAX = 0.2;

    private static final double DASH_PERIOD = 0.72;

    private static final double DASH_LENGTH = 0.44;

    private static final double SOLID_PIECE = 0.6;

    private static final double FLOW_SPEED = 1.3;

    private static final double FADE_IN = 1.4;

    private static final double SHEEN_SPEED = 9.0;

    private static final double SHEEN_REACH = 1.6;

    private static final double SHEEN_REST = 14.0;

    private static final float HALO_ALPHA = 0.22F;

    private static final float HALO_WIDTH = 2.8F;

    private static final double HALO_NEAR = 24.0;

    private static final double HALO_FAR = 40.0;

    private static final int AIM_PIECES = 200;

    private static final int TRACK_PIECES = 110;

    private static final float GHOST_ALPHA = 0.38F;

    private static final float STRAY_ALPHA = 0.7F;

    private static final float FLYING_ALPHA = 0.85F;

    private static final float UNFURL_SECONDS = 0.32F;

    private static final double MARKER_UNIT = 0.5;

    private static final double MARKER_LIFT = 0.025;

    private static final int RIM_STEPS = 24;

    private static final int ORBIT_ARCS = 10;

    private static final int SUN_RAYS = 12;

    private static final long TARGET_MEMORY_MS = 6000L;

    private static final long LABEL_TTL_NANOS = 250000000L;

    private static final long EPOCH = System.nanoTime();

    private static final String PEARL_PREDICT_ID = "predict";

    private static final String PEARL_OTHERS_ID = "others-pearls";

    private static final double[] RIM_COS = new double[25];

    private static final double[] RIM_SIN = new double[25];

    private static final double DIAGONAL = 1.0 / Math.sqrt(3.0);

    private static final double[][] BURST_RAYS =
            new double[][] {
                {1.0, 0.0, 0.0},
                {-1.0, 0.0, 0.0},
                {0.0, 1.0, 0.0},
                {0.0, -1.0, 0.0},
                {0.0, 0.0, 1.0},
                {0.0, 0.0, -1.0},
                {DIAGONAL, DIAGONAL, DIAGONAL},
                {-DIAGONAL, DIAGONAL, DIAGONAL},
                {DIAGONAL, -DIAGONAL, DIAGONAL},
                {DIAGONAL, DIAGONAL, -DIAGONAL},
                {-DIAGONAL, -DIAGONAL, DIAGONAL},
                {-DIAGONAL, DIAGONAL, -DIAGONAL},
                {DIAGONAL, -DIAGONAL, -DIAGONAL},
                {-DIAGONAL, -DIAGONAL, -DIAGONAL}
            };

    private final BooleanSetting aimPreview =
            this.addSetting("aim-preview", new BooleanSetting("Aim Preview", true));

    private final BooleanSetting ghostArc =
            this.addSetting("ghost-arc", new BooleanSetting("Ghost Arc", true));

    private final BooleanSetting inFlight =
            this.addSetting("in-flight", new BooleanSetting("In Flight", true));

    private final BooleanSetting othersShots =
            this.addSetting("others-shots", new BooleanSetting("Others' Shots", true));

    private final NumberSetting range =
            this.addSetting("range", new NumberSetting("Range", 64.0F, 16.0F, 160.0F).step(8.0F));

    private final BooleanSetting bows =
            this.addSetting("bows", new BooleanSetting("Bows & Crossbows", true));

    private final BooleanSetting tridents =
            this.addSetting("tridents", new BooleanSetting("Tridents", true));

    private final BooleanSetting throwables =
            this.addSetting("throwables", new BooleanSetting("Throwables", true));

    private final BooleanSetting pearls =
            this.addSetting("pearls", new BooleanSetting("Ender Pearls", true));

    private final ModeSetting pathStyle =
            this.addSetting(
                    "path-style", new ModeSetting("Path", "Beads", "Beads", "Dashes", "Solid"));

    private final BooleanSetting flow = this.addSetting("flow", new BooleanSetting("Flow", true));

    private final ModeSetting colorMode =
            this.addSetting("color", new ModeSetting("Color", "Theme", "Theme", "Manual"));

    private final ColorSetting pathColor =
            this.addSetting("path-color", new ColorSetting("Path Color", -13338378));

    private final NumberSetting thickness =
            this.addSetting(
                    "thickness", new NumberSetting("Thickness", 2.25F, 0.5F, 5.0F).step(0.25F));

    private final BooleanSetting halo = this.addSetting("halo", new BooleanSetting("Halo", true));

    private final BooleanSetting throughWalls =
            this.addSetting("through-walls", new BooleanSetting("Through Walls", true));

    private final ModeSetting marker =
            this.addSetting("marker", new ModeSetting("Marker", "Bolt", "Bolt", "Ring", "None"));

    private final NumberSetting markerScale =
            this.addSetting(
                    "marker-scale", new NumberSetting("Marker Scale", 1.0F, 0.5F, 2.0F).step(0.1F));

    private final BooleanSetting targetBox =
            this.addSetting("target-box", new BooleanSetting("Target Box", true));

    private final ColorSetting hitColor =
            this.addSetting("hit-color", new ColorSetting("Hit Color", -6635270));

    private final BooleanSetting flightTime =
            this.addSetting("flight-time", new BooleanSetting("Flight Time", true));

    private final TrajectoriesModule.Aim[] volley = new TrajectoriesModule.Aim[3];

    private int volleySize;

    private boolean volleyGhost;

    private Item aimItem;

    private long aimSince = -1L;

    private long markerSince = -1L;

    private final Int2ObjectOpenHashMap<TrajectoriesModule.Track> tracks =
            new Int2ObjectOpenHashMap<>();

    private final ArrayDeque<TrajectoriesModule.Track> spareTracks = new ArrayDeque<>();

    private int sweep;

    private ClientWorld level;

    private final GlyphCanvas canvas = new GlyphCanvas();

    private final List<TrajectoriesModule.Label> labels = new ArrayList<>();

    private long labelsAt;

    private int headTint;

    private int tailTint;

    private int haloTint;

    private int markTint;

    private int ringTint;

    private int sheenTint;

    private int plateTint;

    private int hitTint;

    private boolean onTop;

    private Vec3d camera = Vec3d.ZERO;

    private double strokeTotal;

    private double strokeSheen;

    private float strokeAlpha;

    private float strokeWidth;

    private boolean strokeFade;

    private boolean strokeHalo;

    private double gx;

    private double gy;

    private double gz;

    private double rx;

    private double ry;

    private double rz;

    private double ux;

    private double uy;

    private double uz;

    public TrajectoriesModule() {
        super(
                "trajectories",
                "Trajectories",
                "Shows where your projectiles will land.",
                ModuleCategory.RENDER);
        for (int i = 0; i < 3; i++) {
            this.volley[i] = new TrajectoriesModule.Aim();
        }
        this.ghostArc.visibleWhen(this.aimPreview::get);
        this.othersShots.visibleWhen(this.inFlight::get);
        this.range.visibleWhen(this.inFlight::get);
        this.flow.visibleWhen(() -> !this.pathStyle.is("Solid"));
        this.pathColor.visibleWhen(() -> this.colorMode.is("Manual"));
        this.markerScale.visibleWhen(() -> !this.marker.is("None"));
        this.hitColor.visibleWhen(() -> this.targetBox.get() && this.colorMode.is("Manual"));
    }

    @Override
    protected void onEnable() {
        this.reset();
        this.on(TickEvent.TYPE, Stage.POST, this::onTick);
        this.on(Render3DEvent.TYPE, this::onRender);
    }

    @Override
    protected void onDisable() {
        this.reset();
    }

    private void reset() {
        this.forgetAim();
        this.dropTracks();
        this.labels.clear();
        this.level = null;
    }

    private void syncLevel(ClientWorld level2) {
        if (level2 != this.level) {
            this.reset();
            this.level = level2;
        }
    }

    private void forgetAim() {
        for (TrajectoriesModule.Aim aim : this.volley) {
            aim.forget();
        }
        this.volleySize = 0;
        this.volleyGhost = false;
        this.aimItem = null;
        this.aimSince = -1L;
        this.markerSince = -1L;
    }

    private void dropTracks() {
        if (!this.tracks.isEmpty()) {
            for (TrajectoriesModule.Track track : this.tracks.values()) {
                this.recycle(track);
            }
            this.tracks.clear();
        }
    }

    private void recycle(TrajectoriesModule.Track track) {
        track.shot = null;
        track.departed = false;
        track.forget();
        if (this.spareTracks.size() < 24) {
            this.spareTracks.push(track);
        }
    }

    private void onTick(TickEvent event) {
        this.sweep++;
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        ClientPlayerEntity player = client.player;
        this.syncLevel(world);
        if (world != null && player != null && this.inFlight.get()) {
            double d = this.range.get() * this.range.get();
            PearlTrajectoryModule module = activePearlModule();
            boolean bl = module != null && pearlToggle(module, "others-pearls");
            boolean othersShots2 = this.othersShots.get();
            boolean flightTime2 = this.flightTime.get();
            Vec3d eyePos = player.getEyePos();
            int i = 24;
            for (Entity entity : world.getEntities()) {
                ProjectileEntity projectileEntity;
                if (entity instanceof ProjectileEntity
                        && !(projectileEntity = (ProjectileEntity) entity).isRemoved()) {
                    Entity owner = projectileEntity.getOwner();
                    boolean bl2 = owner == player;
                    Vec3d velocity;
                    Ballistics.Body body2;
                    if ((bl2 || othersShots2)
                            && (body2 =
                                            this.bodyOf(
                                                    projectileEntity,
                                                    module != null && (owner == null || bl2 || bl)))
                                    != null
                            && !(projectileEntity.squaredDistanceTo(player) > d)
                            && !((velocity = projectileEntity.getVelocity()).lengthSquared()
                                    < 1.0E-4)
                            && !resting(projectileEntity)) {
                        if (i-- == 0) {
                            break;
                        }
                        TrajectoriesModule.Track track = this.tracks.get(projectileEntity.getId());
                        if (track == null) {
                            track =
                                    this.spareTracks.isEmpty()
                                            ? new TrajectoriesModule.Track()
                                            : this.spareTracks.pop();
                            this.tracks.put(projectileEntity.getId(), track);
                        }
                        track.shot = projectileEntity;
                        track.seen = this.sweep;
                        track.flight
                                .launch(body2, projectileEntity.getEntityPos(), velocity)
                                .bounds(projectileEntity.getWidth(), projectileEntity.getHeight())
                                .gravity(projectileEntity.hasNoGravity() ? 0.0 : body2.gravity())
                                .wet(projectileEntity.isTouchingWater())
                                .age(projectileEntity.age)
                                .lifetime(200)
                                .ignoring(owner, projectileEntity)
                                .leftShooter(track.departed)
                                .solve(world);
                        track.departed = track.flight.hasLeftShooter();
                        track.caption(
                                eyePos,
                                flightTime2
                                        && (bl2
                                                || track.flight.outcome()
                                                        == Ballistics.Outcome.ENTITY));
                    }
                }
            }
            if (!this.tracks.isEmpty()) {
                this.tracks.values().removeIf(this::stale);
            }
        } else {
            this.dropTracks();
        }
    }

    private boolean stale(TrajectoriesModule.Track track) {
        if (track.seen == this.sweep) {
            return false;
        } else {
            this.recycle(track);
            return true;
        }
    }

    private static boolean resting(ProjectileEntity projectileEntity) {
        double d = projectileEntity.getX() - projectileEntity.lastX;
        double e = projectileEntity.getY() - projectileEntity.lastY;
        double d2 = projectileEntity.getZ() - projectileEntity.lastZ;
        return projectileEntity.age > 1 && d * d + e * e + d2 * d2 < 1.0E-4;
    }

    private Ballistics.Body bodyOf(ProjectileEntity projectileEntity, boolean bl) {
        if (projectileEntity instanceof TridentEntity tridentEntity) {
            return this.tridents.get() && !tridentEntity.isNoClip()
                    ? Ballistics.Body.TRIDENT
                    : null;
        } else if (projectileEntity
                instanceof PersistentProjectileEntity persistentProjectileEntity) {
            return this.bows.get() && !persistentProjectileEntity.isNoClip()
                    ? Ballistics.Body.ARROW
                    : null;
        } else if (!(projectileEntity instanceof EnderPearlEntity)) {
            if (!this.throwables.get()) {
                return null;
            } else if (projectileEntity instanceof PotionEntity) {
                return Ballistics.Body.FLASK;
            } else if (projectileEntity instanceof ExperienceBottleEntity) {
                return Ballistics.Body.BOTTLE;
            } else {
                return !(projectileEntity instanceof SnowballEntity)
                                && !(projectileEntity instanceof EggEntity)
                        ? null
                        : Ballistics.Body.ORB;
            }
        } else {
            return this.pearls.get() && !bl ? Ballistics.Body.ORB : null;
        }
    }

    private static PearlTrajectoryModule activePearlModule() {
        PearlTrajectoryModule module = ModuleRegistry.get(PearlTrajectoryModule.class);
        return module != null && module.isEnabled() ? module : null;
    }

    private static boolean pearlToggle(PearlTrajectoryModule module, String string) {
        for (Setting setting : module.settings()) {
            if (string.equals(setting.id()) && setting instanceof BooleanSetting setting2) {
                return setting2.get();
            }
        }
        return true;
    }

    private void onRender(Render3DEvent event) {
        this.labels.clear();
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        ClientPlayerEntity player = client.player;
        this.syncLevel(world);
        if (world != null && player != null) {
            long l;
            this.labelsAt = l = System.nanoTime();
            float f = (float) (l - EPOCH) / 1.0E9F;
            float partialTick = event.partialTick;
            this.choosePalette();
            this.onTop = this.throughWalls.get();
            this.canvas.begin(client.gameRenderer.getCamera());
            this.camera = client.gameRenderer.getCamera().getCameraPos();
            LivingEntity entity = this.targetBox.get() ? CombatTarget.current(6000L) : null;
            if (this.aimPreview.get()) {
                this.drawAim(world, player, partialTick, l, f, entity);
            } else if (this.aimItem != null || this.volleySize > 0) {
                this.forgetAim();
            }
            if (this.inFlight.get()) {
                this.drawTracks(player, partialTick, f, entity);
            }
            this.canvas.submit(this.onTop);
        }
    }

    private void drawTracks(ClientPlayerEntity player, float f, float g, LivingEntity entity) {
        for (TrajectoriesModule.Track track : this.tracks.values()) {
            ProjectileEntity shot2 = track.shot;
            if (shot2 != null && !shot2.isRemoved()) {
                boolean bl = shot2.getOwner() == player;
                float h = bl ? 1.0F : 0.7F;
                this.drawPath(
                        track.flight,
                        shot2.getLerpedPos(f),
                        1.7976931348623157E308,
                        h * 0.85F,
                        false,
                        false,
                        bl,
                        g);
                this.drawImpact(track.flight, 1.0F, h, g, f, entity);
                this.addLabel(track);
            }
        }
    }

    private void choosePalette() {
        boolean is = this.colorMode.is("Manual");
        int i = is ? this.pathColor.argb() : CosmeticAccent.argb();
        this.headTint = ColorHelper.lerp(0.35F, i, -1);
        this.tailTint = i;
        this.haloTint = i;
        this.markTint = i;
        this.ringTint = this.headTint;
        this.sheenTint = ColorHelper.lerp(0.8F, i, -1);
        this.plateTint = ColorHelper.lerp(0.55F, i, -1);
        this.hitTint = is ? this.hitColor.argb() : ColorHelper.lerp(0.5F, i, -1);
    }

    private void drawAim(
            ClientWorld world,
            ClientPlayerEntity player,
            float f,
            long markerSince2,
            float g,
            LivingEntity entity) {
        ItemStack stack = heldLauncher(player);
        this.volleySize = 0;
        this.volleyGhost = false;
        if (!stack.isEmpty()) {
            this.loadVolley(world, player, stack, f);
        }
        if (this.volleySize == 0) {
            if (this.aimItem != null) {
                this.forgetAim();
            }
        } else {
            Item item = stack.getItem();
            if (item != this.aimItem) {
                this.aimItem = item;
                this.aimSince = markerSince2;
                this.markerSince = -1L;
            }
            float easeOutExpo =
                    easeOutExpo((float) (markerSince2 - this.aimSince) / 1.0E9F / 0.32F);
            Ballistics.Flight flight2 = this.volley[0].flight;
            if (easeOutExpo < 1.0F || flight2.outcome() == Ballistics.Outcome.OPEN) {
                this.markerSince = -1L;
            } else if (this.markerSince < 0L) {
                this.markerSince = markerSince2;
            }
            float h =
                    this.markerSince < 0L
                            ? 0.0F
                            : settle((float) (markerSince2 - this.markerSince) / 1.0E9F);
            float f2 = this.volleyGhost ? 0.38F : 1.0F;
            for (int i = 0; i < this.volleySize; i++) {
                Ballistics.Flight flight3 = this.volley[i].flight;
                this.drawPath(
                        flight3,
                        null,
                        flight3.length() * easeOutExpo,
                        f2,
                        true,
                        !this.volleyGhost,
                        true,
                        g);
                if (h > 0.0F) {
                    this.drawImpact(flight3, h, f2, g, f, entity);
                }
            }
            if (!this.volleyGhost && easeOutExpo >= 1.0F) {
                this.addLabel(this.volley[0]);
            }
        }
    }

    private static ItemStack heldLauncher(ClientPlayerEntity player) {
        if (player.isUsingItem()) {
            return player.getActiveItem();
        } else {
            ItemStack mainHandStack = player.getMainHandStack();
            return launches(mainHandStack.getItem()) ? mainHandStack : player.getOffHandStack();
        }
    }

    private static boolean launches(Item item) {
        return item instanceof RangedWeaponItem
                || item instanceof TridentItem
                || item instanceof SnowballItem
                || item instanceof EggItem
                || item instanceof EnderPearlItem
                || item instanceof ThrowablePotionItem
                || item instanceof ExperienceBottleItem;
    }

    private void loadVolley(
            ClientWorld world, ClientPlayerEntity player, ItemStack stack, float f) {
        Item item = stack.getItem();
        boolean bl = player.isUsingItem() && player.getActiveItem() == stack;
        if (item instanceof BowItem) {
            if (!this.bows.get()
                    || player.getProjectileType(stack).isEmpty()
                    || !bl && !this.ghostArc.get()) {
                return;
            }
            float g = bl ? bowPull(player.getItemUseTime() + f) : 1.0F;
            this.volleyGhost = !bl || g < 0.1F;
            this.aimed(world, player, f, Ballistics.Body.ARROW, Math.max(g, 0.1F) * 3.0F, 0.0F);
        } else if (item instanceof CrossbowItem) {
            if (this.bows.get()) {
                this.crossbowVolley(
                        world,
                        player,
                        f,
                        stack.getOrDefault(
                                DataComponentTypes.CHARGED_PROJECTILES,
                                ChargedProjectilesComponent.DEFAULT));
            }
        } else if (item instanceof TridentItem) {
            if (!this.tridents.get() || !bl && !this.ghostArc.get()) {
                return;
            }
            if (EnchantmentHelper.hasAnyEnchantmentsWith(
                    stack, EnchantmentEffectComponentTypes.TRIDENT_SPIN_ATTACK_STRENGTH)) {
                return;
            }
            this.volleyGhost = !bl || player.getItemUseTime() < 10;
            this.aimed(world, player, f, Ballistics.Body.TRIDENT, 2.5F, 0.0F);
        } else if (item instanceof EnderPearlItem) {
            PearlTrajectoryModule module = activePearlModule();
            if (this.pearls.get() && (module == null || !pearlToggle(module, "predict"))) {
                this.aimed(world, player, f, Ballistics.Body.ORB, EnderPearlItem.POWER, 0.0F);
            }
        } else if (this.throwables.get()) {
            this.lob(world, player, f, item);
        }
    }

    private void lob(ClientWorld world, ClientPlayerEntity player, float f, Item item) {
        if (item instanceof SnowballItem) {
            this.aimed(world, player, f, Ballistics.Body.ORB, SnowballItem.POWER, 0.0F);
        } else if (item instanceof EggItem) {
            this.aimed(world, player, f, Ballistics.Body.ORB, 1.5F, 0.0F);
        } else if (item instanceof ThrowablePotionItem) {
            this.aimed(world, player, f, Ballistics.Body.FLASK, ThrowablePotionItem.POWER, -20.0F);
        } else if (item instanceof ExperienceBottleItem) {
            this.aimed(world, player, f, Ballistics.Body.BOTTLE, 0.7F, -20.0F);
        }
    }

    private static float bowPull(float f) {
        float g = f / 20.0F;
        return Math.min(1.0F, (g * g + g * 2.0F) / 3.0F);
    }

    private void aimed(
            ClientWorld world,
            ClientPlayerEntity player,
            float f,
            Ballistics.Body body2,
            float g,
            float h) {
        float pitch = player.getPitch(f);
        double d = Math.toRadians(pitch);
        double e = Math.toRadians(player.getYaw(f));
        double d2 = Math.toRadians(pitch + h);
        Vec3d vec =
                new Vec3d(-Math.sin(e) * Math.cos(d), -Math.sin(d2), Math.cos(e) * Math.cos(d))
                        .normalize()
                        .multiply(g);
        Vec3d movement = player.getMovement();
        Vec3d vec2 = vec.add(movement.x, player.isOnGround() ? 0.0 : movement.y, movement.z);
        this.release(world, player, body2, muzzle(player, f, 0.1), vec2, 240);
    }

    private void crossbowVolley(
            ClientWorld world,
            ClientPlayerEntity player,
            float f,
            ChargedProjectilesComponent chargedProjectilesComponent) {
        if (!chargedProjectilesComponent.isEmpty()) {
            List projectiles = chargedProjectilesComponent.getProjectiles();
            float g = chargedProjectilesComponent.contains(Items.FIREWORK_ROCKET) ? 1.6F : 3.15F;
            int i = projectiles.size();
            float h = i > 1 ? 20.0F / (i - 1) : 0.0F;
            float f2 = (i - 1) % 2 * h / 2.0F;
            Vec3d rotationVec = player.getRotationVec(f);
            Vec3d oppositeRotationVector = player.getOppositeRotationVector(f);
            for (int j = 0; j < i; j++) {
                ItemStack projectile = (ItemStack) projectiles.get(j);
                if (!projectile.isEmpty()) {
                    float g2 = f2 + ((j & 1) == 1 ? -1.0F : 1.0F) * ((j + 1) / 2) * h;
                    Vec3d vec =
                            swing(rotationVec, oppositeRotationVector, g2).normalize().multiply(g);
                    if (projectile.isOf(Items.FIREWORK_ROCKET)) {
                        this.release(
                                world,
                                player,
                                Ballistics.Body.ROCKET,
                                muzzle(player, f, 0.15),
                                vec,
                                rocketFuse(projectile));
                    } else {
                        this.release(
                                world,
                                player,
                                Ballistics.Body.ARROW,
                                muzzle(player, f, 0.1),
                                vec,
                                240);
                    }
                }
            }
        }
    }

    private static int rocketFuse(ItemStack stack) {
        FireworksComponent fireworksComponent = stack.get(DataComponentTypes.FIREWORKS);
        int i = 1 + (fireworksComponent != null ? fireworksComponent.flightDuration() : 0);
        return 10 * i + 6;
    }

    private static Vec3d swing(Vec3d vec, Vec3d vec2, double d) {
        if (d == 0.0) {
            return vec;
        } else {
            double e = Math.toRadians(d);
            double d2 = Math.cos(e);
            double e2 = Math.sin(e);
            Vec3d vec3 = vec2.normalize();
            return vec.multiply(d2)
                    .add(vec3.crossProduct(vec).multiply(e2))
                    .add(vec3.multiply(vec3.dotProduct(vec) * (1.0 - d2)));
        }
    }

    private static Vec3d muzzle(ClientPlayerEntity player, float f, double d) {
        Vec3d lerpedPos = player.getLerpedPos(f);
        return new Vec3d(lerpedPos.x, lerpedPos.y + player.getStandingEyeHeight() - d, lerpedPos.z);
    }

    private void release(
            ClientWorld world,
            ClientPlayerEntity player,
            Ballistics.Body body2,
            Vec3d vec,
            Vec3d vec2,
            int i) {
        if (this.volleySize != 3) {
            TrajectoriesModule.Aim aim;
            if (!(aim = this.volley[this.volleySize++]).matches(body2, vec, vec2, i, this.sweep)) {
                aim.hold(body2, vec, vec2, i, this.sweep);
                aim.flight.launch(body2, vec, vec2).lifetime(i).ignoring(player, null).solve(world);
                aim.caption(vec, this.flightTime.get());
            }
        }
    }

    private void drawPath(
            Ballistics.Flight flight2,
            Vec3d vec,
            double d,
            float strokeAlpha2,
            boolean strokeFade2,
            boolean bl,
            boolean bl2,
            float f) {
        int i = flight2.size();
        if (i >= 2 && !(strokeAlpha2 < 0.02F)) {
            double e = flight2.length();
            if (vec != null) {
                double d2 = vec.x - flight2.x(0);
                double e2 = vec.y - flight2.y(0);
                double d3 = vec.z - flight2.z(0);
                e += Math.sqrt(d2 * d2 + e2 * e2 + d3 * d3);
            }
            if (!(e < 1.0E-4)) {
                double e3 = Math.min(d, e);
                boolean is = this.pathStyle.is("Solid");
                boolean is2 = this.pathStyle.is("Dashes");
                double d4 = is ? 0.6 : (is2 ? 0.72 : 0.42);
                double e4 = Math.max(1.0, e / (d4 * (strokeFade2 ? 200 : 110)));
                double d5 = (is2 ? 0.72 : 0.42) * e4;
                double e5 = 0.44 * e4;
                double d6 = 0.6 * e4;
                double e6 = !is && this.flow.get() ? f * 1.3 % d5 : 0.0;
                this.strokeTotal = e;
                this.strokeAlpha = strokeAlpha2;
                this.strokeFade = strokeFade2;
                this.strokeSheen = bl ? f * 9.0 % (e + 14.0) - 1.6 : 0.0 / 0.0;
                this.strokeWidth = this.thickness.get();
                boolean bl3 = this.strokeHalo = bl2 && this.halo.get();
                double d7;
                double e7;
                double d8;
                byte b;
                if (vec != null) {
                    d8 = vec.x;
                    e7 = vec.y;
                    d7 = vec.z;
                    b = 0;
                } else {
                    d8 = flight2.x(0);
                    e7 = flight2.y(0);
                    d7 = flight2.z(0);
                    b = 1;
                }
                double e8 = 0.0;
                for (int j = b; j < i && e8 < e3; j++) {
                    double d9 = flight2.x(j);
                    double e9 = d9 - d8;
                    double d10;
                    double e10;
                    double d11;
                    double e11;
                    double d12 =
                            Math.sqrt(
                                    e9 * e9
                                            + (e11 = (d11 = flight2.y(j)) - e7) * e11
                                            + (e10 = (d10 = flight2.z(j)) - d7) * e10);
                    if (d12 > 1.0E-6) {
                        double e12 = Math.min(e8 + d12, e3);
                        if (is) {
                            int k = Math.max(1, (int) Math.ceil((e12 - e8) / d6));
                            double d13 = (e12 - e8) / k;
                            for (int m = 0; m < k; m++) {
                                double e13 = e8 + m * d13;
                                this.stroke(
                                        d8,
                                        e7,
                                        d7,
                                        e9,
                                        e11,
                                        e10,
                                        (e13 - e8) / d12,
                                        (e13 + d13 - e8) / d12,
                                        e13 + d13 * 0.5);
                            }
                        } else {
                            for (double d14 = Math.floor((e8 - e6) / d5) * d5 + e6;
                                    d14 < e12;
                                    d14 += d5) {
                                double e14 = Math.max(d14, e8);
                                double d15 =
                                        is2
                                                ? e5
                                                : this.beadLength(
                                                        d8, e7, d7, e9, e11, e10, (e14 - e8) / d12);
                                double e15 = Math.min(d14 + d15, e12);
                                if (e15 > e14) {
                                    this.stroke(
                                            d8,
                                            e7,
                                            d7,
                                            e9,
                                            e11,
                                            e10,
                                            (e14 - e8) / d12,
                                            (e15 - e8) / d12,
                                            (e14 + e15) * 0.5);
                                }
                            }
                        }
                    }
                    d8 = d9;
                    e7 = d11;
                    d7 = d10;
                    e8 += d12;
                }
            }
        }
    }

    private double beadLength(
            double d, double e, double d2, double e2, double d3, double e3, double d4) {
        double e4 = d + e2 * d4 - this.camera.x;
        double d5 = e + d3 * d4 - this.camera.y;
        double e5 = d2 + e3 * d4 - this.camera.z;
        return MathHelper.clamp(Math.sqrt(e4 * e4 + d5 * d5 + e5 * e5) * 0.011, 0.035, 0.2);
    }

    private void stroke(
            double d,
            double e,
            double d2,
            double e2,
            double d3,
            double e3,
            double d4,
            double e4,
            double d5) {
        float strokeAlpha2 = this.strokeAlpha;
        if (this.strokeFade) {
            strokeAlpha2 *= smooth((float) (d5 / 1.4));
        }
        if (!(strokeAlpha2 < 0.02F)) {
            float f = (float) (d5 / this.strokeTotal);
            int i = ColorHelper.lerp(f, this.headTint, this.tailTint);
            double e5;
            if (!Double.isNaN(this.strokeSheen)
                    && (e5 = 1.0 - Math.abs(d5 - this.strokeSheen) / 1.6) > 0.0) {
                i = ColorHelper.lerp((float) (e5 * 0.7), i, this.sheenTint);
            }
            double d6 = d + e2 * d4;
            double e6 = e + d3 * d4;
            double d7 = d2 + e3 * d4;
            double e7 = d + e2 * e4;
            double d8 = e + d3 * e4;
            double e8 = d2 + e3 * e4;
            float g;
            if (this.strokeHalo && (g = strokeAlpha2 * 0.22F * this.haloFade(d6, e6, d7)) > 0.0F) {
                this.canvas.line(
                        d6,
                        e6,
                        d7,
                        e7,
                        d8,
                        e8,
                        ColorHelper.scaleAlpha(this.haloTint, g),
                        this.strokeWidth * 2.8F);
            }
            this.canvas.line(
                    d6,
                    e6,
                    d7,
                    e7,
                    d8,
                    e8,
                    ColorHelper.scaleAlpha(i, strokeAlpha2),
                    this.strokeWidth);
        }
    }

    private float haloFade(double d, double e, double d2) {
        double e2 = d - this.camera.x;
        double d3 = e - this.camera.y;
        double e3 = d2 - this.camera.z;
        double d4 = e2 * e2 + d3 * d3 + e3 * e3;
        if (d4 <= 576.0) {
            return 1.0F;
        } else {
            return d4 >= 1600.0 ? 0.0F : 1.0F - smooth((float) ((Math.sqrt(d4) - 24.0) / 16.0));
        }
    }

    private void drawImpact(
            Ballistics.Flight flight2, float f, float g, float h, float f2, LivingEntity entity) {
        switch (flight2.outcome()) {
            case ENTITY:
                if (this.targetBox.get()) {
                    this.drawTargetBox(flight2.struck(), g, h, f2, entity);
                }
                break;
            case BLOCK:
                this.drawMarker(flight2, f, g, h);
                break;
            case BURST:
                this.drawBurst(flight2.impact(), f, g);
        }
    }

    private void drawMarker(Ballistics.Flight flight2, float f, float g, float h) {
        if (!this.marker.is("None") && !(f <= 0.01F)) {
            Direction direction = flight2.face();
            Vec3d vec =
                    new Vec3d(
                            direction.getOffsetX(), direction.getOffsetY(), direction.getOffsetZ());
            Vec3d vec2 = new Vec3d(0.0, 1.0, 0.0);
            if (direction.getAxis() == Axis.Y) {
                Vec3d vec3 = flight2.heading();
                vec2 =
                        vec3.x * vec3.x + vec3.z * vec3.z < 1.0E-8
                                ? new Vec3d(0.0, 0.0, 1.0)
                                : new Vec3d(vec3.x, 0.0, vec3.z).normalize();
            }
            Vec3d vec4 = vec2.crossProduct(vec).normalize();
            Vec3d vec5 = flight2.impact().add(vec.multiply(0.025));
            double d = 0.5 * this.markerScale.get() * f;
            this.gx = vec5.x;
            this.gy = vec5.y;
            this.gz = vec5.z;
            this.rx = vec4.x * d;
            this.ry = vec4.y * d;
            this.rz = vec4.z * d;
            this.ux = vec2.x * d;
            this.uy = vec2.y * d;
            this.uz = vec2.z * d;
            if (this.marker.is("Ring")) {
                this.drawSunDisk(g, h);
            } else {
                this.drawBolt(g, h);
            }
        }
    }

    private void drawBolt(float f, float g) {
        int scaleAlpha = ColorHelper.scaleAlpha(this.markTint, f);
        this.disc(0.78, ColorHelper.scaleAlpha(this.plateTint, f * 0.16F));
        this.orbit(
                0.9,
                g * 0.6,
                ColorHelper.scaleAlpha(this.ringTint, f * 0.85F),
                this.thickness.get() * 0.75F);
        this.facet(
                this.at(-0.06, 0.02),
                this.at(0.06, 0.02),
                this.at(0.15, -0.62),
                this.at(-0.15, -0.62),
                scaleAlpha);
        this.facet(
                this.at(-0.06, 0.09),
                this.at(-0.06, -0.03),
                this.at(-0.37, -0.08),
                this.at(-0.37, 0.14),
                scaleAlpha);
        this.facet(
                this.at(0.06, 0.09),
                this.at(0.06, -0.03),
                this.at(0.37, -0.08),
                this.at(0.37, 0.14),
                scaleAlpha);
        for (int j = 0; j < 24; j++) {
            double d = RIM_COS[j];
            double e = RIM_SIN[j];
            double d2 = RIM_COS[j + 1];
            double e2 = RIM_SIN[j + 1];
            this.facet(
                    this.at(0.19 * d, 0.34 + 0.27 * e),
                    this.at(0.19 * d2, 0.34 + 0.27 * e2),
                    this.at(0.1 * d2, 0.34 + 0.17 * e2),
                    this.at(0.1 * d, 0.34 + 0.17 * e),
                    scaleAlpha);
        }
    }

    private void drawSunDisk(float f, float g) {
        int scaleAlpha = ColorHelper.scaleAlpha(this.markTint, f);
        float thickness2 = this.thickness.get();
        this.disc(0.95, ColorHelper.scaleAlpha(this.plateTint, f * 0.12F));
        this.disc(0.3, scaleAlpha);
        int scaleAlpha2 = ColorHelper.scaleAlpha(this.ringTint, f);
        Vec3d vec = this.at(0.42, 0.0);
        for (int k = 1; k <= 24; k++) {
            Vec3d vec2 = this.at(0.42 * RIM_COS[k], 0.42 * RIM_SIN[k]);
            this.line(vec, vec2, scaleAlpha2, thickness2);
            vec = vec2;
        }
        double d = g * 0.35;
        for (int m = 0; m < 12; m++) {
            double e = d + 6.283185307179586 * m / 12.0;
            double d2 = Math.cos(e);
            double e2 = Math.sin(e);
            double d3 = (m & 1) == 0 ? 0.8 : 0.66;
            this.line(
                    this.at(0.52 * d2, 0.52 * e2),
                    this.at(d3 * d2, d3 * e2),
                    scaleAlpha,
                    thickness2);
        }
    }

    private void drawBurst(Vec3d vec, float f, float g) {
        int scaleAlpha = ColorHelper.scaleAlpha(this.markTint, g);
        double d = 0.55 * this.markerScale.get() * f;
        float thickness2 = this.thickness.get();
        for (double[] doubles : BURST_RAYS) {
            Vec3d vec2 = new Vec3d(doubles[0], doubles[1], doubles[2]);
            this.line(
                    vec.add(vec2.multiply(d * 0.25)),
                    vec.add(vec2.multiply(d)),
                    scaleAlpha,
                    thickness2);
        }
    }

    private void drawTargetBox(Entity entity, float f, float g, float h, LivingEntity entity2) {
        if (entity != null && !entity.isRemoved()) {
            Vec3d vec = entity.getLerpedPos(h).subtract(entity.getEntityPos());
            Box box = entity.getBoundingBox().offset(vec).expand(0.06);
            if (!box.contains(this.camera)) {
                boolean bl = entity == entity2;
                float f2 = bl ? 0.5F + 0.5F * MathHelper.sin(g * 6.5F) : 0.0F;
                int scaleAlpha =
                        ColorHelper.scaleAlpha(
                                bl ? ColorHelper.lerp(0.35F * f2, this.hitTint, -1) : this.hitTint,
                                f);
                VisibilityConfigurable visibilityConfigurable =
                        GizmoDrawing.box(
                                box,
                                DrawStyle.filled(
                                        ColorHelper.scaleAlpha(
                                                scaleAlpha, bl ? 0.14F + 0.12F * f2 : 0.1F)));
                if (this.onTop) {
                    visibilityConfigurable.ignoreOcclusion();
                }
                this.brackets(
                        box,
                        bl ? 0.34 + 0.06 * f2 : 0.26,
                        scaleAlpha,
                        this.thickness.get() * (bl ? 1.2F : 1.0F));
            }
        }
    }

    private void brackets(Box box, double d, int i, float f) {
        double e = (box.maxX - box.minX) * d;
        double d2 = (box.maxY - box.minY) * d;
        double e2 = (box.maxZ - box.minZ) * d;
        for (int j = 0; j < 8; j++) {
            boolean bl = (j & 1) != 0;
            boolean bl2 = (j & 2) != 0;
            boolean bl3 = (j & 4) != 0;
            double d3 = bl ? box.maxX : box.minX;
            double e3 = bl2 ? box.maxY : box.minY;
            double d4 = bl3 ? box.maxZ : box.minZ;
            Vec3d vec = new Vec3d(d3, e3, d4);
            this.line(vec, new Vec3d(bl ? d3 - e : d3 + e, e3, d4), i, f);
            this.line(vec, new Vec3d(d3, bl2 ? e3 - d2 : e3 + d2, d4), i, f);
            this.line(vec, new Vec3d(d3, e3, bl3 ? d4 - e2 : d4 + e2), i, f);
        }
    }

    private void disc(double d, int i) {
        Vec3d vec = this.at(0.0, 0.0);
        for (int j = 0; j < 24; j++) {
            this.facet(
                    vec,
                    vec,
                    this.at(d * RIM_COS[j], d * RIM_SIN[j]),
                    this.at(d * RIM_COS[j + 1], d * RIM_SIN[j + 1]),
                    i);
        }
    }

    private void orbit(double d, double e, int i, float f) {
        double d2 = 0.6283185307179586;
        double e2 = d2 * 0.55;
        for (int j = 0; j < 10; j++) {
            double d3 = e + j * d2;
            Vec3d vec = this.at(d * Math.cos(d3), d * Math.sin(d3));
            for (int k = 1; k <= 3; k++) {
                double e3 = d3 + e2 * k / 3.0;
                Vec3d vec2 = this.at(d * Math.cos(e3), d * Math.sin(e3));
                this.line(vec, vec2, i, f);
                vec = vec2;
            }
        }
    }

    private Vec3d at(double d, double e) {
        return new Vec3d(
                this.gx + this.rx * d + this.ux * e,
                this.gy + this.ry * d + this.uy * e,
                this.gz + this.rz * d + this.uz * e);
    }

    private void facet(Vec3d vec, Vec3d vec2, Vec3d vec3, Vec3d vec4, int i) {
        double d = vec3.y - vec.y;
        double e = vec4.z - vec2.z;
        double d2 = vec3.z - vec.z;
        double e2 = vec4.y - vec2.y;
        double d3 = d * e - d2 * e2;
        double e3 = vec4.x - vec2.x;
        double d4 = vec3.x - vec.x;
        double e4 = d2 * e3 - d4 * e;
        double d5 = d4 * e2 - d * e3;
        double e5 =
                d3 * (this.camera.x - vec.x)
                        + e4 * (this.camera.y - vec.y)
                        + d5 * (this.camera.z - vec.z);
        if (e5 >= 0.0) {
            this.canvas.quad(
                    vec.x, vec.y, vec.z, vec2.x, vec2.y, vec2.z, vec3.x, vec3.y, vec3.z, vec4.x,
                    vec4.y, vec4.z, i);
        } else {
            this.canvas.quad(
                    vec4.x, vec4.y, vec4.z, vec3.x, vec3.y, vec3.z, vec2.x, vec2.y, vec2.z, vec.x,
                    vec.y, vec.z, i);
        }
    }

    private void line(Vec3d vec, Vec3d vec2, int i, float f) {
        this.canvas.line(vec.x, vec.y, vec.z, vec2.x, vec2.y, vec2.z, i, f);
    }

    private void addLabel(TrajectoriesModule.Plot plot) {
        if (this.flightTime.get() && plot.seconds != null) {
            Ballistics.Flight flight2 = plot.flight;
            Vec3d vec = flight2.impact();
            Entity entity = flight2.struck();
            Vec3d vec2 =
                    entity != null
                            ? new Vec3d(vec.x, entity.getBoundingBox().maxY + 0.35, vec.z)
                            : vec.add(0.0, 0.55, 0.0);
            this.labels.add(new TrajectoriesModule.Label(vec2, plot.seconds, plot.detail));
        }
    }

    @Override
    public boolean isHudOverlayEnabled() {
        return this.isEnabled() && this.flightTime.get();
    }

    @Override
    public void renderHudOverlay(ImDrawList imDrawList, float f, float g) {
        if (!this.labels.isEmpty() && System.nanoTime() - this.labelsAt <= 250000000L) {
            MinecraftClient client = MinecraftClient.getInstance();
            for (TrajectoriesModule.Label label : this.labels) {
                float[] floats = WorldLabel.project(client, label.pos(), f, g);
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

    private static float smooth(float f) {
        f = MathHelper.clamp(f, 0.0F, 1.0F);
        return f * f * (3.0F - 2.0F * f);
    }

    private static float easeOutExpo(float f) {
        return f >= 1.0F ? 1.0F : 1.0F - (float) Math.pow(2.0, -10.0 * Math.max(0.0F, f));
    }

    private static float settle(float f) {
        return 1.0F - (float) (Math.exp(-5.5 * f) * Math.cos(8.5 * f));
    }

    static {
        for (int i = 0; i <= 24; i++) {
            double d = 6.283185307179586 * i / 24.0;
            RIM_COS[i] = Math.cos(d);
            RIM_SIN[i] = Math.sin(d);
        }
    }

    @Environment(EnvType.CLIENT)
    private static final class Aim extends TrajectoriesModule.Plot {

        Ballistics.Body body;

        double x;

        double y;

        double z;

        double vx;

        double vy;

        double vz;

        int life;

        int stamp;

        boolean held;

        private Aim() {}

        boolean matches(Ballistics.Body body2, Vec3d vec, Vec3d vec2, int i, int j) {
            return this.held
                    && this.stamp == j
                    && this.body == body2
                    && this.life == i
                    && Math.abs(this.x - vec.x) < 0.001
                    && Math.abs(this.y - vec.y) < 0.001
                    && Math.abs(this.z - vec.z) < 0.001
                    && Math.abs(this.vx - vec2.x) < 5.0E-4
                    && Math.abs(this.vy - vec2.y) < 5.0E-4
                    && Math.abs(this.vz - vec2.z) < 5.0E-4;
        }

        void hold(Ballistics.Body body2, Vec3d vec, Vec3d vec2, int life2, int stamp2) {
            this.body = body2;
            this.x = vec.x;
            this.y = vec.y;
            this.z = vec.z;
            this.vx = vec2.x;
            this.vy = vec2.y;
            this.vz = vec2.z;
            this.life = life2;
            this.stamp = stamp2;
            this.held = true;
        }

        @Override
        void forget() {
            super.forget();
            this.held = false;
        }
    }

    @Environment(EnvType.CLIENT)
    private record Label(Vec3d pos, String primary, String secondary) {}

    @Environment(EnvType.CLIENT)
    private static class Plot {

        final Ballistics.Flight flight = new Ballistics.Flight();

        String seconds;

        String detail;

        private Plot() {}

        void caption(Vec3d vec, boolean bl) {
            this.seconds = null;
            this.detail = null;
            if (bl && this.flight.outcome() != Ballistics.Outcome.OPEN && this.flight.size() >= 2) {
                Entity entity = this.flight.struck();
                this.seconds = String.format(Locale.ROOT, "%.2fs", this.flight.ticks() / 20.0F);
                this.detail =
                        entity != null
                                ? NameProtectModule.protect(entity.getName()).getString()
                                : String.format(
                                        Locale.ROOT, "%.0fm", vec.distanceTo(this.flight.impact()));
            }
        }

        void forget() {
            this.flight.clear();
            this.seconds = null;
            this.detail = null;
        }
    }

    @Environment(EnvType.CLIENT)
    private static final class Track extends TrajectoriesModule.Plot {

        ProjectileEntity shot;

        int seen;

        boolean departed;

        private Track() {}
    }
}
