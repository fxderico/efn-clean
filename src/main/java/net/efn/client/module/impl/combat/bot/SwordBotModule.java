package net.efn.client.module.impl.combat.bot;

import net.efn.client.chat.ModuleChat;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.CombatMacroLock;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.pvp.InputControl;
import net.efn.client.util.pvp.Rotation;
import net.efn.client.util.pvp.ServerRotation;
import net.efn.client.util.pvp.TargetingUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
public final class SwordBotModule extends ClientModule {

    private static final String PATIENT = "Patient";

    private static final String AGGRESSIVE = "Aggressive";

    private static final Predicate<ItemStack> GAPPLE =
            itemStack ->
                    itemStack.isOf(Items.GOLDEN_APPLE)
                            || itemStack.isOf(Items.ENCHANTED_GOLDEN_APPLE);

    private static final double HIT_RANGE = 3.0;

    private static final double CHASE_RANGE = 6.0;

    private static final int RESET_WINDOW = 3;

    private final ModeSetting style =
            this.addSetting("style", new ModeSetting("Style", "Patient", "Patient", "Aggressive"));

    private final NumberSetting range =
            this.addSetting(
                    "range", new NumberSetting("Fight Range", 16.0F, 6.0F, 32.0F).step(1.0F));

    private final NumberSetting turnSpeed =
            this.addSetting(
                    "turn-speed", new NumberSetting("Turn Speed", 1.0F, 0.5F, 1.5F).step(0.05F));

    private final BooleanSetting strafe =
            this.addSetting("strafe", new BooleanSetting("Strafe", true));

    private final BooleanSetting crits =
            this.addSetting("crits", new BooleanSetting("Jump Crits", true));

    private final BooleanSetting jumpReset =
            this.addSetting("jump-reset", new BooleanSetting("Jump Reset", true));

    private final NumberSetting eatBelow =
            this.addSetting(
                    "eat-below",
                    new NumberSetting("Gapple Below HP", 10.0F, 0.0F, 20.0F).step(1.0F));

    private final MinecraftClient mc = MinecraftClient.getInstance();

    private final BotBody body = new BotBody();

    private PlayerEntity fought;

    private double closing;

    private double lastTheirReach = 0.0 / 0.0;

    private double jitter;

    private int tapTicks;

    private boolean tapBack;

    private boolean critJump;

    private int landedTicks;

    private int resetUntil;

    private int lastHurt;

    private boolean eating;

    public SwordBotModule() {
        super(
                "sword-bot",
                "Sword PvP Bot",
                "Fights the nearest enemy player with a sword or axe: keeps out of their reach"
                        + " while charging, times crits, strafes and eats gapples.",
                ModuleCategory.COMBAT);
    }

    @Override
    protected void onEnable() {
        this.body.forget();
        this.newFight(null);
        this.eating = false;
        this.on(TickEvent.TYPE, Stage.PRE, tickEvent -> this.tick());
    }

    @Override
    protected void onDisable() {
        this.body.release(this.mc);
        this.body.forget();
        CombatMacroLock.release(this);
    }

    private void newFight(PlayerEntity fought2) {
        this.fought = fought2;
        this.closing = 0.0;
        this.lastTheirReach = 0.0 / 0.0;
        this.jitter = randomJitter();
        this.tapTicks = 0;
        this.critJump = false;
        this.resetUntil = 0;
    }

