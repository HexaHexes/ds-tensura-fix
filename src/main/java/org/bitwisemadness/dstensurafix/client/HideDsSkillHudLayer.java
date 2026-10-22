package org.bitwisemadness.dstensurafix.client;

import by.dragonsurvivalteam.dragonsurvival.client.gui.hud.MagicHUD;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

/**
 * Hides Dragon Survival's magic HUD layer (ability skill bar, mana orbs and
 * cast bar) when the Tensura portrait HUD is on screen.
 *
 * <p>The layer is registered by {@code DragonSurvivalClient#registerGuiLayers}
 * under the name {@link MagicHUD#ID}, and NeoForge's {@code GuiLayerManager}
 * fires {@link RenderGuiLayerEvent.Pre} for every layer - vanilla and
 * mod-registered alike - so cancelling the event skips the draw entirely.
 *
 * <p>This mod requires dstensuracompat (which requires Dragon Survival), so
 * the guard is unconditional: whenever dstensurafix is loaded the DS skill
 * layer is hidden and the vanilla Tensura HUD (restored by the mixins in this
 * mod) is the only player HUD.
 */
@EventBusSubscriber(modid = "dstensurafix", value = Dist.CLIENT)
public final class HideDsSkillHudLayer {

    private HideDsSkillHudLayer() {
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (MagicHUD.ID.equals(event.getName())) {
            event.setCanceled(true);
        }
    }
}
