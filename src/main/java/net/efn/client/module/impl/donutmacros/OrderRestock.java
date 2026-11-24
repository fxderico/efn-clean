package net.efn.client.module.impl.donutmacros;

import net.efn.client.chat.ModuleChat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
final class OrderRestock {

    private static final int ORDER_SLOTS = 45;

    private static final int DEFAULT_COLLECT_BUTTON = 13;

    private static final long OPEN_TIMEOUT_MS = 10000L;

    private static final long DIALOG_TIMEOUT_MS = 8000L;

    private static final long PICK_TIMEOUT_MS = 5000L;

    private final OrderAhBot bot;

    private final MacroWorker worker;

    private final Gui gui;

    private final Product product;

    private long nextCheckAt;

    OrderRestock(OrderAhBot bot2) {
        this.bot = bot2;
        this.worker = bot2.worker;
        this.gui = bot2.gui;
        this.product = bot2.product;
    }

    private void say(ModuleChat.Body body) {
        this.bot.say(body);
    }

    private static AuctionHouse.Problem fail(String string) {
        return new AuctionHouse.Problem(string);
    }

    boolean run() {
        long l = System.currentTimeMillis();
        if (l < this.nextCheckAt) {
            return false;
        } else {
            this.nextCheckAt = l + this.bot.config.orderCheckMs();
            if (!this.openYourOrders()) {
                return false;
            } else {
                int i =
                        this.worker.call(
                                () ->
                                        Gui.findNow(
                                                itemStack -> itemStack.isOf(this.product.item()),
                                                45));
                if (i >= 0) {
                    return this.collect(i) > 0;
                } else {
                    this.gui.closeAll();
                    long orderPrice = this.orderPrice();
                    if (orderPrice <= 0L) {
                        return false;
                    } else {
                        this.create(orderPrice);
                        return false;
                    }
                }
            }
        }
    }

    private boolean openYourOrders() {
        return OrdersNav.openYourOrders(this.worker, this.gui, this::say);
    }

    private boolean isYourOrdersNow() {
        return OrdersNav.isYourOrdersNow();
    }

    private static boolean isNewOrderPane(ItemStack stack) {
        return OrdersNav.isNewOrderPane(stack);
    }

    private int collect(int i) {
        String fingerprint = this.gui.fingerprint();
        this.gui.click(i, 0, SlotActionType.PICKUP);
        boolean wait =
                this.worker.waitFor(
                        () ->
                                Gui.containerOpenNow()
                                        && !this.isYourOrdersNow()
                                        && !Gui.fingerprintNow().equals(fingerprint),
                        2000L);
        if (!wait) {
            this.say(ModuleChat.body().warn("Your order did not open."));
            this.gui.closeAll();
            return 0;
        } else {
            this.worker.sleep(1000L);
            int[] ints =
                    this.worker.call(
                            () -> {
                                List list = Gui.containerNow();
                                int j = -1;
                                for (int k = 0; k < list.size(); k++) {
                                    ItemStack stack = (ItemStack) list.get(k);
                                    if (!stack.isEmpty()) {
                                        String name = Gui.nameOf(stack);
                                        if (stack.isOf(Items.RED_TERRACOTTA)
                                                || name.contains("cancel")) {
                                            return new int[] {1, -1};
                                        }
                                        if (j < 0 && name.contains("collect")) {
                                            j = k;
                                        }
                                    }
                                }
                                if (j < 0
                                        && list.size() > 13
                                        && !((ItemStack) list.get(13)).isEmpty()) {
                                    j = 13;
                                }
                                return new int[] {0, j};
                            });
            if (ints[0] == 1) {
                this.say(
                        ModuleChat.body()
                                .text("Your ")
                                .name(this.product.displayName())
                                .text(" order is still filling."));
                this.gui.closeAll();
                return 0;
            } else if (ints[1] < 0) {
                this.say(ModuleChat.body().warn("No collect button in your order."));
                this.gui.closeAll();
                return 0;
            } else {
                String fingerprint2 = this.gui.fingerprint();
                this.gui.click(ints[1], 0, SlotActionType.PICKUP);
                this.worker.sleep(250L);
                this.gui.waitChange(fingerprint2, 2000L);
                this.worker.sleep(150L);
                int m = 0;
                for (int n = 0;
                        n < 6
                                && this.worker.call(Gui::containerOpenNow)
                                && !this.bot.inventoryCapReached();
                        n++) {
                    boolean bl = false;
                    List list2 = this.gui.container();
                    for (int o = 0; o < list2.size(); o++) {
                        ItemStack stack2 = (ItemStack) list2.get(o);
                        if (!stack2.isEmpty() && stack2.isOf(this.product.item())) {
                            if (this.bot.inventoryCapReached()) {
                                break;
                            }
                            m += stack2.getCount();
                            this.gui.click(o, 0, SlotActionType.QUICK_MOVE);
                            bl = true;
                            this.worker.sleep(40L);
                        }
                    }
                    if (!bl) {
                        break;
                    }
                    this.worker.ticks(1);
                }
                this.gui.closeAll();
                if (m > 0) {
                    this.say(
                            ModuleChat.body()
                                    .good("Collected " + m + "x")
                                    .text(" ")
                                    .name(this.product.displayName())
                                    .text(" from your order."));
                }
                return m;
            }
        }
    }

