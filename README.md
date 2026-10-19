# Tensura Dragon Skin Rebalance

A NeoForge mixin mod for the **Tensura RPG** pack (MC 1.21.1 / NeoForge 21.1.259) that makes Tensura's **Dragon Skin** toggle-passive usable for dragon races, rebalances its defense so it stacks fairly with real armor, and restores the vanilla Tensura HUD as the only player HUD (disabling dstensuracompat's HUD takeover).

## What it does

### 1. Dragon Skin rebalance (v1.0)

Dragon Skin grants flat armor + toughness equal to a full set of your EP-tier dragon material — but the stock skill was unusable and unbalanced in three ways:

1. **Couldn't be slotted.** `canBeSlotted` was overridden to `mastery < 0` (always false, since mastery starts at 0 and only grows), so the skill could never be placed in a skill slot. → Restored to `true` (base-class behavior).
2. **Hard armor restriction.** `hasArmor(player)` gated both `canBeToggled` (blocks toggling on while armored) and `onTick` (instantly force-toggles off if armored). → Neutralized to `false` at the source, so the passive toggles on and stays on regardless of armor.
3. **No balance with real armor.** → The granted defense and toughness are now reduced **17.5% per equipped armor piece** (head/chest/legs/feet), recomputed **live** whenever armor changes while the passive is active — no toggle off/on needed.

#### Resulting bonus per armor worn

| Pieces | Bonus |
|---|---|
| 0 | 100% |
| 1 | 82.5% |
| 2 | 65% |
| 3 | 47.5% |
| 4 (full set) | 30% |

The live recompute piggybacks on the `hasArmor` call inside `onTick` (runs every tick while toggled), server-side only, and only touches the `tensura:dragon_skin` attribute modifier when its value actually changes.

### 2. Vanilla Tensura HUD restored (v1.1)

dstensuracompat takes over the player HUD: it forces Tensura's config every tick (`status.render=false`, bars pinned to a custom position) and draws its own frame, portrait and HP/shield bars. This mod puts the vanilla Tensura portrait HUD back in charge, with **More Vanities**' reskin support intact (vanilla's renderer flows through its own reskin hooks), and removes the doubled ability rows:

1. **DS skill layer hidden.** Dragon Survival registers its skill/mana/cast-bar HUD as a NeoForge GUI layer named `MagicHUD.ID`; a `RenderGuiLayerEvent.Pre` listener cancels that layer, so DS no longer draws a second ability row under the Tensura skill slots. (The listener is unconditional — this mod requires dstensuracompat, which requires Dragon Survival.)
2. **Compat's overlay cancelled.** A mixin cancels `CustomHpOverlay.onRenderGui` at HEAD, so compat's frame, portrait, HP/shield bars and text never draw.
3. **Compat's config forcing neutralized.** A mixin cancels `ClientEventHandler.enforceHudConfigValues` (which ran at init and every client tick) and replicates only its non-HUD intent (`renderItemsInMouth=true`). If the saved config still carries compat's magic position signature (69, 44), the vanilla defaults (`(0,0)`, default rendering, portrait on) are restored once; Tensura re-reads its position caches every frame, so the restore applies immediately.
4. **Portrait facing guarded.** The vanilla portrait's in-world model render can leave Dragon Survival mutating the player's body/head yaw and pitch (the dragon model staring sideways). The mixin saves `MovementData` around `OverlayHandler.renderPlayer` and restores it — the same approach dstensuracompat uses for its own overlay.

## Dependencies (all required — load fails without them)

| Mod | Version range | Why |
|---|---|---|
| NeoForge | `[21.1,)` | Platform |
| Minecraft | `[1.21.1,1.21.2)` | Platform |
| Tensura (`tensura`) | `[2.0,)` | Mixin targets live here (`DragonSkinSkill`, `OverlayHandler`, `HudConfig`) |
| ManasCore (`manascore`) | `[4.0,)` | `DragonSkinSkill`'s supertype chain (`TensuraSkill` → `ManasSkill`), `ConfigRegistry` |
| Dragon Survival (`dragonsurvival`) | `[2.0,)` | `MagicHUD`, `ClientDragonRenderer`, `MovementData`/`DSDataAttachments` |
| Dragon Survival Tensura Compat (`dstensuracompat`) | `[1.0,)` | Grants `DRAGON_SKIN` to the dragon races this rebalance targets; HUD mixins' targets |

## Tuning

`DragonSkinSkillMixin.java`:

- `DSTENSURAFIX_PER_PIECE` (default `0.175`) — armor-penalty per equipped piece. Set to `0` for the full bonus regardless of armor.

## Build & test

Requires JDK 21. Compile-only mod jars go in `libs/`, gametest-run mod jars in `testmods/` (both gitignored). ManasCore is a JarJar wrapper — extract its nested `META-INF/jars/*.jar` into `libs/` for compilation.

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
gradle build          # jar -> build\libs\dstensurafix-1.1.0.jar
gradle runGametest    # headless GameTestServer in run/, graded by the build
```

The gametest (`1` required) proves the mod and its dependency closure load and that the DragonSkinSkill mixin applies (`canBeSlotted` forced true). The HUD mixins are client-only and cannot run headless — verify in-game:

- [ ] Only the vanilla Tensura portrait HUD renders, at the vanilla default position — no compat frame/HP bars, no DS skill/mana/cast bar.
- [ ] More Vanities reskin still applies to the portrait (if a vanity skin is selected).
- [ ] The portrait's in-world model does not leave the dragon facing sideways.

## Install

Copy the jar into the instance's `mods\` folder.

The mixin config is `"required": false`, so if a future Tensura or dstensuracompat update changes a target method, the affected mixin disables itself with a log error instead of crashing the game.
