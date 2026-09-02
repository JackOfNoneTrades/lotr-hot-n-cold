package org.fentanylsolutions.hotncold.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.entity.passive.EntityCow;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;

import org.fentanylsolutions.hotncold.Config;
import org.junit.Test;

import lotr.common.entity.npc.LOTREntityGondorArcher;
import lotr.common.entity.npc.LOTREntityGondorSoldier;
import lotr.common.fac.LOTRFaction;

public class LOTREquipmentControlTest {

    private static final Item IRON_SWORD = new Item();
    private static final Item STONE_SWORD = new Item();
    private static final Item BOW = new Item();
    private static final ItemArmor IRON_HELMET = new ItemArmor(ItemArmor.ArmorMaterial.IRON, 0, 0);
    private static final ItemArmor CHAIN_HELMET = new ItemArmor(ItemArmor.ArmorMaterial.CHAIN, 0, 0);
    private static final ItemArmor IRON_CHEST = new ItemArmor(ItemArmor.ArmorMaterial.IRON, 0, 1);
    private static final ItemArmor IRON_LEGGINGS = new ItemArmor(ItemArmor.ArmorMaterial.IRON, 0, 2);
    private static final ItemArmor IRON_BOOTS = new ItemArmor(ItemArmor.ArmorMaterial.IRON, 0, 3);

    @Test
    public void groupsWeightedWeaponChoicesForExactLOTRNPCs() {
        LOTREquipmentControl.RulePreparation preparation = LOTREquipmentControl.resolveWeaponRules(
            new String[] { " LOTR.GondorSoldier ; minecraft:iron_sword ; 3 ",
                "LOTR.GondorSoldier;minecraft:stone_sword;1", "LOTR.GondorArcher;minecraft:bow;5" },
            entityResolver(),
            itemResolver());

        LOTREquipmentControl.WeightedItemRule soldierRule = preparation.weaponRules.get(LOTREntityGondorSoldier.class);
        LOTREquipmentControl.WeightedItemRule archerRule = preparation.weaponRules.get(LOTREntityGondorArcher.class);
        assertEquals(2, preparation.weaponRules.size());
        assertEquals(2, soldierRule.choices.size());
        assertEquals(4, soldierRule.totalWeight);
        assertEquals(1, archerRule.choices.size());
        assertEquals(
            "LOTR NPC equipment summary: prepared 3 weapon choice(s) for 2 exact NPC type(s); rejected 0 "
                + "invalid or duplicate choice(s).",
            preparation.describeStartup());
    }

    @Test
    public void rejectsMalformedUnknownNonNPCInvalidWeightAndDuplicateChoices() {
        LOTREquipmentControl.RulePreparation preparation = LOTREquipmentControl.resolveWeaponRules(
            new String[] { "invalid", ";minecraft:iron_sword;1", "missing;minecraft:iron_sword;1",
                "Cow;minecraft:iron_sword;1", "LOTR.GondorSoldier;missing;1",
                "LOTR.GondorSoldier;minecraft:iron_sword;0", "LOTR.GondorSoldier;minecraft:iron_sword;2",
                "LOTR.GondorSoldier;minecraft:iron_sword;3", null, "" },
            entityResolver(),
            itemResolver());

        assertEquals(1, preparation.weaponRules.size());
        assertEquals(1, preparation.weaponRules.get(LOTREntityGondorSoldier.class).choices.size());
        assertEquals(
            "LOTR NPC equipment summary: prepared 1 weapon choice(s) for 1 exact NPC type(s); rejected 7 "
                + "invalid or duplicate choice(s).",
            preparation.describeStartup());
    }

    @Test
    public void choosesItemsAtWeightedBoundaries() {
        LOTREquipmentControl.WeightedItemRule rule = LOTREquipmentControl.resolveWeaponRules(
            new String[] { "LOTR.GondorSoldier;minecraft:iron_sword;3", "LOTR.GondorSoldier;minecraft:stone_sword;1" },
            entityResolver(),
            itemResolver()).weaponRules.get(LOTREntityGondorSoldier.class);

        assertSame(IRON_SWORD, rule.choose(new FixedRandom(0)).item);
        assertSame(IRON_SWORD, rule.choose(new FixedRandom(2)).item);
        assertSame(STONE_SWORD, rule.choose(new FixedRandom(3)).item);
    }

