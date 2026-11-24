package net.efn.client.module.impl.movement;

import net.efn.client.event.Stage;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.misc.FreecamModule;
import net.efn.client.module.impl.render.FreelookModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.BlockItem;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@Environment(EnvType.CLIENT)
public final class ClickTpModule extends ClientModule {

    private static final double RAYCAST_DISTANCE = 210.0;

    private static final double BLOCKS_PER_PACKET = 10.0;

    private static final int MAX_PADDING_PACKETS = 19;

    public ClickTpModule() {
        super(
                "click-tp",
                "Click TP",
                "Teleports you to the block you click on.",
                ModuleCategory.MOVEMENT);
    }

    @Override
    protected void onEnable() {
        this.on(TickEvent.TYPE, Stage.POST, this::onPostTick);
    }

    private void onPostTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        if (player != null && world != null) {
            if (player.getInventory().getSelectedStack().getUseAction() == UseAction.NONE) {
                if (client.options.useKey.isPressed()) {
                    Camera camera = client.gameRenderer.getCamera();
                    Vec3d cameraPos = camera.getCameraPos();
                    Vec3d vec = Vec3d.fromPolar(camera.getPitch(), camera.getYaw());
                    FreecamModule module = ModuleRegistry.get(FreecamModule.class);
                    boolean bl = module != null && module.isEnabled();
                    HitResult hitResult =
                            bl
                                    ? pickFromCamera(player, world, cameraPos, vec)
                                    : client.crosshairTarget;
                    if (hitResult != null) {
                        if (hitResult.getType() == Type.ENTITY
                                && player.interact(
                                                ((EntityHitResult) hitResult).getEntity(),
                                                Hand.MAIN_HAND)
                                        != ActionResult.PASS) {
                            return;
                        }
                        if (hitResult.getType() == Type.BLOCK
                                && player.getMainHandStack().getItem() instanceof BlockItem) {
                            return;
                        }
                    }
                    BlockHitResult blockHitResult;
                    if ((blockHitResult =
                                            world.raycast(
                                                    new RaycastContext(
                                                            cameraPos,
                                                            cameraPos.add(vec.multiply(210.0)),
                                                            ShapeType.OUTLINE,
                                                            FluidHandling.NONE,
                                                            player)))
                                    .getType()
                            == Type.BLOCK) {
                        FreelookModule module2;
                        if (!blockHitResult.isInsideBlock()
                                || bl
                                || (module2 = ModuleRegistry.get(FreelookModule.class)) == null
                                || !module2.isEnabled()) {
                            BlockPos pos = blockHitResult.getBlockPos();
                            Direction side = blockHitResult.getSide();
                            if (world.getBlockState(pos).onUse(world, player, blockHitResult)
                                    == ActionResult.PASS) {
                                BlockState blockState = world.getBlockState(pos);
                                VoxelShape collisionShape =
                                        blockState.getCollisionShape(world, pos);
                                if (collisionShape.isEmpty()) {
                                    collisionShape = blockState.getOutlineShape(world, pos);
                                }
                                double d =
                                        collisionShape.isEmpty()
                                                ? 1.0
                                                : collisionShape.getMax(Axis.Y);
                                Vec3d vec2 =
                                        new Vec3d(
                                                pos.getX() + 0.5 + side.getOffsetX(),
                                                pos.getY() + d,
                                                pos.getZ() + 0.5 + side.getOffsetZ());
                                int i =
                                        (int)
                                                        Math.ceil(
                                                                player.getEntityPos()
                                                                                .distanceTo(vec2)
                                                                        / 10.0)
                                                - 1;
                                if (i > 19) {
                                    i = 0;
                                }
                                for (int j = 0; j < i; j++) {
                                    player.networkHandler.sendPacket(new OnGroundOnly(true, true));
                                }
                                player.networkHandler.sendPacket(
                                        new PositionAndOnGround(
                                                vec2.x, vec2.y, vec2.z, true, true));
                                player.setPosition(vec2);
                            }
                        }
                    }
                }
            }
        }
    }

    private static HitResult pickFromCamera(
            ClientPlayerEntity player, ClientWorld world, Vec3d vec, Vec3d vec2) {
        double blockInteractionRange = player.getBlockInteractionRange();
        double entityInteractionRange = player.getEntityInteractionRange();
        double d = Math.max(blockInteractionRange, entityInteractionRange);
        double e = d * d;
        BlockHitResult blockHitResult =
                world.raycast(
                        new RaycastContext(
                                vec,
                                vec.add(vec2.multiply(d)),
                                ShapeType.OUTLINE,
                                FluidHandling.NONE,
                                player));
        double squaredDistance = blockHitResult.getPos().squaredDistanceTo(vec);
        if (blockHitResult.getType() != Type.MISS) {
            e = squaredDistance;
            d = Math.sqrt(squaredDistance);
        }
        Vec3d vec3;
        EntityHitResult entityHitResult;
        if ((entityHitResult =
                                ProjectileUtil.raycast(
                                        player,
                                        vec,
                                        vec3 = vec.add(vec2.multiply(d)),
                                        new Box(vec, vec3).expand(1.0),
                                        EntityPredicates.CAN_HIT,
                                        e))
                        != null
                && entityHitResult.getPos().squaredDistanceTo(vec) < squaredDistance) {
            return entityHitResult.getPos().isInRange(vec, entityInteractionRange)
                    ? entityHitResult
                    : null;
        } else {
            return blockHitResult.getPos().isInRange(vec, blockInteractionRange)
                    ? blockHitResult
                    : null;
        }
    }
}
