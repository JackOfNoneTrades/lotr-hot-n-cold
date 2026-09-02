# Hot N Cold configuration examples

These examples belong inside the `general` section of `config/hotncold.cfg`. Start with only the rules you need.
Registered entity and item names are case-sensitive; biome, group, faction, category, slot, and `empty` keywords are
case-insensitive.

## Stop the War of the Ring animals

This is the simplest configuration for the original elephant/Mumakil problem:

```properties
B:removeAllWarOfTheRingAnimalSpawns=true
```

It removes War of the Ring's natural animal entries from LOTR biomes. Existing animals, spawn eggs, commands,
breeding, mounts, and scripted spawns remain available.

To block only elephants in all LOTR biomes instead:

```properties
S:blockedEntitiesInAllLOTRBiomes <
    MoCreatures.Elephant
>
```

To block different mobs in particular regions:

```properties
S:blockedEntityBiomeRules <
    MoCreatures.Elephant:shire
    MoCreatures.Lion:nearHarad
>
```

## Add natural spawns

The format is `entity:biome:category:weight:smallestGroup:largestGroup`:

```properties
S:addedEntityBiomeRules <
    MoCreatures.Elephant:nearHarad:creature:4:1:2
    MoCreatures.Lion:nearHarad:creature:8:1:3
>
```

Use `/hotncold spawns example nearHarad MoCreatures.Elephant creature` in-game or at the server console to generate
a validated starting line. Blocking always wins if the same entity is both added and blocked.

## Random NPC gear

This example creates a group spanning Gondor and Rohan, then gives its members weighted melee and helmet choices:

```properties
S:lotrNPCGroupMembers <
    guards;lotr.GondorSoldier
    guards;lotr.GondorArcher
    guards;lotr.RohanWarrior
>

S:lotrNPCWeaponRules <
    group:guards;lotr:item.swordGondor;10
    group:guards;lotr:item.swordRohan;5
    group:guards;empty;1
>

S:lotrNPCArmorRules <
    group:guards;helmet;lotr:item.helmetGondor;10
    group:guards;helmet;lotr:item.helmetRohan;5
    group:guards;helmet;empty;1
>
```

Weight is relative: weights `10`, `5`, and `1` mean 10 out of 16, 5 out of 16, and 1 out of 16 selections.

Ranged weapons and shields have independent lists:

```properties
S:lotrNPCRangedWeaponRules <
    lotr.GondorArcher;lotr:item.gondorBow;10
    lotr.GondorArcher;lotr:item.ironCrossbow;2
    lotr.GondorArcher;empty;1
>

S:lotrNPCShieldRules <
    faction:GONDOR;ALIGNMENT_GONDOR;10
    faction:GONDOR;empty;1
>
```

Rule priority is exact NPC, first matching custom group, faction, then `all`, independently for every gear slot.
The default safety settings preserve hired, custom-named, quest-linked, and persistent/location-specific NPCs.

## Reload and inspect

```text
/hotncold spawns reload
/hotncold spawns dump shire creature
/hotncold spawns explain shire MoCreatures.Elephant
/hotncold equipment reload
/hotncold equipment explain group:guards
```

Reloads affect future natural spawns only. They never rewrite existing entities or NPC gear.
