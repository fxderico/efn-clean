package net.efn.client.module.impl.donutmacros;

import net.efn.client.chat.ModuleChat;
import net.efn.client.module.ClientModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
final class SniperBot {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final Pattern RATE_LIMIT =
            Pattern.compile("^you need to wait another ([\\d.]+) seconds?");

    private static final double MAX_WAIT_SECONDS = 30.0;

    private static final Pattern BOUGHT_COUNT = Pattern.compile("^(\\d{1,4})\\s*x?\\s+");

    private static final Pattern TIME = Pattern.compile("\\d+\\s*[dhms]\\b");

    private static final int LISTING_SLOTS = 45;

    private static final int DEFAULT_FILTER = 47;

    private static final int DEFAULT_REFRESH = 49;

    private static final long OPEN_TIMEOUT_MS = 10000L;

    private static final long CONTENT_TIMEOUT_MS = 3000L;

    private static final long SORT_TIMEOUT_MS = 3000L;

    private static final long SETTLE_MS = 150L;

    private static final long SETTLE_LIMIT_MS = 1500L;

    private static final int SETTLE_TICKS = 2;

    private static final long ANSWER_DEFAULT_MS = 1000L;

    private static final long ANSWER_MIN_MS = 700L;

    private static final long ANSWER_MAX_MS = 3000L;

    private static final long ANSWER_SLACK_MS = 300L;

    private static final double CONFIRM_PRICE_SLACK = 1.02;

    private static final long BUY_TIMEOUT_MS = 4000L;

    private static final long SCREEN_GRACE_MS = 400L;

    private static final long REOPEN_WAIT_MS = 1000L;

    private static final long LATE_PURCHASE_MS = 15000L;

    private static final long FAILED_SKIP_MS = 60000L;

    private static final long WRONG_SKIP_MS = 600000L;

    private static final long BOUGHT_SKIP_MS = 15000L;

    private static final long RECOVERY_MS = 2000L;

    private static final long POLL_MS = 2L;

    private static final int TIMER_SLACK_MINUTES = 2;

    private static final int STRIKES_TO_SKIP = 2;

    private static final int UNREADABLE_TO_STOP = 4;

    private static final int MAX_PAGES = 5;

    private static final long RESEARCH_MIN_MS = 2500L;

    private static final long MISS_NOTE_MS = 30000L;

    private static final SniperBot.Hit CLOSED =
            new SniperBot.Hit(-1, ItemStack.EMPTY, 0L, 0L, -1, "", "");

    private final ClientModule module;

    private final Consumer<SniperBot> stopper;

    private final MacroWorker worker;

    private final Gui gui;

    private final SnipeTarget target;

    private final long refreshMs;

    private final long settleMs;

    private final long budget;

    private final boolean requireConfirm;

    private final int maxBuys;

    private final int wantItems;

    private final SniperBot.Sort sort;

    private final AuctionHouse ah;

    private final AuctionHouse.Stock stock;

    private final List<AuctionHouse.Stock> stocks;

    private final Map<String, Long> skip = new HashMap<>();

    private final Map<String, Integer> strikes = new HashMap<>();

    private volatile SniperBot.Purchase purchase;

    private volatile long goneAt;

    private volatile long poorAt;

    private volatile long ownAt;

    private volatile long holdUntil;

    private long handledAt;

    private SniperBot.Hit lastHit;

    private long lastClickAt;

    private long pageKey = -9223372036854775808L;

    private long pageChangedAt;

    private int pageChangedTick;

    private boolean refreshPending;

    private long refreshSentAt;

    private long answerMs;

    private long nextRefresh;

    private volatile boolean confirmPressed;

    private boolean instantWarned;

    private int unreadable;

    private int bought;

    private int unsold;

    private long spent;

    private int wrong;

    private long wrongSpent;

    private int boughtItems;

    private volatile boolean complete;

    private int pagesTurned;

    private int searches;

    private volatile String note = "";

    private volatile long noteAt;

    private long lastMissNote;

    private boolean sortWarned;

    SniperBot(
            ClientModule module2,
            Consumer<SniperBot> consumer,
            MacroWorker macroWorker,
            SnipeTarget snipeTarget,
            long l,
            long l2,
            long l3,
            boolean bl,
            int i,
            long l4,
            String string) {
        this(module2, consumer, macroWorker, snipeTarget, l, l2, l3, bl, i, 0, l4, string);
    }

    static SniperBot quota(
            ClientModule module2,
            Consumer<SniperBot> consumer,
            MacroWorker macroWorker,
            SnipeTarget snipeTarget,
            int i,
            long l,
            long l2) {
        return new SniperBot(
                module2,
                consumer,
                macroWorker,
                snipeTarget,
                l,
                200L,
                l2,
                false,
                0,
                Math.max(1, i),
                0L,
                "");
    }

    private SniperBot(
            ClientModule module2,
            Consumer<SniperBot> stopper2,
            MacroWorker worker2,
            SnipeTarget target2,
            long refreshMs2,
            long settleMs2,
            long budget2,
            boolean requireConfirm2,
            int maxBuys2,
            int wantItems2,
            long l,
            String string) {
        this.module = module2;
        this.stopper = stopper2;
        this.worker = worker2;
        this.gui = new Gui(worker2);
        this.target = target2;
        this.refreshMs = refreshMs2;
        this.settleMs = settleMs2;
        this.budget = budget2;
        this.requireConfirm = requireConfirm2;
        this.maxBuys = maxBuys2;
        this.wantItems = wantItems2;
        SniperBot.Sort sort2 =
                this.sort = wantItems2 > 0 ? SniperBot.Sort.LOWEST : SniperBot.Sort.RECENT;
        if (l > 0L) {
            this.ah =
                    new AuctionHouse(
                            module2,
                            worker2,
                            new AuctionHouse.Config(
                                    0L, 60000L, 20000L, 1, 1000, 0L, 3.0, 20.0, 36, 15000L),
                            string);
            this.stock = new AuctionHouse.Stock(target2.product(l), true);
            this.stocks = List.of(this.stock);
        } else {
            this.ah = null;
            this.stock = null;
            this.stocks = List.of();
        }
    }

