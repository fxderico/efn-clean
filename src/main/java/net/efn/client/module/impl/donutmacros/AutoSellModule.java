package net.efn.client.module.impl.donutmacros;

import net.efn.client.chat.ModuleChat;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.StringListSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

@Environment(EnvType.CLIENT)
public final class AutoSellModule extends ClientModule {

    private static final int INVENTORY_SLOTS = 36;

    private static final int CONFIRM_SLOT = 53;

    private static final long POLL_MS = 350L;

    private static final long COMMAND_COOLDOWN_MS = 4000L;

    private static final long OPEN_TIMEOUT_MS = 10000L;

    private static final long CONFIRM_COOLDOWN_MS = 900L;

    private static final long POST_CONFIRM_MS = 250L;

    private static final int STUCK_CONFIRMS = 3;

    private final StringListSetting neverSell =
            this.addSetting(
                    "never-sell",
                    new StringListSetting(
                                    "Never Sell",
                                    "minecraft:netherite_sword",
                                    "golden_apple",
                                    "minecraft:totem_of_undying")
                            .itemPicker());

    private int menuId = -1;

    private long openingUntil;

    private long lastCommandAt;

    private long lastConfirmAt;

    private long nextPollAt;

    private boolean confirmPending;

    private String lastFingerprint = "";

    private int sameAfterConfirm;

    private int sold;

    private int confirmedCount;

    private boolean warnedNoOpen;

    public AutoSellModule() {
        super(
                "auto-sell",
                "Auto Sell",
                "Keeps the /sell menu open and keeps selling everything that is not on your Never"
                        + " Sell list.",
                ModuleCategory.DONUT_MACROS);
        this.usesNotifications();
    }

    @Override
    public boolean persistEnabled() {
        return false;
    }

    @Override
    protected void onEnable() {
        this.menuId = -1;
        this.openingUntil = 0L;
        this.lastCommandAt = 0L;
        this.lastConfirmAt = 0L;
        this.nextPollAt = 0L;
        this.confirmPending = false;
        this.lastFingerprint = "";
        this.sameAfterConfirm = 0;
        this.sold = 0;
        this.confirmedCount = 0;
        this.warnedNoOpen = false;
    }

    @Override
    protected void onDisable() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null && this.ours(player)) {
            player.closeHandledScreen();
        }
        this.menuId = -1;
        if (this.sold > 0) {
            ModuleChat.send(
                    this,
                    ModuleChat.body()
                            .text("Sold ")
                            .value(this.sold)
                            .text(this.sold == 1 ? " item." : " items."));
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player != null
                && client.getNetworkHandler() != null
                && client.interactionManager != null
                && !player.isDead()) {
            long l = System.currentTimeMillis();
            if (this.menuId < 0 && this.openingUntil > 0L) {
                if (player.currentScreenHandler != player.playerScreenHandler
                        && client.currentScreen instanceof HandledScreen
                        && containerSlots(player.currentScreenHandler) > 53) {
                    this.menuId = player.currentScreenHandler.syncId;
                    this.openingUntil = 0L;
                    this.nextPollAt = l + 150L;
                    this.warnedNoOpen = false;
                } else if (l > this.openingUntil) {
                    this.openingUntil = 0L;
                    if (!this.warnedNoOpen) {
                        ModuleChat.send(
                                this,
                                ModuleChat.body()
                                        .warn("The sell menu did not open")
                                        .text(", retrying."));
                    }
                    this.warnedNoOpen = true;
                }
            } else if (!this.ours(player)) {
                this.menuId = -1;
                if (client.currentScreen == null && l - this.lastCommandAt >= 4000L) {
                    player.networkHandler.sendChatCommand("sell");
                    this.lastCommandAt = l;
                    this.openingUntil = l + 10000L;
                }
            } else if (l >= this.nextPollAt) {
                this.nextPollAt = l + 350L;
                ScreenHandler currentScreenHandler = player.currentScreenHandler;
                if (this.confirmedCount > 0) {
                    int i = this.confirmedCount - countWaiting(currentScreenHandler);
                    this.confirmedCount = 0;
                    if (i > 0) {
                        this.sold += i;
                        ModuleChat.actionBar(
                                this,
                                ModuleChat.body()
                                        .text("Sold ")
                                        .value(this.sold)
                                        .text(" items this session"));
                    }
                }
                if (this.confirmPending) {
                    this.confirmPending = false;
                    if (waiting(currentScreenHandler)
                            && l - this.lastConfirmAt >= 900L
                            && this.sameAfterConfirm < 3) {
                        int countWaiting = countWaiting(currentScreenHandler);
                        client.interactionManager.clickSlot(
                                currentScreenHandler.syncId, 53, 0, SlotActionType.PICKUP, player);
                        this.lastConfirmAt = l;
                        this.lastFingerprint = fingerprint(currentScreenHandler);
                        this.confirmedCount = countWaiting;
                        this.nextPollAt = l + 250L;
                    }
                } else {
                    int j = 0;
                    for (Slot slot : currentScreenHandler.slots) {
                        if (slot.inventory instanceof PlayerInventory
                                && slot.hasStack()
                                && slot.getIndex() < 36
                                && !SellRoutine.neverSell(slot.getStack(), this.neverSell.get())) {
                            client.interactionManager.clickSlot(
                                    currentScreenHandler.syncId,
                                    slot.id,
                                    0,
                                    SlotActionType.QUICK_MOVE,
                                    player);
                            j++;
                        }
                    }
                    String fingerprint = fingerprint(currentScreenHandler);
                    if (j > 0 || !fingerprint.equals(this.lastFingerprint)) {
                        this.sameAfterConfirm = 0;
                    } else if (waiting(currentScreenHandler)) {
                        this.sameAfterConfirm++;
                    }
                    if (j > 0 || waiting(currentScreenHandler)) {
                        this.confirmPending = true;
                        this.nextPollAt = l + 50L;
                    }
                }
            }
        } else {
            this.menuId = -1;
        }
    }

    private boolean ours(ClientPlayerEntity player) {
        return this.menuId >= 0
                && player.currentScreenHandler != player.playerScreenHandler
                && player.currentScreenHandler.syncId == this.menuId;
    }

    private static boolean waiting(ScreenHandler handler) {
        return countWaiting(handler) > 0;
    }

    private static int countWaiting(ScreenHandler handler) {
        int i = 0;
        for (Slot slot : handler.slots) {
            if (!(slot.inventory instanceof PlayerInventory) && slot.id != 53 && slot.hasStack()) {
                i += slot.getStack().getCount();
            }
        }
        return i;
    }

    private static int containerSlots(ScreenHandler handler) {
        int i = 0;
        for (Slot slot : handler.slots) {
            if (!(slot.inventory instanceof PlayerInventory)) {
                i++;
            }
        }
        return i;
    }

    private static String fingerprint(ScreenHandler handler) {
        StringBuilder sb = new StringBuilder();
        for (Slot slot : handler.slots) {
            if (slot.inventory instanceof PlayerInventory) {
                break;
            }
            ItemStack stack = slot.getStack();
            if (!stack.isEmpty()) {
                sb.append(slot.id)
                        .append(':')
                        .append(stack.getItem())
                        .append('x')
                        .append(stack.getCount())
                        .append(';');
            }
        }
        return sb.toString();
    }
}
