package net.efn.client.module.impl.render;

import com.example.client.mixin.AccessorPinkWaterLevelRenderer;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.efn.client.event.EventPriority;
import net.efn.client.event.ListenerHandle;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.chunk.ChunkBuilder.BuiltChunk;
import net.minecraft.client.render.chunk.ChunkRenderData;
import net.minecraft.client.render.chunk.ChunkRendererRegionBuilder;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLongArray;

@Environment(EnvType.CLIENT)
public final class PinkWater {

    public static final String OFF = "Off";

    static final int WARM = 4445678;

    static final int LUKEWARM = 4566514;

    static final int CHERRY_GROVE = 6141935;

    static final int OCEAN = 4159204;

    static final int PALE_GARDEN = 7768221;

    static final int SWAMP = 6388580;

    static final int MANGROVE = 3832426;

    static final int COLD = 4020182;

    static final int MEADOW = 937679;

    static final int FROZEN = 3750089;

    static final int[] BIOMES =
            new int[] {
                4445678, 4566514, 6141935, 4159204, 7768221, 6388580, 3832426, 4020182, 937679,
                3750089
            };

    private static final double[][] BIOME_LAB = new double[BIOMES.length][];

    private static final double FOG_HUE = 240.0;

    private static final double FOG_SATURATION = 0.902;

    private static final int SWITCH_ON_TICKS = 60;

    private static final int SWITCH_OFF_TICKS = 40;

    private static final int CHANGE_TICKS = 15;

    private static final float FOG_STEP = 0.05F;

    private static final int UNSEEN_BATCH = 64;

    private static final int UNSEEN_WAIT = 2;

    private static final int UNSEEN_PATIENCE = 20;

    private static final int UNSEEN_TRICKLE = 8;

    private static final double BLEND_POWER = 3.0;

    private static final double LIGHT_CARRY = 0.8;

    private static volatile PinkWater.Design shown;

    private static volatile PinkWater.Design fogDesign;

    private static volatile float fogShare;

    private static PinkWater.Design chosen;

    private static PinkWater.Design pending;

    private static int pendingTicks;

    private static float fogLevel;

    private static final List<PinkWater.Unseen> UNSEEN;

    private static int unseenNext;

    private static ClientWorld unseenLevel;

    private static BuiltChunkStorage unseenArea;

    private static int unseenWait;

    private static int unseenStalled;

    private static final Object UNSEEN_OWNER;

    private static ListenerHandle<TickEvent> unseenHook;

    private static final int CACHE_SIZE = 64;

    private static final AtomicLongArray CACHE;

    private PinkWater() {}

    public static String[] modes() {
        PinkWater.Design[] designs = PinkWater.Design.values();
        String[] strings = new String[designs.length + 1];
        strings[0] = "Off";
        for (int i = 0; i < designs.length; i++) {
            strings[i + 1] = designs[i].label;
        }
        return strings;
    }

    public static boolean shows() {
        return shown != null;
    }

    public static int water(int i, boolean bl) {
        PinkWater.Design design = shown;
        return design == null ? i : cached(i, bl, design);
    }

