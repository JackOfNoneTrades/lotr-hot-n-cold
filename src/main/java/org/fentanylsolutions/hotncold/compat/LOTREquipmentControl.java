package org.fentanylsolutions.hotncold.compat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

import org.fentanylsolutions.hotncold.Config;
import org.fentanylsolutions.hotncold.HotNCold;

import lotr.common.entity.npc.LOTREntityNPC;

public final class LOTREquipmentControl {

    private static Map<Class<? extends LOTREntityNPC>, WeightedItemRule> cachedWeaponRules = Collections.emptyMap();
    private static Map<Class<? extends LOTREntityNPC>, ArmorRuleSet> cachedArmorRules = Collections.emptyMap();

    private LOTREquipmentControl() {}

    public static RulePreparation prepareConfiguredWeaponRules() {
        RulePreparation preparation = resolveWeaponRules(Config.lotrNPCWeaponRules, new EntityResolver() {

            @Override
            public Class resolve(String entityName) {
                Object value = EntityList.stringToClassMapping.get(entityName);
                return value instanceof Class ? (Class) value : null;
            }
        }, new ItemResolver() {

            @Override
            public Item resolve(String itemName) {
                Object value = Item.itemRegistry.getObject(itemName);
                return value instanceof Item ? (Item) value : null;
            }
        });
        cachedWeaponRules = preparation.weaponRules;
        return preparation;
    }

    public static ArmorRulePreparation prepareConfiguredArmorRules() {
        ArmorRulePreparation preparation = resolveArmorRules(Config.lotrNPCArmorRules, new EntityResolver() {

            @Override
            public Class resolve(String entityName) {
                Object value = EntityList.stringToClassMapping.get(entityName);
                return value instanceof Class ? (Class) value : null;
            }
        }, new ItemResolver() {

            @Override
            public Item resolve(String itemName) {
                Object value = Item.itemRegistry.getObject(itemName);
                return value instanceof Item ? (Item) value : null;
            }
        });
        cachedArmorRules = preparation.armorRules;
        return preparation;
    }

    static RulePreparation resolveWeaponRules(String[] configuredRules, EntityResolver entityResolver,
        ItemResolver itemResolver) {
        Map<Class<? extends LOTREntityNPC>, MutableItemRule> rules = new LinkedHashMap<>();
        int acceptedChoices = 0;
        int rejectedChoices = 0;

        for (String configuredRule : configuredRules) {
            String rule = configuredRule == null ? "" : configuredRule.trim();
            if (rule.isEmpty()) {
                continue;
            }

            String[] fields = rule.split(";", -1);
            if (fields.length != 3) {
                HotNCold.LOG.warn("Invalid LOTR NPC weapon rule '{}'; expected entityName;itemName;weight", rule);
                rejectedChoices++;
                continue;
            }
            for (int fieldIndex = 0; fieldIndex < fields.length; fieldIndex++) {
                fields[fieldIndex] = fields[fieldIndex].trim();
            }
            if (fields[0].isEmpty() || fields[1].isEmpty() || fields[2].isEmpty()) {
                HotNCold.LOG
                    .warn("Invalid LOTR NPC weapon rule '{}'; entity name, item name, and weight are required", rule);
                rejectedChoices++;
                continue;
            }

            Class entityClass = entityResolver.resolve(fields[0]);
            if (entityClass == null) {
                HotNCold.LOG.warn(
                    "LOTR NPC weapon entity '{}' in rule '{}' was not found; names are exact and case-sensitive",
                    fields[0],
                    rule);
                rejectedChoices++;
                continue;
            }
            if (!LOTREntityNPC.class.isAssignableFrom(entityClass)) {
                HotNCold.LOG.warn("Entity '{}' in weapon rule '{}' is not a LOTR NPC", fields[0], rule);
                rejectedChoices++;
                continue;
            }

            Item item = itemResolver.resolve(fields[1]);
            if (item == null) {
                HotNCold.LOG.warn(
                    "Item '{}' in LOTR NPC weapon rule '{}' was not found; names are exact and case-sensitive",
                    fields[1],
                    rule);
                rejectedChoices++;
                continue;
            }

            Integer weight = parsePositiveWeight(fields[2], rule);
            if (weight == null) {
                rejectedChoices++;
                continue;
            }

            @SuppressWarnings("unchecked")
            Class<? extends LOTREntityNPC> npcClass = (Class<? extends LOTREntityNPC>) entityClass;
            MutableItemRule mutableRule = rules.get(npcClass);
            if (mutableRule == null) {
                mutableRule = new MutableItemRule(fields[0]);
                rules.put(npcClass, mutableRule);
            }
            if (!mutableRule.itemNames.add(fields[1])) {
                HotNCold.LOG.warn(
                    "Duplicate item '{}' for LOTR NPC '{}' in weapon rule '{}'; ignoring duplicate",
                    fields[1],
                    fields[0],
                    rule);
                rejectedChoices++;
                continue;
            }
            if ((long) mutableRule.totalWeight + weight > Integer.MAX_VALUE) {
                HotNCold.LOG.warn(
                    "LOTR NPC weapon choices for '{}' exceed the maximum combined weight; rejecting rule '{}'",
                    fields[0],
                    rule);
                rejectedChoices++;
                continue;
            }

            mutableRule.choices.add(new WeightedItem(fields[1], item, weight));
            mutableRule.totalWeight += weight;
            acceptedChoices++;
        }

        Map<Class<? extends LOTREntityNPC>, WeightedItemRule> resolved = new LinkedHashMap<>();
        for (Map.Entry<Class<? extends LOTREntityNPC>, MutableItemRule> entry : rules.entrySet()) {
            MutableItemRule value = entry.getValue();
            if (!value.choices.isEmpty()) {
                resolved.put(entry.getKey(), new WeightedItemRule(value.entityName, value.choices, value.totalWeight));
            }
        }
        return new RulePreparation(resolved, acceptedChoices, rejectedChoices);
    }

