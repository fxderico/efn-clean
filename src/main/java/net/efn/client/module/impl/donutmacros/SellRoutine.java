/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.module.impl.donutmacros;

import net.efn.client.chat.ModuleChat;
import net.efn.client.module.ClientModule;
import net.efn.client.module.setting.StringListSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiPredicate;

@Environment(value = EnvType.CLIENT)
final class SellRoutine {

    private static final int INVENTORY_SLOTS = 36;

    private static final int CONFIRM_SLOT = 53;

    private static final int MAX_CONFIRMS = 3;

    private static final int STRIKES_TO_SKIP = 2;

    private static final int MAX_RESENDS = 3;

    private static final long CHECK_EVERY_MS = 500L;

    private static final long COMMAND_COOLDOWN_MS = 4000L;

    private static final long OPEN_TIMEOUT_MS = 10000L;

    private static final long STRAY_MENU_MS = 5000L;

    private static final long FAIL_COOLDOWN_MS = 30000L;

    private static final long CONFIRM_COOLDOWN_MS = 900L;

    private static final long POST_CONFIRM_MS = 250L;

    private static final long SKIP_MS = 600000L;

    private final ClientModule owner;

    private final StringListSetting neverSell;

    private final BiPredicate<Integer, ItemStack> keep;

    private final Runnable onCommand;

    private State state = State.IDLE;

    private long nextCheckAt;

    private long lastCommandAt;

    private long deadline;

    private boolean rejected;

    private long retryAt;

    private int resends;

    private int menuId = -1;

    private int confirms;

    private final Map<Item, Integer> offered = new LinkedHashMap<Item, Integer>();

    private final Map<Item, Integer> strikes = new HashMap<Item, Integer>();

    private final Map<Item, Long> skipUntil = new HashMap<Item, Long>();

    SellRoutine(
            ClientModule owner2,
            StringListSetting neverSell2,
            BiPredicate<Integer, ItemStack> keep2,
            Runnable onCommand2) {
        this.owner = owner2;
        this.neverSell = neverSell2;
        this.keep = keep2;
        this.onCommand = onCommand2;
    }

    void reset() {
        this.state = State.IDLE;
        this.nextCheckAt = 0L;
        this.lastCommandAt = 0L;
        this.rejected = false;
        this.menuId = -1;
        this.offered.clear();
        this.strikes.clear();
        this.skipUntil.clear();
    }

    boolean active() {
        return this.state != State.IDLE;
    }

    void commandRejected(long retryAt2) {
        if (this.state != State.OPENING) {
            return;
        }
        this.rejected = true;
        this.retryAt = retryAt2;
    }

    void abort(MinecraftClient client) {
        if (this.state == State.IDLE) {
            return;
        }
        ClientPlayerEntity player = client.player;
        if (player != null
                && player.currentScreenHandler != player.playerScreenHandler
                && player.currentScreenHandler.syncId == this.menuId) {
            player.closeHandledScreen();
        }
        this.state = State.IDLE;
        this.menuId = -1;
        this.offered.clear();
        this.nextCheckAt = System.currentTimeMillis() + 500L;
    }

    boolean step(
            MinecraftClient client,
            ClientPlayerEntity player,
            ClientPlayNetworkHandler clientPlayNetworkHandler,
            long l,
            boolean bl) {
        return switch (this.state.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> this.stepIdle(client, player, clientPlayNetworkHandler, l, bl);
            case 1 -> this.stepOpening(client, player, clientPlayNetworkHandler, l);
            case 2 -> this.stepMoving(client, player, l);
            case 3 -> this.stepConfirming(client, player, l);
        };
    }

