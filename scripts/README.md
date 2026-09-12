# Production acceptance tests

These tests launch genuine obfuscated Minecraft 1.7.10 and Forge 1614 with unmodified release jars copied from a Fjord Launcher test instance. They do not use `runClient`/`runServer`, a development Minecraft jar, account credentials, or the development-only Wizardry model workaround. The fixture fails if Minecraft reports a development runtime.

## Run

Requires Java 8, Bash, `jq`, `rg`, `timeout`, and (for the client) Xvfb. The configured launcher instance must have been started once so its libraries and native files exist. Dedicated tests reuse the already accepted `run/server/eula.txt`; accept Minecraft's EULA yourself before running a server.

```sh
./gradlew --no-configuration-cache spotlessApply build productionFixtureJar
scripts/test-production.sh server full
WITH_BADMOBS=true scripts/test-production.sh client full
scripts/test-production.sh client friend
scripts/test-production.sh client baseline
scripts/test-production.sh client terrain
```

- `full`: spawn categories, replacement biomes, blocks/additions, commands, reloads, weighted equipment and protected NPCs, 100,000 elephant guard checks, plus actual NPC-spawner equipment, save/load, WOTR NPC preservation and the real WOTR Mumakil guard. The client also verifies received armor/shield state and a live empty-shield update.
- `friend`: reads `/tmp/hotncold.cfg`, validates the supplied 27-member Gondor group and fill-empty setting, then tests controlled equipment changes, actual NPC spawning, save/load and client synchronization. When History Items is installed, its actual `historyitems:breehelmet` is tested through group and exact rules and must be registered. Otherwise a known LOTR helmet is tested with an explicit exclusion; substitutions are not claimed as exact-pack compatibility.
- `baseline`: terrain and registry snapshot with WOTR but without Hot N Cold. Client only: WOTR's unpatched dedicated-server restriction prevents a dedicated baseline.
- `terrain`: the same snapshot with Hot N Cold's default configuration. Compare the two newly generated `terrain-snapshot.txt` files with `diff -u`.
- `compat`: observes generated Middle-earth terrain in a supplied reference build, without assuming that it contains the current restriction transformer or spawn/equipment features. Records actual Streams/Wild Caves blocks as well as cave air. A completed observation is not a positive assertion that every integration works.
- `streams`: searches up to 289 natural river-generation regions using the actual Middle-earth chunk provider, then populates a valid river's area and requires actual `streams:` water blocks. It fails if the compatibility implementation, river generator, valid river or water blocks are absent. No artificial river or terrain is inserted.
- `worldgen`: checks the merged product's actual Greg generator, cave air, Wild Caves blocks, configuration/blacklist handling, and Streams water when enabled. Also tests the Streams biome policy (names/IDs, WOTR replacement, allowed and forbidden biomes, mouth connectors, empty allowlist, disabled switch and Overworld isolation). The policy test uses a controlled biome provider; generation tests use the real world.
- `combined`: runs `worldgen`, then `friend`, in one process. This checks that the restored terrain integrations coexist with actual NPC spawning, gear save/load and client synchronization. Reads the friend configuration as with `friend`.

Each run prints its isolated directory under gitignored `run/`, containing the full `console.log`, jar checksums in `artifacts.sha256`, and a `production-result.txt` only after assertions pass. Missing-mod notices remain exclusions even when the available tests pass. Read the full log for underlying mods' warnings, not only the pass markers.

The NPC test controls the biome's candidate list, spawn interval, random position and terrain so it reliably exercises the real `LOTRSpawnerNPCs.performSpawning` loop. The production mod itself is not stubbed. This is a deterministic integration test, not a claim about long-term natural spawn frequency. The 100,000-check test is a guard stress test, not 100,000 live elephants.

Client equipment runs also render the actual tracked NPC and save `screenshots/equipment-preview.png`, showing the configured helmet alongside the same NPC without it. A successful render and screenshot are asserted; inspect the image before claiming the helmet looks correct. This is not automated pixel-perfect validation.

## Paths and reruns

Defaults use `$HOME/.local/share/FjordLauncher/instances/lotr-test`. Override `FJORD_ROOT`, `FJORD_INSTANCE`, `PRODUCTION_JAVA`, `FRIEND_CONFIG`, or `MINECRAFT_SERVER_JAR` if necessary. Dedicated tests default to the original server jar in Gradle's Minecraft download cache.

