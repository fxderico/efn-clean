package net.efn.client.easyplace.builder;

import fi.dy.masa.litematica.world.WorldSchematic;

import net.efn.client.easyplace.schematic.SchematicManager;
import net.efn.client.easyplace.schematic.TargetStatus;
import net.efn.client.easyplace.util.BlockStateUtil;
import net.efn.client.easyplace.util.StateRules;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.CrafterBlock;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.TntBlock;
import net.minecraft.block.entity.CrafterBlockEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
final class BuildScan {

    static final int NONE = 2147483647;

    final List<BuildScan.Cell> missing = new ArrayList<>();

    final List<BuildScan.Cell> adjust = new ArrayList<>();

    final List<BuildScan.Liquid> liquids = new ArrayList<>();

    final Set<Long> waiting = new HashSet<>();

    final List<BlockPos> poweredTnt = new ArrayList<>();

    final List<BlockPos> tntForPlayer = new ArrayList<>();

    final List<BlockPos> afterTntForPlayer = new ArrayList<>();

    private final List<BuildScan.Cell> tntCells = new ArrayList<>();

    private boolean powerLeft;

    private final Set<Long> waterBelow = new HashSet<>();

    private final Map<Long, Integer> staged = new HashMap<>();

    private final List<BuildScan.Cell> lateCells = new ArrayList<>();

    private boolean machineLast;

    int late;

    int leversOff;

    final List<BlockPos> levers = new ArrayList<>();

    int railsOff;

    final List<BlockPos> crafters = new ArrayList<>();

    final Map<BlockPos, boolean[]> crafterSlots = new HashMap<>();

    int layer = 2147483647;

    int wrong;

    int noItem;

    int unloaded;

    boolean wholeSchematic = true;

    private boolean placeTnt;

    BuildScan() {}

    static BuildScan run(
            ClientWorld world,
            WorldSchematic worldSchematic,
            List<SchematicManager.ActiveBox> list,
            Predicate<BlockPos> predicate,
            Predicate<BlockPos> predicate2,
            Predicate<BlockPos> predicate3,
            boolean bl,
            Set<Long> set,
            Set<Long> set2,
            boolean bl2,
            boolean bl3,
            boolean bl4) {
        BuildScan buildScan = new BuildScan();
        buildScan.placeTnt = bl3;
        buildScan.machineLast = bl4;
        if (list.isEmpty()) {
            return buildScan;
        } else {
            int i = 2147483647;
            int j = -2147483648;
            for (SchematicManager.ActiveBox activeBox : list) {
                i = Math.min(i, activeBox.minY());
                j = Math.max(j, activeBox.maxY());
            }
            Mutable mutable = new Mutable();
            int k = i;
            do {
                for (buildScan.wholeSchematic = true; k <= j; k++) {
                    if (buildScan.layer != 2147483647 && k > buildScan.layer + 1) {
                        buildScan.wholeSchematic = false;
                        break;
                    }
                    for (SchematicManager.ActiveBox activeBox2 : list) {
                        if (k >= activeBox2.minY() && k <= activeBox2.maxY()) {
                            for (int m = activeBox2.minX(); m <= activeBox2.maxX(); m++) {
                                for (int n = activeBox2.minZ(); n <= activeBox2.maxZ(); n++) {
                                    mutable.set(m, k, n);
                                    buildScan.visit(
                                            world,
                                            worldSchematic,
                                            mutable,
                                            predicate,
                                            predicate2,
                                            predicate3,
                                            bl,
                                            set,
                                            set2,
                                            bl2);
                                }
                            }
                        }
                    }
                }
                if (buildScan.wholeSchematic) {
                    break;
                }
                buildScan.keepApproachesOpen(world, worldSchematic, predicate2, predicate);
            } while (buildScan.layer == 2147483647);
            buildScan.releaseStages(predicate2);
            buildScan.releaseTnt(world, worldSchematic, predicate, predicate2, set);
            buildScan.keepApproachesOpen(world, worldSchematic, predicate2, predicate);
            return buildScan;
        }
    }

    private void releaseStages(Predicate<BlockPos> predicate) {
        int i = 2147483647;
        for (Entry entry : this.staged.entrySet()) {
            if (!predicate.test(BlockPos.fromLong((Long) entry.getKey()))) {
                i = Math.min(i, (Integer) entry.getValue());
            }
        }
        for (Entry entry2 : this.staged.entrySet()) {
            if ((Integer) entry2.getValue() > i) {
                this.waiting.add((Long) entry2.getKey());
            }
        }
    }

