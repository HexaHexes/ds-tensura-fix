# Agent rules and conventions — Tensura Dragon Skin Rebalance

**Repo:** `HexaHexes/tensura-dragon-skin-rebalance` · **Local folder:** `ds-tensura-fix` · **Mod ID:** `dstensurafix` · **Branch:** `main`
**Platform:** Minecraft 1.21.1 / NeoForge 21.1.259 / JDK 21 · **Jar:** `build/libs/dstensurafix-<version>.jar`

Makes Tensura's Dragon Skin toggle-passive usable for dragon races, rebalances its
defense to stack fairly with real armor, restores the vanilla Tensura HUD as the only
player HUD, and bridges the Dragon Survival mana pool into Tensura's magicule maximum.
**This is the sanctioned mixin mod** of the ecosystem — mixins are the
point, not a shortcut.

**Dependencies (all required — load fails without them)**
- Tensura (`tensura`), ManasCore (`manascore`),
  Dragon Survival (`dragonsurvival`),
  Dragon Survival Tensura Compat (`dstensuracompat`).

**Test:** `./gradlew runGametest` — 2 required tests. `expectedTests = 2`.

**Gotchas**
- The gametest proves that the mod + dependency closure load, that the
  `DragonSkinSkill` mixin applies (`canBeSlotted` forced true), and that the mana
  bridge writes a correct modifier onto `tensura:max_magicule`.
- **The mana bridge** (`ManaBridge`, tick-registered by the `DstensuraFix` `@Mod`
  entry point) reads the player's genuine DS stage pool - the `dragonsurvival:mana`
  attribute (registered max `1024`) plus the species XP bonus - and applies one
  `ADD_MULTIPLIED_TOTAL` modifier (id `dstensurafix:mana_bridge`) to
  `TensuraAttributes.MAX_MAGICULE`, amount = min(stageMana, 1024) *
  `DSTENSURAFIX_MANA_TO_MAGICULE` (default `0.01`). **Never read
  `ManaHandler.getRawMaxMana` here**: `dstensuracompat` mixins that method to
  return Tensura's max-magicule attribute value, so reading it fed this bridge's
  own modifier back into itself every tick and pinned max magicule at Tensura's
  1e9 attribute ceiling (fixed in 1.1.2). Only dragons carry the modifier;
  `sync` clears it for everyone else, which also remediates pre-fix characters.
  `apply(player, stageMana)` is kept separate from `sync(player)` so the
  gametest can drive it with a known mana value. `max_magicule` is a syncable
  generic attribute, so it reaches the client on its own. Do not change the
  operation to an add-type one - multiply-total is what makes it stack fairly
  on Tensura's own bonuses.
- **The HUD mixins are client-only and cannot run headless** — verify in-game:
  only the vanilla Tensura portrait HUD renders (no compat frame, no DS skill row),
  More Vanities reskin still applies, and the portrait's in-world model does not leave
  the dragon facing sideways.
- `DSTENSURAFIX_PER_PIECE` (default `0.175`) is the armor penalty per equipped piece;
  set to `0` for full bonus regardless of armor.
- The mixin config is `"required": false` — a target change on a Tensura/compat update
  disables the affected mixin with a log error instead of crashing. Keep it that way.
- ManasCore is a JarJar wrapper — extract its nested `META-INF/jars/*.jar` into `libs/`
  for compilation.

---

## Branch sync (do this first)

Before starting any new development in this repo:

1. `git status` — confirm what uncommitted work exists. Never disturb or sweep it.
2. `git pull --ff-only` — sync with the remote. A fast-forward is the normal case,
   because we push straight to the shared branch.
3. If the pull would **not** fast-forward (the branch has diverged), or if it would
   collide with local uncommitted work, **stop and ask** rather than merging, rebasing
   or stashing on your own.

## Scope doctrine

- **One mod + bundled datapack.** No KubeJS scripts.
- **Data-driven over code** — prefer JSON/datapack content to Java when both work.
- **Public API only, no mixins** in the colony-gear-style addon mods. Sanctioned
  exception: `ds-tensura-fix` (tensura-dragon-skin-rebalance), a mixin fix mod by design.

