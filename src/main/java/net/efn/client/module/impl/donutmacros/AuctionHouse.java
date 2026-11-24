package net.efn.client.module.impl.donutmacros;

import net.efn.client.chat.ModuleChat;
import net.efn.client.module.ClientModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Environment(EnvType.CLIENT)
final class AuctionHouse {

    private static final int INVENTORY_SLOTS = 36;

    private static final int AH_SCAN_SLOTS = 45;

    private static final int AH_ROOT_BUTTON = 51;

    private static final int CHEST_CONFIRM_SLOT = 15;

    private static final int INVENTORY_SETTLE_TICKS = 4;

    private static final int COLLECT_LIMIT = 50;

    private static final int VERIFY_LIMIT = 15;

    private static final long OPEN_TIMEOUT_MS = 10000L;

    private static final long NAV_TIMEOUT_MS = 4000L;

    private static final long SELL_TIMEOUT_MS = 15000L;

    final ClientModule module;

    final MacroWorker worker;

    final Gui gui;

    final AuctionHouse.Config config;

    private final String ign;

    private final String ignKey;

    private volatile long tooManyAt;

    private volatile long alreadyBoughtAt;

    private volatile long listedAt;

    private volatile long errorAt;

    AuctionHouse(
            ClientModule module2, MacroWorker worker2, AuctionHouse.Config config2, String ign2) {
        this.module = module2;
        this.worker = worker2;
        this.gui = new Gui(worker2);
        this.config = config2;
        this.ign = ign2;
        this.ignKey = ServerText.key(ign2);
    }

    void say(ModuleChat.Body body) {
        ModuleChat.send(this.module, body);
    }

    boolean inWorld() {
        return this.worker.call(
                () -> Gui.mc().player != null && Gui.mc().getNetworkHandler() != null);
    }

    void ensurePrice(AuctionHouse.Stock stock) {
        if (stock.fixedPrice) {
            stock.price = stock.product.minPrice();
        } else {
            if (stock.price <= 0L
                    || System.currentTimeMillis() - stock.lastPriceCheck
                            >= this.config.priceCheckMs()) {
                this.updatePrice(stock);
            }
        }
    }

    private void updatePrice(AuctionHouse.Stock stock) {
        Product product2 = stock.product;
        double[] doubles = this.scan(stock);
        stock.lastPriceCheck = System.currentTimeMillis();
        boolean bl = doubles[0] > 0.0;
        boolean bl2 = doubles[1] > 0.0;
        double d = doubles[2];
        long undercut2 = this.config.undercut();
        String displayName = product2.displayName();
        if (d > 0.0) {
            long l = Math.round(d);
            long l2 = Math.max(l - undercut2, product2.minPrice());
            if (stock.price > 0L && l < stock.price) {
                long l3 = stock.price - l;
                double e =
                        undercut2 > 0L ? undercut2 * this.config.ignoreDrops() : stock.price / 2.0;
                if (l3 > e) {
                    this.say(
                            ModuleChat.body()
                                    .name(displayName)
                                    .text(": ")
                                    .warn("ignoring a deep undercut")
                                    .text(" to $")
                                    .value(ServerText.shortMoney(l))
                                    .text(" (drop $")
                                    .value(ServerText.shortMoney(l3))
                                    .text(")."));
                } else if (l2 < stock.price) {
                    this.say(
                            ModuleChat.body()
                                    .name(displayName)
                                    .text(": ")
                                    .warn("undercut")
                                    .text(" to $")
                                    .value(ServerText.shortMoney(l))
                                    .text(", relisting at $")
                                    .value(ServerText.shortMoney(l2))
                                    .text("."));
                    this.relist(stock, l2);
                }
            } else if (stock.price > 0L
                    && l2 > stock.price + undercut2 * this.config.relistGap()
                    && stock.inAh > 0) {
                this.say(
                        ModuleChat.body()
                                .name(displayName)
                                .text(": price went up to $")
                                .value(ServerText.shortMoney(l))
                                .text(", relisting at $")
                                .value(ServerText.shortMoney(l2))
                                .text("."));
                this.relist(stock, l2);
            } else {
                if (stock.price <= 0L) {
                    this.say(
                            ModuleChat.body()
                                    .name(displayName)
                                    .text(": cheapest is $")
                                    .value(ServerText.shortMoney(l))
                                    .text(", selling at $")
                                    .value(ServerText.shortMoney(l2))
                                    .text(" each."));
                }
                stock.price = l2;
            }
        } else if (stock.price <= 0L && bl && !bl2) {
            stock.price = product2.minPrice() * 2L;
            this.say(
                    ModuleChat.body()
                            .name(displayName)
                            .text(": no other listings, selling at $")
                            .value(ServerText.shortMoney(stock.price))
                            .text(" each."));
        } else if (stock.price <= 0L) {
            this.say(
                    ModuleChat.body()
                            .name(displayName)
                            .text(": ")
                            .warn("could not read AH prices")
                            .text(", waiting."));
        }
    }

