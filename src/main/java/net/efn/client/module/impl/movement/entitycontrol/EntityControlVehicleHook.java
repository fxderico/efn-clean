package net.efn.client.module.impl.movement.entitycontrol;

import net.efn.client.input.EfnKeyPolling;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.misc.FreecamModule;
import net.efn.client.module.impl.movement.BoatFlyModule;
import net.efn.client.module.impl.movement.BoatNoclipModule;
import net.efn.client.module.impl.movement.EntityControlModule;
import net.efn.client.module.impl.movement.EntitySpeedModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.AbstractBlock.AbstractBlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil.Key;
import net.minecraft.client.util.InputUtil.Type;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec3d;

import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public final class EntityControlVehicleHook {

    private static final double DIAGONAL = 1.0 / Math.sqrt(2.0);

    public static volatile boolean anyActive;

    private EntityControlVehicleHook() {}

    public static void refresh() {
        anyActive =
                EntityControlModule.active != null
                        || EntitySpeedModule.active != null
                        || BoatFlyModule.active != null
                        || BoatNoclipModule.active != null;
    }

    public static Vec3d onEntityMove(Entity entity, MovementType movementType, Vec3d vec) {
        if (movementType == MovementType.SELF && entity.getEntityWorld().isClient()) {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null
                    && player.getVehicle() == entity
                    && entity.getControllingPassenger() == player) {
                Vec3d velocity = entity.getVelocity();
                BoatNoclipModule active = BoatNoclipModule.active;
                if (active != null && entity instanceof AbstractBoatEntity abstractBoatEntity) {
                    Vec3d vec2 = active.boatMovement(abstractBoatEntity, player);
                    entity.setYaw(player.getYaw());
                    if (vec == velocity) {
                        entity.setVelocity(vec2);
                    }
                    return vec2;
                } else {
                    EntityControlModule active2 = EntityControlModule.active;
                    EntitySpeedModule active3 = EntitySpeedModule.active;
                    BoatFlyModule active4 = BoatFlyModule.active;
                    double d = velocity.x;
                    double e = velocity.y;
                    double d2 = velocity.z;
                    boolean bl = false;
                    if (active3 != null) {
                        if (active3.appliesTo(entity)) {
                            Vec3d vec3 = horizontalVelocity(player, active3.horizontalSpeed());
                            d = vec3.x;
                            d2 = vec3.z;
                            bl = true;
                        }
                    } else if (active4 != null) {
                        Vec3d vec4 = horizontalVelocity(player, active4.horizontalSpeed());
                        d = vec4.x;
                        d2 = vec4.z;
                        bl = true;
                    }
                    if (active4 != null) {
                        e = active4.verticalVelocity();
                        bl = true;
                    }
                    if (active4 != null || active2 != null && active2.locksYaw(entity)) {
                        entity.setYaw(player.getYaw());
                    }
                    if (!bl) {
                        return vec;
                    } else {
                        Vec3d vec5 = new Vec3d(d, e, d2);
                        if (vec == velocity) {
                            entity.setVelocity(vec5);
                        }
                        return vec5;
                    }
                }
            } else {
                return vec;
            }
        } else {
            return vec;
        }
    }

    public static boolean locksBoatTurning(AbstractBoatEntity abstractBoatEntity) {
        if (!anyActive || !abstractBoatEntity.getEntityWorld().isClient()) {
            return false;
        } else if (BoatFlyModule.active == null && BoatNoclipModule.active == null) {
            EntityControlModule active = EntityControlModule.active;
            return active != null && active.locksYaw(abstractBoatEntity);
        } else {
            return true;
        }
    }

    public static Vec3d horizontalVelocity(ClientPlayerEntity player, double d) {
        float yaw = player.getYaw();
        Vec3d vec = Vec3d.fromPolar(0.0F, yaw);
        Vec3d vec2 = Vec3d.fromPolar(0.0F, yaw + 90.0F);
        PlayerInput playerInput = player.input.playerInput;
        double e = 0.0;
        double d2 = 0.0;
        boolean bl = false;
        if (playerInput.forward()) {
            e += vec.x / 20.0 * d;
            d2 += vec.z / 20.0 * d;
            bl = true;
        }
        if (playerInput.backward()) {
            e -= vec.x / 20.0 * d;
            d2 -= vec.z / 20.0 * d;
            bl = true;
        }
        boolean bl2 = false;
        if (playerInput.right()) {
            e += vec2.x / 20.0 * d;
            d2 += vec2.z / 20.0 * d;
            bl2 = true;
        }
        if (playerInput.left()) {
            e -= vec2.x / 20.0 * d;
            d2 -= vec2.z / 20.0 * d;
            bl2 = true;
        }
        if (bl && bl2) {
            e *= DIAGONAL;
            d2 *= DIAGONAL;
        }
        return new Vec3d(e, 0.0, d2);
    }

    public static boolean isPhysicallyDown(KeyBinding keyBinding) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen == null && !keyBinding.isUnbound() && !isFreecamActive()) {
            Key boundKey = KeyBindingHelper.getBoundKeyOf(keyBinding);
            Type category = boundKey.getCategory();
            if (category == Type.KEYSYM) {
                return EfnKeyPolling.isKeyDown(client.getWindow(), boundKey.getCode());
            } else {
                return category == Type.MOUSE
                        ? GLFW.glfwGetMouseButton(
                                        client.getWindow().getHandle(), boundKey.getCode())
                                == 1
                        : keyBinding.isPressed();
            }
        } else {
            return false;
        }
    }

    public static boolean isOnAir(Entity entity) {
        return entity.getEntityWorld()
                .getStatesInBox(entity.getBoundingBox().expand(0.0625).stretch(0.0, -0.55, 0.0))
                .allMatch(AbstractBlockState::isAir);
    }

    private static boolean isFreecamActive() {
        FreecamModule module = ModuleRegistry.get(FreecamModule.class);
        return module != null && module.isEnabled();
    }
}
