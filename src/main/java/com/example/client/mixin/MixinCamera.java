package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.misc.FreecamModule;
import net.efn.client.module.impl.render.FreelookModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({Camera.class})
public abstract class MixinCamera {

    public MixinCamera() {}

    @Shadow
    protected abstract void setRotation(float f, float g);

    @Shadow
    protected abstract void setPos(double d, double e, double d2);

    @WrapOperation(
            method = {"update(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;ZZF)V"},
            at = {
                @At(
                        value = "INVOKE",
                        target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V")
            })
    private void efn$freelookRotation(Camera camera, float f, float g, Operation<Void> operation) {
        FreelookModule module = ModuleRegistry.get(FreelookModule.class);
        if (FreecamModule.active == null && module != null && module.isEnabled()) {
            operation.call(camera, module.cameraYaw(), module.cameraPitch());
        } else {
            operation.call(camera, f, g);
        }
    }

    @WrapOperation(
            method = {"update(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;ZZF)V"},
            at = {
                @At(
                        value = "INVOKE",
                        target = "Lnet/minecraft/client/render/Camera;clipToSpace(F)F")
            })
    private float efn$freelookThroughBlocks(Camera camera, float f, Operation<Float> operation) {
        return FreelookModule.throughBlocks() ? f : (Float) operation.call(camera, f);
    }

    @Inject(
            method = {"update(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;ZZF)V"},
            at = {@At("RETURN")})
    private void efn$freecamCamera(
            World world, Entity entity, boolean bl, boolean bl2, float f, CallbackInfo ci) {
        FreecamModule active = FreecamModule.active;
        if (active != null) {
            this.setPos(active.getX(f), active.getY(f), active.getZ(f));
            this.setRotation(active.getYaw(), active.getPitch());
        }
    }
}
