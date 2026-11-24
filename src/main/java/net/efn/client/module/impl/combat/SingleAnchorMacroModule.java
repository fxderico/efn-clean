package net.efn.client.module.impl.combat;

import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.PacketAppliedEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.CombatMacroLock;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.anchor.AnchorGuard;
import net.efn.client.module.impl.combat.crystal.CrystalDamage;
import net.efn.client.module.impl.combat.crystal.Crystals;
import net.efn.client.module.impl.combat.crystal.LootGuard;
import net.efn.client.module.impl.combat.crystal.LootLatch;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.impl.misc.AutoEatModule;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.pvp.Rotation;
import net.efn.client.util.pvp.RotationUtils;
import net.efn.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public final class SingleAnchorMacroModule extends ClientModule {

    private static final String SINGLE = "Single";

    private static final String DOUBLE = "Double";

    private static final String DETONATE = "Detonate";

    private static final String CANCEL = "Cancel";

    private static final int MAX_STEPS_PER_TICK = 4;

    private static final int GUARD_TRIES = 2;

    private static final int SETTLE_TICKS = 5;

    private static final float BLAST_PROOF = 600.0F;

    private final ModeSetting mode =
            this.addSetting("mode", new ModeSetting("Mode", "Single", "Single", "Double"));

    private final NumberSetting delay =
            this.addSetting("delay", new NumberSetting("Delay", 0.0F, 0.0F, 5.0F).step(1.0F));

    private final BooleanSetting blastGuard =
            this.addSetting("safe-anchor", new BooleanSetting("Safe Anchor", true));

    private final ModeSetting withoutGuard =
            this.addSetting(
                    "without-guard",
                    new ModeSetting("If Unguarded", "Detonate", "Detonate", "Cancel"));

    private final BooleanSetting switchBack =
            this.addSetting("switch-back", new BooleanSetting("Switch Back", false));

    private final BooleanSetting protectLoot =
            this.addSetting("protect-loot", new BooleanSetting("Protect Loot", true));

    private SingleAnchorMacroModule.Step step = SingleAnchorMacroModule.Step.IDLE;

    private int tick;

    private int readyAt;

    private int deadline;

    private int previousSlot = -1;

    private int ownSlot = -1;

    private boolean refilled;

    private BlockPos anchorPos;

    private Vec3d sightline;

    private BlockHitResult replaceHit;

    private ClientWorld activeLevel;

    private AnchorGuard.Spot guard;

    private Block guardBlock;

    private BlockState guardReported;

    private int guardTries;

    public SingleAnchorMacroModule() {
        super(
                "anchor-macro",
                "Anchor Macro",
                "Places, charges and blows up respawn anchors on right-click.",
                ModuleCategory.COMBAT);
        this.withoutGuard.visibleWhen(this.blastGuard::get);
    }

    @Override
    protected void onEnable() {
        this.stop();
    }

    @Override
    protected void onDisable() {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerInventory inventory;
        if (this.step != SingleAnchorMacroModule.Step.IDLE
                && client.player != null
                && !(client.currentScreen instanceof HandledScreen)
                && this.switchBackDue(inventory = client.player.getInventory())) {
            inventory.setSelectedSlot(this.previousSlot);
        }
        this.stop();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        this.tick++;
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player != null
                && client.world != null
                && client.interactionManager != null
                && !player.isDead()) {
            LootLatch.update(client, player);
            LootGuard.update(client.world, player);
            PlayerInventory inventory = player.getInventory();
            if (client.currentScreen != null) {
                if (this.step != SingleAnchorMacroModule.Step.IDLE
                        && this.step != SingleAnchorMacroModule.Step.SWITCH_BACK) {
                    this.wrapUp(inventory);
                }
            } else if (!AutoEatModule.isEating() && !AutoInvTotemModule.wantsHotbar()) {
                if (this.step != SingleAnchorMacroModule.Step.IDLE
                        && (this.hotbarMovedByPlayer(inventory)
                                || client.world != this.activeLevel)) {
                    this.stop();
                }
                if ((LootLatch.latched() || LootLatch.killedNow())
                        && this.step != SingleAnchorMacroModule.Step.IDLE
                        && this.step != SingleAnchorMacroModule.Step.SWITCH_BACK) {
                    this.wrapUp(inventory);
                }
                if (LootLatch.latched() && this.step == SingleAnchorMacroModule.Step.IDLE) {
                    if (!player.isUsingItem() && anchorInPlay(client, player)) {
                        suppressUse(client);
                    }
                } else if (this.step != SingleAnchorMacroModule.Step.IDLE
                        || this.tryStart(client, player)) {
                    swallowClicks(client);
                    if (this.step == SingleAnchorMacroModule.Step.SWITCH_BACK) {
                        if (!MeleeAttacks.actedThisTick(player)) {
                            if (this.switchBackDue(inventory)) {
                                inventory.setSelectedSlot(this.previousSlot);
                            }
                            this.stop();
                        }
                    } else {
                        for (int i = 0;
                                i < 4
                                        && this.tick >= this.readyAt
                                        && this.step != SingleAnchorMacroModule.Step.IDLE
                                        && this.step != SingleAnchorMacroModule.Step.SWITCH_BACK;
                                i++) {
                            if (this.tick >= this.deadline) {
                                this.expire(inventory);
                            } else {
                                if (MeleeAttacks.actedThisTick(player)
                                        || !MeleeAttacks.handsFree(client, player)) {
                                    break;
                                }
                                this.runStep(client, player);
                            }
                        }
                    }
                }
            } else {
                this.stop();
            }
        } else {
            this.stop();
        }
    }

    @EventHandler
    public void onPacketApplied(PacketAppliedEvent event) {
        if (this.step == SingleAnchorMacroModule.Step.AWAIT_GUARD && this.guard != null) {
            BlockPos pos = this.guard.pos();
            if (event.packet instanceof BlockUpdateS2CPacket blockUpdatePacket) {
                if (blockUpdatePacket.getPos().equals(pos)) {
                    this.guardReported = blockUpdatePacket.getState();
                }
            } else if (event.packet instanceof ChunkDeltaUpdateS2CPacket chunkDeltaUpdatePacket) {
                chunkDeltaUpdatePacket.visitUpdates(
                        (blockPos2, blockState) -> {
                            if (blockPos2.equals(pos)) {
                                this.guardReported = blockState;
                            }
                        });
            }
        }
    }

    private boolean tryStart(MinecraftClient client, ClientPlayerEntity player) {
        if (MeleeAttacks.rightClickComing(client) && !CombatMacroLock.heldByOther(this)) {
            if (!player.isSpectator()
                    && canClick(player)
                    && !MeleeAttacks.actedThisTick(player)
                    && MeleeAttacks.handsFree(client, player)
                    && !player.getOffHandStack().isOf(Items.GLOWSTONE)) {
                HitResult hitResult =
                        player.raycast(player.getBlockInteractionRange(), 1.0F, false);
                BlockHitResult blockHitResult;
                if (hitResult instanceof BlockHitResult
                        && (blockHitResult = (BlockHitResult) hitResult).getType() == Type.BLOCK) {
                    ClientWorld world = client.world;
                    BlockState blockState = world.getBlockState(blockHitResult.getBlockPos());
                    boolean of = blockState.isOf(Blocks.RESPAWN_ANCHOR);
                    BlockPos pos;
                    if (of) {
                        pos = blockHitResult.getBlockPos().toImmutable();
                    } else {
                        if (!player.getMainHandStack().isOf(Items.RESPAWN_ANCHOR)
                                || ServerRotation.pending()) {
                            return false;
                        }
                        pos = Crystals.placedObsidian(world, player, blockHitResult, blockState);
                    }
                    if (pos == null || world.getRegistryKey() == World.NETHER) {
                        return false;
                    } else if (this.protectLoot.get() && LootGuard.near(Vec3d.ofCenter(pos))) {
                        return false;
                    } else {
                        PlayerInventory inventory = player.getInventory();
                        boolean bl = chargeOf(blockState) > 0;
                        if (!bl && findHotbar(inventory, Items.GLOWSTONE) == -1) {
                            return false;
                        } else if (AnchorGuard.detonatorSlot(inventory) == -1) {
                            return false;
                        } else if (this.blastGuard.get()
                                && this.withoutGuard.is("Cancel")
                                && guardSlot(inventory, this.glowstoneNeeded(bl)) == -1) {
                            return false;
                        } else if (!CombatMacroLock.tryAcquire(this)) {
                            return false;
                        } else {
                            this.previousSlot = inventory.getSelectedSlot();
                            this.activeLevel = world;
                            this.anchorPos = pos;
                            this.sightline = blockHitResult.getPos();
                            SingleAnchorMacroModule.Step step2 =
                                    this.blastGuard.get()
                                            ? SingleAnchorMacroModule.Step.GUARD
                                            : SingleAnchorMacroModule.Step.CHARGE;
                            if (of) {
                                this.scheduleNow(step2);
                            } else {
                                AnchorGuard.click(client, player, blockHitResult);
                                this.schedule(step2);
                            }
                            return true;
                        }
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private static void swallowClicks(MinecraftClient client) {
        MeleeAttacks.takeAttackClicks(client);
        suppressUse(client);
    }

    private static boolean anchorInPlay(MinecraftClient client, ClientPlayerEntity player) {
        ItemStack mainHandStack = player.getMainHandStack();
        if (!mainHandStack.isOf(Items.RESPAWN_ANCHOR) && !mainHandStack.isOf(Items.GLOWSTONE)) {
            HitResult crosshairTarget = client.crosshairTarget;
            BlockHitResult blockHitResult;
            return crosshairTarget instanceof BlockHitResult
                    && (blockHitResult = (BlockHitResult) crosshairTarget).getType() == Type.BLOCK
                    && client.world
                            .getBlockState(blockHitResult.getBlockPos())
                            .isOf(Blocks.RESPAWN_ANCHOR);
        } else {
            return true;
        }
    }

    private static void suppressUse(MinecraftClient client) {
        KeyBinding useKey = client.options.useKey;
        useKey.setPressed(false);
        while (useKey.wasPressed()) {}
    }

    private void runStep(MinecraftClient client, ClientPlayerEntity player) {
        switch (this.step) {
            case GUARD:
                this.placeGuard(client, player);
                break;
            case AWAIT_GUARD:
                this.awaitGuard(client, player);
                break;
            case CHARGE:
                this.charge(client, player);
                break;
            case DETONATE:
                this.detonate(client, player);
                break;
            case REPLACE:
                this.replace(client, player);
        }
    }

    private void expire(PlayerInventory playerInventory) {
        if (this.step != SingleAnchorMacroModule.Step.GUARD
                && this.step != SingleAnchorMacroModule.Step.AWAIT_GUARD) {
            this.wrapUp(playerInventory);
        } else {
            this.skipGuard(playerInventory);
        }
    }

    private void placeGuard(MinecraftClient client, ClientPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        BlockState blockState = client.world.getBlockState(this.anchorPos);
        if (!blockState.isOf(Blocks.RESPAWN_ANCHOR)) {
            this.wrapUp(inventory);
        } else {
            AnchorGuard.Spot spot =
                    AnchorGuard.find(client.world, player, this.anchorPos, this.sightline);
            if (spot != null && spot.standing()) {
                this.guarded();
            } else {
                int guardSlot =
                        guardSlot(inventory, this.glowstoneNeeded(chargeOf(blockState) > 0));
                Rotation rotation = ServerRotation.sent();
                BlockHitResult blockHitResult =
                        spot != null && rotation != null
                                ? AnchorGuard.hitAlong(client.world, player, rotation, spot)
                                : null;
                if (spot != null
                        && guardSlot != -1
                        && (blockHitResult != null || ServerRotation.canRotate(player))) {
                    if (blockHitResult == null) {
                        ServerRotation.request(
                                RotationUtils.fromTo(player.getEyePos(), spot.aim()));
                        MeleeAttacks.claimTick(client);
                        this.readyAt = this.tick + 1;
                    } else {
                        this.guard = spot;
                        this.guardBlock =
                                Block.getBlockFromItem(inventory.getStack(guardSlot).getItem());
                        this.guardReported = null;
                        this.select(inventory, guardSlot);
                        AnchorGuard.click(client, player, blockHitResult);
                        ServerRotation.request(
                                RotationUtils.fromTo(
                                        player.getEyePos(), Vec3d.ofCenter(this.anchorPos)));
                        this.guardTries++;
                        if (this.withoutGuard.is("Cancel")) {
                            this.schedule(SingleAnchorMacroModule.Step.AWAIT_GUARD);
                            this.deadline = this.readyAt + Crystals.confirmWindow(client, player);
                        } else {
                            this.schedule(SingleAnchorMacroModule.Step.CHARGE);
                        }
                    }
                } else {
                    this.skipGuard(inventory);
                }
            }
        }
    }

    private void awaitGuard(MinecraftClient client, ClientPlayerEntity player) {
        boolean of = client.world.getBlockState(this.guard.pos()).isOf(this.guardBlock);
        if (of && this.guardReported != null && this.guardReported.isOf(this.guardBlock)) {
            this.guarded();
        } else if (of) {
            this.readyAt = this.tick + 1;
        } else if (this.guardTries < 2) {
            this.scheduleNow(SingleAnchorMacroModule.Step.GUARD);
        } else {
            this.skipGuard(player.getInventory());
        }
    }

    private void guarded() {
        this.guard = null;
        this.scheduleNow(SingleAnchorMacroModule.Step.CHARGE);
    }

    private void skipGuard(PlayerInventory playerInventory) {
        this.guard = null;
        if (this.withoutGuard.is("Cancel")) {
            this.wrapUp(playerInventory);
        } else {
            this.scheduleNow(SingleAnchorMacroModule.Step.CHARGE);
        }
    }

    private void charge(MinecraftClient client, ClientPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        BlockState blockState = client.world.getBlockState(this.anchorPos);
        if (!blockState.isOf(Blocks.RESPAWN_ANCHOR)) {
            this.wrapUp(inventory);
        } else if (chargeOf(blockState) > 0 && !this.refilled) {
            this.scheduleNow(SingleAnchorMacroModule.Step.DETONATE);
        } else {
            int hotbar = findHotbar(inventory, Items.GLOWSTONE);
            if (hotbar != -1 && canClick(player)) {
                BlockHitResult blockHitResult =
                        AnchorGuard.anchorHit(client.world, player, this.anchorPos);
                if (blockHitResult == null) {
                    this.readyAt = this.tick + 1;
                } else {
                    this.select(inventory, hotbar);
                    AnchorGuard.click(client, player, blockHitResult);
                    this.schedule(SingleAnchorMacroModule.Step.DETONATE);
                }
            } else {
                this.wrapUp(inventory);
            }
        }
    }

    private void detonate(MinecraftClient client, ClientPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        if (chargeOf(client.world.getBlockState(this.anchorPos)) != 0
                && canClick(player)
                && !player.getOffHandStack().isOf(Items.GLOWSTONE)) {
            BlockHitResult blockHitResult =
                    AnchorGuard.anchorHit(client.world, player, this.anchorPos);
            if (blockHitResult == null) {
                this.readyAt = this.tick + 1;
            } else {
                int i =
                        this.mode.is("Double")
                                        && !this.refilled
                                        && blockHitResult.getSide() == Direction.UP
                                        && this.anchorPos.getY() >= player.getBlockY()
                                        && findHotbar(inventory, Items.GLOWSTONE) != -1
                                ? findHotbar(inventory, Items.RESPAWN_ANCHOR)
                                : -1;
                int j = i != -1 ? i : AnchorGuard.detonatorSlot(inventory);
                if (j == -1) {
                    this.wrapUp(inventory);
                } else if (this.lethal(player, inventory.getStack(j))) {
                    this.wrapUp(inventory);
                } else {
                    this.select(inventory, j);
                    AnchorGuard.click(client, player, blockHitResult);
                    if (j == i) {
                        this.refilled = true;
                        this.replaceHit = blockHitResult;
                        this.schedule(SingleAnchorMacroModule.Step.REPLACE);
                    } else {
                        this.wrapUp(inventory);
                    }
                }
            }
        } else {
            this.wrapUp(inventory);
        }
    }

    private void replace(MinecraftClient client, ClientPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        Rotation rotation = ServerRotation.sent();
        Vec3d eyePos = player.getEyePos();
        boolean bl =
                rotation != null
                        && new Box(this.anchorPos)
                                .raycast(
                                        eyePos,
                                        eyePos.add(
                                                Vec3d.fromPolar(rotation.pitch(), rotation.yaw())
                                                        .multiply(
                                                                player.getBlockInteractionRange())))
                                .isPresent();
        if (this.replaceHit != null
                && bl
                && player.getMainHandStack().isOf(Items.RESPAWN_ANCHOR)
                && canClick(player)) {
            AnchorGuard.click(client, player, this.replaceHit);
            this.replaceHit = null;
            this.schedule(SingleAnchorMacroModule.Step.CHARGE);
        } else {
            this.wrapUp(inventory);
        }
    }

    private boolean lethal(ClientPlayerEntity player, ItemStack stack) {
        if (!stack.isOf(Items.TOTEM_OF_UNDYING)
                && !player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
            float estimate =
                    CrystalDamage.estimate(
                            Vec3d.ofCenter(this.anchorPos), player, 5.0F, this.anchorPos);
            return CrystalDamage.endangers(player, estimate);
        } else {
            return false;
        }
    }

    private void wrapUp(PlayerInventory playerInventory) {
        if (this.switchBackDue(playerInventory)) {
            this.step = SingleAnchorMacroModule.Step.SWITCH_BACK;
        } else {
            this.stop();
        }
    }

    private boolean switchBackDue(PlayerInventory playerInventory) {
        return this.switchBack.get()
                && PlayerInventory.isValidHotbarIndex(this.previousSlot)
                && this.ownSlot != -1
                && this.ownSlot != this.previousSlot
                && playerInventory.getSelectedSlot() == this.ownSlot;
    }

    private boolean hotbarMovedByPlayer(PlayerInventory playerInventory) {
        return playerInventory.getSelectedSlot()
                != (this.ownSlot != -1 ? this.ownSlot : this.previousSlot);
    }

    private void stop() {
        this.step = SingleAnchorMacroModule.Step.IDLE;
        this.anchorPos = null;
        this.sightline = null;
        this.replaceHit = null;
        this.activeLevel = null;
        this.previousSlot = -1;
        this.ownSlot = -1;
        this.refilled = false;
        this.guard = null;
        this.guardBlock = null;
        this.guardReported = null;
        this.guardTries = 0;
        CombatMacroLock.release(this);
    }

    private void schedule(SingleAnchorMacroModule.Step step2) {
        this.step = step2;
        this.readyAt = this.tick + 1 + this.delay.getInt();
        this.deadline = this.readyAt + 5;
    }

    private void scheduleNow(SingleAnchorMacroModule.Step step2) {
        this.step = step2;
        this.readyAt = this.tick;
        this.deadline = this.tick + 5;
    }

    private void select(PlayerInventory playerInventory, int selectedSlot) {
        if (playerInventory.getSelectedSlot() != selectedSlot) {
            playerInventory.setSelectedSlot(selectedSlot);
            this.ownSlot = selectedSlot;
        }
    }

    private static boolean canClick(ClientPlayerEntity player) {
        return !player.shouldCancelInteraction();
    }

    private static int chargeOf(BlockState state) {
        return state.isOf(Blocks.RESPAWN_ANCHOR) ? state.get(RespawnAnchorBlock.CHARGES) : 0;
    }

    private int glowstoneNeeded(boolean bl) {
        int i = bl ? 0 : 1;
        return this.mode.is("Double") ? i + 1 : i;
    }

    private static int guardSlot(PlayerInventory playerInventory, int i) {
        int j = -1;
        for (int k = 0; k < 9; k++) {
            ItemStack stack = playerInventory.getStack(k);
            if (!stack.isOf(Items.GLOWSTONE) && AnchorGuard.isGuardBlock(stack)) {
                if (Block.getBlockFromItem(stack.getItem()).getBlastResistance() >= 600.0F) {
                    return k;
                }
                if (j == -1) {
                    j = k;
                }
            }
        }
        if (j != -1) {
            return j;
        } else {
            int hotbar = findHotbar(playerInventory, Items.GLOWSTONE);
            return hotbar != -1 && countHotbar(playerInventory, Items.GLOWSTONE) > i ? hotbar : -1;
        }
    }

    private static int findHotbar(PlayerInventory playerInventory, Item item) {
        for (int i = 0; i < 9; i++) {
            if (playerInventory.getStack(i).isOf(item)) {
                return i;
            }
        }
        return -1;
    }

    private static int countHotbar(PlayerInventory playerInventory, Item item) {
        int i = 0;
        for (int j = 0; j < 9; j++) {
            ItemStack stack = playerInventory.getStack(j);
            if (stack.isOf(item)) {
                i += stack.getCount();
            }
        }
        return i;
    }

    @Environment(EnvType.CLIENT)
    private static enum Step {
        IDLE,
        GUARD,
        AWAIT_GUARD,
        CHARGE,
        DETONATE,
        REPLACE,
        SWITCH_BACK;

        private Step() {}
    }
}
