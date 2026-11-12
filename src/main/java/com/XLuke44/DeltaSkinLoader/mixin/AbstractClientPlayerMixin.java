package com.XLuke44.DeltaSkinLoader.mixin;

import com.XLuke44.DeltaSkinLoader.DeltaSkinLoaderRuntime;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractClientPlayer.class, priority = 2000)
public abstract class AbstractClientPlayerMixin {
    @Inject(method = "isSkinLoaded", at = @At("HEAD"), cancellable = true)
    private void deltaskinloader$localSkinLoaded(CallbackInfoReturnable<Boolean> callback) {
        if (DeltaSkinLoaderRuntime.getSkinOverride((AbstractClientPlayer) (Object) this) != null) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "isCapeLoaded", at = @At("HEAD"), cancellable = true)
    private void deltaskinloader$localCapeLoaded(CallbackInfoReturnable<Boolean> callback) {
        if (DeltaSkinLoaderRuntime.getCapeOverride((AbstractClientPlayer) (Object) this) != null) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "isElytraLoaded", at = @At("HEAD"), cancellable = true)
    private void deltaskinloader$localElytraLoaded(CallbackInfoReturnable<Boolean> callback) {
        if (DeltaSkinLoaderRuntime.getElytraOverride((AbstractClientPlayer) (Object) this) != null) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "getSkinTextureLocation", at = @At("HEAD"), cancellable = true)
    private void deltaskinloader$skinOverride(CallbackInfoReturnable<ResourceLocation> callback) {
        ResourceLocation override = DeltaSkinLoaderRuntime.getSkinOverride((AbstractClientPlayer) (Object) this);
        if (override != null) {
            callback.setReturnValue(override);
        }
    }
    @Inject(method = "getSkinTextureLocation", at = @At("RETURN"), cancellable = true)
    private void deltaskinloader$defaultSkinReturnOverride(CallbackInfoReturnable<ResourceLocation> callback) {
        ResourceLocation original = callback.getReturnValue();
        ResourceLocation override = DeltaSkinLoaderRuntime.getSkinReturnOverride(
                (AbstractClientPlayer) (Object) this, original);
        if (override != original) {
            callback.setReturnValue(override);
        }
    }

    @Inject(method = "getCloakTextureLocation", at = @At("HEAD"), cancellable = true)
    private void deltaskinloader$capeOverride(CallbackInfoReturnable<ResourceLocation> callback) {
        ResourceLocation override = DeltaSkinLoaderRuntime.getCapeOverride((AbstractClientPlayer) (Object) this);
        if (override != null) {
            callback.setReturnValue(override);
        }
    }

    @Inject(method = "getCloakTextureLocation", at = @At("RETURN"), cancellable = true)
    private void deltaskinloader$capeReturnOverride(CallbackInfoReturnable<ResourceLocation> callback) {
        ResourceLocation override = DeltaSkinLoaderRuntime.getCapeOverride((AbstractClientPlayer) (Object) this);
        if (override != null) {
            callback.setReturnValue(override);
        }
    }

    @Inject(method = "getElytraTextureLocation", at = @At("HEAD"), cancellable = true)
    private void deltaskinloader$elytraOverride(CallbackInfoReturnable<ResourceLocation> callback) {
        ResourceLocation override = DeltaSkinLoaderRuntime.getElytraOverride((AbstractClientPlayer) (Object) this);
        if (override != null) {
            callback.setReturnValue(override);
        }
    }

    @Inject(method = "getElytraTextureLocation", at = @At("RETURN"), cancellable = true)
    private void deltaskinloader$elytraReturnOverride(CallbackInfoReturnable<ResourceLocation> callback) {
        ResourceLocation override = DeltaSkinLoaderRuntime.getElytraOverride((AbstractClientPlayer) (Object) this);
        if (override != null) {
            callback.setReturnValue(override);
        }
    }

    @Inject(method = "getModelName", at = @At("HEAD"), cancellable = true)
    private void deltaskinloader$modelOverride(CallbackInfoReturnable<String> callback) {
        String override = DeltaSkinLoaderRuntime.getModelOverride((AbstractClientPlayer) (Object) this);
        if (override != null) {
            callback.setReturnValue(override);
        }
    }

    @Inject(method = "getModelName", at = @At("RETURN"), cancellable = true)
    private void deltaskinloader$modelReturnOverride(CallbackInfoReturnable<String> callback) {
        String override = DeltaSkinLoaderRuntime.getModelOverride((AbstractClientPlayer) (Object) this);
        if (override != null) {
            callback.setReturnValue(override);
        }
    }
}
