package net.efn.client.module.impl.combat.bot;

import net.efn.client.easyplace.builder.Aim;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.util.pvp.Rotation;
import net.efn.client.util.pvp.RotationUtils;
import net.efn.client.util.pvp.TargetingUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
final class BotBody {

    private static final double KEEP_EXTRA = 8.0;

    private static final float FAR_OFF = 40.0F;

    private static final int FLIP_COOLDOWN = 8;

    private static final int DROP_SCAN = 16;

    private PlayerEntity target;

    private boolean grounded;

    private int side = ThreadLocalRandom.current().nextBoolean() ? 1 : -1;

    private int sideUntil;

    private int flippedAt = -100;

    private double sideCarry;

    private int tick;

    BotBody() {}

    PlayerEntity target() {
        return this.target;
    }

    int tick() {
        return this.tick;
    }

    void forget() {
        this.target = null;
    }

    void hold(PlayerEntity target2) {
        this.target = target2;
    }

    void stayGrounded(boolean grounded2) {
        this.grounded = grounded2;
    }

    void release(MinecraftClient client) {
        client.options.forwardKey.setPressed(false);
        client.options.backKey.setPressed(false);
        client.options.leftKey.setPressed(false);
        client.options.rightKey.setPressed(false);
        client.options.jumpKey.setPressed(false);
        client.options.sprintKey.setPressed(false);
        client.options.useKey.setPressed(false);
    }

    PlayerEntity pick(MinecraftClient client, ClientPlayerEntity player, double d) {
        return this.pick(client, player, d, d + 8.0);
    }

    PlayerEntity pick(MinecraftClient client, ClientPlayerEntity player, double d, double e) {
        this.tick++;
        if (this.tick >= this.sideUntil) {
            this.side = -this.side;
            this.sideUntil = this.tick + 10 + ThreadLocalRandom.current().nextInt(13);
        }
        if (this.target != null
                && (!this.target.isAlive()
                        || this.target.isRemoved()
                        || this.target.getEntityWorld() != player.getEntityWorld()
                        || this.target.distanceTo(player) > e
                        || !MeleeAttacks.isEnemy(player, this.target))) {
            this.target = null;
        }
        if (this.target != null) {
            return this.target;
        } else {
            PlayerEntity player2 = null;
            for (PlayerEntity player3 : client.world.getPlayers()) {
                double distance;
                if (player3 != player
                        && player3.isAlive()
                        && !player3.isSpectator()
                        && MeleeAttacks.isEnemy(player, player3)
                        && (distance = player3.distanceTo(player)) <= d
                        && (player2 == null || distance < player2.distanceTo(player))) {
                    player2 = player3;
                }
            }
            this.target = player2;
            return player2;
        }
    }

    void flipSide() {
        if (this.tick - this.flippedAt >= 8) {
            this.flippedAt = this.tick;
            this.side = -this.side;
            this.sideUntil = this.tick + 10 + ThreadLocalRandom.current().nextInt(13);
        }
    }

    void face(MinecraftClient client, ClientPlayerEntity player, PlayerEntity player2, float f) {
        Vec3d eyePos = player.getEyePos();
        Vec3d vec = player2.getEntityPos().subtract(player2.lastX, player2.lastY, player2.lastZ);
        Box box = player2.getBoundingBox().offset(vec);
        Box box2 =
                box.contract(
                        box.getLengthX() * 0.2, box.getLengthY() * 0.15, box.getLengthZ() * 0.2);
        Rotation rotation = RotationUtils.fromTo(eyePos, TargetingUtils.closestPoint(eyePos, box2));
        float g = Math.abs(MathHelper.wrapDegrees(rotation.yaw() - player.getYaw()));
        Aim.turn(client, rotation.yaw(), rotation.pitch(), (g > 40.0F ? 27.0F : 22.0F) * f);
    }

    void drive(
            MinecraftClient client,
            ClientPlayerEntity player,
            int i,
            double d,
            boolean pressed,
            boolean bl) {
        float yaw = player.getYaw();
        Vec3d vec = Vec3d.fromPolar(0.0F, yaw);
        if (i > 0 && safeStep(player, vec)) {
            client.options.forwardKey.setPressed(true);
            client.options.sprintKey.setPressed(pressed);
            if (player.horizontalCollision && player.isOnGround()) {
                bl = true;
            }
        } else if (i < 0 && safeStep(player, vec.multiply(-1.0))) {
            client.options.backKey.setPressed(true);
        }
        if (bl && !this.grounded && player.isOnGround() && !player.isTouchingWater()) {
            client.options.jumpKey.setPressed(true);
        }
        this.sideCarry = this.sideCarry + Math.max(0.0, Math.min(1.0, d));
        if (!(this.sideCarry < 1.0)) {
            this.sideCarry--;
            Vec3d vec2 = Vec3d.fromPolar(0.0F, yaw - 90.0F);
            Vec3d vec3 = this.side > 0 ? vec2 : vec2.multiply(-1.0);
            if (!safeStep(player, vec3)) {
                this.side = -this.side;
                if (!safeStep(player, vec3.multiply(-1.0))) {
                    return;
                }
            }
            (this.side > 0 ? client.options.leftKey : client.options.rightKey).setPressed(true);
        }
    }

