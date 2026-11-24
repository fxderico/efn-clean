package net.efn.client.util.combat;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public final class TotemPops {

    private TotemPops() {}

    public static void scan(Packet<?> packet, Consumer<LivingEntity> consumer) {
        if (packet instanceof EntityStatusS2CPacket entityStatusPacket) {
            check(entityStatusPacket, consumer);
        } else if (packet instanceof BundleS2CPacket bundlePacket) {
            for (Packet packet2 : bundlePacket.getPackets()) {
                if (packet2 instanceof EntityStatusS2CPacket entityStatusPacket2) {
                    check(entityStatusPacket2, consumer);
                }
            }
        }
    }

    private static void check(
            EntityStatusS2CPacket entityStatusPacket, Consumer<LivingEntity> consumer) {
        if (entityStatusPacket.getStatus() == 35) {
            ClientWorld world = MinecraftClient.getInstance().world;
            if (world != null) {
                Entity entity = entityStatusPacket.getEntity(world);
                LivingEntity entity2;
                if (entity instanceof LivingEntity
                        && !(entity2 = (LivingEntity) entity).isRemoved()) {
                    consumer.accept(entity2);
                }
            }
        }
    }
}
