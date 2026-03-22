package org.voximir.sahu.mixin.client;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.voximir.sahu.items.GunItem;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void onAttack(CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = (MinecraftClient) (Object) this;

        if (client.player != null && client.player.getMainHandStack().getItem() instanceof GunItem) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void onBlockBreaking(boolean breaking, CallbackInfo ci) {
        MinecraftClient client = (MinecraftClient) (Object) this;

        if (client.player != null && client.player.getMainHandStack().getItem() instanceof GunItem) {
            ci.cancel();
        }
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void onItemUse(CallbackInfo ci) {
        MinecraftClient client = (MinecraftClient) (Object) this;

        if (client.player != null && client.player.getMainHandStack().getItem() instanceof GunItem) {
            ci.cancel();
        }
    }
}