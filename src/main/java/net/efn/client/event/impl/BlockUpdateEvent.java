package net.efn.client.event.impl;

import net.efn.client.event.Event;
import net.efn.client.event.EventType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public final class BlockUpdateEvent extends Event {

    public static final EventType<BlockUpdateEvent> TYPE = new EventType<>("BlockUpdateEvent");

    public BlockPos pos;

    public BlockState oldState;

    public BlockState newState;

    private static final BlockUpdateEvent INSTANCE = new BlockUpdateEvent();

    private BlockUpdateEvent() {}

    public static void dispatch(BlockPos pos2, BlockState state, BlockState state2) {
        if (TYPE.hasPreListeners()) {
            INSTANCE.pos = pos2;
            INSTANCE.oldState = state;
            INSTANCE.newState = state2;
            TYPE.dispatch(INSTANCE);
            INSTANCE.pos = null;
            INSTANCE.oldState = null;
            INSTANCE.newState = null;
        }
    }
}
