package com.example.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.toast.ToastManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Environment(EnvType.CLIENT)
@Mixin({ToastManager.class})
public interface AccessorHudToastManager {
    @Accessor("visibleEntries")
    List<?> efn$hudVisibleToasts();
}
