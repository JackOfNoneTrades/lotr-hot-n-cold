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

## Downloads
<!--* [CurseForge ![curse](images/icons/curse.png)](https://www.curseforge.com/minecraft/mc-mods/fentlib)
* [Modrinth ![modrinth](images/icons/modrinth.png)](https://modrinth.com/mod/fentlib)-->
* [Git ![git](images/icons/git.png)](https://github.com/JackOfNoneTrades/lotr-hot-n-cold/releases)

## Dependencies
* [UniMixins](https://modrinth.com/mod/unimixins) ([![curse](images/icons/curse.png)](https://www.curseforge.com/minecraft/mc-mods/unimixins), [![modrinth](images/icons/modrinth.png)](https://modrinth.com/mod/unimixins/versions), [![git](images/icons/git.png)](https://github.com/LegacyModdingMC/UniMixins/releases)) is a required dependency.
* [EnviroMine](https://www.curseforge.com/minecraft/mc-mods/enviromine) is optional and is only needed for the `enviromineBiomeTemperatures` integration.

## Building

`./gradlew build`.

## License

`LgplV3 + SNEED`.

<br>

![license](images/lgplsneed_small.png)
