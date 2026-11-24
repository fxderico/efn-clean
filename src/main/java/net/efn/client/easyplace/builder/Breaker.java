package net.efn.client.easyplace.builder;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

import java.util.Random;

@Environment(EnvType.CLIENT)
final class Breaker {

    private static final int TIME_LIMIT = 100;

    private static final int LEAN_TICKS = 40;

    private static final float TURN = 22.0F;

    private static final Random HAND = new Random();

    private static final float WOBBLE_CHANCE = 0.3F;

    private static final double WOBBLE_DEGREES = 0.35;

    private final BlockPos pos;

    private final boolean mayLean;

    private final Direction side;

    private static final float VARIED_CHANCE = 0.2F;

    private final boolean varied = HAND.nextFloat() < 0.2F;

    private final double u =
            this.varied ? MathHelper.clamp(0.5 + HAND.nextGaussian() * 0.12, 0.22, 0.78) : 0.5;

    private final double v =
            this.varied ? MathHelper.clamp(0.5 + HAND.nextGaussian() * 0.12, 0.22, 0.78) : 0.5;

    private Direction face;

    private Direction leanWay;

    private boolean leaning;

    private float yaw;

    private float pitch;

    private int phase;

    private int ticks;

    private int react = -1;

    Breaker(BlockPos pos2, boolean bl) {
        this(pos2, bl, null);
    }

    Breaker(BlockPos pos2, boolean mayLean2, Direction side2) {
        this.pos = pos2;
        this.mayLean = mayLean2;
        this.side = side2;
    }

    private Aim.Shot shot(MinecraftClient client) {
        return this.side == null
                ? Aim.at(client, this.pos)
                : Aim.mine(client, this.pos, this.side, this.u, this.v);
    }

    BlockPos pos() {
        return this.pos;
    }