    private long orderPrice() {
        long fixedPrice;
        if (!this.product.beatTopOrder()) {
            fixedPrice = this.product.fixedPrice();
        } else {
            double d = -1.0;
            for (int i = 1; i <= 3 && d <= 0.0; i++) {
                this.gui.closeAll();
                this.worker.sleep(i == 1 ? 800L : 1400L);
                this.gui.command("order " + this.product.search());
                d = this.waitTopOrder(i == 3 ? 7000L : 4500L);
            }
            this.gui.closeAll();
            if (d <= 0.0) {
                this.say(
                        ModuleChat.body()
                                .warn("No order price found")
                                .text(" for ")
                                .name(this.product.describe())
                                .text(
                                        this.product.enchants().isEmpty()
                                                ? ", trying later."
                                                : ". Use a Fixed order price for enchanted"
                                                        + " items."));
                return -1L;
            }
            fixedPrice = (long) Math.floor(d) + this.product.bidIncrement();
            long sellPrice = this.bot.sellPrice();
            if (sellPrice > 0L && fixedPrice >= sellPrice) {
                this.say(
                        ModuleChat.body()
                                .warn("Top order is $" + ServerText.shortMoney(d))
                                .text(", not below your AH price $")
                                .value(ServerText.shortMoney(sellPrice))
                                .text(", so no order this time."));
                return -1L;
            }
            this.say(
                    ModuleChat.body()
                            .text("Top order is $")
                            .value(ServerText.shortMoney(d))
                            .text(", ordering at $")
                            .value(ServerText.shortMoney(fixedPrice))
                            .text(" each."));
        }
        if (this.product.maxPrice() > 0L && fixedPrice > this.product.maxPrice()) {
            this.say(
                    ModuleChat.body()
                            .text("Capped at your max price $")
                            .value(ServerText.shortMoney(this.product.maxPrice()))
                            .text("."));
            fixedPrice = this.product.maxPrice();
        }
        return fixedPrice;
    }

    private double waitTopOrder(long l) {
        long l2 = System.currentTimeMillis() + l;
        while (System.currentTimeMillis() < l2) {
            double d =
                    this.worker.call(
                            () -> {
                                if (!Gui.containerOpenNow()) {
                                    return -1.0;
                                } else {
                                    for (ItemStack stack : Gui.containerNow()) {
                                        if (!stack.isEmpty()
                                                && stack.isOf(this.product.item())
                                                && (this.product.enchants().isEmpty()
                                                        || this.product.matches(stack))) {
                                            return ServerText.orderPrice(stack);
                                        }
                                    }
                                    return -1.0;
                                }
                            });
            if (d > 0.0) {
                return d;
            }
            this.worker.sleep(50L);
        }
        return -1.0;
    }

