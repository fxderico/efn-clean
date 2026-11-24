package net.efn.client.easyplace.placement;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.util.LayerRange;

import net.efn.client.easyplace.hopper.HopperPlacementHandler;
import net.efn.client.easyplace.mixin.InvokerBlockItem;
import net.efn.client.easyplace.util.BlockStateUtil;
import net.efn.client.easyplace.util.PlacementUtil;
import net.efn.client.easyplace.util.StateRules;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.ChorusFlowerBlock;
import net.minecraft.block.CropBlock;
import net.minecraft.block.FireBlock;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

import java.util.function.LongPredicate;

@Environment(EnvType.CLIENT)
public final class DependencyManager {

    private static final int SEARCH_RADIUS = 5;

    private final MinecraftClient mc;

    private final HopperPlacementHandler hopperHandler = new HopperPlacementHandler();

    private BuildScope currentScope;

    private double reachSpare;

    private double feetY = 0.0 / 0.0;

    private boolean builderEyes;

    private static final double BUILDER_SPARE = 0.3;

    private static final double REACH_SPARE = 0.6499999999999999;

    private static final double STEADY_SHIFT = 0.02;

    private static final float[][] STEADY_TURNS =
            new float[][] {{0.5F, 0.0F}, {-0.5F, 0.0F}, {0.0F, 0.5F}, {0.0F, -0.5F}};

    public DependencyManager(MinecraftClient mc2) {
        this.mc = mc2;
    }

