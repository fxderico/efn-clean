package net.efn.client.easyplace.builder;

import net.efn.client.easyplace.util.StateRules;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FallingBlock;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
final class StandPlanner {

    private static final double PILLAR_COST = 8.0;

    private static final double BRIDGE_COST = 5.0;

    private static final double BUILD_BRIDGE_COST = 2.0;

    private static final double PROP_PENALTY = 8.0;

    private static final double LEAP_COST = 2.5;

    private static final double WADE_COST = 2.5;

    private static final double ICE_COST = 2.0;

    private static final int MAX_GAP = 1;

    private static final double FALL_COST = 40.0;

    private static final double DROP_COST = 2.0;

    private static final int BRIDGE_LIMIT = 10;

    private static final double EYE = 1.62;

    private static final double CROUCH_EYE = 1.27;

    private static final int MAX_SPOTS = 12000;

    private static final double SLACK = 10.0;

    private static final double GAIN = 2.0;

    private static final int MAX_CANDIDATES = 16;

    private static final int COUNT_CAP = 24;

    private static final int RANGE = 40;

    private static final int AROUND = 8;

    private static final double[][] FACE_POINTS =
            new double[][] {{0.5, 0.5}, {0.2, 0.5}, {0.8, 0.5}, {0.5, 0.2}, {0.5, 0.8}};

    private static final double[][] EDGE_POINTS =
            new double[][] {{0.5, 0.5}, {0.03, 0.5}, {0.97, 0.5}, {0.5, 0.03}, {0.5, 0.97}};

    private static final int MAX_LEAVES = 10;

    private static final long[] NO_LEAVES = new long[0];

    private final ClientWorld level;

    private final ClientPlayerEntity player;

    private final List<StandPlanner.Target> targets;

    private final Predicate<BlockPos> avoid;

    private final double reach;

    private final StandPlanner.Pillar canPillar;

    private final Predicate<BlockPos> canBridge;

    private final StandPlanner.Exact exact;

    private final StandPlanner.Support support;

    private double maxFall;

    private final int maxFeetY;

    private final Box around;

    private final Box targetBox;

    private final BlockPos start;

    private final PriorityQueue<StandPlanner.Node> open =
            new PriorityQueue<>(Comparator.comparingDouble(StandPlanner.Node::rank));

    private final Map<Long, Double> best = new HashMap<>();

    private final Map<Long, Long> parent = new HashMap<>();

    private final Map<Long, StandPlanner.Move> moves = new HashMap<>();

    private final Map<Long, Integer> bridges = new HashMap<>();

    private final Map<Long, long[]> leaves = new HashMap<>();

    private final Map<Long, StandPlanner.Target> bridgeTargets = new HashMap<>();

    private int visited;

    private List<StandPlanner.Step> route;

    private StandPlanner.Target found;

    private BlockPos bestSpot;

    private double bestValue = -1.7976931348623157E308;

    private double firstCost = -1.0;

    private int candidates;

    private boolean propped;

    private BlockPos closest;

    private double closestDistance = 1.7976931348623157E308;

    private final double startDistance;

    StandPlanner(
            ClientWorld level2,
            ClientPlayerEntity player2,
            List<StandPlanner.Target> targets2,
            Predicate<BlockPos> avoid2,
            StandPlanner.Pillar canPillar2,
            Predicate<BlockPos> canBridge2,
            StandPlanner.Exact exact2,
            StandPlanner.Support support2) {
        this.level = level2;
        this.player = player2;
        this.targets = targets2;
        this.avoid = avoid2;
        this.canPillar = canPillar2;
        this.canBridge = canBridge2;
        this.exact = exact2;
        this.support = support2;
        this.reach = player2.getBlockInteractionRange() - 0.25;
        this.start = player2.getBlockPos();
        for (StandPlanner.Target target : targets2) {
            BlockState state = target.expected();
            if (target.use() == StandPlanner.Use.PLACE
                    && StateRules.lookFor(state) == null
                    && StateRules.flatLookFor(state) == null
                    && !(state.getBlock() instanceof FallingBlock)
                    && state.isFullCube(level2, target.pos())) {
                this.bridgeTargets.put(target.pos().asLong(), target);
            }
        }
        Box box = null;
        for (StandPlanner.Target target2 : targets2) {
            box = box == null ? new Box(target2.pos()) : box.union(new Box(target2.pos()));
        }
        this.maxFeetY = box == null ? this.start.getY() : (int) box.maxY + 1;
        this.around =
                box == null ? new Box(this.start) : box.union(new Box(this.start)).expand(8.0);
        this.targetBox = box == null ? new Box(this.start) : box.expand(8.0);
        this.startDistance = this.workDistance(this.start);
        this.open.add(new StandPlanner.Node(this.start.asLong(), 0.0, 0.0));
        this.best.put(this.start.asLong(), 0.0);
        this.parent.put(this.start.asLong(), -9223372036854775808L);
        this.leaves.put(this.start.asLong(), NO_LEAVES);
    }

