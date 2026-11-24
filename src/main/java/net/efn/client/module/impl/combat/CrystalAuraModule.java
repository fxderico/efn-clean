package net.efn.client.module.impl.combat;

import imgui.ImDrawList;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;

import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.PacketReceiveEvent;
import net.efn.client.event.impl.PlayerAttackEntityEvent;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.CombatMacroLock;
import net.efn.client.module.HudOverlayProvider;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.combat.crystal.Combo;
import net.efn.client.module.impl.combat.crystal.CrystalDamage;
import net.efn.client.module.impl.combat.crystal.Crystals;
import net.efn.client.module.impl.combat.crystal.HurtClock;
import net.efn.client.module.impl.combat.crystal.LootGuard;
import net.efn.client.module.impl.combat.crystal.ShotView;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.impl.misc.AutoEatModule;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.KeySetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.pvp.Rotation;
import net.efn.client.util.pvp.RotationUtils;
import net.efn.client.util.pvp.ServerRotation;
import net.efn.client.util.pvp.TargetingUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.Type;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class CrystalAuraModule extends ClientModule implements HudOverlayProvider {

    private static final int COUNTER_CAP = 1000;

    private static final int IDLE_TICKS = 10;

    private static final float FACE_PLACE_HEALTH = 10.0F;

    private static final float FACE_PLACE_DAMAGE = 2.0F;

    private static final float ARMOR_LOW = 0.1F;

    private static final float BREAK_FACTOR = 0.6F;

    private static final double AIM_INSET = 0.1;

    private static final int MAX_BREAK_ATTEMPTS = 3;

    private static final double CHAIN_RANGE = 12.0;

    private static final int WEAPON_SETTLE_TICKS = 2;

    private static final float LOOK_BONUS = 2.0F;

    private static final int MAX_LEAD_TICKS = 5;

    private static final float LOOK_MATCH_DEGREES = 2.0F;

    private static final double PLACE_BREAK_MARGIN = 0.1;

    private static final float ONE_LOOK_BONUS = 2.0F;

    private static final float OBSIDIAN_UPGRADE_BAR = 8.0F;

    private static final float OBSIDIAN_MIN_GAIN = 5.0F;

    private static final float OBSIDIAN_MIN_RATIO = 2.0F;

    private static final float EAGER_UPGRADE_BAR = 11.0F;

    private static final float EAGER_MIN_GAIN = 3.0F;

    private static final float EAGER_MIN_RATIO = 1.4F;

    private static final double PLACE_REACH_MARGIN = 0.1;

    private static final float HELD_VIEW_BONUS = 20.0F;

    private final BooleanSetting requireHold =
            this.addSetting("require-hold", new BooleanSetting("Require Hold", false));

    private final KeySetting holdKey =
            this.addSetting("hold-key", new KeySetting("Hold Key", 1003));

    private final NumberSetting placeDelay =
            this.addSetting(
                    "place-delay", new NumberSetting("Place Delay", 0.0F, 0.0F, 10.0F).step(1.0F));

    private final NumberSetting breakDelay =
            this.addSetting(
                    "break-delay", new NumberSetting("Break Delay", 0.0F, 0.0F, 10.0F).step(1.0F));

    private final NumberSetting minDamage =
            this.addSetting(
                    "min-damage", new NumberSetting("Min Damage", 4.0F, 0.0F, 20.0F).step(0.5F));

    private final NumberSetting maxSelfDamage =
            this.addSetting(
                    "max-self-damage",
                    new NumberSetting("Max Self Damage", 8.0F, 0.0F, 20.0F).step(0.5F));

    private final BooleanSetting facePlace =
            this.addSetting("face-place", new BooleanSetting("Face Place", true));

    private final KeySetting forceFacePlace =
            this.addSetting("force-face-place", new KeySetting("Force Face Place", -1));

    private final BooleanSetting autoObsidian =
            this.addSetting("auto-obsidian", new BooleanSetting("Auto Obsidian", true));

    private final BooleanSetting switchBack =
            this.addSetting("switch-back", new BooleanSetting("Switch Back", true));

    private final NumberSetting turnSpeed =
            this.addSetting(
                    "turn-speed", new NumberSetting("Turn Speed", 4.5F, 1.0F, 10.0F).step(0.5F));

    private final NumberSetting viewAngle =
            this.addSetting(
                    "view-angle", new NumberSetting("View Angle", 90.0F, 30.0F, 180.0F).step(5.0F));

    private final BooleanSetting humanize =
            this.addSetting("humanize", new BooleanSetting("Human Pace", true));

    private final BooleanSetting protectLoot =
            this.addSetting("protect-loot", new BooleanSetting("Protect Loot", true));

    private final BooleanSetting render =
            this.addSetting("render", new BooleanSetting("Render", true));

    private final ColorSetting renderColor =
            this.addSetting("render-color", new ColorSetting("Render Color", -2728705));

    private final ShotView view = new ShotView();

    private boolean engaged;

    private int idleTicks;

    private int savedSlot = -1;

    private int ownSlot = -1;

    private int ticksSinceBreak = 1000;

    private int ticksSincePlace = 1000;

    private int breakGap;

    private int placeGap;

    private ItemStack lastMain;

    private int mainAge = 1000;

    private int pendingBreakId = -1;

    private int pendingBreakAge = 1000;

    private BlockPos pendingObsidian;

    private int pendingObsidianAge = 1000;

    private final Int2IntOpenHashMap breakAttempts = new Int2IntOpenHashMap();

    private final Int2IntOpenHashMap doomed = new Int2IntOpenHashMap();

    private float steerMin;

    private float steerSelf;

    private int steerUntil = -1;

    private float tickMin;

    private float tickSelf;

    private int eagerUntil = -1;

    private int floorY;

    private int floorUntil = -1;

    private int tickFloor = -2147483648;

    private int fastUntil = -1;

    private boolean fullSpeed;

    public CrystalAuraModule() {
        super(
                "crystal-aura",
                "Crystal Aura",
                "Places and breaks end crystals for the most damage.",
                ModuleCategory.COMBAT);
        this.holdKey.visibleWhen(this.requireHold::get);
        this.renderColor.visibleWhen(this.render::get);
    }

    @Override
    protected void onEnable() {
        this.resetState();
    }

    @Override
    protected void onDisable() {
        ClientPlayerEntity player2 = MinecraftClient.getInstance().player;
        if (player2 != null && !AutoEatModule.isEating() && !AutoInvTotemModule.wantsHotbar()) {
            this.restoreSlot(player2);
        }
        this.resetState();
        this.view.clear();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        this.ageCounters();
        ClientPlayerEntity player2 = client.player;
        if (player2 != null && client.world != null) {
            LootGuard.update(client.world, player2);
        }
        if (player2 != null
                && client.world != null
                && client.interactionManager != null
                && !player2.isDead()) {
            HurtClock.tick(player2);
            this.forgetGone(client.world);
            boolean bl = player2.age <= this.steerUntil;
            this.tickMin = bl ? this.steerMin : this.minDamage.get();
            this.tickSelf = bl ? this.steerSelf : this.maxSelfDamage.get();
            this.tickFloor = player2.age <= this.floorUntil ? this.floorY : -2147483648;
            this.fullSpeed = player2.age <= this.fastUntil;
            ItemStack mainHandStack = player2.getMainHandStack();
            if (mainHandStack != this.lastMain) {
                this.lastMain = mainHandStack;
                this.mainAge = 0;
            }
            CrystalAuraModule.Look look =
                    ServerRotation.canRotate(player2)
                            ? CrystalAuraModule.Look.SILENT
                            : CrystalAuraModule.Look.CROSSHAIR;
            boolean bl2 =
                    !this.requireHold.get() || Crystals.isKeyDown(client, this.holdKey.value());
            AnchorAuraModule module = ModuleRegistry.get(AnchorAuraModule.class);
            boolean bl3 =
                    module != null && module.isEnabled() && look == CrystalAuraModule.Look.SILENT;
            if (MeleeAttacks.canAct(client) && !player2.isSpectator() && (bl2 || bl3)) {
                if (!AutoInvTotemModule.wantsHotbar()
                        && !AutoCityModule.mining()
                        && !CombatMacroLock.heldByOther(this)) {
                    if (this.ownSlot != -1 && !this.ownsSelection(player2)) {
                        this.forgetSlots();
                    }
                    ItemStack offHandStack;
                    boolean bl4 =
                            !Crystals.isCombatItem(offHandStack = player2.getOffHandStack())
                                    && !offHandStack.isOf(Items.END_CRYSTAL)
                                    && !offHandStack.isOf(Items.OBSIDIAN);
                    if (!MeleeAttacks.rightClickComing(client)
                            || !bl4
                                    && !mainHandStack.isOf(Items.RESPAWN_ANCHOR)
                                    && !mainHandStack.isOf(Items.GLOWSTONE)) {
                        if (!MeleeAttacks.actedThisTick(player2)
                                && MeleeAttacks.handsFree(client, player2)
                                && (this.ownsSelection(player2) || mayTakeHand(player2, bl3))) {
                            List list = Combo.enemies(client, player2);
                            if (list.isEmpty()) {
                                this.disengage(client, player2);
                            } else {
                                Rotation rotation = null;
                                if (look == CrystalAuraModule.Look.CROSSHAIR) {
                                    rotation = RotationUtils.entityRotation(player2);
                                    if (ServerRotation.pending()
                                            || !sameLook(ServerRotation.sent(), rotation)) {
                                        this.idle(client, player2);
                                        return;
                                    }
                                }
                                int confirmWindow = Crystals.confirmWindow(client, player2);
                                EndCrystalEntity endCrystalEntity =
                                        this.lastHit(client.world, confirmWindow);
                                CrystalAuraModule.Fight fight =
                                        new CrystalAuraModule.Fight(
                                                player2,
                                                client.world,
                                                list,
                                                Combo.friends(client, player2),
                                                confirmWindow,
                                                Crystals.rttTicks(client, player2),
                                                rotation,
                                                look == CrystalAuraModule.Look.SILENT ? 2.0F : 0.0F,
                                                Crystals.isKeyDown(
                                                        client, this.forceFacePlace.value()));
                                Combo.Shot shot =
                                        this.choose(
                                                client,
                                                fight,
                                                endCrystalEntity,
                                                bl2,
                                                bl3 ? module : null);
                                if (shot == null) {
                                    this.idle(client, player2);
                                } else {
                                    this.engage();
                                    if (shot.plan() instanceof CrystalAuraModule.Plan plan2) {
                                        if (this.render.get()) {
                                            this.show(fight, plan2);
                                        }
                                        if (!this.perform(client, player2, fight, plan2, look)
                                                || look != CrystalAuraModule.Look.SILENT) {
                                            return;
                                        }
                                        CrystalAuraModule.Plan plan3 =
                                                this.planCrystal(
                                                        fight,
                                                        this.lastHit(client.world, confirmWindow),
                                                        1,
                                                        this.stagedBreak(fight));
                                        Rotation rotation2 = ServerRotation.sent();
                                        this.turn(
                                                plan3 != null && !fight.holds(plan3)
                                                        ? fight.lookAt(plan3.aim())
                                                        : rotation2);
                                    } else {
                                        module.show(shot);
                                        if (this.fire(client, player2, fight, module, shot)) {
                                            ServerRotation.request(ServerRotation.sent());
                                        }
                                    }
                                }
                            }
                        } else {
                            this.idle(client, player2);
                        }
                    } else {
                        this.disengage(client, player2);
                    }
                } else {
                    CombatMacroLock.release(this);
                }
            } else {
                this.disengage(client, player2);
            }
        } else {
            this.disengage(client, null);
            Crystals.forgetRtt();
        }
    }

    public void steer(ClientPlayerEntity player2, float steerMin2, float steerSelf2) {
        this.steerMin = steerMin2;
        this.steerSelf = steerSelf2;
        this.steerUntil = player2.age + 2;
    }

    public void buildEagerly(ClientPlayerEntity player2) {
        this.eagerUntil = player2.age + 2;
    }

    public void crystalsFrom(ClientPlayerEntity player2, int floorY2) {
        this.floorY = floorY2;
        this.floorUntil = player2.age + 2;
    }

    public void fullSpeed(ClientPlayerEntity player2) {
        this.fastUntil = player2.age + 2;
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (this.render.get()) {
            this.view.draw();
        }
    }

    @Override
    public boolean isHudOverlayEnabled() {
        return this.isEnabled() && this.render.get() && this.view.showing();
    }

    @Override
    public void renderHudOverlay(ImDrawList imDrawList, float f, float g) {
        this.view.label(imDrawList, f, g);
    }

    @EventHandler
    public void onAttack(PlayerAttackEntityEvent event) {
        Entity target2 = event.target;
        EndCrystalEntity endCrystalEntity;
        if (target2 instanceof EndCrystalEntity
                && (endCrystalEntity = (EndCrystalEntity) target2).getEntityWorld().isClient()) {
            this.markBreak(endCrystalEntity);
        }
    }

    @EventHandler
    public void onPacketReceive(PacketReceiveEvent event) {
        ClientPlayerEntity player2 = MinecraftClient.getInstance().player;
        if (player2 != null) {
            if (event.packet instanceof BundleS2CPacket bundlePacket) {
                for (Packet packet : bundlePacket.getPackets()) {
                    this.note(player2, packet);
                }
            } else {
                this.note(player2, event.packet);
            }
            HurtClock.note(player2, event.packet);
        }
    }

    private void note(ClientPlayerEntity player2, Packet<?> packet) {
        EntitySpawnS2CPacket entitySpawnPacket;
        if (packet instanceof EntitySpawnS2CPacket
                && (entitySpawnPacket = (EntitySpawnS2CPacket) packet).getEntityType()
                        == EntityType.END_CRYSTAL) {
            Crystals.noteSpawn(
                    player2,
                    BlockPos.ofFloored(
                            entitySpawnPacket.getX(),
                            entitySpawnPacket.getY() - 1.0,
                            entitySpawnPacket.getZ()));
        }
    }

    private static boolean mayTakeHand(ClientPlayerEntity player2, boolean bl) {
        ItemStack mainHandStack = player2.getMainHandStack();
        return mainHandStack.isOf(Items.END_CRYSTAL)
                || mainHandStack.isOf(Items.OBSIDIAN)
                || Crystals.isCombatItem(mainHandStack)
                || bl
                        && (mainHandStack.isOf(Items.RESPAWN_ANCHOR)
                                || mainHandStack.isOf(Items.GLOWSTONE));
    }

    private boolean ownsSelection(ClientPlayerEntity player2) {
        return this.ownSlot != -1 && player2.getInventory().getSelectedSlot() == this.ownSlot;
    }

    private void engage() {
        this.engaged = true;
        this.idleTicks = 0;
        CombatMacroLock.tryAcquire(this);
    }

    private void idle(MinecraftClient client, ClientPlayerEntity player2) {
        if (!this.engaged || ++this.idleTicks >= 10) {
            this.disengage(client, player2);
        }
    }

    private void disengage(MinecraftClient client, ClientPlayerEntity player2) {
        this.engaged = false;
        this.idleTicks = 0;
        CombatMacroLock.release(this);
        if (player2 == null) {
            this.forgetSlots();
        } else if (client.currentScreen == null
                && !AutoEatModule.isEating()
                && !AutoInvTotemModule.wantsHotbar()
                && !MeleeAttacks.actedThisTick(player2)) {
            this.restoreSlot(player2);
        }
    }

    private void restoreSlot(ClientPlayerEntity player2) {
        if (this.ownSlot != -1) {
            PlayerInventory inventory = player2.getInventory();
            if (this.switchBack.get()
                    && PlayerInventory.isValidHotbarIndex(this.savedSlot)
                    && this.savedSlot != this.ownSlot
                    && inventory.getSelectedSlot() == this.ownSlot) {
                inventory.setSelectedSlot(this.savedSlot);
            }
            this.forgetSlots();
        }
    }

    private void forgetSlots() {
        this.savedSlot = -1;
        this.ownSlot = -1;
    }

    private void resetState() {
        this.engaged = false;
        this.idleTicks = 0;
        this.forgetSlots();
        this.ticksSinceBreak = 1000;
        this.ticksSincePlace = 1000;
        this.breakGap = 0;
        this.placeGap = 0;
        this.lastMain = null;
        this.mainAge = 1000;
        this.pendingBreakId = -1;
        this.pendingBreakAge = 1000;
        this.pendingObsidian = null;
        this.pendingObsidianAge = 1000;
        this.breakAttempts.clear();
        this.doomed.clear();
        Crystals.forgetRtt();
        CombatMacroLock.release(this);
    }

    private void forgetGone(ClientWorld world) {
        forgetGone(world, this.breakAttempts.keySet());
        forgetGone(world, this.doomed.keySet());
    }

    private static void forgetGone(ClientWorld world, IntSet intSet) {
        IntIterator intIterator = intSet.iterator();
        while (intIterator.hasNext()) {
            if (world.getEntityById(intIterator.nextInt()) == null) {
                intIterator.remove();
            }
        }
    }

    private static boolean oneLook(Vec3d vec, Box box, Vec3d vec2, double d) {
        Vec3d vec3 = vec2.subtract(vec).normalize();
        return box.raycast(vec, vec.add(vec3.multiply(d))).isPresent();
    }

    private boolean canHostSoon(CrystalAuraModule.Fight fight, BlockPos pos, Entity entity) {
        BlockPos pos2 = pos.up();
        if (!fight.level.isAir(pos2)) {
            return false;
        } else {
            Box box =
                    new Box(
                            pos2.getX(),
                            pos2.getY(),
                            pos2.getZ(),
                            pos2.getX() + 1.0,
                            pos2.getY() + 2.0,
                            pos2.getZ() + 1.0);
            for (Entity entity2 : fight.level.getOtherEntities(entity, box)) {
                if (entity2.isAlive()) {
                    LivingEntity entity3;
                    if (!(entity2 instanceof LivingEntity)
                            || !fight.enemies.contains(entity3 = (LivingEntity) entity2)) {
                        return false;
                    }
                    Box box2 =
                            entity3.getBoundingBox().offset(Combo.lead(entity3, fight.arriveLead));
                    boolean bl = box2.minY >= box.maxY;
                    boolean bl2 =
                            box2.maxX <= box.minX
                                    || box2.minX >= box.maxX
                                    || box2.maxZ <= box.minZ
                                    || box2.minZ >= box.maxZ;
                    if (!bl && !bl2) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    private EndCrystalEntity lastHit(ClientWorld world, int i) {
        if (this.pendingBreakId >= 0 && this.pendingBreakAge < i) {
            Entity entityById = world.getEntityById(this.pendingBreakId);
            EndCrystalEntity endCrystalEntity;
            return entityById instanceof EndCrystalEntity
                            && (endCrystalEntity = (EndCrystalEntity) entityById).isAlive()
                    ? endCrystalEntity
                    : null;
        } else {
            return null;
        }
    }

    private Combo.Shot choose(
            MinecraftClient client,
            CrystalAuraModule.Fight fight,
            EndCrystalEntity endCrystalEntity,
            boolean bl,
            AnchorAuraModule module) {
        ArrayList list = new ArrayList();
        Combo.Shot shot = null;
        float f = 0.0F;
        if (bl) {
            CrystalAuraModule.Plan plan2 = this.stagedBreak(fight);
            CrystalAuraModule.Plan plan3 = this.planCrystal(fight, endCrystalEntity, 0, plan2);
            if (plan3 != null) {
                shot = this.shot(fight, plan3, false);
                if (this.due(plan3, 0)) {
                    list.add(shot);
                }
            }
            if (module != null && plan2 != null) {
                f = this.shot(fight, plan2, true).value();
            }
        }
        if (module != null) {
            list.addAll(
                    module.shots(client, fight.player, fight.enemies, fight.friends, fight.rtt, f));
        }
        Combo.Shot shot2;
        return (shot2 = Combo.pick(list)) != null ? shot2 : shot;
    }

    private void show(CrystalAuraModule.Fight fight, CrystalAuraModule.Plan plan2) {
        CrystalAuraModule.Side side = fight.side(plan2.step());
        boolean bl = plan2.step() == CrystalAuraModule.Step.OBSIDIAN;
        Vec3d vec =
                plan2.step() == CrystalAuraModule.Step.BREAK
                        ? plan2.crystal().getEntityPos()
                        : Crystals.explosionAt(plan2.target());
        float f = 0.0F;
        for (CrystalDamage.Victim enemy : side.enemies) {
            f = Math.max(f, enemy.damage(vec, bl));
        }
        BlockPos pos =
                plan2.step() == CrystalAuraModule.Step.BREAK
                        ? plan2.crystal().getBlockPos().down()
                        : plan2.target();
        this.view.show(new Box(pos), this.renderColor.argb(), f, side.self.damage(vec, bl));
    }

    private CrystalAuraModule.Plan planCrystal(
            CrystalAuraModule.Fight fight,
            EndCrystalEntity endCrystalEntity,
            int i,
            CrystalAuraModule.Plan plan2) {
        CrystalAuraModule.Plan plan3 = this.planBreak(fight, false);
        if (plan3 != null && this.due(plan3, i)) {
            return plan3;
        } else {
            boolean bl =
                    plan3 != null
                            || plan2 != null
                            || Crystals.anyPlacing(fight.player, fight.window);
            CrystalAuraModule.Plan plan4 =
                    bl
                            ? null
                            : this.planPlacement(fight, endCrystalEntity, endCrystalEntity == null);
            return plan4 != null ? plan4 : plan3;
        }
    }

    private CrystalAuraModule.Plan stagedBreak(CrystalAuraModule.Fight fight) {
        return this.planBreak(fight, true);
    }

    private Combo.Shot shot(
            CrystalAuraModule.Fight fight, CrystalAuraModule.Plan plan2, boolean bl) {
        CrystalAuraModule.Step step2 = plan2.step();
        Vec3d vec =
                step2 == CrystalAuraModule.Step.BREAK
                        ? plan2.crystal().getEntityPos()
                        : Crystals.explosionAt(plan2.target());
        int i =
                step2 == CrystalAuraModule.Step.BREAK
                        ? 0
                        : (step2 == CrystalAuraModule.Step.CRYSTAL ? fight.rtt : fight.rtt + 1);
        return new Combo.Shot(
                Combo.Kind.valueOf(step2.name()),
                plan2.aim(),
                plan2.route(),
                this.score(fight, step2, vec, -1.0F / 0.0F, bl),
                kills(fight, step2, vec, bl),
                i,
                plan2);
    }

    private CrystalAuraModule.Plan planBreak(CrystalAuraModule.Fight fight, boolean bl) {
        double entityInteractionRange = fight.player.getEntityInteractionRange();
        CrystalAuraModule.Plan plan2 = null;
        float f = -1.0F / 0.0F;
        Box box = fight.player.getBoundingBox().expand(entityInteractionRange + 2.0);
        for (EndCrystalEntity endCrystalEntity :
                fight.level.getNonSpectatingEntities(EndCrystalEntity.class, box)) {
            Vec3d eye2;
            if (endCrystalEntity.isAlive()
                    && this.breakAttempts.get(endCrystalEntity.getId()) < 3
                    && (endCrystalEntity.getId() != this.pendingBreakId
                            || this.pendingBreakAge >= fight.window)
                    && (!this.doomed.containsKey(endCrystalEntity.getId())
                            || fight.player.age - this.doomed.get(endCrystalEntity.getId())
                                    >= fight.window)
                    && !((eye2 = fight.eye)
                                    .squaredDistanceTo(
                                            TargetingUtils.closestPoint(
                                                    eye2, endCrystalEntity.getBoundingBox()))
                            > entityInteractionRange * entityInteractionRange)
                    && !(endCrystalEntity.getY() < this.tickFloor - 0.01)) {
                Vec3d entityPos = endCrystalEntity.getEntityPos();
                float score;
                CrystalAuraModule.Plan plan3;
                if ((!this.protectLoot.get() || !LootGuard.near(entityPos))
                        && !(this.bound(fight, CrystalAuraModule.Step.BREAK, entityPos)
                                        + fight.lookBonus
                                <= f)
                        && (plan3 = this.breakLook(fight, endCrystalEntity)) != null
                        && fight.inView(plan3)
                        && (score =
                                        this.score(
                                                fight,
                                                CrystalAuraModule.Step.BREAK,
                                                entityPos,
                                                f - fight.lookBonus,
                                                bl))
                                != -1.0F / 0.0F
                        && !((score = score + fight.lookCredit(plan3)) <= f)) {
                    plan2 = plan3;
                    f = score;
                }
            }
        }
        if (plan2 == null) {
            return null;
        } else {
            int weaponRoute = Crystals.weaponRoute(fight.player);
            return weaponRoute == -1 ? null : plan2.withRoute(weaponRoute);
        }
    }

    private CrystalAuraModule.Plan breakLook(
            CrystalAuraModule.Fight fight, EndCrystalEntity endCrystalEntity) {
        if (fight.fixedLook != null) {
            CrystalAuraModule.Plan plan2 =
                    new CrystalAuraModule.Plan(
                            CrystalAuraModule.Step.BREAK,
                            endCrystalEntity,
                            null,
                            null,
                            endCrystalEntity.getBoundingBox().getCenter(),
                            -2);
            return hitAlong(fight.player, plan2, fight.eye, fight.fixedLook) != null ? plan2 : null;
        } else {
            BlockPos pos = endCrystalEntity.getBlockPos().down();
            CrystalAuraModule.Plan plan3;
            if (Crystals.isBase(fight.level.getBlockState(pos))
                    && hitAlong(
                                    fight.player,
                                    plan3 =
                                            new CrystalAuraModule.Plan(
                                                    CrystalAuraModule.Step.BREAK,
                                                    endCrystalEntity,
                                                    pos,
                                                    pos,
                                                    aimOnTop(fight.level, pos),
                                                    -2),
                                    fight.aimEye,
                                    fight.lookAt(plan3.aim()))
                            != null) {
                return plan3;
            } else {
                Vec3d vec = fight.towardLook(endCrystalEntity.getBoundingBox().contract(0.1));
                CrystalAuraModule.Plan plan4 =
                        new CrystalAuraModule.Plan(
                                CrystalAuraModule.Step.BREAK,
                                endCrystalEntity,
                                null,
                                null,
                                vec,
                                -2);
                if (hitAlong(fight.player, plan4, fight.aimEye, fight.lookAt(vec)) != null) {
                    return plan4;
                } else {
                    Vec3d vec2 =
                            TargetingUtils.closestPoint(
                                    fight.aimEye, endCrystalEntity.getBoundingBox().contract(0.1));
                    CrystalAuraModule.Plan plan5 =
                            new CrystalAuraModule.Plan(
                                    CrystalAuraModule.Step.BREAK,
                                    endCrystalEntity,
                                    null,
                                    null,
                                    vec2,
                                    -2);
                    return hitAlong(fight.player, plan5, fight.aimEye, fight.lookAt(vec2)) != null
                            ? plan5
                            : null;
                }
            }
        }
    }

    private CrystalAuraModule.Plan planPlacement(
            CrystalAuraModule.Fight fight, EndCrystalEntity endCrystalEntity, boolean bl) {
        CrystalAuraModule.Plan plan2 =
                this.planBase(fight, CrystalAuraModule.Step.CRYSTAL, endCrystalEntity);
        if (this.autoObsidian.get() && bl) {
            boolean bl2 = fight.player.age <= this.eagerUntil;
            float f = plan2 == null ? 0.0F : this.enemyDamage(fight, plan2);
            if (plan2 != null) {
                float g = bl2 ? 11.0F : 8.0F;
                if (f >= g) {
                    return plan2;
                }
            }
            CrystalAuraModule.Plan plan3;
            if ((plan3 = this.planBase(fight, CrystalAuraModule.Step.OBSIDIAN, null)) != null
                    && plan2 != null) {
                float enemyDamage = this.enemyDamage(fight, plan3);
                float h = bl2 ? 3.0F : 5.0F;
                boolean bl3;
                if (enemyDamage >= f + h) {
                    float f2 = bl2 ? 1.4F : 2.0F;
                    bl3 = enemyDamage >= f * f2;
                } else {
                    bl3 = false;
                }
                return bl3 ? plan3 : plan2;
            } else {
                return plan3 == null ? plan2 : plan3;
            }
        } else {
            return plan2;
        }
    }

    private float enemyDamage(CrystalAuraModule.Fight fight, CrystalAuraModule.Plan plan2) {
        Vec3d vec = Crystals.explosionAt(plan2.target());
        float f = 0.0F;
        for (CrystalDamage.Victim enemy : fight.side(plan2.step()).enemies) {
            f = Math.max(f, enemy.damage(vec, plan2.step() == CrystalAuraModule.Step.OBSIDIAN));
        }
        return f;
    }

    private CrystalAuraModule.Plan planBase(
            CrystalAuraModule.Fight fight,
            CrystalAuraModule.Step step2,
            EndCrystalEntity endCrystalEntity) {
        ClientPlayerEntity player2 = fight.player;
        ClientWorld level2 = fight.level;
        Item item = step2 == CrystalAuraModule.Step.CRYSTAL ? Items.END_CRYSTAL : Items.OBSIDIAN;
        BlockState defaultState;
        if (Crystals.route(
                        player2,
                        item,
                        defaultState = Blocks.OBSIDIAN.getDefaultState(),
                        this.savedSlot)
                == -1) {
            return null;
        } else if (step2 == CrystalAuraModule.Step.OBSIDIAN
                && Crystals.route(player2, Items.END_CRYSTAL, defaultState, this.savedSlot) == -1) {
            return null;
        } else {
            double placeReach2 = fight.placeReach;
            double d = player2.getEntityInteractionRange() - 0.1;
            Vec3d eye2 = fight.eye;
            BlockPos pos = BlockPos.ofFloored(eye2);
            int i = MathHelper.ceil(placeReach2);
            float f =
                    step2 == CrystalAuraModule.Step.CRYSTAL && fight.lookBonus > 0.0F ? 2.0F : 0.0F;
            ArrayList list = new ArrayList();
            Mutable mutable = new Mutable();
            Mutable mutable2 = new Mutable();
            for (int j = -i; j <= i; j++) {
                for (int k = -i; k <= i; k++) {
                    for (int m = -i; m <= i; m++) {
                        mutable.set(pos, j, k, m);
                        if (mutable.getY() + 1 >= this.tickFloor) {
                            BlockState blockState = level2.getBlockState(mutable);
                            if (step2 == CrystalAuraModule.Step.CRYSTAL
                                    ? Crystals.isBase(blockState)
                                    : blockState.isReplaceable()) {
                                mutable2.set(mutable, Direction.DOWN);
                                if ((step2 != CrystalAuraModule.Step.OBSIDIAN
                                                || level2.getBlockState(mutable2)
                                                        .isSideSolidFullSquare(
                                                                level2, mutable2, Direction.UP))
                                        && (step2 == CrystalAuraModule.Step.CRYSTAL
                                                ? !Crystals.placing(player2, mutable, fight.window)
                                                : !mutable.equals(this.pendingObsidian)
                                                        || this.pendingObsidianAge
                                                                >= fight.window)) {
                                    BlockPos pos2 = mutable.toImmutable();
                                    BlockPos pos3 =
                                            step2 == CrystalAuraModule.Step.CRYSTAL
                                                    ? pos2
                                                    : mutable2.toImmutable();
                                    Vec3d vec = aimOnTop(level2, pos3);
                                    float bound2;
                                    if (!(eye2.squaredDistanceTo(vec) > placeReach2 * placeReach2)
                                            && !(eye2.squaredDistanceTo(
                                                            TargetingUtils.closestPoint(
                                                                    eye2, Crystals.boxOn(pos2)))
                                                    > d * d)
                                            && (!this.protectLoot.get()
                                                    || !LootGuard.near(Crystals.explosionAt(pos2)))
                                            && (bound2 =
                                                            this.bound(
                                                                    fight,
                                                                    step2,
                                                                    Crystals.explosionAt(pos2)))
                                                    != -1.0F / 0.0F) {
                                        list.add(
                                                new CrystalAuraModule.Candidate(
                                                        new CrystalAuraModule.Plan(
                                                                step2, null, pos3, pos2, vec, -2),
                                                        bound2));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            list.sort(Comparator.comparingDouble(CrystalAuraModule.Candidate::bound).reversed());
            CrystalAuraModule.Plan plan2 = null;
            float g = -1.0F / 0.0F;
            Iterator iterator = list.iterator();
            CrystalAuraModule.Candidate candidate;
            while (iterator.hasNext()
                    && !((candidate = (CrystalAuraModule.Candidate) iterator.next()).bound()
                                    + fight.lookBonus
                                    + f
                            <= g)) {
                CrystalAuraModule.Plan plan3 = candidate.plan();
                float score =
                        this.score(
                                fight,
                                step2,
                                Crystals.explosionAt(plan3.target()),
                                g - fight.lookBonus - f,
                                false);
                CrystalAuraModule.Plan plan4;
                if (score != -1.0F / 0.0F
                        && level2.getWorldBorder().contains(plan3.clicked())
                        && this.canHostSoon(
                                fight,
                                plan3.target(),
                                step2 == CrystalAuraModule.Step.CRYSTAL ? endCrystalEntity : null)
                        && (plan4 = this.reachable(fight, plan3)) != null
                        && fight.inView(plan4)) {
                    score += fight.lookCredit(plan4);
                    if (f > 0.0F
                            && oneLook(
                                    fight.aimEye, Crystals.boxOn(plan4.target()), plan4.aim(), d)) {
                        score += f;
                    }
                    if (!(score <= g)) {
                        plan2 = plan4;
                        g = score;
                    }
                }
            }
            if (plan2 == null) {
                return null;
            } else {
                int route2 =
                        Crystals.route(
                                player2,
                                item,
                                level2.getBlockState(plan2.clicked()),
                                this.savedSlot);
                return route2 == -1 ? null : plan2.withRoute(route2);
            }
        }
    }

    private CrystalAuraModule.Plan reachable(
            CrystalAuraModule.Fight fight, CrystalAuraModule.Plan plan2) {
        if (fight.fixedLook == null) {
            BlockPos pos = plan2.clicked();
            double d = plan2.aim().y;
            Vec3d vec =
                    fight.towardLook(
                            new Box(
                                    pos.getX() + 0.1,
                                    d,
                                    pos.getZ() + 0.1,
                                    pos.getX() + 1.0 - 0.1,
                                    d,
                                    pos.getZ() + 1.0 - 0.1));
            CrystalAuraModule.Plan plan3 =
                    new CrystalAuraModule.Plan(
                            plan2.step(), null, pos, plan2.target(), vec, plan2.route());
            if (fight.eye.squaredDistanceTo(vec) <= fight.placeReach * fight.placeReach
                    && hitAlong(fight.player, plan3, fight.aimEye, fight.lookAt(vec)) != null) {
                return plan3;
            }
        }
        if (hitAlong(fight.player, plan2, fight.aimEye, fight.lookAt(plan2.aim())) != null) {
            return plan2;
        } else if (plan2.step() == CrystalAuraModule.Step.CRYSTAL && fight.fixedLook == null) {
            Vec3d vec2 = Vec3d.ofCenter(plan2.clicked());
            for (Direction direction : Type.HORIZONTAL) {
                Vec3d vec3 =
                        vec2.add(direction.getOffsetX() * 0.4, 0.0, direction.getOffsetZ() * 0.4);
                CrystalAuraModule.Plan plan4;
                if (!((fight.aimEye.x - vec3.x) * direction.getOffsetX()
                                        + (fight.aimEye.z - vec3.z) * direction.getOffsetZ()
                                <= 0.0)
                        && hitAlong(
                                        fight.player,
                                        plan4 =
                                                new CrystalAuraModule.Plan(
                                                        plan2.step(),
                                                        null,
                                                        plan2.clicked(),
                                                        plan2.target(),
                                                        vec3,
                                                        plan2.route()),
                                        fight.aimEye,
                                        fight.lookAt(vec3))
                                != null) {
                    return plan4;
                }
            }
            return null;
        } else {
            return null;
        }
    }

    private static Vec3d aimOnTop(ClientWorld world, BlockPos pos) {
        double max = world.getBlockState(pos).getOutlineShape(world, pos).getMax(Axis.Y);
        return new Vec3d(pos.getX() + 0.5, pos.getY() + max - 0.1, pos.getZ() + 0.5);
    }

    private float need(LivingEntity entity, boolean bl, boolean bl2) {
        float f =
                !bl2 && (!this.facePlace.get() || !facePlaced(entity))
                        ? this.tickMin
                        : Math.min(2.0F, this.tickMin);
        return bl ? f * 0.6F : f;
    }

    private static boolean facePlaced(LivingEntity entity) {
        return CrystalDamage.health(entity) <= 10.0F || Combo.inHole(entity) || armorLow(entity);
    }

    private static boolean armorLow(LivingEntity entity) {
        for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES) {
            ItemStack equippedStack;
            if (equipmentSlot.isArmorSlot()
                    && (equippedStack = entity.getEquippedStack(equipmentSlot)).isDamageable()
                    && equippedStack.getMaxDamage() - equippedStack.getDamage()
                            <= equippedStack.getMaxDamage() * 0.1F) {
                return true;
            }
        }
        return false;
    }

    private float bound(CrystalAuraModule.Fight fight, CrystalAuraModule.Step step2, Vec3d vec) {
        float f = -1.0F / 0.0F;
        for (CrystalDamage.Victim enemy : fight.side(step2).enemies) {
            LivingEntity entity = enemy.entity();
            float upperBound = enemy.upperBound(vec);
            if (Combo.kills(upperBound, entity)
                    || !(upperBound
                            < this.need(
                                    entity, step2 == CrystalAuraModule.Step.BREAK, fight.forced))) {
                f = Math.max(f, Combo.value(upperBound, entity));
            }
        }
        return f;
    }

    private float score(
            CrystalAuraModule.Fight fight,
            CrystalAuraModule.Step step2,
            Vec3d vec,
            float f,
            boolean bl) {
        CrystalAuraModule.Side side = fight.side(step2);
        boolean bl2 = step2 == CrystalAuraModule.Step.OBSIDIAN;
        float damage = side.self.damage(vec, bl2);
        if (!(damage > this.tickSelf) && !CrystalDamage.endangers(fight.player, damage)) {
            for (CrystalDamage.Victim friend : side.friends) {
                float damage2 = friend.damage(vec, bl2);
                if (damage2 > this.tickSelf
                        || CrystalDamage.endangers((PlayerEntity) friend.entity(), damage2)) {
                    return -1.0F / 0.0F;
                }
            }
            float g = -1.0F / 0.0F;
            for (CrystalDamage.Victim enemy : side.enemies) {
                LivingEntity entity = enemy.entity();
                float effective = effective(fight, enemy, step2, vec, bl);
                if (Combo.kills(effective, entity)
                        || !(effective
                                < this.need(
                                        entity,
                                        step2 == CrystalAuraModule.Step.BREAK,
                                        fight.forced))) {
                    g = Math.max(g, Combo.value(effective, entity) - 0.5F * damage);
                }
            }
            return g > f ? g : -1.0F / 0.0F;
        } else {
            return -1.0F / 0.0F;
        }
    }

    private static boolean kills(
            CrystalAuraModule.Fight fight, CrystalAuraModule.Step step2, Vec3d vec, boolean bl) {
        for (CrystalDamage.Victim enemy : fight.side(step2).enemies) {
            if (Combo.kills(effective(fight, enemy, step2, vec, bl), enemy.entity())) {
                return true;
            }
        }
        return false;
    }

    private static float effective(
            CrystalAuraModule.Fight fight,
            CrystalDamage.Victim victim,
            CrystalAuraModule.Step step2,
            Vec3d vec,
            boolean bl) {
        int i =
                step2 == CrystalAuraModule.Step.BREAK
                        ? (bl ? HurtClock.opensIn(victim.entity(), fight.rtt) : 0)
                        : fight.rtt;
        return HurtClock.effective(
                victim, victim.raw(vec, step2 == CrystalAuraModule.Step.OBSIDIAN), i, fight.rtt);
    }

    private static HitResult hitAlong(
            ClientPlayerEntity player2,
            CrystalAuraModule.Plan plan2,
            Vec3d vec,
            Rotation rotation) {
        if (rotation == null) {
            return null;
        } else {
            Vec3d vec2 = Vec3d.fromPolar(rotation.pitch(), rotation.yaw());
            if (plan2.step() == CrystalAuraModule.Step.BREAK) {
                Box boundingBox = plan2.crystal().getBoundingBox();
                if (boundingBox.contains(vec)) {
                    return new EntityHitResult(plan2.crystal(), vec);
                } else {
                    Vec3d vec3 =
                            boundingBox
                                    .raycast(
                                            vec,
                                            vec.add(
                                                    vec2.multiply(
                                                            player2.getEntityInteractionRange())))
                                    .orElse(null);
                    return vec3 != null && MeleeAttacks.lineIsClear(player2, vec, vec3)
                            ? new EntityHitResult(plan2.crystal(), vec3)
                            : null;
                }
            } else {
                ClientWorld entityWorld = (ClientWorld) player2.getEntityWorld();
                BlockState blockState = entityWorld.getBlockState(plan2.clicked());
                BlockHitResult blockHitResult =
                        blockState
                                .getOutlineShape(entityWorld, plan2.clicked())
                                .raycast(
                                        vec,
                                        vec.add(vec2.multiply(player2.getBlockInteractionRange())),
                                        plan2.clicked());
                if (blockHitResult != null
                        && MeleeAttacks.lineIsClear(player2, vec, blockHitResult.getPos())) {
                    return plan2.step() == CrystalAuraModule.Step.OBSIDIAN
                                    && !plan2.target()
                                            .equals(
                                                    Crystals.placedObsidian(
                                                            entityWorld,
                                                            player2,
                                                            blockHitResult,
                                                            blockState))
                            ? null
                            : blockHitResult;
                } else {
                    return null;
                }
            }
        }
    }

    private boolean perform(
            MinecraftClient client,
            ClientPlayerEntity player2,
            CrystalAuraModule.Fight fight,
            CrystalAuraModule.Plan plan2,
            CrystalAuraModule.Look look) {
        Vec3d eyePos = player2.getEyePos();
        Rotation rotation = ServerRotation.sent();
        Rotation rotation2 =
                look == CrystalAuraModule.Look.SILENT
                        ? rotation
                        : RotationUtils.entityRotation(player2);
        HitResult hitResult = rotation == null ? null : hitAlong(player2, plan2, eyePos, rotation);
        if (hitResult != null
                && rotation2 != rotation
                && hitAlong(player2, plan2, eyePos, rotation2) == null) {
            hitResult = null;
        }
        if (plan2.route() != -2) {
            this.select(player2, plan2.route());
        }
        boolean bl =
                hitResult != null
                        && this.due(plan2, 0)
                        && (plan2.step() == CrystalAuraModule.Step.BREAK
                                ? this.strike(client, player2, fight, plan2.crystal())
                                : this.place(client, player2, plan2, (BlockHitResult) hitResult));
        if (bl) {
            MeleeAttacks.claimTick(client);
        } else if (look == CrystalAuraModule.Look.SILENT) {
            this.turn(hitResult != null ? rotation : fight.lookAt(plan2.aim()));
        }
        return bl;
    }

    private boolean fire(
            MinecraftClient client,
            ClientPlayerEntity player2,
            CrystalAuraModule.Fight fight,
            AnchorAuraModule module,
            Combo.Shot shot) {
        if (shot.route() != -2) {
            this.select(player2, shot.route());
        }
        if (!module.ready(client, player2, shot)) {
            this.turn(fight.lookAt(shot.aim()));
            return false;
        } else {
            module.fire(client, player2, shot);
            return true;
        }
    }

    private boolean strike(
            MinecraftClient client,
            ClientPlayerEntity player2,
            CrystalAuraModule.Fight fight,
            EndCrystalEntity endCrystalEntity) {
        if (this.mainAge < 2
                && !Crystals.enchantsAddDamage(player2.getMainHandStack())
                && !Crystals.dealsDamage(player2, ItemStack.EMPTY)) {
            return false;
        } else if (!Crystals.strike(client, player2, endCrystalEntity)) {
            return false;
        } else {
            this.markBreak(endCrystalEntity);
            this.breakAttempts.addTo(endCrystalEntity.getId(), 1);
            this.ticksSinceBreak = 0;
            this.breakGap = this.humanize.get() && !this.fullSpeed ? Crystals.humanGap() : 0;
            for (CrystalDamage.Victim enemy : fight.side(CrystalAuraModule.Step.BREAK).enemies) {
                float raw = enemy.raw(endCrystalEntity.getEntityPos(), false);
                if (raw > 0.0F) {
                    HurtClock.shot(
                            player2, enemy.entity(), raw, endCrystalEntity.getId(), fight.rtt);
                }
            }
            Box box = endCrystalEntity.getBoundingBox().expand(12.0);
            for (EndCrystalEntity endCrystalEntity2 :
                    fight.level.getNonSpectatingEntities(EndCrystalEntity.class, box)) {
                if (endCrystalEntity2 != endCrystalEntity
                        && endCrystalEntity2.squaredDistanceTo(endCrystalEntity) <= 144.0) {
                    this.doomed.put(endCrystalEntity2.getId(), player2.age);
                }
            }
            return true;
        }
    }

    private boolean place(
            MinecraftClient client,
            ClientPlayerEntity player2,
            CrystalAuraModule.Plan plan2,
            BlockHitResult blockHitResult) {
        if (!Crystals.rightClick(client, player2, blockHitResult)) {
            return false;
        } else {
            if (plan2.step() == CrystalAuraModule.Step.CRYSTAL) {
                Crystals.notePlace(player2, plan2.target());
            } else {
                this.pendingObsidian = plan2.target();
                this.pendingObsidianAge = 0;
            }
            this.ticksSincePlace = 0;
            this.placeGap = this.humanize.get() && !this.fullSpeed ? Crystals.humanGap() : 0;
            return true;
        }
    }

    private void turn(Rotation rotation) {
        ServerRotation.request(
                rotation, this.fullSpeed ? this.turnSpeed.max() : this.turnSpeed.get());
    }

    private static boolean sameLook(Rotation rotation, Rotation rotation2) {
        return rotation != null
                && Math.abs(RotationUtils.yawDelta(rotation.yaw(), rotation2.yaw())) <= 2.0F
                && Math.abs(rotation.pitch() - rotation2.pitch()) <= 2.0F;
    }

    private boolean due(CrystalAuraModule.Plan plan2, int i) {
        return plan2.step() == CrystalAuraModule.Step.BREAK
                ? this.ticksSinceBreak + i > Math.max(this.breakDelay.getInt(), this.breakGap - 1)
                : this.ticksSincePlace + i > Math.max(this.placeDelay.getInt(), this.placeGap - 1);
    }

    private void select(ClientPlayerEntity player2, int ownSlot2) {
        PlayerInventory inventory = player2.getInventory();
        if (this.savedSlot == -1) {
            this.savedSlot = inventory.getSelectedSlot();
        }
        inventory.setSelectedSlot(ownSlot2);
        this.ownSlot = ownSlot2;
        this.lastMain = player2.getMainHandStack();
        this.mainAge = 0;
    }

    private void markBreak(EndCrystalEntity endCrystalEntity) {
        this.pendingBreakId = endCrystalEntity.getId();
        this.pendingBreakAge = 0;
    }

    private void ageCounters() {
        this.ticksSinceBreak = Math.min(this.ticksSinceBreak + 1, 1000);
        this.ticksSincePlace = Math.min(this.ticksSincePlace + 1, 1000);
        this.mainAge = Math.min(this.mainAge + 1, 1000);
        this.pendingBreakAge = Math.min(this.pendingBreakAge + 1, 1000);
        this.pendingObsidianAge = Math.min(this.pendingObsidianAge + 1, 1000);
    }

    @Environment(EnvType.CLIENT)
    private record Candidate(CrystalAuraModule.Plan plan, float bound) {}

    @Environment(EnvType.CLIENT)
    private final class Fight {

        final ClientPlayerEntity player;

        final ClientWorld level;

        final Vec3d eye;

        final Vec3d aimEye;

        final List<LivingEntity> enemies;

        final List<LivingEntity> friends;

        final int window;

        final int rtt;

        final Rotation fixedLook;

        final Rotation serverLook;

        final float lookBonus;

        final int arriveLead;

        private final int placeLead;

        private final int selfPlaceLead;

        final boolean forced;

        final double placeReach;

        private CrystalAuraModule.Side breakSide;

        private CrystalAuraModule.Side placeSide;

        Fight(
                ClientPlayerEntity player2,
                ClientWorld level2,
                List<LivingEntity> enemies2,
                List<LivingEntity> friends2,
                int window2,
                int rtt2,
                Rotation fixedLook2,
                float lookBonus2,
                boolean forced2) {
            this.player = player2;
            this.level = level2;
            this.eye = player2.getEyePos();
            this.aimEye = fixedLook2 != null ? this.eye : this.eye.add(player2.getVelocity());
            this.enemies = enemies2;
            this.friends = friends2;
            this.window = window2;
            this.rtt = rtt2;
            this.fixedLook = fixedLook2;
            this.serverLook = ServerRotation.sent();
            this.lookBonus = lookBonus2;
            this.arriveLead = Math.min(rtt2, 5);
            this.placeLead = Math.min(2 * rtt2, 5);
            this.selfPlaceLead = Math.min(rtt2, 5);
            this.forced = forced2;
            this.placeReach = player2.getBlockInteractionRange() - 0.1;
        }

        CrystalAuraModule.Side side(CrystalAuraModule.Step step2) {
            if (step2 == CrystalAuraModule.Step.BREAK) {
                if (this.breakSide == null) {
                    this.breakSide =
                            new CrystalAuraModule.Side(
                                    this.player, this.enemies, this.friends, this.arriveLead, 0);
                }
                return this.breakSide;
            } else {
                if (this.placeSide == null) {
                    this.placeSide =
                            new CrystalAuraModule.Side(
                                    this.player,
                                    this.enemies,
                                    this.friends,
                                    this.placeLead,
                                    this.selfPlaceLead);
                }
                return this.placeSide;
            }
        }

        Rotation lookAt(Vec3d vec) {
            return this.fixedLook != null ? this.fixedLook : RotationUtils.fromTo(this.aimEye, vec);
        }

        boolean holds(CrystalAuraModule.Plan plan2) {
            return CrystalAuraModule.hitAlong(this.player, plan2, this.eye, this.serverLook)
                    != null;
        }

        float lookCredit(CrystalAuraModule.Plan plan2) {
            return this.lookBonus > 0.0F && this.holds(plan2) ? this.lookBonus : 0.0F;
        }

        boolean inView(CrystalAuraModule.Plan plan2) {
            if (this.fixedLook == null && !(CrystalAuraModule.this.viewAngle.get() >= 180.0F)) {
                float f =
                        Math.abs(
                                RotationUtils.yawDelta(
                                        this.player.getYaw(),
                                        RotationUtils.fromTo(this.aimEye, plan2.aim()).yaw()));
                return f <= CrystalAuraModule.this.viewAngle.get()
                        || f <= CrystalAuraModule.this.viewAngle.get() + 20.0F && this.holds(plan2);
            } else {
                return true;
            }
        }

        Vec3d towardLook(Box box) {
            Rotation rotation =
                    this.serverLook != null
                            ? this.serverLook
                            : RotationUtils.entityRotation(this.player);
            Vec3d vec = Vec3d.fromPolar(rotation.pitch(), rotation.yaw());
            Vec3d center = box.getCenter();
            boolean bl = box.getLengthY() < 1.0E-6 && vec.y < -1.0E-6 && this.aimEye.y > box.minY;
            double d =
                    bl
                            ? (box.minY - this.aimEye.y) / vec.y
                            : center.subtract(this.aimEye).dotProduct(vec);
            if (d <= 0.0) {
                return center;
            } else {
                Vec3d vec2 = this.aimEye.add(vec.multiply(d));
                return new Vec3d(
                        MathHelper.clamp(vec2.x, box.minX, box.maxX),
                        MathHelper.clamp(vec2.y, box.minY, box.maxY),
                        MathHelper.clamp(vec2.z, box.minZ, box.maxZ));
            }
        }
    }

    @Environment(EnvType.CLIENT)
    private static enum Look {
        SILENT,
        CROSSHAIR;

        private Look() {}
    }

    @Environment(EnvType.CLIENT)
    private record Plan(
            CrystalAuraModule.Step step,
            EndCrystalEntity crystal,
            BlockPos clicked,
            BlockPos target,
            Vec3d aim,
            int route) {

        CrystalAuraModule.Plan withRoute(int i) {
            return new CrystalAuraModule.Plan(
                    this.step, this.crystal, this.clicked, this.target, this.aim, i);
        }
    }

    @Environment(EnvType.CLIENT)
    private static final class Side {

        final List<CrystalDamage.Victim> enemies = new ArrayList<>();

        final List<CrystalDamage.Victim> friends = new ArrayList<>();

        final CrystalDamage.Victim self;

        Side(
                ClientPlayerEntity player2,
                List<LivingEntity> list,
                List<LivingEntity> list2,
                int i,
                int j) {
            for (LivingEntity entity : list) {
                this.enemies.add(new CrystalDamage.Victim(entity, Combo.lead(entity, i)));
            }
            for (LivingEntity entity2 : list2) {
                this.friends.add(new CrystalDamage.Victim(entity2, Combo.lead(entity2, i)));
            }
            this.self = new CrystalDamage.Victim(player2, Combo.lead(player2, j));
        }
    }

    @Environment(EnvType.CLIENT)
    private static enum Step {
        BREAK,
        CRYSTAL,
        OBSIDIAN;

        private Step() {}
    }
}
