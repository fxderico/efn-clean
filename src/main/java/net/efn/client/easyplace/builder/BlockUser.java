package net.efn.client.easyplace.builder;

import net.efn.client.easyplace.util.StateRules;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.CrafterScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
final class BlockUser {

    private static final int CLICK_LIMIT = 30;

    private static final int ANSWER_TICKS = 12;

    private static final int OPEN_TICKS = 40;

    private static final int SYNC_TICKS = 4;

    private final BlockPos pos;

    private final BlockState expected;

    private final boolean[] disabledSlots;

    private Aim.Shot shot;

    private BlockState before;

    private int phase;

    private int phaseTicks;

    private int clicks;

    private int rounds;

    private boolean lever;

    private BlockUser(BlockPos pos2, BlockState expected2, boolean[] disabledSlots2) {
        this.pos = pos2;
        this.expected = expected2;
        this.disabledSlots = disabledSlots2;
    }

    static BlockUser adjust(BlockPos pos2, BlockState state) {
        return new BlockUser(pos2, state, null);
    }

    static BlockUser crafter(BlockPos pos2, boolean[] booleans) {
        return new BlockUser(pos2, null, booleans);
    }

    static BlockUser lever(BlockPos pos2, BlockState state) {
        BlockUser blockUser = new BlockUser(pos2, state, null);
        blockUser.lever = true;
        return blockUser;
    }

    BlockPos pos() {
        return this.pos;
    }

    boolean crafter() {
        return this.disabledSlots != null;
    }

    BlockUser.Result tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player != null && client.world != null && client.interactionManager != null) {
            this.phaseTicks++;
            BlockState blockState = client.world.getBlockState(this.pos);
            if (this.lever) {
                if (!blockState.contains(Properties.POWERED)) {
                    return BlockUser.Result.FAILED;
                }
                if (blockState.get(Properties.POWERED) == this.expected.get(Properties.POWERED)) {
                    return BlockUser.Result.DONE;
                }
            } else if (this.disabledSlots == null && StateRules.done(blockState, this.expected)) {
                return BlockUser.Result.DONE;
            }
            switch (this.phase) {
                case 0:
                    if (player.isSneaking()
                            || player.currentScreenHandler != player.playerScreenHandler) {
                        return BlockUser.Result.RUNNING;
                    }
                    this.shot = Aim.at(client, this.pos);
                    if (this.shot == null) {
                        return BlockUser.Result.FAILED;
                    }
                    emptyHand(player);
                    this.next(1);
                    break;
                case 1:
                    Aim.turn(client, this.shot.yaw(), this.shot.pitch(), 22.0F);
                    if (Aim.looking(player, this.shot.yaw(), this.shot.pitch())
                            && this.phaseTicks >= 2) {
                        this.before = client.world.getBlockState(this.pos);
                        if (!client.interactionManager
                                .interactBlock(player, Hand.MAIN_HAND, this.shot.hit())
                                .isAccepted()) {
                            return BlockUser.Result.FAILED;
                        }
                        player.swingHand(Hand.MAIN_HAND);
                        this.clicks++;
                        this.next(this.disabledSlots == null ? 2 : 3);
                        break;
                    }
                    if (this.phaseTicks > 30) {
                        return BlockUser.Result.FAILED;
                    }
                    return BlockUser.Result.RUNNING;
                case 2:
                    if (!client.world.getBlockState(this.pos).equals(this.before)
                            || this.phaseTicks > 12) {
                        if (this.clicks >= 30) {
                            return BlockUser.Result.FAILED;
                        }
                        this.next(0);
                    }
                    break;
                case 3:
                    if (player.currentScreenHandler instanceof CrafterScreenHandler
                            && this.phaseTicks >= 4) {
                        this.next(4);
                    } else if (this.phaseTicks > 40) {
                        return BlockUser.Result.FAILED;
                    }
                    break;
                case 4:
                    if (!(player.currentScreenHandler
                            instanceof CrafterScreenHandler crafterScreenHandler)) {
                        return BlockUser.Result.FAILED;
                    }
                    boolean bl = false;
                    for (int i = 0; i < 9; i++) {
                        if (crafterScreenHandler.isSlotDisabled(i) != this.disabledSlots[i]
                                && !crafterScreenHandler.getSlot(i).hasStack()) {
                            boolean bl2 = !this.disabledSlots[i];
                            crafterScreenHandler.setSlotEnabled(i, bl2);
                            client.interactionManager.slotChangedState(
                                    i, crafterScreenHandler.syncId, bl2);
                            bl = true;
                        }
                    }
                    this.next(bl ? 5 : 6);
                    break;
                case 5:
                    if (this.phaseTicks >= 12) {
                        this.next(6);
                    }
                    break;
                case 6:
                    ScreenHandler currentScreenHandler = player.currentScreenHandler;
                    CrafterScreenHandler crafterScreenHandler2;
                    boolean bl3 =
                            currentScreenHandler instanceof CrafterScreenHandler
                                    && this.matches(
                                            crafterScreenHandler2 =
                                                    (CrafterScreenHandler) currentScreenHandler);
                    player.closeHandledScreen();
                    if (bl3) {
                        return BlockUser.Result.DONE;
                    }
                    if (++this.rounds >= 2) {
                        return BlockUser.Result.FAILED;
                    }
                    this.next(0);
                    break;
                default:
                    return BlockUser.Result.FAILED;
            }
            return BlockUser.Result.RUNNING;
        } else {
            return BlockUser.Result.FAILED;
        }
    }

    void close(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player != null
                && this.disabledSlots != null
                && player.currentScreenHandler instanceof CrafterScreenHandler) {
            player.closeHandledScreen();
        }
    }

    private static void emptyHand(ClientPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        if (inventory.getStack(inventory.getSelectedSlot()).getItem() instanceof BlockItem) {
            int i = -1;
            for (int j = 0; j < 9; j++) {
                ItemStack stack = inventory.getStack(j);
                if (stack.isEmpty()) {
                    inventory.setSelectedSlot(j);
                    return;
                }
                if (i < 0 && !(stack.getItem() instanceof BlockItem)) {
                    i = j;
                }
            }
            if (i >= 0) {
                inventory.setSelectedSlot(i);
            }
        }
    }

    private boolean matches(CrafterScreenHandler crafterScreenHandler) {
        for (int i = 0; i < 9; i++) {
            if (crafterScreenHandler.isSlotDisabled(i) != this.disabledSlots[i]) {
                return false;
            }
        }
        return true;
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
