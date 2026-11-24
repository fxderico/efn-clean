package net.efn.client.module.impl.donutmacros;

import net.efn.client.chat.ModuleChat;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.StringSetting;
import net.efn.client.util.ItemEnchants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class AhSellModule extends ClientModule {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final int SLOTS = 9;

    private static final String FIXED = "Fixed";

    private static final String AUTO = "Auto (beat cheapest)";

    private final ModeSetting count =
            this.addSetting(
                    "items",
                    new ModeSetting("Items", "1", "1", "2", "3", "4", "5", "6", "7", "8", "9"));

    private final ModeSetting pricing =
            this.addSetting(
                    "pricing",
                    new ModeSetting("Pricing", "Fixed", "Fixed", "Auto (beat cheapest)"));

    private final StringSetting[] items = new StringSetting[9];

    private final StringSetting[] enchants = new StringSetting[9];

    private final StringSetting[] prices = new StringSetting[9];

    private final StringSetting undercut;

    private final NumberSetting priceCheck;

    private final NumberSetting relistGap;

    private final NumberSetting ignoreDrops;

    private final NumberSetting salesCheck;

    private final NumberSetting perListing;

    private final NumberSetting maxListings;

    private final NumberSetting listingDelay;

    private final MacroWorker worker = new MacroWorker("EFN AH Sell");

    private volatile AhSellBot bot;

    public AhSellModule() {
        super(
                "ah-sell",
                "AH Sell",
                "Lists up to 9 items from your inventory on the Auction House at your price or the"
                        + " cheapest price.",
                ModuleCategory.DONUT_MACROS);
        this.usesNotifications();
        for (int i = 0; i < 9; i++) {
            int j = i + 1;
            int k = i;
            this.items[i] =
                    this.addSetting(
                            "item-" + j,
                            new StringSetting("Item " + j, "")
                                    .hint("e.g. diamond_block")
                                    .itemPicker());
            this.enchants[i] =
                    this.addSetting(
                            "enchants-" + j,
                            new StringSetting("Enchantments " + j, "").enchantsFor(this.items[i]));
            this.prices[i] =
                    this.addSetting(
                            "price-" + j,
                            new StringSetting("Price " + j + " (each)", "")
                                    .hint("e.g. 5k (Auto: lowest price)"));
            this.items[i].visibleWhen(() -> j <= this.slotCount());
            this.enchants[i].visibleWhen(
                    () ->
                            j <= this.slotCount()
                                    && ItemEnchants.supports(
                                            Product.resolveItem(this.items[k].get())));
            this.prices[i].visibleWhen(() -> j <= this.slotCount());
        }
        this.undercut =
                this.addSetting(
                        "undercut", new StringSetting("Undercut By", "100").hint("e.g. 100"));
        this.priceCheck =
                this.addSetting(
                        "price-check",
                        new NumberSetting("Price Check (s)", 120.0F, 10.0F, 600.0F).step(10.0F));
        this.relistGap =
                this.addSetting(
                        "relist-gap",
                        new NumberSetting("Relist Higher At (x undercut)", 3.0F, 1.0F, 20.0F)
                                .step(0.5F));
        this.ignoreDrops =
                this.addSetting(
                        "ignore-drops",
                        new NumberSetting("Ignore Drops Over (x undercut)", 20.0F, 1.0F, 100.0F)
                                .step(1.0F));
        this.salesCheck =
                this.addSetting(
                        "sales-check",
                        new NumberSetting("Sales Check (s)", 20.0F, 5.0F, 300.0F).step(5.0F));
        this.perListing =
                this.addSetting(
                        "per-listing",
                        new NumberSetting("Items Per Listing", 1.0F, 1.0F, 64.0F).step(1.0F));
        this.maxListings =
                this.addSetting(
                        "max-listings",
                        new NumberSetting("Max Listings", 25.0F, 1.0F, 50.0F).step(1.0F));
        this.listingDelay =
                this.addSetting(
                        "listing-delay",
                        new NumberSetting("Listing Delay (s)", 0.0F, 0.0F, 5.0F).step(0.25F));
        this.undercut.visibleWhen(this::auto);
        this.priceCheck.visibleWhen(this::auto);
        this.relistGap.visibleWhen(this::auto);
        this.ignoreDrops.visibleWhen(this::auto);
    }

    private int slotCount() {
        return Integer.parseInt(this.count.get());
    }

    private boolean auto() {
        return this.pricing.is("Auto (beat cheapest)");
    }

    @Override
    public boolean persistEnabled() {
        return false;
    }

    @Override
    protected void onEnable() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.getSession() != null) {
            ArrayList list = new ArrayList();
            String products = this.buildProducts(list);
            if (products != null) {
                this.info(ModuleChat.body().warn(products));
                this.requestDisable();
            } else {
                AuctionHouse.Config config =
                        new AuctionHouse.Config(
                                Math.max(0L, ServerText.parseMoney(this.undercut.get())),
                                (long) (this.priceCheck.get() * 1000.0F),
                                (long) (this.salesCheck.get() * 1000.0F),
                                this.perListing.getInt(),
                                this.maxListings.getInt(),
                                (long) (this.listingDelay.get() * 1000.0F),
                                this.relistGap.get(),
                                this.ignoreDrops.get(),
                                36,
                                15000L);
                AhSellBot ahSellBot;
                this.bot =
                        ahSellBot =
                                new AhSellBot(
                                        this,
                                        this.worker,
                                        list,
                                        !this.auto(),
                                        config,
                                        client.getSession().getUsername());
                this.worker.start(
                        ahSellBot::run,
                        throwable -> {
                            LOGGER.error("[EFN] AH Sell stopped", throwable);
                            this.info(ModuleChat.body().danger("Stopped").text(": " + throwable));
                            client.execute(
                                    () -> {
                                        if (this.bot == ahSellBot) {
                                            this.setEnabled(false);
                                        }
                                    });
                        });
            }
        } else {
            this.info(ModuleChat.body().warn("Join a server first."));
            this.requestDisable();
        }
    }

    @Override
    protected void onDisable() {
        AhSellBot bot2 = this.bot;
        this.bot = null;
        this.worker.stop();
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (bot2 != null && player != null) {
            if (player.currentScreenHandler != player.playerScreenHandler) {
                player.closeHandledScreen();
            } else {
                Dialogs.close(client);
            }
            this.info(ModuleChat.body().text(bot2.summary()));
        }
    }

    @EventHandler
    public void onPacket(PacketReceiveNettyEvent event) {
        AhSellBot bot2 = this.bot;
        if (bot2 != null) {
            Packet packet = event.packet;
            GameMessageS2CPacket gameMessagePacket;
            if (event.packet instanceof GameMessageS2CPacket
                    && (gameMessagePacket = (GameMessageS2CPacket) packet).content() != null) {
                bot2.onChat(gameMessagePacket.content().getString());
            }
        }
    }

    private String buildProducts(List<Product> list) {
        if (this.auto() && ServerText.parseMoney(this.undercut.get()) < 0L) {
            return "Undercut By must be a price like 100.";
        } else {
            HashSet set = new HashSet();
            for (int i = 0; i < this.slotCount(); i++) {
                String string = this.items[i].get().trim();
                if (!string.isEmpty()) {
                    Item item = Product.resolveItem(string);
                    if (item == null) {
                        return "Item " + (i + 1) + ": unknown item '" + string + "'.";
                    }
                    if (!set.add(item)) {
                        return "Item " + (i + 1) + ": " + string + " is listed twice.";
                    }
                    long money = ServerText.parseMoney(this.prices[i].get());
                    if (money <= 0L) {
                        return "Item "
                                + (i + 1)
                                + ": set a price like 5k"
                                + (this.auto() ? " (the lowest price Auto may use)." : ".");
                    }
                    List list2 =
                            ItemEnchants.supports(item)
                                    ? Product.parseEnchants(this.enchants[i].get())
                                    : List.of();
                    list.add(
                            new Product(
                                    item,
                                    string,
                                    Product.searchText(item),
                                    new ItemStack(item).getName().getString(),
                                    money,
                                    list2,
                                    false,
                                    1,
                                    true,
                                    1L,
                                    1L,
                                    0L));
                }
            }
            return list.isEmpty() ? "Set at least one item." : null;
        }
    }

    private void info(ModuleChat.Body body) {
        ModuleChat.send(this, body);
    }
}
