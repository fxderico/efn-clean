package net.efn.client.module.impl.donutmacros;

import net.efn.client.chat.ModuleChat;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Environment(EnvType.CLIENT)
public final class OrderDropperModule extends ClientModule {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final int ITEM_SLOTS = 45;

    private static final int DEFAULT_COLLECT_BUTTON = 13;

    private static final int DEFAULT_DROP_BUTTON = 52;

    private static final int DEFAULT_NEXT_BUTTON = 53;

    private static final int MAX_EMPTY_FLIPS = 2;

    private static final long NEXT_PAGE_SETTLE_MS = 250L;

    private static final long OPEN_TIMEOUT_MS = 5000L;

    private static final long PAGE_TIMEOUT_MS = 4000L;

    private final NumberSetting orderSlot =
            this.addSetting(
                    "order-slot",
                    new NumberSetting("Order Slot (0 = first)", 0.0F, 0.0F, 44.0F).step(1.0F));

    private final NumberSetting pages =
            this.addSetting(
                    "pages", new NumberSetting("Pages To Drop", 1.0F, 1.0F, 100.0F).step(1.0F));

    private final NumberSetting delay =
            this.addSetting(
                    "delay", new NumberSetting("Drop Delay (s)", 0.5F, 0.25F, 5.0F).step(0.25F));

    private final MacroWorker worker = new MacroWorker("EFN Order Dropper");

    private volatile boolean running;

    public OrderDropperModule() {
        super(
                "order-dropper",
                "Order Dropper",
                "Opens one of your orders, collects it and drops the chosen number of pages, then"
                        + " turns off.",
                ModuleCategory.DONUT_MACROS);
        this.usesNotifications();
    }

    @Override
    public boolean persistEnabled() {
        return false;
    }

