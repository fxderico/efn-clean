package net.efn.client.module.impl.donutmacros;

import net.efn.client.chat.ModuleChat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
final class AhSellBot {

    private static final int MAX_LISTINGS_PER_PASS = 9;

    private static final long RECOVERY_MS = 3000L;

    private final AuctionHouse ah;

    private final MacroWorker worker;

    private final AuctionHouse.Config config;

    private final List<AuctionHouse.Stock> stocks;

    private final int[] inAh = new int[] {0};

    private boolean warnedFull;

    private boolean warnedEmpty;

    AhSellBot(
            AhSellModule module,
            MacroWorker worker2,
            List<Product> list,
            boolean bl,
            AuctionHouse.Config config2,
            String string) {
        this.ah = new AuctionHouse(module, worker2, config2, string);
        this.worker = worker2;
        this.config = config2;
        ArrayList list2 = new ArrayList();
        for (Product product : list) {
            list2.add(new AuctionHouse.Stock(product, bl));
        }
        this.stocks = List.copyOf(list2);
    }

    void run() {
        ArrayList list = new ArrayList();
        for (AuctionHouse.Stock stock : this.stocks) {
            list.add(
                    stock.product.displayName()
                            + (stock.fixedPrice
                                    ? " ($" + ServerText.shortMoney(stock.price) + ")"
                                    : ""));
        }
        this.ah.say(ModuleChat.body().text("Selling ").name(String.join(", ", list)).text("."));
        try {
            this.ah.gui.closeAll();
            if (this.ah.refreshListings(this.stocks)) {
                this.inAh[0] = this.total();
            }
        } catch (MacroWorker.Cancelled exception) {
            throw exception;
        } catch (RuntimeException exception2) {
            this.ah.gui.closeAll();
        }
        while (true) {
            this.worker.check();
            try {
                this.cycle();
            } catch (MacroWorker.Cancelled exception3) {
                throw exception3;
            } catch (RuntimeException exception4) {
                String string =
                        exception4.getMessage() == null
                                ? exception4.getClass().getSimpleName()
                                : exception4.getMessage();
                this.ah.say(ModuleChat.body().warn(string).text(", trying again."));
                this.worker.check();
                this.ah.gui.closeAll();
                this.worker.sleep(3000L);
            }
        }
    }

    private void cycle() {
        if (!this.ah.inWorld()) {
            this.worker.sleep(2000L);
        } else {
            this.ah.ensureAlive();
            boolean bl = false;
            boolean bl2 = false;
            for (AuctionHouse.Stock stock : this.stocks) {
                this.worker.check();
                int countInventory = this.ah.countInventory(stock.product);
                if (countInventory >= this.config.perListing() || stock.inAh != 0) {
                    this.ah.ensurePrice(stock);
                    if (stock.price > 0L && countInventory >= this.config.perListing()) {
                        bl2 = true;
                        this.inAh[0] = this.total();
                        int i = this.config.maxListings() - this.inAh[0];
                        if (i <= 0) {
                            if (!this.warnedFull) {
                                this.ah.say(
                                        ModuleChat.body()
                                                .text("AH full (")
                                                .value(
                                                        this.inAh[0]
                                                                + "/"
                                                                + this.config.maxListings())
                                                .text("), waiting for sales."));
                            }
                            this.warnedFull = true;
                            break;
                        }
                        this.warnedFull = false;
                        int j = Math.min(Math.min(countInventory / this.config.perListing(), i), 9);
                        if (this.ah.listSome(stock, j, this.inAh) > 0) {
                            bl = true;
                        }
                    }
                }
            }
            if (!bl2 && this.total() == 0) {
                if (!this.warnedEmpty) {
                    this.ah.say(
                            ModuleChat.body()
                                    .text("Nothing to sell right now, waiting for items."));
                }
                this.warnedEmpty = true;
            } else {
                this.warnedEmpty = false;
            }
            if (bl && this.total() < this.config.maxListings() && this.moreToList()) {
                this.worker.sleep(500L);
            } else {
                this.ah.waitSalesCheck();
                if (this.ah.refreshListings(this.stocks)) {
                    this.inAh[0] = this.total();
                }
            }
        }
    }

    private boolean moreToList() {
        for (AuctionHouse.Stock stock : this.stocks) {
            if (stock.price > 0L
                    && this.ah.countInventory(stock.product) >= this.config.perListing()) {
                return true;
            }
        }
        return false;
    }

    private int total() {
        int i = 0;
        for (AuctionHouse.Stock stock : this.stocks) {
            i += stock.inAh;
        }
        return i;
    }

    void onChat(String string) {
        String string2 = ServerText.normalize(string);
        if (!string2.isEmpty()) {
            this.ah.onChat(string2, this.stocks);
        }
    }

    String summary() {
        return AuctionHouse.summary(this.stocks);
    }
}