    @Test
    public void groupsWeightedArmorChoicesIndependentlyByNPCAndSlot() {
        LOTREquipmentControl.ArmorRulePreparation preparation = LOTREquipmentControl.resolveArmorRules(
            new String[] { " LOTR.GondorSoldier ; HeLmEt ; minecraft:iron_helmet ; 3 ",
                "LOTR.GondorSoldier;helmet;minecraft:chainmail_helmet;1",
                "LOTR.GondorSoldier;chest;minecraft:iron_chestplate;2",
                "LOTR.GondorSoldier;leggings;minecraft:iron_leggings;2",
                "LOTR.GondorSoldier;boots;minecraft:iron_boots;2", "LOTR.GondorArcher;helmet;minecraft:iron_helmet;5" },
            entityResolver(),
            itemResolver());

        LOTREquipmentControl.ArmorRuleSet soldierRules = preparation.armorRules.get(LOTREntityGondorSoldier.class);
        LOTREquipmentControl.WeightedItemRule helmetRule = soldierRules.slotRules
            .get(LOTREquipmentControl.ArmorSlot.HELMET);
        assertEquals(2, preparation.armorRules.size());
        assertEquals(4, soldierRules.slotRules.size());
        assertEquals(2, helmetRule.choices.size());
        assertEquals(4, helmetRule.totalWeight);
        assertEquals(
            "LOTR NPC armor summary: prepared 6 choice(s) across 5 slot rule(s) for 2 exact NPC type(s); rejected 0 "
                + "invalid or duplicate choice(s).",
            preparation.describeStartup());
    }

    @Test
    public void rejectsInvalidArmorSlotsItemsWeightsAndDuplicates() {
        LOTREquipmentControl.ArmorRulePreparation preparation = LOTREquipmentControl.resolveArmorRules(
            new String[] { "invalid", ";helmet;minecraft:iron_helmet;1", "missing;helmet;minecraft:iron_helmet;1",
                "Cow;helmet;minecraft:iron_helmet;1", "LOTR.GondorSoldier;hat;minecraft:iron_helmet;1",
                "LOTR.GondorSoldier;helmet;missing;1", "LOTR.GondorSoldier;helmet;minecraft:bow;1",
                "LOTR.GondorSoldier;helmet;minecraft:iron_chestplate;1",
                "LOTR.GondorSoldier;helmet;minecraft:iron_helmet;0",
                "LOTR.GondorSoldier;helmet;minecraft:iron_helmet;2",
                "LOTR.GondorSoldier;helmet;minecraft:iron_helmet;3", null, "" },
            entityResolver(),
            itemResolver());

        assertEquals(1, preparation.armorRules.size());
        assertEquals(1, preparation.armorRules.get(LOTREntityGondorSoldier.class).slotRules.size());
        assertEquals(
            "LOTR NPC armor summary: prepared 1 choice(s) across 1 slot rule(s) for 1 exact NPC type(s); rejected 10 "
                + "invalid or duplicate choice(s).",
            preparation.describeStartup());
    }

    @Test
    public void supportsWeightedEmptyWeaponAndArmorChoices() {
        LOTREquipmentControl.WeightedItemRule weaponRule = LOTREquipmentControl.resolveWeaponRules(
            new String[] { "LOTR.GondorSoldier;minecraft:iron_sword;3", "LOTR.GondorSoldier;empty;1" },
            entityResolver(),
            itemResolver()).weaponRules.get(LOTREntityGondorSoldier.class);
        LOTREquipmentControl.WeightedItemRule helmetRule = LOTREquipmentControl.resolveArmorRules(
            new String[] { "LOTR.GondorSoldier;helmet;minecraft:iron_helmet;2", "LOTR.GondorSoldier;helmet;EMPTY;1",
                "LOTR.GondorSoldier;helmet;empty;4" },
            entityResolver(),
            itemResolver()).armorRules.get(LOTREntityGondorSoldier.class).slotRules
                .get(LOTREquipmentControl.ArmorSlot.HELMET);

        assertSame(IRON_SWORD, weaponRule.choose(new FixedRandom(2)).item);
        assertNull(weaponRule.choose(new FixedRandom(3)).item);
        assertSame(IRON_HELMET, helmetRule.choose(new FixedRandom(1)).item);
        assertNull(helmetRule.choose(new FixedRandom(2)).item);
        assertEquals(2, helmetRule.choices.size());
        assertEquals(3, helmetRule.totalWeight);
    }

