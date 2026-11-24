package net.efn.client.easyplace.builder;

import fi.dy.masa.litematica.world.WorldSchematic;

import net.efn.client.easyplace.util.BlockStateUtil;
import net.efn.client.easyplace.util.StateRules;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.AbstractPressurePlateBlock;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.AbstractRedstoneGateBlock;
import net.minecraft.block.AbstractTorchBlock;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.BeehiveBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CarpetBlock;
import net.minecraft.block.CarvedPumpkinBlock;
import net.minecraft.block.DaylightDetectorBlock;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.GlazedTerracottaBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.LoomBlock;
import net.minecraft.block.ObserverBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.RedstoneTorchBlock;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.StonecutterBlock;
import net.minecraft.block.TntBlock;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.WallRedstoneTorchBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.RailShape;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.math.Direction.Type;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
final class BuildOrder {

    static final int STAGE_NORMAL = 0;

    static final int STAGE_PISTON = 1;

    static final int STAGE_OBSERVER = 2;

    static final int STAGE_POWER = 3;

    private static final int MAX_TRACK = 512;

    private BuildOrder() {}

    static boolean powerOrTnt(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.REDSTONE_BLOCK
                || block instanceof RedstoneTorchBlock
                || block instanceof DaylightDetectorBlock
                || block instanceof TntBlock;
    }

    static boolean late(WorldSchematic worldSchematic, BlockPos pos, BlockState state, boolean bl) {
        return stage(worldSchematic, pos, state, bl) > 0;
    }

    static int stage(WorldSchematic worldSchematic, BlockPos pos, BlockState state, boolean bl) {
        int ownStage = ownStage(worldSchematic, pos, state, bl);
        if (ownStage != 0) {
            return ownStage;
        } else {
            BlockPos pos2 = hangsOn(state, pos);
            return pos2 == null
                    ? 0
                    : ownStage(worldSchematic, pos2, worldSchematic.getBlockState(pos2), bl);
        }
    }

    private static int ownStage(
            WorldSchematic worldSchematic, BlockPos pos, BlockState state, boolean bl) {
        if (powerOrTnt(state) || pushedIntoTnt(worldSchematic, pos, state) != null) {
            return 3;
        } else if (!bl) {
            return 0;
        } else {
            Block block = state.getBlock();
            if (block instanceof PistonBlock) {
                return 1;
            } else {
                return block instanceof ObserverBlock ? 2 : 0;
            }
        }
    }

    static boolean pistonWouldFire(ClientWorld world, BlockPos pos) {
        return world.isReceivingRedstonePower(pos) || world.isReceivingRedstonePower(pos.up());
    }

    static BlockPos pushedIntoTnt(WorldSchematic worldSchematic, BlockPos pos, BlockState state) {
        if (!state.isAir() && !(state.getBlock() instanceof TntBlock)) {
            for (Direction direction : Direction.values()) {
                BlockState blockState =
                        worldSchematic.getBlockState(pos.offset(direction.getOpposite()));
                BlockPos pos2;
                if (blockState.getBlock() instanceof PistonBlock
                        && blockState.get(Properties.FACING) == direction
                        && worldSchematic.getBlockState(pos2 = pos.offset(direction)).getBlock()
                                instanceof TntBlock) {
                    return pos2;
                }
            }
            return null;
        } else {
            return null;
        }
    }

    static BlockPos railBefore(
            ClientWorld world,
            WorldSchematic worldSchematic,
            BlockPos pos,
            BlockState state,
            Predicate<BlockPos> predicate) {
        List list = track(worldSchematic, pos);
        int i = list.indexOf(pos);
        if (i <= 0) {
            return null;
        } else {
            BlockPos pos2 = (BlockPos) list.get(i - 1);
            return !predicate.test(pos2)
                            && !(world.getBlockState(pos2).getBlock() instanceof AbstractRailBlock)
                    ? pos2
                    : null;
        }
    }

    static boolean railShapeOff(BlockState state, BlockState state2) {
        RailShape railShape = railShape(state2);
        RailShape railShape2 = railShape(state);
        return railShape != null && railShape2 != null && railShape != railShape2;
    }

