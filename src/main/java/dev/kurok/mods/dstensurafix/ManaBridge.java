package dev.kurok.mods.dstensurafix;

import by.dragonsurvivalteam.dragonsurvival.common.handlers.magic.ManaHandler;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Bridges Dragon Survival's mana pool into Tensura's magicule maximum.
 *
 * <p>Dragon Survival grows a dragon's mana pool with its growth stage (from the
 * stage-parity datapack, roughly 10 mana at newborn up to ~100 at ancient for a
 * 4-stage species, and up to the DS cap of 1024). Tensura spends magicules on
 * skills and magic, but a dragon's DS mana progression did nothing for its
 * magicule maximum. This bridge feeds the DS pool back into Tensura so DS growth
 * translates into usable magicules.
 *
 * <p>Mechanics: on every server player tick, the player's raw DS max mana
 * ({@link ManaHandler#getRawMaxMana(Player)}, the pool plus any experience
 * bonus) is converted at {@link #DSTENSURAFIX_MANA_TO_MAGICULE} into a single
 * {@link AttributeModifier.Operation#ADD_MULTIPLIED_TOTAL} modifier on
 * {@code tensura:max_magicule}. As a multiply-total operation it scales the
 * player's whole magicule maximum by (1 + mana * ratio), so it stacks fairly on
 * top of whatever base and bonuses Tensura already grants. Because the mana pool
 * grows with stage, a newborn dragon gets a small boost and an ancient dragon a
 * large one - the modifier is recomputed live and tracks the pool as it grows.
 *
 * <p>{@code max_magicule} is a syncable attribute, so the server-side modifier
 * syncs to the client on its own; the handler runs server-side only to stay
 * authoritative and avoid client/server desync.
 */
public final class ManaBridge {

    /** Fixed modifier id, so the bridge replaces its own modifier instead of stacking duplicates. */
    public static final ResourceLocation DSTENSURAFIX_MANA_BRIDGE =
            ResourceLocation.fromNamespaceAndPath("dstensurafix", "mana_bridge");

    /** Fraction of the DS mana pool converted into a magicule multiplier (1% per point of mana). */
    public static final double DSTENSURAFIX_MANA_TO_MAGICULE = 0.01D;

    private ManaBridge() {
    }

    /** Server-side tick hook: recompute the bridge for the ticking player. */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return; // attributes are server-authoritative and sync to the client
        }
        sync(player);
    }

    /** Read the player's current DS max mana and apply the matching magicule modifier. */
    public static void sync(Player player) {
        apply(player, ManaHandler.getRawMaxMana(player));
    }

    /**
     * Apply (or refresh / clear) the bridge modifier for a given DS max-mana value.
     * Kept separate from {@link #sync(Player)} so the arithmetic can be tested with
     * a known mana value instead of whatever the mock player's DS state happens to be.
     */
    public static void apply(Player player, double rawMaxMana) {
        AttributeInstance attr = player.getAttribute(TensuraAttributes.MAX_MAGICULE);
        if (attr == null) {
            return; // should not happen - max_magicule is a generic attribute on every entity
        }

        double amount = rawMaxMana * DSTENSURAFIX_MANA_TO_MAGICULE;
        AttributeModifier current = attr.getModifier(DSTENSURAFIX_MANA_BRIDGE);

        if (amount <= 0.0D) {
            if (current != null) {
                attr.removeModifier(DSTENSURAFIX_MANA_BRIDGE);
            }
            return;
        }

        // Replace only when the amount actually changed, to avoid per-tick sync churn.
        if (current == null || current.amount() != amount) {
            attr.addOrReplacePermanentModifier(
                    new AttributeModifier(DSTENSURAFIX_MANA_BRIDGE, amount,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }
}