## Verification

- Verify with the in-repo GameTest run (`./gradlew runGametest`, or `runGameTestServer`).
  Repos without GameTests verify with `./gradlew build`. Never verify against a live
  CurseForge instance.
- Keep `expectedTests` in `build.gradle` in sync with the `@GameTest` methods.
- **Client-render features cannot be proven headless.** The honest scope is
  "build green + the user's in-game check" - say so rather than overclaiming.


## Build environment

- JDK 21 (`JAVA_HOME` -> JDK 21); the toolchain targets Java 21.
- The Gradle wrapper is committed. `./gradlew build` compiles, `./gradlew runGametest`
  (or `runGameTestServer`) runs the GameTest server.
- `libs/` (compile-only) and `testmods/` (runtime closure) are **gitignored** staging
  folders. Never commit third-party jars.
- On `run/` file-lock errors, kill any stale `java` process and retry.

## PowerShell quirks (this org is developed on Windows)

- A `git push` "error" in PowerShell is usually stderr passthrough — success is the
  `hash..hash  branch -> branch` line, not the absence of red text.
- `New-Object Type(args)` breaks on arithmetic — use `::new()` or a parenthesized
  argument.

## Git & release

- Remote is SSH: `git@github.com:HexaHexes/<name>.git`.
- Push only after tests are green **and** the user has given the go.
- Never sweep the user's uncommitted work into a commit — commit only what is intended.
- Commit subjects: concise, imperative.

## House style

- **No icons** unless the user explicitly asks. Icons are being removed across all repos.
- **No emojis, ever.** Prohibited in all code, docs, configs, and commit messages.
- READMEs are **player-facing only**: pitch, mechanics in plain terms, in-game usage,
  required/optional dependencies, credits, license. No build internals, no class names.
- All repos are **LGPL v2.1**, copyright Bitwise Madness. `NOTICE` / attribution files
  are mandatory and must never be deleted.
- Deploy to the Tensura RPG instance **only** when the user asks.

## Naming

- **Java base package:** `org.bitwisemadness.<project-name>` - repo name lowercased,
  separators stripped (e.g. `ds-tensura-fix` -> `org.bitwisemadness.dstensurafix`).
  The Maven `group` in `build.gradle` matches (`org.bitwisemadness`).
- Keep repo name, folder name, `mod_id`, and the Java package aligned; the rename
  checklist in the template's `README.md` walks the whole set.
- **The template (`neoforge-mod-template`) is canonical for org-wide rules.** When a
  rule is added or changed in any repo's `agents/AGENTS.md`, mirror the change into
  the template so new repos inherit it.

## CI & release model

- `ci.yml` currently enforces the **version-bump guard only**: a beta or stable bump must
  change the version file and nothing else; an `-alpha` bump may bundle work; a PR labelled
  `breaking` forces the next clean bump to raise the major or minor version.
- **Build and GameTest jobs are deferred.** Some dependencies (ManasCore's jar-in-jar
  sub-modules, plus a few pack mods) are not resolvable from a clean checkout via
  CurseMaven alone, so compiling in CI would fail every run. They land once a jar-fetch
  step exists. Until then, run `./gradlew build` and the GameTest task locally before
  pushing.
- `release.yml` is **not yet enabled for the gradle mod repos** - it needs a CI-built jar
  to attach. The datapack repo is the exception: its "build" is a zip, so it has a working
  `release.yml` that tags and publishes on a version bump.
- `version-bump.yml` opens an isolated version-bump PR (only the version file changes).
- CurseForge publishing (`curseforge.yml`) is **deferred** until a mod has a CurseForge
  listing and the `CURSEFORGE_PROJECT_ID` / `CURSEFORGE_API_KEY` secrets. Do not add it
  speculatively.
- Where a release workflow exists, alpha/nightly publishing is opt-in via the
  `ALPHA_RELEASES` repo variable and is **off** by default.

