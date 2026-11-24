package net.efn.client.easyplace.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.AbstractRedstoneGateBlock;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.ConnectingBlock;
import net.minecraft.block.CrafterBlock;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.MultifaceBlock;
import net.minecraft.block.ObserverBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.Direction;

import java.util.Map;
import java.util.Set;

@Environment(EnvType.CLIENT)
public final class StateRules {

    private static final Set<Property<?>> PLACED =
            Set.of(
                    Properties.FACING,
                    Properties.HORIZONTAL_FACING,
                    Properties.HOPPER_FACING,
                    Properties.VERTICAL_DIRECTION,
                    Properties.AXIS,
                    Properties.HORIZONTAL_AXIS,
                    Properties.BLOCK_HALF,
                    Properties.DOUBLE_BLOCK_HALF,
                    Properties.SLAB_TYPE,
                    Properties.ROTATION,
                    Properties.BLOCK_FACE,
                    Properties.ORIENTATION,
                    Properties.DOOR_HINGE,
                    Properties.CHEST_TYPE,
                    Properties.BED_PART,
                    Properties.ATTACHMENT,
                    Properties.HANGING);

    private static final Set<Property<?>> CLICKED =
            Set.of(
                    Properties.DELAY,
                    Properties.COMPARATOR_MODE,
                    Properties.NOTE,
                    Properties.OPEN,
                    Properties.INVERTED);

    private static final Map<Block, Block> TIPS =
            Map.of(
                    Blocks.KELP_PLANT,
                    Blocks.KELP,
                    Blocks.TWISTING_VINES_PLANT,
                    Blocks.TWISTING_VINES,
                    Blocks.WEEPING_VINES_PLANT,
                    Blocks.WEEPING_VINES,
                    Blocks.CAVE_VINES_PLANT,
                    Blocks.CAVE_VINES);

    private StateRules() {}

    public static Block plant(Block block) {
        return TIPS.getOrDefault(block, block);
    }

    public static boolean placed(BlockState state, BlockState state2) {
        if (plant(state.getBlock()) != plant(state2.getBlock())) {
            return false;
        } else {
            for (Property property : state2.getProperties()) {
                if (state.contains(property)
                        && (PLACED.contains(property) || faceProperty(state2, property))
                        && !state.get(property).equals(state2.get(property))) {
                    return false;
                }
            }
            return true;
        }
    }

    public static boolean done(BlockState state, BlockState state2) {
        if (!placed(state, state2)) {
            return false;
        } else {
            for (Property property : state2.getProperties()) {
                if (clickable(state2, property)
                        && state.contains(property)
                        && !state.get(property).equals(state2.get(property))) {
                    return false;
                }
            }
            return true;
        }
    }

    public static boolean needsClicks(BlockState state, BlockState state2) {
        return placed(state, state2) && !done(state, state2);
    }

    public static boolean wouldFit(BlockState state, BlockState state2, boolean bl) {
        if (plant(state.getBlock()) != plant(state2.getBlock())) {
            return false;
        } else {
            for (Property property : state2.getProperties()) {
                if (state.contains(property)
                        && (!bl || property != Properties.CHEST_TYPE)
                        && (property != Properties.SLAB_TYPE
                                || state2.get(Properties.SLAB_TYPE) != SlabType.DOUBLE)
                        && (faceProperty(state2, property)
                                ? state.<Boolean>get(property) && !state2.<Boolean>get(property)
                                : PLACED.contains(property)
                                        && !state.get(property).equals(state2.get(property)))) {
                    return false;
                }
            }
            return true;
        }
    }

    public static boolean slabHalf(BlockState state, BlockState state2) {
        return state2.getBlock() instanceof SlabBlock
                && state.getBlock() == state2.getBlock()
                && state2.get(Properties.SLAB_TYPE) == SlabType.DOUBLE
                && state.get(Properties.SLAB_TYPE) != SlabType.DOUBLE;
    }

    public static int itemsNeeded(BlockState state, BlockState state2) {
        if (state2.getBlock() instanceof SlabBlock
                && state2.get(Properties.SLAB_TYPE) == SlabType.DOUBLE) {
            return slabHalf(state, state2) ? 1 : 2;
        } else {
            return 1;
        }
    }

    public static boolean clickable(BlockState state, Property<?> property) {
        if (!CLICKED.contains(property)) {
            return false;
        } else if (property != Properties.OPEN) {
            return true;
        } else {
            Block block = state.getBlock();
            return block != Blocks.IRON_DOOR && block != Blocks.IRON_TRAPDOOR
                    ? block instanceof DoorBlock
                            || block instanceof TrapdoorBlock
                            || block instanceof FenceGateBlock
                    : false;
        }
    }

    public static Direction lookFor(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CrafterBlock) {
            return state.get(Properties.ORIENTATION).getFacing().getOpposite();
        } else if (block instanceof ObserverBlock) {
            return state.get(Properties.FACING);
        } else {
            return !(block instanceof PistonBlock)
                            && !(block instanceof DispenserBlock)
                            && !(block instanceof BarrelBlock)
                    ? null
                    : state.get(Properties.FACING).getOpposite();
        }
    }

    public static Direction flatLookFor(BlockState state) {
        if (!state.contains(Properties.HORIZONTAL_FACING)) {
            return null;
        } else {
            Block block = state.getBlock();
            Direction direction = state.get(Properties.HORIZONTAL_FACING);
            if (block instanceof ChestBlock
                    || block instanceof AbstractFurnaceBlock
                    || block instanceof AbstractRedstoneGateBlock) {
                return direction.getOpposite();
            } else {
                return !(block instanceof StairsBlock)
                                && !(block instanceof DoorBlock)
                                && !(block instanceof FenceGateBlock)
                        ? null
                        : direction;
            }
        }
    }

    private static boolean faceProperty(BlockState state, Property<?> property) {
        Block block = state.getBlock();
        return (block instanceof MultifaceBlock || block instanceof VineBlock)
                && property instanceof BooleanProperty
                && ConnectingBlock.FACING_PROPERTIES.containsValue(property);
    }
}
