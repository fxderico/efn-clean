package net.efn.client.module.impl.donutmacros;

import net.efn.client.chat.ModuleChat;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.StringSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public final class TpMacroModule extends ClientModule {

    private static final Pattern RATE_LIMIT =
            Pattern.compile("^you need to wait another ([\\d.]+) seconds? to execute a command");

    private static final Pattern VALID_NAME = Pattern.compile("\\.?[A-Za-z0-9_]{1,16}");

    private final ModeSetting command =
            this.addSetting("command", new ModeSetting("Command", "tpa", "tpa", "tpahere"));

    private final StringSetting target =
            this.addSetting("player", new StringSetting("Player", "").hint("Player name"));

    private final NumberSetting interval =
            this.addSetting(
                    "interval", new NumberSetting("Interval (s)", 1.0F, 0.25F, 60.0F).step(0.25F));

    private final TeleportConfirm confirm = new TeleportConfirm(this);

    private volatile long holdUntil;

    private long nextAt;

    private int sent;

    public TpMacroModule() {
        super(
                "tp-macro",
                "Tp Macro",
                "Sends /tpa or /tpahere to one player on a timer.",
                ModuleCategory.DONUT_MACROS);
        this.usesNotifications();
        this.target.privateValue();
    }

    @Override
    public boolean persistEnabled() {
        return false;
    }

    @Override
    protected void onEnable() {
        String string = this.target.get().trim();
        if (!VALID_NAME.matcher(string).matches()) {
            ModuleChat.send(this, ModuleChat.body().warn("Set a valid Player name first."));
            this.requestDisable();
        } else {
            this.confirm.reset();
            this.holdUntil = 0L;
            this.nextAt = 0L;
            this.sent = 0;
            ModuleChat.send(
                    this,
                    ModuleChat.body()
                            .text("Sending ")
                            .value("/" + this.command.get() + " " + string)
                            .text(" every ")
                            .value(this.interval.formatted() + "s")
                            .text("."));
        }
    }

    @Override
    protected void onDisable() {
        if (this.sent > 0) {
            ModuleChat.send(
                    this,
                    ModuleChat.body()
                            .text("Sent ")
                            .value(this.sent)
                            .text(this.sent == 1 ? " request." : " requests."));
        }
    }

    @EventHandler
    public void onPacket(PacketReceiveNettyEvent event) {
        Packet packet = event.packet;
        GameMessageS2CPacket gameMessagePacket;
        if (packet instanceof GameMessageS2CPacket
                && (gameMessagePacket = (GameMessageS2CPacket) packet).content() != null) {
            Matcher matcher =
                    RATE_LIMIT.matcher(
                            ServerText.normalize(gameMessagePacket.content().getString()));
            if (matcher.find()) {
                try {
                    this.holdUntil =
                            System.currentTimeMillis()
                                    + (long)
                                            Math.ceil(
                                                    Math.min(
                                                                    30.0,
                                                                    Double.parseDouble(
                                                                            matcher.group(1)))
                                                            * 1000.0)
                                    + 80L;
                } catch (NumberFormatException exception) {
                }
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player != null && client.getNetworkHandler() != null && !player.isDead()) {
            this.confirm.tick(client);
            long l = System.currentTimeMillis();
            if (l >= this.nextAt && l >= this.holdUntil) {
                player.networkHandler.sendChatCommand(
                        this.command.get() + " " + this.target.get().trim());
                this.confirm.sent();
                this.sent++;
                this.nextAt = l + (long) (this.interval.get() * 1000.0F);
            }
        }
    }
}
