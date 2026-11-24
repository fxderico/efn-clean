/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.module.impl.render;

import com.example.client.mixin.AccessorSpawnerNametagsBaseSpawner;

import imgui.ImDrawList;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.efn.client.esp.ChunkScanner;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.gui.GuiPalette;
import net.efn.client.gui.WorldLabel;
import net.efn.client.gui.WorldText;
import net.efn.client.module.ClientModule;
import net.efn.client.module.HudOverlayProvider;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.spawner.MobSpawnerEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.LongPredicate;

@Environment(value = EnvType.CLIENT)
public final class SpawnerNametagsModule extends ClientModule
        implements HudOverlayProvider, ChunkScanner.Consumer {

    private static final int MAX_LABELS = 48;

    private static final int RING_SEGMENTS = 64;

    private static final float RING_WIDTH = 1.5f;

    private static final float RING_ALPHA_INSIDE = 0.8f;

    private static final float RING_ALPHA_OUTSIDE = 0.4f;

    private static final double LABEL_HEIGHT = 1.2;

    private static final double FADE_SHARE = 0.125;

    private static final String SEPARATOR = "  \u00b7  ";

    private final NumberSetting sRange =
            this.addSetting("range", new NumberSetting("Range", 64.0f, 8.0f, 256.0f).step(1.0f));

    private final BooleanSetting sRadius =
            this.addSetting("activation-radius", new BooleanSetting("Activation Radius", true));

    private final LongOpenHashSet spawners = new LongOpenHashSet();

    public SpawnerNametagsModule() {
        super(
                "spawner-nametags",
                "Spawner Nametags",
                "Shows each spawner's mob and activation range over the block.",
                ModuleCategory.RENDER);
    }

    @Override
    protected void onEnable() {
        ChunkScanner.register(this);
    }

    @Override
    protected void onDisable() {
        ChunkScanner.unregister(this);
        this.spawners.clear();
    }

    public static String mobName(MobSpawnerBlockEntity mobSpawnerBlockEntity) {
        MobSpawnerEntry mobSpawnerEntry =
                ((AccessorSpawnerNametagsBaseSpawner) ((Object) mobSpawnerBlockEntity.getLogic()))
                        .efn$spawnerNametagsNextSpawnData();
        if (mobSpawnerEntry == null) {
            return null;
        }
        String string = mobSpawnerEntry.entity().getString("id", "");
        if (string.isEmpty()) {
            return null;
        }
        return EntityType.get(string)
                .map(entityType -> entityType.getName().getString())
                .orElse(null);
    }

    public static int activationRange(MobSpawnerBlockEntity mobSpawnerBlockEntity) {
        return ((AccessorSpawnerNametagsBaseSpawner) ((Object) mobSpawnerBlockEntity.getLogic()))
                .efn$spawnerNametagsRequiredPlayerRange();
    }

    @Override
    public boolean wants(BlockState state) {
        return state.isOf(Blocks.SPAWNER);
    }

    @Override
    public void found(long l, BlockState state) {
        this.spawners.add(l);
    }

    @Override
    public void removed(long l) {
        this.spawners.remove(l);
    }

    @Override
    public void reset() {
        this.spawners.clear();
    }

    @Override
    public void chunkScanned(long l, LongPredicate longPredicate) {
        LongIterator longIterator = this.spawners.iterator();
        while (longIterator.hasNext()) {
            long l2 = longIterator.nextLong();
            if (ChunkPos.toLong(BlockPos.unpackLongX(l2) >> 4, BlockPos.unpackLongZ(l2) >> 4) != l
                    || longPredicate.test(l2)) continue;
            longIterator.remove();
        }
    }

    private List<Shown> inRange(MinecraftClient client) {
        ArrayList<Shown> list = new ArrayList<Shown>();
        if (this.spawners.isEmpty() || client.world == null || client.player == null) {
            return list;
        }
        ClientWorld world = client.world;
        Vec3d entityPos = client.player.getEntityPos();
        double sRange2 = this.sRange.get();
        LongIterator longIterator = this.spawners.iterator();
        while (longIterator.hasNext()) {
            BlockEntity blockEntity;
            BlockPos pos2 = BlockPos.fromLong(longIterator.nextLong());
            double squaredDistance =
                    entityPos.squaredDistanceTo(
                            (double) pos2.getX() + 0.5,
                            (double) pos2.getY() + 0.5,
                            (double) pos2.getZ() + 0.5);
            if (squaredDistance > sRange2 * sRange2
                    || !((blockEntity = world.getBlockEntity(pos2))
                            instanceof MobSpawnerBlockEntity)) continue;
            MobSpawnerBlockEntity mobSpawnerBlockEntity = (MobSpawnerBlockEntity) blockEntity;
            int activationRange = SpawnerNametagsModule.activationRange(mobSpawnerBlockEntity);
            boolean bl =
                    activationRange < 0
                            || squaredDistance
                                    < (double) activationRange * (double) activationRange;
            list.add(
                    new Shown(
                            pos2,
                            Math.sqrt(squaredDistance),
                            SpawnerNametagsModule.mobName(mobSpawnerBlockEntity),
                            activationRange,
                            bl));
        }
        list.sort(Comparator.comparingDouble(Shown::distance));
        return list.size() > 48 ? list.subList(0, 48) : list;
    }

    private float fade(double d) {
        double sRange2 = this.sRange.get();
        return (float) Math.max(0.0, Math.min(1.0, (sRange2 - d) / (sRange2 * 0.125)));
    }

    @Override
    public boolean isHudOverlayEnabled() {
        return this.isEnabled() && !this.spawners.isEmpty();
    }

    @Override
    public void renderHudOverlay(ImDrawList imDrawList, float f, float g) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.hudHidden) {
            return;
        }
        List<Shown> list = this.inRange(client);
        int hudAccent = WorldText.hudAccent();
        for (int i = list.size() - 1; i >= 0; --i) {
            int[] ints;
            String[] strings;
            Shown shown = list.get(i);
            float[] floats = SpawnerNametagsModule.labelAnchor(client, shown.pos(), f, g);
            if (floats == null) continue;
            String string = shown.mob() == null ? "Spawner" : shown.mob() + " Spawner";
            String string2 = Math.round(shown.distance()) + "m";
            if (this.sRadius.get() && shown.range() > 0) {
                strings = new String[] {string2, SEPARATOR, shown.range() + "m range"};
                ints =
                        new int[] {
                            WorldText.LABEL,
                            WorldText.LABEL,
                            shown.inside() ? hudAccent : WorldText.LABEL
                        };
            } else {
                strings = new String[] {string2};
                ints = new int[] {WorldText.LABEL};
            }
            WorldText.draw(
                    imDrawList,
                    floats[0],
                    floats[1],
                    string,
                    strings,
                    ints,
                    this.fade(shown.distance()));
        }
    }

    public static float[] labelAnchor(MinecraftClient client, BlockPos pos2, float f, float g) {
        float[] floats =
                WorldLabel.project(
                        client,
                        new Vec3d(
                                (double) pos2.getX() + 0.5,
                                (double) pos2.getY() + 1.2,
                                (double) pos2.getZ() + 0.5),
                        f,
                        g);
        if (floats == null) {
            return null;
        }
        for (int i = 0; i < 4; ++i) {
            float[] floats2 =
                    WorldLabel.project(
                            client,
                            new Vec3d(
                                    pos2.getX() + (i & 1),
                                    (double) pos2.getY() + 1.0,
                                    pos2.getZ() + (i >> 1)),
                            f,
                            g);
            if (floats2 == null) continue;
            floats[1] = Math.min(floats[1], floats2[1]);
        }
        return floats;
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (!this.sRadius.get()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        List<Shown> list = this.inRange(client);
        if (list.isEmpty()) {
            return;
        }
        int i = GuiPalette.abgr(WorldText.hudAccent()) & 0xFFFFFF;
        for (Shown shown : list) {
            float f;
            int j;
            if (shown.range() <= 0
                    || (j =
                                            Math.round(
                                                                    (f =
                                                                                    (shown.inside()
                                                                                                    ? 0.8f
                                                                                                    : 0.4f)
                                                                                            * this
                                                                                                    .fade(
                                                                                                            shown
                                                                                                                    .distance()))
                                                                            * 255.0f)
                                                            << 24
                                                    | i)
                                    >>> 24
                            < 4) continue;
            SpawnerNametagsModule.drawRing(Vec3d.ofCenter(shown.pos()), shown.range(), j);
        }
    }

    private static void drawRing(Vec3d vec, double d, int i) {
        Vec3d vec2 = null;
        for (int j = 0; j <= 64; ++j) {
            double e = Math.PI * 2 * (double) j / 64.0;
            Vec3d vec3 = vec.add(Math.cos(e) * d, 0.0, Math.sin(e) * d);
            if (vec2 != null) {
                GizmoDrawing.line(vec2, vec3, i, 1.5f).ignoreOcclusion();
            }
            vec2 = vec3;
        }
    }

    @Environment(value = EnvType.CLIENT)
    private record Shown(BlockPos pos, double distance, String mob, int range, boolean inside) {}
}
