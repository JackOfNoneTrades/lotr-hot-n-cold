package hotncold.production;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import net.minecraft.block.BlockSourceImpl;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.util.FakePlayer;

import org.fentanylsolutions.hotncold.Config;
import org.fentanylsolutions.hotncold.compat.LOTREquipmentReport;

import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import lotr.common.LOTRDimension;
import lotr.common.LOTRMod;
import lotr.common.LOTRShields;
import lotr.common.dispenser.LOTRDispenseSpawnEgg;
import lotr.common.entity.LOTREntities;
import lotr.common.entity.LOTREntityInvasionSpawner;
import lotr.common.entity.LOTREntityNPCRespawner;
import lotr.common.entity.npc.LOTREntityGondorSoldier;
import lotr.common.entity.npc.LOTREntityNPC;
import lotr.common.world.spawning.LOTRInvasions;
import lotr.common.world.structure.LOTRWorldGenAngmarTower;
import lotr.common.world.structure2.LOTRWorldGenGondorWatchtower;

/** Drives real spawning entry points and lets the real world-tick event apply equipment. */
public final class SpawnSourceChecks {

    private static final String[] STAGES = { "disabled", "eggs-only", "structures-only", "invasions-only", "all",
        "fill-empty", "persistent-protected", "named-protected", "named-enabled", "hired-protected", "quest-protected",
        "reload-off", "reload-on" };
    private static final int[] MASKS = { 0, 1, 2, 4, 7, 7, 7, 7, 7, 7, 7, 0, 7 };
    private final WorldServer world;
    private final FakePlayer player;
    private final Item helmet;
    private final Map<LOTREntityNPC, Before> subjects = new IdentityHashMap<>();
    private List<Object> beforeEntities;
    private int stage;
    private boolean started;
    private LOTREntityGondorSoldier savedControl;

    private SpawnSourceChecks(WorldServer world, Item helmet) {
        this.world = world;
        this.helmet = helmet;
        player = new FakePlayer(
            world,
            new GameProfile(UUID.fromString("aa34f753-398b-4fe1-9b9b-260e1356d9ce"), "SourceAcceptance"));
        player.capabilities.isCreativeMode = true;
        player.setPosition(160, 81, 0);
    }

