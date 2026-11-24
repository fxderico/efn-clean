package net.efn.client.module.impl.misc;

import com.example.client.mixin.AccessorNetheriteClientLevel;

import net.efn.client.chat.ModuleChat;
import net.efn.client.util.netherite.NetheriteChunkTracker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Environment(EnvType.CLIENT)
final class AdvancedFinder {

    private static final double REACH = 5.0;

    private static final double PROBES_PER_TICK = 18.0;

    private static final double BURST = 600.0;

    private static final int RECOUNT_INTERVAL_TICKS = 10;

    private static final int MAX_REVEALED = 256;

    private final NetheriteFinderModule module;

    private final Map<NetheriteChunkTracker.Candidate, AdvancedFinder.Scan> scans = new HashMap<>();

    private final Set<NetheriteChunkTracker.Candidate> done = new HashSet<>();

    private final Set<BlockPos> revealed = new LinkedHashSet<>();

    private boolean armed;

    private int ticks;

    private double budget;

    private int recountCooldown;

    private static final int MISSING_REFRESH_TICKS = 10;

    private int handWarning;

    private String lastReason = "";

    private ClientWorld level;

    AdvancedFinder(NetheriteFinderModule module2) {
        this.module = module2;
    }

    boolean armed() {
        return this.armed;
    }

    Collection<BlockPos> revealed() {
        return this.revealed;
    }

    boolean isDone(NetheriteChunkTracker.Candidate candidate) {
        return this.done.contains(candidate);
    }

    float progress(NetheriteChunkTracker.Candidate candidate) {
        AdvancedFinder.Scan scan2 = this.armed ? this.scans.get(candidate) : null;
        return scan2 == null ? -1.0F : fraction(scan2);
    }

    private static float fraction(AdvancedFinder.Scan scan2) {
        return scan2.total == 0 ? 1.0F : Math.min(1.0F, (float) scan2.sent / scan2.total);
    }