    void keepDistance(
            MinecraftClient client,
            ClientPlayerEntity player,
            PlayerEntity player2,
            double d,
            double e,
            double d2,
            boolean bl) {
        double e2 = Math.hypot(player2.getX() - player.getX(), player2.getZ() - player.getZ());
        int i = e2 > e + 0.25 ? 1 : (e2 < d ? -1 : 0);
        this.drive(client, player, i, bl && e2 <= d2 + 1.0 ? 1.0 : 0.0, e2 > d2, false);
    }

    void flee(MinecraftClient client, ClientPlayerEntity player, PlayerEntity player2) {
        float yaw = RotationUtils.fromTo(player2.getEyePos(), player.getEyePos()).yaw();
        Aim.turn(client, yaw, 5.0F, 27.0F);
        this.drive(client, player, 1, 0.0, true, false);
    }

    void away(MinecraftClient client, ClientPlayerEntity player, Vec3d vec) {
        Vec3d vec2 = player.getEntityPos().subtract(vec).multiply(1.0, 0.0, 1.0);
        if (vec2.lengthSquared() < 1.0E-6) {
            vec2 = Vec3d.fromPolar(0.0F, player.getYaw()).multiply(-1.0);
        }
        this.go(client, player, vec2);
    }

    boolean toward(MinecraftClient client, ClientPlayerEntity player, Vec3d vec) {
        Vec3d vec2 = vec.subtract(player.getEntityPos()).multiply(1.0, 0.0, 1.0);
        if (vec2.lengthSquared() < 0.09) {
            return true;
        } else {
            this.go(client, player, vec2);
            if (!this.grounded && player.horizontalCollision && player.isOnGround()) {
                client.options.jumpKey.setPressed(true);
            }
            return false;
        }
    }

    void dropToward(MinecraftClient client, ClientPlayerEntity player, Vec3d vec) {
        Vec3d vec2 = vec.subtract(player.getEntityPos()).multiply(1.0, 0.0, 1.0);
        if (!(vec2.lengthSquared() < 0.09) && safeStep(player, vec2.normalize(), 16)) {
            keys(client, player, vec2);
        }
    }

    private void go(MinecraftClient client, ClientPlayerEntity player, Vec3d vec) {
        if (safeStep(player, vec.normalize())) {
            keys(client, player, vec);
        }
    }

    private static void keys(MinecraftClient client, ClientPlayerEntity player, Vec3d vec) {
        float f = (float) Math.toDegrees(Math.atan2(-vec.x, vec.z));
        float g = MathHelper.wrapDegrees(f - player.getYaw());
        float h = Math.abs(g);
        if (h <= 67.5F) {
            client.options.forwardKey.setPressed(true);
        }
        if (h >= 112.5F) {
            client.options.backKey.setPressed(true);
        }
        if (g <= -22.5F && g >= -157.5F) {
            client.options.leftKey.setPressed(true);
        }
        if (g >= 22.5F && g <= 157.5F) {
            client.options.rightKey.setPressed(true);
        }
    }

    void backOff(MinecraftClient client, ClientPlayerEntity player) {
        this.drive(client, player, -1, 0.0, false, false);
    }

    boolean use(MinecraftClient client, ClientPlayerEntity player, Predicate<ItemStack> predicate) {
        int hotbarSlot = hotbarSlot(player, predicate);
        if (hotbarSlot < 0) {
            return false;
        } else {
            PlayerInventory inventory = player.getInventory();
            if (inventory.getSelectedSlot() != hotbarSlot) {
                inventory.setSelectedSlot(hotbarSlot);
                return true;
            } else {
                client.options.useKey.setPressed(true);
                return true;
            }
        }
    }

    static int hotbarSlot(ClientPlayerEntity player, Predicate<ItemStack> predicate) {
        PlayerInventory inventory = player.getInventory();
        if (predicate.test(inventory.getStack(inventory.getSelectedSlot()))) {
            return inventory.getSelectedSlot();
        } else {
            for (int i = 0; i < 9; i++) {
                if (predicate.test(inventory.getStack(i))) {
                    return i;
                }
            }
            return -1;
        }
    }

    static boolean safeStep(ClientPlayerEntity player, Vec3d vec) {
        return safeStep(player, vec, 3);
    }

    private static boolean safeStep(ClientPlayerEntity player, Vec3d vec, int i) {
        World entityWorld = player.getEntityWorld();
        BlockPos pos =
                BlockPos.ofFloored(
                        player.getX() + vec.x * 0.8,
                        player.getY() + 0.1,
                        player.getZ() + vec.z * 0.8);
        for (int j = 0; j <= i; j++) {
            BlockPos pos2 = pos.down(j);
            BlockState blockState = entityWorld.getBlockState(pos2);
            if (blockState.isOf(Blocks.LAVA)
                    || blockState.isIn(BlockTags.FIRE)
                    || blockState.isOf(Blocks.CACTUS)
                    || blockState.isOf(Blocks.SWEET_BERRY_BUSH)
                    || blockState.isOf(Blocks.COBWEB)
                    || j <= 1 && blockState.isOf(Blocks.MAGMA_BLOCK)) {
                return false;
            }
            if (!blockState.getCollisionShape(entityWorld, pos2).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
