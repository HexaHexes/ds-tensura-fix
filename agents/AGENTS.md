# Agent rules and conventions â€” Tensura Dragon Skin Rebalance

**Repo:** `HexaHexes/tensura-dragon-skin-rebalance` Â· **Local folder:** `ds-tensura-fix` Â· **Mod ID:** `dstensurafix` Â· **Branch:** `main`
**Platform:** Minecraft 1.21.1 / NeoForge 21.1.259 / JDK 21 Â· **Jar:** `build/libs/dstensurafix-<version>.jar`

Makes Tensura's Dragon Skin toggle-passive usable for dragon races, rebalances its
defense to stack fairly with real armor, and restores the vanilla Tensura HUD as the
only player HUD. **This is the sanctioned mixin mod** of the ecosystem â€” mixins are the
point, not a shortcut.

**Dependencies (all required â€” load fails without them)**
- Tensura (`tensura`), ManasCore (`manascore`),
  Dragon Survival (`dragonsurvival`),
  Dragon Survival Tensura Compat (`dstensuracompat`).

**Test:** `./gradlew runGametest` â€” 1 required test. `expectedTests = 1`.

**Gotchas**
- The gametest proves only that the mod + dependency closure load and that the
  `DragonSkinSkill` mixin applies (`canBeSlotted` forced true).
- **The HUD mixins are client-only and cannot run headless** â€” verify in-game:
  only the vanilla Tensura portrait HUD renders (no compat frame, no DS skill row),
  More Vanities reskin still applies, and the portrait's in-world model does not leave
  the dragon facing sideways.
- `DSTENSURAFIX_PER_PIECE` (default `0.175`) is the armor penalty per equipped piece;
  set to `0` for full bonus regardless of armor.
- The mixin config is `"required": false` â€” a target change on a Tensura/compat update
  disables the affected mixin with a log error instead of crashing. Keep it that way.
- ManasCore is a JarJar wrapper â€” extract its nested `META-INF/jars/*.jar` into `libs/`
  for compilation.

---

## Branch sync (do this first)

Before starting any new development in this repo:

1. `git status` â€” confirm what uncommitted work exists. Never disturb or sweep it.
2. `git pull --ff-only` â€” sync with the remote. A fast-forward is the normal case,
   because we push straight to the shared branch.
3. If the pull would **not** fast-forward (the branch has diverged), or if it would
   collide with local uncommitted work, **stop and ask** rather than merging, rebasing
   or stashing on your own.

## Scope doctrine

- **One mod + bundled datapack.** No KubeJS scripts.
- **Data-driven over code** â€” prefer JSON/datapack content to Java when both work.
- **Public API only, no mixins** in the colony-gear-style addon mods. Sanctioned
  exception: `ds-tensura-fix` (tensura-dragon-skin-rebalance), a mixin fix mod by design.

## Verification

- The in-repo GameTest run is the **only** CI gate. Never verify against a live
  CurseForge instance.
- Keep `expectedTests` in `build.gradle` in sync with the `@GameTest` methods.
- **Client-render features cannot be proven headless.** The honest scope is
  "build green + the user's in-game check" â€” say so rather than overclaiming.

## Build environment

- JDK 21 (`JAVA_HOME` -> JDK 21); the toolchain targets Java 21.
- The Gradle wrapper is committed. `./gradlew build` compiles, `./gradlew runGametest`
  (or `runGameTestServer`) runs the GameTest server.
- `libs/` (compile-only) and `testmods/` (runtime closure) are **gitignored** staging
  folders. Never commit third-party jars.
- On `run/` file-lock errors, kill any stale `java` process and retry.

## PowerShell quirks (this org is developed on Windows)

- A `git push` "error" in PowerShell is usually stderr passthrough â€” success is the
  `hash..hash  branch -> branch` line, not the absence of red text.
- `New-Object Type(args)` breaks on arithmetic â€” use `::new()` or a parenthesized
  argument.

## Git & release

- Remote is SSH: `git@github.com:HexaHexes/<name>.git`.
- Push only after tests are green **and** the user has given the go.
- Never sweep the user's uncommitted work into a commit â€” commit only what is intended.
- Commit subjects: concise, imperative.

## House style

- **No icons** unless the user explicitly asks. Icons are being removed across all repos.
- READMEs are **player-facing only**: pitch, mechanics in plain terms, in-game usage,
  required/optional dependencies, credits, license. No build internals, no class names.
- All repos are **LGPL v2.1**, copyright Bitwise Madness. `NOTICE` / attribution files
  are mandatory and must never be deleted.
- Deploy to the Tensura RPG instance **only** when the user asks.

## CI & release model

- `ci.yml` runs on pull requests: a build job and a GameTest job. Both assert a
  non-zero result â€” a run that passes vacuously (0 tests) is a failure.
- `release.yml` runs on every push to the default branch, re-runs the tests, and â€” only
  when the version was bumped â€” tags and publishes a GitHub release with a cumulative
  changelog. Alpha/nightly publishing is opt-in via the `ALPHA_RELEASES` repo variable
  and is **off** by default.
- `version-bump.yml` opens an isolated version-bump PR (only the version file changes).
  Beta/stable bumps must not carry other changes; `-alpha` bumps may bundle work.
- CurseForge publishing (`curseforge.yml`) is **deferred** until a mod has a CurseForge
  listing and the `CURSEFORGE_PROJECT_ID` / `CURSEFORGE_API_KEY` secrets. Do not add it
  speculatively.

