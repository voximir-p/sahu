package org.voximir.sahu.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.voximir.sahu.client.RecoilHandler;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Shadow @Final private MinecraftClient client;

    @Inject(method = "renderWorld", at = @At("HEAD"))
    private void onRenderWorld(RenderTickCounter tickCounter, CallbackInfo ci) {
        RecoilHandler.renderFrame(client, tickCounter.getTickProgress(false));
    }
}

