package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.efn.client.util.TranslationGuard;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStream;
import java.util.List;
import java.util.function.BiConsumer;

@Environment(EnvType.CLIENT)
@Mixin({TranslationStorage.class})
public abstract class MixinTranslationGuardClientLanguage {

    public MixinTranslationGuardClientLanguage() {}

    @Inject(
            method = {
                "load(Lnet/minecraft/resource/ResourceManager;Ljava/util/List;Z)Lnet/minecraft/client/resource/language/TranslationStorage;"
            },
            at = {@At("HEAD")},
            require = 0)
    private static void efn$beginLoad(
            ResourceManager resourceManager,
            List<String> list,
            boolean bl,
            CallbackInfoReturnable<TranslationStorage> cir) {
        TranslationGuard.beginLoad();
    }

    @WrapOperation(
            method = {"load(Ljava/lang/String;Ljava/util/List;Ljava/util/Map;)V"},
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/util/Language;load(Ljava/io/InputStream;Ljava/util/function/BiConsumer;)V")
            },
            require = 0)
    private static void efn$tapLoad(
            InputStream inValue,
            BiConsumer<String, String> biConsumer,
            Operation<Void> operation,
            @Local Resource resource) {
        operation.call(inValue, TranslationGuard.tap(resource, biConsumer));
    }

    @Inject(
            method = {
                "load(Lnet/minecraft/resource/ResourceManager;Ljava/util/List;Z)Lnet/minecraft/client/resource/language/TranslationStorage;"
            },
            at = {@At("RETURN")},
            require = 0)
    private static void efn$endLoad(
            ResourceManager resourceManager,
            List<String> list,
            boolean bl,
            CallbackInfoReturnable<TranslationStorage> cir) {
        TranslationGuard.endLoad();
    }
}
