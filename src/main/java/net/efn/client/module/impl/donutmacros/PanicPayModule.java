package net.efn.client.module.impl.donutmacros;

import net.efn.client.chat.ModuleChat;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.StringSetting;
import net.efn.client.security.PayGuard;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public final class PanicPayModule extends ClientModule {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final Pattern VALID_NAME = Pattern.compile("\\.?[A-Za-z0-9_]{1,16}");

    private static final Pattern RATE_LIMIT =
            Pattern.compile("^you need to wait another ([\\d.]+) seconds?");

    private static final double MAX_WAIT_SECONDS = 30.0;

    private static final int INVENTORY_SLOTS = 36;

    private static final int CONFIRM_SLOT = 53;

    private static final int MAX_STRIKES = 2;

    private static final int MAX_SELL_ROUNDS = 6;

    private static final int[] ARMOR_MENU_SLOTS = new int[] {5, 6, 7, 8};

    private static final int OFFHAND_MENU_SLOT = 45;

    private static final long COMMAND_GAP_MS = 350L;

    private static final long OPEN_TIMEOUT_MS = 8000L;

    private static final long REPLY_TIMEOUT_MS = 6000L;

    private static final long MENU_SETTLE_MS = 50L;

    private static final long CONFIRM_WAIT_MS = 700L;

    private static final long RECONFIRM_WAIT_MS = 900L;

    private static final long RETURN_WAIT_MS = 600L;

    private final StringSetting payTo =
            this.addSetting("pay-to", new StringSetting("Pay To", "").hint("Player name"));

    private final BooleanSetting sellArmor =
            this.addSetting("sell-armor", new BooleanSetting("Sell Armor", true));

    private final BooleanSetting sellOffhand =
            this.addSetting("sell-offhand", new BooleanSetting("Sell Offhand", true));

    private final MacroWorker worker = new MacroWorker("EFN Panic Pay");

    private volatile boolean running;

    private volatile long paidAt;

    private volatile String paidText = "";

    private volatile long poorAt;

    private volatile long errorAt;

    private volatile String errorText = "";

    private volatile long holdUntil;

    private volatile long limitedAt;

    private volatile long balanceAt;

    private volatile double balance;

    private volatile String balanceText = "";

    private long lastCommandAt;

    public PanicPayModule() {
        super(
                "panic-pay",
                "Panic Pay",
                "Sells your whole inventory and armor with /sell, then pays all your money to one"
                        + " player.",
                ModuleCategory.DONUT_MACROS);
        this.usesNotifications();
        this.payTo.sessionOnly();
    }

    @Override
    public boolean persistEnabled() {
        return false;
    }

    @Override
    public boolean inConfigs() {
        return false;
    }

    @Override
    protected void onEnable() {
        MinecraftClient client = MinecraftClient.getInstance();
        String string = this.payTo.get().trim();
        if (client.player == null || client.getNetworkHandler() == null) {
            this.say(ModuleChat.body().warn("Join a server first."));
            this.requestDisable();
        } else if (!VALID_NAME.matcher(string).matches()) {
            this.say(ModuleChat.body().warn("Set Pay To to a player name first."));
            this.requestDisable();
        } else if (string.equalsIgnoreCase(client.getSession().getUsername())) {
            this.say(ModuleChat.body().warn("Pay To is your own name."));
            this.requestDisable();
        } else {
            boolean sellArmor2 = this.sellArmor.get();
            boolean sellOffhand2 = this.sellOffhand.get();
            this.running = true;
            this.lastCommandAt = 0L;
            this.say(
                    ModuleChat.body()
                            .danger("Panic Pay started")
                            .text(": selling everything, then paying ")
                            .name(string)
                            .text(". Press the key again to cancel."));
            this.worker.start(
                    () -> {
                        try {
                            this.run(new Gui(this.worker), string, sellArmor2, sellOffhand2);
                        } catch (AuctionHouse.Problem exception) {
                            this.say(
                                    ModuleChat.body()
                                            .warn(exception.getMessage())
                                            .text(", stopped."));
                        } finally {
                            this.finish(client);
                        }
                    },
                    throwable -> {
                        LOGGER.error("[EFN] Panic Pay stopped", throwable);
                        this.say(ModuleChat.body().danger("Stopped").text(": " + throwable));
                        this.finish(client);
                    });
        }
    }

    private void finish(MinecraftClient client) {
        client.execute(
                () -> {
                    if (this.running && this.isEnabled()) {
                        this.setEnabled(false);
                    }
                });
    }

    @Override
    protected void onDisable() {
        boolean running2 = this.running;
        this.running = false;
        this.worker.stop();
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (running2
                && player != null
                && player.currentScreenHandler != player.playerScreenHandler) {
            player.closeHandledScreen();
        }
    }

    @EventHandler
    public void onPacket(PacketReceiveNettyEvent event) {
        if (this.running) {
            Packet packet = event.packet;
            GameMessageS2CPacket gameMessagePacket;
            if (event.packet instanceof GameMessageS2CPacket
                    && (gameMessagePacket = (GameMessageS2CPacket) packet).content() != null) {
                String string = gameMessagePacket.content().getString();
                String string2 = ServerText.normalize(string);
                long l = System.currentTimeMillis();
                Matcher matcher = RATE_LIMIT.matcher(string2);
                if (matcher.find()) {
                    try {
                        this.holdUntil =
                                l
                                        + (long)
                                                Math.ceil(
                                                        Math.min(
                                                                        30.0,
                                                                        Double.parseDouble(
                                                                                matcher.group(1)))
                                                                * 1000.0)
                                        + 100L;
                        this.limitedAt = l;
                    } catch (NumberFormatException exception) {
                    }
                    return;
                }
                if (string2.startsWith("you paid ")) {
                    this.paidText = string2;
                    this.paidAt = l;
                } else if (string2.contains("enough money")) {
                    this.poorAt = l;
                } else if (!string2.startsWith("you have $")
                        && (!string2.startsWith("balance") || !string2.contains("$"))) {
                    if (string2.contains("not online")
                            || string2.contains("not found")
                            || string2.contains("cannot pay")
                            || string2.contains("can't pay")
                            || string2.contains("not a valid")
                            || string2.contains("invalid")
                            || string2.contains("doesn't exist")
                            || string2.contains("does not exist")) {
                        this.errorText = string;
                        this.errorAt = l;
                    }
                } else {
                    String string3 = string2.substring(string2.indexOf(36));
                    List list = ServerText.moneyAll(string3);
                    String moneyToken = ServerText.moneyToken(string3);
                    if (!list.isEmpty() && moneyToken != null) {
                        this.balance = (Double) list.get(0);
                        this.balanceText = moneyToken;
                        this.balanceAt = l;
                    }
                }
                return;
            }
        }
    }

    private void run(Gui gui, String string, boolean bl, boolean bl2) {
        gui.closeAll();
        HashMap map = new HashMap();
        int i = 0;
        try {
            if (bl || bl2) {
                this.takeOff(gui, bl, bl2, true);
            }
            i = this.sellRounds(gui, map);
            if ((bl || bl2) && this.takeOff(gui, bl, bl2, false)) {
                i += this.sellRounds(gui, map);
            }
        } catch (AuctionHouse.Problem exception) {
            this.say(
                    ModuleChat.body().warn(exception.getMessage()).text(", paying what you have."));
            gui.closeAll();
        }
        this.say(
                ModuleChat.body()
                        .text("Sold ")
                        .value(i)
                        .text(i == 1 ? " item" : " items")
                        .text(", paying ")
                        .name(string)
                        .text("."));
        this.payBalance(string);
    }

    private int sellRounds(Gui gui, Map<Item, Integer> map) {
        int i = 0;
        Map map2;
        for (int j = 0;
                j < 6 && !(map2 = this.worker.call(() -> inventoryCounts(map))).isEmpty();
                j++) {
            this.openSell(gui);
            int k =
                    this.worker.call(
                            () -> {
                                ClientPlayerEntity player = Gui.player();
                                ArrayList list = new ArrayList();
                                for (Slot slot : player.currentScreenHandler.slots) {
                                    if (slot.inventory instanceof PlayerInventory
                                            && slot.hasStack()
                                            && slot.getIndex() < 36
                                            && map.getOrDefault(slot.getStack().getItem(), 0) < 2) {
                                        list.add(slot.id);
                                    }
                                }
                                for (int m : (Iterable<Integer>) list) {
                                    Gui.mc()
                                            .interactionManager
                                            .clickSlot(
                                                    player.currentScreenHandler.syncId,
                                                    m,
                                                    0,
                                                    SlotActionType.QUICK_MOVE,
                                                    player);
                                }
                                return list.size();
                            });
            this.worker.ticks(2);
            Map map3 = k > 0 ? this.worker.call(PanicPayModule::waitingItems) : Map.of();
            Map map4 = map3.isEmpty() ? map3 : this.confirm(gui, map3);
            gui.closeAll();
            if (map4 == null) {
                this.worker.sleep(600L);
            } else if (!map4.isEmpty()) {
                int n = total(map2) - total(map3) + total(map4);
                this.worker.waitFor(() -> total(inventoryCounts(map)) >= n, 600L);
            }
            Map map5 = this.worker.call(() -> inventoryCounts(map));
            int o = 0;
            for (Entry entry : (Iterable<Entry>) map2.entrySet()) {
                o +=
                        Math.max(
                                0,
                                (Integer) entry.getValue()
                                        - (Integer) map5.getOrDefault(entry.getKey(), 0));
            }
            i += o;
            for (Entry entry2 : (Iterable<Entry>) map2.entrySet()) {
                if ((Integer) map5.getOrDefault(entry2.getKey(), 0)
                        >= (Integer) entry2.getValue()) {
                    int p = o > 0 ? 2 : map.getOrDefault(entry2.getKey(), 0) + 1;
                    map.put((Item) entry2.getKey(), p);
                    if (p == 2) {
                        this.say(
                                ModuleChat.body()
                                        .warn("Could not sell ")
                                        .name(
                                                new ItemStack((ItemConvertible) entry2.getKey())
                                                        .getName()
                                                        .getString())
                                        .text(", skipping it."));
                    }
                }
            }
        }
        return i;
    }

    private Map<Item, Integer> confirm(Gui gui, Map<Item, Integer> map) {
        Map map2 = map;
        for (int i = 0; i < 2 && map2.equals(map); i++) {
            if (!this.worker.call(PanicPayModule::sellMenuOpen)) {
                return null;
            }
            gui.click(53, 0, SlotActionType.PICKUP);
            Map map3 = map2;
            this.worker.waitFor(
                    () -> !sellMenuOpen() || !waitingItems().equals(map3), i == 0 ? 700L : 900L);
            map2 = this.worker.call(() -> sellMenuOpen() ? waitingItems() : null);
            if (map2 == null) {
                return null;
            }
        }
        return map2;
    }

    private static Map<Item, Integer> waitingItems() {
        HashMap map = new HashMap();
        for (Slot slot : Gui.player().currentScreenHandler.slots) {
            if (!(slot.inventory instanceof PlayerInventory) && slot.id != 53 && slot.hasStack()) {
                map.merge(
                        slot.getStack().getItem(),
                        slot.getStack().getCount(),
                        (a, value) -> (Integer) a + (Integer) value);
            }
        }
        return map;
    }

    private static int total(Map<Item, Integer> map) {
        int i = 0;
        for (int j : map.values()) {
            i += j;
        }
        return i;
    }

    private static Map<Item, Integer> inventoryCounts(Map<Item, Integer> map) {
        HashMap map2 = new HashMap();
        PlayerInventory inventory = Gui.player().getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty() && map.getOrDefault(stack.getItem(), 0) < 2) {
                map2.merge(
                        stack.getItem(),
                        stack.getCount(),
                        (a, value) -> (Integer) a + (Integer) value);
            }
        }
        return map2;
    }

    private void openSell(Gui gui) {
        for (int i = 0; i < 3; i++) {
            long command = this.command("sell");
            this.worker.waitFor(() -> sellMenuOpen() || this.limitedAt >= command, 8000L);
            if (this.worker.call(PanicPayModule::sellMenuOpen)) {
                this.worker.sleep(50L);
                return;
            }
            gui.closeAll();
        }
        throw new AuctionHouse.Problem("The sell menu did not open");
    }

    private static boolean sellMenuOpen() {
        ClientPlayerEntity player = Gui.mc().player;
        if (player != null
                && player.currentScreenHandler != player.playerScreenHandler
                && Gui.mc().currentScreen instanceof HandledScreen) {
            int i = 0;
            for (Slot slot : player.currentScreenHandler.slots) {
                if (!(slot.inventory instanceof PlayerInventory)) {
                    i++;
                }
            }
            return i > 53;
        } else {
            return false;
        }
    }

    private boolean takeOff(Gui gui, boolean bl, boolean bl2, boolean bl3) {
        gui.closeAll();
        boolean bl4 =
                this.worker.call(
                        () -> {
                            ClientPlayerEntity player = Gui.player();
                            ArrayList list = new ArrayList();
                            if (bl) {
                                for (int i : ARMOR_MENU_SLOTS) {
                                    if (player.playerScreenHandler.getSlot(i).hasStack()) {
                                        list.add(i);
                                    }
                                }
                            }
                            if (bl2 && player.playerScreenHandler.getSlot(45).hasStack()) {
                                list.add(45);
                            }
                            int j = 0;
                            for (int k = 0; k < 36; k++) {
                                if (player.getInventory().getStack(k).isEmpty()) {
                                    j++;
                                }
                            }
                            int m = bl3 ? Math.min(j, list.size()) : list.size();
                            for (int n = 0; n < m; n++) {
                                Gui.mc()
                                        .interactionManager
                                        .clickSlot(
                                                player.playerScreenHandler.syncId,
                                                (Integer) list.get(n),
                                                0,
                                                SlotActionType.QUICK_MOVE,
                                                player);
                            }
                            return m > 0;
                        });
        if (!bl4) {
            return false;
        } else {
            gui.closeInventory();
            this.worker.ticks(2);
            return true;
        }
    }

    private void payBalance(String string) {
        String balance2 = this.readBalance();
        if (this.balance < 1.0) {
            this.say(ModuleChat.body().warn("You have no money to pay."));
        } else {
            double displayStep = ServerText.displayStep(this.balanceText);
            int i = 0;
            for (int j = 0; j < 4; j++) {
                PanicPayModule.Payment payment = this.pay(string, balance2);
                if (payment == PanicPayModule.Payment.PAID) {
                    this.say(
                            ModuleChat.body()
                                    .good("Paid $" + balance2)
                                    .text(" to ")
                                    .name(string)
                                    .text("."));
                    if (!mentions(this.paidText, string)) {
                        this.say(
                                ModuleChat.body()
                                        .danger("Check the payment")
                                        .text(": the server said \"" + this.paidText + "\"."));
                    }
                    return;
                }
                if (payment == PanicPayModule.Payment.POOR) {
                    i++;
                    long l = (long) Math.floor(this.balance - displayStep * i);
                    if (i > 2 || l < 1L) {
                        throw new AuctionHouse.Problem(
                                "/pay "
                                        + string
                                        + " "
                                        + balance2
                                        + " was refused: not enough money");
                    }
                    balance2 = Long.toString(l);
                } else if (payment == PanicPayModule.Payment.FAILED) {
                    throw new AuctionHouse.Problem("/pay failed: " + this.errorText);
                }
            }
            throw new AuctionHouse.Problem("No answer to /pay " + string + " " + balance2);
        }
    }

    private String readBalance() {
        for (int i = 0; i < 3; i++) {
            long command = this.command("bal");
            this.worker.waitFor(
                    () -> this.balanceAt >= command || this.limitedAt >= command, 6000L);
            if (this.balanceAt >= command) {
                return this.balanceText;
            }
        }
        throw new AuctionHouse.Problem("Could not read your balance from /bal");
    }

    private PanicPayModule.Payment pay(String string, String string2) {
        long command = this.command("pay " + string + " " + string2);
        Object[] objects = new Object[] {null};
        long l = command + 6000L;
        while (System.currentTimeMillis() < l) {
            if (this.paidAt >= command) {
                return PanicPayModule.Payment.PAID;
            }
            if (this.poorAt >= command) {
                return PanicPayModule.Payment.POOR;
            }
            if (this.errorAt >= command) {
                return PanicPayModule.Payment.FAILED;
            }
            if (this.limitedAt >= command) {
                return PanicPayModule.Payment.RETRY;
            }
            boolean bl =
                    this.worker.call(
                            () -> {
                                Dialogs.View view = Dialogs.current(Gui.mc());
                                if (view != null && view.dialog() != objects[0]) {
                                    Dialogs.Button button = view.labelled("confirm", "yes", "pay");
                                    if (button == null) {
                                        return false;
                                    } else if (namesOtherPlayer(view, string)) {
                                        Dialogs.close(Gui.mc());
                                        objects[0] = view.dialog();
                                        return true;
                                    } else {
                                        if (Dialogs.press(Gui.mc(), view, button, Map.of())) {
                                            objects[0] = view.dialog();
                                        }
                                        return false;
                                    }
                                } else {
                                    return false;
                                }
                            });
            if (bl) {
                this.errorText = "the confirmation named a different player than " + string;
                return PanicPayModule.Payment.FAILED;
            }
            this.worker.sleep(50L);
        }
        return PanicPayModule.Payment.RETRY;
    }

    private static boolean mentions(String string, String string2) {
        String string3 = string2.toLowerCase(Locale.ROOT);
        if (string3.startsWith(".")) {
            string3 = string3.substring(1);
        }
        for (String string4 : string.split("[^a-z0-9_]+")) {
            if (string4.equals(string3)) {
                return true;
            }
        }
        return false;
    }

    private static boolean namesOtherPlayer(Dialogs.View view, String string) {
        String string2 = string.toLowerCase(Locale.ROOT);
        if (string2.startsWith(".")) {
            string2 = string2.substring(1);
        }
        ArrayList list = new ArrayList<>(view.body());
        list.add(view.title());
        for (String string3 : (Iterable<String>) list) {
            for (String string4 : string3.split("[^a-z0-9_]+")) {
                if (string4.length() > string2.length() && string4.startsWith(string2)) {
                    return true;
                }
            }
        }
        return false;
    }

    private long command(String string) {
        long l = Math.max(this.holdUntil, this.lastCommandAt + 350L) - System.currentTimeMillis();
        if (l > 0L) {
            this.worker.sleep(l);
        }
        long l2 = System.currentTimeMillis();
        this.worker.run(
                () -> {
                    PayGuard.allow(string);
                    Gui.player().networkHandler.sendChatCommand(string);
                });
        this.lastCommandAt = l2;
        return l2;
    }

    private void say(ModuleChat.Body body) {
        ModuleChat.send(this, body);
    }

    @Environment(EnvType.CLIENT)
    private static enum Payment {
        PAID,
        POOR,
        RETRY,
        FAILED;

        private Payment() {}
    }
}
