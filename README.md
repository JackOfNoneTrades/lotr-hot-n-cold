# Hot N Cold: LOTR Addon

![logo](src/main/resources/assets/hotncold/logo.png)


## Description
This mod allows to grant immunity from frost and heat damage for arbitrary mobs, for the LOTR Legacy mod.
It also allows to configure which biomes are hot or frosty. This mod is compatible with the [War of the Ring mod](https://www.curseforge.com/minecraft/mc-mods/war-of-the-ring-mc) and can optionally provide LOTR biome temperatures to EnviroMine.

## Usage
Turn on the `printMobs` option in the config to print all possible entities to the console.
Then add those names in the respective config lists:
```
# List of mobs that should be immune to frost.
S:mobsImmuneToFrost <
    lotr.HighElf
    Sheep
 >

# List of mobs that should be immune to heat.
S:mobsImmuneToHeat <
    Pig
 >
```
You can make arbitrary biomes hot or frosty. Add their name or ID to the appropriate list.
The list can be printed if you enable the `printBiomes` config option.
```
# List of biomes that should apply the frost mechanic.
S:frostBiomes <
    96
    nearHarad
 >
```

## EnviroMine compatibility
If [EnviroMine](https://www.curseforge.com/minecraft/mc-mods/enviromine) is installed, this mod can populate LOTR biome ambient temperatures for EnviroMine through the `enviromineBiomeTemperatures` config list.

On a fresh config, the full list of LOTR biome entries is generated automatically on first startup after LOTR has registered its biomes.
The generated values use the same ambient temperature conversion that EnviroMine uses for its vanilla biome defaults.

Entries use the format `biomeName:temperatureC` or `biomeId:temperatureC`.
Example:
```
# LOTR biome ambient temperature overrides for EnviroMine, in celsius.
S:enviromineBiomeTemperatures <
    shire:26.5
    forodwaith:0
    nearHarad:36.4
    gorgoroth:41.6
 >
```

Existing `enviromineBiomeTemperatures` entries are left alone.
If you want the full generated default list to be recreated in an older config, delete that single config entry and start the game once.

## Other features
* Allows Custom NPCs to select LOTR biomes for NPC spawns
* Removes War of the Ring dedicated-server restriction

## War of the Ring animal spawns

War of the Ring adds a large number of natural animal spawns to LOTR biomes. They can all be disabled with:
```
B:removeAllWarOfTheRingAnimalSpawns=true
```

This affects future natural animal spawning only. It does not remove existing entities or disable spawn eggs, commands,
breeding, mounts, or scripted spawns.

### Rules that apply everywhere

Hot N Cold uses explicit global settings instead of a literal `*` wildcard:

- To remove every animal spawn contributed by War of the Ring from every LOTR biome, enable
  `removeAllWarOfTheRingAnimalSpawns`.
- To block one entity in every LOTR biome, put its name in `blockedEntitiesInAllLOTRBiomes`.
- To add an entity everywhere, add one `addedEntityBiomeRules` line for each desired LOTR biome. The
  `/hotncold spawns example` command generates a valid line for each biome you choose.

A literal `*` in a biome or entity field is rejected, so a typo cannot unexpectedly affect the whole dimension.

## Blocking selected entities in LOTR biomes

Specific entities can be prevented from spawning naturally in every LOTR biome by adding their registered names to:
```
S:blockedEntitiesInAllLOTRBiomes <
    MoCreatures.Elephant
>
```

Names are exact and case-sensitive. Set `B:printMobs=true` for one launch to print the available names to the log. This
setting checks every natural spawn category but does not remove existing entities or affect spawn eggs, commands,
breeding, mounts, or scripted spawns.

Blocked entities are also rejected at the final natural-spawn check and in LOTR world-generation spawning. This catches
entries added by another mod after server startup and prevents a blocked entity's own broken spawn check from running.
Direct spawning through commands, eggs, breeding, and scripts does not use this guard.

To block an entity only in selected LOTR biomes, add one `entityName:biomeName` or `entityName:biomeId` rule per line:
```
S:blockedEntityBiomeRules <
    MoCreatures.Elephant:shire
    MoCreatures.Lion:nearHarad
>
```

Biome names are matched without regard to capitalization. A rule applies to every matching War of the Ring replacement
biome as well as the original LOTR biome.

## Adding entities to LOTR biomes

Add natural spawns with `entityName:biomeName:category:weight:minGroup:maxGroup` rules. A biome ID can replace the
biome name:
```
S:addedEntityBiomeRules <
    MoCreatures.Elephant:shire:creature:10:1:3
    MoCreatures.Lion:nearHarad:creature:6:1:2
>
```

Categories are `creature`, `monster`, `waterCreature`, `ambient`, and `LOTRAmbient` when LOTR provides it. Weight is
relative frequency—a larger number makes that entry more likely compared with other entries in the same category.
The final two numbers are the smallest and largest group sizes. Values must be positive, and the maximum cannot be
smaller than the minimum.

An entity is not added twice to the same biome and category. Existing entries are preserved rather than silently
replaced. If an addition also matches a blocking rule, the blocking rule wins.

## Inspecting biome spawns

Use the read-only dump command from chat or the server console to see the effective spawn list after additions and
blocks have been applied:
```
/hotncold spawns dump shire
/hotncold spawns dump shire creature
```

The optional category keeps large lists manageable. The output includes every matching LOTR or War of the Ring biome
variant, registered entity names, relative weights, and group sizes. Biome IDs can be used instead of names, and tab
completion is available for biome names, IDs, and categories.

To check one entity and see why it is allowed or blocked, use:
```
/hotncold spawns explain shire MoCreatures.Elephant
```

The explanation reports `BLOCKED`, `PRESENT`, or `ABSENT` for every matching LOTR or War of the Ring biome variant. It
names any global block, biome-specific block, or War of the Ring cleanup setting responsible, and shows matching spawn
entries and configured additions. Tab completion is available for biome and registered entity names.

To generate ready-to-copy config values using entities, biomes, and categories from the installed modpack, use:
```
/hotncold spawns example shire MoCreatures.Elephant creature
```

The category is optional and defaults to `creature`. The command prints examples for
`blockedEntitiesInAllLOTRBiomes`, `blockedEntityBiomeRules`, and `addedEntityBiomeRules`. Addition examples use
weight `10` and group size `1-3` as clearly labelled starting values to edit. Biome IDs are accepted, and tab
completion is available for every argument.

After editing the spawn settings in `hotncold.cfg`, an operator or the server console can apply them without restarting:
```
/hotncold spawns reload
```

Reload affects `removeAllWarOfTheRingAnimalSpawns`, `blockedEntitiesInAllLOTRBiomes`,
`blockedEntityBiomeRules`, `addedEntityBiomeRules`, and the blocked-attempt logging settings described below. Hot N
Cold first undoes only the exact spawn-list changes it made previously, then applies the new rules once. Unrelated
entries added by another mod are preserved, and the command prints a short summary of the changes.

Server startup also prints one `LOTR spawn summary` line with the number of entries added, the total removed with a
breakdown by reason, and configured addition targets rejected because that entity was already present. Invalid rules
still receive their specific warning messages.

Blocked natural spawn attempts can optionally be summarized in the server log:
```
B:logBlockedSpawnAttempts=true
I:blockedSpawnLogIntervalSeconds=60
```

The first blocked attempt is reported immediately. Further attempts are combined into at most one line per configured
interval, including the total and the three most common entity, biome, and spawn-path combinations. Logging is disabled
by default and can be enabled or disabled with `/hotncold spawns reload`.

## Configuring naturally spawned LOTR NPC equipment

Use `target;itemName;weight` entries to replace melee weapons. A target can be an exact NPC name, a custom group, a
faction, or `all`.
Add one line for every possible weapon:
```
S:lotrNPCWeaponRules <
    LOTR.GondorSoldier;lotr:swordGondor;10
    LOTR.GondorSoldier;lotr:hammerGondor;3
    LOTR.GondorSoldier;empty;1
    group:guards;lotr:swordGondor;5
    faction:ROHAN;lotr:swordRohan;10
    all;lotr:swordBronze;1
>
```

The weight controls relative likelihood: in this example, the sword has weight ten and the hammer has weight three.
Entity and item names are exact and case-sensitive. Invalid names, non-LOTR entities, non-positive weights, and
duplicate choices are rejected with a clear warning, and startup prints a short `LOTR NPC equipment summary`.

Ranged weapons use the same target, item, and weight format in their own setting:
```
S:lotrNPCRangedWeaponRules <
    LOTR.GondorArcher;lotr:item.gondorBow;10
    LOTR.GondorArcher;lotr:item.ironCrossbow;2
    LOTR.GondorArcher;empty;1
>
```

These choices replace LOTR's separate ranged-weapon inventory slot. In fill-empty mode, an NPC that already has a
bow or other ranged weapon keeps it.

LOTR shields use their built-in shield codes rather than item registry names:
```
S:lotrNPCShieldRules <
    LOTR.GondorSoldier;ALIGNMENT_GONDOR;10
    LOTR.GondorSoldier;ALIGNMENT_ROHAN;2
    LOTR.GondorSoldier;empty;1
    faction:GONDOR;ALIGNMENT_GONDOR;10
>
```

Shield codes are exact and case-sensitive. Common codes follow `ALIGNMENT_FACTION`, such as `ALIGNMENT_GONDOR` and
`ALIGNMENT_ROHAN`; an invalid code is rejected with a warning.

Weapon rules run after LOTR finishes creating a naturally spawned NPC's normal equipment, including LOTR biome
world-generation spawns. A selected melee item replaces the NPC's melee, mounted-melee, idle, and held weapon state;
a selected ranged item replaces its ranged-weapon inventory slot, and a selected shield replaces its displayed LOTR
shield. Existing NPCs and NPCs introduced through commands, spawn eggs, or unrelated scripted spawning are not
changed.

Armor uses `target;slot;itemName;weight`, with independent choices for `boots`, `leggings`, `chest`, and
`helmet`:
```
S:lotrNPCArmorRules <
    LOTR.GondorSoldier;helmet;lotr:helmetGondor;10
    LOTR.GondorSoldier;helmet;lotr:helmetGondorWinged;1
    LOTR.GondorSoldier;helmet;empty;1
    LOTR.GondorSoldier;chest;lotr:bodyGondor;10
    LOTR.GondorSoldier;leggings;lotr:legsGondor;10
    LOTR.GondorSoldier;boots;lotr:bootsGondor;10
>
```

Each configured armor slot makes its own weighted selection. Items that are not armor, or armor assigned to the wrong
slot, are rejected with a warning rather than equipped incorrectly.

Define your own cross-faction or profession-style groups by listing one exact NPC name per member:
```
S:lotrNPCGroupMembers <
    guards;LOTR.GondorSoldier
    guards;LOTR.GondorArcher
    guards;LOTR.RohanWarrior
    archers;LOTR.GondorArcher
    archers;LOTR.RohanArcher
>
```

Then use `group:guards` or `group:archers` as the target in any melee, ranged, shield, or armor rule. Group names ignore
capitalization. If an NPC belongs to several groups that define the same slot, the first matching group listed in
`lotrNPCGroupMembers` wins.

A target beginning with `faction:` applies to every NPC whose LOTR faction matches that code. For example,
`faction:GONDOR` can provide a common equipment pool for Gondor soldiers, archers, and other Gondor NPCs. The target
`all` provides a fallback for every LOTR NPC. Priority is exact NPC rule, custom group, faction, then `all`,
independently for every equipment slot. This allows broad defaults with specific exceptions.

The special item name `empty` gives weapons and armor slots a weighted chance to remain empty. Its likelihood is
calculated like any other choice: a helmet rule with item weights `10` and `1`, plus an empty weight of `1`, has a
one-in-twelve empty chance.

By default, configured choices replace the NPC's normal equipment:
```
B:replaceExistingLOTREquipment=true
```

Set this to `false` to preserve every occupied weapon, armor, ranged-weapon, or shield slot and apply configured
choices only to slots LOTR left empty. This setting affects newly spawned NPCs only.

Hired NPCs, NPCs carrying an explicit custom name tag, quest-linked NPCs, and persistent or location-specific NPCs
are protected by default:
```
B:customizeHiredLOTREquipment=false
B:customizeNamedLOTREquipment=false
B:customizeQuestLOTREquipment=false
B:customizePersistentLOTREquipment=false
```

The persistent/location-specific protection covers LOTR's named characters and NPCs tied to special locations or
structures. The quest protection covers NPCs with a current shared or player-specific quest offer, an open offer, or
an active quest participant. LOTR's ordinary generated NPC names do not count as custom names. Set an option to `true`
only when that category should also receive configured equipment.

After editing any equipment setting, an operator can reload and validate the rules without restarting:
```
/hotncold equipment reload
```

The command prints the number of accepted and rejected choices. Reloading changes only the rules used for future NPC
spawns; it does not alter NPCs already in the world.

To inspect the prepared choices, weights, mode, and safety settings for an exact NPC type, use:
```
/hotncold equipment explain LOTR.GondorSoldier
/hotncold equipment explain group:guards
/hotncold equipment explain faction:GONDOR
/hotncold equipment explain all
```

NPC names are exact and case-sensitive. Group names, faction codes, and `all` are case-insensitive. Tab completion is
available for registered LOTR NPCs, configured groups, faction targets, and `all`.

## Downloads
<!--* [CurseForge ![curse](images/icons/curse.png)](https://www.curseforge.com/minecraft/mc-mods/fentlib)
* [Modrinth ![modrinth](images/icons/modrinth.png)](https://modrinth.com/mod/fentlib)-->
* [Git ![git](images/icons/git.png)](https://github.com/JackOfNoneTrades/lotr-hot-n-cold/releases)

## Dependencies
* [UniMixins](https://modrinth.com/mod/unimixins) ([![curse](images/icons/curse.png)](https://www.curseforge.com/minecraft/mc-mods/unimixins), [![modrinth](images/icons/modrinth.png)](https://modrinth.com/mod/unimixins/versions), [![git](images/icons/git.png)](https://github.com/LegacyModdingMC/UniMixins/releases)) is a required dependency.
* [EnviroMine](https://www.curseforge.com/minecraft/mc-mods/enviromine) is optional and is only needed for the `enviromineBiomeTemperatures` integration.

## Building

`./gradlew build`.

To start a dedicated development server with LOTR but without the optional WOTR, Mo' Creatures, IVToolkit, or
EnviroMine integrations, use:
```
./gradlew runServer -PlotrOnlyRuntime -PrunServerWorkingDirectory=run/lotr-only-server
```

This separate working directory keeps the LOTR-only smoke test isolated from the full WOTR server fixture.

For a development client, use `./gradlew runClient` or `./gradlew runClient25`. These tasks include development-only
compatibility for the War of the Ring 1.3.1 mod bundle; the compatibility fixture is not included in published jars.

## License

`LgplV3 + SNEED`.

<br>

![license](images/lgplsneed_small.png)
