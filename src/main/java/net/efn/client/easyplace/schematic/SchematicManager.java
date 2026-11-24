package net.efn.client.easyplace.schematic;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.RayTraceUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.util.LayerRange;

import net.efn.client.easyplace.EasyPlace;
import net.efn.client.easyplace.util.BlockStateUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Environment(EnvType.CLIENT)
public final class SchematicManager {

    private static final int CACHE_REFRESH_TICKS = 5;

    private static final int NAV_REFRESH_TICKS = 20;

    private final MinecraftClient mc;

    private final Map<Long, SchematicTarget> cachedTargets = new LinkedHashMap<>();

    private final List<SchematicManager.ActiveBox> activeBoxes = new ArrayList<>();

    private long lastRefreshTick = -9223372036854775808L;

    private BlockPos lastCenter;

    private double lastRange = -1.0;

    private boolean lastAura;

    private boolean lastAllBlocks;

    private int lastLayerRangeHash;

    private SchematicTarget cachedNavTarget;

    private long lastNavRefreshTick = -9223372036854775808L;

    public SchematicManager(MinecraftClient mc2) {
        this.mc = mc2;
    }

    public void clear() {
        this.cachedTargets.clear();
        this.activeBoxes.clear();
        this.lastRefreshTick = -9223372036854775808L;
        this.lastNavRefreshTick = -9223372036854775808L;
        this.cachedNavTarget = null;
        this.lastCenter = null;
    }

    public void invalidate() {
        this.lastRefreshTick = -9223372036854775808L;
    }

    public boolean hasActiveSchematic() {
        return this.rebuildActiveBoxes();
    }

    public List<SchematicManager.ActiveBox> activeBoxes() {
        this.rebuildActiveBoxes();
        return List.copyOf(this.activeBoxes);
    }

    public WorldSchematic getSchematicWorld() {
        return SchematicWorldHandler.getSchematicWorld();
    }

    public Collection<SchematicTarget> getTargets(long l, double d, boolean bl, boolean bl2) {
        if (this.mc.player != null && this.mc.world != null) {
            BlockPos pos = this.mc.player.getBlockPos();
            boolean bl3 = this.lastCenter == null || pos.getSquaredDistance(this.lastCenter) > 1.0;
            int layerRangeHash = this.getLayerRangeHash();
            boolean bl4 =
                    d != this.lastRange
                            || bl != this.lastAura
                            || bl2 != this.lastAllBlocks
                            || layerRangeHash != this.lastLayerRangeHash;
            if (l - this.lastRefreshTick >= 5L || bl3 || bl4) {
                this.refresh(l, pos, d, bl, bl2);
            }
            return this.cachedTargets.values();
        } else {
            this.clear();
            return List.of();
        }
    }

