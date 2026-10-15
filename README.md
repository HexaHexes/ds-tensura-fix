# Tensura Dragon Skin Rebalance

A NeoForge mixin mod for the **Tensura RPG** pack (MC 1.21.1 / NeoForge 21.1.259) that makes Tensura's **Dragon Skin** toggle-passive usable for dragon races, and rebalances its defense so it stacks fairly with real armor.

## What it does

Dragon Skin grants flat armor + toughness equal to a full set of your EP-tier dragon material — but the stock skill was unusable and unbalanced in three ways:

1. **Couldn't be slotted.** `canBeSlotted` was overridden to `mastery < 0` (always false, since mastery starts at 0 and only grows), so the skill could never be placed in a skill slot. → Restored to `true` (base-class behavior).
2. **Hard armor restriction.** `hasArmor(player)` gated both `canBeToggled` (blocks toggling on while armored) and `onTick` (instantly force-toggles off if armored). → Neutralized to `false` at the source, so the passive toggles on and stays on regardless of armor.
3. **No balance with real armor.** → The granted defense and toughness are now reduced **17.5% per equipped armor piece** (head/chest/legs/feet), recomputed **live** whenever armor changes while the passive is active — no toggle off/on needed.

### Resulting bonus per armor worn

| Pieces | Bonus |
|---|---|
| 0 | 100% |
| 1 | 82.5% |
| 2 | 65% |
| 3 | 47.5% |
| 4 (full set) | 30% |

The live recompute piggybacks on the `hasArmor` call inside `onTick` (runs every tick while toggled), server-side only, and only touches the `tensura:dragon_skin` attribute modifier when its value actually changes.

## Dependencies (all required — load fails without them)

| Mod | Version range | Why |
|---|---|---|
| NeoForge | `[21.1,)` | Platform |
| Minecraft | `[1.21.1,1.21.2)` | Platform |
| Tensura (`tensura`) | `[2.0,)` | Mixin target lives here (`DragonSkinSkill`) |
| ManasCore (`manascore`) | `[4.0,)` | `DragonSkinSkill`'s supertype chain (`TensuraSkill` → `ManasSkill`) |
| Dragon Survival Tensura Compat (`dstensuracompat`) | `[1.0,)` | Grants `DRAGON_SKIN` to the dragon races this rebalance targets |

## Tuning

`DragonSkinSkillMixin.java`:

- `DSTENSURAFIX_PER_PIECE` (default `0.175`) — armor-penalty per equipped piece. Set to `0` for the full bonus regardless of armor.

## Build

Requires JDK 21.

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
gradle build
```

Output: `build\libs\dstensurafix-1.0.0.jar`

## Install

Copy the jar into the instance's `mods\` folder.

The mixin config is `"required": false`, so if a future Tensura update changes `DragonSkinSkill`, the mixin disables itself with a log error instead of crashing the game.