    @Test
    public void groupsFactionRulesAndRejectsUnknownFactions() {
        LOTREquipmentControl.RulePreparation weapons = LOTREquipmentControl.resolveWeaponRules(
            new String[] { "LOTR.GondorSoldier;minecraft:iron_sword;3", "faction:GONDOR;minecraft:stone_sword;2",
                "Faction:rohan;minecraft:bow;1", "faction:missing;minecraft:bow;1" },
            entityResolver(),
            itemResolver());
        LOTREquipmentControl.ArmorRulePreparation armor = LOTREquipmentControl.resolveArmorRules(
            new String[] { "LOTR.GondorSoldier;helmet;minecraft:iron_helmet;3",
                "faction:GONDOR;helmet;minecraft:chainmail_helmet;2",
                "faction:GONDOR;chest;minecraft:iron_chestplate;1" },
            entityResolver(),
            itemResolver());

        assertEquals(1, weapons.weaponRules.size());
        assertEquals(2, weapons.factionWeaponRules.size());
        assertEquals(2, weapons.factionWeaponRules.get(LOTRFaction.GONDOR).totalWeight);
        assertEquals(
            "LOTR NPC equipment summary: prepared 3 weapon choice(s) for 1 exact NPC type(s) and 2 faction(s); "
                + "rejected 1 invalid or duplicate choice(s).",
            weapons.describeStartup());
        assertEquals(1, armor.armorRules.size());
        assertEquals(1, armor.factionArmorRules.size());
        assertEquals(2, armor.factionArmorRules.get(LOTRFaction.GONDOR).slotRules.size());
        assertEquals(
            "LOTR NPC armor summary: prepared 3 choice(s) across 3 slot rule(s) for 1 exact NPC type(s) and 1 "
                + "faction(s); rejected 0 invalid or duplicate choice(s).",
            armor.describeStartup());
    }

    @Test
    public void groupsCaseInsensitiveAllNPCFallbackRules() {
        LOTREquipmentControl.RulePreparation weapons = LOTREquipmentControl.resolveWeaponRules(
            new String[] { "LOTR.GondorSoldier;minecraft:iron_sword;1", "faction:GONDOR;minecraft:stone_sword;1",
                "all;minecraft:bow;3", "ALL;empty;1", "all;minecraft:bow;2" },
            entityResolver(),
            itemResolver());
        LOTREquipmentControl.ArmorRulePreparation armor = LOTREquipmentControl.resolveArmorRules(
            new String[] { "LOTR.GondorSoldier;helmet;minecraft:iron_helmet;1",
                "faction:GONDOR;chest;minecraft:iron_chestplate;1", "all;leggings;minecraft:iron_leggings;2",
                "ALL;boots;minecraft:iron_boots;1" },
            entityResolver(),
            itemResolver());

        assertEquals(2, weapons.allWeaponRule.choices.size());
        assertEquals(4, weapons.allWeaponRule.totalWeight);
        assertEquals(2, armor.allArmorRules.slotRules.size());
        assertEquals(
            "LOTR NPC equipment summary: prepared 4 weapon choice(s) for 1 exact NPC type(s) and 1 faction(s) and "
                + "an all-NPC fallback; rejected 1 invalid or duplicate choice(s).",
            weapons.describeStartup());
        assertEquals(
            "LOTR NPC armor summary: prepared 4 choice(s) across 4 slot rule(s) for 1 exact NPC type(s) and 1 "
                + "faction(s) and an all-NPC fallback; rejected 0 invalid or duplicate choice(s).",
            armor.describeStartup());
    }

