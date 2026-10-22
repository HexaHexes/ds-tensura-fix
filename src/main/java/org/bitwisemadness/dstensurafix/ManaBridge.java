package org.bitwisemadness.dstensurafix;

import by.dragonsurvivalteam.dragonsurvival.common.capability.DragonStateProvider;
import by.dragonsurvivalteam.dragonsurvival.common.handlers.magic.ManaHandler;
import by.dragonsurvivalteam.dragonsurvival.registry.DSAttributes;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Bridges Dragon Survival's stage mana pool into Tensura's magicule maximum.
 *
 * <p>Dragon Survival grows a dragon's mana pool with its growth stage (roughly
 * 10 mana at newborn up to ~100 at ancient for a 4-stage species; the DS mana
 * attribute is registered with a hard max of 1024). Tensura spends magicules on
 * skills and magic, but a dragon's DS stage progression did nothing for its
 * magicule maximum. This bridge feeds that stage pool into Tensura so DS growth
 * translates into usable magicules.
 *
 * <p>Mechanics: on every server player tick, a dragon's stage mana
 * ({@link #rawStageMana(Player)}) is converted at
 * {@link #DSTENSURAFIX_MANA_TO_MAGICULE} into a single
 * {@link AttributeModifier.Operation#ADD_MULTIPLIED_TOTAL} modifier on
 * {@code tensura:max_magicule}. As a multiply-total operation it scales the
 * player's whole magicule maximum by (1 + mana * ratio), so it stacks fairly on
 * top of whatever base and bonuses Tensura already grants. Because the mana pool
 * grows with stage, a newborn dragon gets a small boost and an ancient dragon a
 * large one - the modifier is recomputed live and tracks the pool as it grows.
 *
 * <p>The stage pool is read from the {@code dragonsurvival:mana} attribute
 * itself, never from {@code ManaHandler.getRawMaxMana}: the Dragon Survival
 * Tensura Compat mod mixins that method to return Tensura's max-magicule
 * attribute value, and reading it here fed this bridge's own modifier back into
 * itself every tick, inflating max magicule until it pinned at Tensura's 1e9
 * attribute ceiling. The attribute read can never exceed
 * {@value #DSTENSURAFIX_MANA_POOL_CAP}, so the bridge multiplier is bounded at
 * {@value #DSTENSURAFIX_MANA_POOL_CAP} * {@value #DSTENSURAFIX_MANA_TO_MAGICULE}
 * by construction and a feedback loop is impossible.
 *
 * <p>Only dragons carry the boost; {@link #sync(Player)} clears the modifier for
 * everyone else, which is also the remediation path for players who carried the
 * pre-fix runaway modifier.
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

    /** Hard max of DS's registered {@code dragonsurvival:mana} attribute - the pool can never exceed this. */
    public static final double DSTENSURAFIX_MANA_POOL_CAP = 1024.0D;

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

    /**
     * Read the player's genuine DS stage mana and apply the matching magicule modifier.
     * Non-dragons have any stale modifier cleared instead.
     */
    public static void sync(Player player) {
        if (!DragonStateProvider.isDragon(player)) {
            clear(player);
            return;
        }
        apply(player, rawStageMana(player));
    }

    /**
     * The genuine Dragon Survival stage pool: the {@code dragonsurvival:mana}
     * attribute (registered max {@value #DSTENSURAFIX_MANA_POOL_CAP}) plus the
     * species XP bonus, which is bounded by the species's maxManaFromLevels and
     * stubbed to zero by the compat mod.
     */
    public static double rawStageMana(Player player) {
        return player.getAttributeValue(DSAttributes.MANA)
                + ManaHandler.getBonusManaFromExperience(player);
    }

    /** Remove the bridge modifier, if present. */
    public static void clear(Player player) {
        AttributeInstance attr = player.getAttribute(TensuraAttributes.MAX_MAGICULE);
        if (attr != null && attr.getModifier(DSTENSURAFIX_MANA_BRIDGE) != null) {
            attr.removeModifier(DSTENSURAFIX_MANA_BRIDGE);
        }
    }

    /**
     * Apply (or refresh / clear) the bridge modifier for a known stage-mana value.
     * Kept separate from {@link #sync(Player)} so the arithmetic can be tested with
     * a known mana value instead of whatever the mock player's DS state happens to
     * be. The value is clamped to {@value #DSTENSURAFIX_MANA_POOL_CAP} so no caller
     * can grow the multiplier past the DS attribute's own ceiling.
     */
    public static void apply(Player player, double stageMana) {
        AttributeInstance attr = player.getAttribute(TensuraAttributes.MAX_MAGICULE);
        if (attr == null) {
            return; // should not happen - max_magicule is a generic attribute on every entity
        }

        double amount = Math.min(stageMana, DSTENSURAFIX_MANA_POOL_CAP) * DSTENSURAFIX_MANA_TO_MAGICULE;
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
