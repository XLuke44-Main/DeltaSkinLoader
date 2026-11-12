package com.XLuke44.DeltaSkinLoader.mixin;

import com.XLuke44.DeltaSkinLoader.DeltaSkinLoaderRuntime;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
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

    @Inject(method = "getElytraTextureLocation", at = @At("HEAD"), cancellable = true)
    private void deltaskinloader$elytraOverride(CallbackInfoReturnable<ResourceLocation> callback) {
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
}
