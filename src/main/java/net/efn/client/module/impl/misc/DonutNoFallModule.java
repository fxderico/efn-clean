package net.efn.client.module.impl.misc;

import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.PacketSendEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;

@Environment(EnvType.CLIENT)
public final class DonutNoFallModule extends ClientModule {

    private static final double TRIGGER_FALL_DISTANCE = 3.0;

    private static final int GROUND_PACKETS_TO_RESET = 4;

    private boolean noFall;

    private double lastFallDistance;

    private int groundTicks;

    private boolean sendingReplacement;

    public DonutNoFallModule() {
        super(
                "donut-nofall",
                "Donut NoFall",
                "Take no fall damage on DonutSMP.",
                ModuleCategory.MOVEMENT);
    }

    @Override
    protected void onEnable() {
        this.noFall = false;
        this.lastFallDistance = 0.0;
        this.groundTicks = 0;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            if (this.noFall) {
                player.setOnGround(false);
            }
            if (this.lastFallDistance > 3.0) {
                this.noFall = true;
            }
            this.lastFallDistance = player.fallDistance;
        }
    }

    @EventHandler
    public void onPacketSend(PacketSendEvent event) {
        if (!this.sendingReplacement) {
            Packet packet = event.packet;
            if (event.packet instanceof PlayerMoveC2SPacket) {
                PlayerMoveC2SPacket playerMovePacket = (PlayerMoveC2SPacket) packet;
                MinecraftClient client = MinecraftClient.getInstance();
                ClientPlayerEntity player = client.player;
                ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
                if (player != null && networkHandler != null) {
                    event.setCancelled(true);
                    if (this.noFall && playerMovePacket.isOnGround()) {
                        this.groundTicks++;
                    }
                    if (this.groundTicks % 2 == 1) {
                        player.setPos(player.getX(), player.getY() + 1.0E-8, player.getZ());
                    }
                    boolean bl = !this.noFall && player.isOnGround();
                    this.sendingReplacement = true;
                    try {
                        networkHandler.sendPacket(
                                new Full(
                                        player.getX(),
                                        player.getY(),
                                        player.getZ(),
                                        player.getYaw(),
                                        player.getPitch(),
                                        bl,
                                        false));
                    } finally {
                        this.sendingReplacement = false;
                    }
                    if (this.groundTicks == 4) {
                        this.noFall = false;
                        this.groundTicks = 0;
                    }
                    return;
                }
                return;
            }
        }
    }
}