    private void keepApproachesOpen(
            ClientWorld world,
            WorldSchematic worldSchematic,
            Predicate<BlockPos> predicate,
            Predicate<BlockPos> predicate2) {
        HashSet set = new HashSet();
        for (BuildScan.Cell cell : this.missing) {
            set.add(cell.pos().asLong());
        }
        HashSet set2 = new HashSet();
        for (BuildScan.Cell lateCell : this.lateCells) {
            set.add(lateCell.pos().asLong());
            set2.add(lateCell.pos().asLong());
        }
        set2.addAll(this.staged.keySet());
        ArrayList list = new ArrayList<>(this.missing);
        list.addAll(this.lateCells);
        for (BuildScan.Cell cell2 : (Iterable<BuildScan.Cell>) list) {
            if (!this.waiting.contains(cell2.pos().asLong())
                    || set2.contains(cell2.pos().asLong())) {
                if (StateRules.lookFor(cell2.expected()) == Direction.UP) {
                    BlockPos pos2 = lowStand(world, worldSchematic, cell2.pos(), set);
                    if (pos2 != null) {
                        if (set.contains(pos2.asLong())) {
                            this.waiting.add(pos2.asLong());
                        }
                        if (set.contains(pos2.up().asLong())) {
                            this.waiting.add(pos2.up().asLong());
                        }
                    } else {
                        this.waiting.add(cell2.pos().asLong());
                    }
                } else {
                    BlockPos pos3;
                    if (!predicate.test(cell2.pos())
                            && (pos3 = approach(cell2.expected(), cell2.pos())) != null
                            && set.contains(pos3.asLong())) {
                        BlockPos pos4 = approach(worldSchematic.getBlockState(pos3), pos3);
                        if (!cell2.pos().equals(pos4)) {
                            this.waiting.add(pos3.asLong());
                        }
                    }
                }
            }
        }
        this.layer = 2147483647;
        for (BuildScan.Cell cell3 : this.missing) {
            if (!this.waiting.contains(cell3.pos().asLong()) && !predicate2.test(cell3.pos())) {
                this.layer = Math.min(this.layer, cell3.pos().getY());
            }
        }
    }

    private static BlockPos lowStand(
            ClientWorld world, WorldSchematic worldSchematic, BlockPos pos2, Set<Long> set) {
        BlockPos pos3 = null;
        int i = -1;
        for (Direction direction : Type.HORIZONTAL) {
            BlockPos pos4 = pos2.down().offset(direction);
            BlockPos pos5 = pos4.down();
            boolean bl = !world.getBlockState(pos5).getCollisionShape(world, pos5).isEmpty();
            BlockState blockState = worldSchematic.getBlockState(pos5);
            int j;
            if ((bl || !blockState.isAir() && blockState.getFluidState().isEmpty())
                    && freeCell(world, worldSchematic, pos4, set)
                    && freeCell(world, worldSchematic, pos4.up(), set)
                    && StateRules.lookFor(worldSchematic.getBlockState(pos4.up())) != Direction.UP
                    && (j = (bl ? 2 : 0) + (set.contains(pos4.asLong()) ? 0 : 1)) > i) {
                i = j;
                pos3 = pos4;
            }
        }
        return pos3;
    }

    private static boolean freeCell(
            ClientWorld world, WorldSchematic worldSchematic, BlockPos pos2, Set<Long> set) {
        BlockState blockState = world.getBlockState(pos2);
        if (blockState.isReplaceable() && blockState.getFluidState().isEmpty()) {
            BlockState blockState2 = worldSchematic.getBlockState(pos2);
            return blockState2.getBlock() instanceof FluidBlock
                    ? false
                    : blockState2.isAir() || set.contains(pos2.asLong());
        } else {
            return false;
        }
    }

    private static BlockPos approach(BlockState state, BlockPos pos2) {
        Direction direction = StateRules.lookFor(state);
        if (direction == null) {
            direction = StateRules.flatLookFor(state);
        }
        Direction direction2;
        if (direction == null
                && state.getBlock() instanceof HopperBlock
                && (direction2 = state.get(Properties.HOPPER_FACING)) != Direction.DOWN) {
            direction = direction2;
        }
        return direction == null ? null : pos2.offset(direction.getOpposite());
    }

    static Map<Item, Integer> remainingItems(
            ClientWorld world,
            WorldSchematic worldSchematic,
            List<SchematicManager.ActiveBox> list) {
        HashMap map = new HashMap();
        Mutable mutable = new Mutable();
        for (SchematicManager.ActiveBox activeBox : list) {
            for (int i = activeBox.minY(); i <= activeBox.maxY(); i++) {
                for (int j = activeBox.minX(); j <= activeBox.maxX(); j++) {
                    for (int k = activeBox.minZ(); k <= activeBox.maxZ(); k++) {
                        mutable.set(j, i, k);
                        BlockState blockState = worldSchematic.getBlockState(mutable);
                        if (!blockState.isAir() && world.isChunkLoaded(mutable)) {
                            BlockState blockState2 = world.getBlockState(mutable);
                            CellFiller.Kind kind2 = needsLiquid(blockState, blockState2);
                            if (kind2 != null) {
                                map.merge(
                                        kind2.item, 1, (a, value) -> (Integer) a + (Integer) value);
                            }
                            Item item;
                            if (BlockStateUtil.isPrimaryPlacementState(blockState)
                                    && (item = BlockStateUtil.requiredItem(blockState)) != null
                                    && BlockStateUtil.classify(blockState, blockState2)
                                            == TargetStatus.MISSING) {
                                map.merge(
                                        item,
                                        StateRules.itemsNeeded(blockState2, blockState),
                                        (a, value2) -> (Integer) a + (Integer) value2);
                            }
                        }
                    }
                }
            }
        }
        return map;
    }