    private void say(ModuleChat.Body body) {
        ModuleChat.send(this.module, body);
    }

    boolean complete() {
        return this.complete;
    }

    int boughtItems() {
        return this.boughtItems;
    }

    long spent() {
        return this.spent + this.wrongSpent;
    }

    void run() {
        if (this.wantItems > 0) {
            this.say(
                    ModuleChat.body()
                            .text("Buying ")
                            .value(this.wantItems)
                            .text("x ")
                            .name(this.target.describe())
                            .text(", at most $")
                            .value(ServerText.shortMoney(this.target.maxPrice))
                            .text(" per listing")
                            .text(
                                    this.budget > 0L
                                            ? ", spending at most $"
                                                    + ServerText.shortMoney(this.budget)
                                            : "")
                            .text("."));
        } else {
            this.say(
                    ModuleChat.body()
                            .text("Sniping ")
                            .name(this.target.describe())
                            .text(" under $")
                            .value(ServerText.shortMoney(this.target.maxPrice))
                            .text(
                                    this.target.minMinutes > 0
                                            ? " with "
                                                    + formatMinutes(this.target.minMinutes)
                                                    + "+ left"
                                            : "")
                            .text(
                                    this.stock != null
                                            ? ", selling at $"
                                                    + ServerText.shortMoney(this.stock.price)
                                            : "")
                            .text(
                                    this.budget > 0L
                                            ? ", spending at most $"
                                                    + ServerText.shortMoney(this.budget)
                                            : "")
                            .text("."));
        }
        while (true) {
            this.worker.check();
            try {
                if (!this.worker.call(
                        () -> Gui.mc().player != null && Gui.mc().getNetworkHandler() != null)) {
                    this.worker.sleep(2000L);
                } else {
                    if (this.stock != null && this.unsold > 0) {
                        this.sellStock();
                    }
                    this.snipe();
                }
            } catch (MacroWorker.Cancelled exception) {
                throw exception;
            } catch (RuntimeException exception2) {
                String string =
                        exception2.getMessage() == null
                                ? exception2.getClass().getSimpleName()
                                : exception2.getMessage();
                this.say(ModuleChat.body().warn(string).text(", trying again."));
                this.worker.check();
                this.gui.closeAll();
                this.worker.sleep(2000L);
            }
        }
    }

    private void snipe() {
        this.openAuction();
        this.ensureSort();
        this.pageKey = -9223372036854775808L;
        this.refreshPending = false;
        this.pagesTurned = 0;
        this.nextRefresh =
                System.currentTimeMillis()
                        + (this.wantItems > 0 ? Math.max(this.refreshMs, 2500L) : this.refreshMs);
        while (true) {
            this.worker.check();
            SniperBot.Purchase purchase2 = this.purchase;
            if (purchase2 != null && purchase2.at() > this.handledAt) {
                if (this.lastHit != null && purchase2.at() - this.lastClickAt <= 15000L) {
                    if (this.bought(this.lastHit, purchase2)) {
                        return;
                    }
                    continue;
                }
                this.handledAt = purchase2.at();
            }
            if (this.wantItems > 0 && this.worker.call(() -> freeSlots(Gui.player()) == 0)) {
                this.finish(
                        ModuleChat.body()
                                .warn("Inventory full")
                                .text(" after buying ")
                                .value(this.boughtItems)
                                .text("x ")
                                .name(this.target.describe())
                                .text("."));
            }
            long l = System.currentTimeMillis();
            SniperBot.Hit hit = this.worker.call(this::pageTick);
            if (hit == CLOSED) {
                return;
            }
            if (hit != null) {
                this.lastHit = hit;
                this.lastClickAt = l;
                if (this.handleBuy(hit, l)) {
                    return;
                }
                this.pageKey = -9223372036854775808L;
                this.nextRefresh = System.currentTimeMillis() + this.refreshMs;
            } else {
                this.worker.sleep(2L);
            }
        }
    }

    private void openAuction() {
        this.gui.closeAll();
        long l = this.holdUntil - System.currentTimeMillis();
        if (l > 0L) {
            this.worker.sleep(l);
        }
        long l2 = System.currentTimeMillis();
        this.gui.command("ah " + this.target.search);
        if (!this.worker.waitFor(SniperBot::auctionOpenNow, 10000L)) {
            String string =
                    this.worker.call(
                            () ->
                                    Gui.mc().currentScreen == null
                                            ? "no screen"
                                            : Gui.mc().currentScreen.getClass().getSimpleName()
                                                    + " '"
                                                    + ServerText.normalize(
                                                            Gui.mc().currentScreen.getTitle())
                                                    + "'");
            String string2 = this.noteAt >= l2 ? this.note : "";
            LOGGER.info(
                    "[EFN] AH did not open for '/ah {}': {}, server said '{}'",
                    this.target.search,
                    string,
                    string2);
            throw new AuctionHouse.Problem(
                    "The AH did not open"
                            + (string2.isEmpty() ? "" : " (server: " + string2 + ")"));
        } else {
            this.worker.waitFor(
                    () -> {
                        if (!auctionOpenNow()) {
                            return true;
                        } else {
                            List list = Gui.containerNow();
                            return refreshSlot(list) >= 0 || filterSlot(list) >= 0;
                        }
                    },
                    3000L);
            this.settle();
        }
    }

    private void settle() {
        long l = System.currentTimeMillis() + 1500L;
        int i = this.worker.call(SniperBot::stateNow);
        long l2 = System.currentTimeMillis();
        while (System.currentTimeMillis() < l) {
            this.worker.sleep(25L);
            int j = this.worker.call(SniperBot::stateNow);
            long l3 = System.currentTimeMillis();
            if (j != i) {
                i = j;
                l2 = l3;
            } else if (l3 - l2 >= 150L) {
                return;
            }
        }
    }

