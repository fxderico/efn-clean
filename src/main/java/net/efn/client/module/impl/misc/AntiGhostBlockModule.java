package net.efn.client.module.impl.misc;

import com.example.client.mixin.AccessorNetheriteClientLevel;

import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class AntiGhostBlockModule extends ClientModule {

    private static final int MAX_PENDING = 256;

    private static AntiGhostBlockModule instance;

    private final Map<Long, AntiGhostBlockModule.Pending> pending = new LinkedHashMap<>();

    private ClientWorld world;

    public AntiGhostBlockModule() {
        super(
                "anti-ghost-block",
                "Anti Ghost Block",
                "Stops invisible ghost blocks when mining: a broken block stays visible until the"
                        + " server confirms the break.",
                ModuleCategory.MISC);
        instance = this;
    }

    @Override
    protected void onEnable() {
        this.pending.clear();
        this.world = MinecraftClient.getInstance().world;
    }

    @Override
    protected void onDisable() {
        this.pending.clear();
        this.world = null;
    }

    private static AntiGhostBlockModule active(ClientWorld world2) {
        AntiGhostBlockModule module = instance;
        if (module != null && module.isEnabled()) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (module.world != client.world) {
                module.pending.clear();
                module.world = client.world;
            }
            return world2 != null
                            && world2 == client.world
                            && client.player != null
                            && client.getNetworkHandler() != null
                            && !client.isInSingleplayer()
                    ? module
                    : null;
        } else {
            return null;
        }
    }

    private static long timeoutNanos(MinecraftClient client) {
        PlayerListEntry listEntry =
                client.getNetworkHandler() != null && client.player != null
                        ? client.getNetworkHandler().getPlayerListEntry(client.player.getUuid())
                        : null;
        long l = listEntry == null ? 0L : Math.max(0, listEntry.getLatency());
        return Math.max(2000L, l * 3L + 500L) * 1000000L;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        AntiGhostBlockModule module = active(client.world);
        if (module != null) {
            long l = System.nanoTime();
            long timeoutNanos = timeoutNanos(client);
            module.pending.values().removeIf(pending -> l - pending.since >= timeoutNanos);
        }
    }

    public static boolean holdBreak(BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        AntiGhostBlockModule module = active(client.world);
        if (module != null && !client.world.getBlockState(pos).isAir()) {
            int sequence2 =
                    ((AccessorNetheriteClientLevel) client.world)
                            .efn$blockStatePredictionHandler()
                            .getSequence();
            long l = pos.asLong();
            if (!module.pending.containsKey(l)) {
                if (module.pending.size() >= 256) {
                    Iterator iterator = module.pending.keySet().iterator();
                    iterator.next();
                    iterator.remove();
                }
                module.pending.put(
                        l, new AntiGhostBlockModule.Pending(sequence2, System.nanoTime()));
            }
            return true;
        } else {
            return false;
        }
    }

    public static boolean waiting(BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        AntiGhostBlockModule module = active(client.world);
        if (module == null) {
            return false;
        } else {
            AntiGhostBlockModule.Pending pending2 = module.pending.get(pos.asLong());
            if (pending2 == null) {
                return false;
            } else if (System.nanoTime() - pending2.since < timeoutNanos(client)) {
                return true;
            } else {
                module.pending.remove(pos.asLong());
                return false;
            }
        }
    }

    public static boolean serverAnswered(ClientWorld world2, BlockPos pos) {
        AntiGhostBlockModule module = active(world2);
        return module != null && module.pending.remove(pos.asLong()) != null;
    }

    public static void acknowledged(ClientWorld world2, int i) {
        AntiGhostBlockModule module = active(world2);
        if (module != null) {
            module.pending.values().removeIf(pending -> pending.sequence <= i);
        }
    }

    @Environment(EnvType.CLIENT)
    private record Pending(int sequence, long since) {}
}