    private double[] scan(AuctionHouse.Stock stock) {
        Product product2 = stock.product;
        this.gui.closeAll();
        this.worker.sleep(200L);
        this.gui.command("ah " + product2.search());
        boolean wait =
                this.worker.waitFor(() -> this.isAhResultsNow() && this.hasContent(), 10000L);
        if (!wait) {
            this.gui.closeAll();
            return new double[] {0.0, 0.0, -1.0};
        } else {
            this.worker.sleep(600L);
            if (!this.worker.call(this::isAhResultsNow)) {
                this.gui.closeAll();
                return new double[] {0.0, 0.0, -1.0};
            } else {
                double[] doubles =
                        this.worker.call(
                                () -> {
                                    List list = Gui.containerNow();
                                    boolean bl = false;
                                    double d = -1.0;
                                    for (int i = 0; i < list.size() && i < 45; i++) {
                                        ItemStack stack = (ItemStack) list.get(i);
                                        if (product2.matches(stack)) {
                                            bl = true;
                                            double listingPrice = ServerText.listingPrice(stack);
                                            double e;
                                            if (!(listingPrice <= 0.0)
                                                    && !this.ownListing(
                                                            stock,
                                                            stack,
                                                            e =
                                                                    listingPrice
                                                                            / Math.max(
                                                                                    1,
                                                                                    stack
                                                                                            .getCount()))
                                                    && (d < 0.0 || e < d)) {
                                                d = e;
                                            }
                                        }
                                    }
                                    return new double[] {1.0, bl ? 1.0 : 0.0, d};
                                });
                this.gui.closeAll();
                this.worker.sleep(200L);
                return doubles;
            }
        }
    }

    private boolean ownListing(AuctionHouse.Stock stock, ItemStack stack, double d) {
        for (String string : ServerText.lore(stack)) {
            if (!this.ignKey.isEmpty() && ServerText.key(string).contains(this.ignKey)) {
                return true;
            }
        }
        return stock.inAh > 0 && stock.price > 0L && Math.abs(d - stock.price) < 1.0;
    }

