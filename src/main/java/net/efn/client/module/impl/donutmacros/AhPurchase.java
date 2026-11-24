package net.efn.client.module.impl.donutmacros;

import net.efn.client.chat.ModuleChat;
import net.efn.client.event.EventPriority;
import net.efn.client.event.ListenerHandle;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.module.ClientModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
public final class AhPurchase {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private final ClientModule owner;

    private final MacroWorker worker = new MacroWorker("EFN AH Purchase");

    private final SniperBot bot;

    private final ListenerHandle<PacketReceiveNettyEvent> chat;

    private volatile AhPurchase.Status status = AhPurchase.Status.RUNNING;

    private AhPurchase(ClientModule owner2, SnipeTarget snipeTarget, int i, long l, long l2) {
        this.owner = owner2;
        this.bot = SniperBot.quota(owner2, this::stopped, this.worker, snipeTarget, i, l, l2);
        this.chat =
                PacketReceiveNettyEvent.TYPE.listen(
                        this,
                        Stage.PRE,
                        EventPriority.NORMAL,
                        packetReceiveNettyEvent -> {
                            if (this.status == AhPurchase.Status.RUNNING) {
                                Packet packet = packetReceiveNettyEvent.packet;
                                GameMessageS2CPacket gameMessagePacket;
                                if (!(packet instanceof GameMessageS2CPacket)
                                        || (gameMessagePacket = (GameMessageS2CPacket) packet)
                                                        .content()
                                                == null) {
                                    Packet packet2 = packetReceiveNettyEvent.packet;
                                    OverlayMessageS2CPacket overlayMessagePacket;
                                    if (packet2 instanceof OverlayMessageS2CPacket
                                            && (overlayMessagePacket =
                                                                    (OverlayMessageS2CPacket)
                                                                            packet2)
                                                            .text()
                                                    != null) {
                                        this.bot.onNote(overlayMessagePacket.text().getString());
                                    }
                                } else if (gameMessagePacket.overlay()) {
                                    this.bot.onNote(gameMessagePacket.content().getString());
                                } else {
                                    this.bot.onChat(gameMessagePacket.content().getString());
                                }
                            }
                        });
        this.worker.start(
                this.bot::run,
                throwable -> {
                    LOGGER.error("[EFN] AH purchase stopped", throwable);
                    ModuleChat.send(
                            owner2,
                            ModuleChat.body().danger("AH purchase stopped").text(": " + throwable));
                    this.stopped(this.bot);
                });
    }

    public static AhPurchase start(
            ClientModule module, Item item, int i, long l, long l2, long l3) {
        SnipeTarget snipeTarget =
                SnipeTarget.matching(
                        item,
                        new ItemStack(item).getName().getString(),
                        Product.searchText(item),
                        itemStack ->
                                itemStack.isOf(item)
                                        && !itemStack.contains(DataComponentTypes.BLOCK_STATE)
                                        && !itemStack.contains(
                                                DataComponentTypes.BLOCK_ENTITY_DATA),
                        l);
        return new AhPurchase(module, snipeTarget, i, l2, l3);
    }

    public static AhPurchase startAny(
            ClientModule module,
            Item item,
            String string,
            String string2,
            Predicate<ItemStack> predicate,
            int i,
            long l,
            long l2,
            long l3) {
        return new AhPurchase(
                module, SnipeTarget.matching(item, string, string2, predicate, l), i, l2, l3);
    }

    public static double unitPrice(ItemStack stack) {
        double askingPrice = ServerText.askingPrice(stack);
        return askingPrice <= 0.0
                ? 1.7976931348623157E308
                : askingPrice / Math.max(1, stack.getCount());
    }

    public static long parseMoney(String string) {
        return ServerText.parseMoney(string);
    }

    public static String money(double d) {
        return ServerText.shortMoney(d);
    }

    public AhPurchase.Status status() {
        return this.status;
    }

    public int boughtItems() {
        return this.bot.boughtItems();
    }

    public long spent() {
        return this.bot.spent();
    }

    public void cancel() {
        if (this.status == AhPurchase.Status.RUNNING) {
            this.finish(AhPurchase.Status.FAILED);
        }
    }

    private void stopped(SniperBot sniperBot) {
        this.finish(sniperBot.complete() ? AhPurchase.Status.DONE : AhPurchase.Status.FAILED);
    }

    private synchronized void finish(AhPurchase.Status status2) {
        if (this.status == AhPurchase.Status.RUNNING) {
            this.status = status2;
            this.chat.unregister();
            this.worker.stop();
            MinecraftClient.getInstance()
                    .execute(
                            () -> {
                                MinecraftClient client = MinecraftClient.getInstance();
                                ClientPlayerEntity player = client.player;
                                if (player != null) {
                                    if (player.currentScreenHandler != player.playerScreenHandler) {
                                        player.closeHandledScreen();
                                    } else {
                                        Dialogs.close(client);
                                    }
                                }
                            });
            if (status2 == AhPurchase.Status.FAILED) {
                LOGGER.info("[EFN] AH purchase for {} ended without finishing", this.owner.name());
            }
        }
    }

    @Environment(EnvType.CLIENT)
    public static enum Status {
        RUNNING,
        DONE,
        FAILED;

        private Status() {}
    }
}