    private static List<BlockPos> track(WorldSchematic worldSchematic, BlockPos pos) {
        HashSet set = new HashSet();
        ArrayList list = new ArrayList<>(List.of(pos));
        BlockPos pos2 = null;
        while (!list.isEmpty() && set.size() < 512) {
            BlockPos pos3 = (BlockPos) list.remove(list.size() - 1);
            if (set.add(pos3)) {
                List list2 = links(worldSchematic, pos3);
                if (list2.size() < 2 && (pos2 == null || before(pos3, pos2))) {
                    pos2 = pos3;
                }
                list.addAll(list2);
            }
        }
        if (pos2 == null) {
            for (BlockPos pos4 : (Iterable<BlockPos>) set) {
                if (pos2 == null || before(pos4, pos2)) {
                    pos2 = pos4;
                }
            }
        }
        ArrayList list3 = new ArrayList();
        HashSet set2 = new HashSet();
        BlockPos pos5 = pos2;
        while (pos5 != null && set2.add(pos5)) {
            list3.add(pos5);
            BlockPos pos6 = null;
            for (BlockPos pos7 : links(worldSchematic, pos5)) {
                if (!set2.contains(pos7) && (pos6 == null || before(pos7, pos6))) {
                    pos6 = pos7;
                }
            }
            pos5 = pos6;
        }
        return list3;
    }

    private static boolean before(BlockPos pos, BlockPos pos2) {
        if (pos.getY() != pos2.getY()) {
            return pos.getY() < pos2.getY();
        } else {
            return pos.getX() != pos2.getX() ? pos.getX() < pos2.getX() : pos.getZ() < pos2.getZ();
        }
    }

    private static List<BlockPos> links(WorldSchematic worldSchematic, BlockPos pos) {
        ArrayList list = new ArrayList(2);
        RailShape railShape = railShape(worldSchematic.getBlockState(pos));
        if (railShape == null) {
            return list;
        } else {
            for (BlockPos pos2 : ends(pos, railShape)) {
                BlockPos pos3 = railAt(worldSchematic, pos2);
                if (pos3 != null) {
                    RailShape railShape2 = railShape(worldSchematic.getBlockState(pos3));
                    for (BlockPos pos4 : ends(pos3, railShape2)) {
                        if (pos.equals(railAt(worldSchematic, pos4))) {
                            list.add(pos3);
                            break;
                        }
                    }
                }
            }
            return list;
        }
    }

    private static BlockPos railAt(WorldSchematic worldSchematic, BlockPos pos) {
        if (railShape(worldSchematic.getBlockState(pos)) != null) {
            return pos;
        } else {
            return railShape(worldSchematic.getBlockState(pos.down())) != null ? pos.down() : null;
        }
    }

    private static List<BlockPos> ends(BlockPos pos, RailShape railShape) {
        return switch (railShape) {
            case NORTH_SOUTH -> List.of(pos.north(), pos.south());
            case EAST_WEST -> List.of(pos.east(), pos.west());
            case ASCENDING_EAST -> List.of(pos.east().up(), pos.west());
            case ASCENDING_WEST -> List.of(pos.west().up(), pos.east());
            case ASCENDING_NORTH -> List.of(pos.north().up(), pos.south());
            case ASCENDING_SOUTH -> List.of(pos.south().up(), pos.north());
            case SOUTH_EAST -> List.of(pos.south(), pos.east());
            case SOUTH_WEST -> List.of(pos.south(), pos.west());
            case NORTH_WEST -> List.of(pos.north(), pos.west());
            case NORTH_EAST -> List.of(pos.north(), pos.east());
        };
    }

    private static RailShape railShape(BlockState state) {
        if (!(state.getBlock() instanceof AbstractRailBlock abstractRailBlock)) {
            return null;
        } else {
            Property<RailShape> shapeProperty = abstractRailBlock.getShapeProperty();
            return state.contains(shapeProperty) ? state.get(shapeProperty) : null;
        }
    }

    static boolean lookOnly(BlockState state) {
        Block block = state.getBlock();
        return block instanceof StairsBlock
                || block instanceof AbstractFurnaceBlock
                || block instanceof BarrelBlock
                || block instanceof PillarBlock
                || block instanceof CarvedPumpkinBlock
                || block instanceof GlazedTerracottaBlock
                || block instanceof FenceGateBlock
                || block instanceof LoomBlock
                || block instanceof StonecutterBlock
                || block instanceof LecternBlock
                || block instanceof BeehiveBlock
                || block instanceof AnvilBlock;
    }

    static List<BlockState> turnedVariants(BlockState state) {
        ArrayList list = new ArrayList();
        for (Property property :
                List.of(
                        Properties.FACING,
                        Properties.HORIZONTAL_FACING,
                        Properties.AXIS,
                        Properties.HORIZONTAL_AXIS)) {
            if (state.contains(property)) {
                for (Object value : property.getValues()) {
                    BlockState state2 = with(state, property, value);
                    if (!state2.equals(state)) {
                        list.add(state2);
                    }
                }
            }
        }
        return list;
    }

    private static BlockState with(BlockState state, Property property, Object object) {
        return state.with(property, (Comparable) object);
    }

