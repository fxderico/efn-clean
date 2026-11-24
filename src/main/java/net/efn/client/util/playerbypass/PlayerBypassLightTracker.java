package net.efn.client.util.playerbypass;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.LightData;
import net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.ChunkPos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

@Environment(EnvType.CLIENT)
public final class PlayerBypassLightTracker {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final Long2ObjectOpenHashMap<ChunkLightSnapshot> SNAPSHOTS =
            new Long2ObjectOpenHashMap<>();

    private static final LongOpenHashSet ALERTED = new LongOpenHashSet();

    private static final LongOpenHashSet SUSPECT = new LongOpenHashSet();

    static final int SUSPECT_TURNED_DARK = 1;

    static final int SUSPECT_AIRLESS = 2;

    private static final Long2IntOpenHashMap SENT_NOT_DARK = new Long2IntOpenHashMap();

    private static final int MAX_REMEMBERED_CHUNKS = 250000;

    private static String sentPlace;

    private static final Long2ObjectOpenHashMap<ChunkLightSnapshot> HELD =
            new Long2ObjectOpenHashMap<>();

    private static final ChunkEntryGuard ENTRY_GUARD = new ChunkEntryGuard();

    private static final ProtectedChunkStore PROTECTED_CHUNKS = ProtectedChunkStore.get();

    private static ClientWorld lastLevel;

    private static PlayerBypassLightTracker.SpottedListener spottedListener;

    private static long visibilityRevision;

    private PlayerBypassLightTracker() {}

    public static void onTick() {
        MinecraftClient client = MinecraftClient.getInstance();
        syncLevel(client);
        ClientPlayerEntity player = client.player;
        if (lastLevel != null && player != null) {
            ENTRY_GUARD.update(
                    player.getChunkPos().toLong(),
                    PlayerBypassLightTracker::isLoaded,
                    PlayerBypassLightTracker::isFlaggedOrHeld,
                    PROTECTED_CHUNKS);
        }
        PROTECTED_CHUNKS.tickAutosave();
    }

    public static void onPacketApplied(Packet<?> packet) {
        LightData lightData;
        int chunkZ;
        int chunkX;
        if (packet instanceof ChunkDataS2CPacket chunkDataPacket) {
            chunkX = chunkDataPacket.getChunkX();
            chunkZ = chunkDataPacket.getChunkZ();
            lightData = chunkDataPacket.getLightData();
        } else {
            if (!(packet instanceof LightUpdateS2CPacket lightUpdatePacket)) {
                return;
            }
            chunkX = lightUpdatePacket.getChunkX();
            chunkZ = lightUpdatePacket.getChunkZ();
            lightData = lightUpdatePacket.getData();
        }
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        syncLevel(client);
        if (world != null && lightData != null) {
            if (client.player != null && client.player.getY() < 0.0) {
                hold(
                        chunkX,
                        chunkZ,
                        lightData,
                        world.countVerticalSections(),
                        world.getBottomSectionCoord());
            } else {
                ingest(
                        chunkX,
                        chunkZ,
                        lightData,
                        world.countVerticalSections(),
                        world.getBottomSectionCoord());
            }
            rememberNotDark(
                    chunkX,
                    chunkZ,
                    lightData,
                    world.countVerticalSections(),
                    world.getBottomSectionCoord());
            visibilityRevision++;
        }
    }

    private static void rememberNotDark(int i, int j, LightData lightData, int k, int m) {
        int sentNotDark = LightDataClassifier.sentNotDark(lightData, k, m);
        if (sentNotDark != 0) {
            if (SENT_NOT_DARK.size() >= 250000) {
                SENT_NOT_DARK.clear();
            }
            long l = ChunkPos.toLong(i, j);
            SENT_NOT_DARK.put(l, SENT_NOT_DARK.get(l) | sentNotDark);
        }
    }

    private static void syncLevel(MinecraftClient client) {
        ClientWorld world = client.world;
        if (world != lastLevel) {
            visibilityRevision++;
            SNAPSHOTS.clear();
            ALERTED.clear();
            SUSPECT.clear();
            HELD.clear();
            ENTRY_GUARD.reset();
            lastLevel = world;
            PROTECTED_CHUNKS.flush();
            if (world == null) {
                PROTECTED_CHUNKS.select(null, null);
            } else {
                String worldKey = worldKey(client);
                String string;
                String string2 =
                        worldKey + "\n" + (string = world.getRegistryKey().getValue().toString());
                if (!string2.equals(sentPlace)) {
                    SENT_NOT_DARK.clear();
                    sentPlace = string2;
                }
                PROTECTED_CHUNKS.select(worldKey, string);
            }
        }
    }

    private static String worldKey(MinecraftClient client) {
        IntegratedServer server = client.getServer();
        if (server != null) {
            Path fileName = server.getSavePath(WorldSavePath.ROOT).normalize().getFileName();
            return ProtectedChunkStore.singleplayerKey(
                    fileName == null ? "world" : fileName.toString());
        } else {
            ServerInfo currentServerEntry = client.getCurrentServerEntry();
            return ProtectedChunkStore.serverKey(
                    currentServerEntry == null ? null : currentServerEntry.address);
        }
    }