    int visited() {
        return this.visited;
    }

    int lowestTargetY() {
        return (int) Math.round(this.targetBox.minY) + 8;
    }

    String extent() {
        int i = 2147483647;
        int j = -2147483648;
        int k = 2147483647;
        int m = -2147483648;
        int n = 2147483647;
        int o = -2147483648;
        int p = 0;
        int q = 0;
        for (Entry entry : this.moves.entrySet()) {
            BlockPos pos2 = BlockPos.fromLong((Long) entry.getKey());
            i = Math.min(i, pos2.getX());
            j = Math.max(j, pos2.getX());
            k = Math.min(k, pos2.getY());
            m = Math.max(m, pos2.getY());
            n = Math.min(n, pos2.getZ());
            o = Math.max(o, pos2.getZ());
            if (entry.getValue() == StandPlanner.Move.PILLAR) {
                p++;
            }
            if (entry.getValue() == StandPlanner.Move.BRIDGE) {
                q++;
            }
        }
        return "x "
                + i
                + ".."
                + j
                + " y "
                + k
                + ".."
                + m
                + " z "
                + n
                + ".."
                + o
                + " pillars "
                + p
                + " bridges "
                + q
                + " targetBox "
                + this.targetBox
                + " maxFeetY "
                + this.maxFeetY
                + " | "
                + this.whyStuck();
    }

    String whyStuck() {
        boolean bl = this.around.contains(this.start.toCenterPos());
        StringBuilder sb =
                new StringBuilder(
                        "near="
                                + bl
                                + " around="
                                + this.around
                                + " maxFeetY="
                                + this.maxFeetY
                                + " avoidUp="
                                + this.avoid.test(this.start.up(2)));
        if (this.canPillar != null) {
            sb.append(" pillar=").append(this.canPillar.possible(this.start, false));
        }
        sb.append(" floor=").append(Walker.floor(this.level, this.start));
        if (this.canBridge != null) {
            for (Direction direction : Type.HORIZONTAL) {
                BlockPos pos2 = this.start.offset(direction);
                sb.append(' ')
                        .append(direction)
                        .append(":side=")
                        .append(
                                Block.isFaceFullSquare(
                                        this.level
                                                .getBlockState(this.start.down())
                                                .getOutlineShape(this.level, this.start.down()),
                                        direction))
                        .append(",gap=")
                        .append(Double.isNaN(Walker.floor(this.level, pos2)))
                        .append(",room=")
                        .append(Walker.roomAt(this.level, pos2))
                        .append(",fits=")
                        .append(this.canBridge.test(pos2.down()))
                        .append(",avoid=")
                        .append(this.avoid.test(pos2));
            }
        }
        return sb.toString();
    }

    StandPlanner falls(double maxFall2) {
        this.maxFall = maxFall2;
        return this;
    }

    List<StandPlanner.Step> route() {
        return this.route;
    }

    StandPlanner.Target target() {
        return this.found;
    }

