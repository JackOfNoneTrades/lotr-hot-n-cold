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
## MineFantasy 2 / Campfire Backport

When **both** mods are installed, Hot N Cold enables firepit-style fuel and food burning on Campfire Backport campfires in all dimensions. Either mod can be absent without affecting the other.

```cfg
general {
    B:enableMineFantasyCampfires=true
    D:campfireFoodBurnChance=0.25
}
```

These are server settings, read at startup. Set `enableMineFantasyCampfires=false` to restore Campfire Backport's normal behavior. `campfireFoodBurnChance` ranges from `0.0` (never burn) to `1.0` (always burn).

Right-click with a MineFantasy firepit fuel to add one item's burn time, up to ten minutes. This uses MineFantasy's actual fuel function, including timber material modifiers. In MineFantasy 2.8.14.6, sticks provide 30 seconds; its timber and cut timber provide 10 seconds times their material modifier, and timber panes provide 30 seconds times the modifier. Vanilla logs, planks and coal are not accepted by that firepit fuel function. Fuel is not consumed from the player's hand in creative mode or when the campfire is full.

New campfires and existing campfires without saved fuel start empty. Add fuel, then light the campfire with a normal Campfire Backport ignitor. Fuel counts down only while the placed campfire is lit. At zero fuel it extinguishes and keeps its cooking inventory. Extinguishing and relighting preserves remaining fuel; relighting never refills it. Regular, soul and signal campfires all need fuel. Fuel survives world/chunk reloads; breaking a campfire discards its remaining fuel. MineFantasy's HUD displays the remaining fuel when looking at a campfire with fuel, and disappears when it is empty.

Cooking keeps Campfire Backport's four slots and recipe times. Each completed food recipe rolls once: by default it has a 25% chance to produce MineFantasy's Burnt Food instead of the normal food output, preserving the output count. Multi-input recipes roll once for the combined result. Non-food outputs and recipe byproducts are unchanged. This does not copy MineFantasy's manual cooking attempts, provisioning skill bonuses or XP.

Campfire Backport's ordinary rain, water and oxygen extinguishing still applies. Its automatic burnout timer is replaced by the fuel supply while this integration is enabled; fuel exhaustion extinguishes the block rather than applying the optional "Burn to Nothing" destruction chance.

Tested releases: MineFantasy II 2.8.14.6 and Campfire Backport 1.11.3. With the regular Campfire jar, load UniMixins first (for example, name its jar `!unimixins-0.3.1.jar`) so Campfire's bundled older Mixin does not take precedence. Campfire also publishes a `+nomixin` variant for installations that already provide Mixin.

## Weather 2 precipitation

With Weather 2 installed, Hot N Cold restores vanilla precipitation and the dimension's own weather renderer. Middle-earth therefore uses LOTR's rain, snow, Mordor ash and desert sandstorms. Weather 2's separate rain/snow particles are suppressed. Its camera renderer replacement is bypassed while this patch is enabled, keeping LOTR's renderer stable instead of letting the two mods replace it every tick. Tornadoes, storm progression, wind, clouds, hail and other effects keep their existing behavior and settings.

```cfg
general {
    B:enableWeather2VanillaRain=true
    I:weather2RainDelaySeconds=120
}
```

These settings are read at startup. With Hot N Cold and Weather 2 on both sides, the **server's settings** are sent to clients. Restart the server and clients after changing them. With only a client installation, the patch can delay local effects but cannot change server-side rain mechanics.

When a Weather 2 rainstorm reaches the player, the blue sky fades toward darker storm-grey overcast and the horizon darkens. The full tint is about 30% darker than ordinary vanilla rain overcast. This tint and reduced sunlight apply during the dry lead-in. Weather 2's ordinary 30%-strength rain is scaled to full native precipitation intensity once the delay ends, making rain clearly visible while retaining native biome-specific rendering.

During the lead-in, Minecraft's rain-strength and rain-exposure checks report dry weather. This keeps rain ripples, rain-induced entity wetness, leaf drips and rain-sensitive campfires/torches from starting early when they use those checks. Native/global server rain mechanics wait too; the remaining delay is synchronized when a player joins or changes dimensions, and joining established native rain does not start another delay. Weather 2's localized storms still use a local onset timer at each player, resetting after clear skies or loading another world. This patch does not turn localized storms into dimension-wide server rain or change Weather 2's storm progression.

Rain/snow (including LOTR's special precipitation), ground splashes and rain sounds start after the configured delay. Set `weather2RainDelaySeconds=0` for immediate precipitation, or disable `enableWeather2VanillaRain` to restore Weather 2's original rendering and rain state. The delay accepts 0–3600 seconds and pauses with a paused single-player game. Dimensions outside Weather 2's weather list retain their native weather cycle and precipitation intensity; this patch does not add dimensions to Weather 2's lists or change storm frequency.

Test target: [Weather 2 2.3.20](https://www.curseforge.com/minecraft/mc-mods/weather-storms-tornadoes/files/2513048), with [CoroUtil 1.1.6](https://www.curseforge.com/minecraft/mc-mods/coroutil/files/2388794), for Minecraft 1.7.10. Use `./gradlew runClient -Pweather2Runtime` to include both in the development runtime. They remain optional and are not bundled into Hot N Cold.

Weather 2 lightning also honors the existing `B:disableLightningExplosionSound=true` setting in `hotncold.cfg`. It mutes only the lightning strike's `random.explode` sound, preserving thunder, the flash and ordinary explosion sounds. Weather 2 emits its lightning sounds locally, so set this on each **client** and restart; it works independently of the precipitation patch. Setting it to `false` restores both lightning sounds.