    private void create(long l) {
        if (this.openYourOrders()) {
            int i = this.worker.call(() -> Gui.findNow(OrderRestock::isNewOrderPane, 45));
            if (i < 0) {
                this.say(
                        ModuleChat.body()
                                .warn("No free order slot")
                                .text(" (no New Order button)."));
                this.gui.closeAll();
            } else {
                this.gui.click(i, 0, SlotActionType.PICKUP);
                Dialogs.View view2 = this.waitDialog(view -> view.titled("item"), 10000L);
                if (view2 == null) {
                    throw fail("Choose Item did not open");
                } else {
                    if (!view2.textInputs().isEmpty()) {
                        Dialogs.Button button2 = view2.labelled("search");
                        view2 =
                                this.press(
                                        view2,
                                        button2,
                                        Map.of(view2.textInputs().get(0), this.product.search()));
                    }
                    Dialogs.Button button3;
                    if ((button3 =
                                    view2.find(
                                            button ->
                                                    this.product.nameMatches(
                                                            button.label(), false)))
                            == null) {
                        button3 = this.closestLoose(view2);
                    }
                    if (button3 == null) {
                        throw fail(
                                this.product.displayName()
                                        + " is not in the search results, "
                                        + view2.describe());
                    } else {
                        view2 = this.press(view2, button3, Map.of());
                        this.checkItem(view2);
                        if (view2.titled("enchant")) {
                            view2 = this.pickEnchants(view2);
                        } else if (!this.product.enchants().isEmpty()) {
                            throw fail(
                                    "No enchantment menu for "
                                            + this.product.displayName()
                                            + ", "
                                            + view2.describe());
                        }
                        if (!view2.titled("how many") && !view2.titled("amount")) {
                            throw fail("Expected the amount menu, got " + view2.describe());
                        } else if (!(view2 =
                                        this.press(
                                                view2,
                                                view2.labelled("next", "continue"),
                                                textFor(
                                                        view2,
                                                        Integer.toString(
                                                                this.product.orderAmount()))))
                                .titled("price")) {
                            throw fail("Expected the price menu, got " + view2.describe());
                        } else if (!(view2 =
                                        this.press(
                                                view2,
                                                view2.labelled("review order", "review", "next"),
                                                textFor(view2, ServerText.exactMoney(l))))
                                .titled("review")) {
                            throw fail("Expected the review menu, got " + view2.describe());
                        } else {
                            this.checkItem(view2);
                            String checkReview = this.checkReview(view2, l);
                            // captured by the lambdas below
                            final Dialogs.View view3 = view2;
                            if (checkReview != null) {
                                Dialogs.Button button4 = view2.labelled("cancel");
                                if (button4 != null) {
                                    this.worker.call(
                                            () ->
                                                    Dialogs.press(
                                                            Gui.mc(), view3, button4, Map.of()));
                                }
                                throw fail(
                                        "Order review did not match ("
                                                + checkReview
                                                + "), cancelled");
                            } else {
                                long l2 = System.currentTimeMillis();
                                Dialogs.Button button5 =
                                        view2.labelled("create order", "create", "confirm");
                                if (button5 == null) {
                                    throw fail("No Create Order button, " + view2.describe());
                                } else if (!this.worker.call(
                                        () -> Dialogs.press(Gui.mc(), view3, button5, Map.of()))) {
                                    throw fail("The review menu closed");
                                } else {
                                    this.worker.waitFor(
                                            () ->
                                                    this.bot.orderedAt >= l2
                                                            || Dialogs.current(Gui.mc()) == null
                                                            || Dialogs.current(Gui.mc()).dialog()
                                                                    != view3.dialog(),
                                            8000L);
                                    this.worker.sleep(300L);
                                    this.gui.closeAll();
                                    this.say(
                                            ModuleChat.body()
                                                    .good(
                                                            "Ordered "
                                                                    + this.product.orderAmount()
                                                                    + "x")
                                                    .text(" ")
                                                    .name(this.product.describe())
                                                    .text(" at $")
                                                    .value(ServerText.shortMoney(l))
                                                    .text(" each."));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private Dialogs.Button closestLoose(Dialogs.View view) {
        for (Dialogs.Button button : view.buttons()) {
            if (this.product.nameMatches(button.key().replaceFirst("^[0-9]+x?", ""), false)) {
                return button;
            }
        }
        return null;
    }

    private void checkItem(Dialogs.View view) {
        if (!view.items().isEmpty()) {
            for (ItemStack item : view.items()) {
                if (item.isOf(this.product.item())) {
                    return;
                }
            }
            String string = view.items().get(0).getName().getString();
            this.worker.run(() -> Dialogs.close(Gui.mc()));
            throw fail(
                    "Picked "
                            + string
                            + " instead of "
                            + this.product.displayName()
                            + ", cancelled");
        }
    }

    private Dialogs.View pickEnchants(Dialogs.View view) {
        if (this.product.enchants().isEmpty()) {
            return this.press(view, view.labelled("skip enchantments", "skip"), Map.of());
        } else {
            for (Product.Enchant enchant : this.product.enchants()) {
                Dialogs.Button button = levelButton(view, enchant);
                if (button == null) {
                    throw fail(enchant.label() + " is not offered, " + view.describe());
                }
                if (!button.green()) {
                    Dialogs.View view3 = view;
                    if (!this.worker.call(() -> Dialogs.press(Gui.mc(), view3, button, Map.of()))) {
                        throw fail("The menu changed before picking " + enchant.label());
                    }
                    Dialogs.View view4 =
                            this.waitDialog(view2 -> view2.dialog() != view3.dialog(), 5000L);
                    if (view4 == null
                            && (view4 = this.worker.call(() -> Dialogs.current(Gui.mc())))
                                    == null) {
                        throw fail("The enchantment menu closed after picking " + enchant.label());
                    }
                    view = view4;
                    Dialogs.Button button2 = levelButton(view4, enchant);
                    if (button2 != null && !button2.green()) {
                        this.say(
                                ModuleChat.body()
                                        .warn("Could not confirm ")
                                        .text(enchant.label())
                                        .text(" was picked, continuing."));
                    }
                }
            }
            return this.press(view, view.labelled("continue", "confirm", "done", "next"), Map.of());
        }
    }

    static Dialogs.Button levelButton(Dialogs.View view, Product.Enchant enchant) {
        String string = "";
        for (Dialogs.Button button : view.buttons()) {
            int roman = ServerText.roman(button.label());
            if (roman > 0) {
                if (string.equals(enchant.key()) && roman == enchant.level()) {
                    return button;
                }
            } else if (!button.label().isBlank()) {
                string = ServerText.key(ServerText.withoutRoman(button.label()));
            }
        }
        return null;
    }

    private String checkReview(Dialogs.View view, long l) {
        for (String string : view.body()) {
            List list;
            if (string.startsWith("amount")
                    && !(list = ServerText.moneyAll(string)).isEmpty()
                    && Math.abs((Double) list.get(0) - this.product.orderAmount())
                            >= ServerText.displayStep(string)) {
                return string;
            }
        }
        for (String string2 : view.body()) {
            List list2;
            if (string2.startsWith("price")
                    && !(list2 = ServerText.moneyAll(string2)).isEmpty()
                    && Math.abs((Double) list2.get(0) - l) > ServerText.displayStep(string2)) {
                return string2;
            }
        }
        return null;
    }

    private static Map<String, String> textFor(Dialogs.View view, String string) {
        return view.textInputs().isEmpty() ? Map.of() : Map.of(view.textInputs().get(0), string);
    }

    private Dialogs.View press(Dialogs.View view, Dialogs.Button button, Map<String, String> map) {
        return this.press(view, button, map, 8000L);
    }

    private Dialogs.View press(
            Dialogs.View view, Dialogs.Button button, Map<String, String> map, long l) {
        if (button == null) {
            throw fail("Button missing, " + view.describe());
        } else if (!this.worker.call(() -> Dialogs.press(Gui.mc(), view, button, map))) {
            throw fail("The menu changed before clicking " + button.label());
        } else {
            Dialogs.View view3 = this.waitDialog(view2 -> view2.dialog() != view.dialog(), l);
            if (view3 == null) {
                throw fail("No answer after clicking '" + button.label() + "'");
            } else {
                return view3;
            }
        }
    }

    private Dialogs.View waitDialog(Predicate<Dialogs.View> predicate, long l) {
        long l2 = System.currentTimeMillis() + l;
        while (System.currentTimeMillis() < l2) {
            Dialogs.View view = this.worker.call(() -> Dialogs.current(Gui.mc()));
            if (view != null && predicate.test(view)) {
                return view;
            }
            this.worker.sleep(50L);
        }
        return null;
    }
}
