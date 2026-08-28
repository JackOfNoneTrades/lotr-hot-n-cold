package org.fentanylsolutions.hotncold.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import net.minecraft.entity.passive.EntityCow;
import net.minecraft.item.Item;

import org.junit.Test;

import lotr.common.entity.npc.LOTREntityGondorArcher;
import lotr.common.entity.npc.LOTREntityGondorSoldier;

public class LOTREquipmentControlTest {

    private static final Item IRON_SWORD = new Item();
    private static final Item STONE_SWORD = new Item();
    private static final Item BOW = new Item();

    @Test
    public void groupsWeightedWeaponChoicesForExactLOTRNPCs() {
        LOTREquipmentControl.RulePreparation preparation = LOTREquipmentControl.resolveWeaponRules(
            new String[] { " LOTR.GondorSoldier ; minecraft:iron_sword ; 3 ",
                "LOTR.GondorSoldier;minecraft:stone_sword;1", "LOTR.GondorArcher;minecraft:bow;5" },
            entityResolver(),
            itemResolver());

        LOTREquipmentControl.WeaponRule soldierRule = preparation.weaponRules.get(LOTREntityGondorSoldier.class);
        LOTREquipmentControl.WeaponRule archerRule = preparation.weaponRules.get(LOTREntityGondorArcher.class);
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
        LOTREquipmentControl.WeaponRule rule = LOTREquipmentControl.resolveWeaponRules(
            new String[] { "LOTR.GondorSoldier;minecraft:iron_sword;3", "LOTR.GondorSoldier;minecraft:stone_sword;1" },
            entityResolver(),
            itemResolver()).weaponRules.get(LOTREntityGondorSoldier.class);

        assertSame(IRON_SWORD, rule.choose(new FixedRandom(0)).item);
        assertSame(IRON_SWORD, rule.choose(new FixedRandom(2)).item);
        assertSame(STONE_SWORD, rule.choose(new FixedRandom(3)).item);
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