    private void tick() {
        ClientPlayerEntity player = this.mc.player;
        this.body.release(this.mc);
        if (player != null
                && this.mc.world != null
                && MeleeAttacks.canAct(this.mc)
                && !player.isDead()) {
            PlayerEntity player2 = this.body.pick(this.mc, player, this.range.get());
            int tick = this.body.tick();
            if (player2 != this.fought) {
                if (player2 != null) {
                    ModuleChat.send(
                            this,
                            ModuleChat.body()
                                    .text("Fighting ")
                                    .name(player2.getName().getString())
                                    .text("."));
                } else if (this.fought != null) {
                    ModuleChat.send(this, ModuleChat.body().text("No enemy in range, waiting."));
                }
                this.newFight(player2);
            }
            if (player2 != null) {
                this.body.face(this.mc, player, player2, this.turnSpeed.get());
                boolean bl =
                        player.getHealth() <= this.eatBelow.get()
                                && player.getAbsorptionAmount() < 4.0F;
                if ((bl || this.eating && player.isUsingItem())
                        && this.body.use(this.mc, player, GAPPLE)) {
                    this.eating = true;
                    this.body.backOff(this.mc, player);
                } else {
                    this.eating = false;
                    if (!player.isUsingItem()) {
                        holdWeapon(player);
                        Vec3d eyePos = player.getEyePos();
                        double distance =
                                eyePos.distanceTo(
                                        TargetingUtils.closestPoint(
                                                eyePos, player2.getBoundingBox()));
                        Vec3d eyePos2 = player2.getEyePos();
                        double distance2 =
                                eyePos2.distanceTo(
                                        TargetingUtils.closestPoint(
                                                eyePos2, player.getBoundingBox()));
                        if (!Double.isNaN(this.lastTheirReach)) {
                            this.closing =
                                    this.closing * 0.5 + (this.lastTheirReach - distance2) * 0.5;
                        }
                        this.lastTheirReach = distance2;
                        int ticksToFull = MeleeAttacks.ticksToFull(player);
                        float charge = MeleeAttacks.charge(player);
                        double facingAngle = facingAngle(player2, player);
                        Vec3d vec =
                                new Vec3d(
                                                player2.getX() - player.getX(),
                                                0.0,
                                                player2.getZ() - player.getZ())
                                        .normalize();
                        Vec3d vec2 =
                                player2.getEntityPos()
                                        .subtract(player2.lastX, player2.lastY, player2.lastZ);
                        double d = vec2.x * vec.x + vec2.z * vec.z;
                        boolean bl2 = player2.hurtTime > 1;
                        boolean blocking = player2.isBlocking();
                        boolean bl3 = distance2 <= 3.15 && facingAngle <= 40.0;
                        boolean bl4 = distance <= 3.0;
                        double e =
                                3.35
                                        + 0.12
                                                * MathHelper.clamp(
                                                        Math.round(ping(player) / 50.0F), 0, 3)
                                        + 2.5 * MathHelper.clamp(this.closing, 0.0, 0.35)
                                        - (this.style.is("Aggressive") ? 0.2 : 0.0);
                        double d2 =
                                ticksToFull <= 0
                                        ? 2.9
                                        : (ticksToFull <= 2
                                                ? 3.08 + 0.1 * (ticksToFull - 1) + this.jitter
                                                : Math.max(
                                                        e + this.jitter, 2.9 + 0.26 * ticksToFull));
                        if (facingAngle < 8.0 && distance2 < 4.2) {
                            this.body.flipSide();
                        }
                        double e2 = this.strafe.get() ? 1.0 : 0.0;
                        if (player.hurtTime > this.lastHurt && player.hurtTime >= 8) {
                            this.resetUntil = tick + 3;
                        }
                        this.lastHurt = player.hurtTime;
                        if (this.tapTicks > 0) {
                            this.tapTicks--;
                            this.body.drive(
                                    this.mc,
                                    player,
                                    this.tapBack ? -1 : 0,
                                    e2 * (this.tapBack ? 0.8 : 1.0),
                                    false,
                                    false);
                        } else if ((!(distance > 6.0) || !(distance > d2 + 1.0))
                                && !(Math.abs(player2.getY() - player.getY()) > 2.5)) {
                            if (this.critJump) {
                                if (player.isOnGround()) {
                                    if (++this.landedTicks >= 2) {
                                        this.critJump = false;
                                    }
                                } else {
                                    this.landedTicks = 0;
                                }
                                if (this.critJump) {
                                    boolean bl5 = ticksToFull <= 1 && player.getVelocity().y < 0.1;
                                    this.body.drive(
                                            this.mc,
                                            player,
                                            bl5 ? 0 : 1,
                                            bl5 ? e2 * 0.35 : 0.0,
                                            !bl5,
                                            false);
                                    if (ticksToFull <= 0 && bl4 && !bl2) {
                                        InputControl.dropSprint();
                                        this.strike(player, player2, 0.95F, true);
                                    }
                                    return;
                                }
                            }
                            if (this.crits.get()
                                    && player.isOnGround()
                                    && !player.isTouchingWater()
                                    && !bl3
                                    && this.critWindow(ticksToFull, distance, facingAngle, d)) {
                                this.critJump = true;
                                this.landedTicks = 0;
                                this.body.drive(this.mc, player, 1, 0.0, true, true);
                            } else if (ticksToFull <= 0 && !blocking) {
                                if (bl2) {
                                    this.body.drive(
                                            this.mc,
                                            player,
                                            distance > 3.12 ? 1 : (distance < 2.88 ? -1 : 0),
                                            e2,
                                            false,
                                            false);
                                } else {
                                    this.body.drive(
                                            this.mc,
                                            player,
                                            distance < 1.6 ? -1 : 1,
                                            distance <= 3.5 ? e2 * 0.35 : 0.0,
                                            distance >= 1.6,
                                            false);
                                    if (bl4) {
                                        this.strike(player, player2, 0.95F, false);
                                    }
                                }
                            } else {
                                if (blocking) {
                                    d2 = Math.max(d2, e);
                                }
                                int i =
                                        distance2 > d2 + 0.12
                                                ? 1
                                                : (distance2 < d2 - 0.12 ? -1 : 0);
                                if (i < 0 && d > 0.15) {
                                    i = 0;
                                }
                                boolean bl6 = bl3 && bl4 && charge >= 0.6F && !bl2 && !blocking;
                                boolean bl7 =
                                        this.jumpReset.get()
                                                && tick < this.resetUntil
                                                && player.isOnGround()
                                                && !player.isTouchingWater();
                                if (bl7) {
                                    this.body.drive(this.mc, player, 1, 0.0, true, true);
                                } else if (ticksToFull <= 2
                                        && !bl6
                                        && this.strafe.get()
                                        && !blocking) {
                                    this.body.drive(this.mc, player, 1, 1.0, true, false);
                                } else {
                                    double d3 =
                                            !this.strafe.get()
                                                    ? 0.0
                                                    : (i == 0 ? 1.0 : (i < 0 ? 0.8 : 0.5));
                                    this.body.drive(this.mc, player, i, d3, i > 0, false);
                                }
                                if (bl6) {
                                    this.strike(player, player2, 0.6F, false);
                                }
                            }
                        } else {
                            this.body.drive(this.mc, player, 1, 0.0, true, false);
                            if (bl4 && ticksToFull <= 0 && !bl2 && !blocking) {
                                this.strike(player, player2, 0.95F, false);
                            }
                        }
                    }
                }
            }
        } else {
            this.body.forget();
        }
    }