    public static byte caveBiome(Biome biome) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) {
            return 0;
        } else {
            Optional optional = world.getRegistryManager().getOptional(RegistryKeys.BIOME);
            if (optional.isEmpty()) {
                return 0;
            } else {
                Optional key = ((Registry) optional.get()).getKey(biome);
                if (key.isEmpty()) {
                    return 1;
                } else {
                    RegistryKey key2 = (RegistryKey) key.get();
                    return (byte)
                            (!key2.equals(BiomeKeys.LUSH_CAVES)
                                            && !key2.equals(BiomeKeys.DRIPSTONE_CAVES)
                                            && !key2.equals(BiomeKeys.DEEP_DARK)
                                    ? 1
                                    : 2);
                }
            }
        }
    }

    public static int waterFog(int i) {
        float f = fogShare;
        PinkWater.Design design = fogDesign;
        return !(f <= 0.0F) && design != null ? mix(i, waterFog(i, design), f) : i;
    }

    public static void underwater(Vector4f vector4f) {
        float f = fogShare;
        PinkWater.Design design = fogDesign;
        if (!(f <= 0.0F) && design != null) {
            float g = 1.0F + ((float) design.fog.shade - 1.0F) * f;
            vector4f.x *= g;
            vector4f.y *= g;
            vector4f.z *= g;
        }
    }

    public static int light(int i) {
        PinkWater.Design design = shown;
        return design == null ? i : light(i, design);
    }

    public static void tick(MinecraftClient client, boolean bl, PinkWater.Design design) {
        PinkWater.Design design2 = bl ? design : null;
        if (design2 == chosen) {
            pending = null;
            pendingTicks = 0;
        } else {
            if (design2 != pending) {
                pending = design2;
                pendingTicks = 0;
            }
            int i = chosen == null ? 60 : (design != null && design2 == null ? 40 : 15);
            if (++pendingTicks >= i) {
                chosen = design2;
                pending = null;
                pendingTicks = 0;
            }
        }
        ClientWorld world = client.world;
        boolean bl2 =
                world != null
                        && world.getRegistryKey() != World.NETHER
                        && world.getRegistryKey() != World.END;
        show(client, bl2 ? chosen : null);
        fogLevel = Math.max(0.0F, Math.min(1.0F, fogLevel + (shown != null ? 0.05F : -0.05F)));
        fogShare = fogLevel * fogLevel * (3.0F - 2.0F * fogLevel);
    }

    public static void reset(MinecraftClient client) {
        chosen = null;
        pending = null;
        pendingTicks = 0;
        fogLevel = 0.0F;
        fogShare = 0.0F;
        show(client, null);
    }

    private static void show(MinecraftClient client, PinkWater.Design design) {
        if (design != shown) {
            shown = design;
            if (design != null) {
                fogDesign = design;
            }
            ClientWorld world = client.world;
            if (client.world != null && client.worldRenderer != null) {
                world.reloadColor();
                refresh(client, world);
            } else {
                clearUnseen();
            }
        }
    }

    private static void refresh(MinecraftClient client, ClientWorld world) {
        WorldRenderer worldRenderer = client.worldRenderer;
        ChunkSectionPos chunkSectionPos =
                ChunkSectionPos.from(client.gameRenderer.getCamera().getCameraPos());
        int i = client.options.getClampedViewDistance() + 1;
        LongOpenHashSet longOpenHashSet = new LongOpenHashSet();
        for (int j = chunkSectionPos.getSectionX() - i;
                j <= chunkSectionPos.getSectionX() + i;
                j++) {
            for (int k = chunkSectionPos.getSectionZ() - i;
                    k <= chunkSectionPos.getSectionZ() + i;
                    k++) {
                WorldChunk worldChunk = world.getChunkManager().getWorldChunk(j, k);
                if (worldChunk != null) {
                    ChunkSection[] sectionArray = worldChunk.getSectionArray();
                    for (int m = 0; m < sectionArray.length; m++) {
                        ChunkSection section2 = sectionArray[m];
                        if (!section2.isEmpty() && section2.hasAny(PinkWater::tinted)) {
                            longOpenHashSet.add(
                                    ChunkSectionPos.asLong(
                                            j, worldChunk.sectionIndexToCoord(m), k));
                        }
                    }
                }
            }
        }
        queueUnseen(worldRenderer, world, chunkSectionPos, longOpenHashSet);
        LongIterator longIterator = longOpenHashSet.iterator();
        while (longIterator.hasNext()) {
            long l = longIterator.nextLong();
            worldRenderer.scheduleChunkRender(
                    ChunkSectionPos.unpackX(l),
                    ChunkSectionPos.unpackY(l),
                    ChunkSectionPos.unpackZ(l));
        }
    }

    private static boolean tinted(BlockState state) {
        return Fluids.WATER.matchesType(state.getFluidState().getFluid())
                || state.isOf(Blocks.WATER_CAULDRON);
    }

    private static void queueUnseen(
            WorldRenderer worldRenderer,
            ClientWorld world,
            ChunkSectionPos chunkSectionPos,
            LongOpenHashSet longOpenHashSet) {
        clearUnseen();
        BuiltChunkStorage builtChunkStorage =
                ((AccessorPinkWaterLevelRenderer) worldRenderer).efn$pinkWaterViewArea();
        if (builtChunkStorage != null
                && builtChunkStorage.chunks != null
                && !longOpenHashSet.isEmpty()) {
            for (BuiltChunk chunk : builtChunkStorage.chunks) {
                long sectionPos;
                if (chunk != null && longOpenHashSet.contains(sectionPos = chunk.getSectionPos())) {
                    boolean bl =
                            chunk.getCurrentRenderData() == ChunkRenderData.HIDDEN
                                    && !chunk.needsRebuild();
                    UNSEEN.add(new PinkWater.Unseen(chunk, sectionPos, bl));
                }
            }
            if (!UNSEEN.isEmpty()) {
                UNSEEN.sort(
                        Comparator.comparingLong(unseen -> distance(unseen.node, chunkSectionPos)));
                unseenLevel = world;
                unseenArea = builtChunkStorage;
                unseenWait = 2;
                unseenStalled = 0;
                if (unseenHook == null) {
                    unseenHook =
                            TickEvent.TYPE.listen(
                                    UNSEEN_OWNER,
                                    Stage.POST,
                                    EventPriority.NORMAL,
                                    tickEvent -> rebuildUnseen(MinecraftClient.getInstance()));
                }
            }
        }
    }

    private static void rebuildUnseen(MinecraftClient client) {
        if (unseenNext >= UNSEEN.size()) {
            clearUnseen();
            unhookUnseen();
        } else {
            WorldRenderer worldRenderer = client.worldRenderer;
            ChunkBuilder chunkBuilder =
                    worldRenderer == null ? null : worldRenderer.getChunkBuilder();
            if (client.world != unseenLevel
                    || chunkBuilder == null
                    || ((AccessorPinkWaterLevelRenderer) worldRenderer).efn$pinkWaterViewArea()
                            != unseenArea) {
                clearUnseen();
                unhookUnseen();
            } else if (unseenWait > 0) {
                unseenWait--;
            } else {
                int i;
                if (chunkBuilder.getScheduledTaskCount() == 0) {
                    i = 64;
                    unseenStalled = 0;
                } else {
                    if (++unseenStalled < 20) {
                        return;
                    }
                    i = 8;
                    unseenStalled = 0;
                }
                ChunkRendererRegionBuilder chunkRendererRegionBuilder = null;
                while (i > 0 && unseenNext < UNSEEN.size()) {
                    PinkWater.Unseen unseen = UNSEEN.get(unseenNext++);
                    BuiltChunk section2 = unseen.section;
                    if (section2.getSectionPos() == unseen.node
                            && section2.needsRebuild()
                            && (section2.getCurrentRenderData() != ChunkRenderData.HIDDEN
                                    || unseen.firstBuild && section2.shouldBuild())) {
                        if (chunkRendererRegionBuilder == null) {
                            chunkRendererRegionBuilder = new ChunkRendererRegionBuilder();
                        }
                        section2.scheduleRebuild(chunkRendererRegionBuilder);
                        section2.cancelRebuild();
                        i--;
                    }
                }
            }
        }
    }

    private static void clearUnseen() {
        UNSEEN.clear();
        unseenNext = 0;
        unseenLevel = null;
        unseenArea = null;
    }

    private static void unhookUnseen() {
        if (unseenHook != null) {
            unseenHook.unregister();
            unseenHook = null;
        }
    }

    private static long distance(long l, ChunkSectionPos chunkSectionPos) {
        long l2 = ChunkSectionPos.unpackX(l) - chunkSectionPos.getSectionX();
        long l3 = ChunkSectionPos.unpackY(l) - chunkSectionPos.getSectionY();
        long l4 = ChunkSectionPos.unpackZ(l) - chunkSectionPos.getSectionZ();
        return l2 * l2 + l3 * l3 + l4 * l4;
    }

    public static int water(int i, PinkWater.Design design) {
        return water(i, false, design);
    }

    public static int water(int i, boolean bl, PinkWater.Design design) {
        int index = index(i & 16777215);
        double[] doubles =
                index >= 0 ? (double[]) design.water[index].clone() : between(oklab(i), design);
        if (bl) {
            for (int k = 0; k < 3; k++) {
                doubles[k] += design.cave[k];
            }
        }
        return fromOklab(doubles[0], doubles[1], doubles[2]);
    }

    private static double[] between(double[] doubles, PinkWater.Design design) {
        double d = 0.0;
        double e = 0.0;
        double d2 = 0.0;
        double e2 = 0.0;
        double d3 = 0.0;
        double e3 = 0.0;
        for (int i = 0; i < BIOMES.length; i++) {
            double[] doubles2 = BIOME_LAB[i];
            double[] doubles3 = design.water[i];
            double d4 = doubles[0] - doubles2[0];
            double e4 = doubles[1] - doubles2[1];
            double d5 = doubles[2] - doubles2[2];
            double e5 = 1.0 / Math.pow(Math.max(d4 * d4 + e4 * e4 + d5 * d5, 1.0E-12), 1.5);
            d += e5;
            e += e5 * doubles3[0];
            d2 += e5 * doubles3[1];
            e2 += e5 * doubles3[2];
            d3 += e5 * doubles2[0];
            e3 += e5 * Math.hypot(doubles2[1], doubles2[2]);
        }
        e /= d;
        e = clamp(e + (doubles[0] - d3 / d) * 0.8, 0.3, 0.92);
        double d6 =
                Math.sqrt(
                        clamp(
                                Math.hypot(doubles[1], doubles[2]) / Math.max(e3 / d, 1.0E-6),
                                0.4,
                                1.6));
        double e6;
        double d7;
        return new double[] {e, (e6 = d2 / d) * d6, (d7 = e2 / d) * d6};
    }

    static int index(int i) {
        for (int j = 0; j < BIOMES.length; j++) {
            if (BIOMES[j] == i) {
                return j;
            }
        }
        return -1;
    }

    public static int waterFog(int i, PinkWater.Design design) {
        PinkWater.Fog fog2 = design.fog;
        int j = i >> 16 & 0xFF;
        int k = i >> 8 & 0xFF;
        int m = i & 0xFF;
        int n = Math.max(j, Math.max(k, m));
        int o = Math.min(j, Math.min(k, m));
        double d = n / 255.0;
        double e = n == 0 ? 0.0 : (double) (n - o) / n;
        double d2 = 0.0;
        if (n != o) {
            double e2 = n - o;
            d2 =
                    n == j
                            ? 60.0 * ((k - m) / e2)
                            : (n == k ? 60.0 * ((m - j) / e2 + 2.0) : 60.0 * ((j - k) / e2 + 4.0));
        }
        double d3 = fog2.hueFollow * smooth(0.1, 0.3, e);
        double e3 = fog2.hue + clamp(-d3 * wrap(d2 - 240.0), -fog2.maxShift, fog2.maxShift);
        double d4 = clamp(fog2.saturation * Math.pow(clamp(e / 0.902, 0.25, 1.1), 0.7), 0.0, 1.0);
        double e4 = clamp(d * fog2.value, 0.0, 1.0);
        return i & 0xFF000000 | hsv(e3, d4, e4);
    }

    public static int underwater(int i, PinkWater.Design design) {
        double shade2 = design.fog.shade;
        return srgbChannel((i >> 16 & 0xFF) / 255.0 * shade2) << 16
                | srgbChannel((i >> 8 & 0xFF) / 255.0 * shade2) << 8
                | srgbChannel((i & 0xFF) / 255.0 * shade2);
    }

    public static int light(int i, PinkWater.Design design) {
        int j = i >>> 20 & 15;
        int k = (design.glow * j + 7) / 15 << 4;
        return k > 0 && (i & 0xFF) < k ? i & -256 | k : i;
    }

    private static int cached(int i, boolean bl, PinkWater.Design design) {
        int j = i & 16777215 | design.ordinal() + 1 << 24 | (bl ? 1073741824 : 0);
        int k = j * -1640531535 >>> 26 & 63;
        long opaque = CACHE.getOpaque(k);
        if ((int) (opaque >>> 32) == j) {
            return (int) opaque;
        } else {
            int water2 = water(i, bl, design);
            CACHE.setOpaque(k, (long) j << 32 | water2 & 4294967295L);
            return water2;
        }
    }

    static double[] oklab(int i) {
        double linear = linear((i >> 16 & 0xFF) / 255.0);
        double linear2 = linear((i >> 8 & 0xFF) / 255.0);
        double linear3 = linear((i & 0xFF) / 255.0);
        double d =
                Math.cbrt(0.4122214708 * linear + 0.5363325363 * linear2 + 0.0514459929 * linear3);
        double e =
                Math.cbrt(0.2119034982 * linear + 0.6806995451 * linear2 + 0.1073969566 * linear3);
        double d2 =
                Math.cbrt(0.0883024619 * linear + 0.2817188376 * linear2 + 0.6299787005 * linear3);
        return new double[] {
            0.2104542553 * d + 0.793617785 * e - 0.0040720468 * d2,
            1.9779984951 * d - 2.428592205 * e + 0.4505937099 * d2,
            0.0259040371 * d + 0.7827717662 * e - 0.808675766 * d2
        };
    }

    private static int fromOklab(double d, double e, double d2) {
        double[] doubles = new double[3];
        if (!fromOklab(d, e, d2, doubles)) {
            double e2 = 0.0;
            double d3 = 1.0;
            for (int i = 0; i < 14; i++) {
                double e3 = (e2 + d3) * 0.5;
                if (fromOklab(d, e * e3, d2 * e3, doubles)) {
                    e2 = e3;
                } else {
                    d3 = e3;
                }
            }
            fromOklab(d, e * e2, d2 * e2, doubles);
        }
        return channel(doubles[0]) << 16 | channel(doubles[1]) << 8 | channel(doubles[2]);
    }

    private static boolean fromOklab(double d, double e, double d2, double[] doubles) {
        double e2 = d + 0.3963377774 * e + 0.2158037573 * d2;
        double d3 = d - 0.1055613458 * e - 0.0638541728 * d2;
        double e3 = d - 0.0894841775 * e - 1.291485548 * d2;
        e2 = e2 * e2 * e2;
        d3 = d3 * d3 * d3;
        e3 = e3 * e3 * e3;
        doubles[0] = 4.0767416621 * e2 - 3.3077115913 * d3 + 0.2309699292 * e3;
        doubles[1] = -1.2684380046 * e2 + 2.6097574011 * d3 - 0.3413193965 * e3;
        doubles[2] = -0.0041960863 * e2 - 0.7034186147 * d3 + 1.707614701 * e3;
        double d4 = 1.0E-4;
        return doubles[0] >= -d4
                && doubles[0] <= 1.0 + d4
                && doubles[1] >= -d4
                && doubles[1] <= 1.0 + d4
                && doubles[2] >= -d4
                && doubles[2] <= 1.0 + d4;
    }

    private static int hsv(double d, double e, double d2) {
        double e2 = (d % 360.0 + 360.0) % 360.0 / 60.0;
        int i = (int) Math.floor(e2) % 6;
        double d3 = e2 - Math.floor(e2);
        double e3 = d2 * (1.0 - e);
        double d4 = d2 * (1.0 - e * d3);
        double e4 = d2 * (1.0 - e * (1.0 - d3));
        double d5;
        double e5;
        double d6 =
                switch (i) {
                    case 0 -> {
                        e5 = d2;
                        d5 = e4;
                        yield e3;
                    }
                    case 1 -> {
                        e5 = d4;
                        d5 = d2;
                        yield e3;
                    }
                    case 2 -> {
                        e5 = e3;
                        d5 = d2;
                        yield e4;
                    }
                    case 3 -> {
                        e5 = e3;
                        d5 = d4;
                        yield d2;
                    }
                    case 4 -> {
                        e5 = e4;
                        d5 = e3;
                        yield d2;
                    }
                    default -> {
                        e5 = d2;
                        d5 = e3;
                        yield d4;
                    }
                };
        return srgbChannel(e5) << 16 | srgbChannel(d5) << 8 | srgbChannel(d6);
    }

    private static double linear(double d) {
        return d <= 0.04045 ? d / 12.92 : Math.pow((d + 0.055) / 1.055, 2.4);
    }

    private static int channel(double d) {
        double e = clamp(d, 0.0, 1.0);
        double d2 = e <= 0.0031308 ? e * 12.92 : 1.055 * Math.pow(e, 0.4166666666666667) - 0.055;
        return srgbChannel(d2);
    }

    private static int srgbChannel(double d) {
        return (int) Math.round(clamp(d, 0.0, 1.0) * 255.0);
    }

    private static int mix(int i, int j, float f) {
        int k = Math.round((i >> 16 & 0xFF) + ((j >> 16 & 0xFF) - (i >> 16 & 0xFF)) * f);
        int m = Math.round((i >> 8 & 0xFF) + ((j >> 8 & 0xFF) - (i >> 8 & 0xFF)) * f);
        int n = Math.round((i & 0xFF) + ((j & 0xFF) - (i & 0xFF)) * f);
        return i & 0xFF000000 | k << 16 | m << 8 | n;
    }

    private static double wrap(double d) {
        double e = d % 360.0;
        if (e > 180.0) {
            e -= 360.0;
        }
        if (e <= -180.0) {
            e += 360.0;
        }
        return e;
    }

    private static double smooth(double d, double e, double d2) {
        double e2 = clamp((d2 - d) / (e - d), 0.0, 1.0);
        return e2 * e2 * (3.0 - 2.0 * e2);
    }

    private static double clamp(double d, double e, double d2) {
        return d < e ? e : (d > d2 ? d2 : d);
    }

    static {
        for (int i = 0; i < BIOMES.length; i++) {
            BIOME_LAB[i] = oklab(BIOMES[i]);
        }
        UNSEEN = new ArrayList<>();
        UNSEEN_OWNER = new Object();
        CACHE = new AtomicLongArray(64);
    }

    @Environment(EnvType.CLIENT)
    public static enum Design {
        ROSE_QUARTZ(
                "Rose Quartz",
                new int[] {
                    16742585, 15429040, 15626161, 13392801, 12357803, 11165580, 10508930, 12476315,
                    12146838, 11817105
                },
                11862992,
                new PinkWater.Fog(338.0, 0.25, 15.0, 0.47, 1.0, 0.84),
                6),
        PINK_SALT_LAKE(
                "Pink Salt Lake",
                new int[] {
                    16759265, 16361428, 16427734, 15633086, 14923972, 14781610, 14452650, 15762095,
                    14908074, 14119845
                },
                12279295,
                new PinkWater.Fog(340.0, 0.15, 10.0, 0.36, 1.6, 0.77),
                7),
        SUNSET_LAGOON(
                "Sunset Lagoon",
                new int[] {
                    16751278, 16746659, 16747428, 16737936, 14130594, 14444925, 13789560, 15753090,
                    14900600, 14113390
                },
                16732671,
                new PinkWater.Fog(352.0, 0.3, 15.0, 0.46, 1.15, 0.81),
                2),
        NEON_NIGHT(
                "Neon Night",
                new int[] {
                    16740085, 15753959, 15885545, 14175952, 13076403, 13459385, 12802735, 12466090,
                    11808155, 11150220
                },
                15466751,
                new PinkWater.Fog(322.0, 0.2, 12.0, 0.5, 1.0, 0.82),
                8),
        ORCHID_DEPTHS(
                "Orchid Depths",
                new int[] {
                    13459425, 12538831, 12670418, 11026355, 10317461, 9193110, 9518220, 9515665,
                    8531337, 7547010
                },
                6815936,
                new PinkWater.Fog(300.0, 0.3, 15.0, 0.47, 0.9, 0.76),
                1),
        COTTON_CANDY(
                "Cotton Candy",
                new int[] {
                    16425215, 15569654, 15701240, 14253288, 14595804, 12157650, 12156370, 12485350,
                    11959777, 11499740
                },
                8402175,
                new PinkWater.Fog(300.0, 0.3, 20.0, 0.38, 1.5, 0.77),
                3);

        public static final PinkWater.Design DEFAULT = ROSE_QUARTZ;

        private final String label;

        final double[][] water;

        final double[] cave;

        final PinkWater.Fog fog;

        final int glow;

        private Design(String label2, int[] ints, int j, PinkWater.Fog fog2, int glow2) {
            if (ints.length != PinkWater.BIOMES.length) {
                throw new IllegalArgumentException(label2 + ": one colour per biome");
            } else {
                this.label = label2;
                this.water = new double[ints.length][];
                for (int i = 0; i < ints.length; i++) {
                    this.water[i] = PinkWater.oklab(ints[i]);
                }
                double[] doubles = this.water[PinkWater.index(4159204)];
                double[] doubles2 = PinkWater.oklab(j);
                this.cave =
                        new double[] {
                            doubles2[0] - doubles[0],
                            doubles2[1] - doubles[1],
                            doubles2[2] - doubles[2]
                        };
                this.fog = fog2;
                this.glow = glow2;
            }
        }

        public String label() {
            return this.label;
        }

        public static PinkWater.Design byLabel(String string) {
            for (PinkWater.Design design : values()) {
                if (design.label.equals(string)) {
                    return design;
                }
            }
            return null;
        }
    }

    @Environment(EnvType.CLIENT)
    record Fog(
            double hue,
            double hueFollow,
            double maxShift,
            double saturation,
            double value,
            double shade) {}

    @Environment(EnvType.CLIENT)
    private record Unseen(BuiltChunk section, long node, boolean firstBuild) {}
}