    private static void ingest(int i, int j, LightData lightData, int k, int m) {
        long l = ChunkPos.toLong(i, j);
        if (HELD.containsKey(l)) {
            hold(i, j, lightData, k, m);
        }
        ChunkLightSnapshot chunkLightSnapshot;
        boolean bl =
                (chunkLightSnapshot = SNAPSHOTS.get(l)) != null && isFlagged(l, chunkLightSnapshot);
        if (chunkLightSnapshot == null
                || chunkLightSnapshot.sectionCount != k
                || chunkLightSnapshot.bottomSectionY != m) {
            chunkLightSnapshot = new ChunkLightSnapshot(k, m);
            SNAPSHOTS.put(l, chunkLightSnapshot);
        }
        try {
            LightDataClassifier.apply(lightData, chunkLightSnapshot);
        } catch (Throwable exception) {
            LOGGER.error("Player Bypass light ingest failed for chunk {}, {}", i, j, exception);
            return;
        }
        boolean baseSignal = chunkLightSnapshot.hasBaseSignal();
        if (baseSignal && !PROTECTED_CHUNKS.flagAllowed(l, bl)) {
            SNAPSHOTS.remove(l);
        } else {
            int n = baseSignal ? suspicion(l, i, j, chunkLightSnapshot) : 0;
            if (baseSignal && n != 0) {
                SUSPECT.add(l);
            } else if (baseSignal) {
                SUSPECT.remove(l);
                if (spottedListener != null && ALERTED.add(l)) {
                    spottedListener.onChunkSpotted(i, j);
                }
            } else if (!ALERTED.contains(l)) {
                SNAPSHOTS.remove(l);
                SUSPECT.remove(l);
            }
        }
    }

    private static int suspicion(long l, int i, int j, ChunkLightSnapshot chunkLightSnapshot) {
        int k = SENT_NOT_DARK.get(l);
        int m = 0;
        for (int n = 1; n < chunkLightSnapshot.sectionCount + 1; n++) {
            if (ChunkLightSnapshot.candidate(n, chunkLightSnapshot.bottomSectionY)
                    && chunkLightSnapshot.sky[n] == LightSectionStatus.ZEROED_PRESENT) {
                if (n < 32 && (k >>> n & 1) != 0) {
                    m |= 1;
                } else {
                    if (lastLevel == null
                            || !DarkSectionCheck.airlessAround(
                                    lastLevel, i, chunkLightSnapshot.bottomSectionY + n - 1, j)) {
                        return 0;
                    }
                    m |= 2;
                }
            }
        }
        return m;
    }

    private static void hold(int i, int j, LightData lightData, int k, int m) {
        long l = ChunkPos.toLong(i, j);
        ChunkLightSnapshot chunkLightSnapshot = HELD.get(l);
        if (chunkLightSnapshot == null
                || chunkLightSnapshot.sectionCount != k
                || chunkLightSnapshot.bottomSectionY != m) {
            chunkLightSnapshot = new ChunkLightSnapshot(k, m);
        }
        try {
            LightDataClassifier.apply(lightData, chunkLightSnapshot);
        } catch (Throwable exception) {
            LOGGER.error("Player Bypass light hold failed for chunk {}, {}", i, j, exception);
            return;
        }
        if (chunkLightSnapshot.hasBaseSignal() && PROTECTED_CHUNKS.flagAllowed(l, false)) {
            HELD.put(l, chunkLightSnapshot);
        } else {
            HELD.remove(l);
        }
    }

    public static void setSpottedListener(
            PlayerBypassLightTracker.SpottedListener spottedListener2) {
        PlayerBypassLightTracker.spottedListener = spottedListener2;
    }

    public static Long2ObjectMap<ChunkLightSnapshot> snapshots() {
        return SNAPSHOTS;
    }

    public static long visibilityRevision() {
        return visibilityRevision;
    }

    public static boolean isSuspect(long l) {
        return SUSPECT.contains(l);
    }

    public static boolean isFlagged(long l, ChunkLightSnapshot chunkLightSnapshot) {
        return ALERTED.contains(l) || chunkLightSnapshot.hasBaseSignal();
    }

    public static boolean isFlagged(long l) {
        ChunkLightSnapshot chunkLightSnapshot = SNAPSHOTS.get(l);
        return chunkLightSnapshot != null && isFlagged(l, chunkLightSnapshot);
    }

    private static boolean isFlaggedOrHeld(long l) {
        return isFlagged(l) || HELD.containsKey(l);
    }

    private static boolean isLoaded(long l) {
        return lastLevel != null
                && lastLevel
                        .getChunkManager()
                        .isChunkLoaded(ChunkPos.getPackedX(l), ChunkPos.getPackedZ(l));
    }

    @FunctionalInterface
    @Environment(EnvType.CLIENT)
    public interface SpottedListener {

        void onChunkSpotted(int i, int j);
    }
}