    private boolean critWindow(int i, double d, double e, double d2) {
        if (i >= 7 && i <= 10 && d >= 2.3 && d <= 5.0) {
            return true;
        } else {
            return i >= 3 && i <= 10 && !(d < 2.3) && !(d > 4.2)
                    ? e >= 100.0 && d2 > 0.05 || this.style.is("Aggressive") && e > 60.0
                    : false;
        }
    }

    private void strike(ClientPlayerEntity player, PlayerEntity player2, float f, boolean bl) {
        if (!CombatMacroLock.heldByOther(this)
                && !MeleeAttacks.actedThisTick(player)
                && this.mc.attackCooldown <= 0) {
            if (!(MeleeAttacks.charge(player) < f)
                    && MeleeAttacks.isMeleeWeapon(player.getMainHandStack())) {
                if (!bl || MeleeAttacks.airborne(player) && !(player.getVelocity().y >= 0.0)) {
                    Rotation rotation = ServerRotation.sent();
                    if (rotation != null) {
                        Vec3d eyePos = player.getEyePos();
                        double d = Math.min(3.0, player.getEntityInteractionRange());
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
                            if (MeleeAttacks.attack(this.mc, new EntityHitResult(player2, vec))) {
                                MeleeAttacks.claimTick(this.mc);
                                this.jitter = randomJitter();
                                this.tapTicks = 1;
                                boolean bl2 =
                                        this.tapBack =
                                                player.getEyePos()
                                                                .distanceTo(
                                                                        TargetingUtils.closestPoint(
                                                                                eyePos,
                                                                                boundingBox))
                                                        < 2.3;
                                if (this.tapBack) {
                                    this.tapTicks = 2;
                                }
                                this.critJump = false;
                            }
                        }
                    }
                }
            }
        }
    }

    private static double facingAngle(PlayerEntity player, ClientPlayerEntity player2) {
        Vec3d vec = Vec3d.fromPolar(player.getPitch(), player.getHeadYaw());
        Vec3d vec2 =
                player2.getEntityPos().add(0.0, 1.1, 0.0).subtract(player.getEyePos()).normalize();
        return Math.toDegrees(Math.acos(MathHelper.clamp(vec.dotProduct(vec2), -1.0, 1.0)));
    }

    private static int ping(ClientPlayerEntity player) {
        PlayerListEntry playerListEntry =
                player.networkHandler.getPlayerListEntry(player.getUuid());
        return playerListEntry == null ? 0 : Math.max(0, playerListEntry.getLatency());
    }

    private static double randomJitter() {
        return ThreadLocalRandom.current().nextDouble(-0.06, 0.06);
    }

    private static void holdWeapon(ClientPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        if (!MeleeAttacks.isMeleeWeapon(player.getMainHandStack())) {
            int i = -1;
            double d = 0.0;
            for (int j = 0; j < 9; j++) {
                ItemStack stack = inventory.getStack(j);
                if (MeleeAttacks.isMeleeWeapon(stack)) {
                    double attackDamage = MeleeAttacks.attackDamage(stack);
                    if (i < 0 || attackDamage > d) {
                        i = j;
                        d = attackDamage;
                    }
                }
            }
            if (i >= 0) {
                inventory.setSelectedSlot(i);
            }
        }
    }
}
