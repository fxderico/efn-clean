package net.efn.client.module.impl.donutmacros;

import net.efn.client.chat.ModuleChat;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.StringSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class AhSniperModule extends ClientModule {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final String EXACT = "Exactly these";

    private static final String AT_LEAST = "At least these";

    private final StringSetting item =
            this.addSetting(
                    "item",
                    new StringSetting("Item", SnipeTarget.Preset.SHARD_AXE.value)
                            .presets(SnipeTarget.Preset.choices()));

    private final StringSetting maxPrice =
            this.addSetting(
                    "max-price", new StringSetting("Buy Below (each)", "").hint("e.g. 50m"));

    private final StringSetting enchants =
            this.addSetting(
                    "enchants",
                    new StringSetting("Enchantments", "")
                            .hint("e.g. protection 2, unbreaking 3, mending"));

    private final ModeSetting enchantMatch =
            this.addSetting(
                    "enchant-match",
                    new ModeSetting(
                            "Enchant Match", "Exactly these", "Exactly these", "At least these"));

    private final NumberSetting minHours =
            this.addSetting(
                    "min-hours",
                    new NumberSetting("Min Time Left (h)", 0.0F, 0.0F, 48.0F).step(0.5F));

    private final NumberSetting refresh =
            this.addSetting(
                    "refresh",
                    new NumberSetting("Refresh Every (ms)", 300.0F, 100.0F, 2000.0F).step(50.0F));

    private final NumberSetting settle =
            this.addSetting(
                    "settle",
                    new NumberSetting("Wait After Page Change (ms)", 200.0F, 50.0F, 1000.0F)
                            .step(25.0F));

    private final StringSetting budget =
            this.addSetting(
                    "budget",
                    new StringSetting("Max Total Spend", "").hint("e.g. 500m, empty = no limit"));

    private final BooleanSetting requireConfirm =
            this.addSetting("require-confirm", new BooleanSetting("Only Buy With Confirm", false));

    private final NumberSetting maxBuys =
            this.addSetting(
                    "max-buys",
                    new NumberSetting("Stop After (buys, 0 = never)", 0.0F, 0.0F, 100.0F)
                            .step(1.0F));

    private final BooleanSetting autoSell =
            this.addSetting("auto-sell", new BooleanSetting("Auto Sell", false));

    private final StringSetting sellPrice =
            this.addSetting(
                    "sell-price", new StringSetting("Sell Price (each)", "").hint("e.g. 60m"));

    private final MacroWorker worker = new MacroWorker("EFN AH Sniper");

    private volatile SniperBot bot;

    public AhSniperModule() {
        super(
                "ah-sniper",
                "AH Sniper",
                "Refreshes the AH and instantly buys your item below your price, with optional auto"
                        + " selling.",
                ModuleCategory.DONUT_MACROS);
        this.usesNotifications();
        this.enchantMatch.visibleWhen(() -> !this.enchants.get().isBlank());
        this.sellPrice.visibleWhen(this.autoSell::get);
    }

    @Override
    public boolean persistEnabled() {
        return false;
    }

    @Override
    protected void onEnable() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null
                && client.getNetworkHandler() != null
                && client.getSession() != null) {
            long money = ServerText.parseMoney(this.maxPrice.get());
            if (money <= 0L) {
                this.say(ModuleChat.body().warn("Set Buy Below to a price like 50m."));
                this.requestDisable();
            } else {
                SnipeTarget snipeTarget =
                        SnipeTarget.parse(
                                this.item.get(),
                                this.enchants.get(),
                                this.enchantMatch.is("Exactly these"),
                                money,
                                Math.round(this.minHours.get() * 60.0F));
                if (snipeTarget == null) {
                    this.say(ModuleChat.body().warn("Pick an item to snipe first."));
                    this.requestDisable();
                } else {
                    long l = 0L;
                    if (!this.budget.get().isBlank()
                            && (l = ServerText.parseMoney(this.budget.get())) <= 0L) {
                        this.say(
                                ModuleChat.body()
                                        .warn(
                                                "Set Max Total Spend to a price like 500m, or leave"
                                                        + " it empty."));
                        this.requestDisable();
                    } else {
                        long l2 = 0L;
                        if (this.autoSell.get()) {
                            l2 = ServerText.parseMoney(this.sellPrice.get());
                            if (l2 <= 0L) {
                                this.say(
                                        ModuleChat.body()
                                                .warn(
                                                        "Set Sell Price to a price like 60m, or"
                                                                + " turn Auto Sell off."));
                                this.requestDisable();
                                return;
                            }
                            if (l2 <= money) {
                                this.say(
                                        ModuleChat.body()
                                                .warn("Sell Price must be higher than Buy Below")
                                                .text(
                                                        ", or the sniper would buy your own listing"
                                                                + " back."));
                                this.requestDisable();
                                return;
                            }
                        }
                        SniperBot sniperBot;
                        this.bot =
                                sniperBot =
                                        new SniperBot(
                                                this,
                                                this::stopFromBot,
                                                this.worker,
                                                snipeTarget,
                                                this.refresh.getInt(),
                                                this.settle.getInt(),
                                                l,
                                                this.requireConfirm.get(),
                                                this.maxBuys.getInt(),
                                                l2,
                                                client.getSession().getUsername());
                        this.worker.start(
                                sniperBot::run,
                                throwable -> {
                                    LOGGER.error("[EFN] AH Sniper stopped", throwable);
                                    this.say(
                                            ModuleChat.body()
                                                    .danger("Stopped")
                                                    .text(": " + throwable));
                                    this.stopFromBot(sniperBot);
                                });
                    }
                }
            }
        } else {
            this.say(ModuleChat.body().warn("Join a server first."));
            this.requestDisable();
        }
    }

    void stopFromBot(SniperBot sniperBot) {
        MinecraftClient.getInstance()
                .execute(
                        () -> {
                            if (this.bot == sniperBot && this.isEnabled()) {
                                this.setEnabled(false);
                            }
                        });
    }

    @Override
    protected void onDisable() {
        SniperBot bot2 = this.bot;
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
            this.say(ModuleChat.body().text(bot2.summary()));
        }
    }

    @EventHandler
    public void onPacket(PacketReceiveNettyEvent event) {
        SniperBot bot2 = this.bot;
        if (bot2 != null) {
            Packet packet = event.packet;
            GameMessageS2CPacket gameMessagePacket;
            if (event.packet instanceof GameMessageS2CPacket
                    && (gameMessagePacket = (GameMessageS2CPacket) packet).content() != null) {
                bot2.onChat(gameMessagePacket.content().getString());
            }
        }
    }

    private void say(ModuleChat.Body body) {
        ModuleChat.send(this, body);
    }
}
