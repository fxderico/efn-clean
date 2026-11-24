package net.efn.client.easyplace.builder;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.math.Vec3d;

import java.util.Set;

@Environment(EnvType.CLIENT)
final class Bridger {

    private static final int EDGE_TICKS = 40;

    private static final int HOLD_TICKS = 20;

    private static final int ANSWER_TICKS = 12;

    private static final int TURN_TICKS = 30;

    private static final int STEP_TICKS = 40;

    private static final int TRIES = 2;

    private static final double LEANING = 0.18;

    private final BlockPos from;

    private final BlockPos spot;

    private final BlockPos leaf;

    private final Direction way;

    private final Set<Long> scaffold;

    private final Item material;

    private boolean chained;

    private int phase;

    private int phaseTicks;

    private int stillTicks;

    private int onAim;

    private int clickedAt = -1;

    private int tries;

    private Vec3d lastPos;

    Bridger(BlockPos from2, BlockPos spot2, Set<Long> scaffold2, Item material2) {
        this.from = from2;
        this.spot = spot2;
        this.leaf = spot2.down();
        this.way =
                spot2.getX() > from2.getX()
                        ? Direction.EAST
                        : (spot2.getX() < from2.getX()
                                ? Direction.WEST
                                : (spot2.getZ() > from2.getZ()
                                        ? Direction.SOUTH
                                        : Direction.NORTH));
        this.scaffold = scaffold2;
        this.material = material2;
    }

    BlockPos spot() {
        return this.spot;
    }

    Bridger chained() {
        this.chained = true;
        return this;
    }

    Bridger.Result tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player != null && client.world != null && client.interactionManager != null) {
            client.options.sneakKey.setPressed(true);
            this.phaseTicks++;
            boolean bl =
                    this.material == null
                            ? Scaffold.block(client.world.getBlockState(this.leaf))
                            : !client.world.getBlockState(this.leaf).isReplaceable();
            switch (this.phase) {
                case 0:
                    if (bl) {
                        this.next(3);
                        return Bridger.Result.RUNNING;
                    }
                    if (this.material == null
                            ? !Scaffold.has(player)
                            : !Hands.has(player, itemStack -> itemStack.isOf(this.material))) {
                        return Bridger.Result.FAILED;
                    }
                    boolean bl2 =
                            this.material == null
                                    ? Scaffold.hold(client, player)
                                    : Hands.hold(
                                            client,
                                            player,
                                            itemStack -> itemStack.isOf(this.material));
                    if (!bl2) {
                        return this.phaseTicks > 20
                                ? Bridger.Result.FAILED
                                : Bridger.Result.RUNNING;
                    }
                    this.lastPos = player.getEntityPos();
                    this.next(1);
                    break;
                case 1:
                    double past = this.past(player.getEntityPos());
                    Vec3d entityPos = player.getEntityPos();
                    this.stillTicks =
                            entityPos.squaredDistanceTo(this.lastPos) < 1.0E-4
                                    ? this.stillTicks + 1
                                    : 0;
                    this.lastPos = entityPos;
                    if (past > 0.18 || past > 0.03 && this.stillTicks >= 3) {
                        client.options.forwardKey.setPressed(false);
                        this.next(2);
                        return Bridger.Result.RUNNING;
                    }
                    if (this.phaseTicks > 40) {
                        return Bridger.Result.FAILED;
                    }
                    float turn =
                            Aim.turn(client, this.way.getPositiveHorizontalDegrees(), 30.0F, 22.0F);
                    client.options.forwardKey.setPressed(turn < 20.0F);
                    break;
                case 2:
                    if (bl) {
                        if (this.material == null) {
                            this.scaffold.add(this.leaf.asLong());
                        }
                        this.next(3);
                        return Bridger.Result.RUNNING;
                    }
                    if (!player.isSneaking()) {
                        if (this.phaseTicks > 10) {
                            return this.retry(client);
                        }
                        return Bridger.Result.RUNNING;
                    }
                    if (this.clickedAt < 0) {
                        Aim.Shot shot = Aim.side(client, this.from.down(), this.way);
                        if (shot == null) {
                            return this.retry(client);
                        }
                        Aim.turn(client, shot.yaw(), shot.pitch(), 22.0F);
                        if (!Aim.looking(player, shot.yaw(), shot.pitch())) {
                            this.onAim = 0;
                            if (this.phaseTicks > 30) {
                                return this.retry(client);
                            }
                            return Bridger.Result.RUNNING;
                        }
                        if (++this.onAim < 2) {
                            return Bridger.Result.RUNNING;
                        }
                        ItemStack mainHandStack = player.getMainHandStack();
                        if (this.material == null
                                ? !Scaffold.item(mainHandStack)
                                : !mainHandStack.isOf(this.material)) {
                            if (++this.tries >= 2) {
                                return Bridger.Result.FAILED;
                            }
                            this.next(0);
                            return Bridger.Result.RUNNING;
                        }
                        if (client.interactionManager
                                .interactBlock(player, Hand.MAIN_HAND, shot.hit())
                                .isAccepted()) {
                            player.swingHand(Hand.MAIN_HAND);
                        }
                        this.clickedAt = this.phaseTicks;
                    } else if (this.phaseTicks - this.clickedAt > 12) {
                        return this.retry(client);
                    }
                    break;
                case 3:
                    Vec3d entityPos2 = player.getEntityPos();
                    double d = this.spot.getX() + 0.5 - entityPos2.x;
                    double e = this.spot.getZ() + 0.5 - entityPos2.z;
                    boolean bl3 = player.isOnGround() && player.getBlockPos().equals(this.spot);
                    if (bl3 && (this.chained || Math.abs(d) < 0.25 && Math.abs(e) < 0.25)) {
                        this.release(client);
                        return Bridger.Result.DONE;
                    }
                    if (this.phaseTicks > 40) {
                        return Bridger.Result.FAILED;
                    }
                    float turn2 =
                            Aim.turn(
                                    client,
                                    (float) Math.toDegrees(Math.atan2(-d, e)),
                                    30.0F,
                                    22.0F);
                    client.options.forwardKey.setPressed(turn2 < 30.0F);
                    break;
                default:
                    return Bridger.Result.FAILED;
            }
            return Bridger.Result.RUNNING;
        } else {
            return Bridger.Result.FAILED;
        }
    }

    private double past(Vec3d vec) {
        double d = this.way.getDirection() == AxisDirection.POSITIVE ? 1.0 : 0.0;
        return switch (this.way.getAxis()) {
            case X -> (vec.x - (this.from.getX() + d)) * this.way.getOffsetX();
            case Z -> (vec.z - (this.from.getZ() + d)) * this.way.getOffsetZ();
            default -> 0.0;
        };
    }

    private Bridger.Result retry(MinecraftClient client) {
        if (++this.tries >= 2) {
            return Bridger.Result.FAILED;
        } else {
            this.lastPos = client.player == null ? this.lastPos : client.player.getEntityPos();
            this.next(1);
            return Bridger.Result.RUNNING;
        }
    }

    private void next(int phase2) {
        this.phase = phase2;
        this.phaseTicks = 0;
        this.stillTicks = 0;
        this.onAim = 0;
        this.clickedAt = -1;
    }

    void release(MinecraftClient client) {
        client.options.forwardKey.setPressed(false);
        client.options.sneakKey.setPressed(false);
    }

    @Environment(EnvType.CLIENT)
    static enum Result {
        RUNNING,
        DONE,
        FAILED;

        private Result() {}
    }
}