    @Test
    public void protectsHiredAndCustomNamedNPCsUnlessEnabled() {
        boolean configuredHired = Config.customizeHiredLOTREquipment;
        boolean configuredNamed = Config.customizeNamedLOTREquipment;
        try {
            Config.customizeHiredLOTREquipment = false;
            Config.customizeNamedLOTREquipment = false;
            assertTrue(LOTREquipmentControl.shouldApplyConfiguredEquipment(false, false));
            assertFalse(LOTREquipmentControl.shouldApplyConfiguredEquipment(true, false));
            assertFalse(LOTREquipmentControl.shouldApplyConfiguredEquipment(false, true));
            assertFalse(LOTREquipmentControl.shouldApplyConfiguredEquipment(true, true));

            Config.customizeHiredLOTREquipment = true;
            assertTrue(LOTREquipmentControl.shouldApplyConfiguredEquipment(true, false));
            assertFalse(LOTREquipmentControl.shouldApplyConfiguredEquipment(true, true));

            Config.customizeNamedLOTREquipment = true;
            assertTrue(LOTREquipmentControl.shouldApplyConfiguredEquipment(true, true));
        } finally {
            Config.customizeHiredLOTREquipment = configuredHired;
            Config.customizeNamedLOTREquipment = configuredNamed;
        }
    }

    @Test
    public void summarizesReloadedWeaponAndArmorRules() {
        LOTREquipmentControl.RulePreparation weapons = LOTREquipmentControl.resolveWeaponRules(
            new String[] { "LOTR.GondorSoldier;minecraft:iron_sword;3", "LOTR.GondorSoldier;missing;1" },
            entityResolver(),
            itemResolver());
        LOTREquipmentControl.ArmorRulePreparation armor = LOTREquipmentControl.resolveArmorRules(
            new String[] { "LOTR.GondorSoldier;helmet;minecraft:iron_helmet;2",
                "LOTR.GondorArcher;chest;minecraft:iron_chestplate;1" },
            entityResolver(),
            itemResolver());

        assertEquals(
            "Reloaded LOTR NPC equipment rules: prepared 1 weapon choice(s) for 1 exact NPC type(s), and 2 armor "
                + "choice(s) across 2 slot rule(s) for 2 exact NPC type(s); rejected 1 invalid or duplicate "
                + "choice(s). Existing NPCs were not changed.",
            new LOTREquipmentControl.EquipmentRuleReloadResult(weapons, armor).describeReload());
    }

    @Test
    public void explainsConfiguredChoicesAndSafetySettings() {
        LOTREquipmentControl.RulePreparation weapons = LOTREquipmentControl.resolveWeaponRules(
            new String[] { "LOTR.GondorSoldier;minecraft:iron_sword;3", "LOTR.GondorSoldier;empty;1" },
            entityResolver(),
            itemResolver());
        LOTREquipmentControl.ArmorRulePreparation armor = LOTREquipmentControl.resolveArmorRules(
            new String[] { "LOTR.GondorSoldier;helmet;minecraft:iron_helmet;2" },
            entityResolver(),
            itemResolver());

        List<String> lines = LOTREquipmentReport.createEquipmentExplanation(
            "LOTR.GondorSoldier",
            LOTREntityGondorSoldier.class,
            weapons.weaponRules,
            armor.armorRules,
            false,
            false,
            true);

        assertEquals(
            "Equipment rules for LOTR.GondorSoldier (lotr.common.entity.npc.LOTREntityGondorSoldier):",
            lines.get(0));
        assertEquals("  Weapon choices (total weight 4):", lines.get(1));
        assertEquals("    minecraft:iron_sword - weight 3 (75.0%)", lines.get(2));
        assertEquals("    empty - weight 1 (25.0%)", lines.get(3));
        assertEquals("  Helmet choices (total weight 2):", lines.get(4));
        assertEquals("    minecraft:iron_helmet - weight 2 (100.0%)", lines.get(5));
        assertEquals("  Mode: configured choices fill only empty slots.", lines.get(6));
        assertEquals("  Hired NPCs: protected.", lines.get(7));
        assertEquals("  NPCs with custom name tags: included.", lines.get(8));
        assertEquals("  Existing NPCs are not changed; these rules apply to future natural spawns.", lines.get(9));
    }

