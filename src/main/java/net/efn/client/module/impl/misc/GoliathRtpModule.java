/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.module.impl.misc;

import net.efn.client.chat.ModuleChat;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.gui.GoliathPickerScreen;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.util.donut.DonutGoliaths;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.world.World;

import java.util.Set;
import java.util.TreeMap;

@Environment(value = EnvType.CLIENT)
public final class GoliathRtpModule extends ClientModule {

    private static final int[] HOP = new int[] {1, 0, 0, 4, 3, 6, 5};

    private static final int SETTLE_TICKS = 15;

    private static final int RESEND_TICKS = 100;

    private static final double LANDED_SQ = 10000.0;

    private static Set<DonutGoliaths.Goliath> lastPick = Set.of();

    private Set<DonutGoliaths.Goliath> targets;

    private String pending;

    private int sinceSent;

    private int settle;

    private int sent;

    private double lastX;

    private double lastZ;

    public GoliathRtpModule() {
        super(
                "goliath-rtp",
                "Goliath RTP",
                "Opens a map of every goliath: pick as many as you like and it uses /rtp until you"
                        + " land on one of them.",
                ModuleCategory.DONUT);
    }

    @Override
    public boolean persistEnabled() {
        return false;
    }

    @Override
    protected void onEnable() {
        this.targets = null;
        this.pending = null;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.getNetworkHandler() == null) {
            ModuleChat.send((ClientModule) this, ModuleChat.body().text("Join the server first."));
            this.requestDisable();
            return;
        }
        client.execute(
                () -> {
                    if (this.isEnabled()) {
                        client.setScreen(new GoliathPickerScreen(lastPick, this::picked));
                    }
                });
    }

    @Override
    protected void onDisable() {
        this.targets = null;
        this.pending = null;
    }

    private void picked(Set<DonutGoliaths.Goliath> targets2) {
        if (!this.isEnabled()) {
            return;
        }
        if (targets2 == null || targets2.isEmpty()) {
            this.requestDisable();
            return;
        }
        this.targets = targets2;
        lastPick = targets2;
        this.pending = null;
        this.settle = 0;
        this.sent = 0;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            this.lastX = player.getX();
            this.lastZ = player.getZ();
        }
        if (targets2.size() == 1) {
            DonutGoliaths.Goliath goliath = targets2.iterator().next();
            ModuleChat.send(
                    (ClientModule) this,
                    ModuleChat.body()
                            .text("Going to ")
                            .name(goliath.label())
                            .text(" with /rtp " + DonutGoliaths.RTP[goliath.region()] + "."));
            return;
        }
        TreeMap<String, Integer> map = new TreeMap<String, Integer>();
        for (DonutGoliaths.Goliath goliath2 : targets2) {
            map.merge(DonutGoliaths.REGIONS[goliath2.region()], 1, Integer::sum);
        }
        StringBuilder sb = new StringBuilder();
        map.forEach(
                (value, integer) ->
                        sb.append(sb.isEmpty() ? "" : ", ")
                                .append((String) value)
                                .append(' ')
                                .append(integer));
        ModuleChat.send(
                (ClientModule) this,
                ModuleChat.body()
                        .text("Going to any of ")
                        .name(targets2.size() + " goliaths")
                        .text(" (" + String.valueOf(sb) + ")."));
    }

    @EventHandler
    public void onTick(TickEvent event) {
        DonutGoliaths.Goliath goliath;
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (this.targets == null
                || player == null
                || client.world == null
                || client.getNetworkHandler() == null) {
            return;
        }
        double d = player.getX() - this.lastX;
        double e;
        // per bytecode: dz*dz
        boolean bl = d * d + (e = player.getZ() - this.lastZ) * e > 10000.0;
        this.lastX = player.getX();
        this.lastZ = player.getZ();
        if (this.pending != null) {
            if (!bl) {
                if (++this.sinceSent >= 100) {
                    this.send(client, this.pending);
                }
                return;
            }
            this.pending = null;
            this.settle = 15;
        }
        if ((goliath = GoliathRtpModule.here(client)) != null && this.targets.contains(goliath)) {
            ModuleChat.send(
                    (ClientModule) this,
                    ModuleChat.body()
                            .text("Landed on ")
                            .name(goliath.label())
                            .text(" after " + this.sent + " /rtp."));
            this.requestDisable();
            return;
        }
        if (this.settle > 0) {
            --this.settle;
            return;
        }
        int i = goliath == null ? -1 : goliath.region();
        int bestRegion = this.bestRegion(i);
        if (bestRegion < 0) {
            bestRegion = i >= 0 ? HOP[i] : HOP[0];
        }
        this.send(client, DonutGoliaths.RTP[bestRegion]);
    }

    private int bestRegion(int i) {
        int[] ints = new int[DonutGoliaths.REGIONS.length];
        for (DonutGoliaths.Goliath target : this.targets) {
            int region = target.region();
            ints[region] = ints[region] + 1;
        }
        int k = -1;
        double d = 0.0;
        for (int j = 0; j < ints.length; ++j) {
            double e;
            if (j == i
                    || ints[j] == 0
                    || DonutGoliaths.RTP[j] == null
                    || !((e = (double) ints[j] / (double) DonutGoliaths.CELLS[j].length) > d))
                continue;
            k = j;
            d = e;
        }
        return k;
    }

    private void send(MinecraftClient client, String pending2) {
        client.getNetworkHandler().sendChatCommand("rtp " + pending2);
        this.pending = pending2;
        this.sinceSent = 0;
        ++this.sent;
    }

    private static DonutGoliaths.Goliath here(MinecraftClient client) {
        if (client.world.getRegistryKey() == World.END) {
            return null;
        }
        double d = client.world.getRegistryKey() == World.NETHER ? 8.0 : 1.0;
        return DonutGoliaths.at(client.player.getX() * d, client.player.getZ() * d);
    }
}
