package net.efn.client.module.impl.combat.bot;

import net.efn.client.chat.ModuleChat;
import net.efn.client.easyplace.builder.Aim;
import net.efn.client.event.EventHandler;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.PacketAppliedEvent;
import net.efn.client.event.impl.PacketReceiveEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.CombatMacroLock;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.combat.AnchorAuraModule;
import net.efn.client.module.impl.combat.AutoCityModule;
import net.efn.client.module.impl.combat.AutoInvTotemModule;
import net.efn.client.module.impl.combat.AutoTotemModule;
import net.efn.client.module.impl.combat.AutoTrapModule;
import net.efn.client.module.impl.combat.CrystalAuraModule;
import net.efn.client.module.impl.combat.anchor.AnchorGuard;
import net.efn.client.module.impl.combat.crystal.Combo;
import net.efn.client.module.impl.combat.crystal.CrystalDamage;
import net.efn.client.module.impl.combat.crystal.Crystals;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.pvp.Rotation;
import net.efn.client.util.pvp.RotationUtils;
import net.efn.client.util.pvp.ServerRotation;
import net.efn.client.util.pvp.TargetingUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
public final class CrystalBotModule extends ClientModule {

    private static final double MIN = 2.4;

    private static final double IDEAL = 3.2;

    private static final double MAX = 4.0;

    private static final Predicate<ItemStack> GAPPLE =
            itemStack ->
                    itemStack.isOf(Items.GOLDEN_APPLE)
                            || itemStack.isOf(Items.ENCHANTED_GOLDEN_APPLE);

    private static final Predicate<ItemStack> XP =
            itemStack -> itemStack.isOf(Items.EXPERIENCE_BOTTLE);

    private static final Predicate<ItemStack> PEARL =
            itemStack -> itemStack.isOf(Items.ENDER_PEARL);

    private static final Predicate<ItemStack> OBSIDIAN =
            itemStack -> itemStack.isOf(Items.OBSIDIAN);

    private static final Predicate<ItemStack> ANCHOR =
            itemStack -> itemStack.isOf(Items.RESPAWN_ANCHOR);

    private static final Predicate<ItemStack> GLOWSTONE =
            itemStack -> itemStack.isOf(Items.GLOWSTONE);

    private static final float ARMOR_LOW = 0.3F;

    private static final int RUN_HOLD = 40;

    private static final int POP_COVER = 40;

    private static final int LAUNCH_GAP = 80;

    private static final int LAUNCH_STALL = 20;

    private static final int LAUNCH_TIMEOUT = 16;

    private static final double LAUNCH_START = 3.9;

    private static final double LAUNCH_GIVE_UP = 4.1;

    private static final double LAUNCH_STEP_IN = 2.9;

    private static final int LAUNCH_WATCH = 8;

    private static final double LAUNCH_LIFT = 0.3;

    private static final double HIT_RANGE = 2.95;

    private static final int THROW_TIMEOUT = 14;

    private static final int PEARL_GAP = 30;

    private static final int SHIELD_GAP = 20;

    private static final int BREACH_WAIT = 40;

    private static final int BREACH_WAIT_CAMPER = 15;

    private static final int BREACH_GAP = 100;

    private static final double ESCAPE_FAR = 20.0;

    private static final double ESCAPE_NEAR = 12.0;

    private static final int SEARCH_GAP = 10;

    private static final double EAT_CLEAR = 8.0;

    private static final int FOLLOW_TICKS = 12;

    private static final int OPENER_WINDOW = 40;

    private static final int CAMP_MAX = 2;

    private static final int SPOT_SCAN_GAP = 5;

    private static final float SPOT_DANGER = 8.0F;

    private static final int WORKING = 0;

    private static final int PLACED = 1;

    private static final int REGROUP_TICKS = 300;

    private static final double REGROUP_RANGE = 64.0;

    private static final int BOX_AIM_TIMEOUT = 10;

    private static final float RETREAT_HEALTH = 6.0F;

    private static final int POP_STREAK = 3;

    private static final int POP_STREAK_GAP = 600;

    private static final int POP_RUN = 200;

    private static final double BLAST_WATCH = 7.0;

    private static final double BELOW_DROP = 3.0;

    private static final double BELOW_REACH = 8.0;

    private static final double DIG_DEEPEST = 6.0;

    private static final int BELOW_SETTLE = 20;

    private static final int DIG_TIMEOUT = 80;

    private static final int DIG_WALK = 40;

    private static final int DIG_MAX = 2;

    private static final int DIG_GAP = 100;

    private static final int DIG_DOWN_TICKS = 50;

    private static final int DIG_BASE_TICKS = 25;

    private static final float LEVEL_MIN_DAMAGE = 2.5F;

    private final NumberSetting range =
            this.addSetting(
                    "range", new NumberSetting("Fight Range", 16.0F, 8.0F, 32.0F).step(1.0F));

    private final BooleanSetting anchors =
            this.addSetting("anchors", new BooleanSetting("Use Anchors", true));

    private final BooleanSetting launches =
            this.addSetting("knock-up", new BooleanSetting("Knock-Up Hits", true));

    private final BooleanSetting breach =
            this.addSetting("break-holes", new BooleanSetting("Break Holes", true));

    private final BooleanSetting guardAnchors =
            this.addSetting("guard-anchors", new BooleanSetting("Guard Anchors", true));

    private final BooleanSetting surround =
            this.addSetting("surround", new BooleanSetting("Surround Self", true));

    private final NumberSetting eatBelow =
            this.addSetting(
                    "eat-below",
                    new NumberSetting("Gapple Below HP", 14.0F, 0.0F, 20.0F).step(1.0F));

    private final BooleanSetting mendArmor =
            this.addSetting("mend-armor", new BooleanSetting("Mend Armor", true));

    private final BooleanSetting escape =
            this.addSetting("escape-pearls", new BooleanSetting("Escape Pearl", true));

    private final BooleanSetting chase =
            this.addSetting("chase-pearls", new BooleanSetting("Chase Pearls", true));

    private final BooleanSetting refill =
            this.addSetting("refill", new BooleanSetting("Refill Hotbar", true));

    private final BooleanSetting learn =
            this.addSetting("learn", new BooleanSetting("Learn Rivals", true));

    private final BooleanSetting dance =
            this.addSetting("strafe", new BooleanSetting("Strafe", true));

    private final MinecraftClient mc = MinecraftClient.getInstance();

    private final BotBody body = new BotBody();

    private final BotKit kit = new BotKit();

    private final RivalBook book = new RivalBook();

    private final BotPause pause = new BotPause();

    private final AirTrack air = new AirTrack();

    private PlayerEntity challenger;

    private int challengerSince;

    private int attackedBy = -1;

    private int attackedAt = -1000;

    private double closest;

    private int closerAt;

    private StandPicker.Spot standing;

    private int standScanAt;

    private int defuseAt = -1000;

    private int retreatAt = -1000;

    private Boolean crystalWasOn;

    private Boolean anchorWasOn;

    private Boolean totemWasOn;

    private int popStreak;

    private boolean poppedOut;

    private BlockPos digAnchor;

    private boolean digPlaced;

    private boolean digAnnounced;

    private int digGuards;

    private int digUntil;

    private int digWalkUntil;

    private int digBlasts;

    private int digAfter;

    private int belowSince = -1;

    private int stepDigAfter;

    private int baseDigAfter;

    private int diggingAt = -100;

    private boolean eating;

    private boolean needsGapple;

    private boolean mending;

    private BlockPos boxingAt;

    private int boxingUntil;

    private int tick;

    private PlayerEntity fought;

    private CrystalBotModule.Stance stance = CrystalBotModule.Stance.HOLD;

    private int runUntil;

    private int ownPopAt = -1000;

    private CrystalBotModule.Throw throwing;

    private Rotation throwLook;

    private Vec3d throwSpot;

    private double throwAway;

    private int throwUntil;

    private int pearlAt = -1000;

    private int searchAfter;

    private int theirPearl = -1;

    private int followedPearl = -1;

    private boolean launching;

    private int launchUntil;

    private int nextLaunch;

    private int launchedAt = -1;

    private double launchY;

    private double launchPeak;

    private int shieldAt = -1000;

    private BlockPos boxAiming;

    private int boxAimTicks;

    private BlockPos boxSkipped;

    private int boxSkippedUntil;

    private int followUntil;

    private float selfCapNow;

    private boolean followLaid = true;

    private BlockPos blocking;

    private int spotScanAt;

    private int cityFails;

    private int openerArm = -1;

    private double openerReach;

    private int openerAt;

    private float openerHealth;

    private int openerPops;

    private boolean cityStarted;

    private int breachAfter;

    private float lastTheirHealth;

    private int hurtAt;

    private int progressAt;

    private int theirPopAt = -1;

    private int holeSince = -1;

    private boolean lowSpell;

    private boolean pearledInSpell;

    public CrystalBotModule() {
        super(
                "crystal-bot",
                "Crystal PvP Bot",
                "Crystal-fights the nearest enemy player: steers the Crystal and Anchor Aura,"
                        + " knocks them up, breaks their hole, guards, eats, mends, refills and"
                        + " pearls.",
                ModuleCategory.COMBAT);
    }

    @Override
    protected void onEnable() {
        this.body.forget();
        this.needsGapple = false;
        this.cityStarted = false;
        this.launching = false;
        this.mending = false;
        this.eating = false;
        this.boxingAt = null;
        this.fought = null;
        this.throwing = null;
        this.tick = 0;
        this.runUntil = 0;
        this.boxSkippedUntil = 0;
        this.followUntil = 0;
        this.nextLaunch = 0;
        this.launchedAt = -1;
        this.shieldAt = -1000;
        this.pearlAt = -1000;
        this.ownPopAt = -1000;
        this.baseDigAfter = 0;
        this.stepDigAfter = 0;
        this.standScanAt = 0;
        this.spotScanAt = 0;
        this.breachAfter = 0;
        this.searchAfter = 0;
        this.retreatAt = -1000;
        this.attackedAt = -1000;
        this.defuseAt = -1000;
        this.attackedBy = -1;
        this.challenger = null;
        this.standing = null;
        this.air.forget();
        this.pause.reset();
        this.totemWasOn = null;
        this.anchorWasOn = null;
        this.crystalWasOn = null;
        this.popStreak = 0;
        this.forgetDig();
        this.on(TickEvent.TYPE, Stage.PRE, tickEvent -> this.tick());
    }

    @Override
    protected void onDisable() {
        this.body.release(this.mc);
        this.body.forget();
        CombatMacroLock.release(this);
        this.restoreAuras();
        AutoCityModule module = ModuleRegistry.get(AutoCityModule.class);
        if (this.cityStarted && module != null && module.isEnabled()) {
            module.setEnabled(false);
        }
        this.cityStarted = false;
        this.book.save(true);
    }

    @EventHandler
    public void onPacketApplied(PacketAppliedEvent event) {
        ClientPlayerEntity player = this.mc.player;
        if (player != null && this.mc.world != null) {
            Packet packet = event.packet;
            EntityStatusS2CPacket entityStatusPacket;
            if (packet instanceof EntityStatusS2CPacket
                    && (entityStatusPacket = (EntityStatusS2CPacket) packet).getStatus() == 35) {
                if (entityStatusPacket.getEntity(this.mc.world) == player) {
                    this.popStreak = this.tick - this.ownPopAt <= 600 ? this.popStreak + 1 : 1;
                    this.ownPopAt = this.tick;
                } else if (this.fought != null
                        && entityStatusPacket.getEntity(this.mc.world) == this.fought) {
                    this.theirPopAt = this.tick;
                    this.openerPops++;
                }
            }
        }
    }

    @EventHandler
    public void onPacketReceive(PacketReceiveEvent event) {
        ClientPlayerEntity player = this.mc.player;
        if (player != null) {
            if (event.packet instanceof PlayerPositionLookS2CPacket playerPositionLookPacket) {
                Vec3d vec = playerPositionLookPacket.change().position();
                Vec3d entityPos = player.getEntityPos();
                Set set = playerPositionLookPacket.relatives();
                this.pause.moved(
                        new Vec3d(
                                set.contains(PositionFlag.X) ? entityPos.x + vec.x : vec.x,
                                set.contains(PositionFlag.Y) ? entityPos.y + vec.y : vec.y,
                                set.contains(PositionFlag.Z) ? entityPos.z + vec.z : vec.z),
                        this.tick - this.pearlAt);
            } else if (event.packet instanceof WorldTimeUpdateS2CPacket worldTimeUpdatePacket) {
                this.pause.time(worldTimeUpdatePacket.time());
            } else if (event.packet instanceof ExplosionS2CPacket explosionPacket) {
                this.air.blast(explosionPacket.center());
            } else {
                Packet packet = event.packet;
                EntityDamageS2CPacket entityDamagePacket;
                if (packet instanceof EntityDamageS2CPacket
                        && (entityDamagePacket = (EntityDamageS2CPacket) packet).entityId()
                                == player.getId()
                        && entityDamagePacket.sourceCauseId() >= 0) {
                    this.attackedBy = entityDamagePacket.sourceCauseId();
                    this.attackedAt = this.tick;
                }
            }
        }
    }

