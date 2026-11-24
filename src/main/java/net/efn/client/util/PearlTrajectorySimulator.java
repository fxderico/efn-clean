package net.efn.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.Items;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public final class PearlTrajectorySimulator {

    private static final double SPAWN_EYE_OFFSET = 0.1;

    private static final double GRAVITY = 0.03;

    private static final float AIR_DRAG = 0.99F;

    private static final float WATER_DRAG = 0.8F;

    private static final int MAX_TICKS = 300;

    private static final double ENTITY_MARGIN = 0.3;

    private PearlTrajectorySimulator() {}

    public static PearlTrajectorySimulator.SimulationResult simulate(
            World world, ClientPlayerEntity player, float f) {
        Vec3d cameraPosVec = player.getCameraPosVec(f);
        Vec3d vec = new Vec3d(cameraPosVec.x, cameraPosVec.y - 0.1, cameraPosVec.z);
        Vec3d vec2 = player.getRotationVec(f).multiply(EnderPearlItem.POWER);
        Vec3d movement = player.getMovement();
        vec2 = vec2.add(movement.x, player.isOnGround() ? 0.0 : movement.y, movement.z);
        return simulate(world, vec, vec2, player, null);
    }

    public static PearlTrajectorySimulator.SimulationResult simulate(
            World world, ProjectileEntity projectileEntity) {
        return simulate(
                world,
                projectileEntity.getEntityPos(),
                projectileEntity.getVelocity(),
                projectileEntity,
                projectileEntity.getOwner());
    }

    public static PearlTrajectorySimulator.SimulationResult simulate(
            World world, Vec3d vec, Vec3d vec2, Entity entity, Entity entity2) {
        ArrayList list = new ArrayList(64);
        list.add(vec);
        Vec3d vec3 = vec;
        BlockHitResult blockHitResult = null;
        Entity entity3 = null;
        int i = 0;
        while (i < 300) {
            i++;
            Vec3d vec4 = vec2.add(0.0, -0.03, 0.0);
            Vec3d vec5 =
                    vec3.add(
                            vec2 =
                                    vec4.multiply(
                                            isInWater(world, vec3)
                                                    ? 0.800000011920929
                                                    : 0.9900000095367432));
            BlockHitResult blockHitResult2 =
                    world.raycast(
                            new RaycastContext(
                                    vec3, vec5, ShapeType.COLLIDER, FluidHandling.NONE, entity));
            Vec3d vec6 = blockHitResult2.getType() != Type.MISS ? blockHitResult2.getPos() : vec5;
            Entity entity4 = firstEntityOnSegment(world, vec3, vec6, entity, entity2);
            if (entity4 != null) {
                Vec3d vec7 = entity4.getBoundingBox().expand(0.3).raycast(vec3, vec6).orElse(vec6);
                list.add(vec7);
                entity3 = entity4;
                break;
            }
            if (blockHitResult2 instanceof BlockHitResult
                    && blockHitResult2.getType() != Type.MISS) {
                list.add(vec6);
                blockHitResult = blockHitResult2;
                break;
            }
            vec3 = vec5;
            list.add(vec5);
            if (vec5.y < world.getBottomY() - 64) {
                break;
            }
        }
        return new PearlTrajectorySimulator.SimulationResult(
                List.copyOf(list), (Vec3d) list.getLast(), blockHitResult, entity3, i);
    }

    private static Entity firstEntityOnSegment(
            World world, Vec3d vec, Vec3d vec2, Entity entity, Entity entity2) {
        Box box = new Box(vec, vec2).expand(1.0);
        Entity entity3 = null;
        double d = 1.7976931348623157E308;
        for (Entity entity4 :
                world.getOtherEntities(entity, box, PearlTrajectorySimulator::canHit)) {
            double squaredDistance;
            Optional optional;
            if (entity4 != entity2
                    && !(optional = entity4.getBoundingBox().expand(0.3).raycast(vec, vec2))
                            .isEmpty()
                    && (squaredDistance = vec.squaredDistanceTo((Vec3d) optional.get())) < d) {
                d = squaredDistance;
                entity3 = entity4;
            }
        }
        return entity3;
    }

    private static boolean canHit(Entity entity) {
        return EntityPredicates.EXCEPT_SPECTATOR.test(entity)
                && entity.canHit()
                && !(entity instanceof ClientPlayerEntity);
    }

    private static boolean isInWater(World world, Vec3d vec) {
        return world.getFluidState(BlockPos.ofFloored(vec)).isIn(FluidTags.WATER);
    }

    public static boolean isHoldingPearl(ClientPlayerEntity player) {
        return player.getMainHandStack().isOf(Items.ENDER_PEARL)
                || player.getOffHandStack().isOf(Items.ENDER_PEARL);
    }

    @Environment(EnvType.CLIENT)
    public record SimulationResult(
            List<Vec3d> points,
            Vec3d landing,
            BlockHitResult blockHit,
            Entity entityHit,
            int ticks) {

        public boolean landed() {
            return this.blockHit != null || this.entityHit != null;
        }
    }
}
