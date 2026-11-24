package net.efn.client.easyplace.builder;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Type;

@Environment(EnvType.CLIENT)
final class CellFiller {

    private static final int ANSWER_TICKS = 12;

    private static final int HOLD_TICKS = 20;

    private static final int TURN_TICKS = 30;

    private static final int TRIES = 2;

    private final BlockPos cell;

    private final CellFiller.Kind kind;

    private Aim.Shot shot;

    private int phase;

    private int phaseTicks;

    private int onAim;

    private int tries;

    CellFiller(BlockPos cell2, CellFiller.Kind kind2) {
        this.cell = cell2;
        this.kind = kind2;
    }

    BlockPos cell() {
        return this.cell;
    }

    CellFiller.Kind kind() {
        return this.kind;
    }

    static boolean filled(MinecraftClient client, BlockPos pos, CellFiller.Kind kind2) {
        BlockState blockState = client.world.getBlockState(pos);
        return switch (kind2) {
            case SUPPORT_LEAF -> Scaffold.block(blockState);
            case WATER_SOURCE ->
                    blockState.getFluidState().isStill()
                            && blockState.getFluidState().isIn(FluidTags.WATER);
            case WATERLOG ->
                    blockState.contains(Properties.WATERLOGGED)
                            && blockState.get(Properties.WATERLOGGED);
            case LAVA_SOURCE ->
                    blockState.getFluidState().isStill()
                            && blockState.getFluidState().isIn(FluidTags.LAVA);
            case REFILL -> false;
        };
    }

    static boolean reachable(MinecraftClient client, BlockPos pos, CellFiller.Kind kind2) {
        return aim(client, pos, kind2) != null;
    }

    CellFiller.Result tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null || client.interactionManager == null) {
            return CellFiller.Result.FAILED;
        } else if (filled(client, this.cell, this.kind)) {
            return CellFiller.Result.DONE;
        } else {
            this.phaseTicks++;
            switch (this.phase) {
                case 0:
                    if (this.kind != CellFiller.Kind.SUPPORT_LEAF
                            ? !Hands.hold(
                                    client, player, itemStack -> itemStack.isOf(this.kind.item))
                            : !Scaffold.hold(client, player)) {
                        if (this.phaseTicks > 20) {
                            return CellFiller.Result.FAILED;
                        }
                        return CellFiller.Result.RUNNING;
                    }
                    this.shot = aim(client, this.cell, this.kind);
                    if (this.shot == null) {
                        return CellFiller.Result.FAILED;
                    }
                    this.next(1);
                    break;
                case 1:
                    Aim.turn(client, this.shot.yaw(), this.shot.pitch(), 22.0F);
                    if (!Aim.looking(player, this.shot.yaw(), this.shot.pitch())) {
                        this.onAim = 0;
                        if (this.phaseTicks > 30) {
                            return CellFiller.Result.FAILED;
                        }
                        return CellFiller.Result.RUNNING;
                    }
                    if (++this.onAim < 2) {
                        return CellFiller.Result.RUNNING;
                    }
                    boolean bl =
                            this.kind == CellFiller.Kind.SUPPORT_LEAF
                                    ? client.interactionManager
                                            .interactBlock(player, Hand.MAIN_HAND, this.shot.hit())
                                            .isAccepted()
                                    : client.interactionManager
                                            .interactItem(player, Hand.MAIN_HAND)
                                            .isAccepted();
                    if (bl) {
                        player.swingHand(Hand.MAIN_HAND);
                    }
                    this.next(2);
                    break;
                case 2:
                    if (this.kind == CellFiller.Kind.REFILL
                            && player.getMainHandStack().isOf(Items.WATER_BUCKET)) {
                        return CellFiller.Result.DONE;
                    }
                    if (this.phaseTicks > 12) {
                        if (++this.tries >= 2) {
                            return CellFiller.Result.FAILED;
                        }
                        this.next(0);
                    }
                    break;
                default:
                    return CellFiller.Result.FAILED;
            }
            return CellFiller.Result.RUNNING;
        }
    }

    private void next(int phase2) {
        this.phase = phase2;
        this.phaseTicks = 0;
        this.onAim = 0;
    }

    private static Aim.Shot aim(MinecraftClient client, BlockPos pos, CellFiller.Kind kind2) {
        return switch (kind2) {
            case SUPPORT_LEAF -> Aim.against(client, pos, null);
            case WATER_SOURCE -> Aim.against(client, pos, Fluids.WATER);
            case WATERLOG -> Aim.at(client, pos);
            case LAVA_SOURCE -> Aim.against(client, pos, Fluids.LAVA);
            case REFILL -> Aim.water(client, pos);
        };
    }

    static boolean endless(ClientWorld world, BlockPos pos) {
        if (world.getBlockState(pos).isOf(Blocks.WATER) && world.getFluidState(pos).isStill()) {
            int i = 0;
            for (Direction direction : Type.HORIZONTAL) {
                FluidState fluidState = world.getFluidState(pos.offset(direction));
                if (fluidState.isStill() && fluidState.isIn(FluidTags.WATER)) {
                    i++;
                }
            }
            if (i < 2) {
                return false;
            } else {
                BlockState blockState = world.getBlockState(pos.down());
                return blockState.isSolid()
                        || blockState.getFluidState().isStill()
                                && blockState.getFluidState().isIn(FluidTags.WATER);
            }
        } else {
            return false;
        }
    }

    @Environment(EnvType.CLIENT)
    static enum Kind {
        SUPPORT_LEAF(Items.OAK_LEAVES),
        WATER_SOURCE(Items.WATER_BUCKET),
        WATERLOG(Items.WATER_BUCKET),
        LAVA_SOURCE(Items.LAVA_BUCKET),
        REFILL(Items.BUCKET);

        final Item item;

        private Kind(Item item2) {
            this.item = item2;
        }
    }

    @Environment(EnvType.CLIENT)
    static enum Result {
        RUNNING,
        DONE,
        FAILED;

        private Result() {}
    }
}
