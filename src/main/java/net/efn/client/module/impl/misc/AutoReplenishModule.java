package net.efn.client.module.impl.misc;

import com.example.client.mixin.AccessorElytraFlyMultiPlayerGameMode;
import com.example.client.mixin.AccessorLocalPlayer;

import net.efn.client.event.EventHandler;
import net.efn.client.event.EventPriority;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.AutoInvTotemModule;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.service.humanize.HumanInventoryTiming;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class AutoReplenishModule extends ClientModule {

    private static final int TRACKED_SLOTS = 10;

    private static final int OFFHAND_TRACKED_SLOT = 9;

    private static final int OFFHAND_SWAP_BUTTON = 40;

    private static final int MAIN_INVENTORY_FIRST = 9;

    private static final int MAIN_INVENTORY_LAST = 35;

    private static final double FAST_MOVEMENT_THRESHOLD_SQ = 0.06;

    private static final String MODE_INSTANT = "Instant";

    private static final String MODE_LEGIT = "Legit";

    private final ModeSetting mode =
            this.addSetting("mode", new ModeSetting("Mode", "Instant", "Instant", "Legit"));

    private final NumberSetting delay =
            this.addSetting("delay", new NumberSetting("Delay", 1.0F, 0.0F, 20.0F).step(1.0F));

    private final NumberSetting threshold =
            this.addSetting(
                    "threshold", new NumberSetting("Threshold", 8.0F, 1.0F, 64.0F).step(1.0F));

    private final BooleanSetting offhand =
            this.addSetting("offhand", new BooleanSetting("Offhand", true));

    private final ItemStack[] lastKnown = new ItemStack[10];

    private final ItemStack[] pendingRefill = new ItemStack[10];

    private static final int QUIET_TICKS_BEFORE_OPEN = 10;

    private static final int MAX_WAIT_TICKS = 10;

    private ClientPlayerEntity trackedPlayer;

    private int stage;

    private int delayTimer;

    private int quietTicks;

    private int waitTicks;

    private int lastSelectedSlot = -1;

    private int activeSlot = -1;

    private ItemStack activeReference = ItemStack.EMPTY;

    private boolean wasScreenOpen;

    public AutoReplenishModule() {
        super(
                "auto-replenish",
                "Auto Replenish",
                "Refills a hotbar or offhand slot from the main inventory once it runs low.",
                ModuleCategory.MISC);
        this.delay.visibleWhen(() -> this.mode.is("Instant"));
        this.mode.onChange(this::resetJob);
    }

    @Override
    protected void onEnable() {
        this.resetJob();
    }

    @Override
    protected void onDisable() {
        this.resetJob();
        this.trackedPlayer = null;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player != null && client.interactionManager != null) {
            if (player != this.trackedPlayer) {
                this.resync(player);
            }
            this.updateTracking(client, player);
            if (this.delayTimer > 0) {
                this.delayTimer--;
            } else if (this.mode.is("Instant")) {
                this.refillNow(client, player);
            } else {
                for (int i = 0; i < 3; i++) {
                    switch (this.stage) {
                        case 0:
                            if (client.currentScreen != null
                                    && !(client.currentScreen instanceof InventoryScreen)) {
                                return;
                            }
                            if (player.isInCreativeMode() || AutoInvTotemModule.busy()) {
                                return;
                            }
                            if (this.isMovingFast(player)
                                    || ((AccessorLocalPlayer) player).efn$lastSentInput().sneak()) {
                                return;
                            }
                            if (player.isUsingItem()
                                    || client.interactionManager.isBreakingBlock()) {
                                return;
                            }
                            if (this.quietTicks < 10) {
                                return;
                            }
                            int nextPending = this.nextPending(player);
                            if (nextPending < 0) {
                                return;
                            }
                            this.activeSlot = nextPending;
                            this.activeReference = this.pendingRefill[nextPending];
                            this.pendingRefill[nextPending] = ItemStack.EMPTY;
                            this.wasScreenOpen = client.currentScreen instanceof InventoryScreen;
                            this.stage = this.wasScreenOpen ? 2 : 1;
                            break;
                        case 1:
                            client.setScreen(new InventoryScreen(player));
                            this.stage = 2;
                            this.delayTimer = HumanInventoryTiming.openGuiDelayTicks();
                            return;
                        case 2:
                            if (!(client.currentScreen
                                    instanceof InventoryScreen inventoryScreen)) {
                                if (this.activeSlot >= 0
                                        && !this.activeReference.isEmpty()
                                        && this.pendingRefill[this.activeSlot].isEmpty()) {
                                    this.pendingRefill[this.activeSlot] = this.activeReference;
                                }
                                this.resetJob();
                                return;
                            }
                            boolean bl =
                                    AutoInvTotemModule.clickSafe(player)
                                            && !AutoInvTotemModule.busy();
                            if (!bl && ++this.waitTicks <= 10) {
                                return;
                            }
                            if (!bl
                                    && this.activeSlot >= 0
                                    && this.pendingRefill[this.activeSlot].isEmpty()) {
                                this.pendingRefill[this.activeSlot] = this.activeReference;
                            }
                            Slot sourceSlot;
                            if (bl
                                    && this.stillNeedsRefill(player)
                                    && (sourceSlot =
                                                    this.findSourceSlot(
                                                            inventoryScreen.getScreenHandler(),
                                                            player))
                                            != null) {
                                int j = this.activeSlot == 9 ? 40 : this.activeSlot;
                                ((AccessorElytraFlyMultiPlayerGameMode) client.interactionManager)
                                        .efn$elytraFlySyncSelected();
                                client.interactionManager.clickSlot(
                                        inventoryScreen.getScreenHandler().syncId,
                                        sourceSlot.id,
                                        j,
                                        SlotActionType.SWAP,
                                        player);
                            }
                            this.stage = 3;
                            this.delayTimer = HumanInventoryTiming.closeGuiDelayTicks();
                            return;
                        case 3:
                            if (!this.wasScreenOpen
                                    && client.currentScreen instanceof InventoryScreen) {
                                client.player.closeHandledScreen();
                            }
                            this.resetJob();
                            return;
                        default:
                            this.resetJob();
                    }
                }
            }
        }
    }

    private void refillNow(MinecraftClient client, ClientPlayerEntity player) {
        if (client.currentScreen == null && !player.isInCreativeMode() && !player.isUsingItem()) {
            if (AutoInvTotemModule.clickSafe(player) && !AutoInvTotemModule.busy()) {
                int nextPending = this.nextPending(player);
                if (nextPending >= 0) {
                    this.activeSlot = nextPending;
                    this.activeReference = this.pendingRefill[nextPending];
                    this.pendingRefill[nextPending] = ItemStack.EMPTY;
                    PlayerScreenHandler playerScreenHandler = player.playerScreenHandler;
                    Slot sourceSlot = this.findSourceSlot(playerScreenHandler, player);
                    if (sourceSlot != null && playerScreenHandler.getCursorStack().isEmpty()) {
                        int i = this.activeSlot == 9 ? 40 : this.activeSlot;
                        ((AccessorElytraFlyMultiPlayerGameMode) client.interactionManager)
                                .efn$elytraFlySyncSelected();
                        client.interactionManager.clickSlot(
                                playerScreenHandler.syncId,
                                sourceSlot.id,
                                i,
                                SlotActionType.SWAP,
                                player);
                        this.delayTimer = this.delay.getInt();
                    }
                    this.activeSlot = -1;
                    this.activeReference = ItemStack.EMPTY;
                }
            }
        }
    }

    private void resync(ClientPlayerEntity trackedPlayer2) {
        this.trackedPlayer = trackedPlayer2;
        for (int i = 0; i < 10; i++) {
            ItemStack stack =
                    i == 9
                            ? trackedPlayer2.getOffHandStack()
                            : trackedPlayer2.getInventory().getStack(i);
            this.lastKnown[i] = stack.copy();
            this.pendingRefill[i] = ItemStack.EMPTY;
        }
        this.resetJob();
    }

    private void updateTracking(MinecraftClient client, ClientPlayerEntity player) {
        boolean bl = this.stage == 0 && client.currentScreen instanceof HandledScreen;
        boolean inCreativeMode = player.isInCreativeMode();
        boolean bl2 = false;
        int selectedSlot = player.getInventory().getSelectedSlot();
        if (selectedSlot != this.lastSelectedSlot) {
            this.lastSelectedSlot = selectedSlot;
            bl2 = true;
        }
        for (int j = 0; j < 10; j++) {
            if (j == 9 && !this.offhand.get()) {
                this.lastKnown[j] = player.getOffHandStack().copy();
                this.pendingRefill[j] = ItemStack.EMPTY;
            } else {
                ItemStack stack = this.lastKnown[j];
                ItemStack stack2 =
                        j == 9 ? player.getOffHandStack() : player.getInventory().getStack(j);
                if (!ItemStack.areEqual(stack, stack2)) {
                    bl2 = true;
                }
                this.lastKnown[j] = stack2.copy();
                if (stack2.isOf(Items.TOTEM_OF_UNDYING) || stack.isOf(Items.TOTEM_OF_UNDYING)) {
                    this.pendingRefill[j] = ItemStack.EMPTY;
                } else if (inCreativeMode) {
                    this.pendingRefill[j] = ItemStack.EMPTY;
                } else if (!bl) {
                    boolean bl3 = stack.isEmpty() || stack2.getItem() == stack.getItem();
                    if (stack2.isEmpty() && !stack.isEmpty()) {
                        this.pendingRefill[j] = stack.copy();
                    } else if (!stack.isEmpty() && !stack.isStackable() && !bl3) {
                        this.pendingRefill[j] = stack.copy();
                    } else if (!stack2.isStackable()
                            || stack2.isEmpty()
                            || !bl3
                            || stack2.getCount() > this.threshold.getInt()
                            || !this.pendingRefill[j].isEmpty()
                                    && this.pendingRefill[j].getItem() != stack2.getItem()) {
                        if (!stack2.isEmpty() && stack2.getCount() > this.threshold.getInt()) {
                            this.pendingRefill[j] = ItemStack.EMPTY;
                        }
                    } else {
                        this.pendingRefill[j] = stack2.copy();
                    }
                }
            }
        }
        this.quietTicks = bl2 ? 0 : Math.min(this.quietTicks + 1, 1000);
    }

    private int nextPending(ClientPlayerEntity player) {
        for (int i = 0; i < 10; i++) {
            if (!this.pendingRefill[i].isEmpty()
                    && this.hasSource(player, i, this.pendingRefill[i])) {
                return i;
            }
        }
        return -1;
    }

    private boolean stillNeedsRefill(ClientPlayerEntity player) {
        ItemStack stack =
                this.activeSlot == 9
                        ? player.getOffHandStack()
                        : player.getInventory().getStack(this.activeSlot);
        if (stack.isEmpty()) {
            return true;
        } else {
            return stack.getItem() != this.activeReference.getItem()
                    ? true
                    : stack.isStackable() && stack.getCount() <= this.threshold.getInt();
        }
    }

    private boolean hasSource(ClientPlayerEntity player, int i, ItemStack stack) {
        ItemStack stack2 = i == 9 ? player.getOffHandStack() : player.getInventory().getStack(i);
        for (int j = 9; j <= 35; j++) {
            if (isViableSource(player.getInventory().getStack(j), stack2, stack)) {
                return true;
            }
        }
        return false;
    }

    private Slot findSourceSlot(ScreenHandler handler, ClientPlayerEntity player) {
        ItemStack stack =
                this.activeSlot == 9
                        ? player.getOffHandStack()
                        : player.getInventory().getStack(this.activeSlot);
        int i = ItemStack.areItemsEqual(stack, this.activeReference) ? stack.getCount() : 0;
        Slot slot = null;
        for (Slot slot2 : handler.slots) {
            ItemStack stack2;
            if (slot2.id >= 9
                    && slot2.id <= 35
                    && matchesReference(stack2 = slot2.getStack(), this.activeReference)
                    && stack2.getCount() > i) {
                slot = slot2;
                i = stack2.getCount();
            }
        }
        return slot;
    }

    private static boolean isViableSource(ItemStack stack, ItemStack stack2, ItemStack stack3) {
        int i = ItemStack.areItemsEqual(stack2, stack3) ? stack2.getCount() : 0;
        return matchesReference(stack, stack3) && stack.getCount() > i;
    }

    private static boolean matchesReference(ItemStack stack, ItemStack stack2) {
        if (stack.isEmpty()) {
            return false;
        } else {
            return stack2.isStackable()
                    ? ItemStack.areItemsAndComponentsEqual(stack, stack2)
                    : ItemStack.shouldSkipHandAnimationOnSwap(
                            stack,
                            stack2,
                            componentType -> componentType == DataComponentTypes.DAMAGE);
        }
    }

    private boolean isMovingFast(ClientPlayerEntity player) {
        if (player.isSprinting()) {
            return true;
        } else if (player.hasVehicle()) {
            Entity rootVehicle = player.getRootVehicle();
            double d = rootVehicle.getX() - rootVehicle.lastX;
            double e;
            return d * d + (e = rootVehicle.getZ() - rootVehicle.lastZ) * e > 0.06;
        } else {
            Vec3d velocity = player.getVelocity();
            return velocity.x * velocity.x + velocity.z * velocity.z > 0.06;
        }
    }

    private void resetJob() {
        this.stage = 0;
        this.delayTimer = 0;
        this.waitTicks = 0;
        this.activeSlot = -1;
        this.activeReference = ItemStack.EMPTY;
        this.wasScreenOpen = false;
    }
}
