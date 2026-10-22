package org.bitwisemadness.dstensurafix.mixin;

import by.dragonsurvivalteam.dragonsurvival.registry.attachments.DSDataAttachments;
import by.dragonsurvivalteam.dragonsurvival.registry.attachments.MovementData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Guards the vanilla Tensura portrait's in-world model render against a
 * Dragon Survival side effect: {@code OverlayHandler#renderPlayer} renders the
 * player entity through {@code renderEntityInInventoryFollowsMouse}, which can
 * mutate the player's {@link MovementData} (body/head yaw and pitch) and leave
 * the dragon model facing sideways between HUD frames.
 *
 * <p>dstensuracompat solved this the same way for its own overlay - save the
 * facing angles before the render and restore them right after. We mirror that
 * exactly, keyed on the vanilla method's own head and every return.
 */
@Mixin(targets = "io.github.manasmods.tensura.handler.client.OverlayHandler")
public abstract class OverlayHandlerMixin {

    @Unique
    private static MovementData dstensurafix$savedMovement;
    @Unique
    private static double dstensurafix$savedBodyYaw;
    @Unique
    private static double dstensurafix$savedHeadYaw;
    @Unique
    private static double dstensurafix$savedHeadPitch;

    @Inject(method = "renderPlayer(ZZ)V", at = @At("HEAD"))
    private static void dstensurafix$saveDragonFacing(CallbackInfo ci) {
        dstensurafix$savedMovement = null;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        MovementData data = player.getData(DSDataAttachments.MOVEMENT);
        if (data == null) {
            return;
        }
        dstensurafix$savedMovement = data;
        dstensurafix$savedBodyYaw = data.bodyYaw;
        dstensurafix$savedHeadYaw = data.headYaw;
        dstensurafix$savedHeadPitch = data.headPitch;
    }

    @Inject(method = "renderPlayer(ZZ)V", at = @At("RETURN"))
    private static void dstensurafix$restoreDragonFacing(CallbackInfo ci) {
        if (dstensurafix$savedMovement != null) {
            dstensurafix$savedMovement.bodyYaw = dstensurafix$savedBodyYaw;
            dstensurafix$savedMovement.headYaw = dstensurafix$savedHeadYaw;
            dstensurafix$savedMovement.headPitch = dstensurafix$savedHeadPitch;
            dstensurafix$savedMovement = null;
        }
    }
}
