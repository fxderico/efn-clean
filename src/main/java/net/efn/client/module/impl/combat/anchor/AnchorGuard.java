/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.module.impl.combat.anchor;

import net.efn.client.module.impl.combat.crystal.Crystals;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.util.pvp.Rotation;
import net.efn.client.util.pvp.RotationUtils;
import net.efn.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.EmptyBlockView;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

@Environment(value = EnvType.CLIENT)
public final class AnchorGuard {

    public static final float BLAST_POWER = 5.0f;

    private static final double MIN_SHADE = 0.15;

    private static final double FACE_INSET = 0.3;

    private static final double AIM_INSET = 0.1;

    private AnchorGuard() {}

    public static Spot find(
            ClientWorld world, ClientPlayerEntity player, BlockPos pos2, Vec3d vec) {
        Object object;
        Object object2;
        Vec3d vec2 = Vec3d.ofCenter(pos2);
        Box boundingBox = player.getBoundingBox();
        List<Vec3d> list = AnchorGuard.bodyPoints(boundingBox);
        ArrayList<Vec3d> list2 = new ArrayList<Vec3d>(list.size());
        for (Vec3d vec3 : list) {
            object2 =
                    world.raycast(
                            new RaycastContext(
                                    vec3,
                                    vec2,
                                    RaycastContext.ShapeType.COLLIDER,
                                    RaycastContext.FluidHandling.NONE,
                                    player));
            if (((BlockHitResult) object2).getType() != HitResult.Type.MISS
                    && !((BlockHitResult) object2).getBlockPos().equals(pos2)) continue;
            list2.add(vec3);
        }
        BlockPos pos3 = player.getBlockPos();
        if ((double) list2.size() < Math.ceil((double) list.size() * 0.15)) {
            return new Spot(pos3, null, null, null);
        }
        int k = (int) Math.ceil((double) list2.size() * 0.15);
        object2 = player.getEyePos();
        LinkedHashSet<BlockPos> set = new LinkedHashSet<BlockPos>();
        for (int i = -1; i <= 1; ++i) {
            for (int j = -1; j <= 1; ++j) {
                if (i == 0 && j == 0) continue;
                set.add(pos3.add(i, 0, j));
            }
        }
        for (Direction direction : Direction.Type.HORIZONTAL) {
            set.add(pos2.offset(direction));
        }
        ArrayList list3 = new ArrayList(set.size());
        for (BlockPos pos4 : set) {
            int shade2;
            object = new Box(pos4);
            if (pos4.equals(pos2)
                    || ((Box) object).intersects(boundingBox)
                    || ((Box) object).raycast((Vec3d) object2, vec).isPresent()
                    || (shade2 = AnchorGuard.shade((Box) object, vec2, list2)) < k) continue;
            list3.add(new Cell(pos4, shade2, ((Box) object).getCenter().squaredDistanceTo(vec2)));
        }
        list3.sort(
                Comparator.comparingInt(Cell::shade)
                        .reversed()
                        .thenComparingDouble(Cell::distance));
        Iterator iterator = list3.iterator();
        while (iterator.hasNext()) {
            Spot spot;
            Cell cell = (Cell) iterator.next();
            object = world.getBlockState(cell.pos());
            if (!((AbstractBlock.AbstractBlockState) object).isReplaceable()
                    || (spot =
                                    ((AbstractBlock.AbstractBlockState) object)
                                                    .getOutlineShape(world, cell.pos())
                                                    .isEmpty()
                                            ? AnchorGuard.supportFor(
                                                    world, player, (Vec3d) object2, cell.pos())
                                            : AnchorGuard.direct(
                                                    world,
                                                    player,
                                                    (Vec3d) object2,
                                                    cell.pos(),
                                                    (BlockState) object))
                            == null) continue;
            return spot;
        }
        return null;
    }

    public static BlockHitResult hitAlong(
            ClientWorld world, ClientPlayerEntity player, Rotation rotation, Spot spot) {
        BlockHitResult blockHitResult = AnchorGuard.trace(world, player, rotation);
        if (blockHitResult == null || !blockHitResult.getBlockPos().equals(spot.support())) {
            return null;
        }
        if (spot.face() != null && blockHitResult.getSide() != spot.face()) {
            return null;
        }
        BlockPos pos2 =
                Crystals.placedObsidian(
                        world, player, blockHitResult, world.getBlockState(spot.support()));
        return spot.pos().equals(pos2) ? blockHitResult : null;
    }

    public static BlockHitResult anchorHit(
            ClientWorld world, ClientPlayerEntity player, BlockPos pos2) {
        Rotation rotation = ServerRotation.sent();
        BlockHitResult blockHitResult =
                rotation == null ? null : AnchorGuard.trace(world, player, rotation);
        return blockHitResult != null && blockHitResult.getBlockPos().equals(pos2)
                ? blockHitResult
                : null;
    }