    static ArmorRulePreparation resolveArmorRules(String[] configuredRules, EntityResolver entityResolver,
        ItemResolver itemResolver) {
        Map<Class<? extends LOTREntityNPC>, EnumMap<ArmorSlot, MutableItemRule>> rules = new LinkedHashMap<>();
        int acceptedChoices = 0;
        int rejectedChoices = 0;

        for (String configuredRule : configuredRules) {
            String rule = configuredRule == null ? "" : configuredRule.trim();
            if (rule.isEmpty()) {
                continue;
            }

            String[] fields = rule.split(";", -1);
            if (fields.length != 4) {
                HotNCold.LOG.warn("Invalid LOTR NPC armor rule '{}'; expected entityName;slot;itemName;weight", rule);
                rejectedChoices++;
                continue;
            }
            for (int fieldIndex = 0; fieldIndex < fields.length; fieldIndex++) {
                fields[fieldIndex] = fields[fieldIndex].trim();
            }
            if (fields[0].isEmpty() || fields[1].isEmpty() || fields[2].isEmpty() || fields[3].isEmpty()) {
                HotNCold.LOG.warn(
                    "Invalid LOTR NPC armor rule '{}'; entity name, slot, item name, and weight are required",
                    rule);
                rejectedChoices++;
                continue;
            }

            Class entityClass = entityResolver.resolve(fields[0]);
            if (entityClass == null) {
                HotNCold.LOG.warn(
                    "LOTR NPC armor entity '{}' in rule '{}' was not found; names are exact and case-sensitive",
                    fields[0],
                    rule);
                rejectedChoices++;
                continue;
            }
            if (!LOTREntityNPC.class.isAssignableFrom(entityClass)) {
                HotNCold.LOG.warn("Entity '{}' in armor rule '{}' is not a LOTR NPC", fields[0], rule);
                rejectedChoices++;
                continue;
            }

            ArmorSlot slot = ArmorSlot.resolve(fields[1]);
            if (slot == null) {
                HotNCold.LOG.warn(
                    "Armor slot '{}' in LOTR NPC armor rule '{}' was not found; available slots are {}",
                    fields[1],
                    rule,
                    ArmorSlot.availableNames());
                rejectedChoices++;
                continue;
            }

            Item item = itemResolver.resolve(fields[2]);
            if (item == null) {
                HotNCold.LOG.warn(
                    "Item '{}' in LOTR NPC armor rule '{}' was not found; names are exact and case-sensitive",
                    fields[2],
                    rule);
                rejectedChoices++;
                continue;
            }
            if (!(item instanceof ItemArmor) || ((ItemArmor) item).armorType != slot.armorType) {
                HotNCold.LOG.warn(
                    "Item '{}' in LOTR NPC armor rule '{}' is not compatible with the {} slot",
                    fields[2],
                    rule,
                    slot.configName);
                rejectedChoices++;
                continue;
            }

            Integer weight = parsePositiveWeight(fields[3], rule);
            if (weight == null) {
                rejectedChoices++;
                continue;
            }

            @SuppressWarnings("unchecked")
            Class<? extends LOTREntityNPC> npcClass = (Class<? extends LOTREntityNPC>) entityClass;
            EnumMap<ArmorSlot, MutableItemRule> npcRules = rules.get(npcClass);
            if (npcRules == null) {
                npcRules = new EnumMap<>(ArmorSlot.class);
                rules.put(npcClass, npcRules);
            }
            MutableItemRule mutableRule = npcRules.get(slot);
            if (mutableRule == null) {
                mutableRule = new MutableItemRule(fields[0]);
                npcRules.put(slot, mutableRule);
            }
            if (!mutableRule.itemNames.add(fields[2])) {
                HotNCold.LOG.warn(
                    "Duplicate {} item '{}' for LOTR NPC '{}' in armor rule '{}'; ignoring duplicate",
                    slot.configName,
                    fields[2],
                    fields[0],
                    rule);
                rejectedChoices++;
                continue;
            }
            if ((long) mutableRule.totalWeight + weight > Integer.MAX_VALUE) {
                HotNCold.LOG.warn(
                    "LOTR NPC {} choices for '{}' exceed the maximum combined weight; rejecting rule '{}'",
                    slot.configName,
                    fields[0],
                    rule);
                rejectedChoices++;
                continue;
            }

            mutableRule.choices.add(new WeightedItem(fields[2], item, weight));
            mutableRule.totalWeight += weight;
            acceptedChoices++;
        }

        Map<Class<? extends LOTREntityNPC>, ArmorRuleSet> resolved = new LinkedHashMap<>();
        int slotRuleCount = 0;
        for (Map.Entry<Class<? extends LOTREntityNPC>, EnumMap<ArmorSlot, MutableItemRule>> npcEntry : rules
            .entrySet()) {
            EnumMap<ArmorSlot, WeightedItemRule> slotRules = new EnumMap<>(ArmorSlot.class);
            for (Map.Entry<ArmorSlot, MutableItemRule> slotEntry : npcEntry.getValue()
                .entrySet()) {
                MutableItemRule value = slotEntry.getValue();
                if (!value.choices.isEmpty()) {
                    slotRules.put(
                        slotEntry.getKey(),
                        new WeightedItemRule(value.entityName, value.choices, value.totalWeight));
                }
            }
            if (!slotRules.isEmpty()) {
                resolved.put(npcEntry.getKey(), new ArmorRuleSet(slotRules));
                slotRuleCount += slotRules.size();
            }
        }
        return new ArmorRulePreparation(resolved, acceptedChoices, rejectedChoices, slotRuleCount);
    }

