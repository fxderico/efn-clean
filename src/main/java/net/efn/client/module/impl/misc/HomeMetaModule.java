package net.efn.client.module.impl.misc;

import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class HomeMetaModule extends ClientModule {

    private static final double TELEPORT_DISTANCE_SQ = 25.0;

    private final NumberSetting homeNumber =
            this.addSetting(
                    "home-number", new NumberSetting("Home Number", 3.0F, 1.0F, 10.0F).step(1.0F));

    private final NumberSetting delay =
            this.addSetting("delay", new NumberSetting("Delay (s)", 0.5F, 0.0F, 10.0F).step(0.5F));

    private final NumberSetting rtpTimeout =
            this.addSetting(
                    "rtp-max-timeout",
                    new NumberSetting("RTP Timeout (s)", 10.0F, 1.0F, 30.0F).step(1.0F));

    private HomeMetaModule.Step step;

    private long nextAtMs;

    private long rtpDeadlineMs;

    private Vec3d rtpStart;

    private int home;

    public HomeMetaModule() {
        super(
                "home-meta",
                "Home Meta",
                "Re-sets a home, RTPs away, then teleports back home.",
                ModuleCategory.BASE_RAID);
    }

    @Override
    public boolean persistEnabled() {
        return false;
    }

    @Override
    public boolean inConfigs() {
        return false;
    }

    @Override
    protected void onEnable() {
        if (MinecraftClient.getInstance().player == null) {
            this.requestDisable();
        } else {
            this.home = this.homeNumber.getInt();
            this.step = HomeMetaModule.Step.DELHOME;
            this.nextAtMs = 0L;
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.getNetworkHandler() != null) {
            long l = System.currentTimeMillis();
            if (l >= this.nextAtMs) {
                long l2 = (long) (this.delay.get() * 1000.0);
                switch (this.step) {
                    case DELHOME:
                        command(client, "delhome " + this.home);
                        this.step = HomeMetaModule.Step.SETHOME;
                        this.nextAtMs = l + l2;
                        break;
                    case SETHOME:
                        command(client, "sethome " + this.home);
                        this.step = HomeMetaModule.Step.RTP;
                        this.nextAtMs = l + l2;
                        break;
                    case RTP:
                        this.rtpStart = client.player.getEntityPos();
                        command(client, "rtp");
                        this.rtpDeadlineMs = l + (long) (this.rtpTimeout.get() * 1000.0);
                        this.step = HomeMetaModule.Step.WAIT_TELEPORT;
                        break;
                    case WAIT_TELEPORT:
                        boolean bl =
                                client.player.getEntityPos().squaredDistanceTo(this.rtpStart)
                                        > 25.0;
                        if (bl || l >= this.rtpDeadlineMs) {
                            this.step = HomeMetaModule.Step.HOME;
                            this.nextAtMs = l + l2;
                        }
                        break;
                    case HOME:
                        command(client, "home " + this.home);
                        this.requestDisable();
                }
            }
        } else {
            this.requestDisable();
        }
    }

    private static void command(MinecraftClient client, String string) {
        client.getNetworkHandler().sendChatCommand(string);
    }

    @Environment(EnvType.CLIENT)
    private static enum Step {
        DELHOME,
        SETHOME,
        RTP,
        WAIT_TELEPORT,
        HOME;

        private Step() {}
    }
}
