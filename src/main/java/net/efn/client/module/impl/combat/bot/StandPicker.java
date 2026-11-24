package net.efn.client.module.impl.combat.bot;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Environment(EnvType.CLIENT)
final class StandPicker {

    private static final float BLAST_PROOF = 600.0F;

    private static final double[] RADII = new double[] {1.5, 3.0, 4.0};

    private static final int HOLE_SEARCH = 4;

    private static final int MAX_HOLES = 6;

    private StandPicker() {}

    static StandPicker.Spot pick(
            World world,
            PlayerEntity player,
            PlayerEntity player2,
            double d,
            double e,
            double d2,
            double e2) {
        BlockPos pos = player.getBlockPos();
        ArrayList list = new ArrayList();
        list.add(pos);
        for (double d3 : RADII) {
            for (int i = 0; i < 8; i++) {
                double e3 = 0.7853981633974483 * i;
                list.add(
                        BlockPos.ofFloored(
                                player.getX() + Math.cos(e3) * d3,
                                player.getY(),
                                player.getZ() + Math.sin(e3) * d3));
            }
        }
        list.addAll(holesNear(world, pos));
        StandPicker.Spot spot = null;
        double d4 = -1.0 / 0.0;
        for (BlockPos pos2 : (Iterable<BlockPos>) list) {
            BlockPos pos3 = standable(world, pos2);
            if (pos3 != null) {
                double score2 = score(world, player, player2, pos3, d, e, d2, e2);
                if (pos3.equals(pos)) {
                    d4 = Math.max(d4, score2);
                }
                if (spot == null || score2 > spot.score()) {
                    spot = new StandPicker.Spot(pos3, score2);
                }
            }
        }
        return spot != null && !spot.feet().equals(pos) && !(spot.score() < d4 + 0.5) ? spot : null;
    }

    static double hole(World world, BlockPos pos) {
        int i = 0;
        for (Direction direction : Type.HORIZONTAL) {
            if (world.getBlockState(pos.offset(direction)).getBlock().getBlastResistance()
                    >= 600.0F) {
                i++;
            }
        }
        return i == 4 ? 1.0 : (i == 3 ? 0.4 : 0.0);
    }

    private static double score(
            World world,
            PlayerEntity player,
            PlayerEntity player2,
            BlockPos pos,
            double d,
            double e,
            double d2,
            double e2) {
        Vec3d vec = Vec3d.ofBottomCenter(pos);
        double d3 = Math.hypot(player2.getX() - vec.x, player2.getZ() - vec.z);
        if (!(d3 < d - 0.5) && !(d3 > d2 + 1.5)) {
            double e3 = Math.hypot(player.getX() - vec.x, player.getZ() - vec.z);
            return -Math.abs(d3 - e) + 3.0 * e2 * hole(world, pos) - 0.15 * e3;
        } else {
            return -10.0 - Math.abs(d3 - e);
        }
    }

    static BlockPos standable(World world, BlockPos pos) {
        for (int i : new int[] {0, -1, 1}) {
            BlockPos pos2 = pos.up(i);
            BlockPos pos3 = pos2.down();
            BlockState blockState = world.getBlockState(pos3);
            if (!blockState.getCollisionShape(world, pos3).isEmpty()
                    && !harmful(blockState)
                    && !blockState.isOf(Blocks.MAGMA_BLOCK)
                    && clear(world, pos2)
                    && clear(world, pos2.up())) {
                return pos2;
            }
        }
        return null;
    }

    private static boolean clear(World world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        return blockState.getCollisionShape(world, pos).isEmpty()
                && !harmful(blockState)
                && world.getFluidState(pos).isEmpty();
    }

    private static boolean harmful(BlockState state) {
        return state.isOf(Blocks.LAVA)
                || state.isIn(BlockTags.FIRE)
                || state.isOf(Blocks.CACTUS)
                || state.isOf(Blocks.SWEET_BERRY_BUSH)
                || state.isOf(Blocks.COBWEB)
                || state.isOf(Blocks.POWDER_SNOW);
    }

    private static List<BlockPos> holesNear(World world, BlockPos pos) {
        ArrayList list = new ArrayList();
        for (BlockPos pos2 : BlockPos.iterate(pos.add(-4, -1, -4), pos.add(4, 1, 4))) {
            if (hole(world, pos2) >= 1.0
                    && standable(world, pos2) != null
                    && standable(world, pos2).equals(pos2)) {
                list.add(pos2.toImmutable());
            }
        }
        list.sort(Comparator.comparingDouble((BlockPos pos3) -> pos3.getSquaredDistance(pos)));
        return (List<BlockPos>) (list.size() > 6 ? list.subList(0, 6) : list);
    }

    @Environment(EnvType.CLIENT)
    record Spot(BlockPos feet, double score) {

        Vec3d center() {
            return Vec3d.ofBottomCenter(this.feet);
        }
    }
}