    public static boolean applyConfiguredWeapon(LOTREntityNPC npc) {
        if (npc == null || npc.worldObj == null || npc.worldObj.isRemote) {
            return false;
        }

        @SuppressWarnings("unchecked")
        WeightedItemRule rule = cachedWeaponRules.get((Class<? extends LOTREntityNPC>) npc.getClass());
        if (rule == null) {
            return false;
        }

        ItemStack weapon = new ItemStack(rule.choose(npc.getRNG()).item);
        npc.npcItemsInv.setMeleeWeapon(weapon.copy());
        npc.npcItemsInv.setMeleeWeaponMounted(weapon.copy());
        npc.npcItemsInv.setIdleItem(weapon.copy());
        npc.npcItemsInv.setIdleItemMounted(weapon.copy());
        npc.setCurrentItemOrArmor(0, weapon.copy());
        return true;
    }

    public static int applyConfiguredArmor(LOTREntityNPC npc) {
        if (npc == null || npc.worldObj == null || npc.worldObj.isRemote) {
            return 0;
        }

        @SuppressWarnings("unchecked")
        ArmorRuleSet ruleSet = cachedArmorRules.get((Class<? extends LOTREntityNPC>) npc.getClass());
        if (ruleSet == null) {
            return 0;
        }

        int appliedSlots = 0;
        for (Map.Entry<ArmorSlot, WeightedItemRule> entry : ruleSet.slotRules.entrySet()) {
            ItemStack armor = new ItemStack(
                entry.getValue()
                    .choose(npc.getRNG()).item);
            npc.setCurrentItemOrArmor(entry.getKey().equipmentSlot, armor);
            appliedSlots++;
        }
        return appliedSlots;
    }

    public static IEntityLivingData finishNaturalSpawn(EntityLiving entity, IEntityLivingData livingData) {
        IEntityLivingData result = entity.onSpawnWithEgg(livingData);
        if (entity instanceof LOTREntityNPC) {
            LOTREntityNPC npc = (LOTREntityNPC) entity;
            applyConfiguredWeapon(npc);
            applyConfiguredArmor(npc);
        }
        return result;
    }

    private static Integer parsePositiveWeight(String value, String rule) {
        try {
            int weight = Integer.parseInt(value);
            if (weight > 0) {
                return weight;
            }
        } catch (NumberFormatException ignored) {}

        HotNCold.LOG.warn(
            "Invalid weight '{}' in LOTR NPC equipment rule '{}'; the value must be a positive whole number",
            value,
            rule);
        return null;
    }