    @Override
    protected void onEnable() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.getNetworkHandler() != null) {
            int orderSlot2 = this.orderSlot.getInt();
            int pages2 = this.pages.getInt();
            long l = (long) (this.delay.get() * 1000.0F);
            this.running = true;
            this.worker.start(
                    () -> {
                        try {
                            this.drop(new Gui(this.worker), orderSlot2, pages2, l);
                        } catch (AuctionHouse.Problem exception) {
                            this.say(ModuleChat.body().warn(exception.getMessage()).text("."));
                        } finally {
                            this.finish(client);
                        }
                    },
                    throwable -> {
                        LOGGER.error("[EFN] Order Dropper stopped", throwable);
                        this.say(ModuleChat.body().danger("Stopped").text(": " + throwable));
                        this.finish(client);
                    });
        } else {
            this.say(ModuleChat.body().warn("Join a server first."));
            this.requestDisable();
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

    private void drop(Gui gui, int i, int j, long l) {
        ItemStack[] stacks = new ItemStack[] {ItemStack.EMPTY};
        String openCollect = this.openCollect(gui, i, stacks);
        int k = 0;
        int m = 0;
        for (int n = 0; n < j; n++) {
            if (!this.worker.call(Gui::containerOpenNow)) {
                openCollect = this.openCollect(gui, i, stacks);
            }
            int[] ints = this.worker.call(OrderDropperModule::pageState);
            for (int o = 0; ints[0] == 0 && ints[2] >= 0 && o < 2; o++) {
                String fingerprint = gui.fingerprint();
                gui.click(ints[2], 0, SlotActionType.PICKUP);
                gui.waitChange(fingerprint, 4000L);
                this.worker.sleep(250L);
                ints = this.worker.call(OrderDropperModule::pageState);
            }
            if (ints[0] == 0) {
                this.say(ModuleChat.body().text("The order is empty."));
                break;
            }
            if (ints[1] < 0) {
                throw new AuctionHouse.Problem("No Drop button in the collect menu");
            }
            String fingerprint2 = gui.fingerprint();
            gui.click(ints[1], 0, SlotActionType.PICKUP);
            k++;
            m += ints[0];
            if (n < j - 1) {
                gui.waitChange(fingerprint2, 4000L);
                this.worker.sleep(l);
            }
        }
        this.worker.sleep(300L);
        gui.closeAll();
        this.say(
                ModuleChat.body()
                        .good("Dropped " + k + (k == 1 ? " page" : " pages"))
                        .text(" (")
                        .value(m)
                        .text(" items) from ")
                        .name(openCollect)
                        .text("."));
    }

    private String openCollect(Gui gui, int i, ItemStack[] stacks) {
        if (!OrdersNav.openYourOrders(this.worker, gui, this::say)) {
            throw new AuctionHouse.Problem("Could not open Your Orders");
        } else {
            ItemStack stack = this.worker.call(() -> Gui.slotNow(i));
            if (OrdersNav.isPane(stack)) {
                throw new AuctionHouse.Problem("There is no order in slot " + i);
            } else {
                String string = stack.getName().getString();
                if (stacks[0].isEmpty()) {
                    stacks[0] = stack.copy();
                } else if (!stack.isOf(stacks[0].getItem())
                        || !string.equals(stacks[0].getName().getString())) {
                    gui.closeAll();
                    throw new AuctionHouse.Problem(
                            "Slot "
                                    + i
                                    + " now holds a different order ("
                                    + string
                                    + "), stopped so nothing else is dropped");
                }
                String fingerprint = gui.fingerprint();
                gui.click(i, 0, SlotActionType.PICKUP);
                if (!this.worker.waitFor(
                        () ->
                                Gui.containerOpenNow()
                                        && !OrdersNav.isYourOrdersNow()
                                        && !Gui.fingerprintNow().equals(fingerprint),
                        5000L)) {
                    throw new AuctionHouse.Problem("The order in slot " + i + " did not open");
                } else {
                    this.worker.sleep(300L);
                    int j =
                            this.worker.call(
                                    () -> {
                                        List list = Gui.containerNow();
                                        for (int k = 0; k < list.size(); k++) {
                                            if (!((ItemStack) list.get(k)).isEmpty()
                                                    && Gui.nameOf((ItemStack) list.get(k))
                                                            .contains("collect")) {
                                                return k;
                                            }
                                        }
                                        return list.size() > 13
                                                        && !((ItemStack) list.get(13)).isEmpty()
                                                ? 13
                                                : -1;
                                    });
                    if (j < 0) {
                        throw new AuctionHouse.Problem("No Collect button in the order");
                    } else {
                        String fingerprint2 = gui.fingerprint();
                        gui.click(j, 0, SlotActionType.PICKUP);
                        if (!this.worker.waitFor(
                                () ->
                                        Gui.containerOpenNow()
                                                && !Gui.fingerprintNow().equals(fingerprint2)
                                                && dropButton(Gui.containerNow()) >= 0,
                                5000L)) {
                            throw new AuctionHouse.Problem("The collect menu did not open");
                        } else {
                            this.worker.sleep(300L);
                            return string;
                        }
                    }
                }
            }
        }
    }

    private static int[] pageState() {
        List list = Gui.containerNow();
        int i = 0;
        for (int j = 0; j < 45 && j < list.size(); j++) {
            i += ((ItemStack) list.get(j)).getCount();
        }
        return new int[] {i, dropButton(list), nextButton(list)};
    }

    private static int nextButton(List<ItemStack> list) {
        for (int i = 45; i < list.size(); i++) {
            ItemStack stack = (ItemStack) list.get(i);
            if (!stack.isEmpty() && Gui.nameOf(stack).contains("next")) {
                return i;
            }
        }
        return list.size() > 53 && ((ItemStack) list.get(53)).isOf(Items.ARROW) ? 53 : -1;
    }

    private static int dropButton(List<ItemStack> list) {
        for (int i = 45; i < list.size(); i++) {
            ItemStack stack = (ItemStack) list.get(i);
            if (!stack.isEmpty()
                    && (stack.isOf(Items.DROPPER) || Gui.nameOf(stack).equals("drop"))) {
                return i;
            }
        }
        return list.size() > 52 && ((ItemStack) list.get(52)).isOf(Items.DROPPER) ? 52 : -1;
    }

    private void say(ModuleChat.Body body) {
        ModuleChat.send(this, body);
    }
}