    private boolean hasContent() {
        List list = Gui.containerNow();
        if (list.size() > 49 && !((ItemStack) list.get(49)).isEmpty()) {
            return true;
        } else {
            for (int i = 0; i <= 10 && i < list.size(); i++) {
                if (!((ItemStack) list.get(i)).isEmpty()) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean isAhResultsNow() {
        if (!Gui.containerOpenNow()) {
            return false;
        } else {
            String string = ServerText.normalize(Gui.mc().currentScreen.getTitle());
            if (string.contains("order")) {
                return false;
            } else {
                return !string.contains("auction")
                                && !string.contains("market")
                                && !string.equals("ah")
                                && !string.contains(" ah ")
                        ? isChest(Gui.slotNow(51))
                        : true;
            }
        }
    }

    private boolean isAhRootNow() {
        if (!Gui.containerOpenNow()) {
            return false;
        } else {
            return ServerText.normalize(Gui.mc().currentScreen.getTitle()).contains("order")
                    ? false
                    : isChest(Gui.slotNow(51));
        }
    }

    private static boolean isChest(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        } else {
            String path = Registries.ITEM.getId(stack.getItem()).getPath();
            return path.equals("chest") || path.equals("trapped_chest");
        }
    }

    private boolean openYourItems() {
        this.gui.closeAll();
        this.worker.sleep(200L);
        this.gui.command("ah " + this.ign);
        if (!this.worker.waitFor(this::isAhRootNow, 10000L)) {
            return false;
        } else {
            for (int i = 0; i < 2; i++) {
                String fingerprint = this.gui.fingerprint();
                this.gui.click(51, 0, SlotActionType.PICKUP);
                if (this.gui.waitChange(fingerprint, 4000L)) {
                    this.worker.sleep(200L);
                    return this.worker.call(Gui::containerOpenNow);
                }
            }
            return false;
        }
    }

    boolean refreshListings(List<AuctionHouse.Stock> list) {
        if (!this.openYourItems()) {
            this.gui.closeAll();
            return false;
        } else {
            int[] ints =
                    this.worker.call(
                            () -> {
                                int[] ints2 = new int[list.size()];
                                for (ItemStack stack : Gui.containerNow()) {
                                    for (int i = 0; i < list.size(); i++) {
                                        if (((AuctionHouse.Stock) list.get(i))
                                                .product.matches(stack)) {
                                            ints2[i]++;
                                            break;
                                        }
                                    }
                                }
                                return ints2;
                            });
            this.gui.closeAll();
            this.worker.sleep(200L);
            for (int j = 0; j < list.size(); j++) {
                AuctionHouse.Stock stock = (AuctionHouse.Stock) list.get(j);
                int k = stock.inAh - ints[j];
                if (k > 0 && stock.chatSales.getAndSet(0) == 0) {
                    stock.sold.addAndGet(k);
                    stock.earned.addAndGet(
                            k * Math.max(0L, stock.price) * this.config.perListing());
                    this.say(
                            ModuleChat.body()
                                    .good("Sold " + k + "x")
                                    .text(" ")
                                    .name(stock.product.displayName())
                                    .text(" (total ")
                                    .value(stock.sold.get())
                                    .text(")."));
                }
                stock.chatSales.set(0);
                stock.inAh = ints[j];
            }
            return true;
        }
    }

    private int collect(AuctionHouse.Stock stock) {
        if (!this.openYourItems()) {
            this.say(ModuleChat.body().warn("Could not open Your Items."));
            this.gui.closeAll();
            return 0;
        } else {
            int pass = this.collectPass(stock.product, 50);
            if (pass > 0) {
                this.gui.closeAll();
                this.worker.sleep(500L);
                if (this.openYourItems()) {
                    pass += this.collectPass(stock.product, 15);
                }
            }
            this.gui.closeAll();
            this.worker.sleep(200L);
            if (pass > 0) {
                this.say(
                        ModuleChat.body()
                                .text("Took back ")
                                .value(pass)
                                .text(pass == 1 ? " listing of " : " listings of ")
                                .name(stock.product.displayName())
                                .text("."));
            }
            return pass;
        }
    }

    private int collectPass(Product product2, int i) {
        int j = 0;
        HashSet set = new HashSet();
        int k;
        while (j < i
                && this.worker.call(Gui::containerOpenNow)
                && (k =
                                this.worker.call(
                                        () -> {
                                            List list = Gui.containerNow();
                                            for (int m = 0; m < list.size(); m++) {
                                                if (!set.contains(m)
                                                        && product2.matches(
                                                                (ItemStack) list.get(m))) {
                                                    return m;
                                                }
                                            }
                                            return -1;
                                        }))
                        >= 0) {
            long l = System.currentTimeMillis();
            this.gui.click(k, 0, SlotActionType.PICKUP);
            this.worker.ticks(3);
            if (this.alreadyBoughtAt >= l) {
                set.add(k);
            } else {
                j++;
            }
        }
        return j;
    }

    private void relist(AuctionHouse.Stock stock, long l) {
        this.refreshListings(List.of(stock));
        stock.price = l;
        this.collect(stock);
        stock.inAh = 0;
    }

    int listSome(AuctionHouse.Stock stock, int i, int[] ints) {
        if (i <= 0) {
            return 0;
        } else {
            this.gui.closeAll();
            this.gui.closeInventory();
            int j = this.worker.call(() -> Gui.player().getInventory().getSelectedSlot());
            int k = 0;
            int m = 0;
            try {
                for (int n = 0; n < i; n++) {
                    this.ensureAlive();
                    int stage = this.stage(stock.product);
                    if (stage < 0) {
                        this.say(
                                ModuleChat.body()
                                        .warn("Could not prepare a hotbar slot")
                                        .text(" (inventory full?)."));
                        break;
                    }
                    AuctionHouse.Listing listing = this.listFrom(stock, stage);
                    if (listing == AuctionHouse.Listing.FULL) {
                        ints[0] = Math.max(ints[0], this.config.maxListings());
                        this.say(ModuleChat.body().warn("The AH says you have too many listings."));
                        break;
                    }
                    if (listing == AuctionHouse.Listing.FAILED) {
                        if (++m >= 2) {
                            break;
                        }
                    } else {
                        k++;
                        stock.inAh++;
                        ints[0]++;
                        stock.listed.incrementAndGet();
                        if (this.config.listingDelayMs() > 0L && n < i - 1) {
                            this.worker.sleep(this.config.listingDelayMs());
                        }
                    }
                }
            } finally {
                this.worker.run(
                        () -> {
                            ClientPlayerEntity player = Gui.mc().player;
                            if (player != null) {
                                player.getInventory().setSelectedSlot(j);
                            }
                        });
            }
            if (k > 0) {
                this.say(
                        ModuleChat.body()
                                .text("Listed ")
                                .value(k + "x")
                                .text(" ")
                                .name(stock.product.displayName())
                                .text(" at $")
                                .value(
                                        ServerText.shortMoney(
                                                stock.price * this.config.perListing()))
                                .text(" (in AH: ")
                                .value(ints[0])
                                .text(")."));
            }
            return k;
        }
    }

    private AuctionHouse.Listing listFrom(AuctionHouse.Stock stock, int selectedSlot) {
        Product product2 = stock.product;
        this.worker.run(() -> Gui.player().getInventory().setSelectedSlot(selectedSlot));
        this.worker.ticks(2);
        int i =
                this.worker.call(
                        () -> Gui.player().getInventory().getStack(selectedSlot).getCount());
        long l = System.currentTimeMillis();
        this.gui.command(
                "ah sell " + ServerText.exactMoney(stock.price * this.config.perListing()));
        long l2 = l + 15000L;
        boolean[] booleans = new boolean[] {false};
        Object[] objects = new Object[] {null};
        int[] ints = new int[] {-1};
        while (System.currentTimeMillis() < l2) {
            if (this.tooManyAt >= l) {
                this.gui.closeAll();
                return AuctionHouse.Listing.FULL;
            }
            if (this.listedAt >= l) {
                this.worker.sleep(150L);
                this.gui.closeAll();
                return AuctionHouse.Listing.LISTED;
            }
            if (this.errorAt >= l) {
                this.gui.closeAll();
                return AuctionHouse.Listing.FAILED;
            }
            long l3 = System.currentTimeMillis() - l;
            int j =
                    this.worker.call(
                            () -> {
                                Dialogs.View view = Dialogs.current(Gui.mc());
                                if (view != null) {
                                    Dialogs.Button button;
                                    if (view.dialog() != objects[0]
                                            && (view.titled("sure")
                                                    || view.titled("confirm")
                                                    || view.titled("sell"))
                                            && !view.titled("buy")
                                            && !view.titled("purchase")
                                            && (button = view.labelled("yes", "confirm")) != null
                                            && Dialogs.press(Gui.mc(), view, button, Map.of())) {
                                        objects[0] = view.dialog();
                                        booleans[0] = true;
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else if (Gui.containerOpenNow()) {
                                    ClientPlayerEntity player = Gui.player();
                                    int syncId = player.currentScreenHandler.syncId;
                                    ItemStack stack = Gui.slotNow(15);
                                    String string =
                                            ServerText.normalize(Gui.mc().currentScreen.getTitle());
                                    boolean bl =
                                            string.contains("buy")
                                                    || string.contains("purchase")
                                                    || string.contains("(page")
                                                    || string.contains("order");
                                    if (syncId != ints[0]
                                            && !bl
                                            && !stack.isEmpty()
                                            && Registries.ITEM
                                                    .getId(stack.getItem())
                                                    .getPath()
                                                    .contains("lime")) {
                                        Gui.mc()
                                                .interactionManager
                                                .clickSlot(
                                                        syncId,
                                                        15,
                                                        0,
                                                        SlotActionType.PICKUP,
                                                        player);
                                        ints[0] = syncId;
                                        booleans[0] = true;
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    ItemStack stack2 =
                                            Gui.player().getInventory().getStack(selectedSlot);
                                    boolean bl2 =
                                            !product2.matches(stack2) || stack2.getCount() < i;
                                    return !bl2 || !booleans[0] && l3 <= 2000L ? 0 : 2;
                                }
                            });
            if (j == 2) {
                this.gui.closeAll();
                return AuctionHouse.Listing.LISTED;
            }
            this.worker.sleep(j == 1 ? 250L : 50L);
        }
        this.gui.closeAll();
        this.say(ModuleChat.body().warn("Listing timed out").text("."));
        return AuctionHouse.Listing.FAILED;
    }

    private int stage(Product product2) {
        int perListing2 = this.config.perListing();
        boolean bl = false;
        try {
            for (int j = 0; j < 12; j++) {
                int[] ints = this.worker.call(() -> planStage(product2, perListing2));
                if (ints[0] == 0) {
                    return ints[1];
                }
                if (ints[0] < 0) {
                    return -1;
                }
                for (int k = 2; k < ints.length; k += 2) {
                    this.gui.clickInventory(
                            ints[k] >> 1,
                            ints[k] & 1,
                            ints[k + 1] == 1 ? SlotActionType.QUICK_MOVE : SlotActionType.PICKUP);
                    bl = true;
                    this.worker.ticks(1);
                }
            }
            return -1;
        } finally {
            if (bl) {
                this.gui.closeInventory();
                this.worker.ticks(4);
            }
        }
    }

    private static int[] planStage(Product product2, int i) {
        ClientPlayerEntity player = Gui.player();
        PlayerInventory inventory = player.getInventory();
        ItemStack cursorStack = player.playerScreenHandler.getCursorStack();
        if (!cursorStack.isEmpty()) {
            int emptyMain = emptyMain(inventory);
            int[] ints;
            if (emptyMain < 0) {
                int[] ints2 = new int[1];
                ints = ints2;
                ints2[0] = -1;
            } else {
                int[] ints3 = new int[] {1, 0, click(menuSlot(emptyMain), 0), 0};
                ints = ints3;
                ints3[3] = 0;
            }
            return ints;
        } else {
            for (int k = 0; k < 9; k++) {
                ItemStack stack = inventory.getStack(k);
                if (product2.matches(stack) && stack.getCount() == i) {
                    return new int[] {0, k};
                }
            }
            int j = -1;
            for (int n = 0; n < 9; n++) {
                ItemStack stack2 = inventory.getStack(n);
                if (product2.matches(stack2) && stack2.getCount() < i) {
                    j = n;
                    break;
                }
            }
            if (j < 0) {
                for (int o = 0; o < 9; o++) {
                    if (inventory.getStack(o).isEmpty()) {
                        j = o;
                        break;
                    }
                }
            }
            if (j < 0) {
                int m = -1;
                for (int q = 8; q >= 0; q--) {
                    if (!product2.matches(inventory.getStack(q))) {
                        m = q;
                        break;
                    }
                }
                if (m < 0) {
                    m = 8;
                }
                return emptyMain(inventory) < 0
                        ? new int[] {-1}
                        : new int[] {1, 0, click(menuSlot(m), 0), 1};
            } else {
                int p =
                        product2.matches(inventory.getStack(j))
                                ? inventory.getStack(j).getCount()
                                : 0;
                int r = -1;
                int t = 0;
                for (int v = 0; v < 36; v++) {
                    ItemStack stack3;
                    if (v != j
                            && product2.matches(stack3 = inventory.getStack(v))
                            && stack3.getCount() > t) {
                        t = stack3.getCount();
                        r = v;
                    }
                }
                if (r < 0) {
                    return new int[] {-1};
                } else {
                    int u = i - p;
                    int[] ints4 = new int[2 + 2 * (u + 2)];
                    int w = 2;
                    ints4[0] = 1;
                    ints4[w++] = click(menuSlot(r), 0);
                    ints4[w++] = 0;
                    if (t <= u) {
                        ints4[w++] = click(menuSlot(j), 0);
                        ints4[w++] = 0;
                    } else {
                        for (int j2 = 0; j2 < u; j2++) {
                            ints4[w++] = click(menuSlot(j), 1);
                            ints4[w++] = 0;
                        }
                        ints4[w++] = click(menuSlot(r), 0);
                        ints4[w++] = 0;
                    }
                    return Arrays.copyOf(ints4, w);
                }
            }
        }
    }

    private static int click(int i, int j) {
        return i << 1 | j;
    }

    private static int menuSlot(int i) {
        return i < 9 ? 36 + i : i;
    }

    private static int emptyMain(PlayerInventory playerInventory) {
        for (int i = 9; i < 36; i++) {
            if (playerInventory.getStack(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    int countInventory(Product product2) {
        return this.worker.call(
                () -> {
                    ClientPlayerEntity player = Gui.mc().player;
                    if (player == null) {
                        return 0;
                    } else {
                        int i = 0;
                        for (int j = 0; j < 36; j++) {
                            ItemStack stack = player.getInventory().getStack(j);
                            if (product2.matches(stack)) {
                                i += stack.getCount();
                            }
                        }
                        return i;
                    }
                });
    }

    boolean inventoryCapReached(Product product2) {
        return this.worker.call(
                () -> {
                    PlayerInventory inventory = Gui.player().getInventory();
                    int i = 0;
                    int j = 0;
                    for (int k = 0; k < 36; k++) {
                        ItemStack stack = inventory.getStack(k);
                        if (stack.isEmpty()) {
                            j++;
                        } else if (stack.isOf(product2.item())) {
                            i++;
                        }
                    }
                    return i >= this.config.collectSlots() || j <= 0;
                });
    }

    void ensureAlive() {
        if (this.worker.call(() -> Gui.mc().player != null && Gui.mc().player.isDead())) {
            this.say(ModuleChat.body().danger("You died").text(", respawning."));
            long l = System.currentTimeMillis() + 30000L;
            for (long l2 = System.currentTimeMillis() + 700L;
                    System.currentTimeMillis() < l
                            && this.worker.call(
                                    () -> Gui.mc().player != null && Gui.mc().player.isDead());
                    this.worker.sleep(100L)) {
                if (System.currentTimeMillis() >= l2) {
                    this.worker.run(
                            () -> {
                                ClientPlayerEntity player = Gui.mc().player;
                                if (player != null) {
                                    player.requestRespawn();
                                }
                            });
                    l2 = System.currentTimeMillis() + 1500L;
                }
            }
            this.worker.run(
                    () -> {
                        if (Gui.mc().currentScreen instanceof DeathScreen) {
                            Gui.mc().setScreen(null);
                        }
                    });
            throw new AuctionHouse.Problem("Respawned");
        }
    }

    void waitSalesCheck() {
        long l = System.currentTimeMillis() + this.config.salesCheckMs();
        while (System.currentTimeMillis() < l) {
            this.worker.sleep(Math.min(1000L, l - System.currentTimeMillis()));
        }
    }

    String onChat(String string, List<AuctionHouse.Stock> list) {
        long l = System.currentTimeMillis();
        if (string.contains("too many listed items")) {
            this.tooManyAt = l;
        }
        if (string.contains("already bought")) {
            this.alreadyBoughtAt = l;
        }
        if (string.startsWith("you listed")) {
            this.listedAt = l;
        }
        if (string.contains("you cannot do this")
                || string.contains("you need to wait another")
                || string.contains("don't have enough")) {
            this.errorAt = l;
        }
        int i;
        if ((i = string.indexOf(" bought your ")) <= 0) {
            return null;
        } else {
            String string2 = string.substring(i + " bought your ".length());
            int j = string2.lastIndexOf(" for ");
            String string3 = j > 0 ? string2.substring(0, j) : string2;
            AuctionHouse.Stock stock = null;
            for (AuctionHouse.Stock stock2 : list) {
                if (stock2.product.nameMatches(string3, false)) {
                    stock = stock2;
                    break;
                }
            }
            if (stock == null) {
                for (AuctionHouse.Stock stock3 : list) {
                    if (stock3.product.nameMatches(string3, true)) {
                        stock = stock3;
                        break;
                    }
                }
            }
            if (stock == null) {
                return null;
            } else {
                List list2 = j > 0 ? ServerText.moneyAll(string2.substring(j)) : List.of();
                long l2 = list2.isEmpty() ? 0L : Math.round((Double) list2.get(0));
                stock.sold.incrementAndGet();
                stock.chatSales.incrementAndGet();
                stock.earned.addAndGet(l2);
                this.say(
                        ModuleChat.body()
                                .good("Sold")
                                .text(" ")
                                .name(stock.product.displayName())
                                .text(l2 > 0L ? " for $" + ServerText.shortMoney(l2) : "")
                                .text(" (total ")
                                .value(stock.sold.get())
                                .text(", ~$")
                                .value(ServerText.shortMoney(stock.earned.get()))
                                .text(")."));
                return stock.product.displayName();
            }
        }
    }

    static String summary(List<AuctionHouse.Stock> list) {
        int i = 0;
        int j = 0;
        long l = 0L;
        for (AuctionHouse.Stock stock : list) {
            i += stock.listed.get();
            j += stock.sold.get();
            l += stock.earned.get();
        }
        return "Listed " + i + ", sold " + j + ", earned ~$" + ServerText.shortMoney(l) + ".";
    }

    @Environment(EnvType.CLIENT)
    record Config(
            long undercut,
            long priceCheckMs,
            long salesCheckMs,
            int perListing,
            int maxListings,
            long listingDelayMs,
            double relistGap,
            double ignoreDrops,
            int collectSlots,
            long orderCheckMs) {}

    @Environment(EnvType.CLIENT)
    private static enum Listing {
        LISTED,
        FULL,
        FAILED;

        private Listing() {}
    }

    @Environment(EnvType.CLIENT)
    static final class Problem extends RuntimeException {

        Problem(String string) {
            super(string, null, false, false);
        }
    }

    @Environment(EnvType.CLIENT)
    static final class Stock {

        final Product product;

        final boolean fixedPrice;

        long price = -1L;

        long lastPriceCheck;

        int inAh;

        final AtomicInteger listed = new AtomicInteger();

        final AtomicInteger sold = new AtomicInteger();

        final AtomicInteger chatSales = new AtomicInteger();

        final AtomicLong earned = new AtomicLong();

        Stock(Product product2, boolean fixedPrice2) {
            this.product = product2;
            this.fixedPrice = fixedPrice2;
            if (fixedPrice2) {
                this.price = product2.minPrice();
            }
        }
    }
}
