package net.efn.client.module.impl.combat;

import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.CombatMacroLock;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.combat.CombatTarget;
import net.efn.client.util.pvp.Rotation;
import net.efn.client.util.pvp.RotationUtils;
import net.efn.client.util.pvp.ServerRotation;
import net.efn.client.util.pvp.TargetingUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.ThreadLocalRandom;

@Environment(EnvType.CLIENT)
public final class AimAssistModule extends ClientModule {

    private static final long LAST_HIT_MS = 1500L;

    private static final double STICK_RANGE = 0.5;

    private static final double STICK_FOV = 1.3;

    private static final long MOUSE_IDLE_MS = 150L;

    private static final double[] STILL = new double[] {0.0, 0.0};

    private static final long MINING_QUIET_MS = 250L;

    private final NumberSetting range =
            this.addSetting("range", new NumberSetting("Range", 4.0F, 3.0F, 6.0F).step(0.1F));

    private final NumberSetting fov =
            this.addSetting("fov", new NumberSetting("FOV", 60.0F, 10.0F, 180.0F).step(5.0F));

    private final NumberSetting speed =
            this.addSetting("speed", new NumberSetting("Speed", 60.0F, 10.0F, 180.0F).step(5.0F));

    private final BooleanSetting weaponsOnly =
            this.addSetting("weapons-only", new BooleanSetting("Weapons Only", true));

    private final BooleanSetting vertical =
            this.addSetting("vertical", new BooleanSetting("Vertical", true));

    private PlayerEntity target;

    private double pace;

    private double pendingYaw;

    private double pendingPitch;

    private long movedAt;

    private double userYawSign;

    private double userPitchSign;

    private long minedAt;

    public AimAssistModule() {
        super(
                "aim-assist",
                "Aim Assist",
                "Eases your crosshair onto the nearest enemy player while you move the mouse.",
                ModuleCategory.COMBAT);
    }

    @Override
    public int settingsVersion() {
        return 1;
    }

    @Override
    public void migrateSettings(int i) {
        if (i < 1) {
            this.speed.set(60.0F);
        }
    }

    @Override
    protected void onDisable() {
        this.follow(null);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (client.interactionManager != null && client.interactionManager.isBreakingBlock()) {
            this.minedAt = Util.getMeasuringTimeMs();
        }
        if (MeleeAttacks.canAct(client)
                && !player.isUsingItem()
                && !client.options.useKey.isPressed()
                && (!this.weaponsOnly.get()
                        || MeleeAttacks.isMeleeWeapon(player.getMainHandStack()))
                && !ServerRotation.pending()
                && !CombatMacroLock.heldByOther(this)
                && Util.getMeasuringTimeMs() - this.minedAt >= 250L) {
            this.follow(this.choose(client, player));
        } else {
            this.follow(null);
        }
    }

    public double[] nudge(ClientPlayerEntity player, double d, double e, double d2) {
        long measuringTimeMs = Util.getMeasuringTimeMs();
        if (d != 0.0 || e != 0.0) {
            this.movedAt = measuringTimeMs;
        }
        if (d != 0.0) {
            this.userYawSign = Math.signum(d);
        }
        if (e != 0.0) {
            this.userPitchSign = Math.signum(e);
        }
        Rotation rotation =
                this.target != null && measuringTimeMs - this.movedAt <= 150L
                        ? this.aim(player, player.getEyePos(), core(this.target))
                        : null;
        if (rotation == null) {
            this.pendingYaw = 0.0;
            this.pendingPitch = 0.0;
            this.userYawSign = 0.0;
            this.userPitchSign = 0.0;
            return STILL;
        } else {
            double e2 = Math.min(d2, 0.05);
            float f = MathHelper.wrapDegrees(rotation.yaw() - player.getYaw());
            float g = this.vertical.get() ? rotation.pitch() - player.getPitch() : 0.0F;
            if (this.userYawSign * f < 0.0) {
                f = 0.0F;
            }
            if (this.userPitchSign * g < 0.0) {
                g = 0.0F;
            }
            double d3 = this.speed.get() * this.pace * e2;
            double e3 = 1.0 - Math.exp(-12.0 * e2);
            double mouseScale = RotationUtils.mouseScale(MinecraftClient.getInstance(), player);
            double d4 = (float) mouseScale * 0.15F;
            this.pendingYaw = this.pendingYaw + MathHelper.clamp(f * e3, -d3, d3) / d4;
            this.pendingPitch = this.pendingPitch + MathHelper.clamp(g * e3 * 0.6, -d3, d3) / d4;
            long pendingYaw2 = (long) this.pendingYaw;
            long pendingPitch2 = (long) this.pendingPitch;
            this.pendingYaw -= pendingYaw2;
            this.pendingPitch -= pendingPitch2;
            return new double[] {pendingYaw2 * mouseScale, pendingPitch2 * mouseScale};
        }
    }