    private static int stateNow() {
        ClientPlayerEntity player = Gui.mc().player;
        return player == null ? -1 : player.currentScreenHandler.getRevision();
    }

    private static boolean auctionOpenNow() {
        if (!Gui.containerOpenNow()) {
            return false;
        } else {
            String string = ServerText.normalize(Gui.mc().currentScreen.getTitle());
            return string.contains("order")
                    ? false
                    : string.contains("auction") || refreshSlot(Gui.containerNow()) >= 0;
        }
    }

    private void ensureSort() {
        for (int i = 0; i < 5; i++) {
            int[] ints =
                    this.worker.call(
                            () -> {
                                List list = Gui.containerNow();
                                int filterSlot = filterSlot(list);
                                return new int[] {
                                    filterSlot,
                                    filterSlot < 0
                                            ? SniperBot.Sort.UNKNOWN.ordinal()
                                            : selectedSort((ItemStack) list.get(filterSlot))
                                                    .ordinal()
                                };
                            });
            if (ints[0] < 0) {
                if (!this.sortWarned) {
                    this.say(
                            ModuleChat.body()
                                    .warn("No Filter button found")
                                    .text(", sniping with the current sorting."));
                }
                this.sortWarned = true;
                return;
            }
            SniperBot.Sort sort2 = SniperBot.Sort.values()[ints[1]];
            if (sort2 == this.sort) {
                return;
            }
            if (sort2 == SniperBot.Sort.UNKNOWN && i > 0) {
                if (!this.sortWarned) {
                    this.say(
                            ModuleChat.body()
                                    .warn("Could not read the Filter")
                                    .text(
                                            ", make sure it is on "
                                                    + (this.sort == SniperBot.Sort.LOWEST
                                                            ? "Lowest Price."
                                                            : "Recently Listed.")));
                }
                this.sortWarned = true;
                return;
            }
            String fingerprint2 = this.gui.fingerprint();
            this.gui.click(ints[0], 0, SlotActionType.PICKUP);
            if (sort2 == SniperBot.Sort.UNKNOWN) {
                this.gui.waitChange(fingerprint2, 3000L);
            } else {
                this.worker.waitFor(
                        () -> {
                            SniperBot.Sort sort3 = sortNow();
                            return sort3 != SniperBot.Sort.UNKNOWN && sort3 != sort2;
                        },
                        3000L);
            }
            this.settle();
        }
    }

    private static SniperBot.Sort sortNow() {
        ClientPlayerEntity player = Gui.mc().player;
        if (player != null
                && auctionOpenNow()
                && player.currentScreenHandler.getCursorStack().isEmpty()) {
            List list = Gui.containerNow();
            int filterSlot = filterSlot(list);
            return filterSlot < 0
                    ? SniperBot.Sort.UNKNOWN
                    : selectedSort((ItemStack) list.get(filterSlot));
        } else {
            return SniperBot.Sort.UNKNOWN;
        }
    }

    private static int filterSlot(List<ItemStack> list) {
        for (int i = 45; i < list.size(); i++) {
            ItemStack stack2 = (ItemStack) list.get(i);
            if (!stack2.isEmpty()
                    && (stack2.isOf(Items.HOPPER) || Gui.nameOf(stack2).contains("filter"))) {
                return i;
            }
        }
        return list.size() > 47 && !((ItemStack) list.get(47)).isEmpty() ? 47 : -1;
    }

    private static int refreshSlot(List<ItemStack> list) {
        for (int i = 45; i < list.size(); i++) {
            ItemStack stack2 = (ItemStack) list.get(i);
            if (!stack2.isEmpty() && stack2.isOf(Items.ANVIL)) {
                return i;
            }
        }
        return list.size() > 49 && ((ItemStack) list.get(49)).isOf(Items.ANVIL) ? 49 : -1;
    }