    StandPlanner.Status step(long l) {
        while (!this.open.isEmpty()) {
            if (System.nanoTime() > l) {
                return StandPlanner.Status.RUNNING;
            }
            StandPlanner.Node node = this.open.poll();
            if (!(node.cost() > this.best.getOrDefault(node.pos(), 1.7976931348623157E308))) {
                if (this.firstCost >= 0.0
                        && (node.cost() > this.firstCost + 10.0 || this.candidates >= 16)) {
                    return this.finish();
                }
                if (++this.visited > 12000) {
                    return this.finish();
                }
                BlockPos pos2 = BlockPos.fromLong(node.pos());
                if (!pos2.equals(this.start)) {
                    double workDistance = this.workDistance(pos2);
                    if (workDistance < this.closestDistance) {
                        this.closestDistance = workDistance;
                        this.closest = pos2;
                    }
                    long[] longs = this.leaves.getOrDefault(node.pos(), NO_LEAVES);
                    StandPlanner.Target target =
                            this.moves.get(node.pos()) == StandPlanner.Move.BRIDGE
                                    ? this.bridgeTargets.get(pos2.down().asLong())
                                    : null;
                    this.propped = false;
                    if (target == null) {
                        target = this.workFrom(pos2, longs);
                    }
                    boolean propped2 = this.propped;
                    if (target != null) {
                        double d =
                                this.countFrom(pos2, longs) * 2.0
                                        - node.cost()
                                        - (propped2
                                                        && this.bridgeTargets.containsKey(
                                                                target.pos().asLong())
                                                ? 8.0
                                                : 0.0);
                        if (this.firstCost < 0.0) {
                            this.firstCost = node.cost();
                        }
                        this.candidates++;
                        if (d > this.bestValue) {
                            this.bestValue = d;
                            this.bestSpot = pos2;
                            this.found = target;
                        }
                    }
                }
                for (BlockPos pos3 : Walker.steps(this.level, pos2)) {
                    int i = pos3.getY() - pos2.getY();
                    double e =
                            (this.level.getFluidState(pos3).isEmpty() ? 0.0 : 2.5)
                                    + (Walker.icy(this.level, pos3) ? 2.0 : 0.0);
                    this.relax(
                            pos2,
                            pos3,
                            node.cost() + 1.0 + (i > 0 ? 0.6 : -i * 2.0) + e,
                            StandPlanner.Move.WALK);
                }
                if (this.maxFall > 0.0) {
                    for (Direction direction : Type.HORIZONTAL) {
                        BlockPos pos4 = Walker.fall(this.level, pos2, direction, this.maxFall);
                        if (pos4 != null) {
                            this.relax(
                                    pos2,
                                    pos4,
                                    node.cost() + 40.0 + (pos2.getY() - pos4.getY()) * 2.0,
                                    StandPlanner.Move.WALK);
                        }
                    }
                }
                double floor = Walker.floorOr(this.level, pos2);
                for (Direction direction2 : Type.HORIZONTAL) {
                    for (int j = 1; j <= 1; j++) {
                        if (Walker.leap(this.level, pos2, floor, direction2, j)) {
                            this.relax(
                                    pos2,
                                    pos2.offset(direction2, j + 1),
                                    node.cost() + 2.5 + j,
                                    StandPlanner.Move.LEAP);
                        }
                    }
                }
                boolean bl =
                        this.around.contains(pos2.toCenterPos())
                                || pos2.getManhattanDistance(this.start) <= 4;
                boolean bl2 =
                        bl
                                || pos2.getY() < this.targetBox.minY
                                        && pos2.getX() + 0.5 >= this.targetBox.minX
                                        && pos2.getX() + 0.5 <= this.targetBox.maxX
                                        && pos2.getZ() + 0.5 >= this.targetBox.minZ
                                        && pos2.getZ() + 0.5 <= this.targetBox.maxZ;
                boolean bl3 =
                        this.moves.get(node.pos()) == StandPlanner.Move.PILLAR
                                || this.moves.get(node.pos()) == StandPlanner.Move.BRIDGE;
                if (bl2
                        && this.canPillar != null
                        && pos2.getY() + 1 <= this.maxFeetY
                        && this.canPillar.possible(pos2, bl3)) {
                    if (pos2.getY() + 2 <= this.maxFeetY) {
                        this.relax(pos2, pos2.up(2), node.cost() + 8.0, StandPlanner.Move.PILLAR);
                    }
                    if (pos2.getY() + 1 >= this.lowestTargetY()) {
                        this.relax(pos2, pos2.up(1), node.cost() + 4.8, StandPlanner.Move.PILLAR);
                    }
                }
                if (bl
                        && pos2.getY() >= this.targetBox.minY
                        && this.canBridge != null
                        && this.bridges.getOrDefault(node.pos(), 0) < 10) {
                    this.bridgeFrom(pos2, node.cost());
                }
            }
        }
        return this.finish();
    }

    private StandPlanner.Status finish() {
        if (this.bestSpot == null) {
            if (this.closest != null && !(this.closestDistance > this.startDistance - 2.0)) {
                this.route = this.path(this.closest);
                return StandPlanner.Status.APPROACH;
            } else {
                return StandPlanner.Status.NONE;
            }
        } else {
            this.route = this.path(this.bestSpot);
            return StandPlanner.Status.FOUND;
        }
    }