Use `PRODUCTION_MOD_JAR` for an exact released/reference jar, `PRODUCTION_EXTRA_MODS` for a folder of additional release jars, and `PRODUCTION_CONFIG` for an initial Hot N Cold configuration. These are copied into the isolated run; the supplied files are not modified. Record the precise dependency versions when reporting results. For example:

```sh
PRODUCTION_MOD_JAR=/tmp/hotncold-streams-greg-wild-test4.jar \
PRODUCTION_EXTRA_MODS="$PWD/run/compat-dependencies" \
PRODUCTION_CONFIG=/tmp/hotncold.cfg \
scripts/test-production.sh client streams
```

For the current combined build, omit `PRODUCTION_MOD_JAR` and run `client combined`. To test all compatibility switches off with dependencies still installed, set `PRODUCTION_CONFIG="$PWD/scripts/production-worldgen/all-disabled.cfg"` and run `client worldgen`. `empty-streams.cfg` checks an enabled Streams switch with an empty allowlist. `diagnostics.cfg` enables all three integrations and their per-chunk diagnostics. For independent-addon tests, point `PRODUCTION_EXTRA_MODS` at a folder containing only that addon and its dependencies (Streams/Farseek, Greg Caves/Mycelium, or Wild Caves 3).

To require the friend's actual custom helmet, put a History Items release jar in the extra-mods folder and set `REQUIRE_HISTORY_ITEMS=true` with `friend` or `combined`. The run then fails if the helmet is unavailable instead of substituting another item:

```sh
REQUIRE_HISTORY_ITEMS=true \
PRODUCTION_EXTRA_MODS="$PWD/run/compat-history-items-neid" \
scripts/test-production.sh client combined
```

Record the downloaded release and checksum, not only its mod-list text: the History Items 6.1 release contains stale `4.4.1` text in `mcmod.info`, although its mod annotation declares `6.1`. CurseForge file ID `8188686` is the tested 6.1 release; the friend's exact version must be confirmed separately.

The History Items 6.1 + WOTR 1.3.1 test stack exceeds Forge's normal block-ID range during WOTR registration, including with Hot N Cold absent. The `compat-history-items-neid` test folder adds NotEnoughIDs 2.1.11 and its GTNHLib 0.9.53 dependency, alongside History Items and the terrain addons. This is an explicit test-stack dependency, not a new Hot N Cold requirement. Keep ID-extension experiments in disposable saves; do not remove an ID extender from a world using extended IDs.

Each run has its own copied jars. Do not edit the launcher script while it is executing. For cave mods with random initialization, exact air/decorative/water counts are observations, not golden values; positive block assertions and all-disabled baseline comparisons serve different purposes.

Set `PRODUCTION_TEST_DIR` to an existing absolute test directory to reopen its world. Keep the same side/profile and Bad Mobs choice. Previous logs/results are archived inside that directory; a stale marker cannot turn a failure into a pass. For a terrain comparison, use fresh directories for both sides of the comparison.

## Optional NPC spawn sources

Run `scripts/test-production.sh client sources` and `scripts/test-production.sh server sources` to exercise real LOTR egg use, dispenser eggs, generated Gondor and Angmar towers, structure NPC respawners, and invasion waves. The profile rewrites its isolated config and executes the real equipment reload command for each case: switches off, each source separately, all sources on, fill-empty mode, persistent/named/hired/quest protections, and reload off/on. It checks saved entity data, unchanged existing NPCs, and client equipment tracking/rendering for each enabled source.

Use `PRODUCTION_EXTRA_MODS` and `REQUIRE_HISTORY_ITEMS=true` to require the actual `historyitems:breehelmet`. `PRODUCTION_BASE_MODS` can select an explicit folder of base release jars instead of the launcher's default LOTR/WOTR set (for example, LOTR v36.14 and UniMixins 0.3.1 without WOTR). Checksums record what was actually loaded. Structure and invasion geometry, weights, positions and random timing are controlled only by the fixture to make the real spawning code deterministic.

## Lightning sound regression