    private void tick() {
        this.tick++;
        ClientPlayerEntity player = this.mc.player;
        ClientWorld world = this.mc.world;
        this.body.release(this.mc);
        if (player != null && world != null && MeleeAttacks.canAct(this.mc) && !player.isDead()) {
            CombatMacroLock.release(this);
            this.keepTotem();
            this.body.stayGrounded(blastNear(world, player));
            if (this.learn.get()) {
                this.book.save(false);
            }
            BotPause.State state;
            if ((state = this.pause.tick(this.mc, player)) == BotPause.State.STOP) {
                ModuleChat.send(
                        this,
                        ModuleChat.body()
                                .danger("The server keeps pulling us back")
                                .text(", stopping."));
                this.requestDisable();
            } else if (state != BotPause.State.RUN) {
                this.throwing = null;
                this.eating = false;
                this.launching = false;
                if (state == BotPause.State.HOLD) {
                    CombatMacroLock.take(this);
                }
            } else {
                boolean bl = this.needsGapple || this.eating || this.tick - this.retreatAt < 300;
                PlayerEntity player2 =
                        this.choose(
                                world,
                                player,
                                this.body.pick(
                                        this.mc,
                                        player,
                                        this.range.get(),
                                        bl ? 64.0 : this.range.get() + 8.0));
                if (player2 != this.fought) {
                    this.newFight(player2);
                }
                boolean bl2 =
                        player.getHealth() <= this.eatAt() && player.getAbsorptionAmount() < 4.0F;
                if (player.getHealth() > this.eatAt() + 4.0F
                        || player.getAbsorptionAmount() >= 4.0F) {
                    this.needsGapple = false;
                }
                if (player2 == null) {
                    this.launching = false;
                    this.throwing = null;
                    CombatMacroLock.release(this);
                    if ((bl2 || this.needsGapple || this.eating && player.isUsingItem())
                            && this.hands()
                            && this.body.use(this.mc, player, GAPPLE)) {
                        this.eating = true;
                    } else {
                        this.eating = false;
                        if (!this.refill.get()
                                || !this.kit.refill(this.mc, player, this.anchors.get())) {
                            if (!this.mendIdle(player)) {
                                this.mending = false;
                            }
                        }
                    }
                } else {
                    RivalBook.Rival rival =
                            this.learn.get() ? this.book.of(player2.getUuid()) : null;
                    this.watch(world, player2, rival);
                    if (feetY(player) < MathHelper.floor(player2.getY() + 0.01)) {
                        this.body.stayGrounded(true);
                    }
                    this.air.note(player2, this.tick, rival);
                    Combo.prefer(player, player2);
                    Combo.flight(player, player2, i -> this.air.lead(i, this.tick));
                    this.runAuras(world);
                    CrystalBotModule.Stance stance2 = this.stance;
                    this.stance = this.stance(world, player, player2, rival);
                    if (this.stance == CrystalBotModule.Stance.RUN
                            && stance2 != CrystalBotModule.Stance.RUN) {
                        ModuleChat.send(
                                this,
                                this.poppedOut
                                        ? ModuleChat.body()
                                                .warn("3 totems popped")
                                                .text(", getting away.")
                                        : ModuleChat.body().warn("Losing").text(", getting away."));
                    }
                    this.poppedOut = false;
                    this.steer(player, player2, rival);
                    double distance = player.distanceTo(player2);
                    if (distance < this.closest - 0.5) {
                        this.closest = distance;
                        this.closerAt = this.tick;
                    }
                    if (this.throwing == null || !this.keepThrowing(player, player2)) {
                        if (this.stance != CrystalBotModule.Stance.RUN
                                || !this.getAway(world, player, player2)) {
                            if (!this.getDown(world, player, player2)) {
                                if (!this.guardAnchors.get()
                                        || !this.defuse(world, player)
                                                && !this.guard(world, player, player2)) {
                                    if (!this.launching || !this.launch(player, player2)) {
                                        if (this.followLaid
                                                || this.tick > this.followUntil
                                                || !this.followUp(world, player, player2)) {
                                            boolean bl3 =
                                                    player2.isUsingItem()
                                                            && player2.getActiveItem()
                                                                    .contains(
                                                                            DataComponentTypes.FOOD)
                                                            && totems(player) >= 1
                                                            && player.getHealth() > 8.0F;
                                            if (bl2 && !bl3) {
                                                this.needsGapple = true;
                                            }
                                            if (this.needsGapple
                                                    || this.eating && player.isUsingItem()) {
                                                if (distance >= 8.0) {
                                                    if (BotBody.hotbarSlot(player, GAPPLE) >= 0) {
                                                        if (this.hands()
                                                                && this.body.use(
                                                                        this.mc, player, GAPPLE)) {
                                                            this.eating = true;
                                                        }
                                                        this.body.face(
                                                                this.mc, player, player2, 1.0F);
                                                        this.body.backOff(this.mc, player);
                                                        return;
                                                    }
                                                } else if (this.critical(player)
                                                        && BotBody.hotbarSlot(player, GAPPLE)
                                                                >= 0) {
                                                    this.eating = false;
                                                    this.retreatAt = this.tick;
                                                    if (this.pearlAway(world, player, player2)) {
                                                        return;
                                                    }
                                                    this.body.flee(this.mc, player, player2);
                                                    return;
                                                }
                                            }
                                            this.eating = false;
                                            if (this.cover(world, player, player2, distance)) {
                                                this.body.face(this.mc, player, player2, 1.0F);
                                            } else if (this.stance == CrystalBotModule.Stance.GUARD
                                                    && this.closeSpot(world, player, player2)) {
                                                this.body.face(this.mc, player, player2, 1.0F);
                                            } else if (!this.breach.get()
                                                    || !this.breakHole(
                                                            world, player, player2, rival)) {
                                                if (!this.mendArmor.get()
                                                        || !(distance >= 12.0)
                                                        || !armorLow(player)
                                                        || !this.hands()
                                                        || !this.throwXp(player)) {
                                                    this.mending = false;
                                                    if (!this.chase.get()
                                                            || !this.chasePearl(
                                                                    world, player, player2,
                                                                    rival)) {
                                                        boolean bl4 =
                                                                distance >= 8.0
                                                                        || BotBody.hotbarSlot(
                                                                                        player,
                                                                                        itemStack ->
                                                                                                itemStack
                                                                                                        .isOf(
                                                                                                                Items
                                                                                                                        .END_CRYSTAL))
                                                                                < 0;
                                                        if (this.refill.get()
                                                                && bl4
                                                                && this.kit.needed(
                                                                        player, this.anchors.get())
                                                                && this.hands()
                                                                && this.kit.refill(
                                                                        this.mc,
                                                                        player,
                                                                        this.anchors.get())) {
                                                            this.body.face(
                                                                    this.mc, player, player2, 1.0F);
                                                        } else if (!this.finish(player, player2)) {
                                                            if (!this.launches.get()
                                                                    || !this.startLaunch(
                                                                            player, player2,
                                                                            rival)) {
                                                                if (!this.digDown(
                                                                                world, player,
                                                                                player2, distance)
                                                                        && !this.digBase(
                                                                                world, player,
                                                                                player2,
                                                                                distance)) {
                                                                    CombatMacroLock.release(this);
                                                                    handBack(player);
                                                                    this.body.face(
                                                                            this.mc, player,
                                                                            player2, 1.0F);
                                                                    if (!surrounded(
                                                                            world, player)) {
                                                                        if (!this.stayLow(
                                                                                world, player,
                                                                                player2,
                                                                                distance)) {
                                                                            if (this.tick
                                                                                    >= this
                                                                                            .standScanAt) {
                                                                                this.standScanAt =
                                                                                        this.tick
                                                                                                + 10;
                                                                                this.standing =
                                                                                        StandPicker
                                                                                                .pick(
                                                                                                        world,
                                                                                                        player,
                                                                                                        player2,
                                                                                                        2.4,
                                                                                                        3.2,
                                                                                                        4.0,
                                                                                                        this
                                                                                                                .stance
                                                                                                                .cover);
                                                                            }
                                                                            if (this.standing
                                                                                            == null
                                                                                    || this.body
                                                                                            .toward(
                                                                                                    this
                                                                                                            .mc,
                                                                                                    player,
                                                                                                    this
                                                                                                            .standing
                                                                                                            .center())) {
                                                                                this.standing =
                                                                                        null;
                                                                                this.body
                                                                                        .keepDistance(
                                                                                                this
                                                                                                        .mc,
                                                                                                player,
                                                                                                player2,
                                                                                                2.4,
                                                                                                3.2,
                                                                                                4.0,
                                                                                                this
                                                                                                        .dance
                                                                                                        .get());
                                                                            }
                                                                        }
                                                                    }
                                                                } else {
                                                                    this.body.face(
                                                                            this.mc, player,
                                                                            player2, 1.0F);
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            this.body.forget();
            CombatMacroLock.release(this);
            this.throwing = null;
            this.launching = false;
        }
    }

    private PlayerEntity choose(
            ClientWorld world, ClientPlayerEntity player, PlayerEntity player2) {
        PlayerEntity player3 = null;
        double d = -1.0 / 0.0;
        for (PlayerEntity player4 : enemies(world, player, this.range.get())) {
            double worth = this.worth(player, player4);
            if (worth > d) {
                player3 = player4;
                d = worth;
            }
        }
        if (player2 == null || !player2.isAlive() || this.fought == null) {
            this.challenger = null;
            if (player3 != null) {
                this.body.hold(player3);
            }
            return player3;
        } else if (player3 != null && player3 != player2) {
            boolean bl = player.distanceTo(player2) <= 6.0;
            double worth2 = this.worth(player, player2);
            double e = bl ? 8.0 : 2.0;
            if (d < worth2 + e) {
                this.challenger = null;
                return player2;
            } else if (player3 != this.challenger) {
                this.challenger = player3;
                this.challengerSince = this.tick;
                return player2;
            } else if (this.tick - this.challengerSince < (bl ? 20 : 6)) {
                return player2;
            } else {
                this.challenger = null;
                this.body.hold(player3);
                return player3;
            }
        } else {
            this.challenger = null;
            return player2;
        }
    }

    private double worth(ClientPlayerEntity player, PlayerEntity player2) {
        double distance = player.distanceTo(player2);
        double d = distance <= 6.0 ? 1.0 : (distance >= 10.0 ? 0.0 : (10.0 - distance) / 4.0);
        double e =
                4.0 * d
                        - 0.2 * distance
                        + (20.0F - Math.min(20.0F, CrystalDamage.health(player2))) / 5.0;
        if (!player2.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)
                && !player2.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
            // bytecode: LDC 1.5 (decompiler printed ++)
            e += 1.5;
        }
        if (armorLow(player2)) {
            e++;
        }
        if (player2.getId() == this.attackedBy
                && this.tick - this.attackedAt <= 60
                && distance <= 4.5) {
            e += 4.0;
        }
        return e;
    }

    private boolean finish(ClientPlayerEntity player, PlayerEntity player2) {
        if (this.pause.slowServer()
                || player2.hurtTime > 0
                || reach(player, player2) > 2.95
                || CrystalDamage.health(player2) > 3.0F) {
            return false;
        } else if (!MeleeAttacks.isMeleeWeapon(player.getMainHandStack())
                || MeleeAttacks.charge(player) < 0.9F) {
            return false;
        } else if (!this.hands()) {
            return true;
        } else {
            this.body.face(this.mc, player, player2, 1.0F);
            this.strike(player, player2);
            return true;
        }
    }

    private void newFight(PlayerEntity fought2) {
        if (this.fought != null && this.lowSpell && this.pearledInSpell && this.learn.get()) {
            this.book.of(this.fought.getUuid()).lowSpell(true);
        }
        this.fought = fought2;
        this.launching = false;
        this.holeSince = -1;
        this.theirPopAt = -1;
        this.launchedAt = -1;
        this.pearledInSpell = false;
        this.lowSpell = false;
        this.followedPearl = -1;
        this.theirPearl = -1;
        this.openerArm = -1;
        this.closest = 1.7976931348623157E308;
        this.closerAt = this.tick;
        this.standing = null;
        this.followLaid = true;
        this.blocking = null;
        this.cityFails = 0;
        this.air.forget();
        this.forgetDig();
        this.progressAt = this.hurtAt = this.tick;
        float f = this.lastTheirHealth = fought2 == null ? 0.0F : CrystalDamage.health(fought2);
        if (fought2 != null) {
            ModuleChat.send(
                    this,
                    ModuleChat.body()
                            .text("Fighting ")
                            .name(fought2.getName().getString())
                            .text("."));
            if (this.learn.get()) {
                this.book.of(fought2.getUuid()).fought();
            }
        } else {
            ModuleChat.send(this, ModuleChat.body().text("No enemy in range, waiting."));
        }
    }

    private CrystalBotModule.Stance stance(
            ClientWorld world,
            ClientPlayerEntity player,
            PlayerEntity player2,
            RivalBook.Rival rival) {
        int totems = totems(player);
        float f = player.getHealth() + player.getAbsorptionAmount();
        float g = worstThreat(world, player, this.digAnchor) / Math.max(1.0F, f);
        int enemiesWithin = enemiesWithin(world, player, 8.0);
        boolean bl = totems == 0 && enemiesWithin >= 2;
        if (bl) {
            this.runUntil = Math.max(this.runUntil, this.tick + 40);
        }
        if (this.popStreak >= 3) {
            this.popStreak = 0;
            this.poppedOut = true;
            this.runUntil = this.tick + 200;
        }
        if (this.tick < this.runUntil) {
            return CrystalBotModule.Stance.RUN;
        } else if (this.tick - this.ownPopAt <= 40) {
            return CrystalBotModule.Stance.GUARD;
        } else if (this.finishing(player2, rival)) {
            return CrystalBotModule.Stance.PRESS;
        } else {
            boolean bl2 = f <= this.retreatAt() + 2.0F || totems <= 1 || g >= 0.35F;
            return bl2 && f <= 8.0F ? CrystalBotModule.Stance.GUARD : CrystalBotModule.Stance.PRESS;
        }
    }

    private boolean finishing(PlayerEntity player, RivalBook.Rival rival) {
        if (this.theirPopAt >= 0 && !player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
            int i = rival == null ? 8 : MathHelper.clamp(rival.retotemWindow(8), 3, 30);
            return this.tick - this.theirPopAt <= i;
        } else {
            return false;
        }
    }

    private void steer(ClientPlayerEntity player, PlayerEntity player2, RivalBook.Rival rival) {
        float f = 2.0F;
        float g = player.getHealth() + player.getAbsorptionAmount();
        float h = g - (player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING) ? 1.0F : 4.0F);
        float f2;
        this.selfCapNow = f2 = Math.max(0.5F, Math.min(this.stance.selfCap + f, h));
        float g2 = this.finishing(player2, rival) ? 2.0F : this.stance.minDamage;
        if (feetY(player) >= MathHelper.floor(player2.getY() + 0.01) && !Combo.inHole(player2)) {
            g2 = Math.min(g2, 2.5F);
        }
        CrystalAuraModule module;
        if ((module = ModuleRegistry.get(CrystalAuraModule.class)) != null) {
            module.steer(player, g2, f2);
            module.crystalsFrom(player, feetY(player) + 1);
            module.fullSpeed(player);
            module.buildEagerly(player);
        }
        AnchorAuraModule module2;
        if ((module2 = ModuleRegistry.get(AnchorAuraModule.class)) != null) {
            module2.steer(
                    player,
                    this.finishing(player2, rival) ? 2.0F : this.stance.anchorMin,
                    Math.min(f2, 11.0F));
        }
    }

    private float eatAt() {
        return this.eatBelow.get() - 2.0F;
    }

    private float retreatAt() {
        return 4.0F;
    }

    private boolean critical(ClientPlayerEntity player) {
        return player.getHealth() + player.getAbsorptionAmount() <= this.retreatAt()
                && !player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);
    }

    private static int totems(ClientPlayerEntity player) {
        int i = player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING) ? 1 : 0;
        PlayerInventory inventory = player.getInventory();
        for (int j = 0; j < 36; j++) {
            if (inventory.getStack(j).isOf(Items.TOTEM_OF_UNDYING)) {
                i += inventory.getStack(j).getCount();
            }
        }
        return i;
    }

    private static float worstThreat(ClientWorld world, ClientPlayerEntity player, BlockPos pos) {
        List list = enemies(world, player, 16.0);
        float f = 0.0F;
        for (EndCrystalEntity endCrystalEntity :
                world.getNonSpectatingEntities(
                        EndCrystalEntity.class, player.getBoundingBox().expand(12.0))) {
            Vec3d entityPos = endCrystalEntity.getEntityPos();
            float estimate = CrystalDamage.estimate(entityPos, player);
            if (estimate > f
                    && worthIt(
                            list,
                            endCrystalEntity.getBoundingBox(),
                            entityPos,
                            estimate,
                            4.0,
                            6.0F,
                            null)) {
                f = estimate;
            }
        }
        BlockPos pos2 = player.getBlockPos();
        for (BlockPos pos3 : BlockPos.iterate(pos2.add(-5, -2, -5), pos2.add(5, 3, 5))) {
            BlockState blockState = world.getBlockState(pos3);
            Vec3d vec;
            float estimate2;
            BlockPos pos4;
            if (blockState.isOf(Blocks.RESPAWN_ANCHOR)
                    && blockState.get(RespawnAnchorBlock.CHARGES) > 0
                    && !pos3.equals(pos)
                    && (estimate2 =
                                    CrystalDamage.estimate(
                                            vec = Vec3d.ofCenter(pos4 = pos3.toImmutable()),
                                            player,
                                            5.0F,
                                            pos4))
                            > f
                    && worthIt(list, new Box(pos4), vec, estimate2, 5.5, 5.0F, pos4)) {
                f = estimate2;
            }
        }
        return f;
    }

    private static boolean worthIt(
            List<PlayerEntity> list, Box box, Vec3d vec, float f, double d, float g, BlockPos pos) {
        for (PlayerEntity player : list) {
            Vec3d eyePos = player.getEyePos();
            if (!(eyePos.squaredDistanceTo(TargetingUtils.closestPoint(eyePos, box)) > d * d)
                    && CrystalDamage.estimate(vec, player, g, pos) < f) {
                return true;
            }
        }
        return false;
    }

    private static int enemiesWithin(ClientWorld world, ClientPlayerEntity player, double d) {
        int i = 0;
        for (PlayerEntity player2 : world.getPlayers()) {
            if (player2 != player
                    && player2.isAlive()
                    && MeleeAttacks.isEnemy(player, player2)
                    && player2.distanceTo(player) <= d) {
                i++;
            }
        }
        return i;
    }

    private static List<PlayerEntity> enemies(
            ClientWorld world, ClientPlayerEntity player, double d) {
        ArrayList list = new ArrayList();
        for (PlayerEntity player2 : world.getPlayers()) {
            if (player2 != player
                    && player2.isAlive()
                    && MeleeAttacks.isEnemy(player, player2)
                    && player2.distanceTo(player) <= d) {
                list.add(player2);
            }
        }
        return list;
    }

    private void runAuras(ClientWorld world) {
        CrystalAuraModule module = ModuleRegistry.get(CrystalAuraModule.class);
        if (module != null && !module.isEnabled()) {
            if (this.crystalWasOn == null) {
                this.crystalWasOn = false;
            }
            module.setEnabled(true);
        }
        AnchorAuraModule module2 = ModuleRegistry.get(AnchorAuraModule.class);
        boolean bl = this.anchors.get() && world.getRegistryKey() != World.NETHER;
        if (module2 != null && bl && !module2.isEnabled()) {
            if (this.anchorWasOn == null) {
                this.anchorWasOn = false;
            }
            module2.setEnabled(true);
        }
    }

    private void keepTotem() {
        AutoInvTotemModule module = ModuleRegistry.get(AutoInvTotemModule.class);
        AutoTotemModule module2 = ModuleRegistry.get(AutoTotemModule.class);
        if (module != null && !module.isEnabled() && (module2 == null || !module2.isEnabled())) {
            if (this.totemWasOn == null) {
                this.totemWasOn = false;
            }
            module.setEnabled(true);
        }
    }

    private void restoreAuras() {
        CrystalAuraModule module = ModuleRegistry.get(CrystalAuraModule.class);
        if (module != null && Boolean.FALSE.equals(this.crystalWasOn)) {
            module.setEnabled(false);
        }
        AnchorAuraModule module2;
        if ((module2 = ModuleRegistry.get(AnchorAuraModule.class)) != null
                && Boolean.FALSE.equals(this.anchorWasOn)) {
            module2.setEnabled(false);
        }
        AutoInvTotemModule module3;
        if ((module3 = ModuleRegistry.get(AutoInvTotemModule.class)) != null
                && Boolean.FALSE.equals(this.totemWasOn)) {
            module3.setEnabled(false);
        }
        this.totemWasOn = null;
        this.anchorWasOn = null;
        this.crystalWasOn = null;
    }

    private static boolean blastNear(ClientWorld world, ClientPlayerEntity player) {
        for (EndCrystalEntity endCrystalEntity :
                world.getNonSpectatingEntities(
                        EndCrystalEntity.class, player.getBoundingBox().expand(7.0))) {
            if (CrystalDamage.estimate(endCrystalEntity.getEntityPos(), player) >= 1.0F) {
                return true;
            }
        }
        BlockPos pos = player.getBlockPos();
        int i = (int) Math.ceil(7.0);
        for (BlockPos pos2 : BlockPos.iterate(pos.add(-i, -2, -i), pos.add(i, 3, i))) {
            BlockState blockState = world.getBlockState(pos2);
            if (blockState.isOf(Blocks.RESPAWN_ANCHOR)
                    && blockState.get(RespawnAnchorBlock.CHARGES) > 0
                    && Vec3d.ofCenter(pos2).distanceTo(player.getEntityPos()) <= 7.0) {
                return true;
            }
        }
        return false;
    }

    private static void handBack(ClientPlayerEntity player) {
        ItemStack mainHandStack = player.getMainHandStack();
        if (!mainHandStack.isOf(Items.END_CRYSTAL)
                && !mainHandStack.isOf(Items.OBSIDIAN)
                && !mainHandStack.isOf(Items.RESPAWN_ANCHOR)
                && !mainHandStack.isOf(Items.GLOWSTONE)
                && !Crystals.isCombatItem(mainHandStack)) {
            int hotbarSlot =
                    BotBody.hotbarSlot(player, itemStack -> itemStack.isOf(Items.END_CRYSTAL));
            if (hotbarSlot < 0) {
                hotbarSlot = BotBody.hotbarSlot(player, MeleeAttacks::isMeleeWeapon);
            }
            if (hotbarSlot >= 0) {
                player.getInventory().setSelectedSlot(hotbarSlot);
            }
        }
    }

    private boolean hands() {
        CombatMacroLock.take(this);
        return !MeleeAttacks.actedThisTick(this.mc.player);
    }

    private void watch(ClientWorld world, PlayerEntity player, RivalBook.Rival rival) {
        float health = CrystalDamage.health(player);
        if (health < this.lastTheirHealth - 0.5F) {
            this.hurtAt = this.tick;
        }
        if (health < this.lastTheirHealth - 0.5F || !Combo.inHole(player)) {
            this.progressAt = this.tick;
        }
        this.lastTheirHealth = health;
        if (this.openerArm >= 0 && this.tick - this.openerAt >= 40) {
            float f = this.openerHealth - health + 10.0F * this.openerPops;
            if (rival != null) {
                rival.opened(
                        this.openerArm, this.openerReach, MathHelper.clamp(f / 12.0F, 0.0F, 1.5F));
            }
            this.openerArm = -1;
        }
        if (this.launchedAt >= 0) {
            this.launchPeak = Math.max(this.launchPeak, player.getY() - this.launchY);
            if (this.tick - this.launchedAt >= 8) {
                if (rival != null) {
                    rival.launched(this.launchPeak >= 0.3);
                }
                this.launchedAt = -1;
            }
        }
        if (this.theirPopAt >= 0 && rival != null) {
            if (player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                rival.retotemed(this.tick - this.theirPopAt);
                this.theirPopAt = -1;
            } else if (this.tick - this.theirPopAt > 100) {
                rival.retotemed(100);
                this.theirPopAt = -1;
            }
        } else if (this.theirPopAt >= 0 && this.tick - this.theirPopAt > 100) {
            this.theirPopAt = -1;
        }
        if (Combo.inHole(player)) {
            if (this.holeSince < 0) {
                this.holeSince = this.tick;
            }
        } else if (this.holeSince >= 0) {
            if (rival != null) {
                rival.leftHole(Math.min(600, this.tick - this.holeSince));
            }
            this.holeSince = -1;
        }
        if (!this.lowSpell && health <= 8.0F) {
            this.lowSpell = true;
            this.pearledInSpell = false;
        } else if (this.lowSpell && health > 12.0F) {
            if (rival != null) {
                rival.lowSpell(this.pearledInSpell);
            }
            this.lowSpell = false;
        }
        for (EnderPearlEntity enderPearlEntity :
                world.getNonSpectatingEntities(
                        EnderPearlEntity.class, player.getBoundingBox().expand(4.0))) {
            if (enderPearlEntity.getOwner() == player
                    && enderPearlEntity.age <= 2
                    && enderPearlEntity.getId() != this.followedPearl) {
                if (this.lowSpell) {
                    this.pearledInSpell = true;
                }
                this.theirPearl = enderPearlEntity.getId();
            }
        }
    }

    private boolean getAway(ClientWorld world, ClientPlayerEntity player, PlayerEntity player2) {
        if (player.distanceTo(player2) < 12.0 && this.pearlAway(world, player, player2)) {
            return true;
        } else if (player.distanceTo(player2) >= 16.0
                && player.getHealth() <= this.eatAt() + 4.0F
                && this.hands()
                && this.body.use(this.mc, player, GAPPLE)) {
            this.eating = true;
            return true;
        } else {
            this.body.flee(this.mc, player, player2);
            return true;
        }
    }

    private boolean pearlAway(ClientWorld world, ClientPlayerEntity player, PlayerEntity player2) {
        if (this.escape.get() && this.pearlReady(player) && this.tick >= this.searchAfter) {
            List list = enemies(world, player, 48.0);
            Rotation rotation = PearlFlight.escape(player, list, 20.0);
            this.throwAway = 16.0;
            if (rotation == null) {
                rotation = PearlFlight.escape(player, list, 12.0);
                this.throwAway = 12.0;
            }
            if (rotation == null) {
                this.searchAfter = this.tick + 10;
                return false;
            } else {
                this.startThrow(CrystalBotModule.Throw.ESCAPE, rotation, null);
                return this.keepThrowing(player, player2);
            }
        } else {
            return false;
        }
    }

    private boolean pearlReady(ClientPlayerEntity player) {
        return this.tick - this.pearlAt >= 30
                && BotBody.hotbarSlot(player, PEARL) >= 0
                && !player.getItemCooldownManager().isCoolingDown(new ItemStack(Items.ENDER_PEARL))
                && (player.getHealth() + player.getAbsorptionAmount() > 6.0F
                        || player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING));
    }

    private void startThrow(
            CrystalBotModule.Throw throwing2, Rotation throwLook2, Vec3d throwSpot2) {
        this.throwing = throwing2;
        this.throwLook = throwLook2;
        this.throwSpot = throwSpot2;
        this.throwUntil = this.tick + 14;
    }

    private boolean keepThrowing(ClientPlayerEntity player, PlayerEntity player2) {
        if (this.tick > this.throwUntil || !this.pearlReady(player)) {
            this.throwing = null;
            this.pearlAt = this.tick - 15;
            return false;
        } else if (!this.hands()) {
            return true;
        } else {
            Aim.turn(this.mc, this.throwLook.yaw(), this.throwLook.pitch(), 27.0F);
            int hotbarSlot = BotBody.hotbarSlot(player, PEARL);
            if (player.getInventory().getSelectedSlot() != hotbarSlot) {
                player.getInventory().setSelectedSlot(hotbarSlot);
                return true;
            } else if (!ServerRotation.desynced()
                    && !player.isUsingItem()
                    && MeleeAttacks.handsFree(this.mc, player)) {
                if (player.isOnGround()
                        && !(player.getMovement().horizontalLengthSquared() > 1.0E-4)) {
                    float yaw = player.getYaw();
                    float pitch = player.getPitch();
                    Rotation rotation = ServerRotation.commit(player);
                    if (!(Math.abs(MathHelper.wrapDegrees(rotation.yaw() - yaw)) > 0.01F)
                            && !(Math.abs(rotation.pitch() - pitch) > 0.01F)) {
                        boolean bl =
                                this.throwing == CrystalBotModule.Throw.ESCAPE
                                        ? PearlFlight.escapes(
                                                player,
                                                yaw,
                                                pitch,
                                                enemies(this.mc.world, player, 48.0),
                                                this.throwAway)
                                        : PearlFlight.reaches(
                                                player, yaw, pitch, this.throwSpot, 0.0, 3.0);
                        if (!bl) {
                            return true;
                        } else {
                            if (this.mc
                                    .interactionManager
                                    .interactItem(player, Hand.MAIN_HAND)
                                    .isAccepted()) {
                                player.swingHand(Hand.MAIN_HAND);
                                MeleeAttacks.claimTick(this.mc);
                                this.pearlAt = this.tick;
                                if (this.throwing == CrystalBotModule.Throw.ESCAPE) {
                                    ModuleChat.send(
                                            this, ModuleChat.body().warn("Pearled away").text("."));
                                } else {
                                    ModuleChat.send(
                                            this,
                                            ModuleChat.body()
                                                    .text("Pearled after ")
                                                    .name(player2.getName().getString())
                                                    .text("."));
                                }
                            }
                            this.throwing = null;
                            return true;
                        }
                    } else {
                        return true;
                    }
                } else {
                    return true;
                }
            } else {
                return true;
            }
        }
    }

    private boolean chasePearl(
            ClientWorld world,
            ClientPlayerEntity player,
            PlayerEntity player2,
            RivalBook.Rival rival) {
        if ((this.stance == CrystalBotModule.Stance.PRESS
                        || this.stance == CrystalBotModule.Stance.HOLD)
                && this.pearlReady(player)) {
            float f = player.getHealth() + player.getAbsorptionAmount();
            if (f < 6.0F) {
                return false;
            } else {
                int i = count(player, PEARL);
                boolean bl = this.theirPearl >= 0;
                boolean bl2 =
                        totems(player) == 0 && player.getHealth() <= 12.0F
                                || !bl
                                        && rival != null
                                        && rival.pearlsWhenLow()
                                        && CrystalDamage.health(player2) <= 8.0F;
                if (i <= (bl2 ? 1 : 0)) {
                    return false;
                } else {
                    Vec3d vec = null;
                    if (bl) {
                        this.followedPearl = this.theirPearl;
                        this.theirPearl = -1;
                        Entity entityById = world.getEntityById(this.followedPearl);
                        EnderPearlEntity enderPearlEntity;
                        PearlFlight.Landing landing;
                        if (entityById instanceof EnderPearlEntity
                                && (landing =
                                                PearlFlight.fly(
                                                        world,
                                                        enderPearlEntity =
                                                                (EnderPearlEntity) entityById,
                                                        enderPearlEntity.getEntityPos(),
                                                        enderPearlEntity.getVelocity()))
                                        != null
                                && landing.at().distanceTo(player.getEntityPos()) > 8.0) {
                            vec = landing.at();
                        }
                    }
                    double distance = player.distanceTo(player2);
                    if (vec == null && distance <= this.range.get() + 8.0) {
                        Vec3d vec2 =
                                player2.getEntityPos()
                                        .subtract(player.getEntityPos())
                                        .multiply(1.0, 0.0, 1.0)
                                        .normalize();
                        Vec3d vec3 =
                                player2.getEntityPos()
                                        .subtract(player2.lastX, player2.lastY, player2.lastZ);
                        boolean bl3 = distance >= 12.0 && vec3.x * vec2.x + vec3.z * vec2.z > 0.12;
                        BlockPos pos = player2.getBlockPos().down();
                        boolean bl4 =
                                !world.getBlockState(pos).getCollisionShape(world, pos).isEmpty();
                        boolean bl5 =
                                distance >= 3.0 && player2.getY() - player.getY() >= 3.0 && bl4;
                        boolean bl6 =
                                distance >= 4.0 && player.getY() - player2.getY() >= 4.0 && bl4;
                        boolean bl7 = distance > 6.0 && this.tick - this.closerAt > 60;
                        if (bl3 || bl5 || bl6 || bl7) {
                            vec =
                                    player2.getEntityPos()
                                            .add(
                                                    vec3.multiply(1.0, 0.0, 1.0)
                                                            .multiply(
                                                                    PearlFlight.flightTicks(
                                                                            distance)));
                        }
                        AirTrack.Landing landing2;
                        if (vec == null
                                && !player2.isOnGround()
                                && distance >= 6.0
                                && (landing2 = this.air.landing(this.tick)) != null
                                && player2.getY() - landing2.at().y >= 3.0) {
                            vec = landing2.at();
                        }
                    }
                    if (vec != null && this.tick >= this.searchAfter) {
                        Rotation rotation = PearlFlight.toward(player, vec, 0.8, 3.0);
                        if (rotation == null) {
                            this.searchAfter = this.tick + 10;
                            return false;
                        } else {
                            this.startThrow(CrystalBotModule.Throw.CHASE, rotation, vec);
                            return this.keepThrowing(player, player2);
                        }
                    } else {
                        return false;
                    }
                }
            }
        } else {
            return false;
        }
    }

    private boolean getDown(ClientWorld world, ClientPlayerEntity player, PlayerEntity player2) {
        double d = player.getY() - player2.getY();
        double e = Math.hypot(player2.getX() - player.getX(), player2.getZ() - player.getZ());
        if (!(d < 3.0)
                && !(e > 8.0)
                && this.stance != CrystalBotModule.Stance.RUN
                && !this.critical(player)) {
            if (this.belowSince < 0) {
                this.belowSince = this.tick;
            }
            if (this.tick - this.belowSince >= 20 && player.isOnGround() && player2.isOnGround()) {
                if (this.digAnchor != null
                        && !world.getBlockState(this.digAnchor).isOf(Blocks.RESPAWN_ANCHOR)) {
                    if (this.digPlaced) {
                        this.digBlasts++;
                        this.digWalkUntil = this.tick + 40;
                    }
                    this.digAnchor = null;
                }
                if (this.tick >= this.digWalkUntil && !openDrop(world, player, player2)) {
                    boolean bl =
                            d <= 6.0
                                    && this.digBlasts < 2
                                    && this.tick >= this.digAfter
                                    && this.anchors.get()
                                    && world.getRegistryKey() != World.NETHER
                                    && BotBody.hotbarSlot(player, ANCHOR) >= 0
                                    && BotBody.hotbarSlot(player, GLOWSTONE) >= 0;
                    return bl && this.blastDown(world, player, player2)
                            ? true
                            : this.pearlDown(player, player2);
                } else {
                    this.body.face(this.mc, player, player2, 1.0F);
                    this.body.dropToward(this.mc, player, player2.getEntityPos());
                    return true;
                }
            } else {
                return false;
            }
        } else {
            this.forgetDig();
            return false;
        }
    }

    private boolean blastDown(ClientWorld world, ClientPlayerEntity player, PlayerEntity player2) {
        if (this.digAnchor == null) {
            this.digAnchor = digCell(world, player, player2);
            if (this.digAnchor == null) {
                this.digAfter = this.tick + 100;
                return false;
            }
            this.digPlaced = false;
            this.digGuards = 0;
            this.digUntil = this.tick + 80;
            if (!this.digAnnounced) {
                ModuleChat.send(
                        this,
                        ModuleChat.body()
                                .text("Blasting down to ")
                                .name(player2.getName().getString())
                                .text("."));
            }
            this.digAnnounced = true;
        }
        if (this.tick > this.digUntil) {
            this.digBlasts = 2;
            this.digAnchor = null;
            return false;
        } else {
            this.body.face(this.mc, player, player2, 1.0F);
            BlockState blockState = world.getBlockState(this.digAnchor);
            if (!blockState.isOf(Blocks.RESPAWN_ANCHOR)) {
                AnchorGuard.Spot spot =
                        blockState.isReplaceable()
                                ? AnchorGuard.supportFor(
                                        world, player, player.getEyePos(), this.digAnchor)
                                : null;
                if (spot == null) {
                    this.digAnchor = null;
                    this.digAfter = this.tick + 100;
                    return false;
                } else {
                    if (this.placeAt(world, player, spot, BotBody.hotbarSlot(player, ANCHOR))
                            == 1) {
                        this.digPlaced = true;
                    }
                    return true;
                }
            } else if (blockState.get(RespawnAnchorBlock.CHARGES) <= 0) {
                return this.clickDigAnchor(world, player, BotBody.hotbarSlot(player, GLOWSTONE));
            } else {
                Vec3d vec = AnchorGuard.anchorAim(world, player, this.digAnchor);
                if (vec == null) {
                    this.digAnchor = null;
                    return false;
                } else {
                    int guardBlockSlot = guardBlockSlot(player);
                    AnchorGuard.Spot spot2;
                    if (this.digGuards < 2
                            && guardBlockSlot >= 0
                            && (spot2 = AnchorGuard.find(world, player, this.digAnchor, vec))
                                    != null
                            && !spot2.standing()) {
                        if (this.placeAt(world, player, spot2, guardBlockSlot) == 1) {
                            this.digGuards++;
                        }
                        return true;
                    } else {
                        float estimate =
                                CrystalDamage.estimate(
                                        Vec3d.ofCenter(this.digAnchor),
                                        player,
                                        5.0F,
                                        this.digAnchor);
                        int detonatorRoute =
                                AnchorGuard.detonatorRoute(
                                        player.getInventory(), player.getMainHandStack());
                        if (!(estimate > Math.min(this.selfCapNow, 10.0F))
                                && !CrystalDamage.endangers(player, estimate)
                                && detonatorRoute != -1) {
                            return this.clickDigAnchor(
                                    world,
                                    player,
                                    detonatorRoute == -2
                                            ? player.getInventory().getSelectedSlot()
                                            : detonatorRoute);
                        } else {
                            this.digBlasts = 2;
                            this.digAnchor = null;
                            return false;
                        }
                    }
                }
            }
        }
    }

    private static BlockPos digCell(
            ClientWorld world, ClientPlayerEntity player, PlayerEntity player2) {
        Vec3d eyePos = player.getEyePos();
        double d = player.getBlockInteractionRange() - 0.5;
        BlockPos pos = player.getBlockPos();
        ArrayList list = new ArrayList();
        for (BlockPos pos2 : BlockPos.iterate(pos.add(-4, -2, -4), pos.add(4, 0, 4))) {
            if (!(eyePos.distanceTo(Vec3d.ofCenter(pos2)) > d)) {
                BlockPos pos3 = pos2.down();
                Box box;
                if (world.getBlockState(pos2).isReplaceable()
                        && world.getFluidState(pos2).isEmpty()
                        && world.getBlockState(pos3)
                                .isSideSolidFullSquare(world, pos3, Direction.UP)
                        && !(box = new Box(pos2)).intersects(player.getBoundingBox())
                        && !box.intersects(player2.getBoundingBox())) {
                    list.add(pos2.toImmutable());
                }
            }
        }
        list.sort(
                Comparator.comparingDouble(
                        (BlockPos pos4) ->
                                Vec3d.ofCenter(pos4).squaredDistanceTo(player2.getEntityPos())));
        BlockPos pos5 = null;
        for (BlockPos pos6 : (Iterable<BlockPos>) list.subList(0, Math.min(list.size(), 8))) {
            if (AnchorGuard.supportFor(world, player, eyePos, pos6) != null) {
                if (pos5 == null) {
                    pos5 = pos6;
                }
                if (AnchorGuard.find(
                                world,
                                player,
                                pos6,
                                TargetingUtils.closestPoint(eyePos, new Box(pos6).contract(0.1)))
                        != null) {
                    return pos6;
                }
            }
        }
        return pos5;
    }

    private static boolean openDrop(
            ClientWorld world, ClientPlayerEntity player, PlayerEntity player2) {
        Vec3d vec = player2.getEntityPos().subtract(player.getEntityPos()).multiply(1.0, 0.0, 1.0);
        if (vec.lengthSquared() < 0.25) {
            return false;
        } else {
            Vec3d vec2 = player.getEntityPos().add(vec.normalize().multiply(0.8));
            BlockPos pos = BlockPos.ofFloored(vec2.x, player.getY() + 0.1, vec2.z);
            int i = MathHelper.floor(player2.getY() + 0.01);
            for (BlockPos pos2 = pos; pos2.getY() >= i - 1; pos2 = pos2.down()) {
                BlockState blockState = world.getBlockState(pos2);
                if (blockState.isOf(Blocks.LAVA) || blockState.isIn(BlockTags.FIRE)) {
                    return false;
                }
                if (!blockState.getCollisionShape(world, pos2).isEmpty()) {
                    return pos2.getY() < player.getY() - 1.5 && pos2.getY() <= i + 1;
                }
            }
            return false;
        }
    }

    private boolean clickDigAnchor(ClientWorld world, ClientPlayerEntity player, int selectedSlot) {
        if (selectedSlot < 0) {
            this.digAnchor = null;
            return false;
        } else if (!this.hands()) {
            return true;
        } else {
            BlockHitResult blockHitResult = AnchorGuard.anchorHit(world, player, this.digAnchor);
            if (blockHitResult == null) {
                Vec3d vec = AnchorGuard.anchorAim(world, player, this.digAnchor);
                if (vec == null) {
                    this.digAnchor = null;
                    return false;
                } else {
                    ServerRotation.request(RotationUtils.fromTo(player.getEyePos(), vec), 6.0F);
                    return true;
                }
            } else if (player.getInventory().getSelectedSlot() != selectedSlot) {
                player.getInventory().setSelectedSlot(selectedSlot);
                return true;
            } else {
                if (MeleeAttacks.handsFree(this.mc, player)) {
                    AnchorGuard.click(this.mc, player, blockHitResult);
                }
                return true;
            }
        }
    }

    private boolean pearlDown(ClientPlayerEntity player, PlayerEntity player2) {
        if (this.chase.get() && this.pearlReady(player) && this.tick >= this.searchAfter) {
            Vec3d entityPos = player2.getEntityPos();
            Rotation rotation = PearlFlight.toward(player, entityPos, 0.8, 3.0);
            if (rotation == null) {
                this.searchAfter = this.tick + 10;
                return false;
            } else {
                this.startThrow(CrystalBotModule.Throw.CHASE, rotation, entityPos);
                return this.keepThrowing(player, player2);
            }
        } else {
            return false;
        }
    }

    private static int feetY(ClientPlayerEntity player) {
        return MathHelper.floor(player.getY() + 0.01);
    }

    private boolean wantsLower(ClientPlayerEntity player, PlayerEntity player2, double d) {
        return this.stance != CrystalBotModule.Stance.RUN
                && player.isOnGround()
                && player2.isOnGround()
                && !Combo.inHole(player2)
                && d <= 5.5
                && feetY(player) == MathHelper.floor(player2.getY() + 0.01)
                && !this.justLaunched();
    }

    private boolean justLaunched() {
        return this.launching || this.launchedAt >= 0 || this.tick <= this.followUntil + 20;
    }

    private boolean stayLow(
            ClientWorld world, ClientPlayerEntity player, PlayerEntity player2, double d) {
        if (this.stance != CrystalBotModule.Stance.RUN
                && player.isOnGround()
                && player2.isOnGround()
                && !Combo.inHole(player2)
                && !(d > 5.5)) {
            if (feetY(player) < MathHelper.floor(player2.getY() + 0.01)) {
                return true;
            } else if (!this.wantsLower(player, player2, d)) {
                return false;
            } else {
                BlockPos pos = lowerStep(world, player, player2, feetY(player) - 1);
                if (pos == null) {
                    return false;
                } else {
                    this.body.toward(this.mc, player, Vec3d.ofBottomCenter(pos));
                    return true;
                }
            }
        } else {
            return false;
        }
    }

    private boolean digDown(
            ClientWorld world, ClientPlayerEntity player, PlayerEntity player2, double d) {
        if (this.wantsLower(player, player2, d) && this.tick >= this.stepDigAfter) {
            if (lowerStep(world, player, player2, feetY(player) - 1) != null) {
                return false;
            } else {
                BlockPos pos = player.getBlockPos().down();
                BlockState blockState = world.getBlockState(pos);
                if (BotBody.hotbarSlot(player, OBSIDIAN) < 0 && !Crystals.isBase(blockState)) {
                    return false;
                } else {
                    BlockPos pos2 = pos.down();
                    boolean bl =
                            !blockState.hasBlockEntity()
                                    && digTicks(world, player, pos) <= 50
                                    && !world.getBlockState(pos2)
                                            .getCollisionShape(world, pos2)
                                            .isEmpty()
                                    && !nearLava(world, pos);
                    if (bl && this.mine(world, player, pos)) {
                        return true;
                    } else {
                        this.stepDigAfter = this.tick + 100;
                        return false;
                    }
                }
            }
        } else {
            return false;
        }
    }

    private boolean digBase(
            ClientWorld world, ClientPlayerEntity player, PlayerEntity player2, double d) {
        int i = MathHelper.floor(player2.getY() + 0.01);
        if (this.stance != CrystalBotModule.Stance.RUN
                && player.isOnGround()
                && player2.isOnGround()
                && !Combo.inHole(player2)
                && !(d > 5.5)
                && feetY(player) == i - 1
                && this.tick >= this.baseDigAfter
                && BotBody.hotbarSlot(player, OBSIDIAN) >= 0
                && !this.justLaunched()) {
            Vec3d eyePos = player.getEyePos();
            BlockPos pos = player2.getBlockPos();
            BlockPos pos2 = null;
            double e = 1.7976931348623157E308;
            for (BlockPos pos3 : BlockPos.iterate(pos.add(-2, -1, -2), pos.add(2, -1, 2))) {
                BlockState blockState = world.getBlockState(pos3);
                if (Crystals.isBase(blockState) && Crystals.canHost(world, pos3, null)) {
                    return false;
                }
                Vec3d vec;
                double distance;
                if ((pos3.getX() != pos.getX() || pos3.getZ() != pos.getZ())
                        && Math.max(
                                        Math.abs(pos3.getX() - player.getBlockX()),
                                        Math.abs(pos3.getZ() - player.getBlockZ()))
                                >= 2
                        && !blockState.isReplaceable()
                        && !blockState.hasBlockEntity()
                        && world.isAir(pos3.up())
                        && !((distance =
                                        (vec = Vec3d.ofCenter(pos3).add(0.0, 0.5, 0.0))
                                                .distanceTo(player2.getEntityPos()))
                                > 2.5)
                        && !(eyePos.distanceTo(vec) > player.getBlockInteractionRange() - 0.5)
                        && digTicks(world, player, pos3) <= 25
                        && !(distance >= e)) {
                    pos2 = pos3.toImmutable();
                    e = distance;
                }
            }
            if (pos2 != null && this.mine(world, player, pos2)) {
                return true;
            } else {
                this.baseDigAfter = this.tick + 100;
                return false;
            }
        } else {
            return false;
        }
    }

    private static BlockPos lowerStep(
            ClientWorld world, ClientPlayerEntity player, PlayerEntity player2, int i) {
        BlockPos pos = player.getBlockPos();
        BlockPos pos2 = null;
        double d = 1.7976931348623157E308;
        for (BlockPos pos3 : BlockPos.iterate(pos.add(-2, -1, -2), pos.add(2, -1, 2))) {
            BlockPos pos4 = StandPicker.standable(world, pos3);
            if (pos4 != null && pos4.getY() == i && pos4.equals(pos3)) {
                Vec3d vec = Vec3d.ofBottomCenter(pos4);
                double e = Math.hypot(vec.x - player2.getX(), vec.z - player2.getZ());
                double squaredDistance;
                if (!(e < 2.4)
                        && !(e > 4.5)
                        && walkable(world, player.getEntityPos(), vec)
                        && (squaredDistance = pos4.getSquaredDistance(pos)) < d) {
                    pos2 = pos4.toImmutable();
                    d = squaredDistance;
                }
            }
        }
        return pos2;
    }

    private static int digTicks(ClientWorld world, ClientPlayerEntity player, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        float hardness = blockState.getHardness(world, pos);
        if (hardness < 0.0F) {
            return 2147483647;
        } else if (hardness == 0.0F) {
            return 1;
        } else {
            int toolSlot = toolSlot(player.getInventory(), blockState);
            ItemStack stack =
                    toolSlot < 0 ? ItemStack.EMPTY : player.getInventory().getStack(toolSlot);
            float miningSpeedMultiplier = stack.getMiningSpeedMultiplier(blockState);
            int i;
            if (miningSpeedMultiplier > 1.0F
                    && (i =
                                    player.getRegistryManager()
                                            .getOrThrow(RegistryKeys.ENCHANTMENT)
                                            .getOptional(Enchantments.EFFICIENCY)
                                            .map(
                                                    reference ->
                                                            EnchantmentHelper.getLevel(
                                                                    reference, stack))
                                            .orElse(0))
                            > 0) {
                miningSpeedMultiplier += i * i + 1;
            }
            boolean bl = stack.isSuitableFor(blockState) || !blockState.isToolRequired();
            return MathHelper.ceil(hardness * (bl ? 30.0F : 100.0F) / miningSpeedMultiplier);
        }
    }

    private static boolean nearLava(ClientWorld world, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (world.getFluidState(pos.offset(direction)).isIn(FluidTags.LAVA)) {
                return true;
            }
        }
        return false;
    }

    private void forgetDig() {
        this.digAnchor = null;
        this.digAnnounced = false;
        this.digPlaced = false;
        this.digBlasts = 0;
        this.digGuards = 0;
        this.digAfter = 0;
        this.digWalkUntil = 0;
        this.digUntil = 0;
        this.belowSince = -1;
    }

    private boolean startLaunch(
            ClientPlayerEntity player, PlayerEntity player2, RivalBook.Rival rival) {
        if (player.age - this.diggingAt <= 1) {
            return false;
        } else if (this.tick >= this.nextLaunch
                && !this.pause.slowServer()
                && (this.stance == CrystalBotModule.Stance.PRESS
                        || this.stance == CrystalBotModule.Stance.HOLD)
                && (rival == null || !rival.shrugsOffLaunches())) {
            if (player2.isOnGround()
                    && !Combo.inHole(player2)
                    && (!player2.isBlocking() || axeInHotbar(player))) {
                if (!this.mc
                        .world
                        .getNonSpectatingEntities(
                                EndCrystalEntity.class, player2.getBoundingBox().expand(3.0))
                        .isEmpty()) {
                    return false;
                } else {
                    double reach = reach(player, player2);
                    if (weaponSlot(player, player2.isBlocking()) >= 0 && !(reach > 3.9)) {
                        boolean bl =
                                MeleeAttacks.isMeleeWeapon(player.getMainHandStack())
                                        && MeleeAttacks.charge(player) >= 0.5F;
                        if (!bl) {
                            int i =
                                    rival != null
                                            ? rival.opener(reach)
                                            : (this.tick - this.hurtAt >= 20 ? 0 : 1);
                            if (this.openerArm < 0) {
                                this.judgeOpener(i, reach, player2);
                            }
                            if (i == 1) {
                                this.nextLaunch = this.tick + 40;
                                return false;
                            }
                        }
                        this.launching = true;
                        this.launchUntil = this.tick + 16;
                        return this.launch(player, player2);
                    } else {
                        return false;
                    }
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private void judgeOpener(int openerArm2, double openerReach2, PlayerEntity player) {
        this.openerArm = openerArm2;
        this.openerReach = openerReach2;
        this.openerAt = this.tick;
        this.openerHealth = CrystalDamage.health(player);
        this.openerPops = 0;
    }

    private boolean followUp(ClientWorld world, ClientPlayerEntity player, PlayerEntity player2) {
        AirTrack.Landing landing = this.air.landing(this.tick);
        Vec3d vec = landing != null ? landing.at() : player2.getEntityPos();
        if (this.anchorUnder(world, player, player2, vec)) {
            return true;
        } else {
            int hotbarSlot = BotBody.hotbarSlot(player, OBSIDIAN);
            if (hotbarSlot < 0) {
                this.followLaid = true;
                return false;
            } else {
                BlockPos pos = BlockPos.ofFloored(vec.x, vec.y + 0.2, vec.z);
                CrystalDamage.Victim victim =
                        new CrystalDamage.Victim(player2, vec.subtract(player2.getEntityPos()));
                CrystalDamage.Victim victim2 = new CrystalDamage.Victim(player, Vec3d.ZERO);
                Vec3d eyePos = player.getEyePos();
                AnchorGuard.Spot spot = null;
                float f = 4.0F;
                for (BlockPos pos2 : BlockPos.iterate(pos.add(-1, -1, -1), pos.add(1, 0, 1))) {
                    if (pos2.getX() != pos.getX() || pos2.getZ() != pos.getZ()) {
                        BlockState blockState = world.getBlockState(pos2);
                        Vec3d vec2 = Crystals.explosionAt(pos2);
                        if (Crystals.isBase(blockState) && Crystals.canHost(world, pos2, null)) {
                            if (victim.damage(vec2, false) >= f) {
                                this.followLaid = true;
                                return false;
                            }
                        } else {
                            BlockPos pos3;
                            if (blockState.isReplaceable()
                                    && world.isAir(pos2.up())
                                    && world.isAir(pos2.up(2))
                                    && world.getBlockState(pos3 = pos2.down())
                                            .isSideSolidFullSquare(world, pos3, Direction.UP)
                                    && world.canPlace(
                                            Blocks.OBSIDIAN.getDefaultState(),
                                            pos2,
                                            ShapeContext.absent())
                                    && !(eyePos.distanceTo(Vec3d.ofCenter(pos2))
                                            > player.getBlockInteractionRange())) {
                                float damage = victim.damage(vec2, true);
                                float damage2 = victim2.damage(vec2, true);
                                AnchorGuard.Spot spot2;
                                if (!(damage <= f)
                                        && !(damage2 > this.selfCapNow)
                                        && !CrystalDamage.endangers(player, damage2)
                                        && (spot2 =
                                                        AnchorGuard.supportFor(
                                                                world,
                                                                player,
                                                                eyePos,
                                                                pos2.toImmutable()))
                                                != null) {
                                    spot = spot2;
                                    f = damage;
                                }
                            }
                        }
                    }
                }
                this.body.face(this.mc, player, player2, 1.0F);
                if (spot == null) {
                    if (eyePos.distanceTo(vec) > 3.5) {
                        this.body.drive(this.mc, player, 1, 0.0, true, false);
                    }
                    return true;
                } else {
                    if (this.placeAt(world, player, spot, hotbarSlot) == 1) {
                        this.followLaid = true;
                    }
                    return true;
                }
            }
        }
    }

    private boolean anchorUnder(
            ClientWorld world, ClientPlayerEntity player, PlayerEntity player2, Vec3d vec) {
        if (this.anchors.get() && world.getRegistryKey() != World.NETHER && !player2.isOnGround()) {
            int hotbarSlot =
                    BotBody.hotbarSlot(player, itemStack -> itemStack.isOf(Items.RESPAWN_ANCHOR));
            if (hotbarSlot >= 0
                    && BotBody.hotbarSlot(player, itemStack -> itemStack.isOf(Items.GLOWSTONE))
                            >= 0) {
                BlockPos pos = BlockPos.ofFloored(vec.x, vec.y + 0.2, vec.z);
                BlockPos pos2 = pos.down();
                if (world.getBlockState(pos).isReplaceable()
                        && world.getBlockState(pos2)
                                .isSideSolidFullSquare(world, pos2, Direction.UP)) {
                    if (!player2.getBoundingBox().intersects(new Box(pos))
                            && !player.getBoundingBox().intersects(new Box(pos))) {
                        Vec3d eyePos = player.getEyePos();
                        if (eyePos.distanceTo(Vec3d.ofCenter(pos))
                                > player.getBlockInteractionRange()) {
                            return false;
                        } else {
                            Vec3d vec2 = Vec3d.ofCenter(pos);
                            float estimate = CrystalDamage.estimate(vec2, player, 5.0F, pos);
                            Vec3d vec3 = new Vec3d(vec.x, pos.getY() + 1.0, vec.z);
                            float damage =
                                    new CrystalDamage.Victim(
                                                    player2, vec3.subtract(player2.getEntityPos()))
                                            .damage(vec2, 5.0F, pos);
                            if (!(damage < 6.0F)
                                    && !(estimate > Math.min(this.selfCapNow, 11.0F))
                                    && !CrystalDamage.endangers(player, estimate)) {
                                AnchorGuard.Spot spot =
                                        AnchorGuard.supportFor(world, player, eyePos, pos);
                                if (spot == null) {
                                    return false;
                                } else {
                                    this.body.face(this.mc, player, player2, 1.0F);
                                    if (this.placeAt(world, player, spot, hotbarSlot) == 1) {
                                        this.followLaid = true;
                                    }
                                    return true;
                                }
                            } else {
                                return false;
                            }
                        }
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean launch(ClientPlayerEntity player, PlayerEntity player2) {
        if (this.tick > this.launchUntil
                || this.stance == CrystalBotModule.Stance.RUN
                || this.stance == CrystalBotModule.Stance.GUARD
                || reach(player, player2) > 4.1
                || Combo.inHole(player2)) {
            this.launching = false;
            this.nextLaunch = this.tick + 40;
            return false;
        } else if (!this.hands()) {
            return true;
        } else {
            int weaponSlot = weaponSlot(player, player2.isBlocking());
            if (weaponSlot < 0) {
                this.launching = false;
                return false;
            } else {
                if (player.getInventory().getSelectedSlot() != weaponSlot) {
                    player.getInventory().setSelectedSlot(weaponSlot);
                }
                this.body.face(this.mc, player, player2, 1.0F);
                float charge = MeleeAttacks.charge(player);
                double reach = reach(player, player2);
                Rotation rotation = ServerRotation.sent();
                boolean sprinting = player.isSprinting();
                if (charge >= 0.95F && reach <= 2.95 && this.strike(player, player2)) {
                    this.launching = false;
                    this.nextLaunch = this.tick + 80;
                    this.launchedAt = this.tick;
                    this.launchY = player2.getY();
                    this.launchPeak = 0.0;
                    this.followLaid = false;
                    this.followUntil = this.tick + 12;
                    this.air.kicked(
                            player,
                            player2,
                            rotation != null ? rotation.yaw() : player.getYaw(),
                            sprinting,
                            this.tick);
                    this.body.drive(this.mc, player, 0, 1.0, false, false);
                    return true;
                } else {
                    this.body.drive(
                            this.mc, player, reach > 2.9 ? 1 : 0, 0.0, charge >= 0.85F, false);
                    return true;
                }
            }
        }
    }

    private boolean strike(ClientPlayerEntity player, PlayerEntity player2) {
        if (!this.pause.slowServer()
                && !MeleeAttacks.actedThisTick(player)
                && this.mc.attackCooldown <= 0
                && MeleeAttacks.isMeleeWeapon(player.getMainHandStack())) {
            Rotation rotation = ServerRotation.sent();
            if (rotation == null) {
                return false;
            } else {
                Vec3d eyePos = player.getEyePos();
                double d = Math.min(2.95, player.getEntityInteractionRange());
                Box boundingBox = player2.getBoundingBox();
                Vec3d vec =
                        boundingBox.contains(eyePos)
                                ? eyePos
                                : boundingBox
                                        .raycast(
                                                eyePos,
                                                eyePos.add(
                                                        Vec3d.fromPolar(
                                                                        rotation.pitch(),
                                                                        rotation.yaw())
                                                                .multiply(d)))
                                        .orElse(null);
                if (vec != null && MeleeAttacks.lineIsClear(player, eyePos, vec)) {
                    if (!MeleeAttacks.attack(this.mc, new EntityHitResult(player2, vec))) {
                        return false;
                    } else {
                        MeleeAttacks.claimTick(this.mc);
                        return true;
                    }
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    private static double reach(ClientPlayerEntity player, PlayerEntity player2) {
        Vec3d eyePos = player.getEyePos();
        return eyePos.distanceTo(TargetingUtils.closestPoint(eyePos, player2.getBoundingBox()));
    }

    private static boolean axeInHotbar(ClientPlayerEntity player) {
        return BotBody.hotbarSlot(player, itemStack -> itemStack.isIn(ItemTags.AXES)) >= 0;
    }

    private static int weaponSlot(ClientPlayerEntity player, boolean bl) {
        PlayerInventory inventory = player.getInventory();
        int i = -1;
        double d = 0.0;
        for (int j = 0; j < 9; j++) {
            ItemStack stack = inventory.getStack(j);
            if (MeleeAttacks.isMeleeWeapon(stack)
                    && !stack.isOf(Items.MACE)
                    && (!bl || stack.isIn(ItemTags.AXES))) {
                double attackDamage = MeleeAttacks.attackDamage(stack);
                if (i < 0 || attackDamage > d) {
                    i = j;
                    d = attackDamage;
                }
            }
        }
        return i;
    }

    private boolean breakHole(
            ClientWorld world,
            ClientPlayerEntity player,
            PlayerEntity player2,
            RivalBook.Rival rival) {
        AutoCityModule module = ModuleRegistry.get(AutoCityModule.class);
        if (module == null) {
            return false;
        } else if (module.isEnabled()) {
            if (!this.cityStarted) {
                return false;
            } else {
                CombatMacroLock.release(this);
                this.body.face(this.mc, player, player2, 1.0F);
                return true;
            }
        } else {
            if (this.cityStarted) {
                this.cityStarted = false;
                this.breachAfter = this.tick + 100;
                this.progressAt = this.tick;
                if (Combo.inHole(player2)) {
                    this.cityFails++;
                }
            }
            if (this.stance != CrystalBotModule.Stance.RUN
                    && this.tick >= this.breachAfter
                    && Combo.inHole(player2)
                    && !(player.getHealth() + player.getAbsorptionAmount() < 8.0F)) {
                int i = rival != null && rival.campsInHoles() ? 15 : 40;
                if (this.tick - this.progressAt < i) {
                    return false;
                } else {
                    boolean bl =
                            BotBody.hotbarSlot(
                                            player, itemStack -> itemStack.isIn(ItemTags.PICKAXES))
                                    >= 0;
                    if (this.cityFails >= 2 && bl && this.roofOff(world, player, player2)) {
                        return true;
                    } else if (this.cityFails >= 2 || !bl) {
                        return this.camp(world, player, player2);
                    } else if (player.distanceTo(player2) > 3.6) {
                        this.body.face(this.mc, player, player2, 1.0F);
                        this.body.keepDistance(this.mc, player, player2, 1.6, 2.6, 3.4, false);
                        return true;
                    } else {
                        module.setEnabled(true);
                        if (!module.isEnabled()) {
                            return false;
                        } else {
                            this.cityStarted = true;
                            ModuleChat.send(
                                    this,
                                    ModuleChat.body()
                                            .text("Breaking ")
                                            .name(player2.getName().getString())
                                            .text("'s hole open."));
                            return true;
                        }
                    }
                }
            } else {
                return false;
            }
        }
    }

    private boolean roofOff(ClientWorld world, ClientPlayerEntity player, PlayerEntity player2) {
        if (this.anchors.get()
                && world.getRegistryKey() != World.NETHER
                && BotBody.hotbarSlot(player, itemStack -> itemStack.isOf(Items.RESPAWN_ANCHOR))
                        >= 0
                && BotBody.hotbarSlot(player, itemStack -> itemStack.isOf(Items.GLOWSTONE)) >= 0) {
            BlockPos pos = player2.getBlockPos().up(2);
            BlockState blockState = world.getBlockState(pos);
            if (blockState.isReplaceable() || blockState.getHardness(world, pos) < 0.0F) {
                return false;
            } else if (this.mine(world, player, pos)) {
                this.body.face(this.mc, player, player2, 1.0F);
                return true;
            } else {
                BlockPos pos2 = roofView(world, player, pos);
                if (pos2 == null) {
                    return false;
                } else {
                    this.body.face(this.mc, player, player2, 1.0F);
                    this.body.toward(this.mc, player, Vec3d.ofBottomCenter(pos2));
                    return true;
                }
            }
        } else {
            return false;
        }
    }

    private static boolean walkable(ClientWorld world, Vec3d vec, Vec3d vec2) {
        Vec3d vec3 = vec2.subtract(vec);
        int i = Math.max(1, (int) Math.ceil(vec3.horizontalLength() / 0.5));
        int j = MathHelper.floor(vec.y + 0.01);
        for (int k = 1; k <= i; k++) {
            Vec3d vec4 = vec.add(vec3.multiply((double) k / i));
            BlockPos pos = StandPicker.standable(world, BlockPos.ofFloored(vec4.x, j, vec4.z));
            if (pos == null || Math.abs(pos.getY() - j) > 1) {
                return false;
            }
            j = pos.getY();
        }
        return true;
    }

    private static BlockPos roofView(ClientWorld world, ClientPlayerEntity player, BlockPos pos) {
        BlockPos pos2 = player.getBlockPos();
        BlockPos pos3 = null;
        double d = 1.7976931348623157E308;
        double e = player.getBlockInteractionRange() - 0.2;
        for (BlockPos pos4 : BlockPos.iterate(pos2.add(-4, -1, -4), pos2.add(4, 1, 4))) {
            BlockPos pos5 = StandPicker.standable(world, pos4);
            if (pos5 != null
                    && pos5.equals(pos4)
                    && walkable(world, player.getEntityPos(), Vec3d.ofBottomCenter(pos5))) {
                Vec3d vec = Vec3d.ofBottomCenter(pos5).add(0.0, player.getStandingEyeHeight(), 0.0);
                double squaredDistance = pos5.getSquaredDistance(pos2);
                if (!(squaredDistance >= d)) {
                    for (Direction direction : Direction.values()) {
                        Vec3d vec2 = Vec3d.ofCenter(pos).offset(direction, 0.5);
                        BlockHitResult blockHitResult;
                        if (!(vec.distanceTo(vec2) > e)
                                && (blockHitResult =
                                                        world.raycast(
                                                                new RaycastContext(
                                                                        vec,
                                                                        vec2.offset(
                                                                                direction, -0.05),
                                                                        ShapeType.OUTLINE,
                                                                        FluidHandling.NONE,
                                                                        player)))
                                                .getType()
                                        == Type.BLOCK
                                && blockHitResult.getBlockPos().equals(pos)
                                && blockHitResult.getSide() == direction) {
                            pos3 = pos5.toImmutable();
                            d = squaredDistance;
                            break;
                        }
                    }
                }
            }
        }
        return pos3;
    }

    private boolean mine(ClientWorld world, ClientPlayerEntity player, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        int toolSlot = toolSlot(player.getInventory(), blockState);
        if (toolSlot < 0) {
            return false;
        } else {
            BlockHitResult blockHitResult = visibleFace(world, player, pos);
            if (blockHitResult == null) {
                return false;
            } else if (!this.hands()) {
                return true;
            } else {
                Rotation rotation = ServerRotation.sent();
                BlockHitResult blockHitResult2 =
                        rotation == null ? null : AnchorGuard.trace(world, player, rotation);
                if (blockHitResult2 == null || !blockHitResult2.getBlockPos().equals(pos)) {
                    ServerRotation.request(
                            RotationUtils.fromTo(player.getEyePos(), blockHitResult.getPos()),
                            6.0F);
                    return true;
                } else if (player.getInventory().getSelectedSlot() != toolSlot) {
                    player.getInventory().setSelectedSlot(toolSlot);
                    return true;
                } else {
                    ServerRotation.request(rotation);
                    if (MeleeAttacks.actedThisTick(player)) {
                        return true;
                    } else {
                        if (this.mc.interactionManager.updateBlockBreakingProgress(
                                pos, blockHitResult2.getSide())) {
                            player.swingHand(Hand.MAIN_HAND);
                        }
                        MeleeAttacks.noteAction(player);
                        this.diggingAt = player.age;
                        return true;
                    }
                }
            }
        }
    }

    public static boolean digging() {
        CrystalBotModule module = ModuleRegistry.get(CrystalBotModule.class);
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        return module != null
                && module.isEnabled()
                && player != null
                && player.age - module.diggingAt <= 1;
    }

    private static int toolSlot(PlayerInventory playerInventory, BlockState state) {
        int i = -1;
        float f = 0.0F;
        for (int j = 0; j < 9; j++) {
            ItemStack stack = playerInventory.getStack(j);
            boolean bl = stack.isSuitableFor(state) || !state.isToolRequired();
            if (bl && !(stack.getMiningSpeedMultiplier(state) <= f)) {
                i = j;
                f = stack.getMiningSpeedMultiplier(state);
            }
        }
        return i;
    }

    private static BlockHitResult visibleFace(
            ClientWorld world, ClientPlayerEntity player, BlockPos pos) {
        Vec3d eyePos = player.getEyePos();
        BlockHitResult blockHitResult = null;
        double d = player.getBlockInteractionRange() * player.getBlockInteractionRange();
        for (Direction direction : Direction.values()) {
            Vec3d vec = Vec3d.ofCenter(pos).offset(direction, 0.5);
            double squaredDistance = eyePos.squaredDistanceTo(vec);
            BlockHitResult blockHitResult2;
            if (!(squaredDistance > d)
                    && (blockHitResult2 =
                                    AnchorGuard.trace(
                                            world,
                                            player,
                                            RotationUtils.fromTo(
                                                    eyePos, vec.offset(direction, -0.05))))
                            != null
                    && blockHitResult2.getBlockPos().equals(pos)
                    && blockHitResult2.getSide() == direction) {
                blockHitResult = blockHitResult2;
                d = squaredDistance;
            }
        }
        return blockHitResult;
    }

    private boolean camp(ClientWorld world, ClientPlayerEntity player, PlayerEntity player2) {
        BlockPos pos = player2.getBlockPos();
        if (world.getNonSpectatingEntities(
                                EndCrystalEntity.class, new Box(pos).expand(3.0, 1.0, 3.0))
                        .size()
                >= 2) {
            return false;
        } else {
            Vec3d eyePos = player.getEyePos();
            BlockPos pos2 = null;
            float f = this.selfCapNow - 2.0F;
            double d = player.getEntityInteractionRange() - 0.1;
            for (BlockPos pos3 : BlockPos.iterate(pos.add(-2, 0, -2), pos.add(2, 0, 2))) {
                float estimate;
                BlockPos pos4;
                if (Math.max(Math.abs(pos3.getX() - pos.getX()), Math.abs(pos3.getZ() - pos.getZ()))
                                == 2
                        && (pos4 = pos3.down().toImmutable()).getY() >= feetY(player)
                        && Crystals.isBase(world.getBlockState(pos4))
                        && Crystals.canHost(world, pos4, null)
                        && !(eyePos.squaredDistanceTo(
                                        TargetingUtils.closestPoint(eyePos, Crystals.boxOn(pos4)))
                                > d * d)
                        && !((estimate = CrystalDamage.estimate(Crystals.explosionAt(pos4), player))
                                > f)
                        && !CrystalDamage.endangers(player, estimate)
                        && topAim(world, player, pos4) != null) {
                    pos2 = pos4;
                    f = estimate;
                }
            }
            if (pos2 == null) {
                return false;
            } else {
                this.body.face(this.mc, player, player2, 1.0F);
                this.placeCrystal(world, player, pos2);
                return true;
            }
        }
    }

    private static Vec3d topAim(ClientWorld world, ClientPlayerEntity player, BlockPos pos) {
        Vec3d eyePos = player.getEyePos();
        for (double[] doubles :
                new double[][] {{0.5, 0.5}, {0.2, 0.2}, {0.8, 0.2}, {0.2, 0.8}, {0.8, 0.8}}) {
            Vec3d vec =
                    new Vec3d(pos.getX() + doubles[0], pos.getY() + 1.0, pos.getZ() + doubles[1]);
            BlockHitResult blockHitResult;
            if (!(eyePos.distanceTo(vec) > player.getBlockInteractionRange() - 0.1)
                    && (blockHitResult =
                                    AnchorGuard.trace(
                                            world, player, RotationUtils.fromTo(eyePos, vec)))
                            != null
                    && blockHitResult.getBlockPos().equals(pos)
                    && blockHitResult.getSide() == Direction.UP) {
                return vec;
            }
        }
        return null;
    }

    private void placeCrystal(ClientWorld world, ClientPlayerEntity player, BlockPos pos) {
        int hotbarSlot = BotBody.hotbarSlot(player, itemStack -> itemStack.isOf(Items.END_CRYSTAL));
        if (hotbarSlot >= 0 && this.hands()) {
            Rotation rotation = ServerRotation.sent();
            BlockHitResult blockHitResult =
                    rotation == null ? null : AnchorGuard.trace(world, player, rotation);
            if (blockHitResult == null
                    || !blockHitResult.getBlockPos().equals(pos)
                    || blockHitResult.getSide() != Direction.UP) {
                Vec3d vec = topAim(world, player, pos);
                if (vec != null) {
                    ServerRotation.request(RotationUtils.fromTo(player.getEyePos(), vec), 6.0F);
                }
            } else if (player.getInventory().getSelectedSlot() != hotbarSlot) {
                player.getInventory().setSelectedSlot(hotbarSlot);
            } else if (MeleeAttacks.handsFree(this.mc, player)) {
                if (Crystals.rightClick(this.mc, player, blockHitResult)) {
                    Crystals.notePlace(player, pos);
                    MeleeAttacks.claimTick(this.mc);
                }
                ServerRotation.request(rotation);
            }
        }
    }

    private boolean closeSpot(ClientWorld world, ClientPlayerEntity player, PlayerEntity player2) {
        int guardBlockSlot = guardBlockSlot(player);
        if (guardBlockSlot < 0) {
            return false;
        } else {
            if (this.blocking == null && this.tick >= this.spotScanAt) {
                this.spotScanAt = this.tick + 5;
                this.blocking = dangerSpot(world, player, player2);
            }
            if (this.blocking == null) {
                return false;
            } else if (world.getBlockState(this.blocking).isReplaceable()
                    && world.canPlace(
                            Blocks.OBSIDIAN.getDefaultState(),
                            this.blocking,
                            ShapeContext.absent())) {
                AnchorGuard.Spot spot =
                        AnchorGuard.supportFor(world, player, player.getEyePos(), this.blocking);
                if (spot == null) {
                    this.blocking = null;
                    return false;
                } else {
                    if (this.placeAt(world, player, spot, guardBlockSlot) == 1) {
                        this.blocking = null;
                    }
                    return true;
                }
            } else {
                this.blocking = null;
                return false;
            }
        }
    }

    private static BlockPos dangerSpot(
            ClientWorld world, ClientPlayerEntity player, PlayerEntity player2) {
        BlockPos pos = player.getBlockPos();
        Vec3d eyePos = player2.getEyePos();
        double d = Math.max(4.6, player.getBlockInteractionRange() + 0.1);
        BlockPos pos2 = null;
        float f = 0.0F;
        float g = 0.0F;
        for (BlockPos pos3 : BlockPos.iterate(pos.add(-4, -2, -4), pos.add(4, 1, 4))) {
            Vec3d vec;
            float estimate;
            if (Crystals.isBase(world.getBlockState(pos3))
                    && Crystals.canHost(world, pos3, null)
                    && !(eyePos.distanceTo(
                                    new Vec3d(
                                            pos3.getX() + 0.5,
                                            pos3.getY() + 1.0,
                                            pos3.getZ() + 0.5))
                            > d)
                    && !((estimate =
                                    CrystalDamage.estimate(
                                            vec = Crystals.explosionAt(pos3), player))
                            <= g)
                    && !(CrystalDamage.estimate(vec, player2) >= estimate + 2.0F)) {
                if (estimate > f) {
                    g = f;
                    f = estimate;
                    pos2 = pos3.up().toImmutable();
                } else {
                    g = estimate;
                }
            }
        }
        return f >= 8.0F && g <= f / 2.0F ? pos2 : null;
    }

    private int placeAt(
            ClientWorld world, ClientPlayerEntity player, AnchorGuard.Spot spot, int selectedSlot) {
        if (!this.hands()) {
            return 0;
        } else {
            Rotation rotation = ServerRotation.sent();
            BlockHitResult blockHitResult =
                    rotation == null ? null : AnchorGuard.hitAlong(world, player, rotation, spot);
            if (blockHitResult == null) {
                ServerRotation.request(RotationUtils.fromTo(player.getEyePos(), spot.aim()), 6.0F);
                return 0;
            } else if (player.getInventory().getSelectedSlot() != selectedSlot) {
                player.getInventory().setSelectedSlot(selectedSlot);
                return 0;
            } else if (!MeleeAttacks.handsFree(this.mc, player)) {
                return 0;
            } else {
                AnchorGuard.click(this.mc, player, blockHitResult);
                ServerRotation.request(rotation);
                return 1;
            }
        }
    }

    private boolean defuse(ClientWorld world, ClientPlayerEntity player) {
        if (this.tick - this.defuseAt >= 10 && world.getRegistryKey() != World.NETHER) {
            BlockPos pos = player.getBlockPos();
            for (BlockPos pos2 : BlockPos.iterate(pos.add(-5, -2, -5), pos.add(5, 3, 5))) {
                BlockState blockState = world.getBlockState(pos2);
                BlockPos pos3;
                Vec3d vec;
                if (blockState.isOf(Blocks.RESPAWN_ANCHOR)
                        && blockState.get(RespawnAnchorBlock.CHARGES) > 0
                        && player.canInteractWithBlockAt(pos2, -0.1)
                        && !pos2.equals(this.digAnchor)
                        && !(CrystalDamage.estimate(
                                        vec = Vec3d.ofCenter(pos3 = pos2.toImmutable()),
                                        player,
                                        5.0F,
                                        pos3)
                                > Math.max(1.0F, 0.15F * player.getHealth()))) {
                    boolean bl = false;
                    for (PlayerEntity player2 : enemies(world, player, 16.0)) {
                        bl |= player2.getEyePos().distanceTo(vec) <= 5.5;
                    }
                    if (bl) {
                        PlayerInventory inventory = player.getInventory();
                        int detonatorRoute =
                                AnchorGuard.detonatorRoute(inventory, player.getMainHandStack());
                        if (detonatorRoute == -1 || !this.hands()) {
                            return false;
                        }
                        BlockHitResult blockHitResult = AnchorGuard.anchorHit(world, player, pos3);
                        if (blockHitResult != null) {
                            if (detonatorRoute != -2) {
                                inventory.setSelectedSlot(detonatorRoute);
                                return true;
                            }
                            if (!MeleeAttacks.handsFree(this.mc, player)) {
                                return true;
                            }
                            AnchorGuard.click(this.mc, player, blockHitResult);
                            this.defuseAt = this.tick;
                            return true;
                        }
                        Vec3d vec2 = AnchorGuard.anchorAim(world, player, pos3);
                        if (vec2 != null) {
                            ServerRotation.request(
                                    RotationUtils.fromTo(player.getEyePos(), vec2), 6.0F);
                            return true;
                        }
                    }
                }
            }
            return false;
        } else {
            return false;
        }
    }

    private boolean guard(ClientWorld world, ClientPlayerEntity player, PlayerEntity player2) {
        float f = player.getHealth() + player.getAbsorptionAmount();
        BlockPos pos = null;
        float g = 0.0F;
        BlockPos pos2 = player.getBlockPos();
        for (BlockPos pos3 : BlockPos.iterate(pos2.add(-5, -2, -5), pos2.add(5, 3, 5))) {
            Vec3d vec;
            float estimate;
            if (world.getBlockState(pos3).isOf(Blocks.RESPAWN_ANCHOR)
                    && !pos3.equals(this.digAnchor)
                    && (!((estimate =
                                            CrystalDamage.estimate(
                                                    vec = Vec3d.ofCenter(pos3),
                                                    player,
                                                    5.0F,
                                                    pos3.toImmutable()))
                                    < Math.max(4.0F, f * 0.3F))
                            || !(estimate < f - 1.0F))
                    && !(player2.getEyePos().distanceTo(vec) > 5.5)
                    && !(estimate <= g)) {
                g = estimate;
                pos = pos3.toImmutable();
            }
        }
        if (pos == null) {
            return false;
        } else {
            AnchorGuard.Spot spot = AnchorGuard.find(world, player, pos, player.getEyePos());
            if (spot != null && spot.standing()) {
                return false;
            } else {
                int guardBlockSlot = guardBlockSlot(player);
                if (spot != null && guardBlockSlot >= 0 && this.tick - this.shieldAt >= 20) {
                    this.body.face(this.mc, player, player2, 1.0F);
                    if (this.placeAt(world, player, spot, guardBlockSlot) == 1) {
                        this.shieldAt = this.tick;
                    }
                    return true;
                } else {
                    this.body.face(this.mc, player, player2, 1.0F);
                    this.body.away(this.mc, player, Vec3d.ofCenter(pos));
                    return true;
                }
            }
        }
    }

    private static int guardBlockSlot(ClientPlayerEntity player) {
        int hotbarSlot = BotBody.hotbarSlot(player, OBSIDIAN);
        return hotbarSlot >= 0 ? hotbarSlot : BotBody.hotbarSlot(player, AnchorGuard::isGuardBlock);
    }

    private boolean cover(
            ClientWorld world, ClientPlayerEntity player, PlayerEntity player2, double d) {
        boolean bl =
                this.boxingAt != null
                        && this.boxingAt.equals(player.getBlockPos())
                        && this.tick < this.boxingUntil;
        boolean bl2 =
                player.getHealth() + player.getAbsorptionAmount() <= this.retreatAt() + 4.0F
                        || this.stance == CrystalBotModule.Stance.GUARD;
        if (this.surround.get() && !bl && bl2 && player.isOnGround()) {
            this.boxingAt = player.getBlockPos();
            this.boxingUntil = this.tick + 60;
            bl = true;
        }
        if (!bl) {
            this.boxingAt = null;
        }
        return this.surround.get() && bl && player.isOnGround() && this.boxIn(world, player);
    }

    private boolean boxIn(ClientWorld world, ClientPlayerEntity player) {
        List list = openAroundFeet(world, player);
        if (!list.isEmpty() && BotBody.hotbarSlot(player, OBSIDIAN) >= 0) {
            if (!this.hands()) {
                return true;
            } else {
                for (BlockPos pos : (Iterable<BlockPos>) list) {
                    BlockHitResult blockHitResult = AutoTrapModule.support(world, player, pos);
                    if (blockHitResult != null
                            && (!pos.equals(this.boxSkipped)
                                    || this.tick >= this.boxSkippedUntil)) {
                        BlockHitResult blockHitResult2 =
                                AutoTrapModule.seenByServer(world, player, blockHitResult);
                        if (blockHitResult2 != null) {
                            int hotbarSlot = BotBody.hotbarSlot(player, OBSIDIAN);
                            if (player.getInventory().getSelectedSlot() != hotbarSlot) {
                                player.getInventory().setSelectedSlot(hotbarSlot);
                                return true;
                            }
                            if (!MeleeAttacks.handsFree(this.mc, player)) {
                                return true;
                            }
                            ActionResult actionResult =
                                    this.mc.interactionManager.interactBlock(
                                            player, Hand.MAIN_HAND, blockHitResult2);
                            if (actionResult.isAccepted()) {
                                player.swingHand(Hand.MAIN_HAND);
                                MeleeAttacks.noteAction(player);
                                ServerRotation.request(ServerRotation.sent());
                            }
                            return true;
                        }
                        this.boxAimTicks = pos.equals(this.boxAiming) ? this.boxAimTicks + 1 : 0;
                        this.boxAiming = pos;
                        if (this.boxAimTicks < 10) {
                            ServerRotation.request(
                                    RotationUtils.fromTo(
                                            player.getEyePos(), blockHitResult.getPos()),
                                    6.0F);
                            return true;
                        }
                        this.boxSkipped = pos;
                        this.boxSkippedUntil = this.tick + 40;
                    }
                }
                return false;
            }
        } else {
            return false;
        }
    }

    private static List<BlockPos> openAroundFeet(ClientWorld world, ClientPlayerEntity player) {
        ArrayList list = new ArrayList();
        for (BlockPos pos : ring(player)) {
            BlockState blockState = world.getBlockState(pos);
            if (blockState.isReplaceable()
                    && !(pos.toCenterPos().distanceTo(player.getEyePos())
                            > player.getBlockInteractionRange())
                    && world.canPlace(
                            Blocks.OBSIDIAN.getDefaultState(), pos, ShapeContext.absent())) {
                list.add(pos);
            }
        }
        return list;
    }

    private static boolean surrounded(ClientWorld world, ClientPlayerEntity player) {
        for (BlockPos pos : ring(player)) {
            if (world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static Set<BlockPos> ring(ClientPlayerEntity player) {
        Box boundingBox = player.getBoundingBox();
        int i = MathHelper.floor(boundingBox.minY + 0.01);
        LinkedHashSet set = new LinkedHashSet();
        for (int j = MathHelper.floor(boundingBox.minX);
                j <= MathHelper.floor(boundingBox.maxX - 1.0E-4);
                j++) {
            for (int k = MathHelper.floor(boundingBox.minZ);
                    k <= MathHelper.floor(boundingBox.maxZ - 1.0E-4);
                    k++) {
                set.add(BlockPos.asLong(j, 0, k));
            }
        }
        LinkedHashSet set2 = new LinkedHashSet();
        for (long l : (Iterable<Long>) set) {
            BlockPos pos = BlockPos.fromLong(l).withY(i);
            for (Direction direction : net.minecraft.util.math.Direction.Type.HORIZONTAL) {
                BlockPos pos2 = pos.offset(direction);
                if (!set.contains(BlockPos.asLong(pos2.getX(), 0, pos2.getZ()))) {
                    set2.add(pos2);
                }
            }
        }
        return set2;
    }

    private static boolean armorLow(LivingEntity entity) {
        for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES) {
            ItemStack equippedStack;
            if (equipmentSlot.isArmorSlot()
                    && (equippedStack = entity.getEquippedStack(equipmentSlot)).isDamageable()
                    && equippedStack.getMaxDamage() - equippedStack.getDamage()
                            < equippedStack.getMaxDamage() * 0.3F) {
                return true;
            }
        }
        return false;
    }

    private boolean throwXp(ClientPlayerEntity player) {
        if (BotBody.hotbarSlot(player, XP) < 0) {
            return false;
        } else {
            this.mending = true;
            Aim.turn(this.mc, player.getYaw(), 86.0F, 22.0F);
            if (player.getPitch() > 70.0F) {
                this.body.use(this.mc, player, XP);
            }
            return true;
        }
    }

    private boolean mendIdle(ClientPlayerEntity player) {
        if (this.mendArmor.get() && (armorLow(player) || this.mending) && hasDamagedArmor(player)) {
            CombatMacroLock.take(this);
            return this.throwXp(player);
        } else {
            return false;
        }
    }

    private static boolean hasDamagedArmor(ClientPlayerEntity player) {
        for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES) {
            if (equipmentSlot.isArmorSlot() && player.getEquippedStack(equipmentSlot).isDamaged()) {
                return true;
            }
        }
        return false;
    }

    private static int count(ClientPlayerEntity player, Predicate<ItemStack> predicate) {
        int i = 0;
        PlayerInventory inventory = player.getInventory();
        for (int j = 0; j < 36; j++) {
            if (predicate.test(inventory.getStack(j))) {
                i += inventory.getStack(j).getCount();
            }
        }
        return i
                + (predicate.test(player.getOffHandStack())
                        ? player.getOffHandStack().getCount()
                        : 0);
    }

    @Environment(EnvType.CLIENT)
    private static enum Stance {
        PRESS(3.5F, 10.0F, 5.0F, 0.7),
        HOLD(4.5F, 8.0F, 6.0F, 1.0),
        GUARD(6.0F, 5.0F, 7.0F, 1.6),
        RUN(99.0F, 3.0F, 99.0F, 2.0);

        final float minDamage;

        final float selfCap;

        final float anchorMin;

        final double cover;

        private Stance(float minDamage2, float selfCap2, float anchorMin2, double cover2) {
            this.minDamage = minDamage2;
            this.selfCap = selfCap2;
            this.anchorMin = anchorMin2;
            this.cover = cover2;
        }
    }

    @Environment(EnvType.CLIENT)
    private static enum Throw {
        ESCAPE,
        CHASE;

        private Throw() {}
    }
}