    public static boolean isGuardBlock(ItemStack stack) {
        Item item = stack.getItem();
        if (!(item instanceof BlockItem)) {
            return false;
        }
        BlockItem blockItem = (BlockItem) item;
        Block block = blockItem.getBlock();
        if (block instanceof BlockEntityProvider
                || block instanceof FallingBlock
                || block == Blocks.RESPAWN_ANCHOR
                || block == Blocks.TNT) {
            return false;
        }
        return block.getDefaultState().isFullCube(EmptyBlockView.INSTANCE, BlockPos.ORIGIN);
    }

    public static int detonatorSlot(PlayerInventory playerInventory) {
        int hotbarSlot = Crystals.hotbarSlotOf(playerInventory, Items.TOTEM_OF_UNDYING);
        if (hotbarSlot != -1) {
            return hotbarSlot;
        }
        for (int i = 0; i < PlayerInventory.getHotbarSize(); ++i) {
            if (!Crystals.isCombatItem(playerInventory.getStack(i))) continue;
            return i;
        }
        return -1;
    }

    public static int detonatorRoute(PlayerInventory playerInventory, ItemStack stack) {
        if (Crystals.isCombatItem(stack)) {
            return -2;
        }
        int detonatorSlot = AnchorGuard.detonatorSlot(playerInventory);
        return detonatorSlot == -1 ? -1 : detonatorSlot;
    }

    public static Vec3d anchorAim(ClientWorld world, ClientPlayerEntity player, BlockPos pos2) {
        Vec3d vec;
        BlockHitResult blockHitResult;
        Vec3d eyePos = player.getEyePos();
        Box box = new Box(pos2).contract(0.1);
        Rotation rotation = ServerRotation.sent();
        if (rotation != null) {
            Vec3d vec2 = Vec3d.fromPolar(rotation.pitch(), rotation.yaw());
            double dotProduct = box.getCenter().subtract(eyePos).dotProduct(vec2);
            if (dotProduct > 0.0) {
                Vec3d vec3 = eyePos.add(vec2.multiply(dotProduct));
                Vec3d vec4 =
                        new Vec3d(
                                MathHelper.clamp(vec3.x, box.minX, box.maxX),
                                MathHelper.clamp(vec3.y, box.minY, box.maxY),
                                MathHelper.clamp(vec3.z, box.minZ, box.maxZ));
                BlockHitResult blockHitResult2 =
                        AnchorGuard.trace(
                                world,
                                player,
                                ServerRotation.next(player, RotationUtils.fromTo(eyePos, vec4)));
                if (blockHitResult2 != null && blockHitResult2.getBlockPos().equals(pos2)) {
                    return vec4;
                }
            }
        }
        return (blockHitResult =
                                        AnchorGuard.trace(
                                                world,
                                                player,
                                                ServerRotation.next(
                                                        player,
                                                        RotationUtils.fromTo(
                                                                eyePos,
                                                                vec =
                                                                        new Vec3d(
                                                                                MathHelper.clamp(
                                                                                        eyePos.x,
                                                                                        box.minX,
                                                                                        box.maxX),
                                                                                MathHelper.clamp(
                                                                                        eyePos.y,
                                                                                        box.minY,
                                                                                        box.maxY),
                                                                                MathHelper.clamp(
                                                                                        eyePos.z,
                                                                                        box.minZ,
                                                                                        box.maxZ))))))
                                != null
                        && blockHitResult.getBlockPos().equals(pos2)
                ? vec
                : null;
    }

    public static void click(
            MinecraftClient client, ClientPlayerEntity player, BlockHitResult blockHitResult) {
        ActionResult.Success success;
        ItemStack mainHandStack = player.getMainHandStack();
        int count = mainHandStack.getCount();
        ActionResult actionResult =
                client.interactionManager.interactBlock(player, Hand.MAIN_HAND, blockHitResult);
        if (actionResult instanceof ActionResult.Success
                && (success = (ActionResult.Success) actionResult).swingSource()
                        == ActionResult.SwingSource.CLIENT) {
            player.swingHand(Hand.MAIN_HAND);
            if (!mainHandStack.isEmpty()
                    && (mainHandStack.getCount() != count || player.isInCreativeMode())) {
                client.gameRenderer.firstPersonRenderer.resetEquipProgress(Hand.MAIN_HAND);
            }
        }
        MeleeAttacks.claimTick(client);
    }

    private static Spot direct(
            ClientWorld world,
            ClientPlayerEntity player,
            Vec3d vec,
            BlockPos pos2,
            BlockState state) {
        Spot spot =
                new Spot(
                        pos2,
                        pos2,
                        null,
                        state.getOutlineShape(world, pos2)
                                .getBoundingBox()
                                .offset(pos2)
                                .getCenter());
        Rotation rotation = ServerRotation.next(player, RotationUtils.fromTo(vec, spot.aim()));
        return AnchorGuard.hitAlong(world, player, rotation, spot) != null ? spot : null;
    }

