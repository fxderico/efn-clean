package net.efn.client.module.impl.render;

import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.LightSnapshot;
import net.efn.client.util.SessionWorker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import net.minecraft.world.debug.gizmo.VisibilityConfigurable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CancellationException;

@Environment(EnvType.CLIENT)
public class LightEspModule extends ClientModule {

    private static final long SCAN_COOLDOWN_MS = 1000L;

    private static final int MAX_BOXES = 12000;

    private static final float LINE_WIDTH = 1.5F;

    private final SessionWorker<Map<Integer, List<Box>>> worker = new SessionWorker<>("EFN-Light");

    private ClientWorld sessionLevel;

    private LightSnapshot collecting;

    private long lastScanTime;

    private Map<Integer, List<Box>> renderData = Map.of();

    private final NumberSetting sRadius =
            this.addSetting(
                    "render-radius",
                    new NumberSetting("Render Radius", 8.0F, 1.0F, 16.0F).step(1.0F));

    private final NumberSetting sMinLight =
            this.addSetting(
                    "min-light", new NumberSetting("Min Light", 5.0F, 1.0F, 15.0F).step(1.0F));

    private final BooleanSetting sDenoise =
            this.addSetting("denoise", new BooleanSetting("Denoise", true));

    private final ColorSetting sColor =
            this.addSetting("color", new ColorSetting("Color", -2130719744));

    public LightEspModule() {
        super(
                "light-debug",
                "Light Debug",
                "Highlights artificial light underground.",
                ModuleCategory.BASE_RAID);
        List.of(this.sRadius, this.sMinLight, this.sDenoise)
                .forEach(setting -> setting.onChange(this::resetScan));
    }

    @Override
    protected void onEnable() {
        this.resetScan();
    }

    @Override
    protected void onDisable() {
        this.resetScan();
    }

    private void resetScan() {
        this.worker.close();
        this.sessionLevel = null;
        this.collecting = null;
        this.renderData = Map.of();
        this.lastScanTime = 0L;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (this.sessionLevel != client.world) {
            this.resetScan();
            this.sessionLevel = client.world;
        }
        if (client.world != null && client.player != null) {
            this.worker.poll(map -> this.renderData = map);
            if (!this.worker.busy()) {
                long l = System.currentTimeMillis();
                if (this.collecting == null) {
                    if (l - this.lastScanTime < 1000L) {
                        return;
                    }
                    this.collecting =
                            new LightSnapshot(
                                    client.player.getChunkPos(),
                                    clampInt(num(this.sRadius, 8), 1, 16),
                                    clampInt(num(this.sMinLight, 5), 1, 15));
                }
                if (this.collecting.collect(client.world)) {
                    Map map2 = this.collecting.hits();
                    boolean denoise = this.isDenoise();
                    int clampInt = clampInt(num(this.sMinLight, 5), 1, 15);
                    this.collecting = null;
                    this.lastScanTime = l;
                    this.worker.submit(() -> buildBoxes(map2, denoise, clampInt));
                }
            }
        }
    }

