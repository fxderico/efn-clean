package net.efn.client.module.impl.donutmacros;

import net.efn.client.event.EventPriority;
import net.efn.client.event.ListenerHandle;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.setting.StringListSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public final class DonutSell {

    private static final Pattern RATE_LIMIT =
            Pattern.compile("you need to wait another ([\\d.]+) seconds?");

    private static final long GIVE_UP_MS = 45000L;

    private final SellRoutine routine;

    private final ListenerHandle<PacketReceiveNettyEvent> chat;

    private final long started = System.currentTimeMillis();

    private boolean sent;

    private boolean done;

    private int ticks;

    private DonutSell(ClientModule module, Map<Integer, Item> map) {
        Map map2 = Map.copyOf(map);
        this.routine =
                new SellRoutine(
                        module,
                        new StringListSetting("Never Sell"),
                        (integer, itemStack) ->
                                map2.get(integer) == null
                                        || !itemStack.isOf((Item) map2.get(integer)),
                        () -> this.sent = true);
        this.chat =
                PacketReceiveNettyEvent.TYPE.listen(
                        this,
                        Stage.PRE,
                        EventPriority.NORMAL,
                        packetReceiveNettyEvent -> {
                            Packet packet = packetReceiveNettyEvent.packet;
                            GameMessageS2CPacket gameMessagePacket;
                            if (packet instanceof GameMessageS2CPacket
                                    && (gameMessagePacket = (GameMessageS2CPacket) packet).content()
                                            != null) {
                                Matcher matcher =
                                        RATE_LIMIT.matcher(
                                                ServerText.normalize(
                                                        gameMessagePacket.content().getString()));
                                if (matcher.find()) {
                                    try {
                                        long l =
                                                System.currentTimeMillis()
                                                        + (long)
                                                                Math.ceil(
                                                                        Double.parseDouble(
                                                                                        matcher
                                                                                                .group(
                                                                                                        1))
                                                                                * 1000.0)
                                                        + 100L;
                                        MinecraftClient.getInstance()
                                                .execute(() -> this.routine.commandRejected(l));
                                    } catch (NumberFormatException exception) {
                                    }
                                }
                            }
                        });
    }

    public static DonutSell start(ClientModule module, Map<Integer, Item> map) {
        return new DonutSell(module, map);
    }

    public boolean tick() {
        if (this.done) {
            return false;
        } else {
            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            if (player != null
                    && client.getNetworkHandler() != null
                    && client.interactionManager != null) {
                long l = System.currentTimeMillis();
                boolean step =
                        this.routine.step(client, player, client.getNetworkHandler(), l, true);
                this.ticks++;
                if (!step && !this.routine.active()) {
                    if (this.sent || this.ticks > 3 || l - this.started > 45000L) {
                        this.finish();
                    }
                    return !this.done;
                } else {
                    if (l - this.started > 45000L) {
                        this.cancel();
                    }
                    return !this.done;
                }
            } else {
                this.cancel();
                return false;
            }
        }
    }

    public void cancel() {
        if (!this.done) {
            this.routine.abort(MinecraftClient.getInstance());
            this.finish();
        }
    }

    private void finish() {
        this.done = true;
        this.chat.unregister();
    }
}
