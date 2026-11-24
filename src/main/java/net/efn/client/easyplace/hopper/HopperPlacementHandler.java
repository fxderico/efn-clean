package net.efn.client.easyplace.hopper;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.HopperBlock;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;

@Environment(EnvType.CLIENT)
public final class HopperPlacementHandler {

    public HopperPlacementHandler() {}

    public boolean isHopper(BlockState state) {
        return state.getBlock() instanceof HopperBlock;
    }

    public boolean faceCanProduceExpectedFacing(BlockState state, Direction direction) {
        if (this.isHopper(state) && state.contains(Properties.HOPPER_FACING)) {
            Direction direction2 = state.get(Properties.HOPPER_FACING);
            Direction opposite = direction.getOpposite();
            if (opposite.getAxis() == Axis.Y) {
                opposite = Direction.DOWN;
            }
            return opposite == direction2;
        } else {
            return true;
        }
    }

    public int facingScore(BlockState state, Direction direction) {
        return this.faceCanProduceExpectedFacing(state, direction) ? 1000 : -1000;
    }
}
