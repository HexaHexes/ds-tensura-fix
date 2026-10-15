package dev.kurok.mods.dstensurafix.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fixes and rebalances Tensura's DragonSkinSkill for dragon races.
 *
 * Bugs corrected:
 *
 * 1) Armor restriction. DragonSkinSkill.hasArmor(player) returns true when the
 *    player wears armor, and that result gates two things:
 *      - canBeToggled(...) => !hasArmor(player)  (blocks toggling the passive ON)
 *      - onTick(...)       => if (hasArmor) toggle OFF  (instantly cancels it)
 *    We force hasArmor to always return false, so the toggle-passive can be
 *    turned on and stays on regardless of armor.
 *
 * 2) Slot restriction. canBeSlotted(...) was overridden to return
 *    (mastery < 0), which is essentially never true (mastery starts at 0 and
 *    only grows), so the skill could never be placed into a skill slot. We
 *    restore the base TensuraSkill behaviour (return true).
 *
 * Balance change (per user request):
 *
 * 3) Instead of the passive being useless while armored, the defense and
 *    toughness it grants are now reduced by 17.5% per equipped armor piece
 *    (head/chest/legs/feet). calculateArmor / calculateToughness are scaled at
 *    their return, and the applied DRAGON_ARMOR modifier is re-computed every
 *    tick (via the hasArmor call site in onTick) so the bonus updates live when
 *    armor is equipped or unequipped while the passive is active.
 *
 * The re-compute is guarded to run server-side only and only when the modifier
 * is already present (i.e. the skill is toggled on), so it never applies the
 * bonus while the passive is off and never causes client/server desync.
 */
@Mixin(targets = "io.github.manasmods.tensura.ability.skill.intrinsic.DragonSkinSkill")
public class DragonSkinSkillMixin {

    /** Same ID Tensura uses for the armor/toughness modifiers it applies on toggle-on. */
    private static final ResourceLocation DSTENSURAFIX_DRAGON_ARMOR =
            ResourceLocation.fromNamespaceAndPath("tensura", "dragon_skin");

    /** Reduction applied per equipped armor piece (17.5%). */
    private static final double DSTENSURAFIX_PER_PIECE = 0.175D;

    @Shadow
    protected int calculateArmor(LivingEntity entity) {
        throw new AssertionError("shadow");
    }

    @Shadow
    protected float calculateToughness(LivingEntity entity) {
        throw new AssertionError("shadow");
    }

    /**
     * Neutralize the armor requirement at its source, and re-apply the
     * DRAGON_ARMOR modifier so the granted defense tracks armor changes live.
     *
     * hasArmor is a private method called by both canBeToggled and onTick.
     * onTick invokes it every tick while the passive is toggled on, so this
     * injection also serves as a per-tick re-compute hook that already has the
     * LivingEntity in scope (no need for the ManasSkillInstance param).
     */
    @Inject(method = "hasArmor", at = @At("HEAD"), cancellable = true)
    private void dstensurafix$neverRequireArmorAndRecompute(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        // Live re-compute: refresh the applied modifier to match current armor.
        // Only on the server (attributes are server-authoritative) and only when
        // the modifier already exists (skill currently toggled on).
        if (entity != null && !entity.level().isClientSide()) {
            dstensurafix$refreshModifier(entity, Attributes.ARMOR, this.calculateArmor(entity));
            dstensurafix$refreshModifier(entity, Attributes.ARMOR_TOUGHNESS, this.calculateToughness(entity));
        }
        cir.setReturnValue(false);
    }

    /**
     * Scale the granted armor value down by 17.5% per equipped armor piece.
     */
    @Inject(method = "calculateArmor", at = @At("RETURN"), cancellable = true)
    private void dstensurafix$scaleArmor(LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        int base = cir.getReturnValue();
        double scale = dstensurafix$armorScale(entity);
        cir.setReturnValue((int) Math.round(base * scale));
    }

    /**
     * Scale the granted armor toughness down by 17.5% per equipped armor piece.
     */
    @Inject(method = "calculateToughness", at = @At("RETURN"), cancellable = true)
    private void dstensurafix$scaleToughness(LivingEntity entity, CallbackInfoReturnable<Float> cir) {
        float base = cir.getReturnValue();
        double scale = dstensurafix$armorScale(entity);
        cir.setReturnValue((float) (base * scale));
    }

    /**
     * Restore slotting. The stock override returned mastery < 0 (always false);
     * make it behave like the base class so the skill can be placed in a slot.
     */
    @Inject(method = "canBeSlotted", at = @At("HEAD"), cancellable = true)
    private void dstensurafix$alwaysSlottable(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }

    /**
     * If the given attribute already carries the DRAGON_ARMOR modifier and its
     * value differs from the freshly computed one, replace it. No-op when the
     * modifier is absent (passive off) or unchanged (avoids sync churn).
     */
    private static void dstensurafix$refreshModifier(LivingEntity entity,
                                                     net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                                                     double newValue) {
        AttributeInstance attr = entity.getAttribute(attribute);
        if (attr == null) {
            return;
        }
        AttributeModifier current = attr.getModifier(DSTENSURAFIX_DRAGON_ARMOR);
        if (current == null) {
            return; // passive not toggled on — do not apply the bonus
        }
        if (current.amount() != newValue) {
            attr.addOrReplacePermanentModifier(
                    new AttributeModifier(DSTENSURAFIX_DRAGON_ARMOR, newValue, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    /**
     * Bonus multiplier: 100% with no armor, minus 17.5% per equipped piece.
     * Clamped at zero for safety (players only have 4 armor slots, so this can
     * never realistically go negative, but modded slots could).
     */
    private static double dstensurafix$armorScale(LivingEntity entity) {
        int pieces = 0;
        if (entity.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof ArmorItem) {
            pieces++;
        }
        if (entity.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ArmorItem) {
            pieces++;
        }
        if (entity.getItemBySlot(EquipmentSlot.LEGS).getItem() instanceof ArmorItem) {
            pieces++;
        }
        if (entity.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof ArmorItem) {
            pieces++;
        }
        return Math.max(0.0D, 1.0D - DSTENSURAFIX_PER_PIECE * pieces);
    }
}