    static BlockPos hangsOn(BlockState state, BlockPos pos) {
        Block block = state.getBlock();
        if (block instanceof WallTorchBlock
                || block instanceof WallRedstoneTorchBlock
                || block instanceof WallSignBlock
                || block instanceof LadderBlock) {
            return pos.offset(state.get(Properties.HORIZONTAL_FACING).getOpposite());
        } else if (block instanceof WallMountedBlock) {
            return switch ((BlockFace) state.get(Properties.BLOCK_FACE)) {
                case FLOOR -> pos.down();
                case CEILING -> pos.up();
                case WALL -> pos.offset(state.get(Properties.HORIZONTAL_FACING).getOpposite());
            };
        } else {
            return !(block instanceof RedstoneWireBlock)
                            && !(block instanceof AbstractRedstoneGateBlock)
                            && !(block instanceof AbstractRailBlock)
                            && !(block instanceof CarpetBlock)
                            && !(block instanceof AbstractPressurePlateBlock)
                            && !(block instanceof AbstractTorchBlock)
                    ? null
                    : pos.down();
        }
    }

    static List<BlockPos> supportCells(BlockState state, BlockPos pos) {
        Direction direction = StateRules.lookFor(state);
        if (direction == null) {
            direction = StateRules.flatLookFor(state);
        }
        if (direction != null) {
            ArrayList list = new ArrayList<>(List.of(pos.offset(direction), pos.down()));
            for (Direction direction2 : Type.HORIZONTAL) {
                if (direction2 != direction) {
                    list.add(pos.offset(direction2));
                }
            }
            if (direction != Direction.UP) {
                list.add(pos.up());
            }
            return list;
        } else {
            Block block = state.getBlock();
            if (block instanceof HopperBlock) {
                Direction direction3 = state.get(Properties.HOPPER_FACING);
                return direction3 == Direction.DOWN
                        ? List.of(pos.down(), pos.up())
                        : List.of(pos.offset(direction3));
            } else if (hangsOn(state, pos) != null) {
                return List.of();
            } else if (state.contains(Properties.AXIS)) {
                Axis axis = state.get(Properties.AXIS);
                return List.of(
                        pos.offset(Direction.from(axis, AxisDirection.NEGATIVE)),
                        pos.offset(Direction.from(axis, AxisDirection.POSITIVE)));
            } else if (state.contains(Properties.SLAB_TYPE)) {
                return state.get(Properties.SLAB_TYPE) == SlabType.TOP
                        ? List.of(pos.up())
                        : List.of(pos.down());
            } else if (state.contains(Properties.BLOCK_HALF)) {
                return state.get(Properties.BLOCK_HALF) == BlockHalf.TOP
                        ? List.of(pos.up())
                        : List.of(pos.down());
            } else {
                return List.of(
                        pos.down(), pos.north(), pos.south(), pos.east(), pos.west(), pos.up());
            }
        }
    }

    static boolean waits(
            ClientWorld world,
            WorldSchematic worldSchematic,
            BlockPos pos,
            BlockState state,
            Predicate<BlockPos> predicate,
            boolean bl) {
        Block block = state.getBlock();
        BlockPos pos2 = pushedIntoTnt(worldSchematic, pos, state);
        if (pos2 != null && !(world.getBlockState(pos2).getBlock() instanceof TntBlock)) {
            return true;
        } else if (block instanceof PistonBlock) {
            return pistonWouldFire(world, pos);
        } else if (block instanceof AbstractRailBlock
                && railBefore(world, worldSchematic, pos, state, predicate) != null) {
            return true;
        } else if (block instanceof ObserverBlock) {
            BlockPos pos3 = pos.offset(state.get(Properties.FACING));
            BlockState blockState = worldSchematic.getBlockState(pos3);
            if (BlockStateUtil.requiredItem(blockState) == null
                    || stage(worldSchematic, pos3, blockState, bl) > 2
                    || predicate.test(pos3)) {
                return false;
            } else {
                return blockState.getBlock() instanceof ObserverBlock
                                && pos3.offset(blockState.get(Properties.FACING)).equals(pos)
                        ? false
                        : !StateRules.placed(world.getBlockState(pos3), blockState);
            }
        } else if (block instanceof FallingBlock) {
            return FallingBlock.canFallThrough(world.getBlockState(pos.down()));
        } else if (!state.contains(Properties.WATERLOGGED)
                && state.getFluidState().isStill()
                && state.getFluidState().isIn(FluidTags.WATER)) {
            return !world.getFluidState(pos).isStill()
                    || !world.getFluidState(pos).isIn(FluidTags.WATER);
        } else {
            return block instanceof TntBlock ? world.isReceivingRedstonePower(pos) : false;
        }
    }
}
