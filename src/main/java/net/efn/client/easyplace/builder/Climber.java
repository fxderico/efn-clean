package net.efn.client.easyplace.builder;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Environment(EnvType.CLIENT)
final class Climber {

    static final int HEIGHT = 2;

    private static final int STEP_LIMIT = 40;

    private static final int TRIES = 3;

    private static final int DIG_DEPTH = 2;

    private static final double CENTERED = 0.12;

    private final Set<Long> scaffold;

    private final int x;

    private final int z;

    private final int baseY;

    private final int height;

    private int placed;

    private int phase;

    private int phaseTicks;

    private int tries;

    private final List<BlockPos> toBreak = new ArrayList<>();

    private Breaker breaker;

    Climber(BlockPos pos, Set<Long> set) {
        this(pos, set, 2);
    }

    Climber(BlockPos pos, Set<Long> scaffold2, int height2) {
        this.x = pos.getX();
        this.z = pos.getZ();
        this.baseY = pos.getY();
        this.scaffold = scaffold2;
        this.height = height2;
    }

    static BlockPos column(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player != null && client.world != null && player.isOnGround()) {
            int i = (int) Math.floor(player.getY() + 0.01);
            if (Math.abs(player.getY() - i) > 0.01) {
                return null;
            } else {
                BlockPos pos = BlockPos.ofFloored(player.getX(), i, player.getZ());
                if (groundUnder(client, pos)) {
                    return pos;
                } else {
                    double d = player.getWidth() / 2.0 - 0.01;
                    for (double[] doubles : new double[][] {{-d, -d}, {d, -d}, {-d, d}, {d, d}}) {
                        BlockPos pos2 =
                                BlockPos.ofFloored(
                                        player.getX() + doubles[0], i, player.getZ() + doubles[1]);
                        if (groundUnder(client, pos2)) {
                            return pos2;
                        }
                    }
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    static boolean hasRoom(MinecraftClient client, BlockPos pos) {
        for (int i = 0; i <= 4; i++) {
            BlockPos pos2 = pos.up(i);
            if (!client.world.getBlockState(pos2).getCollisionShape(client.world, pos2).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean groundUnder(MinecraftClient client, BlockPos pos) {
        BlockPos pos2 = pos.down();
        VoxelShape collisionShape =
                client.world.getBlockState(pos2).getCollisionShape(client.world, pos2);
        return !collisionShape.isEmpty() && collisionShape.getMax(Axis.Y) >= 0.99;
    }

    BlockPos column() {
        return new BlockPos(this.x, this.baseY, this.z);
    }

    static int leavesCount(ClientPlayerEntity player) {
        return Scaffold.count(player);
    }

    Climber.Result tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null || client.interactionManager == null) {
            return Climber.Result.FAILED;
        } else if (this.breaker != null || this.placed >= this.height) {
            return this.breakBelow(client);
        } else if (!Scaffold.has(player)) {
            return Climber.Result.FAILED;
        } else if (!Scaffold.hold(client, player)) {
            return Climber.Result.RUNNING;
        } else {
            this.phaseTicks++;
            BlockPos pos = new BlockPos(this.x, this.baseY + this.placed, this.z);
            switch (this.phase) {
                case 0:
                    double d = this.x + 0.5 - player.getX();
                    double e = this.z + 0.5 - player.getZ();
                    boolean bl = Math.abs(d) < 0.12 && Math.abs(e) < 0.12;
                    if (bl) {
                        client.options.forwardKey.setPressed(false);
                        Aim.turn(client, player.getYaw(), 90.0F, 22.0F);
                        Vec3d velocity = player.getVelocity();
                        if (this.phaseTicks >= 2
                                && player.getPitch() > 88.0F
                                && velocity.x * velocity.x + velocity.z * velocity.z < 4.0E-4) {
                            this.next(1);
                        }
                    } else {
                        float turn =
                                Aim.turn(
                                        client,
                                        (float) Math.toDegrees(Math.atan2(-d, e)),
                                        player.getPitch(),
                                        22.0F);
                        client.options.forwardKey.setPressed(turn < 30.0F);
                        if (this.phaseTicks > 40) {
                            client.options.forwardKey.setPressed(false);
                            return Climber.Result.FAILED;
                        }
                    }
                    break;
                case 1:
                    Aim.look(client, player.getYaw(), 90.0F);
                    if (player.isOnGround()) {
                        client.options.sneakKey.setPressed(Aim.clickable(client, pos.down()));
                        client.options.jumpKey.setPressed(true);
                        this.next(2);
                    } else if (this.phaseTicks > 40) {
                        return Climber.Result.FAILED;
                    }
                    break;
                case 2:
                    Aim.look(client, player.getYaw(), 90.0F);
                    if (!player.isOnGround()) {
                        client.options.jumpKey.setPressed(false);
                    }
                    if (player.getY() >= pos.getY() + 1.02) {
                        client.options.jumpKey.setPressed(false);
                        BlockHitResult blockHitResult =
                                new BlockHitResult(
                                        new Vec3d(this.x + 0.5, pos.getY(), this.z + 0.5),
                                        Direction.UP,
                                        pos.down(),
                                        false);
                        if (client.interactionManager
                                .interactBlock(player, Hand.MAIN_HAND, blockHitResult)
                                .isAccepted()) {
                            player.swingHand(Hand.MAIN_HAND);
                            this.scaffold.add(pos.asLong());
                        }
                        client.options.sneakKey.setPressed(false);
                        this.next(3);
                    } else if (player.isOnGround() && this.phaseTicks > 4 || this.phaseTicks > 40) {
                        client.options.jumpKey.setPressed(false);
                        if (++this.tries > 3) {
                            return Climber.Result.FAILED;
                        }
                        this.next(0);
                    }
                    break;
                case 3:
                    Aim.look(client, player.getYaw(), 90.0F);
                    boolean block = Scaffold.block(client.world.getBlockState(pos));
                    boolean bl2 =
                            Math.abs(this.x + 0.5 - player.getX()) < 0.5
                                    && Math.abs(this.z + 0.5 - player.getZ()) < 0.5;
                    if (block && bl2 && player.isOnGround() && player.getY() >= pos.getY() + 0.99) {
                        this.placed++;
                        SchematicBuilderModule.trace(
                                "climber base "
                                        + this.baseY
                                        + " placed "
                                        + this.placed
                                        + " at y "
                                        + player.getY());
                        this.tries = 0;
                        if (this.placed >= this.height) {
                            for (int i = this.baseY + this.height - 2; i >= this.baseY - 2; i--) {
                                BlockPos pos2 = new BlockPos(this.x, i, this.z);
                                if (this.scaffold.contains(pos2.asLong())
                                        && Scaffold.block(client.world.getBlockState(pos2))) {
                                    this.toBreak.add(pos2);
                                } else if (!client.world.getBlockState(pos2).isAir()) {
                                    break;
                                }
                            }
                            return this.breakBelow(client);
                        }
                        this.next(0);
                    } else if (this.phaseTicks > 40
                            || player.isOnGround() && !bl2 && this.phaseTicks > 4) {
                        SchematicBuilderModule.trace(
                                "climber retry base "
                                        + this.baseY
                                        + " placed "
                                        + this.placed
                                        + " there "
                                        + block
                                        + " over "
                                        + bl2
                                        + " y "
                                        + player.getY());
                        if (!block) {
                            this.scaffold.remove(pos.asLong());
                        }
                        if (!bl2) {
                            return Climber.Result.FAILED;
                        }
                        if (++this.tries > 3) {
                            return Climber.Result.FAILED;
                        }
                        this.next(1);
                    }
                    break;
                default:
                    return Climber.Result.FAILED;
            }
            return Climber.Result.RUNNING;
        }
    }

    void release(MinecraftClient client) {
        client.options.sneakKey.setPressed(false);
        client.options.jumpKey.setPressed(false);
        client.options.forwardKey.setPressed(false);
    }

    private Climber.Result breakBelow(MinecraftClient client) {
        if (this.breaker == null) {
            if (this.toBreak.isEmpty()) {
                return Climber.Result.DONE;
            }
            this.breaker = new Breaker(this.toBreak.removeFirst(), true);
        }
        Breaker.Result result;
        if ((result = this.breaker.tick(client)) == Breaker.Result.RUNNING) {
            return Climber.Result.RUNNING;
        } else {
            if (result == Breaker.Result.DONE) {
                this.scaffold.remove(this.breaker.pos().asLong());
            }
            this.breaker = null;
            return this.toBreak.isEmpty() ? Climber.Result.DONE : Climber.Result.RUNNING;
        }
    }

    private void next(int phase2) {
        this.phase = phase2;
        this.phaseTicks = 0;
    }

    @Environment(EnvType.CLIENT)
    static enum Result {
        RUNNING,
        DONE,
        FAILED;

        private Result() {}
    }
}