    interface EntityResolver {

        Class resolve(String entityName);
    }

    interface ItemResolver {

        Item resolve(String itemName);
    }

    public static final class RulePreparation {

        final Map<Class<? extends LOTREntityNPC>, WeightedItemRule> weaponRules;
        private final int acceptedChoices;
        private final int rejectedChoices;

        private RulePreparation(Map<Class<? extends LOTREntityNPC>, WeightedItemRule> weaponRules, int acceptedChoices,
            int rejectedChoices) {
            this.weaponRules = Collections.unmodifiableMap(new LinkedHashMap<>(weaponRules));
            this.acceptedChoices = acceptedChoices;
            this.rejectedChoices = rejectedChoices;
        }

        public String describeStartup() {
            return "LOTR NPC equipment summary: prepared " + acceptedChoices
                + " weapon choice(s) for "
                + weaponRules.size()
                + " exact NPC type(s); rejected "
                + rejectedChoices
                + " invalid or duplicate choice(s).";
        }
    }

    public static final class ArmorRulePreparation {

        final Map<Class<? extends LOTREntityNPC>, ArmorRuleSet> armorRules;
        private final int acceptedChoices;
        private final int rejectedChoices;
        private final int slotRuleCount;

        private ArmorRulePreparation(Map<Class<? extends LOTREntityNPC>, ArmorRuleSet> armorRules, int acceptedChoices,
            int rejectedChoices, int slotRuleCount) {
            this.armorRules = Collections.unmodifiableMap(new LinkedHashMap<>(armorRules));
            this.acceptedChoices = acceptedChoices;
            this.rejectedChoices = rejectedChoices;
            this.slotRuleCount = slotRuleCount;
        }

        public String describeStartup() {
            return "LOTR NPC armor summary: prepared " + acceptedChoices
                + " choice(s) across "
                + slotRuleCount
                + " slot rule(s) for "
                + armorRules.size()
                + " exact NPC type(s); rejected "
                + rejectedChoices
                + " invalid or duplicate choice(s).";
        }
    }

    static final class ArmorRuleSet {

        final Map<ArmorSlot, WeightedItemRule> slotRules;

        private ArmorRuleSet(EnumMap<ArmorSlot, WeightedItemRule> slotRules) {
            this.slotRules = Collections.unmodifiableMap(new EnumMap<>(slotRules));
        }
    }

    enum ArmorSlot {

        BOOTS("boots", 1, 3),
        LEGGINGS("leggings", 2, 2),
        CHEST("chest", 3, 1),
        HELMET("helmet", 4, 0);

        final String configName;
        final int equipmentSlot;
        final int armorType;

        ArmorSlot(String configName, int equipmentSlot, int armorType) {
            this.configName = configName;
            this.equipmentSlot = equipmentSlot;
            this.armorType = armorType;
        }

        static ArmorSlot resolve(String configuredName) {
            for (ArmorSlot slot : values()) {
                if (slot.configName.equalsIgnoreCase(configuredName)) {
                    return slot;
                }
            }
            return null;
        }

        static String availableNames() {
            StringBuilder names = new StringBuilder();
            for (ArmorSlot slot : values()) {
                if (names.length() > 0) {
                    names.append(", ");
                }
                names.append(slot.configName);
            }
            return names.toString();
        }
    }

    static final class WeightedItemRule {

        final String entityName;
        final List<WeightedItem> choices;
        final int totalWeight;

        private WeightedItemRule(String entityName, List<WeightedItem> choices, int totalWeight) {
            this.entityName = entityName;
            this.choices = Collections.unmodifiableList(new ArrayList<>(choices));
            this.totalWeight = totalWeight;
        }

        WeightedItem choose(Random random) {
            int selection = random.nextInt(totalWeight);
            for (WeightedItem choice : choices) {
                selection -= choice.weight;
                if (selection < 0) {
                    return choice;
                }
            }
            throw new IllegalStateException("Weighted LOTR NPC item selection fell outside its configured range");
        }
    }

    static final class WeightedItem {

        final String itemName;
        final Item item;
        final int weight;

        private WeightedItem(String itemName, Item item, int weight) {
            this.itemName = itemName;
            this.item = item;
            this.weight = weight;
        }
    }

    private static final class MutableItemRule {

        private final String entityName;
        private final List<WeightedItem> choices = new ArrayList<>();
        private final Set<String> itemNames = new LinkedHashSet<>();
        private int totalWeight;

        private MutableItemRule(String entityName) {
            this.entityName = entityName;
        }
    }
}
