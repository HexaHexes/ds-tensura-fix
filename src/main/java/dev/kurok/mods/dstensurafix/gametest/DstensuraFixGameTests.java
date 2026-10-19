package dev.kurok.mods.dstensurafix.gametest;

import io.github.manasmods.tensura.ability.skill.intrinsic.DragonSkinSkill;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
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
}