```sh
scripts/test-production.sh server lightning
scripts/test-production.sh client lightning
PRODUCTION_CONFIG="$PWD/scripts/production-lightning/muted.cfg" scripts/test-production.sh server lightning
PRODUCTION_CONFIG="$PWD/scripts/production-lightning/muted.cfg" scripts/test-production.sh client lightning
```

The `lightning` profile tests the actual transformed vanilla lightning entity in the Overworld and Middle-earth, with the sound switch off/on/off and LOTR's lightning-grief protection off/on. It observes sound calls and compares thunder arguments, damage, fire, random-number consumption and bolt lifetime; a real explosion's sound remains audible. The client additionally receives a real server weather entity and sound packets, then observes thunder, the expected presence/absence of the impact sound, an unchanged ordinary explosion sound, and the lightning flash. No audio recording or subjective listening is claimed. Repeat with `PRODUCTION_BASE_MODS` pointing at a LOTR-only stack to verify WOTR is not required.

### Development client: actual Wizardry command

The opt-in `clientLightningTest` fixture exercises `/cast lightning_bolt`, not a substitute spell or a direct test-created bolt. It checks the sound pipeline in both the Overworld and Middle-earth with impact muting enabled, disabled, and restored. It records the incoming and replacement sound names, requires LOTR's replacement thunder in Middle-earth, checks the visible flash, and verifies that a separate ordinary explosion still plays. This fixture is never packaged in the released mod.

Use a fresh isolated directory with a copy of the development client's configuration. The copied configuration must have `general.B:disableLightningExplosionSound=true` in `hotncold.cfg` and `B:"New weather"=true` in `lotr.cfg`. The fixture temporarily changes the runtime mute flag for comparisons; it does not rewrite the saved sound setting.

```sh
lightning_run=$(mktemp -d "$PWD/run/lightning-command-XXXXXX")
cp -a run/client/config "$lightning_run/config"
xvfb-run -a ./gradlew --configuration-cache \
  -PrunClientWorkingDirectory="$lightning_run" \
  -PclientLightningTest -PclientFixtureWorld=lightning-command runClient25
```

Use `runClient` instead for Java 8. Success requires five `CLIENT_LIGHTNING_COMMAND_CASE_PASSED` lines followed by `CLIENT_LIGHTNING_COMMAND_PASSED` and a clean shutdown. For a rerun in the same test world, also pass `-PclientFixtureTerrain=existing`. Do not point this terrain-modifying fixture at a world you play in.

When investigating a reported remaining sound, distinguish `random.explode` from `ambient.weather.thunder` (replaced by `lotr:ambient.weather.thunder` in Middle-earth with LOTR's new weather enabled). The option intentionally preserves thunder. The option is read at Minecraft startup, not by the spawn/equipment reload commands.

The fixture temporarily controls random seeds and terrain in its disposable world; the released mod does not. The startup config is restored after the off/on/off checks and used for the client sound test. The setting requires a normal game/server restart; the test does not add a sound reload command.

## Unsupported spawn-list regression

All `null-*` profiles register a test-only extra creature category that real LOTR biomes do not support. Run with `client` or `server`:

- `null-control`: start with blocking off.
- `null-block`: start with a global block for vanilla `Chicken` (the original crash reproduction).
- `null-biome`: start with `Chicken` blocked only in the Shire.
- `null-wotr`: start with broad WOTR animal cleanup enabled; use a WOTR test stack.
- `null-add`: start with an addition targeting an unsupported category.

Each profile then exercises all block modes, WOTR cleanup/counting and NPC protection when installed, valid additions to LOTR's extra ambient category, rejected unsupported additions, dump/explain/example commands, repeated reloads, and restoration of the original lists and entry objects. The client must actually enter the world. All profiles should pass on the fixed product. This reproduces the missing-list condition, not the specific addon introducing that category in the friend's full pack.

```sh
scripts/test-production.sh client null-block
scripts/test-production.sh client null-wotr
scripts/test-production.sh server null-wotr
```

Use `PRODUCTION_MOD_JAR` to compare against the reported `6781391` build: `null-block` must fail during the real server-starting event at `LOTRSpawnControl.removeBlockedEntries:550`. Do not count this expected negative control as an acceptance pass.

The fixture is packaged separately at `build/production-test/hotncold-production-fixture.jar` and must never be distributed as part of the mod or installed in a normal playing instance. Tests rewrite only their isolated copies, never the launcher instance or original friend configuration.
