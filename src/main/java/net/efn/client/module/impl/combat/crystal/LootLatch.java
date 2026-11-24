package net.efn.client.module.impl.combat.crystal;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

import net.efn.client.event.impl.PacketAppliedEvent;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Util;

@Environment(EnvType.CLIENT)
public final class LootLatch {

    private static final double RANGE_SQR = 100.0;

    private static final int GRACE_TICKS = 10;

    private static final long ACTIVE_MS = 250L;

    private static final Int2IntOpenHashMap DEAD = new Int2IntOpenHashMap();

    private static ClientPlayerEntity self;

    private static int selfTick = -1;

    private static int clock;

    private static boolean reported;

    private static boolean latched;

    private static boolean killed;

    private static long updatedAt;

    private LootLatch() {}

    public static void update(MinecraftClient client, ClientPlayerEntity player) {
        if (player != self || player.age != selfTick) {
            self = player;
            selfTick = player.age;
            KeyBinding useKey = client.options.useKey;
            evaluate(
                    client.world,
                    player,
                    Crystals.isPhysicallyDown(client, useKey),
                    MeleeAttacks.clickPending(useKey));
        }
    }

    public static boolean latched() {
        return latched;
    }

    public static boolean killedNow() {
        return killed;
    }

    public static boolean active() {
        return latched && Util.getMeasuringTimeMs() - updatedAt < 250L;
    }

    private static void evaluate(
            ClientWorld world, ClientPlayerEntity player, boolean bl, boolean bl2) {
        int i = ++clock;
        updatedAt = Util.getMeasuringTimeMs();
        DEAD.int2IntEntrySet()
                .removeIf(
                        entry -> {
                            PlayerEntity player2;
                            Entity entityById;
                            return i - entry.getIntValue() > 10
                                    && (!((entityById = world.getEntityById(entry.getIntKey()))
                                                    instanceof PlayerEntity)
                                            || !(player2 = (PlayerEntity) entityById).isDead());
                        });
        boolean bl3 = reported;
        reported = false;
        for (PlayerEntity player3 : world.getPlayers()) {
            if (player3 != player && player3.isDead() && !DEAD.containsKey(player3.getId())) {
                DEAD.put(player3.getId(), i);
                if (player3.squaredDistanceTo(player) <= 100.0) {
                    bl3 = true;
                }
            }
        }
        killed = bl3;
        if (bl3 && bl) {
            latched = true;
        } else if (!bl || latched && bl2) {
            latched = false;
        }
    }

    private static void onPacket(PacketAppliedEvent event) {
        Packet packet = event.packet;
        EntityStatusS2CPacket entityStatusPacket;
        if (packet instanceof EntityStatusS2CPacket
                && (entityStatusPacket = (EntityStatusS2CPacket) packet).getStatus() == 3) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world != null && client.player != null) {
                Entity entity = entityStatusPacket.getEntity(client.world);
                PlayerEntity player;
                if (entity instanceof PlayerEntity
                        && (player = (PlayerEntity) entity) != client.player
                        && !DEAD.containsKey(player.getId())) {
                    DEAD.put(player.getId(), clock);
                    if (player.squaredDistanceTo(client.player) <= 100.0) {
                        reported = true;
                    }
                }
            }
        }
    }

    static {
        PacketAppliedEvent.TYPE.listen(LootLatch.class, LootLatch::onPacket);
    }
}
