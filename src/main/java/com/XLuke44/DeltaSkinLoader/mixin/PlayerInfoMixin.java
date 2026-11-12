package com.XLuke44.DeltaSkinLoader.mixin;

import com.XLuke44.DeltaSkinLoader.DeltaSkinLoaderRuntime;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PlayerInfo.class, priority = 2000)
public abstract class PlayerInfoMixin {
    @Inject(method = "isSkinLoaded", at = @At("HEAD"), cancellable = true)
    private void deltaskinloader$localSkinLoaded(CallbackInfoReturnable<Boolean> callback) {
        if (DeltaSkinLoaderRuntime.getSkinOverride(((PlayerInfo) (Object) this).getProfile()) != null) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "isCapeLoaded", at = @At("HEAD"), cancellable = true)
    private void deltaskinloader$localCapeLoaded(CallbackInfoReturnable<Boolean> callback) {
        if (DeltaSkinLoaderRuntime.getCapeOverride(((PlayerInfo) (Object) this).getProfile()) != null) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "getSkinLocation", at = @At("HEAD"), cancellable = true)
    private void deltaskinloader$skinOverride(CallbackInfoReturnable<ResourceLocation> callback) {
        ResourceLocation override = DeltaSkinLoaderRuntime.getSkinOverride(
                ((PlayerInfo) (Object) this).getProfile());
        if (override != null) {
            callback.setReturnValue(override);
        }
    }

    @Inject(method = "getCapeLocation", at = @At("HEAD"), cancellable = true)
    private void deltaskinloader$capeOverride(CallbackInfoReturnable<ResourceLocation> callback) {
        ResourceLocation override = DeltaSkinLoaderRuntime.getCapeOverride(
                ((PlayerInfo) (Object) this).getProfile());
        if (override != null) {
            callback.setReturnValue(override);
        }
    }

    @Inject(method = "getElytraLocation", at = @At("HEAD"), cancellable = true)
    private void deltaskinloader$elytraOverride(CallbackInfoReturnable<ResourceLocation> callback) {
        ResourceLocation override = DeltaSkinLoaderRuntime.getElytraOverride(
                ((PlayerInfo) (Object) this).getProfile());
        if (override != null) {
            callback.setReturnValue(override);
        }
    }

    @Inject(method = "getModelName", at = @At("HEAD"), cancellable = true)
    private void deltaskinloader$modelOverride(CallbackInfoReturnable<String> callback) {
        PlayerInfo playerInfo = (PlayerInfo) (Object) this;
        String override = DeltaSkinLoaderRuntime.getModelOverride(
                playerInfo.getProfile(), playerInfo.getSkinLocation());
        if (override != null) {
            callback.setReturnValue(override);
        }
    }
}