    void forEachProbe(
            Vec3d vec, double d, int i, float f, AdvancedFinder.ProbeVisitor probeVisitor) {
        if (this.armed) {
            double e = d * d;
            int j = 0;
            for (AdvancedFinder.Scan scan2 : this.scans.values()) {
                if (!(fraction(scan2) >= f)) {
                    int k = scan2.section.chunkX() << 4;
                    int m = scan2.section.sectionY() << 4;
                    int n = scan2.section.chunkZ() << 4;
                    for (int o = 0; o < 4096; o++) {
                        int p = scan2.stamp[o];
                        int q;
                        double d2;
                        int r;
                        double e2;
                        int t;
                        double d3;
                        if (p != 0
                                && !((d3 = (t = k + (o & 15)) + 0.5 - vec.x) * d3
                                                + (e2 = (r = m + (o >> 8)) + 0.5 - vec.y) * e2
                                                + (d2 = (q = n + (o >> 4 & 15)) + 0.5 - vec.z) * d2
                                        > e)) {
                            probeVisitor.visit(t, r, q, this.ticks - p + 1);
                            if (++j >= i) {
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    void forEachMissing(
            Vec3d vec, double d, int i, float f, AdvancedFinder.MissingVisitor missingVisitor) {
        if (this.armed) {
            double e = d * d;
            int j = 0;
            for (AdvancedFinder.Scan scan2 : this.scans.values()) {
                if (!(fraction(scan2) < f)) {
                    this.refreshMissing(scan2);
                    int k = scan2.section.chunkX() << 4;
                    int m = scan2.section.sectionY() << 4;
                    int n = scan2.section.chunkZ() << 4;
                    for (int o = 0; o < scan2.missingCount; o++) {
                        int p = scan2.missing[o];
                        int q;
                        double d2;
                        int r;
                        double e2;
                        int t;
                        double d3;
                        if (!scan2.probed.get(p)
                                && !((d3 = (t = k + (p & 15)) + 0.5 - vec.x) * d3
                                                + (e2 = (r = m + (p >> 8)) + 0.5 - vec.y) * e2
                                                + (d2 = (q = n + (p >> 4 & 15)) + 0.5 - vec.z) * d2
                                        > e)) {
                            missingVisitor.visit(t, r, q);
                            if (++j >= i) {
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private void refreshMissing(AdvancedFinder.Scan scan2) {
        if (this.level != null && this.ticks - scan2.missingAt >= 10) {
            scan2.missingAt = this.ticks;
            int i = scan2.section.chunkX() << 4;
            int j = scan2.section.sectionY() << 4;
            int k = scan2.section.chunkZ() << 4;
            Mutable mutable = new Mutable();
            int m = 0;
            for (int n = scan2.probed.nextClearBit(0);
                    n < 4096;
                    n = scan2.probed.nextClearBit(n + 1)) {
                mutable.set(i + (n & 15), j + (n >> 8), k + (n >> 4 & 15));
                if (worthProbing(this.level, mutable)) {
                    scan2.missing[m++] = n;
                }
            }
            scan2.missingCount = m;
        }
    }

    void arm() {
        this.armed = true;
        this.lastReason = "";
    }

    void disarm(MinecraftClient client) {
        this.armed = false;
        this.lastReason = "";
    }

    void reset(MinecraftClient client) {
        this.disarm(client);
        this.scans.clear();
        this.done.clear();
        this.revealed.clear();
        this.budget = 0.0;
        this.level = null;
    }

    boolean onRevealed(BlockPos pos2) {
        NetheriteChunkTracker.Candidate candidate =
                new NetheriteChunkTracker.Candidate(
                        pos2.getX() >> 4, pos2.getZ() >> 4, pos2.getY() >> 4);
        if (!this.revealed.add(pos2)) {
            return false;
        } else {
            if (this.revealed.size() > 256) {
                Iterator iterator = this.revealed.iterator();
                iterator.next();
                iterator.remove();
            }
            if (this.armed && this.scans.remove(candidate) != null) {
                this.done.add(candidate);
                ModuleChat.send(
                        this.module,
                        ModuleChat.body()
                                .text("Ancient debris ")
                                .good("found")
                                .text(" at ")
                                .coords(pos2.getX(), pos2.getY(), pos2.getZ()));
            }
            return true;
        }
    }

    void maintain(MinecraftClient client) {
        ClientWorld world = client.world;
        if (world == this.level) {
            if (world != null) {
                this.revealed.removeIf(
                        blockPos -> !world.getBlockState(blockPos).isOf(Blocks.ANCIENT_DEBRIS));
            }
        } else {
            this.scans.clear();
            this.done.clear();
            this.revealed.clear();
            this.level = world;
        }
    }

    void tick(MinecraftClient client) {
        if (this.armed) {
            ClientPlayerEntity player = client.player;
            ClientWorld world = client.world;
            if (player != null
                    && world != null
                    && player.networkHandler != null
                    && client.interactionManager != null) {
                this.ticks++;
                this.budget = Math.min(600.0, this.budget + 18.0);
                if (this.handWarning > 0) {
                    this.handWarning--;
                }
                if (!NetheriteChunkTracker.isNether(world)) {
                    this.hold("Not in the Nether");
                } else {
                    this.lastReason = "";
                    if (client.currentScreen == null
                            && !player.isUsingItem()
                            && !client.interactionManager.isBreakingBlock()) {
                        Vec3d eyePos = player.getEyePos();
                        this.refreshScans(world, eyePos);
                        if (!this.scans.isEmpty() && !(this.budget < 1.0)) {
                            List list = this.collect(world, eyePos);
                            if (!list.isEmpty()) {
                                int selectedSlot = player.getInventory().getSelectedSlot();
                                int probeSlot = probeSlot(player, selectedSlot);
                                if (probeSlot < 0) {
                                    if (this.handWarning <= 0) {
                                        this.handWarning = 200;
                                        ModuleChat.send(
                                                this.module,
                                                ModuleChat.body()
                                                        .text(
                                                                "Hotbar needs an empty or non-tool"
                                                                        + " slot"));
                                    }
                                } else {
                                    if (probeSlot != selectedSlot) {
                                        player.networkHandler.sendPacket(
                                                new UpdateSelectedSlotC2SPacket(probeSlot));
                                    }
                                    BlockPos pos2 = null;
                                    for (AdvancedFinder.Probe probe :
                                            (Iterable<AdvancedFinder.Probe>) list) {
                                        if (this.budget < 1.0) {
                                            break;
                                        }
                                        sendAction(
                                                player,
                                                world,
                                                Action.START_DESTROY_BLOCK,
                                                probe.pos());
                                        probe.scan().probed.set(probe.index());
                                        probe.scan().sent++;
                                        probe.scan().stamp[probe.index()] = this.ticks;
                                        probe.scan().dirty = true;
                                        this.budget--;
                                        pos2 = probe.pos();
                                    }
                                    if (pos2 != null) {
                                        sendAction(player, world, Action.ABORT_DESTROY_BLOCK, pos2);
                                    }
                                    if (probeSlot != selectedSlot) {
                                        player.networkHandler.sendPacket(
                                                new UpdateSelectedSlotC2SPacket(selectedSlot));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void refreshScans(ClientWorld world, Vec3d vec) {
        HashSet set = new HashSet<>(NetheriteChunkTracker.snapshot());
        this.scans.keySet().retainAll(set);
        for (NetheriteChunkTracker.Candidate candidate :
                (Iterable<NetheriteChunkTracker.Candidate>) set) {
            if (!this.done.contains(candidate)
                    && !this.scans.containsKey(candidate)
                    && distanceToSectionSq(vec, candidate) <= 25.0) {
                this.scans.put(
                        candidate,
                        new AdvancedFinder.Scan(candidate, countProbeable(world, candidate)));
            }
        }
        if (this.recountCooldown-- <= 0) {
            this.recountCooldown = 10;
            Iterator iterator = this.scans.values().iterator();
            while (iterator.hasNext()) {
                AdvancedFinder.Scan scan2 = (AdvancedFinder.Scan) iterator.next();
                if (scan2.dirty) {
                    scan2.dirty = false;
                    if (!hasUnprobed(world, scan2)) {
                        iterator.remove();
                        this.done.add(scan2.section);
                        NetheriteChunkTracker.Candidate section2 = scan2.section;
                        ModuleChat.send(
                                this.module,
                                ModuleChat.body()
                                        .text("Section ")
                                        .good("clean")
                                        .text(" at ")
                                        .coords(
                                                section2.chunkX() * 16 + 8,
                                                section2.sectionY() * 16 + 8,
                                                section2.chunkZ() * 16 + 8));
                    }
                }
            }
        }
    }

    private List<AdvancedFinder.Probe> collect(ClientWorld world, Vec3d vec) {
        ArrayList list = new ArrayList();
        ArrayList list2 = new ArrayList();
        int i = (int) Math.ceil(5.0);
        int j = (int) Math.floor(vec.x);
        int k = (int) Math.floor(vec.y);
        int m = (int) Math.floor(vec.z);
        Mutable mutable = new Mutable();
        for (AdvancedFinder.Scan scan2 : this.scans.values()) {
            int n = scan2.section.chunkX() << 4;
            int o = scan2.section.sectionY() << 4;
            int p = scan2.section.chunkZ() << 4;
            int q = Math.max(n, j - i);
            int r = Math.min(n + 15, j + i);
            int t = Math.max(o, k - i);
            int u = Math.min(o + 15, k + i);
            int v = Math.max(p, m - i);
            int w = Math.min(p + 15, m + i);
            for (int i2 = q; i2 <= r; i2++) {
                for (int j2 = t; j2 <= u; j2++) {
                    for (int k2 = v; k2 <= w; k2++) {
                        int m2 = j2 & 15;
                        int n2 = k2 & 15;
                        int o2 = i2 & 15;
                        int p2 = m2 << 8 | n2 << 4 | o2;
                        if (!scan2.probed.get(p2)) {
                            mutable.set(i2, j2, k2);
                            double distanceToBlockSq = distanceToBlockSq(vec, mutable);
                            if (!(distanceToBlockSq > 25.0)) {
                                if (!worthProbing(world, mutable)) {
                                    scan2.probed.set(p2);
                                } else {
                                    AdvancedFinder.Probe probe =
                                            new AdvancedFinder.Probe(
                                                    scan2,
                                                    p2,
                                                    mutable.toImmutable(),
                                                    distanceToBlockSq);
                                    if (o2 % 3 == 1 && m2 % 3 == 1 && n2 % 3 == 1) {
                                        list.add(probe);
                                    } else {
                                        list2.add(probe);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        list.sort(Comparator.comparingDouble(AdvancedFinder.Probe::distanceSq));
        list2.sort(Comparator.comparingDouble(AdvancedFinder.Probe::distanceSq));
        list.addAll(list2);
        return list;
    }

    private static int countProbeable(
            ClientWorld world, NetheriteChunkTracker.Candidate candidate) {
        int i = candidate.chunkX() << 4;
        int j = candidate.sectionY() << 4;
        int k = candidate.chunkZ() << 4;
        Mutable mutable = new Mutable();
        int m = 0;
        for (int n = 0; n < 4096; n++) {
            mutable.set(i + (n & 15), j + (n >> 8), k + (n >> 4 & 15));
            if (worthProbing(world, mutable)) {
                m++;
            }
        }
        return m;
    }

    private static boolean hasUnprobed(ClientWorld world, AdvancedFinder.Scan scan2) {
        int i = scan2.section.chunkX() << 4;
        int j = scan2.section.sectionY() << 4;
        int k = scan2.section.chunkZ() << 4;
        Mutable mutable = new Mutable();
        for (int m = scan2.probed.nextClearBit(0); m < 4096; m = scan2.probed.nextClearBit(m + 1)) {
            mutable.set(i + (m & 15), j + (m >> 8), k + (m >> 4 & 15));
            if (worthProbing(world, mutable)) {
                return true;
            }
        }
        return false;
    }

    private static boolean worthProbing(ClientWorld world, BlockPos pos2) {
        BlockState blockState = world.getBlockState(pos2);
        return !blockState.isAir()
                        && blockState.getFluidState().isEmpty()
                        && !blockState.isOf(Blocks.ANCIENT_DEBRIS)
                ? blockState.isOpaqueFullCube() && blockState.getHardness(world, pos2) > 0.0F
                : false;
    }

    private void hold(String lastReason2) {
        if (!lastReason2.equals(this.lastReason)) {
            this.lastReason = lastReason2;
            ModuleChat.send(this.module, ModuleChat.body().text(lastReason2));
        }
    }

    private static void sendAction(
            ClientPlayerEntity player, ClientWorld world, Action action, BlockPos pos2) {
        PendingUpdateManager pendingUpdateManager =
                ((AccessorNetheriteClientLevel) world).efn$blockStatePredictionHandler();
        try (PendingUpdateManager pendingUpdateManager2 =
                pendingUpdateManager.incrementSequence()) {
            player.networkHandler.sendPacket(
                    new PlayerActionC2SPacket(
                            action, pos2, Direction.UP, pendingUpdateManager2.getSequence()));
        }
    }

    private static double distanceToSectionSq(
            Vec3d vec, NetheriteChunkTracker.Candidate candidate) {
        double d = candidate.chunkX() * 16.0;
        double e = candidate.sectionY() * 16.0;
        double d2 = candidate.chunkZ() * 16.0;
        double e2 = Math.max(0.0, Math.max(d - vec.x, vec.x - (d + 16.0)));
        double d3 = Math.max(0.0, Math.max(e - vec.y, vec.y - (e + 16.0)));
        double e3 = Math.max(0.0, Math.max(d2 - vec.z, vec.z - (d2 + 16.0)));
        return e2 * e2 + d3 * d3 + e3 * e3;
    }

    private static double distanceToBlockSq(Vec3d vec, BlockPos pos2) {
        double d = Math.max(0.0, Math.max(pos2.getX() - vec.x, vec.x - (pos2.getX() + 1.0)));
        double e = Math.max(0.0, Math.max(pos2.getY() - vec.y, vec.y - (pos2.getY() + 1.0)));
        double d2 = Math.max(0.0, Math.max(pos2.getZ() - vec.z, vec.z - (pos2.getZ() + 1.0)));
        return d * d + e * e + d2 * d2;
    }

    private static int probeSlot(ClientPlayerEntity player, int i) {
        if (!isTool(player.getInventory().getStack(i))) {
            return i;
        } else {
            for (int j = 0; j < 9; j++) {
                if (player.getInventory().getStack(j).isEmpty()) {
                    return j;
                }
            }
            for (int k = 0; k < 9; k++) {
                if (!isTool(player.getInventory().getStack(k))) {
                    return k;
                }
            }
            return -1;
        }
    }

    private static boolean isTool(ItemStack stack) {
        return stack.contains(DataComponentTypes.TOOL);
    }

    @FunctionalInterface
    @Environment(EnvType.CLIENT)
    interface MissingVisitor {

        void visit(int i, int j, int k);
    }

    @Environment(EnvType.CLIENT)
    private record Probe(AdvancedFinder.Scan scan, int index, BlockPos pos, double distanceSq) {}

    @FunctionalInterface
    @Environment(EnvType.CLIENT)
    interface ProbeVisitor {

        void visit(int i, int j, int k, int m);
    }

    @Environment(EnvType.CLIENT)
    private static final class Scan {

        final NetheriteChunkTracker.Candidate section;

        final BitSet probed = new BitSet(4096);

        final int[] stamp = new int[4096];

        final int total;

        final int[] missing = new int[4096];

        int missingCount;

        int missingAt = -1000;

        int sent;

        boolean dirty;

        Scan(NetheriteChunkTracker.Candidate section2, int total2) {
            this.section = section2;
            this.total = total2;
        }
    }
}