    public static void start() {
        Item helmet = (Item) Item.itemRegistry.getObject("historyitems:breehelmet");
        if (Boolean.getBoolean("hotncold.fixture.requireHistoryItems")) {
            ProductionFixture.require(helmet != null, "History Items helmet required for spawn-source tests");
        }
        if (helmet == null) {
            helmet = LOTRMod.helmetRohanMarshal;
        }
        WorldServer world = DimensionManager.getWorld(LOTRDimension.MIDDLE_EARTH.dimensionID);
        ProductionFixture.require(world != null, "Middle-earth world is not loaded");
        SpawnSourceChecks checks = new SpawnSourceChecks(world, helmet);
        FMLCommonHandler.instance()
            .bus()
            .register(checks);
        ProductionFixture.LOG.info(
            "PRODUCTION_SOURCES_START: helmet={}, cases={}",
            Item.itemRegistry.getNameForObject(helmet),
            STAGES.length);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void tick(TickEvent.WorldTickEvent event) throws Exception {
        if (event.world != world || stage >= STAGES.length) {
            return;
        }
        if (event.phase == TickEvent.Phase.START && !started) {
            started = true;
            configure();
            prepareGround();
            beforeEntities = new ArrayList<>(world.loadedEntityList);
            world.playerEntities.add(player);
            spawnAndRecord("egg", 1, this::egg);
            spawnAndRecord("dispenser", 1, this::dispenser);
            spawnAndRecord("structure", 2, this::structure);
            spawnAndRecord("angmar-structure", 2, this::angmarStructure);
            spawnAndRecord("respawner", 2, this::respawner);
            spawnAndRecord("invasion", 4, this::invasion);
            spawnAndRecord("direct", 0, () -> {
                LOTREntityGondorSoldier npc = new LOTREntityGondorSoldier(world);
                npc.setPosition(8, 81, 0);
                npc.onSpawnWithEgg(null);
                world.spawnEntityInWorld(npc);
            });
            world.playerEntities.remove(player);
        } else if (event.phase == TickEvent.Phase.END && started) {
            verify();
            for (Object value : new ArrayList<>(world.loadedEntityList)) {
                if (!beforeEntities.contains(value) && value != savedControl
                    && !(value instanceof net.minecraft.entity.player.EntityPlayer)) {
                    world.removePlayerEntityDangerously((Entity) value);
                }
            }
            subjects.clear();
            stage++;
            started = false;
            if (stage == STAGES.length) {
                FMLCommonHandler.instance()
                    .bus()
                    .unregister(this);
                ProductionFixture.LOG.info(
                    "PRODUCTION_SOURCES_PASSED: independent switches, real eggs/dispenser/structures/respawner/invasion, protections, reload, persistence");
                ProductionFixture.completeServerChecks();
            }
        }
    }

    private void configure() {
        Configuration config = new Configuration(new File("config/hotncold.cfg"));
        int mask = MASKS[stage];
        set(config, "customizeSpawnEggLOTREquipment", (mask & 1) != 0);
        set(config, "customizeStructureLOTREquipment", (mask & 2) != 0);
        set(config, "customizeInvasionLOTREquipment", (mask & 4) != 0);
        set(config, "replaceExistingLOTREquipment", stage != 5);
        set(config, "customizePersistentLOTREquipment", stage != 6);
        set(config, "customizeNamedLOTREquipment", stage == 8);
        set(config, "customizeHiredLOTREquipment", false);
        set(config, "customizeQuestLOTREquipment", false);
        set(config, "logLOTREquipmentChanges", true);
        config.get("general", "lotrNPCArmorRules", new String[0])
            .set(new String[] { "all;helmet;" + Item.itemRegistry.getNameForObject(helmet) + ";100" });
        config.get("general", "lotrNPCWeaponRules", new String[0])
            .set(new String[] { "all;lotr:item.swordRohan;100" });
        config.get("general", "lotrNPCRangedWeaponRules", new String[0])
            .set(new String[] { "all;lotr:item.gondorBow;100" });
        config.get("general", "lotrNPCShieldRules", new String[0])
            .set(new String[] { "all;ALIGNMENT_ROHAN;100" });
        config.save();
        int commands = MinecraftServer.getServer()
            .getCommandManager()
            .executeCommand(MinecraftServer.getServer(), "hotncold equipment reload");
        ProductionFixture.require(commands == 1, "Equipment reload command failed");
        ProductionFixture.require(
            Config.customizeSpawnEggLOTREquipment == ((mask & 1) != 0)
                && Config.customizeStructureLOTREquipment == ((mask & 2) != 0)
                && Config.customizeInvasionLOTREquipment == ((mask & 4) != 0),
            "Spawn-source settings were not reloaded");
        if (stage == 6) {
            ProductionFixture.require(
                LOTREquipmentReport.createEquipmentExplanation("all")
                    .toString()
                    .contains("enable customizePersistentLOTREquipment"),
                "Missing persistent-NPC explanation");
        }
        ProductionFixture.LOG.info("PRODUCTION_SOURCE_STAGE: {}", STAGES[stage]);
    }

    private static void set(Configuration config, String key, boolean value) {
        config.get("general", key, false)
            .set(value);
    }

    private void prepareGround() {
        // These test platforms ensure valid spawning; structure geometry and NPC creation use LOTR itself.
        for (int center : new int[] { 0, 48, 128 }) {
            for (int x = center - 10; x <= center + 10; x++) {
                for (int z = -10; z <= 10; z++) {
                    world.setBlock(x, 80, z, Blocks.grass, 0, 2);
                    for (int y = 81; y <= 88; y++) {
                        world.setBlock(x, y, z, Blocks.air, 0, 2);
                    }
                }
            }
        }
    }

    private void egg() {
        ItemStack egg = eggStack();
        ProductionFixture.require(
            egg.getItem()
                .onItemUse(egg, player, world, 0, 80, 0, 1, 0, 0, 0),
            "LOTR spawn egg failed");
    }

    private ItemStack eggStack() {
        ItemStack egg = new ItemStack(
            LOTRMod.spawnEgg,
            1,
            LOTREntities.getEntityIDFromClass(LOTREntityGondorSoldier.class));
        if (stage == 7 || stage == 8) {
            egg.setStackDisplayName("Named source test");
        }
        return egg;
    }

    private void dispenser() {
        world.setBlock(4, 81, 0, Blocks.dispenser, 5, 2);
        ItemStack remaining = new LOTRDispenseSpawnEgg().dispense(new BlockSourceImpl(world, 4, 81, 0), eggStack());
        ProductionFixture.require(remaining.stackSize == 0, "Dispenser did not consume the egg");
    }

    private void structure() {
        LOTRWorldGenGondorWatchtower tower = new LOTRWorldGenGondorWatchtower(false);
        tower.restrictions = false;
        tower.shouldFindSurface = false;
        ProductionFixture.require(
            tower.generateWithSetRotation(world, new Random(1823), 0, 81, 64, 0),
            "Watchtower generation failed");
    }

    private void angmarStructure() {
        LOTRWorldGenAngmarTower tower = new LOTRWorldGenAngmarTower(false);
        tower.restrictions = false;
        ProductionFixture
            .require(tower.generate(world, new Random(7823), -64, 81, 64), "Angmar tower generation failed");
    }

    private void respawner() {
        LOTREntityNPCRespawner respawner = new LOTREntityNPCRespawner(world);
        respawner.setPosition(48, 81, 0);
        respawner.setSpawnClass(LOTREntityGondorSoldier.class);
        respawner.setCheckRanges(8, -1, 3, 3);
        respawner.setSpawnRanges(6, 0, 0, 8);
        respawner.setSpawnInterval(1);
        respawner.setNoPlayerRange(0);
        world.spawnEntityInWorld(respawner);
        respawner.onUpdate();
        respawner.setDead();
    }

    private void invasion() {
        LOTREntityInvasionSpawner invasion = new LOTREntityInvasionSpawner(world);
        invasion.setPosition(128, 81, 0);
        invasion.setInvasionType(LOTRInvasions.GONDOR);
        invasion.startInvasion(null, 12);
        List<LOTRInvasions.InvasionSpawnEntry> original = LOTRInvasions.GONDOR.invasionMobs;
        LOTRInvasions.GONDOR.invasionMobs = Arrays
            .asList(new LOTRInvasions.InvasionSpawnEntry(LOTREntityGondorSoldier.class, 1));
        ReflectionHelper.setPrivateValue(Entity.class, invasion, new Random(68193) {

            @Override
            public int nextInt(int bound) {
                return bound == 160 ? 0 : super.nextInt(bound);
            }
        }, "rand", "field_70146_Z");
        try {
            world.spawnEntityInWorld(invasion);
            for (int attempt = 0; attempt < 8; attempt++) {
                invasion.onUpdate();
            }
        } finally {
            LOTRInvasions.GONDOR.invasionMobs = original;
            invasion.setDead();
        }
    }

    private void spawnAndRecord(String source, int bit, Runnable action) {
        List<Object> previous = new ArrayList<>(world.loadedEntityList);
        action.run();
        int count = 0;
        for (Object value : world.loadedEntityList) {
            if (value instanceof LOTREntityNPC && !previous.contains(value)) {
                LOTREntityNPC npc = (LOTREntityNPC) value;
                if (stage == 7 || stage == 8) {
                    // Soldiers reject normal renaming. Set custom-name NBT as a script/editor can,
                    // after the real source finishes, to verify deferred protection checks.
                    NBTTagCompound named = new NBTTagCompound();
                    npc.writeToNBT(named);
                    named.setString("CustomName", "Named source test");
                    npc.readFromNBT(named);
                    ProductionFixture.require(npc.hasCustomNameTag(), "Custom-name NBT did not load");
                }
                if (stage == 9) {
                    npc.hiredNPCInfo.isActive = true;
                }
                if (stage == 10) {
                    NBTTagCompound quest = new NBTTagCompound();
                    NBTTagList players = new NBTTagList();
                    players.appendTag(
                        new NBTTagString(
                            player.getUniqueID()
                                .toString()));
                    quest.setTag("ActiveQuestPlayers", players);
                    npc.questInfo.readFromNBT(quest);
                }
                if (bit != 0) {
                    ProductionFixture.require(npc.isNPCPersistent, "Expected LOTR source's persistent flag: " + source);
                }
                subjects.put(npc, new Before(source, bit, npc));
                count++;
            }
        }
        ProductionFixture.require(count > 0, "Actual " + source + " produced no NPCs at stage " + STAGES[stage]);
    }

    private void verify() {
        Map<String, Integer> counts = new java.util.TreeMap<>();
        for (Map.Entry<LOTREntityNPC, Before> entry : subjects.entrySet()) {
            LOTREntityNPC npc = entry.getKey();
            Before before = entry.getValue();
            boolean changed = (MASKS[stage] & before.bit) != 0 && stage != 5
                && stage != 6
                && stage != 7
                && stage != 9
                && stage != 10;
            boolean fill = stage == 5 && (MASKS[stage] & before.bit) != 0;
            Item expected = changed || (fill && before.helmet == null) ? helmet : before.helmet;
            ProductionFixture.require(
                item(npc.getEquipmentInSlot(4)) == expected,
                "Wrong " + before.source + " helmet in " + STAGES[stage] + ": " + npc.getEquipmentInSlot(4));
            ProductionFixture.require(
                ItemStack.areItemStacksEqual(before.chest, npc.getEquipmentInSlot(3)),
                "Helmet rule changed chest armor");
            if (changed) {
                ProductionFixture.require(
                    item(npc.npcItemsInv.getMeleeWeapon()) == LOTRMod.swordRohan
                        && item(npc.npcItemsInv.getRangedWeapon()) == LOTRMod.gondorBow
                        && npc.npcShield == LOTRShields.ALIGNMENT_ROHAN,
                    "Missing configured weapons/shield for " + before.source);
                NBTTagCompound data = new NBTTagCompound();
                npc.writeToNBT(data);
                LOTREntityNPC restored = (LOTREntityNPC) LOTREntities.createEntityByClass(npc.getClass(), world);
                restored.readFromNBT(data);
                ProductionFixture.require(
                    item(restored.getEquipmentInSlot(4)) == helmet && restored.npcShield == LOTRShields.ALIGNMENT_ROHAN,
                    "Source equipment did not survive entity NBT save/load");
                if (stage == 4 && !ProductionFixture.visibilityLabels.contains(before.source)) {
                    ProductionFixture.addVisibilityCheck(npc, helmet, before.source);
                    if (savedControl == null && restored instanceof LOTREntityGondorSoldier) {
                        savedControl = (LOTREntityGondorSoldier) restored;
                        savedControl.setPosition(200, 81, 0);
                        world.spawnEntityInWorld(savedControl);
                        // Make an existing-NPC sentinel that future reloads must not overwrite.
                        savedControl.setCurrentItemOrArmor(4, new ItemStack(LOTRMod.helmetRanger));
                    }
                }
            } else if (fill) {
                ProductionFixture.require(
                    item(npc.npcItemsInv.getMeleeWeapon())
                        == (before.melee == null ? LOTRMod.swordRohan : item(before.melee)),
                    "Fill mode replaced an existing melee weapon");
                ProductionFixture.require(
                    item(npc.npcItemsInv.getRangedWeapon())
                        == (before.ranged == null ? LOTRMod.gondorBow : item(before.ranged)),
                    "Fill mode replaced an existing ranged weapon");
                ProductionFixture.require(
                    npc.npcShield == (before.shield == null ? LOTRShields.ALIGNMENT_ROHAN : before.shield),
                    "Fill mode replaced an existing shield");
            } else {
                ProductionFixture.require(
                    ItemStack.areItemStacksEqual(before.melee, npc.npcItemsInv.getMeleeWeapon())
                        && ItemStack.areItemStacksEqual(before.ranged, npc.npcItemsInv.getRangedWeapon())
                        && before.shield == npc.npcShield,
                    "Protected/disabled NPC gear changed for " + before.source);
            }
            counts.put(before.source, counts.getOrDefault(before.source, 0) + 1);
        }
        if (savedControl != null) {
            ProductionFixture.require(
                item(savedControl.getEquipmentInSlot(4)) == LOTRMod.helmetRanger,
                "Reload/ticking re-rolled existing NPC gear");
        }
        ProductionFixture.LOG.info("PRODUCTION_SOURCE_CASE_PASSED: stage={}, counts={}", STAGES[stage], counts);
    }

    private static Item item(ItemStack stack) {
        return stack == null ? null : stack.getItem();
    }

    private static final class Before {

        final String source;
        final int bit;
        final Item helmet;
        final ItemStack chest;
        final ItemStack melee;
        final ItemStack ranged;
        final LOTRShields shield;

        Before(String source, int bit, LOTREntityNPC npc) {
            this.source = source;
            this.bit = bit;
            helmet = item(npc.getEquipmentInSlot(4));
            chest = ItemStack.copyItemStack(npc.getEquipmentInSlot(3));
            melee = ItemStack.copyItemStack(npc.npcItemsInv.getMeleeWeapon());
            ranged = ItemStack.copyItemStack(npc.npcItemsInv.getRangedWeapon());
            shield = npc.npcShield;
        }
    }
}