    Breaker.Result tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null || client.interactionManager == null) {
            return this.finish(client, Breaker.Result.FAILED);
        } else if (client.world.getBlockState(this.pos).isAir()) {
            if (this.phase >= 2) {
                client.interactionManager.cancelBlockBreaking();
            }
            return this.finish(client, Breaker.Result.DONE);
        } else if (++this.ticks > 100) {
            client.interactionManager.cancelBlockBreaking();
            return this.finish(client, Breaker.Result.FAILED);
        } else {
            switch (this.phase) {
                case 0:
                    Aim.Shot shot = this.shot(client);
                    if (shot == null) {
                        if (this.mayLean && this.ticks <= 40) {
                            if (this.leanWay == null) {
                                this.leanWay = this.leanWay(client, player);
                            }
                            if (this.leanWay == null) {
                                return this.finish(client, Breaker.Result.FAILED);
                            }
                            this.leaning = true;
                            client.options.sneakKey.setPressed(true);
                            float turn =
                                    Aim.turn(
                                            client,
                                            this.leanWay.getPositiveHorizontalDegrees(),
                                            70.0F,
                                            22.0F);
                            client.options.forwardKey.setPressed(turn < 20.0F);
                            return Breaker.Result.RUNNING;
                        }
                        return this.finish(client, Breaker.Result.FAILED);
                    }
                    if (this.leaning) {
                        client.options.forwardKey.setPressed(false);
                    }
                    this.face = shot.hit().getSide();
                    this.yaw = shot.yaw();
                    this.pitch = shot.pitch();
                    this.phase = 1;
                    break;
                case 1:
                    if (this.react < 0) {
                        if (!(Aim.turn(client, this.yaw, this.pitch, 22.0F) > 1.0F)
                                && !(Math.abs(player.getPitch() - this.pitch) > 1.0F)) {
                            Aim.Shot shot2 = this.shot(client);
                            if (shot2 == null) {
                                this.phase = 0;
                                return Breaker.Result.RUNNING;
                            }
                            this.face = shot2.hit().getSide();
                            Aim.look(client, shot2.yaw(), shot2.pitch());
                            this.yaw = player.getYaw();
                            this.pitch = player.getPitch();
                            this.react =
                                    this.varied
                                            ? (int)
                                                    MathHelper.clamp(
                                                            Math.round(
                                                                    Math.exp(
                                                                            Math.log(2.6)
                                                                                    + HAND
                                                                                                    .nextGaussian()
                                                                                            * 0.35)),
                                                            2L,
                                                            6L)
                                            : 2;
                            return Breaker.Result.RUNNING;
                        }
                        return Breaker.Result.RUNNING;
                    }
                    if (--this.react > 0) {
                        return Breaker.Result.RUNNING;
                    }
                    BlockHitResult blockHitResult =
                            Aim.trace(
                                    client,
                                    player,
                                    player.getYaw(),
                                    player.getPitch(),
                                    player.getBlockInteractionRange());
                    if (blockHitResult == null
                            || blockHitResult.getType() != Type.BLOCK
                            || !blockHitResult.getBlockPos().equals(this.pos)
                            || blockHitResult.getSide() != this.face) {
                        this.react = -1;
                        return Breaker.Result.RUNNING;
                    }
                    client.interactionManager.attackBlock(this.pos, this.face);
                    player.swingHand(Hand.MAIN_HAND);
                    this.phase = 2;
                    break;
                default:
                    if (this.side != null && this.varied && HAND.nextFloat() < 0.3F) {
                        this.wobble(client, player);
                    }
                    Aim.look(client, this.yaw, this.pitch);
                    client.interactionManager.updateBlockBreakingProgress(this.pos, this.face);
                    player.swingHand(Hand.MAIN_HAND);
            }
            return Breaker.Result.RUNNING;
        }
    }

    private void wobble(MinecraftClient client, ClientPlayerEntity player) {
        float f = this.yaw + (float) (HAND.nextGaussian() * 0.35);
        float g;
        BlockHitResult blockHitResult =
                Aim.trace(
                        client,
                        player,
                        f,
                        g = this.pitch + (float) (HAND.nextGaussian() * 0.35 * 0.7),
                        player.getBlockInteractionRange());
        if (blockHitResult != null
                && blockHitResult.getType() == Type.BLOCK
                && blockHitResult.getBlockPos().equals(this.pos)
                && blockHitResult.getSide() == this.face) {
            Aim.look(client, f, g);
            this.yaw = player.getYaw();
            this.pitch = player.getPitch();
        }
    }

    private Direction leanWay(MinecraftClient client, ClientPlayerEntity player) {
        BlockPos pos2 = player.getBlockPos();
        Direction direction = null;
        for (Direction direction2 : net.minecraft.util.math.Direction.Type.HORIZONTAL) {
            BlockPos pos3;
            if (client.world.getBlockState(this.pos.offset(direction2)).isReplaceable()
                    && client.world
                            .getBlockState(pos3 = pos2.offset(direction2))
                            .getCollisionShape(client.world, pos3)
                            .isEmpty()
                    && client.world
                            .getBlockState(pos3.up())
                            .getCollisionShape(client.world, pos3.up())
                            .isEmpty()
                    && (direction == null
                            || Math.abs(
                                            MathHelper.wrapDegrees(
                                                    direction2.getPositiveHorizontalDegrees()
                                                            - player.getYaw()))
                                    < Math.abs(
                                            MathHelper.wrapDegrees(
                                                    direction.getPositiveHorizontalDegrees()
                                                            - player.getYaw())))) {
                direction = direction2;
            }
        }
        return direction;
    }

    void release(MinecraftClient client) {
        this.finish(client, Breaker.Result.FAILED);
    }

    private Breaker.Result finish(MinecraftClient client, Breaker.Result result) {
        if (this.leaning) {
            client.options.forwardKey.setPressed(false);
            client.options.sneakKey.setPressed(false);
            this.leaning = false;
        }
        return result;
    }

    @Environment(EnvType.CLIENT)
    static enum Result {
        RUNNING,
        DONE,
        FAILED;

        private Result() {}
    }
}
