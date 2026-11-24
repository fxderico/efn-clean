package net.efn.client.module.impl.combat;

import net.efn.client.event.EventHandler;
import net.efn.client.event.EventPriority;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.CombatMacroLock;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.combat.crystal.CrystalDamage;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.util.pvp.InputControl;
import net.efn.client.util.pvp.Rotation;
import net.efn.client.util.pvp.RotationUtils;
import net.efn.client.util.pvp.ServerRotation;
import net.efn.client.util.pvp.TargetingUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttackRangeComponent;
import net.minecraft.component.type.PiercingWeaponComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class KillAuraModule extends ClientModule {

    private static final String PRIORITY_DISTANCE = "Distance";

    private static final String PRIORITY_HEALTH = "Health";

    private static final String PRIORITY_ANGLE = "Angle";

    private static final String CRITS_OFF = "Off";

    private static final String CRITS_WAIT = "Wait";

    private static final String CRITS_JUMP = "Jump";

    private static final int CRIT_HOLD_TICKS = 8;

    private static final int JUMP_LEAD = 7;

    private static final double REACH_MARGIN = 0.01;

    private static final double RIDING_BONUS = 2.5;

    private static final double MAX_STEP_SQ = 1.0;

    private static final double[] AIM_HEIGHTS = new double[] {0.85, 0.5, 0.15};

    private final BooleanSetting players =
            this.addSetting("players", new BooleanSetting("Players", true));

    private final BooleanSetting hostiles =
            this.addSetting("hostiles", new BooleanSetting("Hostiles", false));

    private final ModeSetting priority =
            this.addSetting(
                    "priority",
                    new ModeSetting("Priority", "Distance", "Distance", "Health", "Angle"));

    private final ModeSetting crits =
            this.addSetting("crits", new ModeSetting("Crits", "Wait", "Off", "Wait", "Jump"));

    private LivingEntity target;

    private int critWait;

    public KillAuraModule() {
        super(
                "kill-aura",
                "Kill Aura",
                "Attacks nearby targets without moving your camera.",
                ModuleCategory.COMBAT);
    }

    @Override
    protected void onDisable() {
        this.target = null;
        this.critWait = 0;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (MeleeAttacks.canAct(client)
                && !player.isDead()
                && !player.isSpectator()
                && client.attackCooldown <= 0
                && MeleeAttacks.handsFree(client, player)
                && !CombatMacroLock.heldByOther(this)
                && !MeleeAttacks.actedThisTick(player)
                && !MeleeAttacks.rightClickComing(client)) {
            boolean vehicle = player.hasVehicle();
            ItemStack mainHandStack = player.getMainHandStack();
            boolean bl = mainHandStack.contains(DataComponentTypes.PIERCING_WEAPON);
            double d =
                    bl
                            ? player.getAttackRange().getEffectiveMaxRange(player) - 0.01
                            : player.getEntityInteractionRange() - 0.01 + (vehicle ? 2.5 : 0.0);
            Vec3d eyePos = player.getEyePos();
            this.target = this.pick(client, player, eyePos, d);
            if (this.target == null) {
                this.critWait = 0;
            } else {
                MaceModule module = ModuleRegistry.get(MaceModule.class);
                boolean bl2 = !bl && module != null && module.smashReady(player, this.target);
                boolean bl3 =
                        bl
                                ? !player.isBelowMinimumAttackCharge(mainHandStack, 0)
                                : bl2 || MeleeAttacks.charge(player) >= 1.0F;
                boolean bl4 = !bl3 && MeleeAttacks.ticksToFull(player) <= 1;
                boolean bl5 =
                        !bl
                                && this.crits.is("Jump")
                                && !vehicle
                                && player.isOnGround()
                                && !player.isTouchingWater()
                                && MeleeAttacks.ticksToFull(player) <= 7;
                if (bl5) {
                    InputControl.jump();
                }
                Box boundingBox = this.target.getBoundingBox();
                if (bl4) {
                    boundingBox = boundingBox.offset(stride(this.target));
                }
                Vec3d vec;
                if ((vec = aimPoint(player, eyePos, boundingBox)) != null) {
                    double e = Math.min(0.03, boundingBox.getLengthX() * 0.15);
                    Box box = boundingBox.contract(e, 0.01, e);
                    Rotation rotation = RotationUtils.fromTo(eyePos, vec);
                    Rotation rotation2 =
                            !vehicle
                                            && !box.contains(eyePos)
                                            && ServerRotation.canRotate(player)
                                            && !ServerRotation.pending()
                                    ? ServerRotation.nextStep(player, rotation)
                                    : null;
                    Vec3d vec2 = hits(player, eyePos, box, d, rotation2);
                    if (!bl3) {
                        if (bl4 && vec2 != null) {
                            ServerRotation.request(rotation);
                        }
                    } else {
                        Rotation rotation3 =
                                vehicle
                                        ? RotationUtils.entityRotation(player)
                                        : ServerRotation.sent();
                        Vec3d vec3 = hits(player, eyePos, box, d, rotation3);
                        if (vec3 != null || vec2 != null) {
                            if (bl) {
                                if (vec3 == null) {
                                    ServerRotation.request(rotation);
                                } else if (!hitsOthers(player, this.target, eyePos, rotation3)
                                        && MeleeAttacks.stab(
                                                client, new EntityHitResult(this.target, vec3))) {
                                    MeleeAttacks.claimTick(client);
                                }
                            } else {
                                boolean bl6 =
                                        bl2
                                                ? module.holdsSmash(player, this.target)
                                                : !this.crits.is("Off")
                                                        && !vehicle
                                                        && this.waitForCrit(player, bl5);
                                if (bl6) {
                                    if (bl2) {
                                        InputControl.dropSprint();
                                    }
                                    if (vec2 != null
                                            && !client.options.attackKey.isPressed()
                                            && !MeleeAttacks.clickPending(client)) {
                                        ServerRotation.request(rotation);
                                    }
                                } else {
                                    if (vec3 == null) {
                                        ServerRotation.request(rotation);
                                        vec3 = vec2;
                                    }
                                    if (MeleeAttacks.attack(
                                            client,
                                            new EntityHitResult(this.target, vec3),
                                            vehicle ? 2.5 : 0.0)) {
                                        this.critWait = 0;
                                        if (bl2) {
                                            module.noteSmash();
                                        }
                                        MeleeAttacks.claimTick(client);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            this.critWait = 0;
        }
    }

    private boolean waitForCrit(ClientPlayerEntity player, boolean bl) {
        if (MeleeAttacks.airborne(player)) {
            if (player.isSprinting() || !(player.fallDistance > 0.0) && this.critWait++ < 8) {
                InputControl.dropSprint();
                return true;
            } else {
                return false;
            }
        } else {
            return player.isOnGround()
                    && (bl || player.input.playerInput.jump())
                    && this.critWait++ < 8;
        }
    }

    private LivingEntity pick(
            MinecraftClient client, ClientPlayerEntity player, Vec3d vec, double d) {
        LivingEntity entity = null;
        double e = 0.0;
        double d2 = 0.0;
        for (Entity entity2 : client.world.getEntities()) {
            Vec3d vec2;
            LivingEntity entity3;
            Box boundingBox;
            double distance;
            if (entity2 instanceof LivingEntity
                    && !((distance =
                                    vec.distanceTo(
                                            TargetingUtils.closestPoint(
                                                    vec,
                                                    boundingBox =
                                                            (entity3 = (LivingEntity) entity2)
                                                                    .getBoundingBox())))
                            > d)
                    && this.wanted(player, entity3)
                    && (vec2 = aimPoint(player, vec, boundingBox)) != null) {
                if (entity3 == this.target) {
                    return entity3;
                }
                double rank = this.rank(player, vec, entity3, vec2, distance);
                if (entity != null) {
                    int compare = Double.compare(rank, e);
                    if (compare == 0) {
                        compare = Boolean.compare(entity3.hurtTime > 0, entity.hurtTime > 0);
                    }
                    if (compare == 0) {
                        compare = Double.compare(distance, d2);
                    }
                    if (compare >= 0) {
                        continue;
                    }
                }
                entity = entity3;
                e = rank;
                d2 = distance;
            }
        }
        return entity;
    }

    private double rank(
            ClientPlayerEntity player, Vec3d vec, LivingEntity entity, Vec3d vec2, double d) {
        if (this.priority.is("Health")) {
            return CrystalDamage.health(entity);
        } else if (!this.priority.is("Angle")) {
            return d;
        } else {
            Rotation rotation = ServerRotation.sent();
            Rotation rotation2 = rotation != null ? rotation : RotationUtils.entityRotation(player);
            return RotationUtils.angleTo(rotation2, RotationUtils.fromTo(vec, vec2));
        }
    }

    private boolean wanted(ClientPlayerEntity player, LivingEntity entity) {
        if (!MeleeAttacks.isEnemy(player, entity) || MeleeAttacks.wastedOnShield(player, entity)) {
            return false;
        } else {
            return entity instanceof PlayerEntity
                    ? this.players.get()
                    : this.hostiles.get() && MeleeAttacks.isHostile(entity);
        }
    }

    private static Vec3d aimPoint(ClientPlayerEntity player, Vec3d vec, Box box) {
        double d = Math.min(0.15, box.getLengthX() * 0.3);
        Vec3d vec2 =
                TargetingUtils.closestPoint(
                        vec, box.contract(d, Math.min(0.2, box.getLengthY() * 0.25), d));
        if (MeleeAttacks.lineIsClear(player, vec, vec2)) {
            return vec2;
        } else {
            Vec3d center = box.getCenter();
            for (double e : AIM_HEIGHTS) {
                Vec3d vec3 = new Vec3d(center.x, box.minY + box.getLengthY() * e, center.z);
                if (MeleeAttacks.lineIsClear(player, vec, vec3)) {
                    return vec3;
                }
            }
            return null;
        }
    }

    private static Vec3d stride(LivingEntity entity) {
        double d = entity.getX() - entity.lastX;
        double e;
        return d * d + (e = entity.getZ() - entity.lastZ) * e > 1.0
                ? Vec3d.ZERO
                : new Vec3d(d, 0.0, e);
    }

    private static Vec3d hits(
            ClientPlayerEntity player, Vec3d vec, Box box, double d, Rotation rotation) {
        if (rotation == null) {
            return null;
        } else {
            Vec3d vec2 = Vec3d.fromPolar(rotation.pitch(), rotation.yaw());
            Vec3d vec3 =
                    box.raycast(vec, vec.add(vec2.multiply(d)))
                            .orElse(box.contains(vec) ? vec : null);
            return vec3 != null
                            && vec.distanceTo(vec3) <= d
                            && MeleeAttacks.lineIsClear(player, vec, vec3)
                    ? vec3
                    : null;
        }
    }

    private static boolean hitsOthers(
            ClientPlayerEntity player, Entity entity, Vec3d vec, Rotation rotation) {
        AttackRangeComponent attackRange = player.getAttackRange();
        Vec3d vec2 = Vec3d.fromPolar(rotation.pitch(), rotation.yaw());
        Vec3d vec3 = vec.add(vec2.multiply(attackRange.getEffectiveMinRange(player)));
        double d = Math.max(0.0, player.getMovement().dotProduct(vec2));
        Vec3d vec4 = vec.add(vec2.multiply(attackRange.getEffectiveMaxRange(player) + d));
        for (Entity entity2 :
                player.getEntityWorld().getOtherEntities(player, new Box(vec3, vec4).expand(1.0))) {
            Box box;
            if (entity2 != entity
                    && entity2 instanceof LivingEntity
                    && PiercingWeaponComponent.canHit(player, entity2)
                    && !MeleeAttacks.isEnemy(player, entity2)
                    && ((box = entity2.getBoundingBox().expand(attackRange.hitboxMargin()))
                                    .contains(vec3)
                            || box.raycast(vec3, vec4).isPresent())) {
                return true;
            }
        }
        return false;
    }
}
