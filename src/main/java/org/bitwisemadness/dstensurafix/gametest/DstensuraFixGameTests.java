package org.bitwisemadness.dstensurafix.gametest;

import org.bitwisemadness.dstensurafix.ManaBridge;
import io.github.manasmods.tensura.ability.skill.intrinsic.DragonSkinSkill;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Proves the DragonSkinSkillMixin actually applies at runtime: the mixin
 * cancels {@code canBeSlotted} at HEAD and forces true, so a freshly
 * constructed skill (mastery 0 - which vanilla would reject) must come back
 * slottable. The handler never touches the arguments, so passing nulls is
 * safe and keeps the test independent of ManasSkillInstance internals.
 *
 * <p>The HUD mixins in this mod are client-only and cannot run in a headless
 * GameTestServer; they are covered by the build (mixin targets resolved at
 * compile time) and the in-game checklist in the README.
 */
@GameTestHolder("dstensurafix")
@PrefixGameTestTemplate(false)
public class DstensuraFixGameTests {

    @GameTest(template = "empty")
    public static void dragonSkinSkillIsSlottable(GameTestHelper helper) {
        DragonSkinSkill skill = new DragonSkinSkill();
        if (!skill.canBeSlotted(null, null, 0)) {
            throw new GameTestAssertException(
                    "DragonSkinSkill.canBeSlotted returned false - DragonSkinSkillMixin not applied");
        }
        helper.succeed();
    }

    /**
     * Proves the mana bridge writes a correct modifier onto Tensura's
     * {@code tensura:max_magicule} attribute: a known stage-mana value must land as
     * a single ADD_MULTIPLIED_TOTAL modifier whose amount is mana * 0.01, runaway
     * values must clamp at the DS pool cap, non-positive values must clear the
     * modifier, and the {@link ManaBridge#sync} path must refuse to apply anything
     * to a player who is not a Dragon Survival dragon (clearing stale modifiers -
     * the remediation path for characters that carried the pre-fix runaway one).
     */
    @GameTest(template = "empty")
    public static void manaBridgeBoostsMagiculeMax(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        AttributeInstance attr = player.getAttribute(TensuraAttributes.MAX_MAGICULE);
        if (attr == null) {
            throw new GameTestAssertException("tensura:max_magicule attribute missing on player");
        }

        double before = attr.getValue();
        ManaBridge.apply(player, 100.0D);

        AttributeModifier mod = attr.getModifier(ManaBridge.DSTENSURAFIX_MANA_BRIDGE);
        if (mod == null) {
            throw new GameTestAssertException("mana bridge modifier was not applied to max_magicule");
        }
        if (mod.operation() != AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
            throw new GameTestAssertException("mana bridge modifier operation was " + mod.operation()
                    + " - expected ADD_MULTIPLIED_TOTAL");
        }
        if (mod.amount() != 1.0D) {
            throw new GameTestAssertException("mana bridge amount was " + mod.amount()
                    + " - expected 1.0 (100 mana * 0.01)");
        }
        if (attr.getValue() <= before) {
            throw new GameTestAssertException("max_magicule did not increase (before=" + before
                    + ", after=" + attr.getValue() + ")");
        }

        // Clamp: a runaway stage-mana read can never exceed DS's registered pool max.
        ManaBridge.apply(player, 1.0E9D);
        AttributeModifier clamped = attr.getModifier(ManaBridge.DSTENSURAFIX_MANA_BRIDGE);
        if (clamped == null) {
            throw new GameTestAssertException("mana bridge dropped the modifier at a large mana value");
        }
        double expectedCap = ManaBridge.DSTENSURAFIX_MANA_POOL_CAP * ManaBridge.DSTENSURAFIX_MANA_TO_MAGICULE;
        if (clamped.amount() != expectedCap) {
            throw new GameTestAssertException("mana bridge clamp failed: amount was " + clamped.amount()
                    + " - expected " + expectedCap + " (pool cap * ratio)");
        }

        // Non-positive mana clears the modifier entirely.
        ManaBridge.apply(player, -50.0D);
        if (attr.getModifier(ManaBridge.DSTENSURAFIX_MANA_BRIDGE) != null) {
            throw new GameTestAssertException("mana bridge kept a modifier for negative mana");
        }

        // sync() on a player who is not a DS dragon must clear, never apply -
        // the guard that breaks the compat mana/magicule feedback loop.
        ManaBridge.apply(player, 100.0D);
        ManaBridge.sync(player);
        if (attr.getModifier(ManaBridge.DSTENSURAFIX_MANA_BRIDGE) != null) {
            throw new GameTestAssertException("mana bridge sync applied a modifier to a non-dragon player");
        }

        helper.succeed();
    }
}