    private void releaseTnt(
            ClientWorld world,
            WorldSchematic worldSchematic,
            Predicate<BlockPos> predicate,
            Predicate<BlockPos> predicate2,
            Set<Long> set) {
        if (!this.powerLeft && set.isEmpty()) {
            for (BuildScan.Cell tntCell : this.tntCells) {
                if (BuildOrder.waits(
                        world,
                        worldSchematic,
                        tntCell.pos(),
                        tntCell.expected(),
                        predicate2,
                        this.machineLast)) {
                    this.poweredTnt.add(tntCell.pos());
                } else {
                    this.waiting.remove(tntCell.pos().asLong());
                    if (!predicate.test(tntCell.pos())
                            && (this.layer == 2147483647 || tntCell.pos().getY() < this.layer)) {
                        this.layer = tntCell.pos().getY();
                    }
                }
            }
        }
    }

    private void visit(
            ClientWorld world,
            WorldSchematic worldSchematic,
            Mutable mutable,
            Predicate<BlockPos> predicate,
            Predicate<BlockPos> predicate2,
            Predicate<BlockPos> predicate3,
            boolean bl,
            Set<Long> set,
            Set<Long> set2,
            boolean bl2) {
        BlockState blockState = worldSchematic.getBlockState(mutable);
        if (!blockState.isAir()) {
            if (!world.isChunkLoaded(mutable)) {
                this.unloaded++;
            } else {
                BlockState blockState2 = world.getBlockState(mutable);
                CellFiller.Kind kind2 = needsLiquid(blockState, blockState2);
                if (kind2 != null) {
                    BlockPos pos2 = mutable.toImmutable();
                    boolean bl3 =
                            kind2 == CellFiller.Kind.LAVA_SOURCE && capped(worldSchematic, pos2);
                    this.liquids.add(new BuildScan.Liquid(pos2, kind2, bl3));
                    if ((kind2 != CellFiller.Kind.LAVA_SOURCE || bl3) && predicate3.test(pos2)) {
                        this.waterBelow.add(pos2.asLong());
                    }
                }
                if (blockState.getFluidState().isEmpty()
                        || blockState.getBlock().asItem() != Items.AIR) {
                    if (BlockStateUtil.isPrimaryPlacementState(blockState)) {
                        if (BlockStateUtil.requiredItem(blockState) == null) {
                            this.noItem++;
                        } else {
                            TargetStatus targetStatus =
                                    BlockStateUtil.classify(blockState, blockState2);
                            switch (targetStatus) {
                                case MISSING:
                                    BlockPos pos3 = mutable.toImmutable();
                                    boolean bl4 = blockState.getBlock() instanceof TntBlock;
                                    boolean bl5 =
                                            !bl4
                                                    && BuildOrder.pushedIntoTnt(
                                                                    worldSchematic,
                                                                    pos3,
                                                                    blockState)
                                                            != null;
                                    if (!this.placeTnt && (bl4 || bl5)) {
                                        (bl4 ? this.tntForPlayer : this.afterTntForPlayer)
                                                .add(pos3);
                                        this.waiting.add(pos3.asLong());
                                        return;
                                    }
                                    int stage =
                                            BuildOrder.stage(
                                                    worldSchematic,
                                                    pos3,
                                                    blockState,
                                                    this.machineLast);
                                    if (bl && !bl4 && !bl5 && stage > 0) {
                                        this.powerLeft = true;
                                        this.staged.put(pos3.asLong(), stage);
                                    }
                                    if (bl && bl4) {
                                        this.missing.add(
                                                new BuildScan.Cell(pos3, blockState, blockState2));
                                        this.tntCells.add(
                                                new BuildScan.Cell(pos3, blockState, blockState2));
                                        this.waiting.add(pos3.asLong());
                                        return;
                                    }
                                    if (!bl && stage > 0) {
                                        this.late++;
                                        this.lateCells.add(
                                                new BuildScan.Cell(pos3, blockState, blockState2));
                                        this.waiting.add(pos3.asLong());
                                        return;
                                    }
                                    this.missing.add(
                                            new BuildScan.Cell(pos3, blockState, blockState2));
                                    if (this.waterBelow.contains(pos3.down().asLong())
                                            || BuildOrder.waits(
                                                    world,
                                                    worldSchematic,
                                                    pos3,
                                                    blockState,
                                                    predicate2,
                                                    this.machineLast)) {
                                        this.waiting.add(pos3.asLong());
                                        return;
                                    }
                                    if (this.layer == 2147483647 && !predicate.test(pos3)) {
                                        this.layer = pos3.getY();
                                    }
                                    break;
                                case ADJUST:
                                    this.adjust.add(
                                            new BuildScan.Cell(
                                                    mutable.toImmutable(),
                                                    blockState,
                                                    blockState2));
                                    break;
                                case CORRECT:
                                    if (blockState.getBlock() instanceof LeverBlock
                                            && blockState.get(Properties.POWERED)
                                            && !blockState2.get(Properties.POWERED)) {
                                        this.leversOff++;
                                        this.levers.add(mutable.toImmutable());
                                    }
                                    if (BuildOrder.railShapeOff(blockState2, blockState)) {
                                        this.railsOff++;
                                    }
                                    boolean[] booleans;
                                    if (bl2
                                            && blockState.getBlock() instanceof CrafterBlock
                                            && !set2.contains(mutable.asLong())
                                            && (booleans = disabledSlots(worldSchematic, mutable))
                                                    != null) {
                                        BlockPos pos4 = mutable.toImmutable();
                                        this.crafters.add(pos4);
                                        this.crafterSlots.put(pos4, booleans);
                                    }
                                    break;
                                case WRONG:
                                    if (!set.contains(mutable.asLong())) {
                                        this.wrong++;
                                    }
                            }
                        }
                    }
                }
            }
        }
    }

