# Tensura Dragon Skin Rebalance

Makes Tensura's **Dragon Skin** passive actually usable for dragon races, balances its
defense so it plays fair alongside real armor, restores the normal Tensura HUD
(removing the doubled ability rows some setups show), and feeds your Dragon Survival
mana pool into your Tensura magicules.

## What it changes

### Dragon Skin works and stays on

Dragon Skin is a toggle that grants flat armor and toughness based on your dragon
material. As shipped, three things stopped it from being usable:

- It **couldn't be slotted** into a skill slot at all. Now it can.
- Putting on any armor **force-toggled it off** and blocked turning it back on. Now it
  toggles on and stays on, armor or not.
- It stacked at full strength on top of real armor, which was too strong. Now the bonus
  shrinks fairly as you equip more armor pieces — recomputed live, so you never need to
  toggle it off and on.

| Armor pieces worn | Dragon Skin bonus |
|---|---|
| 0 | 100% |
| 1 | 82.5% |
| 2 | 65% |
| 3 | 47.5% |
| 4 (full set) | 30% |

### One clean HUD

The Tensura Compat mod takes over the player HUD and can leave you with two stacked
ability rows (Dragon Survival's skill bar plus Tensura's). This mod puts the normal
Tensura portrait HUD back in charge:

- Dragon Survival's extra skill/mana/cast row is hidden.
- The compat overlay's frame, portrait and HP/shield bars are removed.
- The portrait's 3D model no longer leaves your dragon facing sideways.
- **More Vanities** reskins still apply to the portrait.

### Dragon mana feeds your magicules

Dragon Survival grows a dragon's mana pool as it ages (from the stage-parity
datapack: roughly 10 mana as a newborn, up to about 100 as an ancient). Tensura
spends magicules on skills and magic, but that growing pool did nothing for your
magicules. This mod bridges the two:

- Your maximum magicules are multiplied by **1% of your Dragon Survival mana
  pool**. An ancient dragon around 100 mana gets roughly double the magicule
  maximum; a newborn around 10 mana gets a small 10% bump.
- The bonus tracks your mana pool live, so it grows as your dragon ages and
  stacks fairly on top of whatever magicule bonuses your race already grants.

Tune the conversion with `DSTENSURAFIX_MANA_TO_MAGICULE` in the config
(default `0.01` = 1% of the pool; set `0` to turn the bridge off).

## Getting it in-game

Install the jar and play — no configuration. If a future Tensura or Compat update
changes something the mod patches, the affected piece disables itself with a log error
instead of crashing your game.

To tune how hard the armor penalty bites, see `DSTENSURAFIX_PER_PIECE` in the config
(default `0.175` = 17.5% per piece; set `0` for the full bonus regardless of armor).

## Dependencies

All required — the mod won't load without them:

| Mod | Why |
|---|---|
| Tensura: Reincarnated | The Dragon Skin skill and HUD |
| ManasCore | Tensura's core library |
| [Dragon Survival](https://www.curseforge.com/minecraft/mc-mods/dragon-survival) | Dragon races |
| Dragon Survival Tensura Compat | Grants Dragon Skin to dragon races; the HUD being fixed |

Made for the **Tensura RPG** pack (Minecraft 1.21.1, NeoForge).

## License

GNU Lesser General Public License v2.1 — see [`LICENSE`](LICENSE).
Copyright (c) 2026 Bitwise Madness.
