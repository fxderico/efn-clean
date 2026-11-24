package net.efn.client.util.netherite;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionTypes;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Environment(EnvType.CLIENT)
public final class NetheriteChunkTracker {

    static final int MAXIMUM_CANDIDATES = 512;

    private static final int ANCIENT_STATE_ID =
            Block.getRawIdFromState(Blocks.ANCIENT_DEBRIS.getDefaultState());

    private static final RegistryKey<World> DONUT_NETHER =
            RegistryKey.of(RegistryKeys.WORLD, Identifier.of("worlds", "smp_nether"));

    private static final Set<NetheriteChunkTracker.Candidate> CANDIDATES = new LinkedHashSet<>();

    private static WeakReference<ClientWorld> lastLevel = new WeakReference<>(null);

    private static RegistryKey<World> lastDimension;

    private static NetheriteChunkTracker.CandidateListener listener;

    private static NetheriteChunkTracker.RevealListener revealListener;

    private NetheriteChunkTracker() {}

    public static void onPacketApplied(Packet<?> packet) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world != null && isNether(world)) {
            if (packet instanceof ChunkDataS2CPacket chunkDataPacket) {
                trackLevel(world);
                for (int i :
                        HiddenDebrisScanner.findHiddenSections(
                                chunkDataPacket.getChunkData(),
                                world.countVerticalSections(),
                                world.getBottomSectionCoord(),
                                ANCIENT_STATE_ID)) {
                    add(
                            new NetheriteChunkTracker.Candidate(
                                    chunkDataPacket.getChunkX(), chunkDataPacket.getChunkZ(), i));
                }
            } else if (packet instanceof BlockUpdateS2CPacket blockUpdatePacket) {
                onBlockUpdate(blockUpdatePacket.getPos(), blockUpdatePacket.getState());
            } else if (packet instanceof ChunkDeltaUpdateS2CPacket chunkDeltaUpdatePacket) {
                chunkDeltaUpdatePacket.visitUpdates(NetheriteChunkTracker::onBlockUpdate);
            }
        }
    }

    private static void onBlockUpdate(BlockPos pos, BlockState state) {
        if (Block.getRawIdFromState(state) == ANCIENT_STATE_ID) {
            if (revealListener != null) {
                revealListener.onRevealed(pos.toImmutable());
            }
            CANDIDATES.remove(
                    new NetheriteChunkTracker.Candidate(
                            pos.getX() >> 4, pos.getZ() >> 4, pos.getY() >> 4));
        }
    }

    private static void add(NetheriteChunkTracker.Candidate candidate) {
        if (!CANDIDATES.contains(candidate)) {
            if (CANDIDATES.size() >= 512) {
                Iterator iterator = CANDIDATES.iterator();
                iterator.next();
                iterator.remove();
            }
            CANDIDATES.add(candidate);
            if (listener != null) {
                listener.onCandidate(candidate);
            }
        }
    }

    private static void trackLevel(ClientWorld world) {
        RegistryKey registryKey = world.getRegistryKey();
        if (world != lastLevel.get() || !registryKey.equals(lastDimension)) {
            CANDIDATES.clear();
            lastLevel = new WeakReference<>(world);
            lastDimension = registryKey;
        }
    }

    public static boolean isNether(ClientWorld world) {
        RegistryKey registryKey = world.getRegistryKey();
        return registryKey == World.NETHER
                || DONUT_NETHER.equals(registryKey)
                || world.getDimensionEntry().matchesKey(DimensionTypes.THE_NETHER);
    }

    public static List<NetheriteChunkTracker.Candidate> snapshot() {
        return MinecraftClient.getInstance().world != lastLevel.get()
                ? List.of()
                : List.copyOf(CANDIDATES);
    }

    public static void setListener(NetheriteChunkTracker.CandidateListener candidateListener) {
        listener = candidateListener;
    }

    public static void setRevealListener(NetheriteChunkTracker.RevealListener revealListener2) {
        NetheriteChunkTracker.revealListener = revealListener2;
    }

    @Environment(EnvType.CLIENT)
    public record Candidate(int chunkX, int chunkZ, int sectionY) {}

    @FunctionalInterface
    @Environment(EnvType.CLIENT)
    public interface CandidateListener {

        void onCandidate(NetheriteChunkTracker.Candidate candidate);
    }

    @FunctionalInterface
    @Environment(EnvType.CLIENT)
    public interface RevealListener {

        void onRevealed(BlockPos pos);
    }
}