    static Map<Integer, List<Box>> buildBoxes(Map<BlockPos, Integer> map, boolean bl, int i) {
        HashSet set = new HashSet();
        if (bl) {
            for (Entry entry : map.entrySet()) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new CancellationException();
                }
                BlockPos key = (BlockPos) entry.getKey();
                int entry2 = (Integer) entry.getValue();
                if (entry2 >= i + 1 && entry2 <= i + 2 && !set.contains(key)) {
                    boolean bl2 = false;
                    for (int k = -1; k <= 1 && !bl2; k++) {
                        for (int m = -1; m <= 1 && !bl2; m++) {
                            for (int n = -1; n <= 1; n++) {
                                if (map.getOrDefault(key.add(k, m, n), 0) > entry2) {
                                    bl2 = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (!bl2) {
                        ArrayDeque arrayDeque = new ArrayDeque();
                        arrayDeque.add(key);
                        set.add(key);
                        while (!arrayDeque.isEmpty()) {
                            if (Thread.currentThread().isInterrupted()) {
                                throw new CancellationException();
                            }
                            BlockPos pos = (BlockPos) arrayDeque.removeFirst();
                            for (Direction direction : Direction.values()) {
                                BlockPos pos2 = pos.offset(direction);
                                int j = map.getOrDefault(pos2, 0);
                                if (j >= i && j <= entry2 && set.add(pos2)) {
                                    arrayDeque.addLast(pos2);
                                }
                            }
                        }
                    }
                }
            }
        }
        HashMap map2 = new HashMap();
        int o = 0;
        for (Entry entry3 : map.entrySet()) {
            if (Thread.currentThread().isInterrupted()) {
                throw new CancellationException();
            }
            BlockPos key2 = (BlockPos) entry3.getKey();
            if (!set.contains(key2)) {
                boolean bl3 = false;
                for (Direction direction2 : Direction.values()) {
                    BlockPos pos3 = key2.offset(direction2);
                    if (!map.containsKey(pos3) || set.contains(pos3)) {
                        bl3 = true;
                        break;
                    }
                }
                if (bl3) {
                    ((java.util.List)
                                    map2.computeIfAbsent(
                                            (Integer) entry3.getValue(),
                                            integer -> new ArrayList<>()))
                            .add(new Box(key2));
                    if (++o < 12000) {
                        continue;
                    }
                    break;
                }
            }
        }
        map2.replaceAll((integer, list) -> List.copyOf((java.util.Collection) list));
        return Map.copyOf(map2);
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null && client.player != null && client.world == this.sessionLevel) {
            Map renderData2 = this.renderData;
            if (!renderData2.isEmpty() && this.sColor != null) {
                int i = this.sColor.argb();
                int j = i >>> 24 & 0xFF;
                int k = i >> 16 & 0xFF;
                int m = i >> 8 & 0xFF;
                int n = i & 0xFF;
                for (Entry entry : (Iterable<Entry>) renderData2.entrySet()) {
                    float f = clamp01(((Integer) entry.getKey()).intValue() / 15.0F);
                    int o = lerp(k, 255, f);
                    int p = lerp(m, 255, f);
                    int q = lerp(n, 255, f);
                    int clampByte = clampByte(Math.round(j * (0.5F + 0.5F * f)));
                    int r = clampByte << 24 | o << 16 | p << 8 | q;
                    int t =
                            0xFF000000
                                    | Math.round(o * 0.45F) << 16
                                    | Math.round(p * 0.45F) << 8
                                    | Math.round(q * 0.45F);
                    DrawStyle drawStyle = DrawStyle.filledAndStroked(t, 1.5F, r);
                    for (Box box : (Iterable<Box>) (List) entry.getValue()) {
                        int u = (int) box.minX >> 4;
                        int v = (int) box.minZ >> 4;
                        if (client.world.getChunkManager().isChunkLoaded(u, v)) {
                            VisibilityConfigurable visibilityConfigurable =
                                    GizmoDrawing.box(box, drawStyle);
                            visibilityConfigurable.ignoreOcclusion();
                        }
                    }
                }
            }
        }
    }

    private boolean isDenoise() {
        return this.sDenoise != null && this.sDenoise.value().get();
    }

    private static int num(NumberSetting setting, int i) {
        return setting == null ? i : Math.round(setting.value()[0]);
    }

    private static int clampInt(int i, int j, int k) {
        return Math.max(j, Math.min(k, i));
    }

    private static int clampByte(int i) {
        return Math.max(0, Math.min(255, i));
    }

    private static float clamp01(float f) {
        return Math.max(0.0F, Math.min(1.0F, f));
    }

    private static int lerp(int i, int j, float f) {
        return Math.round(i + (j - i) * f);
    }
}
