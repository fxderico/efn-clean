package net.efn.client.module.impl.combat;

import net.efn.client.event.EventHandler;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ThreadLocalRandom;

@Environment(EnvType.CLIENT)
public final class KnockbackDelayModule extends ClientModule {

    private static final double FIGHT_RANGE = 6.0;

    private final NumberSetting chance =
            this.addSetting(
                    "chance", new NumberSetting("Chance (%)", 40.0F, 0.0F, 100.0F).step(5.0F));

    private final NumberSetting airMin =
            this.addSetting(
                    "air-min",
                    new NumberSetting("Air Delay Min (ms)", 50.0F, 0.0F, 500.0F).step(10.0F));

    private final NumberSetting airMax =
            this.addSetting(
                    "air-max",
                    new NumberSetting("Air Delay Max (ms)", 100.0F, 0.0F, 500.0F).step(10.0F));

    private final NumberSetting groundMin =
            this.addSetting(
                    "ground-min",
                    new NumberSetting("Ground Delay Min (ms)", 200.0F, 0.0F, 1000.0F).step(10.0F));

    private final NumberSetting groundMax =
            this.addSetting(
                    "ground-max",
                    new NumberSetting("Ground Delay Max (ms)", 250.0F, 0.0F, 1000.0F).step(10.0F));

    private final BooleanSetting waterCheck =
            this.addSetting("water-check", new BooleanSetting("Not In Water", false));

    private final BooleanSetting onlyFights =
            this.addSetting("only-fights", new BooleanSetting("Only Near Enemies", true));

    private final MinecraftClient mc = MinecraftClient.getInstance();

    private final Deque<KnockbackDelayModule.Held> held = new ArrayDeque<>();

    private ClientPlayNetworkHandler heldFor;

    private volatile boolean fightNear;

    public KnockbackDelayModule() {
        super(
                "knockback-delay",
                "Knockback Delay",
                "Takes some hits' knockback a random moment late: a little in the air, longer on"
                        + " the ground.",
                ModuleCategory.COMBAT);
    }

    @Override
    protected void onEnable() {
        Deque held2 = this.held;
        synchronized (held2) {
            this.held.clear();
        }
        this.on(
                TickEvent.TYPE,
                Stage.PRE,
                tickEvent -> {
                    this.fightNear = this.mc.player != null && this.enemyNear(this.mc.player);
                    this.applyDue(false);
                });
    }

    @Override
    protected void onDisable() {
        this.mc.execute(() -> this.applyDue(true));
    }

    @EventHandler
    public void onPacketReceive(PacketReceiveNettyEvent event) {
        ClientPlayerEntity player = this.mc.player;
        if (player != null && pushesMe(event.packet, player)) {
            Deque held2 = this.held;
            boolean bl;
            synchronized (held2) {
                bl = !this.held.isEmpty();
            }
            if (bl || this.delays(player)) {
                boolean onGround = player.isOnGround();
                long l =
                        bl
                                ? 0L
                                : randomBetween(
                                        onGround ? this.groundMin.get() : this.airMin.get(),
                                        onGround ? this.groundMax.get() : this.airMax.get());
                Deque held3 = this.held;
                synchronized (held3) {
                    if (this.held.isEmpty()) {
                        this.heldFor = player.networkHandler;
                    }
                    long l2 =
                            Math.max(
                                    System.currentTimeMillis() + l,
                                    this.held.isEmpty() ? 0L : this.held.peekLast().due());
                    this.held.addLast(new KnockbackDelayModule.Held(event.packet, l2));
                }
                event.setCancelled(true);
            }
        }
    }

    private static boolean pushesMe(Packet<?> packet2, ClientPlayerEntity player) {
        ExplosionS2CPacket explosionPacket;
        return packet2 instanceof EntityVelocityUpdateS2CPacket entityVelocityUpdatePacket
                ? entityVelocityUpdatePacket.getEntityId() == player.getId()
                : packet2 instanceof ExplosionS2CPacket
                        && (explosionPacket = (ExplosionS2CPacket) packet2)
                                .playerKnockback()
                                .isPresent();
    }

    private boolean delays(ClientPlayerEntity player) {
        if (this.waterCheck.get() && player.isTouchingWater()) {
            return false;
        } else {
            return this.onlyFights.get() && !this.fightNear
                    ? false
                    : ThreadLocalRandom.current().nextDouble(100.0) < this.chance.get();
        }
    }

    private boolean enemyNear(ClientPlayerEntity player) {
        for (PlayerEntity player2 : player.getEntityWorld().getPlayers()) {
            if (player2 != player
                    && player2.distanceTo(player) <= 6.0
                    && MeleeAttacks.isEnemy(player, player2)) {
                return true;
            }
        }
        return false;
    }

    private static long randomBetween(float f, float g) {
        long l = Math.round(Math.min(f, g));
        long l2 = Math.round(Math.max(f, g));
        return l2 <= l ? l : ThreadLocalRandom.current().nextLong(l, l2 + 1L);
    }

    private void applyDue(boolean bl) {
        ClientPlayNetworkHandler networkHandler = this.mc.getNetworkHandler();
        long l = System.currentTimeMillis();
        while (true) {
            Deque held2 = this.held;
            KnockbackDelayModule.Held held3;
            synchronized (held2) {
                held3 = this.held.peekFirst();
                if (held3 == null || !bl && held3.due() > l) {
                    return;
                }
                this.held.pollFirst();
                if (networkHandler == null || networkHandler != this.heldFor) {
                    this.held.clear();
                    return;
                }
            }
            dispatch(held3.packet(), networkHandler);
        }
    }

    private static void dispatch(Packet packet2, PacketListener packetListener) {
        packet2.apply(packetListener);
    }

    @Environment(EnvType.CLIENT)
    private record Held(Packet<?> packet, long due) {}
}