    @Test
    public void explainsUnknownAndNonLOTRNPCNames() {
        assertEquals(
            "Entity 'missing' was not found. Names are exact and case-sensitive.",
            LOTREquipmentReport.createEquipmentExplanation(
                "missing",
                null,
                new HashMap<Class<? extends lotr.common.entity.npc.LOTREntityNPC>, LOTREquipmentControl.WeightedItemRule>(),
                new HashMap<Class<? extends lotr.common.entity.npc.LOTREntityNPC>, LOTREquipmentControl.ArmorRuleSet>(),
                true,
                false,
                false)
                .get(0));
        assertEquals(
            "Entity 'Cow' is not a LOTR NPC.",
            LOTREquipmentReport.createEquipmentExplanation(
                "Cow",
                EntityCow.class,
                new HashMap<Class<? extends lotr.common.entity.npc.LOTREntityNPC>, LOTREquipmentControl.WeightedItemRule>(),
                new HashMap<Class<? extends lotr.common.entity.npc.LOTREntityNPC>, LOTREquipmentControl.ArmorRuleSet>(),
                true,
                false,
                false)
                .get(0));
    }

    @Test
    public void explainsEffectiveExactAndFactionRules() {
        LOTREquipmentControl.RulePreparation combinedWeapons = LOTREquipmentControl.resolveWeaponRules(
            new String[] { "LOTR.GondorSoldier;minecraft:iron_sword;1", "faction:GONDOR;minecraft:stone_sword;1" },
            entityResolver(),
            itemResolver());
        LOTREquipmentControl.ArmorRulePreparation combinedArmor = LOTREquipmentControl.resolveArmorRules(
            new String[] { "LOTR.GondorSoldier;helmet;minecraft:iron_helmet;1",
                "faction:GONDOR;helmet;minecraft:chainmail_helmet;1",
                "faction:GONDOR;chest;minecraft:iron_chestplate;1" },
            entityResolver(),
            itemResolver());
        List<String> combinedLines = LOTREquipmentReport.createEquipmentExplanation(
            "LOTR.GondorSoldier",
            LOTREntityGondorSoldier.class,
            combinedWeapons.weaponRules,
            combinedArmor.armorRules,
            LOTRFaction.GONDOR,
            combinedWeapons.factionWeaponRules,
            combinedArmor.factionArmorRules,
            true,
            false,
            false);
        assertTrue(combinedLines.contains("  Weapon choices (total weight 1):"));
        assertTrue(combinedLines.contains("    minecraft:iron_sword - weight 1 (100.0%)"));
        assertFalse(combinedLines.contains("    minecraft:stone_sword - weight 1 (100.0%)"));
        assertTrue(combinedLines.contains("  Chest (from faction:GONDOR) choices (total weight 1):"));
        assertTrue(combinedLines.contains("  Helmet choices (total weight 1):"));
        assertFalse(combinedLines.contains("    minecraft:chainmail_helmet - weight 1 (100.0%)"));
    }

    @Test
    public void explainsFactionRulesAndExactRulePriority() {
        LOTREquipmentControl.RulePreparation weapons = LOTREquipmentControl.resolveWeaponRules(
            new String[] { "faction:GONDOR;minecraft:iron_sword;3", "faction:GONDOR;empty;1" },
            entityResolver(),
            itemResolver());
        LOTREquipmentControl.ArmorRulePreparation armor = LOTREquipmentControl.resolveArmorRules(
            new String[] { "faction:GONDOR;helmet;minecraft:iron_helmet;2" },
            entityResolver(),
            itemResolver());

        List<String> lines = LOTREquipmentReport.createFactionExplanation(
            "gondor",
            LOTRFaction.GONDOR,
            weapons.factionWeaponRules,
            armor.factionArmorRules,
            true,
            false,
            false);

        assertEquals("Equipment rules for faction:GONDOR:", lines.get(0));
        assertEquals("  Weapon choices (total weight 4):", lines.get(1));
        assertEquals("    minecraft:iron_sword - weight 3 (75.0%)", lines.get(2));
        assertEquals("    empty - weight 1 (25.0%)", lines.get(3));
        assertEquals("  Helmet choices (total weight 2):", lines.get(4));
        assertEquals("    minecraft:iron_helmet - weight 2 (100.0%)", lines.get(5));
        assertEquals("  Exact NPC rules take priority over faction rules for the same equipment slot.", lines.get(6));
        assertEquals(
            "LOTR faction 'missing' was not found. Use a faction code such as GONDOR or ROHAN.",
            LOTREquipmentReport
                .createFactionExplanation(
                    "missing",
                    null,
                    weapons.factionWeaponRules,
                    armor.factionArmorRules,
                    true,
                    false,
                    false)
                .get(0));
    }

