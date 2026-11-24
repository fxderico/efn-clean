package net.efn.client.module.impl.combat;

import net.efn.client.event.EventHandler;
import net.efn.client.event.EventPriority;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.CombatMacroLock;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.pvp.InputControl;
import net.efn.client.util.pvp.Rotation;
import net.efn.client.util.pvp.ServerRotation;
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
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class TriggerbotModule extends ClientModule {

    private static final String CRITS_OFF = "Off";

    private static final String CRITS_SMART = "Smart";

    private static final String CRITS_ONLY = "Only";

    private static final int TRACK_GRACE_TICKS = 4;

    private static final int SMART_MAX_WAIT_TICKS = 12;

    private final NumberSetting reaction =
            this.addSetting(
                    "reaction", new NumberSetting("Reaction", 3.0F, 0.0F, 10.0F).step(1.0F));

    private final ModeSetting crits =
            this.addSetting("crits", new ModeSetting("Crits", "Smart", "Off", "Smart", "Only"));

    private final BooleanSetting holdAttack =
            this.addSetting("hold-attack", new BooleanSetting("Hold Attack", false));

    private final BooleanSetting players =
            this.addSetting("players", new BooleanSetting("Players", true));

    private final BooleanSetting hostiles =
            this.addSetting("hostiles", new BooleanSetting("Hostiles", false));

    private final BooleanSetting invisible =
            this.addSetting("invisible", new BooleanSetting("Invisible", false));

    private final BooleanSetting weaponsOnly =
            this.addSetting("weapons-only", new BooleanSetting("Weapons Only", true));

    private int ticks;

    private int trackedId = -1;

    private int seenTick;

    private int reactionDoneTick;

    private int chargedTick = -1;

    private int chargePause;

    private int critWait;

    private int critWaitTick = -1;

    public TriggerbotModule() {
        super(
                "triggerbot",
                "Triggerbot",
                "Hits the entity under the crosshair once the swing is charged, with a reaction"
                        + " delay.",
                ModuleCategory.COMBAT);
    }

    @Override
    protected void onEnable() {
        this.reset();
    }

    @Override
    protected void onDisable() {
        this.reset();
    }

    private void reset() {
        this.ticks = 0;
        this.trackedId = -1;
        this.seenTick = 0;
        this.reactionDoneTick = 0;
        this.chargedTick = -1;
        this.chargePause = 0;
        this.critWait = 0;
        this.critWaitTick = -1;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onTick(TickEvent event) {
        this.ticks++;
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (!MeleeAttacks.canAct(client)) {
            this.trackedId = -1;
        } else {
            this.trackCharge(player);
            EntityHitResult entityHitResult = MeleeAttacks.crosshairHit(client);
            if (this.holdAttack.get()
                    && this.trackedId != -1
                    && client.crosshairTarget != null
                    && client.crosshairTarget.getType() == Type.MISS) {
                MeleeAttacks.takeAttackClicks(client);
            }
            LivingEntity entity;
            Entity entity2;
            if (entityHitResult != null
                    && (entity2 = entityHitResult.getEntity()) instanceof LivingEntity
                    && this.isTarget(player, entity = (LivingEntity) entity2)) {
                if (entity.getId() != this.trackedId) {
                    this.trackedId = entity.getId();
                    this.reactionDoneTick =
                            this.ticks + this.reaction.getInt() + MeleeAttacks.spread(2);
                }
                this.seenTick = this.ticks;
                MaceModule module = ModuleRegistry.get(MaceModule.class);
                boolean bl = module != null && module.smashReady(player, entity);
                if (this.ticks >= this.reactionDoneTick
                        && (bl
                                || this.chargedTick >= 0
                                        && this.ticks - this.chargedTick >= this.chargePause)) {
                    if (!this.holdAttack.get() || client.options.attackKey.isPressed()) {
                        if (!this.weaponsOnly.get()
                                || MeleeAttacks.isMeleeWeapon(player.getMainHandStack())) {
                            if (!CombatMacroLock.heldByOther(this)
                                    && !MeleeAttacks.wastedOnShield(player, entity)) {
                                if (client.attackCooldown <= 0
                                        && !MeleeAttacks.clickPending(client)
                                        && !MeleeAttacks.rightClickComing(client)
                                        && !ServerRotation.pending()) {
                                    boolean bl2 =
                                            player.getMainHandStack()
                                                    .contains(DataComponentTypes.PIERCING_WEAPON);
                                    if (bl2 || bl || this.critsAllow(client, player, entity)) {
                                        if (!bl2
                                                || (entityHitResult = this.stabHit(player, entity))
                                                        != null) {
                                            if (bl2
                                                    ? MeleeAttacks.stab(client, entityHitResult)
                                                    : MeleeAttacks.attack(
                                                            client, entityHitResult)) {
                                                this.chargedTick = -1;
                                                if (bl) {
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
                }
            } else {
                if (this.ticks - this.seenTick > 4) {
                    this.trackedId = -1;
                }
            }
        }
    }

    private void trackCharge(ClientPlayerEntity player) {
        if (MeleeAttacks.charge(player) < 1.0F) {
            this.chargedTick = -1;
        } else if (this.chargedTick < 0) {
            this.chargedTick = this.ticks;
            this.chargePause = MeleeAttacks.spread(1);
        }
    }

    private EntityHitResult stabHit(ClientPlayerEntity player, LivingEntity entity) {
        Rotation rotation = ServerRotation.sent();
        if (rotation == null) {
            return null;
        } else {
            AttackRangeComponent attackRange = player.getAttackRange();
            Vec3d vec = Vec3d.fromPolar(rotation.pitch(), rotation.yaw());
            Vec3d eyePos = player.getEyePos();
            Vec3d vec2 = eyePos.add(vec.multiply(attackRange.getEffectiveMinRange(player)));
            Vec3d vec3 =
                    eyePos.add(
                            vec.multiply(
                                    attackRange.getEffectiveMaxRange(player)
                                            + Math.max(0.0, player.getMovement().dotProduct(vec))));
            float hitboxMargin = attackRange.hitboxMargin();
            Vec3d vec4 =
                    entity.getBoundingBox().expand(hitboxMargin).raycast(eyePos, vec3).orElse(null);
            if (vec4 != null && MeleeAttacks.lineIsClear(player, eyePos, vec4)) {
                for (Entity entity2 :
                        player.getEntityWorld()
                                .getOtherEntities(player, new Box(vec2, vec3).expand(1.0))) {
                    Box box;
                    if (entity2 != entity
                            && entity2 instanceof LivingEntity entity3
                            && PiercingWeaponComponent.canHit(player, entity2)
                            && !this.isTarget(player, entity3)
                            && ((box = entity2.getBoundingBox().expand(hitboxMargin)).contains(vec2)
                                    || box.raycast(vec2, vec3).isPresent())) {
                        return null;
                    }
                }
                return new EntityHitResult(entity, vec4);
            } else {
                return null;
            }
        }
    }

    private boolean isTarget(ClientPlayerEntity player, LivingEntity entity) {
        if (!MeleeAttacks.isEnemy(player, entity)) {
            return false;
        } else if (entity.isInvisible() && !this.invisible.get()) {
            return false;
        } else {
            return entity instanceof PlayerEntity
                    ? this.players.get()
                    : this.hostiles.get() && MeleeAttacks.isHostile(entity);
        }
    }

    private boolean critsAllow(
            MinecraftClient client, ClientPlayerEntity player, LivingEntity entity) {
        String crits2 = this.crits.get();
        return switch (crits2) {
            case "Only" -> {
                if (MeleeAttacks.airborne(player)
                        && player.isSprinting()
                        && player.fallDistance > 0.0) {
                    InputControl.dropSprint();
                }
                yield MeleeAttacks.wouldCrit(player, entity);
            }
            case "Smart" -> {
                if (MeleeAttacks.wouldCrit(player, entity)) {
                    yield true;
                } else {
                    boolean bl =
                            player.isOnGround()
                                    && client.options.jumpKey.isPressed()
                                    && !player.isSprinting();
                    if (!MeleeAttacks.critStance(player, entity) && !bl) {
                        yield true;
                    } else {
                        if (this.critWaitTick != this.ticks - 1) {
                            this.critWait = 0;
                        }
                        this.critWaitTick = this.ticks;
                        yield this.critWait++ >= 12;
                    }
                }
            }
            default -> true;
        };
    }
}
