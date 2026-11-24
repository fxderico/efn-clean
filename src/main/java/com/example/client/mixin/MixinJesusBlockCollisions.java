package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.efn.client.module.impl.movement.JesusModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockCollisionSpliterator;
import net.minecraft.world.CollisionView;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({BlockCollisionSpliterator.class})
public abstract class MixinJesusBlockCollisions {

    public MixinJesusBlockCollisions() {}

    @WrapOperation(
            method = {"computeNext()Ljava/lang/Object;"},
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/block/ShapeContext;getCollisionShape(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/CollisionView;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/util/shape/VoxelShape;")
            })
    private VoxelShape efn$jesusCollisionShape(
            ShapeContext shapeContext,
            BlockState state,
            CollisionView collisionView,
            BlockPos pos,
            Operation<VoxelShape> operation) {
        VoxelShape voxelShape =
                (VoxelShape) operation.call(shapeContext, state, collisionView, pos);
        JesusModule active = JesusModule.active;
        return active == null
                ? voxelShape
                : active.collisionShape(collisionView, state, pos, voxelShape);
    }
}