    private boolean stepIdle(
            MinecraftClient client,
            ClientPlayerEntity player,
            ClientPlayNetworkHandler clientPlayNetworkHandler,
            long l,
            boolean bl) {
        if (l - this.lastCommandAt < 5000L && SellRoutine.sellMenuOpen(client, player)) {
            player.closeHandledScreen();
            return true;
        }
        if (l < this.nextCheckAt) {
            return false;
        }
        this.nextCheckAt = l + 500L;
        if (client.currentScreen != null || !bl || l - this.lastCommandAt < 4000L) {
            return false;
        }
        if (!this.hasSellable(player.getInventory(), l)) {
            return false;
        }
        this.resends = 0;
        this.send(clientPlayNetworkHandler, l);
        return true;
    }

    private void send(ClientPlayNetworkHandler clientPlayNetworkHandler, long lastCommandAt2) {
        clientPlayNetworkHandler.sendChatCommand("sell");
        this.lastCommandAt = lastCommandAt2;
        this.onCommand.run();
        this.deadline = lastCommandAt2 + 10000L;
        this.rejected = false;
        this.state = State.OPENING;
    }

    private boolean stepOpening(
            MinecraftClient client,
            ClientPlayerEntity player,
            ClientPlayNetworkHandler clientPlayNetworkHandler,
            long l) {
        if (SellRoutine.sellMenuOpen(client, player)) {
            this.menuId = player.currentScreenHandler.syncId;
            this.state = State.MOVING;
            return true;
        }
        if (this.rejected && l >= this.retryAt && this.resends < 3) {
            ++this.resends;
            this.send(clientPlayNetworkHandler, l);
            return true;
        }
        if (l < this.deadline) {
            return true;
        }
        this.state = State.IDLE;
        ModuleChat.send(
                this.owner,
                ModuleChat.body()
                        .warn("The sell menu did not open")
                        .text(", trying again in 30s."));
        this.nextCheckAt = l + 30000L;
        return false;
    }

    private boolean stepMoving(MinecraftClient client, ClientPlayerEntity player, long deadline2) {
        ScreenHandler currentScreenHandler = player.currentScreenHandler;
        if (!this.ours(player)) {
            return this.finish(null, deadline2);
        }
        int i = 0;
        for (Slot slot : currentScreenHandler.slots) {
            int index;
            if (!(slot.inventory instanceof PlayerInventory)
                    || !slot.hasStack()
                    || (index = slot.getIndex()) >= 36
                    || !this.sellable(index, slot.getStack(), deadline2)) continue;
            client.interactionManager.clickSlot(
                    currentScreenHandler.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, player);
            ++i;
        }
        if (i == 0) {
            return this.close(player, null, deadline2);
        }
        this.offered.clear();
        this.offered.putAll(SellRoutine.waitingItems(currentScreenHandler));
        this.confirms = 0;
        this.deadline = deadline2;
        this.state = State.CONFIRMING;
        return true;
    }

    private boolean stepConfirming(MinecraftClient client, ClientPlayerEntity player, long l) {
        if (!this.ours(player)) {
            return this.finish(null, l);
        }
        if (l < this.deadline) {
            return true;
        }
        ScreenHandler currentScreenHandler = player.currentScreenHandler;
        if (this.confirms < 3 && SellRoutine.itemsWaiting(currentScreenHandler)) {
            client.interactionManager.clickSlot(
                    currentScreenHandler.syncId, 53, 0, SlotActionType.PICKUP, player);
            ++this.confirms;
            this.deadline = l + (this.confirms == 1 ? 250L : 900L);
            return true;
        }
        return this.close(player, SellRoutine.waitingItems(currentScreenHandler), l);
    }

    private boolean close(ClientPlayerEntity player, Map<Item, Integer> map, long l) {
        if (this.ours(player)) {
            player.closeHandledScreen();
        }
        return this.finish(map, l);
    }

    private boolean finish(Map<Item, Integer> map, long l) {
        if (map != null) {
            this.settle(map, l);
        }
        this.offered.clear();
        this.menuId = -1;
        this.state = State.IDLE;
        this.nextCheckAt = l + 500L;
        return false;
    }