    private void refresh(
            long lastRefreshTick2,
            BlockPos pos,
            double lastRange2,
            boolean lastAura2,
            boolean lastAllBlocks2) {
        this.cachedTargets.clear();
        this.lastRefreshTick = lastRefreshTick2;
        this.lastCenter = pos.toImmutable();
        this.lastRange = lastRange2;
        this.lastAura = lastAura2;
        this.lastAllBlocks = lastAllBlocks2;
        this.lastLayerRangeHash = this.getLayerRangeHash();
        if (this.rebuildActiveBoxes()) {
            WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();
            if (schematicWorld != null) {
                if (!lastAura2) {
                    BlockPos schematicWorldTraceIfClosestNoFluids =
                            RayTraceUtils.getSchematicWorldTraceIfClosestNoFluids(
                                    this.mc.world, this.mc.player, lastRange2);
                    if (schematicWorldTraceIfClosestNoFluids != null) {
                        this.addTarget(
                                schematicWorldTraceIfClosestNoFluids,
                                schematicWorld,
                                lastAllBlocks2);
                    }
                } else {
                    Vec3d vec =
                            new Vec3d(
                                    this.mc.player.getX(),
                                    this.mc.player.getY(),
                                    this.mc.player.getZ());
                    int i = (int) Math.ceil(lastRange2);
                    double d = lastRange2 * lastRange2;
                    Mutable mutable = new Mutable();
                    for (int j = pos.getX() - i; j <= pos.getX() + i; j++) {
                        for (int k = pos.getY() - i; k <= pos.getY() + i; k++) {
                            for (int m = pos.getZ() - i; m <= pos.getZ() + i; m++) {
                                mutable.set(j, k, m);
                                if (!(mutable.toCenterPos().squaredDistanceTo(vec) > d)) {
                                    this.addTarget(mutable, schematicWorld, lastAllBlocks2);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void addTarget(BlockPos pos, WorldSchematic worldSchematic, boolean bl) {
        if (this.insideActivePlacement(pos)) {
            if (this.isInRenderLayer(pos)) {
                BlockState blockState = worldSchematic.getBlockState(pos);
                if (!blockState.isAir() && BlockStateUtil.isPrimaryPlacementState(blockState)) {
                    if (bl || BlockStateUtil.isSimpleFullBlock(blockState, worldSchematic, pos)) {
                        BlockState blockState2 = this.mc.world.getBlockState(pos);
                        TargetStatus targetStatus =
                                BlockStateUtil.classify(blockState, blockState2);
                        if (targetStatus != TargetStatus.CORRECT) {
                            BlockPos pos2 = pos.toImmutable();
                            double squaredDistance =
                                    pos2.toCenterPos()
                                            .squaredDistanceTo(this.mc.player.getEyePos());
                            this.cachedTargets.put(
                                    pos2.asLong(),
                                    new SchematicTarget(
                                            pos2,
                                            SchematicBlockState.of(blockState),
                                            blockState2,
                                            targetStatus,
                                            squaredDistance));
                        }
                    }
                }
            }
        }
    }

    private boolean rebuildActiveBoxes() {
        this.activeBoxes.clear();
        for (SchematicPlacement allSchematicsPlacement :
                DataManager.getSchematicPlacementManager().getAllSchematicsPlacements()) {
            if (allSchematicsPlacement.isEnabled()) {
                for (Box box :
                        allSchematicsPlacement
                                .getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED)
                                .values()) {
                    if (box.getPos1() != null && box.getPos2() != null) {
                        this.activeBoxes.add(
                                SchematicManager.ActiveBox.of(box.getPos1(), box.getPos2()));
                    }
                }
            }
        }
        return !this.activeBoxes.isEmpty();
    }

    private boolean insideActivePlacement(BlockPos pos) {
        for (SchematicManager.ActiveBox activeBox : this.activeBoxes) {
            if (activeBox.contains(pos)) {
                return true;
            }
        }
        return false;
    }

    public static boolean inRenderLayer(BlockPos pos) {
        try {
            LayerRange renderLayerRange = DataManager.getRenderLayerRange();
            return renderLayerRange == null || renderLayerRange.isPositionWithinRange(pos);
        } catch (Exception exception) {
            return true;
        }
    }

    private boolean isInRenderLayer(BlockPos pos) {
        try {
            LayerRange renderLayerRange = DataManager.getRenderLayerRange();
            return renderLayerRange == null || renderLayerRange.isPositionWithinRange(pos);
        } catch (Exception exception) {
            return true;
        }
    }

    private int getLayerRangeHash() {
        try {
            LayerRange renderLayerRange = DataManager.getRenderLayerRange();
            return renderLayerRange == null
                    ? 0
                    : Objects.hash(
                            renderLayerRange.getLayerMode(),
                            renderLayerRange.getLayerRangeMin(),
                            renderLayerRange.getLayerRangeMax(),
                            renderLayerRange.getLayerSingle(),
                            renderLayerRange.getAxis());
        } catch (Exception exception) {
            return 0;
        }
    }

    public SchematicTarget nearestMissingForNavigation(long lastNavRefreshTick2, boolean bl) {
        if (this.mc.player != null && this.mc.world != null) {
            if (lastNavRefreshTick2 - this.lastNavRefreshTick < 20L) {
                return this.cachedNavTarget;
            } else {
                this.lastNavRefreshTick = lastNavRefreshTick2;
                this.cachedNavTarget = null;
                try {
                    if (!this.rebuildActiveBoxes()) {
                        return null;
                    }
                    WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();
                    if (schematicWorld == null) {
                        return null;
                    }
                    Vec3d eyePos = this.mc.player.getEyePos();
                    double d = 1.7976931348623157E308;
                    Mutable mutable = new Mutable();
                    int i = 0;
                    int j = 0;
                    int k = 0;
                    for (SchematicManager.ActiveBox activeBox : this.activeBoxes) {
                        for (int m = activeBox.minX(); m <= activeBox.maxX(); m++) {
                            for (int n = activeBox.minY(); n <= activeBox.maxY(); n++) {
                                for (int o = activeBox.minZ(); o <= activeBox.maxZ(); o++) {
                                    i++;
                                    mutable.set(m, n, o);
                                    BlockState blockState = schematicWorld.getBlockState(mutable);
                                    if (!blockState.isAir()
                                            && BlockStateUtil.isPrimaryPlacementState(blockState)) {
                                        j++;
                                        BlockState blockState2;
                                        if ((bl
                                                        || BlockStateUtil.isSimpleFullBlock(
                                                                blockState,
                                                                schematicWorld,
                                                                mutable))
                                                && BlockStateUtil.classify(
                                                                blockState,
                                                                blockState2 =
                                                                        this.mc.world.getBlockState(
                                                                                mutable))
                                                        == TargetStatus.MISSING) {
                                            k++;
                                            BlockPos pos = mutable.toImmutable();
                                            double squaredDistance =
                                                    pos.toCenterPos().squaredDistanceTo(eyePos);
                                            if (squaredDistance < d) {
                                                d = squaredDistance;
                                                this.cachedNavTarget =
                                                        new SchematicTarget(
                                                                pos,
                                                                SchematicBlockState.of(blockState),
                                                                blockState2,
                                                                TargetStatus.MISSING,
                                                                squaredDistance);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    EasyPlace.LOG.debug(
                            "NavScan: scanned={} nonAir={} missing={} result={}",
                            i,
                            j,
                            k,
                            this.cachedNavTarget == null
                                    ? "null"
                                    : this.cachedNavTarget.pos().toShortString());
                } catch (Exception exception) {
                    EasyPlace.LOG.warn(
                            "NavScan error: {}: {}",
                            exception.getClass().getSimpleName(),
                            exception.getMessage());
                }
                return this.cachedNavTarget;
            }
        } else {
            return null;
        }
    }

    @Environment(EnvType.CLIENT)
    public record ActiveBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

        private static SchematicManager.ActiveBox of(BlockPos pos, BlockPos pos2) {
            return new SchematicManager.ActiveBox(
                    Math.min(pos.getX(), pos2.getX()),
                    Math.min(pos.getY(), pos2.getY()),
                    Math.min(pos.getZ(), pos2.getZ()),
                    Math.max(pos.getX(), pos2.getX()),
                    Math.max(pos.getY(), pos2.getY()),
                    Math.max(pos.getZ(), pos2.getZ()));
        }

        public boolean contains(BlockPos pos) {
            return pos.getX() >= this.minX
                    && pos.getX() <= this.maxX
                    && pos.getY() >= this.minY
                    && pos.getY() <= this.maxY
                    && pos.getZ() >= this.minZ
                    && pos.getZ() <= this.maxZ;
        }
    }
}
