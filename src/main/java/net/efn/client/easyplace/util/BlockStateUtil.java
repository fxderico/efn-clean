package net.efn.client.easyplace.util;

import net.efn.client.easyplace.schematic.TargetStatus;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public final class BlockStateUtil {

    private BlockStateUtil() {}

    public static boolean statesMatch(BlockState state, BlockState state2) {
        return StateRules.placed(state2, state) || StateRules.slabHalf(state2, state);
    }

    public static TargetStatus classify(BlockState state, BlockState state2) {
        if (StateRules.done(state2, state)) {
            return TargetStatus.CORRECT;
        } else if (StateRules.placed(state2, state)) {
            return TargetStatus.ADJUST;
        } else {
            return !state2.isAir() && !state2.isReplaceable() && !StateRules.slabHalf(state2, state)
                    ? TargetStatus.WRONG
                    : TargetStatus.MISSING;
        }
    }

    public static Item requiredItem(BlockState state) {
        Item item = StateRules.plant(state.getBlock()).asItem();
        return item == Items.AIR ? null : item;
    }

    public static boolean isPrimaryPlacementState(BlockState state) {
        return state.contains(Properties.DOUBLE_BLOCK_HALF)
                        && state.get(Properties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER
                ? false
                : !state.contains(Properties.BED_PART)
                        || state.get(Properties.BED_PART) != BedPart.HEAD;
    }

    public static boolean isSimpleFullBlock(BlockState state, World world, BlockPos pos) {
        return state.isFullCube(world, pos) && state.getBlock().asItem() != Items.AIR;
    }
}
