package net.efn.client.util.seedscan;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public final class BlobJudge {

    public static final int JUDGED_MIN_Y = 0;

    public static final int JUDGED_MAX_Y = 30;

    public static final double MIN_MATCH = 0.6;

    public static final int MIN_JUDGED = 8;

    public static final int MAX_FILLED = 3;

    public static final int MAX_CLUSTER = 20000;

    public static final int SHELL_MODEL_CAVE = 16;

    public static final int SHELL_JUDGED = 32;

    public static final int SHELL_STONE = 64;

    private static final Set<Block> NATURAL =
            set(
                    Blocks.CALCITE,
                    Blocks.SMOOTH_BASALT,
                    Blocks.AMETHYST_BLOCK,
                    Blocks.BUDDING_AMETHYST,
                    Blocks.DRIPSTONE_BLOCK,
                    Blocks.POINTED_DRIPSTONE,
                    Blocks.MOSS_BLOCK,
                    Blocks.MOSS_CARPET,
                    Blocks.ROOTED_DIRT,
                    Blocks.CLAY,
                    Blocks.SCULK,
                    Blocks.SCULK_VEIN,
                    Blocks.SCULK_CATALYST,
                    Blocks.SCULK_SENSOR,
                    Blocks.SCULK_SHRIEKER,
                    Blocks.MAGMA_BLOCK,
                    Blocks.NETHERRACK,
                    Blocks.CRYING_OBSIDIAN);

    private static final Set<Block> FAKES =
            set(
                    Blocks.COAL_ORE,
                    Blocks.DEEPSLATE_COAL_ORE,
                    Blocks.IRON_ORE,
                    Blocks.DEEPSLATE_IRON_ORE,
                    Blocks.COPPER_ORE,
                    Blocks.DEEPSLATE_COPPER_ORE,
                    Blocks.GOLD_ORE,
                    Blocks.DEEPSLATE_GOLD_ORE,
                    Blocks.REDSTONE_ORE,
                    Blocks.DEEPSLATE_REDSTONE_ORE,
                    Blocks.LAPIS_ORE,
                    Blocks.DEEPSLATE_LAPIS_ORE,
                    Blocks.DIAMOND_ORE,
                    Blocks.DEEPSLATE_DIAMOND_ORE,
                    Blocks.EMERALD_ORE,
                    Blocks.DEEPSLATE_EMERALD_ORE,
                    Blocks.RAW_IRON_BLOCK,
                    Blocks.RAW_COPPER_BLOCK,
                    Blocks.INFESTED_STONE,
                    Blocks.INFESTED_DEEPSLATE,
                    Blocks.MOSSY_COBBLESTONE,
                    Blocks.OBSIDIAN,
                    Blocks.CHEST,
                    Blocks.ENDER_CHEST);

    private static final Map<Block, Boolean> STRUCTURE = new ConcurrentHashMap<>();

    private BlobJudge() {}

    public static List<BlobJudge.Cluster> clusters(int i, int j, BlobJudge.Vanilla vanilla2) {
        ArrayList list = new ArrayList();
        VanillaBand vanillaBand = vanilla2.band(i, j);
        if (vanillaBand == null) {
            return list;
        } else {
            BlobJudge.Bands bands = new BlobJudge.Bands(vanilla2);
            for (byte b : new byte[] {4, 5, 6, 8}) {
                int familyBit = VBlock.familyBit(b);
                LongOpenHashSet longOpenHashSet = new LongOpenHashSet();
                for (int m = 0; m <= 30; m++) {
                    for (int n = j << 4; n < (j << 4) + 16; n++) {
                        for (int o = i << 4; o < (i << 4) + 16; o++) {
                            BlobJudge.Cluster cluster;
                            long l;
                            if ((vanillaBand.families(o, m, n) & familyBit) != 0
                                    && !longOpenHashSet.contains(l = BlockPos.asLong(o, m, n))
                                    && BlockPos.unpackLongX(
                                                            (cluster =
                                                                            cluster(
                                                                                    l,
                                                                                    b,
                                                                                    bands,
                                                                                    longOpenHashSet))
                                                                    .owner())
                                                    >> 4
                                            == i
                                    && BlockPos.unpackLongZ(cluster.owner()) >> 4 == j) {
                                list.add(cluster);
                            }
                        }
                    }
                }
            }
            return list;
        }
    }

    private static BlobJudge.Cluster cluster(
            long l, byte b, BlobJudge.Bands bands, LongOpenHashSet longOpenHashSet) {
        int familyBit = VBlock.familyBit(b);
        LongArrayList longArrayList = new LongArrayList();
        LongOpenHashSet longOpenHashSet2 = new LongOpenHashSet();
        LongArrayFIFOQueue longArrayFIFOQueue = new LongArrayFIFOQueue();
        longArrayFIFOQueue.enqueue(l);
        longOpenHashSet2.add(l);
        boolean bl = false;
        boolean bl2 = false;
        boolean bl3 = false;
        long l2 = l;
        while (!longArrayFIFOQueue.isEmpty()) {
            long dequeueLong = longArrayFIFOQueue.dequeueLong();
            longArrayList.add(dequeueLong);
            int unpackLongY = BlockPos.unpackLongY(dequeueLong);
            if (unpackLongY >= 0 && unpackLongY <= 30) {
                if (!bl3 || before(dequeueLong, l2)) {
                    l2 = dequeueLong;
                }
                bl3 = true;
            }
            if (longArrayList.size() > 20000) {
                bl2 = true;
                break;
            }
            int unpackLongX = BlockPos.unpackLongX(dequeueLong);
            int unpackLongY2 = BlockPos.unpackLongY(dequeueLong);
            int unpackLongZ = BlockPos.unpackLongZ(dequeueLong);
            for (int o = -1; o <= 1; o++) {
                for (int p = -1; p <= 1; p++) {
                    for (int q = -1; q <= 1; q++) {
                        long l3;
                        int i;
                        int j;
                        int k;
                        if ((o != 0 || p != 0 || q != 0)
                                && !longOpenHashSet2.contains(
                                        l3 =
                                                BlockPos.asLong(
                                                        k = unpackLongX + o,
                                                        j = unpackLongY2 + p,
                                                        i = unpackLongZ + q))) {
                            if (!VanillaBand.inBand(j)) {
                                bl2 = true;
                            } else {
                                VanillaBand vanillaBand = bands.at(k, i);
                                if (vanillaBand == null) {
                                    bl = true;
                                } else if ((vanillaBand.families(k, j, i) & familyBit) != 0) {
                                    longOpenHashSet2.add(l3);
                                    longArrayFIFOQueue.enqueue(l3);
                                }
                            }
                        }
                    }
                }
            }
        }
        longOpenHashSet.addAll(longOpenHashSet2);
        long[] longs = new long[0];
        if (bl2) {
            return new BlobJudge.Cluster(
                    b, l2, BlobJudge.Status.OUT_OF_BAND, longs, longs, longs, new byte[0]);
        } else if (bl) {
            return new BlobJudge.Cluster(
                    b, l2, BlobJudge.Status.INCOMPLETE, longs, longs, longs, new byte[0]);
        } else {
            LongArrayList longArrayList2 = new LongArrayList();
            LongOpenHashSet longOpenHashSet3 = new LongOpenHashSet();
            for (int v = 0; v < longArrayList.size(); v++) {
                long l4 = longArrayList.getLong(v);
                int unpackLongX2 = BlockPos.unpackLongX(l4);
                int unpackLongY3 = BlockPos.unpackLongY(l4);
                int unpackLongZ2 = BlockPos.unpackLongZ(l4);
                if (unpackLongY3 >= 0
                        && unpackLongY3 <= 30
                        && bands.at(unpackLongX2, unpackLongZ2)
                                        .certain(unpackLongX2, unpackLongY3, unpackLongZ2)
                                == b) {
                    longArrayList2.add(l4);
                }
                for (int k2 = -1; k2 <= 1; k2++) {
                    for (int m2 = -1; m2 <= 1; m2++) {
                        for (int n2 = -1; n2 <= 1; n2++) {
                            long l5 =
                                    BlockPos.asLong(
                                            unpackLongX2 + k2,
                                            unpackLongY3 + m2,
                                            unpackLongZ2 + n2);
                            if (!longOpenHashSet2.contains(l5)) {
                                longOpenHashSet3.add(l5);
                            }
                        }
                    }
                }
            }
            long[] longArray = longOpenHashSet3.toLongArray();
            ByteArrayList byteArrayList = new ByteArrayList(longArray.length);
            for (long l6 : longArray) {
                int unpackLongX3 = BlockPos.unpackLongX(l6);
                int unpackLongY4 = BlockPos.unpackLongY(l6);
                int unpackLongZ3 = BlockPos.unpackLongZ(l6);
                int m = 0;
                VanillaBand vanillaBand2 = bands.at(unpackLongX3, unpackLongZ3);
                if (vanillaBand2 != null && VanillaBand.inBand(unpackLongY4)) {
                    m = vanillaBand2.families(unpackLongX3, unpackLongY4, unpackLongZ3);
                    byte certain = vanillaBand2.certain(unpackLongX3, unpackLongY4, unpackLongZ3);
                    if (VBlock.isOpen(certain)) {
                        m |= 16;
                    }
                    if (certain == 3) {
                        m |= 64;
                    }
                    if (unpackLongY4 >= 0 && unpackLongY4 <= 30) {
                        m |= 32;
                    }
                }
                byteArrayList.add((byte) m);
            }
            return new BlobJudge.Cluster(
                    b,
                    l2,
                    null,
                    longArrayList.toLongArray(),
                    longArrayList2.toLongArray(),
                    longArray,
                    byteArrayList.toByteArray());
        }
    }

    public static BlobJudge.Verdict judge(
            BlobJudge.Cluster cluster, BlobJudge.Client client, int i) {
        byte kind2 = cluster.kind();
        long owner2 = cluster.owner();
        if (cluster.unfinished() != null) {
            return verdict(cluster.unfinished(), kind2, owner2);
        } else {
            boolean bl = false;
            for (long cell : cluster.cells()) {
                BlockState state =
                        client.state(
                                BlockPos.unpackLongX(cell),
                                BlockPos.unpackLongY(cell),
                                BlockPos.unpackLongZ(cell));
                if (state == null) {
                    return verdict(BlobJudge.Status.INCOMPLETE, kind2, owner2);
                }
                if (open(state)) {
                    return verdict(BlobJudge.Status.OPEN, kind2, owner2);
                }
                bl |= isStructure(state.getBlock());
            }
            long[] longs = cluster.shell();
            byte[] bytes = cluster.shellFlags();
            int j = 0;
            for (int k = 0; k < longs.length; k++) {
                BlockState state2 =
                        client.state(
                                BlockPos.unpackLongX(longs[k]),
                                BlockPos.unpackLongY(longs[k]),
                                BlockPos.unpackLongZ(longs[k]));
                if (state2 == null) {
                    return verdict(BlobJudge.Status.INCOMPLETE, kind2, owner2);
                }
                if (open(state2)) {
                    return verdict(BlobJudge.Status.OPEN, kind2, owner2);
                }
                bl |= isStructure(state2.getBlock());
                if ((bytes[k] & 16) != 0) {
                    j++;
                }
            }
            if (bl) {
                return verdict(BlobJudge.Status.STRUCTURE, kind2, owner2);
            } else if (j > 3) {
                return verdict(BlobJudge.Status.TERRAIN, kind2, owner2);
            } else {
                Block block = VBlock.familyBlock(kind2);
                int m = 0;
                int n = 0;
                int o = 0;
                LongArrayList longArrayList = new LongArrayList();
                LongArrayList longArrayList2 = new LongArrayList();
                for (long l : cluster.judged()) {
                    Block block2 =
                            client.state(
                                            BlockPos.unpackLongX(l),
                                            BlockPos.unpackLongY(l),
                                            BlockPos.unpackLongZ(l))
                                    .getBlock();
                    if (block2 == block) {
                        m++;
                        n++;
                    } else if (FAKES.contains(block2)) {
                        o++;
                    } else if (!NATURAL.contains(block2)) {
                        m++;
                        longArrayList.add(l);
                    }
                }
                for (int p = 0; p < longs.length; p++) {
                    if ((bytes[p] & 32) != 0 && (bytes[p] & 16) == 0) {
                        int unpackLongX = BlockPos.unpackLongX(longs[p]);
                        int unpackLongZ;
                        int unpackLongY;
                        BlockState state3 =
                                client.state(
                                        unpackLongX,
                                        unpackLongY = BlockPos.unpackLongY(longs[p]),
                                        unpackLongZ = BlockPos.unpackLongZ(longs[p]));
                        byte b = VBlock.of(state3);
                        int familyBit = VBlock.familyBit(b);
                        if (familyBit != 0) {
                            if ((bytes[p] & familyBit) == 0
                                    && (b != 8
                                            || !nearOpen(
                                                    client,
                                                    unpackLongX,
                                                    unpackLongY,
                                                    unpackLongZ))) {
                                longArrayList2.add(longs[p]);
                            }
                        } else if ((bytes[p] & 64) != 0
                                && b != 3
                                && !FAKES.contains(state3.getBlock())
                                && !NATURAL.contains(state3.getBlock())
                                && !nearOpen(client, unpackLongX, unpackLongY, unpackLongZ)) {
                            longArrayList2.add(longs[p]);
                        }
                    }
                }
                if (m + o > 0 && o * 10 > (m + o) * 3) {
                    return verdict(BlobJudge.Status.HIDDEN, kind2, owner2);
                } else if (m < 8) {
                    return verdict(BlobJudge.Status.TOO_FEW, kind2, owner2);
                } else if (n < 0.6 * m) {
                    return verdict(BlobJudge.Status.MISPREDICTED, kind2, owner2);
                } else {
                    long[] longs2 = new long[longArrayList.size() + longArrayList2.size()];
                    for (int v = 0; v < longArrayList.size(); v++) {
                        longs2[v] = longArrayList.getLong(v);
                    }
                    for (int w = 0; w < longArrayList2.size(); w++) {
                        longs2[longArrayList.size() + w] = longArrayList2.getLong(w);
                    }
                    BlobJudge.Status status2 =
                            longs2.length >= Math.max(1, i)
                                    ? BlobJudge.Status.ALERT
                                    : BlobJudge.Status.CLEAN;
                    return new BlobJudge.Verdict(status2, kind2, owner2, longs2);
                }
            }
        }
    }

    private static BlobJudge.Verdict verdict(BlobJudge.Status status2, byte b, long l) {
        return new BlobJudge.Verdict(status2, b, l, new long[0]);
    }

    public static boolean open(BlockState state) {
        return state.isAir() || !state.getFluidState().isEmpty();
    }

    private static boolean nearOpen(BlobJudge.Client client, int i, int j, int k) {
        for (int m = -1; m <= 1; m++) {
            for (int n = -1; n <= 1; n++) {
                for (int o = -1; o <= 1; o++) {
                    BlockState state = client.state(i + m, j + n, k + o);
                    if (state == null || open(state)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean before(long l, long l2) {
        if (BlockPos.unpackLongY(l) != BlockPos.unpackLongY(l2)) {
            return BlockPos.unpackLongY(l) < BlockPos.unpackLongY(l2);
        } else {
            return BlockPos.unpackLongZ(l) != BlockPos.unpackLongZ(l2)
                    ? BlockPos.unpackLongZ(l) < BlockPos.unpackLongZ(l2)
                    : BlockPos.unpackLongX(l) < BlockPos.unpackLongX(l2);
        }
    }

    static boolean isStructure(Block block) {
        Boolean flag = STRUCTURE.get(block);
        return flag != null ? flag : STRUCTURE.computeIfAbsent(block, BlobJudge::structureBlock);
    }

    private static boolean structureBlock(Block block) {
        if (block != Blocks.BONE_BLOCK
                && block != Blocks.SPAWNER
                && block != Blocks.TRIAL_SPAWNER
                && block != Blocks.VAULT
                && block != Blocks.END_PORTAL_FRAME
                && block != Blocks.REINFORCED_DEEPSLATE
                && block != Blocks.SEA_LANTERN) {
            String path = Registries.BLOCK.getId(block).getPath();
            return path.contains("stone_brick")
                    || path.contains("tuff") && !path.equals("tuff")
                    || path.contains("copper")
                            && !path.contains("ore")
                            && !path.equals("raw_copper_block")
                    || path.startsWith("deepslate_brick")
                    || path.startsWith("deepslate_tile")
                    || path.startsWith("cracked_deepslate")
                    || path.startsWith("polished_deepslate")
                    || path.equals("chiseled_deepslate")
                    || path.startsWith("suspicious_")
                    || path.contains("prismarine")
                    || path.endsWith("sponge")
                    || path.contains("terracotta")
                    || path.startsWith("mud_brick");
        } else {
            return true;
        }
    }

    private static Set<Block> set(Block... block) {
        Set setFromMap = Collections.newSetFromMap(new IdentityHashMap());
        Collections.addAll(setFromMap, block);
        return setFromMap;
    }

    @Environment(EnvType.CLIENT)
    private static final class Bands implements BlobJudge.Vanilla {

        private final BlobJudge.Vanilla vanilla;

        private int lastX = -2147483648;

        private int lastZ = -2147483648;

        private VanillaBand last;

        Bands(BlobJudge.Vanilla vanilla2) {
            this.vanilla = vanilla2;
        }

        VanillaBand at(int i, int j) {
            return this.band(i >> 4, j >> 4);
        }

        @Override
        public VanillaBand band(int i, int j) {
            if (i != this.lastX || j != this.lastZ) {
                this.lastX = i;
                this.lastZ = j;
                this.last = this.vanilla.band(i, j);
            }
            return this.last;
        }
    }

    @Environment(EnvType.CLIENT)
    public interface Client {

        BlockState state(int i, int j, int k);
    }

    @Environment(EnvType.CLIENT)
    public record Cluster(
            byte kind,
            long owner,
            BlobJudge.Status unfinished,
            long[] cells,
            long[] judged,
            long[] shell,
            byte[] shellFlags) {}

    @Environment(EnvType.CLIENT)
    public static enum Status {
        ALERT,
        CLEAN,
        OPEN,
        STRUCTURE,
        TERRAIN,
        MISPREDICTED,
        HIDDEN,
        TOO_FEW,
        INCOMPLETE,
        OUT_OF_BAND;

        private Status() {}
    }

    @Environment(EnvType.CLIENT)
    public interface Vanilla {

        VanillaBand band(int i, int j);
    }

    @Environment(EnvType.CLIENT)
    public record Verdict(BlobJudge.Status status, byte kind, long owner, long[] changes) {

        public boolean alert() {
            return this.status == BlobJudge.Status.ALERT;
        }
    }
}