    private void settle(Map<Item, Integer> map, long l) {
        int i = 0;
        for (Map.Entry<Item, Integer> entry : this.offered.entrySet()) {
            Item key = entry.getKey();
            int j = map.getOrDefault(key, 0);
            i += Math.max(0, entry.getValue() - j);
            if (j <= 0) {
                this.strikes.remove(key);
                continue;
            }
            if (this.strikes.merge(key, 1, Integer::sum) < 2) continue;
            this.strikes.remove(key);
            this.skipUntil.put(key, l + 600000L);
            ModuleChat.send(
                    this.owner,
                    ModuleChat.body()
                            .warn("Could not sell ")
                            .name(key.getName().getString())
                            .text(", skipping it for 10 minutes."));
        }
        if (i > 0) {
            ModuleChat.send(
                    this.owner,
                    ModuleChat.body().text("Sold ").value(i).text(i == 1 ? " item." : " items."));
        }
    }

    private boolean ours(ClientPlayerEntity player) {
        return player.currentScreenHandler != player.playerScreenHandler
                && player.currentScreenHandler.syncId == this.menuId;
    }

    private boolean hasSellable(PlayerInventory playerInventory, long l) {
        for (int i = 0; i < 36; ++i) {
            ItemStack stack = playerInventory.getStack(i);
            if (stack.isEmpty() || !this.sellable(i, stack, l)) continue;
            return true;
        }
        return false;
    }

    private boolean sellable(int i, ItemStack stack, long l) {
        return !this.skipped(stack.getItem(), l)
                && !this.keep.test(i, stack)
                && !this.blacklisted(stack);
    }

    private boolean skipped(Item item, long l) {
        Long long_Value = this.skipUntil.get(item);
        if (long_Value == null) {
            return false;
        }
        if (l < long_Value) {
            return true;
        }
        this.skipUntil.remove(item);
        return false;
    }

    private boolean blacklisted(ItemStack stack) {
        return SellRoutine.neverSell(stack, this.neverSell.get());
    }

    static boolean neverSell(ItemStack stack, List<String> list) {
        String string = Registries.ITEM.getId(stack.getItem()).toString().toLowerCase(Locale.ROOT);
        String string2 = string.substring(string.indexOf(58) + 1);
        for (String string3 : list) {
            String string4 = string3.trim().toLowerCase(Locale.ROOT);
            if (string4.isEmpty()) continue;
            if (string.equals(string4) || string2.equals(string4)) {
                return true;
            }
            if (string4.indexOf(58) >= 0 || !string.contains(string4)) continue;
            return true;
        }
        return false;
    }

    private static boolean itemsWaiting(ScreenHandler handler) {
        for (Slot slot : handler.slots) {
            if (slot.inventory instanceof PlayerInventory || slot.id == 53 || !slot.hasStack())
                continue;
            return true;
        }
        return false;
    }

    private static Map<Item, Integer> waitingItems(ScreenHandler handler) {
        LinkedHashMap<Item, Integer> map = new LinkedHashMap<Item, Integer>();
        for (Slot slot : handler.slots) {
            if (slot.inventory instanceof PlayerInventory || slot.id == 53 || !slot.hasStack())
                continue;
            map.merge(slot.getStack().getItem(), slot.getStack().getCount(), Integer::sum);
        }
        return map;
    }

    private static boolean sellMenuOpen(MinecraftClient client, ClientPlayerEntity player) {
        ScreenHandler currentScreenHandler = player.currentScreenHandler;
        return currentScreenHandler != player.playerScreenHandler
                && client.currentScreen instanceof HandledScreen
                && SellRoutine.containerSlots(currentScreenHandler) > 53;
    }

    private static int containerSlots(ScreenHandler handler) {
        int i = 0;
        for (Slot slot : handler.slots) {
            if (slot.inventory instanceof PlayerInventory) continue;
            ++i;
        }
        return i;
    }

    @Environment(value = EnvType.CLIENT)
    private static enum State {
        IDLE,
        OPENING,
        MOVING,
        CONFIRMING
    }
}