    public static Spot supportFor(
            ClientWorld world, ClientPlayerEntity player, Vec3d vec, BlockPos pos2) {
        Spot spot = null;
        double mAX_VALUE = Double.MAX_VALUE;
        for (Direction direction : Direction.values()) {
            VoxelShape outlineShape;
            BlockPos pos3 = pos2.offset(direction);
            BlockState blockState = world.getBlockState(pos3);
            if (blockState.isReplaceable()
                    || (outlineShape = blockState.getOutlineShape(world, pos3)).isEmpty()) continue;
            Direction opposite = direction.getOpposite();
            Spot spot2 = null;
            for (Vec3d vec2 :
                    AnchorGuard.faceAims(outlineShape.getBoundingBox().offset(pos3), opposite)) {
                Spot spot3 = new Spot(pos2, pos3, opposite, vec2);
                Rotation rotation = ServerRotation.next(player, RotationUtils.fromTo(vec, vec2));
                if (AnchorGuard.hitAlong(world, player, rotation, spot3) == null) continue;
                spot2 = spot3;
                break;
            }
            if (spot2 == null) continue;
            double d = direction == Direction.DOWN ? -1.0 : vec.squaredDistanceTo(spot2.aim());
            double e = d;
            if (!(d < mAX_VALUE)) continue;
            spot = spot2;
            mAX_VALUE = d;
        }
        return spot;
    }

    private static List<Vec3d> faceAims(Box box, Direction direction) {
        Direction.Axis axis = direction.getAxis();
        double d =
                direction.getDirection() == Direction.AxisDirection.POSITIVE
                        ? box.getMax(axis)
                        : box.getMin(axis);
        Vec3d vec = box.getCenter().withAxis(axis, d);
        ArrayList<Vec3d> list = new ArrayList<Vec3d>(5);
        list.add(vec);
        for (Direction direction2 : Direction.values()) {
            Direction.Axis axis2 = direction2.getAxis();
            if (axis2 == axis) continue;
            list.add(vec.offset(direction2, (box.getMax(axis2) - box.getMin(axis2)) * 0.3));
        }
        return list;
    }

    public static BlockHitResult trace(
            ClientWorld world, ClientPlayerEntity player, Rotation rotation) {
        Vec3d vec;
        Vec3d eyePos = player.getEyePos();
        BlockHitResult blockHitResult =
                world.raycast(
                        new RaycastContext(
                                eyePos,
                                eyePos.add(
                                        vec =
                                                Vec3d.fromPolar(rotation.pitch(), rotation.yaw())
                                                        .multiply(
                                                                player.getBlockInteractionRange())),
                                RaycastContext.ShapeType.OUTLINE,
                                RaycastContext.FluidHandling.NONE,
                                player));
        return blockHitResult.getType() == HitResult.Type.BLOCK ? blockHitResult : null;
    }

    private static int shade(Box box, Vec3d vec, List<Vec3d> list) {
        int i = 0;
        for (Vec3d vec2 : list) {
            if (!box.raycast(vec2, vec).isPresent()) continue;
            ++i;
        }
        return i;
    }

    private static List<Vec3d> bodyPoints(Box box) {
        double d = 1.0 / ((box.maxX - box.minX) * 2.0 + 1.0);
        double e = 1.0 / ((box.maxY - box.minY) * 2.0 + 1.0);
        double d2 = 1.0 / ((box.maxZ - box.minZ) * 2.0 + 1.0);
        double e2 = (1.0 - Math.floor(1.0 / d) * d) / 2.0;
        double d3 = (1.0 - Math.floor(1.0 / d2) * d2) / 2.0;
        ArrayList<Vec3d> list = new ArrayList<Vec3d>();
        for (double e3 = 0.0; e3 <= 1.0; e3 += d) {
            for (double d4 = 0.0; d4 <= 1.0; d4 += e) {
                for (double e4 = 0.0; e4 <= 1.0; e4 += d2) {
                    list.add(
                            new Vec3d(
                                    MathHelper.lerp(e3, box.minX, box.maxX) + e2,
                                    MathHelper.lerp(d4, box.minY, box.maxY),
                                    MathHelper.lerp(e4, box.minZ, box.maxZ) + d3));
                }
            }
        }
        return list;
    }

    @Environment(value = EnvType.CLIENT)
    public record Spot(BlockPos pos, BlockPos support, Direction face, Vec3d aim) {

        public boolean standing() {
            return this.support == null;
        }
    }

    @Environment(value = EnvType.CLIENT)
    private record Cell(BlockPos pos, int shade, double distance) {}
}
