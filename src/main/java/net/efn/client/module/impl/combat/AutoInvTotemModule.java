package net.efn.client.module.impl.combat;

import com.example.client.mixin.AccessorElytraFlyMultiPlayerGameMode;
import com.example.client.mixin.AccessorLocalPlayer;

import net.efn.client.event.EventHandler;
import net.efn.client.event.EventPriority;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.CombatMacroLock;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.combat.totem.DoubleHand;
import net.efn.client.module.impl.movement.SneakModule;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public final class AutoInvTotemModule extends ClientModule {

    private static final String INVENTORY = "Inventory";

    private static final String HOTBAR_SWAP = "Hotbar Swap";

    private static final int MIN_CONFIRM_TICKS = 10;

    private static final int MAX_CONFIRM_TICKS = 40;

    private static final int PASS_COOLDOWN_TICKS = 4;

    private static final int GIVE_UP_COOLDOWN_TICKS = 10;

    private static final int MAX_PASS_SWAPS = 2;

    private static final int MAX_RESELECTS = 2;

    private static final int MAX_STEPS_PER_TICK = 3;

    private static final int MAX_UNSAFE_TICKS = 10;

    private static AutoInvTotemModule instance;

    private final ModeSetting mode =
            this.addSetting(
                    "mode", new ModeSetting("Mode", "Inventory", "Inventory", "Hotbar Swap"));

    private final BooleanSetting hotbarRefill =
            this.addSetting("hotbar-refill", new BooleanSetting("Hotbar Refill", false));

    private final NumberSetting hotbarSlot =
            this.addSetting(
                    "hotbar-slot", new NumberSetting("Hotbar Slot", 1.0F, 1.0F, 9.0F).step(1.0F));

    private final NumberSetting reactionDelay =
            this.addSetting(
                    "reaction-delay",
                    new NumberSetting("Reaction Delay", 0.0F, 0.0F, 10.0F).step(1.0F));

    private final NumberSetting swapDelay =
            this.addSetting(
                    "swap-delay", new NumberSetting("Swap Delay", 1.0F, 0.0F, 10.0F).step(1.0F));

    private final NumberSetting closeDelay =
            this.addSetting(
                    "close-delay", new NumberSetting("Close Delay", 0.0F, 0.0F, 10.0F).step(1.0F));

    private final NumberSetting switchBackDelay =
            this.addSetting(
                    "switch-back-delay",
                    new NumberSetting("Switch Back Delay", 2.0F, 0.0F, 10.0F).step(1.0F));

    private final BooleanSetting doubleHand =
            this.addSetting("double-hand", new BooleanSetting("Double Hand", false));

    private final DoubleHand totemHand = new DoubleHand();

    private AutoInvTotemModule.Step step = AutoInvTotemModule.Step.IDLE;

    private int tick;

    private int readyAt;

    private int reactionAt = -1;

    private int idleUntil;

    private int confirmDeadline = -1;

    private int clickedAt = -1;

    private int swappedAt = -1;

    private boolean openedInventory;

    private int passSwaps;

    private int unsafeTicks;

    private int totemSlot = -1;

    private int previousSlot = -1;

    private int reselects;

    public AutoInvTotemModule() {
        super(
                "autoinvtotem",
                "Auto Inventory Totem",
                "Refills the offhand totem after a pop through the inventory or a hotbar swap;"
                        + " Double Hand also keeps one in the main hand while in danger.",
                ModuleCategory.COMBAT);
        instance = this;
        this.hotbarSlot.visibleWhen(this.hotbarRefill::get);
        this.switchBackDelay.visibleWhen(() -> this.mode.is("Hotbar Swap"));
    }

    public static boolean wantsHotbar() {
        AutoInvTotemModule module = instance;
        return module != null && module.isEnabled()
                ? module.step == AutoInvTotemModule.Step.HOTBAR_SWAP
                        || module.step == AutoInvTotemModule.Step.HOTBAR_RETURN
                        || module.totemHand.holding()
                : AutoTotemModule.wantsHotbar();
    }

    public static boolean busy() {
        AutoInvTotemModule module = instance;
        if (module != null && module.isEnabled()) {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null && !player.isSpectator() && !player.isInCreativeMode()) {
                PlayerInventory inventory = player.getInventory();
                return module.step != AutoInvTotemModule.Step.IDLE
                        || module.totemHand.holding()
                        || module.awaitingServer(player)
                        || module.clickedAt == module.tick
                        || player.getOffHandStack().isEmpty()
                                && (findStoredTotem(inventory) != -1
                                        || module.hotbarSource(inventory) != -1);
            } else {
                return false;
            }
        } else {
            return AutoTotemModule.busy();
        }
    }

    public static boolean clickSafe(ClientPlayerEntity player) {
        AccessorLocalPlayer accessorLocalPlayer = (AccessorLocalPlayer) player;
        PlayerInput playerInput = accessorLocalPlayer.efn$lastSentInput();
        return !accessorLocalPlayer.efn$wasSprinting()
                && !playerInput.forward()
                && !playerInput.backward()
                && !playerInput.left()
                && !playerInput.right()
                && !playerInput.jump()
                && !playerInput.sneak();
    }

    @Override
    protected void onEnable() {
        AutoTotemModule module = ModuleRegistry.get(AutoTotemModule.class);
        if (module != null) {
            module.setEnabled(false);
        }
        this.idleUntil = 0;
    }

    @Override
    protected void onDisable() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player != null) {
            PlayerInventory inventory = player.getInventory();
            if (this.openedInventory && client.currentScreen instanceof InventoryScreen) {
                player.closeHandledScreen();
            }
            if ((this.step == AutoInvTotemModule.Step.HOTBAR_SWAP
                            || this.step == AutoInvTotemModule.Step.HOTBAR_RETURN)
                    && client.currentScreen == null) {
                this.restoreSlot(inventory);
            }
            this.totemHand.putBack(inventory);
        }
        this.reset();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTick(TickEvent event) {
        this.tick++;
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player != null
                && client.world != null
                && client.interactionManager != null
                && client.getNetworkHandler() != null
                && !player.isDead()
                && !player.isSpectator()
                && !player.isInCreativeMode()) {
            if (this.step == AutoInvTotemModule.Step.IDLE) {
                this.tryStart(client, player);
            }
            if (this.step == AutoInvTotemModule.Step.IDLE) {
                this.totemHand.tick(client, player, this.doubleHand.get());
            }
            for (int i = 0;
                    i < 3 && this.step != AutoInvTotemModule.Step.IDLE && this.tick >= this.readyAt;
                    i++) {
                switch (this.step) {
                    case INVENTORY_SWAP:
                        this.inventorySwap(client, player);
                        break;
                    case INVENTORY_CLOSE:
                        this.closeInventory(client, player);
                        break;
                    case HOTBAR_SWAP:
                        this.hotbarSwap(client, player);
                        break;
                    case HOTBAR_RETURN:
                        this.hotbarReturn(client, player);
                }
            }
        } else {
            if (player != null
                    && this.openedInventory
                    && client.currentScreen instanceof InventoryScreen) {
                player.closeHandledScreen();
            }
            this.reset();
        }
    }

    private void tryStart(MinecraftClient client, ClientPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        if (!player.getOffHandStack().isEmpty()) {
            this.confirmDeadline = -1;
        }
        boolean bl =
                player.getOffHandStack().isEmpty()
                        && !this.awaitingServer(player)
                        && (findStoredTotem(inventory) != -1 || this.hotbarSource(inventory) != -1);
        if (!bl && !this.refillDue(client, player)) {
            this.reactionAt = -1;
        } else {
            if (this.reactionAt < 0) {
                this.reactionAt = this.tick + this.reactionDelay.getInt();
            }
            if (this.tick >= this.reactionAt && this.tick >= this.idleUntil) {
                if (client.currentScreen instanceof HandledScreen) {
                    this.startPass(false);
                } else if (client.currentScreen == null) {
                    boolean bl2 = !client.interactionManager.hasRidingInventory() && !sneakForced();
                    if (!bl
                            || this.hotbarSource(inventory) == -1
                            || !this.mode.is("Hotbar Swap") && bl2) {
                        if (bl2) {
                            client.setScreen(new InventoryScreen(player));
                            this.startPass(true);
                        } else {
                            this.idleUntil = this.tick + 10;
                        }
                    } else {
                        this.startHotbarSwap(inventory);
                    }
                }
            }
        }
    }

    private boolean refillDue(MinecraftClient client, ClientPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        return !player.getOffHandStack().isEmpty()
                        && this.refillWanted(inventory)
                        && findStoredTotem(inventory) != -1
                ? client.currentScreen instanceof HandledScreen
                        || client.currentScreen == null
                                && clickSafe(player)
                                && !this.totemHand.holding()
                                && !CombatMacroLock.heldByOther(this)
                : false;
    }

    private boolean refillWanted(PlayerInventory playerInventory) {
        return this.hotbarRefill.get()
                && !playerInventory
                        .getStack(this.hotbarSlot.getInt() - 1)
                        .isOf(Items.TOTEM_OF_UNDYING);
    }

    private static boolean sneakForced() {
        SneakModule active = SneakModule.active;
        return active != null && (active.doVanilla() || active.doPacket());
    }

    private void startPass(boolean openedInventory2) {
        this.openedInventory = openedInventory2;
        this.passSwaps = 0;
        this.unsafeTicks = 0;
        this.schedule(
                AutoInvTotemModule.Step.INVENTORY_SWAP,
                openedInventory2 ? Math.max(1, this.swapDelay.getInt()) : this.swapDelay.getInt());
    }

    private void inventorySwap(MinecraftClient client, ClientPlayerEntity player) {
        if (client.currentScreen instanceof HandledScreen handledScreen
                && (!this.openedInventory || client.currentScreen instanceof InventoryScreen)) {
            ScreenHandler screenHandler = handledScreen.getScreenHandler();
            if (clickSafe(player) && screenHandler.getCursorStack().isEmpty()) {
                int passSource = this.passSource(player);
                if (passSource != -1 && this.passSwaps < 2) {
                    int i = player.getOffHandStack().isEmpty() ? 40 : this.hotbarSlot.getInt() - 1;
                    screenHandler
                            .getSlotIndex(player.getInventory(), passSource)
                            .ifPresent(
                                    j ->
                                            client.interactionManager.clickSlot(
                                                    screenHandler.syncId,
                                                    j,
                                                    i,
                                                    SlotActionType.SWAP,
                                                    player));
                    this.clickedAt = this.tick;
                    this.passSwaps++;
                    if (this.passSource(player) != -1 && this.passSwaps < 2) {
                        this.schedule(
                                AutoInvTotemModule.Step.INVENTORY_SWAP,
                                Math.max(1, this.swapDelay.getInt()));
                    } else {
                        this.schedule(
                                AutoInvTotemModule.Step.INVENTORY_CLOSE, this.closeDelay.getInt());
                    }
                } else {
                    this.schedule(
                            AutoInvTotemModule.Step.INVENTORY_CLOSE, this.closeDelay.getInt());
                }
            } else {
                if (++this.unsafeTicks > 10) {
                    this.schedule(AutoInvTotemModule.Step.INVENTORY_CLOSE, 0);
                } else {
                    this.readyAt = this.tick + 1;
                }
            }
        } else {
            this.finishPass();
        }
    }

    private void closeInventory(MinecraftClient client, ClientPlayerEntity player) {
        if (this.passSource(player) != -1
                && this.passSwaps < 2
                && this.unsafeTicks <= 10
                && client.currentScreen instanceof HandledScreen) {
            this.schedule(
                    AutoInvTotemModule.Step.INVENTORY_SWAP, Math.max(1, this.swapDelay.getInt()));
        } else {
            if (this.openedInventory && client.currentScreen instanceof InventoryScreen) {
                if (!clickSafe(player) && ++this.unsafeTicks <= 10) {
                    this.readyAt = this.tick + 1;
                    return;
                }
                player.closeHandledScreen();
            }
            this.finishPass();
        }
    }

    private void finishPass() {
        this.idleUntil = this.tick + 4;
        this.endSequence();
    }

    private int passSource(ClientPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        int storedTotem = findStoredTotem(inventory);
        if (player.getOffHandStack().isEmpty()) {
            return storedTotem != -1 ? storedTotem : this.hotbarSource(inventory);
        } else {
            return this.refillWanted(inventory) ? storedTotem : -1;
        }
    }

    private void startHotbarSwap(PlayerInventory playerInventory) {
        this.totemSlot = this.hotbarSource(playerInventory);
        this.previousSlot = playerInventory.getSelectedSlot();
        this.reselects = 0;
        playerInventory.setSelectedSlot(this.totemSlot);
        this.schedule(AutoInvTotemModule.Step.HOTBAR_SWAP, this.swapDelay.getInt());
    }

    private void hotbarSwap(MinecraftClient client, ClientPlayerEntity player) {
        if (client.currentScreen != null) {
            this.readyAt = this.tick + 1;
        } else if (!player.getOffHandStack().isEmpty()) {
            this.schedule(AutoInvTotemModule.Step.HOTBAR_RETURN, 0);
        } else {
            PlayerInventory inventory = player.getInventory();
            if (inventory.getSelectedSlot() == this.totemSlot
                    && player.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
                ((AccessorElytraFlyMultiPlayerGameMode) client.interactionManager)
                        .efn$elytraFlySyncSelected();
                while (client.options.swapHandsKey.wasPressed()) {}
                networkHandler.sendPacket(
                        new PlayerActionC2SPacket(
                                Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
                this.confirmDeadline = this.tick + confirmWindow(networkHandler, player);
                this.swappedAt = this.tick;
                this.schedule(AutoInvTotemModule.Step.HOTBAR_RETURN, this.switchBackDelay.getInt());
            } else {
                int hotbarSource = this.hotbarSource(inventory);
                if (hotbarSource != -1 && this.reselects++ < 2) {
                    this.totemSlot = hotbarSource;
                    inventory.setSelectedSlot(hotbarSource);
                    this.schedule(AutoInvTotemModule.Step.HOTBAR_SWAP, this.swapDelay.getInt());
                } else {
                    this.idleUntil = this.tick + 10;
                    this.schedule(AutoInvTotemModule.Step.HOTBAR_RETURN, 0);
                }
            }
        }
    }

    private void hotbarReturn(MinecraftClient client, ClientPlayerEntity player) {
        if (client.currentScreen == null
                && (this.swappedAt != this.tick
                        || !player.isUsingItem()
                        || player.getActiveHand() != Hand.MAIN_HAND)) {
            this.restoreSlot(player.getInventory());
            this.endSequence();
        } else {
            this.readyAt = this.tick + 1;
        }
    }

    private void restoreSlot(PlayerInventory playerInventory) {
        if (PlayerInventory.isValidHotbarIndex(this.previousSlot)
                && this.previousSlot != this.totemSlot
                && playerInventory.getSelectedSlot() == this.totemSlot) {
            playerInventory.setSelectedSlot(this.previousSlot);
        }
    }

    private int hotbarSource(PlayerInventory playerInventory) {
        int selectedSlot = playerInventory.getSelectedSlot();
        int i = this.hotbarRefill.get() ? this.hotbarSlot.getInt() - 1 : -1;
        int j = -1;
        for (int m = 0; m < 9; m++) {
            if (m != selectedSlot && playerInventory.getStack(m).isOf(Items.TOTEM_OF_UNDYING)) {
                if (m != i) {
                    return m;
                }
                j = m;
            }
        }
        if (j != -1) {
            return j;
        } else {
            return !this.totemHand.holding()
                            && playerInventory.getStack(selectedSlot).isOf(Items.TOTEM_OF_UNDYING)
                    ? selectedSlot
                    : -1;
        }
    }

    private static int findStoredTotem(PlayerInventory playerInventory) {
        for (int i = 9; i < 36; i++) {
            if (playerInventory.getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
                return i;
            }
        }
        return -1;
    }

    private static int confirmWindow(
            ClientPlayNetworkHandler clientPlayNetworkHandler, ClientPlayerEntity player) {
        PlayerListEntry playerListEntry =
                clientPlayNetworkHandler.getPlayerListEntry(player.getUuid());
        int i = playerListEntry != null ? playerListEntry.getLatency() : 0;
        return MathHelper.clamp(i / 25 + 4, 10, 40);
    }

    private boolean awaitingServer(ClientPlayerEntity player) {
        return this.confirmDeadline >= 0
                && this.tick < this.confirmDeadline
                && !player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);
    }

    private void schedule(AutoInvTotemModule.Step step2, int i) {
        this.step = step2;
        this.readyAt = this.tick + i;
    }

    private void endSequence() {
        this.step = AutoInvTotemModule.Step.IDLE;
        this.reactionAt = -1;
        this.openedInventory = false;
        this.passSwaps = 0;
        this.unsafeTicks = 0;
        this.totemSlot = -1;
        this.previousSlot = -1;
        this.reselects = 0;
    }

    private void reset() {
        this.endSequence();
        this.confirmDeadline = -1;
        this.totemHand.reset();
    }

    @Environment(EnvType.CLIENT)
    private static enum Step {
        IDLE,
        INVENTORY_SWAP,
        INVENTORY_CLOSE,
        HOTBAR_SWAP,
        HOTBAR_RETURN;

        private Step() {}
    }
}