    public PlacementTarget searchForPlaceables(
            WorldSchematic worldSchematic,
            boolean bl,
            boolean bl2,
            float f,
            double d,
            float g,
            BuildScope currentScope2,
            LongPredicate longPredicate) {
        if (this.mc.player != null && this.mc.world != null && worldSchematic != null) {
            BlockPos pos = this.mc.player.getBlockPos();
            Vec3d vec =
                    new Vec3d(
                            this.mc.player.getX(), this.mc.player.getEyeY(), this.mc.player.getZ());
            this.feetY = this.mc.player.getY();
            this.builderEyes = currentScope2 != null;
            this.reachSpare = currentScope2 != null ? 0.3 : 0.0;
            float yaw = this.mc.player.getYaw();
            PlacementTarget placementTarget = null;
            double e = 1.7976931348623157E308;
            for (int i = -5; i <= 5; i++) {
                for (int j = -5; j <= 5; j++) {
                    for (int k = -5; k <= 5; k++) {
                        int m = pos.getX() + i;
                        int n = pos.getY() + j;
                        int o = pos.getZ() + k;
                        BlockPos pos2 = new BlockPos(m, n, o);
                        BlockState blockState;
                        boolean slabHalf;
                        BlockState blockState2;
                        if ((currentScope2 == null || currentScope2.allows(pos2))
                                && !longPredicate.test(pos2.asLong())
                                && !(blockState2 = worldSchematic.getBlockState(pos2)).isAir()
                                && BlockStateUtil.isPrimaryPlacementState(blockState2)
                                && (currentScope2 != null || this.isInRenderLayer(pos2))
                                && ((slabHalf =
                                                StateRules.slabHalf(
                                                        blockState =
                                                                this.mc.world.getBlockState(pos2),
                                                        blockState2))
                                        || isReplaceable(blockState))
                                && !StateRules.placed(blockState, blockState2)
                                && (slabHalf
                                        || !this.mc
                                                .player
                                                .getBoundingBox()
                                                .intersects(new Box(pos2)))) {
                            this.currentScope = currentScope2;
                            PlacementTarget placementTarget2 =
                                    slabHalf
                                            ? this.secondSlab(
                                                    blockState2,
                                                    blockState,
                                                    pos2,
                                                    vec,
                                                    currentScope2 != null)
                                            : this.possibleToPlace(
                                                    blockState2,
                                                    m,
                                                    n,
                                                    o,
                                                    vec,
                                                    bl,
                                                    currentScope2 != null);
                            if (placementTarget2 != null) {
                                if (bl2) {
                                    double d2 = m + 0.5 - vec.x;
                                    double e2 = n + 0.5 - vec.y;
                                    double d3 = o + 0.5 - vec.z;
                                    double e3 = d2 * d2 + e2 * e2 + d3 * d3;
                                    if (e3 < 1.0E-4) {
                                        continue;
                                    }
                                    double d4 = Math.toRadians(yaw);
                                    double e4 = Math.toRadians(this.mc.player.getPitch());
                                    double d5 = -Math.sin(d4) * Math.cos(e4);
                                    double e5 = -Math.sin(e4);
                                    double d6 = Math.cos(d4) * Math.cos(e4);
                                    double e6 = (d2 * d5 + e2 * e5 + d3 * d6) / Math.sqrt(e3);
                                    double d7 =
                                            Math.toDegrees(
                                                    Math.acos(Math.max(-1.0, Math.min(1.0, e6))));
                                    if (d7 > f * 0.5) {
                                        continue;
                                    }
                                }
                                double e7;
                                if (!((e7 =
                                                Math.abs(
                                                        MathHelper.wrapDegrees(
                                                                placementTarget2.yaw() - yaw)))
                                        > d)) {
                                    double squaredDistance =
                                            vec.squaredDistanceTo(
                                                    new Vec3d(m + 0.5, n + 0.5, o + 0.5));
                                    double d8 = e7 * 2.0 + squaredDistance * 0.5;
                                    if (currentScope2 != null) {
                                        d8 =
                                                e7 * 0.2
                                                        - Math.sqrt(squaredDistance) * 20.0
                                                        + (n - pos.getY() + 5)
                                                                * currentScope2.lowWeight();
                                    }
                                    double e8;
                                    if (g != 0.0F
                                            && Math.signum(
                                                            e8 =
                                                                    MathHelper.wrapDegrees(
                                                                            placementTarget2.yaw()
                                                                                    - yaw))
                                                    == -Math.signum(g)
                                            && Math.abs(e8) > 10.0) {
                                        d8 += Math.abs(e8) * 5.0;
                                    }
                                    if (d8 < e) {
                                        e = d8;
                                        placementTarget = placementTarget2;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return placementTarget;
        } else {
            return null;
        }
    }

    private PlacementTarget possibleToPlace(
            BlockState state, int i, int j, int k, Vec3d vec, boolean bl, boolean bl2) {
        BlockPos pos = new BlockPos(i, j, k);
        boolean partnerMissing = this.partnerMissing(state, pos);
        int item = this.findItem(state, bl2);
        if (item < 0) {
            return null;
        } else {
            for (Direction direction : Direction.values()) {
                BlockPos pos2 = pos.offset(direction);
                BlockState blockState = this.mc.world.getBlockState(pos2);
                if (!isReplaceable(blockState) && state.canPlaceAt(this.mc.world, pos)) {
                    Direction opposite = direction.getOpposite();
                    if (!bl || this.hopperHandler.faceCanProduceExpectedFacing(state, opposite)) {
                        boolean bl3 =
                                !pos2.equals(pos)
                                        && Clicks.usesClick(
                                                this.mc.world, pos2, this.wholeInventory(item));
                        boolean bl4 =
                                PlacementUtil.isFaceVisibleToEye(
                                                pos2, opposite, this.builderEye(vec, bl3))
                                        || this.builderEyes
                                                && !bl3
                                                && PlacementUtil.isFaceVisibleToEye(
                                                        pos2, opposite, this.builderEye(vec, true));
                        VoxelShape outlineShape;
                        if (bl4
                                && !(outlineShape = blockState.getOutlineShape(this.mc.world, pos2))
                                        .isEmpty()) {
                            Box boundingBox = outlineShape.getBoundingBox();
                            for (Vec3d vec2 : aabbSideMultipliers(direction)) {
                                double d =
                                        pos2.getX()
                                                + boundingBox.minX * vec2.x
                                                + boundingBox.maxX * (1.0 - vec2.x);
                                Vec3d vec3 =
                                        new Vec3d(
                                                d,
                                                pos2.getY()
                                                        + boundingBox.minY * vec2.y
                                                        + boundingBox.maxY * (1.0 - vec2.y),
                                                pos2.getZ()
                                                        + boundingBox.minZ * vec2.z
                                                        + boundingBox.maxZ * (1.0 - vec2.z));
                                PlacementTarget placementTarget =
                                        this.tryAim(
                                                state,
                                                pos,
                                                pos2,
                                                opposite,
                                                vec3,
                                                vec,
                                                item,
                                                partnerMissing);
                                if (placementTarget != null) {
                                    return placementTarget;
                                }
                            }
                        }
                    }
                }
            }
            return null;
        }
    }

    public boolean placeableFrom(BlockState state, BlockPos pos, Vec3d vec, boolean bl) {
        if (this.mc.player != null && this.mc.world != null) {
            BlockState blockState = this.mc.world.getBlockState(pos);
            this.reachSpare = 0.6499999999999999;
            double feetY2 = this.feetY;
            boolean builderEyes2 = this.builderEyes;
            this.feetY = vec.y - this.mc.player.getEyeHeight(EntityPose.STANDING);
            this.builderEyes = true;
            boolean bl2;
            try {
                if (StateRules.slabHalf(blockState, state)) {
                    return this.secondSlab(state, blockState, pos, vec, true) != null;
                }
                if (!isReplaceable(blockState) || StateRules.placed(blockState, state)) {
                    return false;
                }
                boolean bl3 =
                        this.possibleToPlace(
                                        state, pos.getX(), pos.getY(), pos.getZ(), vec, bl, true)
                                != null;
                bl2 = bl3;
            } finally {
                this.reachSpare = 0.0;
                this.feetY = feetY2;
                this.builderEyes = builderEyes2;
            }
            return bl2;
        } else {
            return false;
        }
    }

    private boolean partnerMissing(BlockState state, BlockPos pos) {
        if (!state.contains(Properties.CHEST_TYPE)) {
            return true;
        } else {
            ChestType chestType = state.get(Properties.CHEST_TYPE);
            if (chestType == ChestType.SINGLE) {
                return true;
            } else {
                Direction direction = state.get(Properties.HORIZONTAL_FACING);
                Direction direction2 =
                        chestType == ChestType.LEFT
                                ? direction.rotateYClockwise()
                                : direction.rotateYCounterclockwise();
                BlockState blockState = this.mc.world.getBlockState(pos.offset(direction2));
                return !blockState.contains(Properties.CHEST_TYPE)
                        || blockState.get(Properties.CHEST_TYPE) != ChestType.SINGLE
                        || blockState.get(Properties.HORIZONTAL_FACING) != direction;
            }
        }
    }

    public boolean verify(
            BlockState state,
            BlockPos pos,
            BlockHitResult blockHitResult,
            float f,
            float g,
            int i,
            boolean bl) {
        return this.mc.player != null && this.mc.world != null && i >= 0
                ? this.wouldPlace(
                        state,
                        pos,
                        blockHitResult,
                        new float[] {f, g},
                        i,
                        this.partnerMissing(state, pos),
                        bl)
                : false;
    }

    public String wouldBe(
            BlockPos pos,
            BlockHitResult blockHitResult,
            float yaw,
            float pitch,
            int i,
            boolean bl) {
        if (this.mc.player != null && this.mc.world != null && i >= 0) {
            ClientPlayerEntity player = this.mc.player;
            PlayerInput playerInput = player.input.playerInput;
            ItemStack stack = player.getInventory().getStack(i);
            if (!(stack.getItem() instanceof BlockItem blockItem)) {
                return "no block item";
            } else {
                float yaw2 = player.getYaw();
                float pitch2 = player.getPitch();
                player.setYaw(yaw);
                player.setPitch(pitch);
                player.input.playerInput =
                        new PlayerInput(
                                playerInput.forward(),
                                playerInput.backward(),
                                playerInput.left(),
                                playerInput.right(),
                                playerInput.jump(),
                                bl,
                                playerInput.sprint());
                String string;
                try {
                    ItemPlacementContext itemPlacementContext =
                            new ItemPlacementContext(player, Hand.MAIN_HAND, stack, blockHitResult);
                    if (!itemPlacementContext.canPlace()) {
                        return "cannot place";
                    }
                    if (itemPlacementContext.getBlockPos().equals(pos)) {
                        return ((InvokerBlockItem) blockItem)
                                        .easyplace$getPlacementState(itemPlacementContext)
                                + " facing "
                                + itemPlacementContext.getHorizontalPlayerFacing();
                    }
                    String string2 =
                            "goes to " + itemPlacementContext.getBlockPos().toShortString();
                    string = string2;
                } catch (Exception exception) {
                    return exception.toString();
                } finally {
                    player.setYaw(yaw2);
                    player.setPitch(pitch2);
                    player.input.playerInput = playerInput;
                }
                return string;
            }
        } else {
            return "?";
        }
    }

    private PlacementTarget secondSlab(
            BlockState state, BlockState state2, BlockPos pos, Vec3d vec, boolean bl) {
        int item = this.findItem(state, bl);
        if (item < 0) {
            return null;
        } else {
            boolean bl2 = state2.get(Properties.SLAB_TYPE) == SlabType.BOTTOM;
            Direction direction = bl2 ? Direction.UP : Direction.DOWN;
            double d = pos.getY() + 0.5;
            if (bl2 ? !(vec.y <= d + 0.05) : !(vec.y >= d - 0.05)) {
                for (double[] doubles :
                        new double[][] {
                            {0.5, 0.5}, {0.25, 0.5}, {0.75, 0.5}, {0.5, 0.25}, {0.5, 0.75}
                        }) {
                    Vec3d vec2 = new Vec3d(pos.getX() + doubles[0], d, pos.getZ() + doubles[1]);
                    PlacementTarget placementTarget =
                            this.tryAim(state, pos, pos, direction, vec2, vec, item, true);
                    if (placementTarget != null) {
                        return placementTarget;
                    }
                }
                return null;
            } else {
                return null;
            }
        }
    }

    private PlacementTarget tryAim(
            BlockState state,
            BlockPos pos,
            BlockPos pos2,
            Direction direction,
            Vec3d vec,
            Vec3d vec2,
            int i,
            boolean bl) {
        boolean bl2 =
                !pos2.equals(pos) && Clicks.usesClick(this.mc.world, pos2, this.wholeInventory(i));
        PlacementTarget placementTarget =
                this.aimWith(state, pos, pos2, direction, vec, vec2, i, bl, bl2, false);
        if (placementTarget != null) {
            return placementTarget;
        } else {
            return bl2 || !(state.getBlock() instanceof ChestBlock) && !this.builderEyes
                    ? null
                    : this.aimWith(state, pos, pos2, direction, vec, vec2, i, bl, true, true);
        }
    }

    private PlacementTarget aimWith(
            BlockState state,
            BlockPos pos,
            BlockPos pos2,
            Direction direction,
            Vec3d vec,
            Vec3d vec2,
            int i,
            boolean bl,
            boolean bl2,
            boolean bl3) {
        Vec3d vec3 = this.builderEye(vec2, bl2);
        float[] rotation;
        BlockHitResult blockHitResult =
                PlacementUtil.rayTraceFrom(
                        this.mc.player,
                        vec3,
                        (rotation = PlacementUtil.calcRotation(vec3, vec))[0],
                        rotation[1],
                        this.mc.player.getBlockInteractionRange() - this.reachSpare);
        if (blockHitResult == null || blockHitResult.getType() != Type.BLOCK) {
            return null;
        } else if (!blockHitResult.getBlockPos().equals(pos2)
                || blockHitResult.getSide() != direction) {
            return null;
        } else if (!this.wouldPlace(state, pos, blockHitResult, rotation, i, bl, bl2)) {
            return null;
        } else {
            return !this.steady(state, pos, blockHitResult, rotation, i, bl, bl2)
                    ? null
                    : new PlacementTarget(
                            pos,
                            pos2,
                            direction,
                            direction,
                            i,
                            rotation[0],
                            rotation[1],
                            0,
                            blockHitResult.getPos(),
                            bl3);
        }
    }

    private boolean steady(
            BlockState state,
            BlockPos pos,
            BlockHitResult blockHitResult,
            float[] floats,
            int i,
            boolean bl,
            boolean bl2) {
        Vec3d vec = blockHitResult.getPos();
        Direction side = blockHitResult.getSide();
        for (Direction direction : Direction.values()) {
            if (direction.getAxis() != side.getAxis()
                    && !this.wouldPlace(
                            state,
                            pos,
                            new BlockHitResult(
                                    vec.add(
                                            direction.getOffsetX() * 0.02,
                                            direction.getOffsetY() * 0.02,
                                            direction.getOffsetZ() * 0.02),
                                    side,
                                    blockHitResult.getBlockPos(),
                                    blockHitResult.isInsideBlock()),
                            floats,
                            i,
                            bl,
                            bl2)) {
                return false;
            }
        }
        for (float[] floats2 : STEADY_TURNS) {
            if (!this.wouldPlace(
                    state,
                    pos,
                    blockHitResult,
                    new float[] {floats[0] + floats2[0], floats[1] + floats2[1]},
                    i,
                    bl,
                    bl2)) {
                return false;
            }
        }
        return true;
    }

    private Vec3d builderEye(Vec3d vec, boolean bl) {
        return this.builderEyes && !Double.isNaN(this.feetY)
                ? new Vec3d(
                        vec.x,
                        this.feetY
                                + this.mc.player.getEyeHeight(
                                        bl ? EntityPose.CROUCHING : EntityPose.STANDING),
                        vec.z)
                : vec;
    }

    private int findItem(BlockState state, boolean bl) {
        Item item = BlockStateUtil.requiredItem(state);
        if (item != null && item instanceof BlockItem && this.mc.player != null) {
            PlayerInventory inventory = this.mc.player.getInventory();
            int i = bl ? 36 : 9;
            for (int j = 0; j < i; j++) {
                if (inventory.getStack(j).isOf(item)) {
                    return j;
                }
            }
            return -1;
        } else {
            return -1;
        }
    }

    private boolean wholeInventory(int i) {
        return i >= 9 || this.currentScope != null;
    }

    private boolean wouldPlace(
            BlockState state,
            BlockPos pos,
            BlockHitResult blockHitResult,
            float[] floats,
            int i,
            boolean bl,
            boolean bl2) {
        ClientPlayerEntity player = this.mc.player;
        PlayerInput playerInput = player.input.playerInput;
        ItemStack stack = player.getInventory().getStack(i);
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        } else {
            float yaw = player.getYaw();
            float pitch = player.getPitch();
            float f = MathHelper.wrapDegrees(floats[0] - yaw);
            player.setYaw(yaw + f);
            player.setPitch(floats[1]);
            player.input.playerInput =
                    new PlayerInput(
                            playerInput.forward(),
                            playerInput.backward(),
                            playerInput.left(),
                            playerInput.right(),
                            playerInput.jump(),
                            bl2,
                            playerInput.sprint());
            boolean bl3;
            try {
                ItemPlacementContext itemPlacementContext =
                        new ItemPlacementContext(player, Hand.MAIN_HAND, stack, blockHitResult);
                if (!itemPlacementContext.canPlace()
                        || !itemPlacementContext.getBlockPos().equals(pos)) {
                    return false;
                }
                BlockState state2 =
                        ((InvokerBlockItem) blockItem)
                                .easyplace$getPlacementState(itemPlacementContext);
                if (state2 == null) {
                    return false;
                }
                if (!(state.getBlock() instanceof ChestBlock)
                        || !(state2.getBlock() instanceof ChestBlock)) {
                    return StateRules.wouldFit(state2, state, false);
                }
                ChestType chestType = state.get(Properties.CHEST_TYPE);
                ChestType chestType2 = state2.get(Properties.CHEST_TYPE);
                if (chestType2 != chestType && (chestType2 != ChestType.SINGLE || !bl)) {
                    return false;
                }
                boolean wouldFit = StateRules.wouldFit(state2, state, true);
                bl3 = wouldFit;
            } catch (Exception exception) {
                return false;
            } finally {
                player.setYaw(yaw);
                player.setPitch(pitch);
                player.input.playerInput = playerInput;
            }
            return bl3;
        }
    }

    private static Vec3d[] aabbSideMultipliers(Direction direction) {
        Vec3d[] vecs;
        switch (direction) {
            case DOWN:
                Vec3d[] vecs2 =
                        new Vec3d[] {
                            new Vec3d(0.5, 0.0, 0.5),
                            new Vec3d(0.1, 0.0, 0.5),
                            new Vec3d(0.9, 0.0, 0.5),
                            new Vec3d(0.5, 0.0, 0.1),
                            new Vec3d(0.5, 0.0, 0.9),
                            new Vec3d(0.03, 0.0, 0.5),
                            new Vec3d(0.97, 0.0, 0.5),
                            new Vec3d(0.5, 0.0, 0.03),
                            new Vec3d(0.5, 0.0, 0.97),
                            new Vec3d(0.25, 0.0, 0.25),
                            new Vec3d(0.75, 0.0, 0.25),
                            new Vec3d(0.25, 0.0, 0.75),
                            null
                        };
                vecs = vecs2;
                vecs2[12] = new Vec3d(0.75, 0.0, 0.75);
                break;
            case UP:
                Vec3d[] vecs3 =
                        new Vec3d[] {
                            new Vec3d(0.5, 1.0, 0.5),
                            new Vec3d(0.1, 1.0, 0.5),
                            new Vec3d(0.9, 1.0, 0.5),
                            new Vec3d(0.5, 1.0, 0.1),
                            new Vec3d(0.5, 1.0, 0.9),
                            new Vec3d(0.25, 1.0, 0.25),
                            new Vec3d(0.75, 1.0, 0.25),
                            new Vec3d(0.25, 1.0, 0.75),
                            null
                        };
                vecs = vecs3;
                vecs3[8] = new Vec3d(0.75, 1.0, 0.75);
                break;
            default:
                double d = direction.getOffsetX() == 0 ? 0.5 : (1 + direction.getOffsetX()) / 2.0;
                double e = direction.getOffsetZ() == 0 ? 0.5 : (1 + direction.getOffsetZ()) / 2.0;
                Vec3d[] vecs4 =
                        new Vec3d[] {
                            new Vec3d(d, 0.5, e),
                            new Vec3d(d, 0.25, e),
                            new Vec3d(d, 0.75, e),
                            new Vec3d(
                                    direction.getOffsetX() == 0 ? 0.15 : d,
                                    0.5,
                                    direction.getOffsetZ() == 0 ? 0.15 : e),
                            null
                        };
                vecs = vecs4;
                vecs4[4] =
                        new Vec3d(
                                direction.getOffsetX() == 0 ? 0.85 : d,
                                0.5,
                                direction.getOffsetZ() == 0 ? 0.85 : e);
        }
        return vecs;
    }

    private static boolean isReplaceable(BlockState state) {
        if (state.isAir()) {
            return true;
        } else {
            Block block = state.getBlock();
            if (block instanceof FluidBlock) {
                return true;
            } else if (block instanceof CropBlock
                    && state.contains(CropBlock.AGE)
                    && state.get(CropBlock.AGE) == 7) {
                return true;
            } else {
                return !(block instanceof FireBlock) && !(block instanceof ChorusFlowerBlock)
                        ? state.isReplaceable()
                        : true;
            }
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
}