    static SniperBot.Sort selectedSort(ItemStack stack2) {
        ArrayList list = new ArrayList();
        ArrayList list2 = new ArrayList();
        for (Text line :
                stack2.getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT).lines()) {
            String string = ServerText.normalize(Dialogs.visibleText(line));
            SniperBot.Sort sort2 =
                    string.contains("recent")
                            ? SniperBot.Sort.RECENT
                            : (string.contains("lowest")
                                    ? SniperBot.Sort.LOWEST
                                    : (string.contains("highest")
                                            ? SniperBot.Sort.HIGHEST
                                            : SniperBot.Sort.UNKNOWN));
            if (sort2 != SniperBot.Sort.UNKNOWN) {
                list.add(sort2);
                list2.add(color(line));
            }
        }
        if (list.size() < 2) {
            return SniperBot.Sort.UNKNOWN;
        } else {
            for (int i = 0; i < list.size(); i++) {
                boolean bl = true;
                boolean bl2 = true;
                int j = -2;
                for (int k = 0; k < list.size(); k++) {
                    if (k != i) {
                        if (((Integer) list2.get(k)).equals(list2.get(i))) {
                            bl = false;
                        }
                        if (j == -2) {
                            j = (Integer) list2.get(k);
                        } else if (j != (Integer) list2.get(k)) {
                            bl2 = false;
                        }
                    }
                }
                if (bl && bl2) {
                    return (SniperBot.Sort) list.get(i);
                }
            }
            int m = -1;
            SniperBot.Sort uNKNOWN = SniperBot.Sort.UNKNOWN;
            for (int n = 0; n < list.size(); n++) {
                int o = (Integer) list2.get(n);
                int p = (o >> 16 & 0xFF) + (o >> 8 & 0xFF) + (o & 0xFF);
                if (p > m) {
                    m = p;
                    uNKNOWN = (SniperBot.Sort) list.get(n);
                } else if (p == m) {
                    uNKNOWN = SniperBot.Sort.UNKNOWN;
                }
            }
            return uNKNOWN;
        }
    }

    private static int color(Text text) {
        int[] ints = new int[] {-1};
        text.visit(
                (style, value) -> {
                    if (ints[0] < 0
                            && !value.isBlank()
                            && !value.trim().equals("\u2022")
                            && !value.trim().equals("\u25aa")) {
                        TextColor color = style.getColor();
                        ints[0] = color == null ? 11141290 : color.getRgb();
                        return Optional.empty();
                    } else {
                        return Optional.empty();
                    }
                },
                Style.EMPTY);
        return ints[0];
    }

    private SniperBot.Hit pageTick() {
        ClientPlayerEntity player = Gui.mc().player;
        if (player != null && auctionOpenNow()) {
            long l = System.currentTimeMillis();
            long l2 =
                    (long) player.currentScreenHandler.syncId << 32
                            | player.currentScreenHandler.getRevision() & 4294967295L;
            if (l2 != this.pageKey) {
                this.pageKey = l2;
                this.pageChangedAt = l;
                this.pageChangedTick = player.age;
                if (this.refreshPending) {
                    this.refreshPending = false;
                    this.learnAnswer(l - this.refreshSentAt);
                }
                return null;
            } else {
                if (this.refreshPending) {
                    if (l - this.refreshSentAt < this.answerTimeout(player)) {
                        return null;
                    }
                    this.refreshPending = false;
                }
                if (l - this.pageChangedAt < this.settleMs
                        || player.age - this.pageChangedTick < 2) {
                    return null;
                } else if (!player.currentScreenHandler.getCursorStack().isEmpty()) {
                    return null;
                } else {
                    String string = ServerText.normalize(Gui.mc().currentScreen.getTitle());
                    if (this.wantItems <= 0
                            && string.contains("(page")
                            && !string.contains("(page 1)")) {
                        return null;
                    } else {
                        this.skip.values().removeIf(long_ -> long_ < l);
                        List list = Gui.containerNow();
                        int i = -1;
                        double d = 1.7976931348623157E308;
                        for (int j = 0; j < 45 && j < list.size(); j++) {
                            ItemStack stack2 = (ItemStack) list.get(j);
                            double askingPrice;
                            if (!stack2.isEmpty()
                                    && this.target.matches(stack2)
                                    && !((askingPrice = ServerText.askingPrice(stack2)) <= 0.0)) {
                                long l3 = Math.round(askingPrice / Math.max(1, stack2.getCount()));
                                String fingerprint2;
                                if ((this.wantItems > 0 ? Math.round(askingPrice) : l3)
                                                <= this.target.maxPrice
                                        && (this.budget <= 0L
                                                || this.spent
                                                                + this.wrongSpent
                                                                + Math.round(askingPrice)
                                                        <= this.budget)
                                        && !this.skip.containsKey(
                                                fingerprint2 = fingerprint(stack2, askingPrice))) {
                                    if (this.wantItems <= 0) {
                                        return this.click(
                                                player, j, stack2, askingPrice, l3, fingerprint2);
                                    }
                                    double e = askingPrice / Math.max(1, stack2.getCount());
                                    if (room(player, stack2) >= stack2.getCount() && e < d) {
                                        i = j;
                                        d = e;
                                    }
                                }
                            }
                        }
                        if (i >= 0) {
                            ItemStack stack3 = (ItemStack) list.get(i);
                            double askingPrice2 = ServerText.askingPrice(stack3);
                            return this.click(
                                    player,
                                    i,
                                    stack3,
                                    askingPrice2,
                                    Math.round(askingPrice2 / Math.max(1, stack3.getCount())),
                                    fingerprint(stack3, askingPrice2));
                        } else if (this.wantItems > 0) {
                            int nextPageSlot = nextPageSlot(list);
                            if (nextPageSlot >= 0 && this.pagesTurned < 5) {
                                this.pagesTurned++;
                                Gui.mc()
                                        .interactionManager
                                        .clickSlot(
                                                player.currentScreenHandler.syncId,
                                                nextPageSlot,
                                                0,
                                                SlotActionType.PICKUP,
                                                player);
                                this.refreshPending = true;
                                this.refreshSentAt = l;
                                return null;
                            } else if (l < this.nextRefresh) {
                                return null;
                            } else {
                                this.noteMiss(string, list, l);
                                return CLOSED;
                            }
                        } else {
                            if (l >= this.nextRefresh) {
                                int refreshSlot = refreshSlot(list);
                                if (refreshSlot >= 0) {
                                    Gui.mc()
                                            .interactionManager
                                            .clickSlot(
                                                    player.currentScreenHandler.syncId,
                                                    refreshSlot,
                                                    0,
                                                    SlotActionType.PICKUP,
                                                    player);
                                    this.refreshPending = true;
                                    this.refreshSentAt = l;
                                }
                                this.nextRefresh = l + this.refreshMs;
                            }
                            return null;
                        }
                    }
                }
            }
        } else {
            return CLOSED;
        }
    }

    private static int nextPageSlot(List<ItemStack> list) {
        for (int i = 45; i < list.size(); i++) {
            ItemStack stack2 = (ItemStack) list.get(i);
            String name2;
            if (!stack2.isEmpty()
                    && !(name2 = Gui.nameOf(stack2)).contains("prev")
                    && !name2.contains("back")
                    && (name2.contains("next")
                            || stack2.isOf(Items.ARROW) && name2.contains("page"))) {
                return i;
            }
        }
        return -1;
    }

    private void noteMiss(String string, List<ItemStack> list, long lastMissNote2) {
        this.searches++;
        if (lastMissNote2 - this.lastMissNote >= 30000L) {
            this.lastMissNote = lastMissNote2;
            int i = 0;
            for (int j = 0; j < 45 && j < list.size(); j++) {
                if (!((ItemStack) list.get(j)).isEmpty()) {
                    i++;
                }
            }
            this.say(
                    ModuleChat.body()
                            .text("No ")
                            .name(this.target.describe())
                            .text(" for $")
                            .value(ServerText.shortMoney(this.target.maxPrice))
                            .text(" or less on ")
                            .value("/ah " + this.target.search)
                            .text(" yet (" + i + " other listings), searching again."));
            LOGGER.info(
                    "[EFN] AH purchase found no {} on '{}' after {} searches:{}",
                    this.target.describe(),
                    string,
                    this.searches,
                    describe(list));
        }
    }

    private SniperBot.Hit click(
            ClientPlayerEntity player, int i, ItemStack stack2, double d, long l, String string) {
        this.confirmPressed = false;
        Gui.mc()
                .interactionManager
                .clickSlot(player.currentScreenHandler.syncId, i, 0, SlotActionType.PICKUP, player);
        return new SniperBot.Hit(
                i,
                stack2,
                Math.round(d),
                l,
                this.target.timed() ? SnipeTarget.minutesLeft(stack2) : -1,
                string,
                stack2.getName().getString());
    }

    private static int freeSlots(ClientPlayerEntity player) {
        int i = 0;
        for (int j = 0; j < 36; j++) {
            if (player.getInventory().getStack(j).isEmpty()) {
                i++;
            }
        }
        return i;
    }

    private static int room(ClientPlayerEntity player, ItemStack stack2) {
        int i = 0;
        for (int j = 0; j < 36; j++) {
            ItemStack stack3 = player.getInventory().getStack(j);
            if (stack3.isEmpty()) {
                i += stack2.getMaxCount();
            } else if (ItemStack.areItemsAndComponentsEqual(stack3, stack2)) {
                i += Math.max(0, stack3.getMaxCount() - stack3.getCount());
            }
        }
        return i;
    }

    private void learnAnswer(long l) {
        if (l > 0L && l <= 3000L) {
            this.answerMs = this.answerMs <= 0L ? l : (this.answerMs * 3L + l) / 4L;
        }
    }

    private long answerTimeout(ClientPlayerEntity player) {
        long l = this.answerMs > 0L ? this.answerMs * 3L : 1000L;
        PlayerListEntry listEntry =
                Gui.mc().getNetworkHandler() == null
                        ? null
                        : Gui.mc().getNetworkHandler().getPlayerListEntry(player.getUuid());
        if (listEntry != null) {
            l = Math.max(l, listEntry.getLatency() * 2L);
        }
        return Math.clamp(l + 300L, 700L, 3000L);
    }

    private static String fingerprint(ItemStack stack2, double d) {
        StringBuilder sb = new StringBuilder();
        sb.append(Registries.ITEM.getId(stack2.getItem()))
                .append('|')
                .append(stack2.getCount())
                .append('|')
                .append(ServerText.key(stack2.getName().getString()))
                .append('|')
                .append(Math.round(d))
                .append('|')
                .append(stack2.getDamage())
                .append('|')
                .append(EnchantmentHelper.getEnchantments(stack2));
        for (String string : ServerText.lore(stack2)) {
            String string2 = ServerText.normalize(string);
            if (!TIME.matcher(string2).find()) {
                sb.append('|').append(string2);
            }
        }
        return sb.toString();
    }

    private boolean handleBuy(SniperBot.Hit hit, long l) {
        Object[] objects = new Object[] {null};
        int[] ints = new int[] {-1};
        long[] longs = new long[] {0L, -9223372036854775808L};
        boolean[] booleans = new boolean[] {false};
        SniperBot.Problem[] problems = new SniperBot.Problem[] {null};
        long l2 = System.currentTimeMillis() + 4000L;
        while (System.currentTimeMillis() < l2) {
            SniperBot.Purchase purchase2 = this.purchase;
            if (purchase2 != null && purchase2.at() >= l && purchase2.at() > this.handledAt) {
                return this.bought(hit, purchase2);
            }
            if (this.poorAt >= l) {
                this.say(
                        ModuleChat.body()
                                .danger("Not enough money")
                                .text(" for ")
                                .name(hit.name())
                                .text(" at $")
                                .value(ServerText.shortMoney(hit.total()))
                                .text(", stopping."));
                this.stopper.accept(this);
                throw new MacroWorker.Cancelled();
            }
            if (this.goneAt >= l || this.ownAt >= l) {
                this.skip.put(hit.fingerprint(), System.currentTimeMillis() + 60000L);
                return false;
            }
            SniperBot.Step step =
                    this.worker.call(
                            () -> this.confirmStep(hit, objects, ints, longs, booleans, problems));
            if (step == SniperBot.Step.WRONG) {
                if (this.strikes.merge(hit.fingerprint(), 1, Integer::sum) >= 2) {
                    this.skip.put(hit.fingerprint(), System.currentTimeMillis() + 600000L);
                }
                this.say(
                        ModuleChat.body()
                                .warn("Did not buy")
                                .text(" ")
                                .name(hit.name())
                                .text(": " + problems[0].reason() + "."));
                if (problems[0].fatal()) {
                    this.unreadable = 0;
                } else if (++this.unreadable >= 4) {
                    this.say(
                            ModuleChat.body()
                                    .danger("The purchase menu could not be checked")
                                    .text(
                                            " "
                                                    + this.unreadable
                                                    + " times in a row, so AH Sniper stopped"
                                                    + " without buying. Send a screenshot of the"
                                                    + " menu to the EFN team."));
                    this.stopper.accept(this);
                    throw new MacroWorker.Cancelled();
                }
                return !this.worker.waitFor(SniperBot::auctionOpenNow, 1000L);
            }
            if (step == SniperBot.Step.PRESSED) {
                l2 = Math.max(l2, System.currentTimeMillis() + 4000L);
            }
            this.worker.sleep(2L);
        }
        this.skip.put(hit.fingerprint(), System.currentTimeMillis() + 60000L);
        return !this.worker.call(SniperBot::auctionOpenNow);
    }

    private SniperBot.Step confirmStep(
            SniperBot.Hit hit,
            Object[] objects,
            int[] ints,
            long[] longs,
            boolean[] booleans,
            SniperBot.Problem[] problems) {
        ClientPlayerEntity player = Gui.mc().player;
        if (player == null) {
            return SniperBot.Step.WAIT;
        } else {
            Dialogs.View view = Dialogs.current(Gui.mc());
            if (view != null) {
                if (view.dialog() == objects[0]) {
                    return SniperBot.Step.WAIT;
                } else if (!view.titled("buy")
                        && !view.titled("sure")
                        && !view.titled("purchase")
                        && !view.titled("confirm")) {
                    return SniperBot.Step.WAIT;
                } else {
                    Dialogs.Button button = view.labelled("yes", "confirm", "buy");
                    if (button == null) {
                        return SniperBot.Step.WAIT;
                    } else {
                        SniperBot.Problem problem = this.dialogProblem(view, hit, booleans[0]);
                        if (problem != null) {
                            LOGGER.info(
                                    "[EFN] AH Sniper cancelled a purchase dialog ({}): {} body={}"
                                            + " items={}",
                                    problem.reason(),
                                    view.describe(),
                                    view.body(),
                                    describe(view.items()));
                            Dialogs.close(Gui.mc());
                            problems[0] = problem;
                            return SniperBot.Step.WRONG;
                        } else if (!Dialogs.press(Gui.mc(), view, button, Map.of())) {
                            return SniperBot.Step.WAIT;
                        } else {
                            objects[0] = view.dialog();
                            booleans[0] = true;
                            this.confirmPressed = true;
                            return SniperBot.Step.PRESSED;
                        }
                    }
                }
            } else if (Gui.containerOpenNow() && !auctionOpenNow()) {
                int syncId = player.currentScreenHandler.syncId;
                if (syncId == ints[0]) {
                    return SniperBot.Step.WAIT;
                } else {
                    List list = Gui.containerNow();
                    int confirmSlot = confirmSlot(list, hit);
                    if (confirmSlot < 0) {
                        return SniperBot.Step.WAIT;
                    } else {
                        long l = System.currentTimeMillis();
                        if (longs[1] != syncId) {
                            longs[0] = l;
                            longs[1] = syncId;
                        }
                        SniperBot.Problem problem2;
                        if ((problem2 = this.chestProblem(list, confirmSlot, hit, booleans[0]))
                                != null) {
                            if (!problem2.fatal() && l - longs[0] < 400L) {
                                return SniperBot.Step.WAIT;
                            } else {
                                LOGGER.info(
                                        "[EFN] AH Sniper cancelled a purchase menu ({}): '{}' {}",
                                        problem2.reason(),
                                        ServerText.normalize(Gui.mc().currentScreen.getTitle()),
                                        describe(list));
                                player.closeHandledScreen();
                                problems[0] = problem2;
                                return SniperBot.Step.WRONG;
                            }
                        } else {
                            Gui.mc()
                                    .interactionManager
                                    .clickSlot(
                                            syncId, confirmSlot, 0, SlotActionType.PICKUP, player);
                            ints[0] = syncId;
                            booleans[0] = true;
                            this.confirmPressed = true;
                            return SniperBot.Step.PRESSED;
                        }
                    }
                }
            } else {
                return SniperBot.Step.WAIT;
            }
        }
    }

    private static int confirmSlot(List<ItemStack> list, SniperBot.Hit hit) {
        int i = -1;
        for (int j = 0; j < list.size(); j++) {
            ItemStack stack2 = (ItemStack) list.get(j);
            if (!stack2.isEmpty()
                    && !stack2.isOf(hit.stack().getItem())
                    && Registries.ITEM.getId(stack2.getItem()).getPath().contains("lime")) {
                String name2 = Gui.nameOf(stack2);
                if (name2.contains("confirm")
                        || name2.contains("buy")
                        || name2.contains("purchase")
                        || name2.contains("yes")) {
                    return j;
                }
                if (i < 0) {
                    i = j;
                }
            }
        }
        return i;
    }

    private SniperBot.Problem chestProblem(
            List<ItemStack> list, int i, SniperBot.Hit hit, boolean bl) {
        boolean bl2 = false;
        double d = -1.0;
        for (int j = 0; j < list.size(); j++) {
            ItemStack stack2 = (ItemStack) list.get(j);
            if (!stack2.isEmpty()) {
                double askingPrice = ServerText.askingPrice(textOf(stack2));
                if (askingPrice > d) {
                    d = askingPrice;
                }
                if (j != i) {
                    if (this.sameListing(stack2, hit)) {
                        bl2 = true;
                    } else if (!decoration(stack2) && askingPrice > 0.0) {
                        return new SniperBot.Problem(
                                "the menu was for " + stack2.getName().getString(), true);
                    }
                }
            }
        }
        return this.verdict(bl2, d, hit, bl);
    }

    private SniperBot.Problem dialogProblem(Dialogs.View view, SniperBot.Hit hit, boolean bl) {
        ArrayList list = new ArrayList();
        list.add(view.title());
        list.addAll(view.body());
        for (Dialogs.Button button : view.buttons()) {
            list.add(button.label());
        }
        double askingPrice = ServerText.askingPrice(list);
        boolean bl2 = false;
        for (ItemStack item : view.items()) {
            if (!item.isEmpty()) {
                double askingPrice2 = ServerText.askingPrice(textOf(item));
                if (askingPrice2 > askingPrice) {
                    askingPrice = askingPrice2;
                }
                if (this.sameListing(item, hit)) {
                    bl2 = true;
                } else if (!decoration(item)) {
                    return new SniperBot.Problem(
                            "the menu was for " + item.getName().getString(), true);
                }
            }
        }
        if (!bl2 && view.items().isEmpty()) {
            String key = ServerText.key(hit.name());
            bl2 = !key.isEmpty() && ServerText.key(String.join(" ", list)).contains(key);
        }
        return this.verdict(bl2, askingPrice, hit, bl);
    }

    private SniperBot.Problem verdict(boolean bl, double d, SniperBot.Hit hit, boolean bl2) {
        if (d > this.limit(hit)) {
            return new SniperBot.Problem("the menu asked $" + ServerText.shortMoney(d), true);
        } else if (bl2) {
            return null;
        } else if (!bl) {
            return new SniperBot.Problem("the menu did not show this listing", false);
        } else {
            return d <= 0.0
                    ? new SniperBot.Problem("the menu did not show the price", false)
                    : null;
        }
    }

    private double limit(SniperBot.Hit hit) {
        double d =
                this.wantItems > 0
                        ? this.target.maxPrice
                        : (double) this.target.maxPrice * Math.max(1, hit.stack().getCount());
        return Math.min(d, hit.total() * 1.02 + 1.0);
    }

    private boolean sameListing(ItemStack stack2, SniperBot.Hit hit) {
        ItemStack stack3 = hit.stack();
        if (stack2.isOf(stack3.getItem())
                && stack2.getCount() == stack3.getCount()
                && stack2.getDamage() == stack3.getDamage()) {
            String key = ServerText.key(stack3.getName().getString());
            if (!ServerText.key(stack2.getName().getString()).contains(key)) {
                return false;
            } else if (!EnchantmentHelper.getEnchantments(stack2)
                    .equals(EnchantmentHelper.getEnchantments(stack3))) {
                return false;
            } else if (!this.target.sameKind(stack2)) {
                return false;
            } else if (hit.minutes() < 0) {
                return true;
            } else {
                int minutesLeft = SnipeTarget.minutesLeft(stack2);
                return minutesLeft >= 0 && Math.abs(minutesLeft - hit.minutes()) <= 2;
            }
        } else {
            return false;
        }
    }

    private static boolean decoration(ItemStack stack2) {
        String path = Registries.ITEM.getId(stack2.getItem()).getPath();
        if (!path.contains("glass_pane")
                && !path.endsWith("stained_glass")
                && !path.equals("glass")
                && !path.equals("barrier")
                && !path.equals("structure_void")
                && !path.equals("light")
                && !path.endsWith("_head")
                && !path.endsWith("_skull")) {
            boolean bl =
                    path.startsWith("lime_")
                            || path.startsWith("green_")
                            || path.startsWith("red_");
            if (!bl
                    || !path.endsWith("_dye")
                            && !path.endsWith("_wool")
                            && !path.endsWith("_concrete")
                            && !path.endsWith("_terracotta")
                            && !path.endsWith("_carpet")) {
                String name2 = Gui.nameOf(stack2);
                return name2.contains("confirm")
                        || name2.contains("cancel")
                        || name2.contains("go back")
                        || name2.contains("close");
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    private static List<String> textOf(ItemStack stack2) {
        ArrayList list = new ArrayList();
        list.add(stack2.getName().getString());
        list.addAll(ServerText.lore(stack2));
        return list;
    }

    private static String describe(List<ItemStack> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size() && sb.length() < 2000; i++) {
            ItemStack stack2 = (ItemStack) list.get(i);
            if (!stack2.isEmpty()) {
                sb.append(" [")
                        .append(i)
                        .append(' ')
                        .append(Registries.ITEM.getId(stack2.getItem()).getPath())
                        .append(" x")
                        .append(stack2.getCount())
                        .append(" '")
                        .append(stack2.getName().getString())
                        .append('\'');
                for (String string : ServerText.lore(stack2)) {
                    sb.append(" / ").append(string);
                }
                sb.append(']');
            }
        }
        return sb.toString();
    }

    private boolean bought(SniperBot.Hit hit, SniperBot.Purchase purchase2) {
        this.handledAt = purchase2.at();
        int i = Math.max(1, purchase2.count());
        long l = purchase2.total() > 0L ? purchase2.total() : hit.total();
        boolean bl = purchase2.name().isEmpty() || namesMatch(purchase2.name(), hit.stack());
        boolean bl2 =
                this.wantItems > 0 ? l <= this.target.maxPrice : l <= this.target.maxPrice * i;
        this.skip.put(hit.fingerprint(), System.currentTimeMillis() + 15000L);
        if (bl && bl2) {
            this.bought++;
            this.boughtItems += i;
            this.spent += l;
            this.unreadable = 0;
            this.unsold++;
            this.say(
                    ModuleChat.body()
                            .good(this.wantItems > 0 ? "Bought" : "Sniped")
                            .text(" ")
                            .name(hit.name())
                            .text(" for $")
                            .value(ServerText.shortMoney(l))
                            .text(" (")
                            .value(this.bought)
                            .text(this.bought == 1 ? " buy" : " buys")
                            .text(", $")
                            .value(ServerText.shortMoney(this.spent))
                            .text(" spent)."));
            if (!this.confirmPressed) {
                if (this.requireConfirm) {
                    this.say(
                            ModuleChat.body()
                                    .danger("Your AH buy confirm menu is off")
                                    .text(
                                            ", so that click bought the listing right away. AH"
                                                + " Sniper stopped because Only Buy With Confirm is"
                                                + " on. Turn the buy confirm menu on in"
                                                + " /settings."));
                    this.stopper.accept(this);
                    throw new MacroWorker.Cancelled();
                }
                if (!this.instantWarned) {
                    this.instantWarned = true;
                    this.say(
                            ModuleChat.body()
                                    .warn("Your AH buy confirm menu is off")
                                    .text(
                                            ", so a click buys right away. AH Sniper only clicks"
                                                    + " once the page has been still for "
                                                    + this.settleMs
                                                    + "ms. Turn the confirm menu on in /settings to"
                                                    + " have every purchase checked before you"
                                                    + " pay."));
                }
            }
            if (this.wantItems > 0 && this.boughtItems >= this.wantItems) {
                this.finish(
                        ModuleChat.body()
                                .text("Got ")
                                .value(this.boughtItems)
                                .text("x ")
                                .name(this.target.describe())
                                .text(" for $")
                                .value(ServerText.shortMoney(this.spent))
                                .text("."));
            }
            if (this.budget > 0L && this.spent + this.wrongSpent >= this.budget) {
                if (this.stock != null) {
                    this.worker.sleep(300L);
                    this.sellStock();
                }
                this.say(
                        ModuleChat.body()
                                .text("Spent $")
                                .value(ServerText.shortMoney(this.spent + this.wrongSpent))
                                .text(" of your $")
                                .value(ServerText.shortMoney(this.budget))
                                .text(" limit, stopping."));
                this.stopper.accept(this);
                throw new MacroWorker.Cancelled();
            } else if (this.maxBuys > 0 && this.bought >= this.maxBuys) {
                if (this.stock != null) {
                    this.worker.sleep(300L);
                    this.sellStock();
                }
                this.say(
                        ModuleChat.body()
                                .text("Reached ")
                                .value(this.maxBuys)
                                .text(this.maxBuys == 1 ? " buy" : " buys")
                                .text(", stopping."));
                this.stopper.accept(this);
                throw new MacroWorker.Cancelled();
            } else if (this.stock != null) {
                this.worker.sleep(300L);
                this.sellStock();
                return true;
            } else if (this.wantItems <= 0) {
                return !this.worker.call(SniperBot::auctionOpenNow);
            } else {
                this.gui.closeAll();
                return true;
            }
        } else {
            this.wrong++;
            this.wrongSpent += l;
            LOGGER.warn(
                    "[EFN] AH Sniper bought {}x '{}' for {} while sniping '{}' under {}",
                    i,
                    purchase2.name(),
                    l,
                    hit.name(),
                    this.target.maxPrice);
            this.say(
                    ModuleChat.body()
                            .danger("Bought the wrong listing")
                            .text(": " + i + "x ")
                            .name(purchase2.name().isEmpty() ? hit.name() : purchase2.name())
                            .text(" for $")
                            .value(ServerText.shortMoney(l))
                            .text(". AH Sniper stopped to keep your money safe."));
            this.stopper.accept(this);
            throw new MacroWorker.Cancelled();
        }
    }

    private void finish(ModuleChat.Body body) {
        this.complete = true;
        this.say(body);
        this.stopper.accept(this);
        throw new MacroWorker.Cancelled();
    }

    private static boolean namesMatch(String string, ItemStack stack2) {
        String key = ServerText.key(string);
        if (key.isEmpty()) {
            return true;
        } else {
            String key2 = ServerText.key(stack2.getName().getString());
            String key3 = ServerText.key(new ItemStack(stack2.getItem()).getName().getString());
            String key4 = ServerText.key(Registries.ITEM.getId(stack2.getItem()).getPath());
            return !key2.isEmpty() && (key.contains(key2) || key2.contains(key))
                    || !key3.isEmpty() && key.contains(key3)
                    || !key4.isEmpty() && key.contains(key4);
        }
    }

    private void sellStock() {
        int i = Math.min(this.unsold, this.ah.countInventory(this.stock.product));
        if (i > 0) {
            this.unsold = this.unsold - this.ah.listSome(this.stock, Math.min(i, 9), new int[] {0});
        }
    }

    void onNote(String string) {
        String string2 = ServerText.normalize(string);
        if (!string2.isEmpty()) {
            this.note = string2.length() > 120 ? string2.substring(0, 120) : string2;
            this.noteAt = System.currentTimeMillis();
        }
    }

    void onChat(String string) {
        String string2 = ServerText.normalize(string);
        if (!string2.isEmpty()) {
            long l = System.currentTimeMillis();
            this.onNote(string);
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
                } catch (NumberFormatException exception) {
                }
            }
            if (string2.startsWith("you bought")) {
                this.purchase = parsePurchase(string2, l);
            }
            if (string2.contains("already bought")
                    || string2.contains("no longer available")
                    || string2.contains("not available")) {
                this.goneAt = l;
            }
            if (string2.contains("enough money")) {
                this.poorAt = l;
            }
            if (string2.contains("your own")) {
                this.ownAt = l;
            }
            if (this.ah != null) {
                this.ah.onChat(string2, this.stocks);
            }
        }
    }

    private static SniperBot.Purchase parsePurchase(String string, long l) {
        String string2 = string.substring("you bought".length()).trim();
        int i = 1;
        Matcher matcher = BOUGHT_COUNT.matcher(string2);
        if (matcher.find()) {
            i = Math.max(1, Integer.parseInt(matcher.group(1)));
            string2 = string2.substring(matcher.end());
        }
        int j;
        String string3 =
                ((j = string2.lastIndexOf(" for ")) >= 0 ? string2.substring(0, j) : string2)
                        .trim();
        long l2 = -1L;
        List list;
        if (j >= 0 && !(list = ServerText.moneyAll(string2.substring(j + 5))).isEmpty()) {
            l2 = Math.round((Double) list.get(0));
        }
        return new SniperBot.Purchase(l, i, string3, l2);
    }

    String summary() {
        return "Sniped "
                + this.bought
                + (this.bought == 1 ? " item" : " items")
                + " for $"
                + ServerText.shortMoney(this.spent)
                + (this.wrong > 0
                        ? ", plus "
                                + this.wrong
                                + " wrong "
                                + (this.wrong == 1 ? "listing" : "listings")
                                + " for $"
                                + ServerText.shortMoney(this.wrongSpent)
                        : "")
                + (this.stock != null ? ". " + AuctionHouse.summary(this.stocks) : ".");
    }

    private static String formatMinutes(int i) {
        return i % 60 == 0 ? i / 60 + "h" : i / 60 + "h " + i % 60 + "m";
    }

    @Environment(EnvType.CLIENT)
    private record Hit(
            int slot,
            ItemStack stack,
            long total,
            long unit,
            int minutes,
            String fingerprint,
            String name) {}

    @Environment(EnvType.CLIENT)
    private record Problem(String reason, boolean fatal) {}

    @Environment(EnvType.CLIENT)
    private record Purchase(long at, int count, String name, long total) {}

    @Environment(EnvType.CLIENT)
    static enum Sort {
        UNKNOWN,
        LOWEST,
        HIGHEST,
        RECENT;

        private Sort() {}
    }

    @Environment(EnvType.CLIENT)
    private static enum Step {
        WAIT,
        PRESSED,
        WRONG;

        private Step() {}
    }
}
