package net.efn.client.module.impl.donutmacros;

import net.efn.client.chat.ModuleChat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.List;

@Environment(EnvType.CLIENT)
final class OrderAhBot {

    private static final int MAX_LISTINGS_PER_PASS = 9;

    private static final long RECOVERY_MS = 3000L;

    final AuctionHouse ah;

    final MacroWorker worker;

    final Gui gui;

    final Product product;

    final AuctionHouse.Config config;

    private final AuctionHouse.Stock stock;

    private final List<AuctionHouse.Stock> stocks;

    private final OrderRestock restock;

    private final int[] inAh = new int[] {0};

    private boolean warnedFull;

    volatile long orderedAt;

    OrderAhBot(
            OrderAhSellerModule module,
            MacroWorker worker2,
            Product product2,
            AuctionHouse.Config config2,
            String string) {
        this.ah = new AuctionHouse(module, worker2, config2, string);
        this.worker = worker2;
        this.gui = this.ah.gui;
        this.product = product2;
        this.config = config2;
        this.stock = new AuctionHouse.Stock(product2, false);
        this.stocks = List.of(this.stock);
        this.restock = new OrderRestock(this);
    }

    void say(ModuleChat.Body body) {
        this.ah.say(body);
    }

    boolean inventoryCapReached() {
        return this.ah.inventoryCapReached(this.product);
    }

    long sellPrice() {
        return this.stock.price;
    }

    void run() {
        this.say(
                ModuleChat.body()
                        .text("Selling ")
                        .name(this.product.describe())
                        .text(this.product.restock() ? ", restocking with orders." : "."));
        try {
            this.gui.closeAll();
            if (this.ah.refreshListings(this.stocks)) {
                this.inAh[0] = this.stock.inAh;
            }
        } catch (MacroWorker.Cancelled exception) {
            throw exception;
        } catch (RuntimeException exception2) {
            this.gui.closeAll();
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
                this.say(ModuleChat.body().warn(string).text(", trying again."));
                this.worker.check();
                this.gui.closeAll();
                this.worker.sleep(3000L);
            }
        }
    }

    private void cycle() {
        if (!this.ah.inWorld()) {
            this.worker.sleep(2000L);
        } else {
            this.ah.ensureAlive();
            this.ah.ensurePrice(this.stock);
            this.inAh[0] = this.stock.inAh;
            if (this.stock.price <= 0L) {
                this.worker.sleep(Math.min(5000L, this.config.priceCheckMs()));
            } else {
                int countInventory = this.ah.countInventory(this.product);
                if (countInventory < this.config.perListing()
                        && this.product.restock()
                        && this.inAh[0] < this.config.maxListings()
                        && this.restock.run()) {
                    countInventory = this.ah.countInventory(this.product);
                }
                if (countInventory >= this.config.perListing()) {
                    int i = this.config.maxListings() - this.inAh[0];
                    if (i <= 0) {
                        if (!this.warnedFull) {
                            this.say(
                                    ModuleChat.body()
                                            .text("AH full (")
                                            .value(this.inAh[0] + "/" + this.config.maxListings())
                                            .text("), waiting for sales."));
                        }
                        this.warnedFull = true;
                    } else {
                        this.warnedFull = false;
                        int listSome =
                                this.ah.listSome(
                                        this.stock,
                                        Math.min(
                                                Math.min(
                                                        countInventory / this.config.perListing(),
                                                        i),
                                                9),
                                        this.inAh);
                        if (listSome > 0
                                && this.ah.countInventory(this.product) >= this.config.perListing()
                                && this.inAh[0] < this.config.maxListings()) {
                            this.worker.sleep(500L);
                            return;
                        }
                    }
                }
                this.ah.waitSalesCheck();
                if (this.ah.refreshListings(this.stocks)) {
                    this.inAh[0] = this.stock.inAh;
                }
                int countInventory2;
                if ((countInventory2 = this.ah.countInventory(this.product))
                                < this.config.perListing()
                        || this.inAh[0] >= this.config.maxListings()) {
                    if (countInventory2 < this.config.perListing()
                            && this.product.restock()
                            && this.inAh[0] < this.config.maxListings()) {
                        this.restock.run();
                    }
                }
            }
        }
    }

    void onChat(String string) {
        String string2 = ServerText.normalize(string);
        if (!string2.isEmpty()) {
            if (string2.startsWith("you ordered")) {
                this.orderedAt = System.currentTimeMillis();
            }
            this.ah.onChat(string2, this.stocks);
        }
    }

    String summary() {
        return AuctionHouse.summary(this.stocks);
    }
}
