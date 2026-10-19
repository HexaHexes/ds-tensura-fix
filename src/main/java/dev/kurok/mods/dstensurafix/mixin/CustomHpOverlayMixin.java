package dev.kurok.mods.dstensurafix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops dstensuracompat's {@code CustomHpOverlay} from drawing its own frame,
 * portrait and HP/shield bars over the HUD. The listener method has no gating
 * - it draws for every player every frame - so cancelling it removes the
 * overlay completely; the vanilla Tensura HUD restored by the other mixins in
 * this config takes over.
 */
@Mixin(targets = "com.dstensuracompat.client.CustomHpOverlay")
public abstract class CustomHpOverlayMixin {

    @Inject(
            method = "onRenderGui(Lnet/neoforged/neoforge/client/event/RenderGuiEvent$Post;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void dstensurafix$skipCustomHpOverlay(CallbackInfo ci) {
        ci.cancel();
    }
}
