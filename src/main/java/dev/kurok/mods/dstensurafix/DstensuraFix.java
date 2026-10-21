package dev.kurok.mods.dstensurafix;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Mod entry point. The rest of this mod is mixins (which need no entry point),
 * but the mana bridge needs a tick listener, so this class registers it.
 */
@Mod(DstensuraFix.MOD_ID)
public class DstensuraFix {

    public static final String MOD_ID = "dstensurafix";

    public DstensuraFix() {
        NeoForge.EVENT_BUS.addListener(ManaBridge::onPlayerTick);
    }
}
