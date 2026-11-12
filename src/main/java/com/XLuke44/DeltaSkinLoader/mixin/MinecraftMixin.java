package com.XLuke44.DeltaSkinLoader.mixin;

import com.XLuke44.DeltaSkinLoader.DeltaSkinLoaderRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "setLevel", at = @At("HEAD"))
    private void deltaskinloader$beforeClientWorldUnload(ClientLevel nextLevel, CallbackInfo callback) {
        Minecraft client = (Minecraft) (Object) this;
        if (client.level != null && client.level != nextLevel) {
            DeltaSkinLoaderRuntime.clearClientWorldState();
        }
    }
}
