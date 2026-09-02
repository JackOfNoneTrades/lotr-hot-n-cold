# Migrating an existing Hot N Cold installation

## Before updating

1. Back up the world and `config/hotncold.cfg`.
2. Replace only the Hot N Cold jar. Keep LOTR Legacy and UniMixins installed.
3. War of the Ring, Mo' Creatures, EnviroMine, Custom NPCs, and Bad Mobs remain optional.
4. Start once so Forge appends the new settings, then stop before editing the configuration.

Existing frost, heat, biome, and EnviroMine settings retain their old behavior. New spawn and NPC-equipment lists are
empty by default, so updating alone does not alter spawning or gear.

## Replacing Bad Mobs rules for LOTR biomes

If Bad Mobs was used only to suppress the affected War of the Ring animals, remove those entries from Bad Mobs after
enabling the equivalent Hot N Cold rule. Choose one approach:

- Set `removeAllWarOfTheRingAnimalSpawns=true` to remove every animal entry War of the Ring adds to LOTR biomes.
- Add an entity to `blockedEntitiesInAllLOTRBiomes` to block it naturally throughout Middle-earth.
- Add `entityName:biomeName` entries to `blockedEntityBiomeRules` for regional blocks.

Hot N Cold and Bad Mobs can remain installed together, but maintaining the same blacklist in both places makes later
troubleshooting harder. Hot N Cold's rules intentionally leave existing mobs, eggs, commands, breeding, mounts, and
scripted spawns alone.

## Adding NPC equipment gradually

Begin with one NPC or group and inspect it with `/hotncold equipment explain <target>`. Rules affect only future
naturally or LOTR-world-generation spawned NPCs.

The safest initial mode is:

```properties
B:replaceExistingLOTREquipment=false
B:customizeHiredLOTREquipment=false
B:customizeNamedLOTREquipment=false
B:customizeQuestLOTREquipment=false
B:customizePersistentLOTREquipment=false
```

This fills only empty slots and protects player-managed or special NPCs. Change `replaceExistingLOTREquipment` to
`true` when the configured choices should replace LOTR's normal gear.

Equipment target priority is exact NPC, first matching custom group, faction, then `all`. If migrating broad faction
rules, check whether a newer exact or group rule now overrides them for the same slot.

## Validate the migration

Run these commands after editing:

```text
/hotncold spawns reload
/hotncold spawns explain shire MoCreatures.Elephant
/hotncold equipment reload
/hotncold equipment explain lotr.GondorSoldier
```

Read the startup or reload summaries for rejected names, duplicate choices, invalid weights, or incompatible armor.
Keep the backup until newly generated terrain and an existing area have both been played without unexpected spawns.
