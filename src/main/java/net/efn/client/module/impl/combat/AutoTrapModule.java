package net.efn.client.module.impl.combat;

import net.efn.client.chat.ModuleChat;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.crystal.Crystals;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.impl.combat.melee.SlotReturn;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.pvp.Rotation;
import net.efn.client.util.pvp.RotationUtils;
import net.efn.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Type;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class AutoTrapModule extends ClientModule {

    private static final Direction[] SUPPORT_ORDER =
            new Direction[] {
                Direction.DOWN,
                Direction.NORTH,
                Direction.SOUTH,
                Direction.EAST,
                Direction.WEST,
                Direction.UP
            };

    private static final int TARGET_COLOR = -24512;

    private static final int CELL_COLOR = -2130706433;

    private final NumberSetting range =
            this.addSetting("range", new NumberSetting("Range", 4.0F, 1.0F, 6.0F).step(0.5F));

    private final BooleanSetting rotate =
            this.addSetting("rotate", new BooleanSetting("Rotate", true));

    private final NumberSetting perTick =
            this.addSetting(
                    "blocks-per-tick",
                    new NumberSetting("Blocks Per Tick", 4.0F, 1.0F, 8.0F).step(1.0F));

    private final BooleanSetting top =
            this.addSetting("top-block", new BooleanSetting("Top Block", true));

    private final BooleanSetting show =
            this.addSetting("show-target", new BooleanSetting("Show Target", true));

    private final MinecraftClient mc = MinecraftClient.getInstance();

    private final SlotReturn slot = new SlotReturn(this);

    private PlayerEntity target;

    private List<BlockPos> plan = List.of();

    private boolean warnedEmpty;

    public AutoTrapModule() {
        super(
                "auto-trap",
                "Auto Trap",
                "Boxes the nearest enemy in with obsidian: sides at feet and head height, then the"
                        + " top.",
                ModuleCategory.COMBAT);
        this.perTick.visibleWhen(() -> !this.rotate.get());
    }

    @Override
    protected void onEnable() {
        this.target = null;
        this.plan = List.of();
        this.warnedEmpty = false;
        this.on(TickEvent.TYPE, Stage.PRE, tickEvent -> this.tick());
        this.on(Render3DEvent.TYPE, Stage.POST, render3DEvent -> this.render());
    }

    @Override
    protected void onDisable() {
        this.slot.drop(this.mc, true);
        this.target = null;
        this.plan = List.of();
    }

    private void tick() {
        ClientPlayerEntity player = this.mc.player;
        ClientWorld world = this.mc.world;
        if (player != null
                && world != null
                && this.mc.interactionManager != null
                && MeleeAttacks.canAct(this.mc)) {
            this.target = this.pick(player);
            this.plan = this.target == null ? List.of() : this.cells(world, player, this.target);
            if (this.plan.isEmpty()) {
                this.slot.tick(this.mc, true);
            } else {
                int hotbarSlot = Crystals.hotbarSlotOf(player.getInventory(), Items.OBSIDIAN);
                if (hotbarSlot < 0) {
                    if (!this.warnedEmpty) {
                        ModuleChat.send(this, ModuleChat.body().warn("No obsidian in the hotbar"));
                    }
                    this.warnedEmpty = true;
                    this.slot.tick(this.mc, true);
                } else {
                    this.warnedEmpty = false;
                    int i = this.rotate.get() ? 1 : Math.round(this.perTick.get());
                    int j = 0;
                    for (BlockPos pos : this.plan) {
                        if (j < i) {
                            BlockHitResult blockHitResult = support(world, player, pos);
                            if (blockHitResult == null) {
                                continue;
                            }
                            if (this.rotate.get()) {
                                BlockHitResult blockHitResult2 =
                                        seenByServer(world, player, blockHitResult);
                                if (blockHitResult2 == null) {
                                    ServerRotation.request(
                                            RotationUtils.fromTo(
                                                    player.getEyePos(), blockHitResult.getPos()));
                                    return;
                                }
                                blockHitResult = blockHitResult2;
                            }
                            if (j != 0
                                    || !MeleeAttacks.actedThisTick(player)
                                            && MeleeAttacks.handsFree(this.mc, player)) {
                                if (!this.slot.take(player, hotbarSlot)) {
                                    return;
                                }
                                ActionResult actionResult =
                                        this.mc.interactionManager.interactBlock(
                                                player, Hand.MAIN_HAND, blockHitResult);
                                if (actionResult.isAccepted()) {
                                    player.swingHand(Hand.MAIN_HAND);
                                    MeleeAttacks.noteAction(player);
                                    j++;
                                }
                                continue;
                            }
                            return;
                        }
                        break;
                    }
                    if (this.rotate.get() && j > 0) {
                        ServerRotation.request(ServerRotation.sent());
                    }
                    return;
                }
            }
        } else {
            this.slot.tick(this.mc, true);
        }
    }

    private PlayerEntity pick(ClientPlayerEntity player) {
        double d = this.range.get() + 1.5;
        PlayerEntity player2 = null;
        for (PlayerEntity player3 : this.mc.world.getPlayers()) {
            double distance;
            if (player3 != player
                    && player3.isAlive()
                    && MeleeAttacks.isEnemy(player, player3)
                    && (distance = player3.distanceTo(player)) <= d
                    && (player2 == null || distance < player2.distanceTo(player))) {
                player2 = player3;
            }
        }
        return player2;
    }

    private List<BlockPos> cells(
            ClientWorld world, ClientPlayerEntity player, PlayerEntity player2) {
        Box boundingBox = player2.getBoundingBox();
        int i = MathHelper.floor(boundingBox.minY + 0.01);
        LinkedHashSet set = new LinkedHashSet();
        for (int j = MathHelper.floor(boundingBox.minX);
                j <= MathHelper.floor(boundingBox.maxX - 1.0E-4);
                j++) {
            for (int k = MathHelper.floor(boundingBox.minZ);
                    k <= MathHelper.floor(boundingBox.maxZ - 1.0E-4);
                    k++) {
                set.add(BlockPos.asLong(j, 0, k));
            }
        }
        ArrayList list = new ArrayList();
        for (int m = 0; m <= 1; m++) {
            ArrayList list2 = new ArrayList();
            for (long l : (Iterable<Long>) set) {
                BlockPos pos = BlockPos.fromLong(l).withY(i + m);
                for (Direction direction : Type.HORIZONTAL) {
                    BlockPos pos2 = pos.offset(direction);
                    if (!set.contains(BlockPos.asLong(pos2.getX(), 0, pos2.getZ()))
                            && this.open(world, player, pos2)
                            && !list2.contains(pos2)) {
                        list2.add(pos2);
                    }
                }
            }
            list2.sort(
                    Comparator.comparingDouble(
                            (BlockPos pos3) -> pos3.getSquaredDistance(player.getEyePos())));
            list.addAll(list2);
        }
        if (this.top.get()) {
            for (long l2 : (Iterable<Long>) set) {
                BlockPos pos4 = BlockPos.fromLong(l2).withY(i + 2);
                if (this.open(world, player, pos4)) {
                    list.add(pos4);
                }
            }
        }
        return list;
    }

    private boolean open(ClientWorld world, ClientPlayerEntity player, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        if (blockState.isReplaceable()
                && (blockState.getFluidState().isEmpty() || blockState.getFluidState().isStill())) {
            return pos.toCenterPos().distanceTo(player.getEyePos())
                            > player.getBlockInteractionRange()
                    ? false
                    : world.canPlace(Blocks.OBSIDIAN.getDefaultState(), pos, ShapeContext.absent());
        } else {
            return false;
        }
    }

    public static BlockHitResult support(
            ClientWorld world, ClientPlayerEntity player, BlockPos pos) {
        Vec3d eyePos = player.getEyePos();
        double blockInteractionRange = player.getBlockInteractionRange();
        for (Direction direction : SUPPORT_ORDER) {
            BlockPos pos2 = pos.offset(direction);
            BlockState blockState = world.getBlockState(pos2);
            if (!blockState.isReplaceable()
                    && !blockState.getCollisionShape(world, pos2).isEmpty()) {
                Direction opposite = direction.getOpposite();
                Vec3d vec =
                        Vec3d.ofCenter(pos2)
                                .add(
                                        opposite.getOffsetX() * 0.5,
                                        opposite.getOffsetY() * 0.5,
                                        opposite.getOffsetZ() * 0.5);
                BlockHitResult blockHitResult;
                if (!(vec.distanceTo(eyePos) > blockInteractionRange)
                        && (blockHitResult =
                                                world.raycast(
                                                        new RaycastContext(
                                                                eyePos,
                                                                vec.add(
                                                                        Vec3d.of(
                                                                                        direction
                                                                                                .getVector())
                                                                                .multiply(0.05)),
                                                                ShapeType.OUTLINE,
                                                                FluidHandling.NONE,
                                                                player)))
                                        .getType()
                                == net.minecraft.util.hit.HitResult.Type.BLOCK
                        && blockHitResult.getBlockPos().equals(pos2)
                        && blockHitResult.getSide() == opposite) {
                    return new BlockHitResult(vec, opposite, pos2, false);
                }
            }
        }
        return null;
    }

    public static BlockHitResult seenByServer(
            ClientWorld world, ClientPlayerEntity player, BlockHitResult blockHitResult) {
        Rotation rotation = ServerRotation.sent();
        if (rotation == null) {
            return null;
        } else {
            Vec3d eyePos = player.getEyePos();
            BlockHitResult blockHitResult2 =
                    world.raycast(
                            new RaycastContext(
                                    eyePos,
                                    eyePos.add(
                                            Vec3d.fromPolar(rotation.pitch(), rotation.yaw())
                                                    .multiply(player.getBlockInteractionRange())),
                                    ShapeType.OUTLINE,
                                    FluidHandling.NONE,
                                    player));
            return blockHitResult2.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK
                            && blockHitResult2.getBlockPos().equals(blockHitResult.getBlockPos())
                            && blockHitResult2.getSide() == blockHitResult.getSide()
                    ? blockHitResult2
                    : null;
        }
    }

    private void render() {
        if (this.show.get() && this.target != null) {
            GizmoDrawing.box(this.target.getBoundingBox(), DrawStyle.stroked(-24512, 2.0F));
            for (BlockPos pos : this.plan) {
                GizmoDrawing.box(pos, DrawStyle.stroked(-2130706433, 1.0F));
            }
        }
    }
}