    private int countFrom(BlockPos pos2, long[] longs) {
        double floor = Walker.floorOr(this.level, pos2);
        Vec3d vec = new Vec3d(pos2.getX() + 0.5, floor + 1.62, pos2.getZ() + 0.5);
        Box box =
                new Box(
                        pos2.getX() + 0.2,
                        floor,
                        pos2.getZ() + 0.2,
                        pos2.getX() + 0.8,
                        floor + 1.8,
                        pos2.getZ() + 0.8);
        int i = 0;
        Iterator iterator = this.targets.iterator();
        while (iterator.hasNext()) {
            StandPlanner.Target target;
            BlockPos pos3;
            if (!((pos3 = (target = (StandPlanner.Target) iterator.next()).pos())
                                    .toCenterPos()
                                    .distanceTo(vec)
                            > Math.min(this.reach, target.within()))
                    && !box.intersects(new Box(pos3))
                    && (target.use() == StandPlanner.Use.PLACE
                            ? this.seenFrom(vec, target) || this.onPlanned(vec, target, longs)
                            : this.placeableFrom(vec, target, longs))) {
                if (++i >= 24) {
                    break;
                }
            }
        }
        return i;
    }

    private boolean seenFrom(Vec3d vec, StandPlanner.Target target) {
        Direction direction = StateRules.lookFor(target.expected());
        Direction direction2 = StateRules.flatLookFor(target.expected());
        for (Direction direction3 : Direction.values()) {
            BlockPos pos2 = target.pos().offset(direction3);
            BlockState blockState2 = this.level.getBlockState(pos2);
            VoxelShape outlineShape;
            if (!blockState2.isReplaceable()
                    && !(outlineShape = blockState2.getOutlineShape(this.level, pos2)).isEmpty()) {
                Direction opposite = direction3.getOpposite();
                Box box = outlineShape.getBoundingBox().offset(pos2);
                if (this.seen(vec, pos2, box, opposite, direction, direction2)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void bridgeFrom(BlockPos pos2, double d) {
        StandPlanner.Move move2 = this.moves.get(pos2.asLong());
        boolean bl = move2 == StandPlanner.Move.BRIDGE || move2 == StandPlanner.Move.PILLAR;
        if (bl || Walker.floor(this.level, pos2) == pos2.getY()) {
            BlockPos pos3 = pos2.down();
            for (Direction direction : Type.HORIZONTAL) {
                boolean bl2;
                BlockPos pos4;
                if ((bl
                                || Block.isFaceFullSquare(
                                        this.level
                                                .getBlockState(pos3)
                                                .getOutlineShape(this.level, pos3),
                                        direction))
                        && Double.isNaN(Walker.floor(this.level, pos4 = pos2.offset(direction)))
                        && Walker.roomAt(this.level, pos4)
                        && ((bl2 = this.bridgeTargets.containsKey(pos4.down().asLong()))
                                || this.canBridge.test(pos4.down()))) {
                    this.relax(pos2, pos4, d + (bl2 ? 2.0 : 5.0), StandPlanner.Move.BRIDGE);
                }
            }
        }
    }

    private void relax(BlockPos pos2, BlockPos pos3, double d, StandPlanner.Move move2) {
        if (!this.avoid.test(pos3)) {
            if (Math.abs(pos3.getX() - this.start.getX()) <= 40
                    && Math.abs(pos3.getZ() - this.start.getZ()) <= 40
                    && Math.abs(pos3.getY() - this.start.getY()) <= 40
                    && pos3.getY() <= this.maxFeetY + 1) {
                long l = pos3.asLong();
                if (!(d >= this.best.getOrDefault(l, 1.7976931348623157E308))) {
                    this.best.put(l, d);
                    this.parent.put(l, pos2.asLong());
                    this.moves.put(l, move2);
                    this.bridges.put(
                            l,
                            this.bridges.getOrDefault(pos2.asLong(), 0)
                                    + (move2 == StandPlanner.Move.BRIDGE ? 1 : 0));
                    long[] longs = this.leaves.getOrDefault(pos2.asLong(), NO_LEAVES);
                    this.leaves.put(
                            l,
                            move2 != StandPlanner.Move.BRIDGE && move2 != StandPlanner.Move.PILLAR
                                    ? longs
                                    : withLeaf(
                                            longs, pos3.down(), move2 == StandPlanner.Move.PILLAR));
                    this.open.add(new StandPlanner.Node(l, d, d + this.guess(pos3)));
                }
            }
        }
    }

    private double workDistance(BlockPos pos2) {
        Vec3d vec = new Vec3d(pos2.getX() + 0.5, pos2.getY() + 1.62, pos2.getZ() + 0.5);
        double d = 1.7976931348623157E308;
        for (int i = 0; i < Math.min(32, this.targets.size()); i++) {
            d = Math.min(d, this.targets.get(i).pos().toCenterPos().distanceTo(vec));
        }
        return d;
    }

    private double guess(BlockPos pos2) {
        Vec3d vec = new Vec3d(pos2.getX() + 0.5, pos2.getY() + 1.62, pos2.getZ() + 0.5);
        double d =
                Math.max(
                        0.0,
                        Math.max(
                                this.targetBox.minX + 8.0 - vec.x,
                                vec.x - (this.targetBox.maxX - 8.0)));
        double e =
                Math.max(
                                0.0,
                                Math.max(
                                        this.targetBox.minY + 8.0 - vec.y,
                                        vec.y - (this.targetBox.maxY - 8.0)))
                        * 6.5;
        double d2 =
                Math.max(
                        0.0,
                        Math.max(
                                this.targetBox.minZ + 8.0 - vec.z,
                                vec.z - (this.targetBox.maxZ - 8.0)));
        return Math.max(0.0, Math.sqrt(d * d + e * e + d2 * d2) - this.reach) * 0.6;
    }

    private List<StandPlanner.Step> path(BlockPos pos2) {
        ArrayList list = new ArrayList();
        long l = pos2.asLong();
        while (l != this.start.asLong() && l != -9223372036854775808L) {
            list.add(
                    new StandPlanner.Step(
                            BlockPos.fromLong(l),
                            this.moves.getOrDefault(l, StandPlanner.Move.WALK)));
            l = this.parent.get(l);
        }
        Collections.reverse(list);
        return list;
    }

    private StandPlanner.Target workFrom(BlockPos pos2, long[] longs) {
        if ((this.level.getFluidState(pos2).isEmpty() || this.level.getFluidState(pos2).isStill())
                && !Walker.nearLava(this.level, pos2)) {
            double floor = Walker.floorOr(this.level, pos2);
            Vec3d vec = new Vec3d(pos2.getX() + 0.5, floor + 1.62, pos2.getZ() + 0.5);
            Box box =
                    new Box(
                            pos2.getX() + 0.2,
                            floor,
                            pos2.getZ() + 0.2,
                            pos2.getX() + 0.8,
                            floor + 1.8,
                            pos2.getZ() + 0.8);
            for (StandPlanner.Target target : this.targets) {
                BlockPos pos3 = target.pos();
                if (!(pos3.toCenterPos().squaredDistanceTo(vec)
                                > (this.reach + 0.9) * (this.reach + 0.9))
                        && !(pos3.toCenterPos().distanceTo(vec) > target.within())
                        && (!box.intersects(new Box(pos3))
                                || target.use() == StandPlanner.Use.PLACE
                                        && target.expected()
                                                .getCollisionShape(this.level, pos3)
                                                .isEmpty())
                        && this.placeableFrom(vec, target, longs)) {
                    return target;
                }
            }
            return null;
        } else {
            return null;
        }
    }

    private boolean placeableFrom(Vec3d vec, StandPlanner.Target target, long[] longs) {
        BlockPos pos2 = target.pos();
        switch (target.use()) {
            case WATERLOG:
            case BREAK:
            case CLICK:
                if (pos2.toCenterPos().distanceTo(vec) > this.reach) {
                    return false;
                } else {
                    VoxelShape outlineShape =
                            this.level.getBlockState(pos2).getOutlineShape(this.level, pos2);
                    if (outlineShape.isEmpty()) {
                        return false;
                    } else {
                        Box box = outlineShape.getBoundingBox().offset(pos2);
                        for (Direction direction : Direction.values()) {
                            if (facesEye(box, direction, vec)
                                    && this.seen(vec, pos2, box, direction, null, null)) {
                                return true;
                            }
                        }
                        return false;
                    }
                }
            case NEAR:
                return pos2.toCenterPos().distanceTo(vec) <= this.reach - 0.5
                        && this.support != null
                        && this.support.possible(pos2, vec);
            default:
                Direction direction2 =
                        target.use() == StandPlanner.Use.PLACE
                                ? StateRules.lookFor(target.expected())
                                : null;
                Direction direction3 =
                        target.use() == StandPlanner.Use.PLACE
                                ? StateRules.flatLookFor(target.expected())
                                : null;
                for (Direction direction4 : Direction.values()) {
                    BlockPos pos3 = pos2.offset(direction4);
                    BlockState blockState2 = this.level.getBlockState(pos3);
                    VoxelShape outlineShape2;
                    if (!blockState2.isReplaceable()
                            && !(outlineShape2 = blockState2.getOutlineShape(this.level, pos3))
                                    .isEmpty()) {
                        Direction opposite = direction4.getOpposite();
                        Box box2 = outlineShape2.getBoundingBox().offset(pos3);
                        if (this.seen(vec, pos3, box2, opposite, direction2, direction3)
                                && (target.use() != StandPlanner.Use.PLACE
                                        || this.exact == null
                                        || this.exact.placeable(target.expected(), pos2, vec))) {
                            return true;
                        }
                    }
                }
                if (this.onPlanned(vec, target, longs)) {
                    return true;
                } else if (target.use() != StandPlanner.Use.PLACE) {
                    return false;
                } else if (StateRules.slabHalf(this.level.getBlockState(pos2), target.expected())) {
                    return this.exact != null && this.exact.placeable(target.expected(), pos2, vec);
                } else {
                    this.propped =
                            this.support != null
                                    && this.support.behind(target.expected(), pos2, vec);
                    return this.propped;
                }
        }
    }

    private boolean onPlanned(Vec3d vec, StandPlanner.Target target, long[] longs) {
        if (longs.length == 0) {
            return false;
        } else if (target.use() != StandPlanner.Use.PLACE
                || StateRules.lookFor(target.expected()) == null
                        && StateRules.flatLookFor(target.expected()) == null) {
            BlockPos pos2 = target.pos();
            for (long l : longs) {
                BlockPos pos3 = BlockPos.fromLong(l);
                Direction facing;
                Box box;
                if (pos3.getManhattanDistance(pos2) == 1
                        && this.level.getBlockState(pos3).isReplaceable()
                        && facesEye(
                                box = new Box(pos3),
                                facing =
                                        Direction.getFacing(
                                                (float) (pos2.getX() - pos3.getX()),
                                                (float) (pos2.getY() - pos3.getY()),
                                                (float) (pos2.getZ() - pos3.getZ())),
                                vec)) {
                    for (double[] doubles : FACE_POINTS) {
                        Vec3d vec2 = facePoint(box, facing, doubles[0], doubles[1]);
                        if (!(vec2.distanceTo(vec) > this.reach)
                                && this.level
                                                .raycast(
                                                        new RaycastContext(
                                                                vec,
                                                                vec2.subtract(
                                                                        vec2.subtract(vec)
                                                                                .normalize()
                                                                                .multiply(0.05)),
                                                                ShapeType.OUTLINE,
                                                                FluidHandling.NONE,
                                                                this.player))
                                                .getType()
                                        == net.minecraft.util.hit.HitResult.Type.MISS) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } else {
            return false;
        }
    }

    private static long[] withLeaf(long[] longs, BlockPos pos2, boolean bl) {
        long[] longs2 = new long[Math.min(longs.length + 1, 10)];
        int i = 0;
        for (int j = Math.max(0, longs.length + 1 - 10); j < longs.length; j++) {
            BlockPos pos3 = BlockPos.fromLong(longs[j]);
            if (!bl
                    || pos3.getX() != pos2.getX()
                    || pos3.getZ() != pos2.getZ()
                    || pos3.getY() >= pos2.getY()) {
                longs2[i++] = longs[j];
            }
        }
        longs2[i++] = pos2.asLong();
        return i == longs2.length ? longs2 : Arrays.copyOf(longs2, i);
    }

    private boolean seen(
            Vec3d vec,
            BlockPos pos2,
            Box box,
            Direction direction,
            Direction direction2,
            Direction direction3) {
        return this.seenFrom(vec, pos2, box, direction, direction2, direction3, FACE_POINTS, 0.2)
                ? true
                : direction2 == Direction.UP
                        && this.seenFrom(
                                vec.subtract(0.0, 0.3500000000000001, 0.0),
                                pos2,
                                box,
                                direction,
                                direction2,
                                direction3,
                                EDGE_POINTS,
                                0.05);
    }

    private boolean seenFrom(
            Vec3d vec,
            BlockPos pos2,
            Box box,
            Direction direction,
            Direction direction2,
            Direction direction3,
            double[][] doubleGrid,
            double d) {
        if (!facesEye(box, direction, vec)) {
            return false;
        } else {
            for (double[] doubles : doubleGrid) {
                Vec3d vec2 = facePoint(box, direction, doubles[0], doubles[1]);
                BlockHitResult blockHitResult;
                if (!(vec2.distanceTo(vec) > this.reach)
                        && (direction2 == null
                                || Walker.dominant(vec2.subtract(vec), direction2, d))
                        && (direction3 == null || Walker.facingFlat(vec2.subtract(vec), direction3))
                        && (blockHitResult =
                                                this.level.raycast(
                                                        new RaycastContext(
                                                                vec,
                                                                vec2.add(
                                                                        vec2.subtract(vec)
                                                                                .normalize()
                                                                                .multiply(0.05)),
                                                                ShapeType.OUTLINE,
                                                                FluidHandling.NONE,
                                                                this.player)))
                                        .getType()
                                == net.minecraft.util.hit.HitResult.Type.BLOCK
                        && blockHitResult.getBlockPos().equals(pos2)
                        && blockHitResult.getSide() == direction) {
                    return true;
                }
            }
            return false;
        }
    }

    private static boolean facesEye(Box box, Direction direction, Vec3d vec) {
        double d = 0.05;
        return switch (direction) {
            case UP -> vec.y > box.maxY + d;
            case DOWN -> vec.y < box.minY - d;
            case NORTH -> vec.z < box.minZ - d;
            case SOUTH -> vec.z > box.maxZ + d;
            case WEST -> vec.x < box.minX - d;
            case EAST -> vec.x > box.maxX + d;
        };
    }

    private static Vec3d facePoint(Box box, Direction direction, double d, double e) {
        double d2 = box.minX + (box.maxX - box.minX) * d;
        double e2 = box.minY + (box.maxY - box.minY) * e;
        return switch (direction) {
            case UP -> new Vec3d(d2, box.maxY, box.minZ + (box.maxZ - box.minZ) * e);
            case DOWN -> new Vec3d(d2, box.minY, box.minZ + (box.maxZ - box.minZ) * e);
            case NORTH -> new Vec3d(d2, e2, box.minZ);
            case SOUTH -> new Vec3d(d2, e2, box.maxZ);
            case WEST -> new Vec3d(box.minX, e2, box.minZ + (box.maxZ - box.minZ) * d);
            case EAST -> new Vec3d(box.maxX, e2, box.minZ + (box.maxZ - box.minZ) * d);
        };
    }

    @Environment(EnvType.CLIENT)
    interface Exact {

        boolean placeable(BlockState state, BlockPos pos2, Vec3d vec);
    }

    @Environment(EnvType.CLIENT)
    static enum Move {
        WALK,
        PILLAR,
        BRIDGE,
        LEAP;

        private Move() {}
    }

    @Environment(EnvType.CLIENT)
    private record Node(long pos, double cost, double rank) {}

    @Environment(EnvType.CLIENT)
    interface Pillar {

        boolean possible(BlockPos pos2, boolean bl);
    }

    @Environment(EnvType.CLIENT)
    static enum Status {
        RUNNING,
        FOUND,
        NONE,
        APPROACH;

        private Status() {}
    }

    @Environment(EnvType.CLIENT)
    record Step(BlockPos pos, StandPlanner.Move move) {}

    @Environment(EnvType.CLIENT)
    interface Support {

        boolean possible(BlockPos pos2, Vec3d vec);

        boolean behind(BlockState state, BlockPos pos2, Vec3d vec);
    }

    @Environment(EnvType.CLIENT)
    record Target(BlockPos pos, BlockState expected, StandPlanner.Use use, double within) {

        Target(BlockPos blockPos, BlockState blockState, StandPlanner.Use use) {
            this(blockPos, blockState, use, 1.7976931348623157E308);
        }
    }

    @Environment(EnvType.CLIENT)
    static enum Use {
        PLACE,
        POUR,
        WATERLOG,
        NEAR,
        BREAK,
        CLICK;

        private Use() {}
    }
}