    static CellFiller.Kind needsLiquid(BlockState state, BlockState state2) {
        FluidState fluidState = state.getFluidState();
        if (!fluidState.isEmpty() && fluidState.isStill()) {
            FluidState fluidState2 = state2.getFluidState();
            if (fluidState.isIn(FluidTags.LAVA)) {
                if (fluidState2.isStill() && fluidState2.isIn(FluidTags.LAVA)) {
                    return null;
                } else {
                    return !state2.isAir()
                                    && (!state2.isReplaceable()
                                            || !fluidState2.isEmpty()
                                                    && !fluidState2.isIn(FluidTags.LAVA))
                            ? null
                            : CellFiller.Kind.LAVA_SOURCE;
                }
            } else if (!fluidState.isIn(FluidTags.WATER)) {
                return null;
            } else if (state.contains(Properties.WATERLOGGED)) {
                return StateRules.placed(state2, state) && !state2.get(Properties.WATERLOGGED)
                        ? CellFiller.Kind.WATERLOG
                        : null;
            } else if (fluidState2.isStill() && fluidState2.isIn(FluidTags.WATER)) {
                return null;
            } else {
                return !state2.isAir() && !state2.isReplaceable()
                        ? null
                        : CellFiller.Kind.WATER_SOURCE;
            }
        } else {
            return null;
        }
    }

    private static boolean capped(WorldSchematic worldSchematic, BlockPos pos2) {
        Mutable mutable = pos2.mutableCopy();
        for (int i = 0; i < 64; i++) {
            mutable.move(Direction.UP);
            BlockState blockState = worldSchematic.getBlockState(mutable);
            if (!blockState.getFluidState().isIn(FluidTags.LAVA)) {
                return !blockState.isAir() && blockState.getFluidState().isEmpty();
            }
        }
        return false;
    }

    private static boolean[] disabledSlots(WorldSchematic worldSchematic, BlockPos pos2) {
        if (!(worldSchematic.getBlockEntity(pos2)
                instanceof CrafterBlockEntity crafterBlockEntity)) {
            return null;
        } else {
            boolean[] booleans = new boolean[9];
            boolean bl = false;
            for (int i = 0; i < 9; i++) {
                booleans[i] = crafterBlockEntity.isSlotDisabled(i);
                bl |= booleans[i];
            }
            return bl ? booleans : null;
        }
    }

    Map<Item, Integer> needs(int i) {
        HashMap map = new HashMap();
        for (BuildScan.Cell cell : this.missing) {
            Item item;
            if (cell.pos().getY() <= i
                    && (item = BlockStateUtil.requiredItem(cell.expected())) != null) {
                map.merge(
                        item,
                        StateRules.itemsNeeded(cell.actual(), cell.expected()),
                        (a, value) -> (Integer) a + (Integer) value);
            }
        }
        return map;
    }

    @Environment(EnvType.CLIENT)
    record Cell(BlockPos pos, BlockState expected, BlockState actual) {}

    @Environment(EnvType.CLIENT)
    record Liquid(BlockPos pos, CellFiller.Kind kind, boolean capped) {}
}
