package dev.kurok.mods.dstensurafix.mixin;

import by.dragonsurvivalteam.dragonsurvival.client.render.ClientDragonRenderer;
import io.github.manasmods.manascore.config.ConfigRegistry;
import io.github.manasmods.tensura.config.client.HudConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Neutralizes dstensuracompat's HUD forcing so the vanilla Tensura portrait
 * HUD stays active.
 *
 * <p>Compat calls {@code enforceHudConfigValues()} at init and on every client
 * tick, forcing {@code status.render=false}, {@code statusBars
 * .defaultRendering=false} and pinning the bars to (69, 44) - values that then
 * get persisted into the config file. We cancel the method and replicate only
 * its non-HUD intent ({@code renderItemsInMouth=true}); if the config still
 * carries compat's magic (69, 44) signature, the vanilla defaults are restored
 * once. Tensura re-reads its position caches every frame from
 * {@code OverlayHandler#renderHud}, so the restore takes effect immediately.
 */
@Mixin(targets = "com.dstensuracompat.client.ClientEventHandler")
public abstract class ClientEventHandlerMixin {

    @Inject(method = "enforceHudConfigValues", at = @At("HEAD"), cancellable = true)
    private void dstensurafix$restoreVanillaHudConfig(CallbackInfo ci) {
        // Compat also enables items rendered in the dragon's mouth - keep that.
        ClientDragonRenderer.renderItemsInMouth = Boolean.TRUE;

        HudConfig config = (HudConfig) ConfigRegistry.getConfig(HudConfig.class);
        HudConfig.StatusBars bars = config.statusBars;
        // (69, 44) is compat's pinned position - the residue of its earlier
        // forced writes. Vanilla defaults: position (0, 0), defaultRendering
        // on, portrait on.
        if (bars.positionX == 69.0F && bars.positionY == 44.0F) {
            bars.positionX = 0.0F;
            bars.positionY = 0.0F;
            bars.defaultRendering = true;
            config.status.render = true;
        }

        // Never let compat's forcing run again.
        ci.cancel();
    }
}