    @Test
    public void explainsAllFallbackAndThreeLevelPriority() {
        LOTREquipmentControl.RulePreparation weapons = LOTREquipmentControl.resolveWeaponRules(
            new String[] { "LOTR.GondorSoldier;minecraft:iron_sword;1", "faction:GONDOR;minecraft:stone_sword;1",
                "all;minecraft:bow;1" },
            entityResolver(),
            itemResolver());
        LOTREquipmentControl.ArmorRulePreparation armor = LOTREquipmentControl.resolveArmorRules(
            new String[] { "LOTR.GondorSoldier;helmet;minecraft:iron_helmet;1",
                "faction:GONDOR;chest;minecraft:iron_chestplate;1", "all;helmet;minecraft:chainmail_helmet;1",
                "all;leggings;minecraft:iron_leggings;1" },
            entityResolver(),
            itemResolver());

        List<String> effectiveLines = LOTREquipmentReport.createEquipmentExplanation(
            "LOTR.GondorSoldier",
            LOTREntityGondorSoldier.class,
            weapons.weaponRules,
            armor.armorRules,
            LOTRFaction.GONDOR,
            weapons.factionWeaponRules,
            armor.factionArmorRules,
            weapons.allWeaponRule,
            armor.allArmorRules,
            true,
            false,
            false);
        List<String> allLines = LOTREquipmentReport
            .createAllExplanation(weapons.allWeaponRule, armor.allArmorRules, true, false, false);

        assertTrue(effectiveLines.contains("  Weapon choices (total weight 1):"));
        assertTrue(effectiveLines.contains("    minecraft:iron_sword - weight 1 (100.0%)"));
        assertFalse(effectiveLines.contains("    minecraft:stone_sword - weight 1 (100.0%)"));
        assertFalse(effectiveLines.contains("    minecraft:bow - weight 1 (100.0%)"));
        assertTrue(effectiveLines.contains("  Chest (from faction:GONDOR) choices (total weight 1):"));
        assertTrue(effectiveLines.contains("  Leggings (from all) choices (total weight 1):"));
        assertTrue(effectiveLines.contains("  Helmet choices (total weight 1):"));
        assertEquals("Equipment rules for all LOTR NPCs:", allLines.get(0));
        assertTrue(allLines.contains("  Weapon choices (total weight 1):"));
        assertTrue(allLines.contains("  Leggings choices (total weight 1):"));
        assertTrue(allLines.contains("  Helmet choices (total weight 1):"));
    }

    private static LOTREquipmentControl.EntityResolver entityResolver() {
        final Map<String, Class> entities = new HashMap<>();
        entities.put("LOTR.GondorSoldier", LOTREntityGondorSoldier.class);
        entities.put("LOTR.GondorArcher", LOTREntityGondorArcher.class);
        entities.put("Cow", EntityCow.class);
        return new LOTREquipmentControl.EntityResolver() {

            @Override
            public Class resolve(String entityName) {
                return entities.get(entityName);
            }
        };
    }

    private static LOTREquipmentControl.ItemResolver itemResolver() {
        final Map<String, Item> items = new HashMap<>();
        items.put("minecraft:iron_sword", IRON_SWORD);
        items.put("minecraft:stone_sword", STONE_SWORD);
        items.put("minecraft:bow", BOW);
        items.put("minecraft:iron_helmet", IRON_HELMET);
        items.put("minecraft:chainmail_helmet", CHAIN_HELMET);
        items.put("minecraft:iron_chestplate", IRON_CHEST);
        items.put("minecraft:iron_leggings", IRON_LEGGINGS);
        items.put("minecraft:iron_boots", IRON_BOOTS);
        return new LOTREquipmentControl.ItemResolver() {

            @Override
            public Item resolve(String itemName) {
                return items.get(itemName);
            }
        };
    }

    private static final class FixedRandom extends Random {

        private static final long serialVersionUID = 1L;
        private final int value;

        private FixedRandom(int value) {
            this.value = value;
        }

        @Override
        public int nextInt(int bound) {
            if (value < 0 || value >= bound) {
                throw new AssertionError("Fixed random value " + value + " is outside bound " + bound);
            }
            return value;
        }
    }
}