    private void follow(PlayerEntity target2) {
        if (target2 != this.target) {
            this.target = target2;
            this.pace = ThreadLocalRandom.current().nextDouble(0.85, 1.15);
            this.pendingYaw = 0.0;
            this.pendingPitch = 0.0;
        }
    }

    private PlayerEntity choose(MinecraftClient client, ClientPlayerEntity player) {
        Vec3d eyePos = player.getEyePos();
        double d = this.fov.get() / 2.0;
        LivingEntity entity = CombatTarget.lastHit(1500L);
        PlayerEntity player2;
        if (entity instanceof PlayerEntity
                && this.offAngle(player, eyePos, player2 = (PlayerEntity) entity, this.range.get())
                        <= d) {
            return player2;
        } else if (this.target != null
                && this.target.getEntityWorld() == player.getEntityWorld()
                && this.offAngle(player, eyePos, this.target, this.range.get() + 0.5) <= d * 1.3) {
            return this.target;
        } else {
            PlayerEntity player3 = null;
            double e = d;
            for (PlayerEntity player4 : client.world.getPlayers()) {
                double offAngle = this.offAngle(player, eyePos, player4, this.range.get());
                if (!(offAngle > e)) {
                    player3 = player4;
                    e = offAngle;
                }
            }
            return player3;
        }
    }

    private double offAngle(ClientPlayerEntity player, Vec3d vec, PlayerEntity player2, double d) {
        if (MeleeAttacks.isEnemy(player, player2)
                && (!player2.isInvisible() || armourShows(player2))) {
            if (vec.distanceTo(TargetingUtils.closestPoint(vec, player2.getBoundingBox())) > d) {
                return 1.7976931348623157E308;
            } else {
                Box box = core(player2);
                if (!MeleeAttacks.lineIsClear(player, vec, box.getCenter())) {
                    return 1.7976931348623157E308;
                } else {
                    Rotation rotation = this.aim(player, vec, box);
                    return rotation == null
                            ? 0.0
                            : RotationUtils.angleTo(RotationUtils.entityRotation(player), rotation);
                }
            }
        } else {
            return 1.7976931348623157E308;
        }
    }

    private Rotation aim(ClientPlayerEntity player, Vec3d vec, Box box) {
        Vec3d rotationVec = player.getRotationVec(1.0F);
        if (box.raycast(vec, vec.add(rotationVec.multiply(this.range.get() + 1.0))).isPresent()) {
            return null;
        } else {
            Vec3d vec2 =
                    TargetingUtils.closestPoint(
                            vec.add(rotationVec.multiply(vec.distanceTo(box.getCenter()))), box);
            return RotationUtils.fromTo(vec, vec2);
        }
    }

    private static boolean armourShows(PlayerEntity player) {
        for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES) {
            if (equipmentSlot.isArmorSlot() && !player.getEquippedStack(equipmentSlot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static Box core(PlayerEntity player) {
        Box boundingBox = player.getBoundingBox();
        return boundingBox.contract(
                boundingBox.getLengthX() * 0.25,
                boundingBox.getLengthY() * 0.1,
                boundingBox.getLengthX() * 0.25);
    }
}
