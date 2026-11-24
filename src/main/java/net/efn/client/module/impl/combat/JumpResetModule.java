package net.efn.client.module.impl.combat;

import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.PacketAppliedEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.util.pvp.InputControl;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class JumpResetModule extends ClientModule {

    private static final double MIN_KNOCKBACK = 0.1;

    private boolean pending;

    public JumpResetModule() {
        super(
                "jump-reset",
                "Jump Reset",
                "Jumps the tick knockback lands to take less of it.",
                ModuleCategory.COMBAT);
    }

    @Override
    protected void onDisable() {
        this.pending = false;
    }

    @EventHandler
    public void onPacketApplied(PacketAppliedEvent event) {
        if (event.packet instanceof EntityVelocityUpdateS2CPacket entityVelocityUpdatePacket) {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null && entityVelocityUpdatePacket.getEntityId() == player.getId()) {
                Vec3d velocity = entityVelocityUpdatePacket.getVelocity();
                if (velocity.horizontalLength() >= 0.1 && velocity.y > 0.0) {
                    this.pending = true;
                }
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (this.pending) {
            this.pending = false;
            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            if (player != null && client.currentScreen == null) {
                if (player.isOnGround()
                        && player.isSprinting()
                        && player.input.playerInput.forward()) {
                    if (!player.isTouchingWater()
                            && !player.isInLava()
                            && !player.isClimbing()
                            && !player.hasVehicle()
                            && !player.isGliding()) {
                        InputControl.jump();
                    }
                }
            }
        }
    }
}
