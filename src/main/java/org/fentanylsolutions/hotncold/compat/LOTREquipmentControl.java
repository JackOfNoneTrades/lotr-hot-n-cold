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
import lotr.common.fac.LOTRFaction;

public final class LOTREquipmentControl {

    private static Map<Class<? extends LOTREntityNPC>, WeightedItemRule> cachedWeaponRules = Collections.emptyMap();
    private static Map<LOTRFaction, WeightedItemRule> cachedFactionWeaponRules = Collections.emptyMap();
    private static WeightedItemRule cachedAllWeaponRule;
    private static Map<Class<? extends LOTREntityNPC>, WeightedItemRule> cachedRangedWeaponRules = Collections
        .emptyMap();
    private static Map<LOTRFaction, WeightedItemRule> cachedFactionRangedWeaponRules = Collections.emptyMap();
    private static WeightedItemRule cachedAllRangedWeaponRule;
    private static Map<Class<? extends LOTREntityNPC>, ArmorRuleSet> cachedArmorRules = Collections.emptyMap();
    private static Map<LOTRFaction, ArmorRuleSet> cachedFactionArmorRules = Collections.emptyMap();
    private static ArmorRuleSet cachedAllArmorRules;

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
        cachedFactionWeaponRules = preparation.factionWeaponRules;
        cachedAllWeaponRule = preparation.allWeaponRule;
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
        cachedFactionArmorRules = preparation.factionArmorRules;
        cachedAllArmorRules = preparation.allArmorRules;
        return preparation;
    }

    public static RulePreparation prepareConfiguredRangedWeaponRules() {
        RulePreparation preparation = resolveRangedWeaponRules(Config.lotrNPCRangedWeaponRules, new EntityResolver() {

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
        cachedRangedWeaponRules = preparation.weaponRules;
        cachedFactionRangedWeaponRules = preparation.factionWeaponRules;
        cachedAllRangedWeaponRule = preparation.allWeaponRule;
        return preparation;
    }

    public static EquipmentRuleReloadResult reloadConfiguredRules() {
        if (!Config.reloadNPCEquipmentConfiguration()) {
            return null;
        }
        return new EquipmentRuleReloadResult(
            prepareConfiguredWeaponRules(),
            prepareConfiguredRangedWeaponRules(),
            prepareConfiguredArmorRules());
    }

    static Map<Class<? extends LOTREntityNPC>, WeightedItemRule> getPreparedWeaponRules() {
        return cachedWeaponRules;
    }

    static Map<Class<? extends LOTREntityNPC>, ArmorRuleSet> getPreparedArmorRules() {
        return cachedArmorRules;
    }

    static Map<LOTRFaction, WeightedItemRule> getPreparedFactionWeaponRules() {
        return cachedFactionWeaponRules;
    }

    static Map<LOTRFaction, ArmorRuleSet> getPreparedFactionArmorRules() {
        return cachedFactionArmorRules;
    }

    static WeightedItemRule getPreparedAllWeaponRule() {
        return cachedAllWeaponRule;
    }

    static ArmorRuleSet getPreparedAllArmorRules() {
        return cachedAllArmorRules;
    }

    static Map<Class<? extends LOTREntityNPC>, WeightedItemRule> getPreparedRangedWeaponRules() {
        return cachedRangedWeaponRules;
    }

    static Map<LOTRFaction, WeightedItemRule> getPreparedFactionRangedWeaponRules() {
        return cachedFactionRangedWeaponRules;
    }

    static WeightedItemRule getPreparedAllRangedWeaponRule() {
        return cachedAllRangedWeaponRule;
    }

    static RulePreparation resolveWeaponRules(String[] configuredRules, EntityResolver entityResolver,
        ItemResolver itemResolver) {
        return resolveItemRules(configuredRules, entityResolver, itemResolver, "weapon");
    }

    static RulePreparation resolveRangedWeaponRules(String[] configuredRules, EntityResolver entityResolver,
        ItemResolver itemResolver) {
        return resolveItemRules(configuredRules, entityResolver, itemResolver, "ranged weapon");
    }

    private static RulePreparation resolveItemRules(String[] configuredRules, EntityResolver entityResolver,
        ItemResolver itemResolver, String equipmentType) {
        Map<EquipmentTarget, MutableItemRule> rules = new LinkedHashMap<>();
        int acceptedChoices = 0;
        int rejectedChoices = 0;

        for (String configuredRule : configuredRules) {
            String rule = configuredRule == null ? "" : configuredRule.trim();
            if (rule.isEmpty()) {
                continue;
            }

            String[] fields = rule.split(";", -1);
            if (fields.length != 3) {
                HotNCold.LOG
                    .warn("Invalid LOTR NPC {} rule '{}'; expected entityName;itemName;weight", equipmentType, rule);
                rejectedChoices++;
                continue;
            }
            for (int fieldIndex = 0; fieldIndex < fields.length; fieldIndex++) {
                fields[fieldIndex] = fields[fieldIndex].trim();
            }
            if (fields[0].isEmpty() || fields[1].isEmpty() || fields[2].isEmpty()) {
                HotNCold.LOG.warn(
                    "Invalid LOTR NPC {} rule '{}'; entity name, item name, and weight are required",
                    equipmentType,
                    rule);
                rejectedChoices++;
                continue;
            }

            EquipmentTarget target = resolveTarget(fields[0], rule, equipmentType, entityResolver);
            if (target == null) {
                rejectedChoices++;
                continue;
            }

            boolean emptyChoice = isEmptyChoice(fields[1]);
            Item item = emptyChoice ? null : itemResolver.resolve(fields[1]);
            if (!emptyChoice && item == null) {
                HotNCold.LOG.warn(
                    "Item '{}' in LOTR NPC {} rule '{}' was not found; names are exact and case-sensitive",
                    fields[1],
                    equipmentType,
                    rule);
                rejectedChoices++;
                continue;
            }

            Integer weight = parsePositiveWeight(fields[2], rule);
            if (weight == null) {
                rejectedChoices++;
                continue;
            }

            MutableItemRule mutableRule = rules.get(target);
            if (mutableRule == null) {
                mutableRule = new MutableItemRule(fields[0]);
                rules.put(target, mutableRule);
            }
            String choiceKey = emptyChoice ? "empty" : fields[1];
            if (!mutableRule.itemNames.add(choiceKey)) {
                HotNCold.LOG.warn(
                    "Duplicate item '{}' for LOTR NPC '{}' in {} rule '{}'; ignoring duplicate",
                    fields[1],
                    fields[0],
                    equipmentType,
                    rule);
                rejectedChoices++;
                continue;
            }
            if ((long) mutableRule.totalWeight + weight > Integer.MAX_VALUE) {
                HotNCold.LOG.warn(
                    "LOTR NPC {} choices for '{}' exceed the maximum combined weight; rejecting rule '{}'",
                    equipmentType,
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
        Map<LOTRFaction, WeightedItemRule> resolvedFactions = new LinkedHashMap<>();
        WeightedItemRule resolvedAll = null;
        for (Map.Entry<EquipmentTarget, MutableItemRule> entry : rules.entrySet()) {
            MutableItemRule value = entry.getValue();
            if (!value.choices.isEmpty()) {
                WeightedItemRule resolvedRule = new WeightedItemRule(
                    value.entityName,
                    value.choices,
                    value.totalWeight);
                if (entry.getKey().npcClass != null) {
                    resolved.put(entry.getKey().npcClass, resolvedRule);
                } else if (entry.getKey().faction != null) {
                    resolvedFactions.put(entry.getKey().faction, resolvedRule);
                } else {
                    resolvedAll = resolvedRule;
                }
            }
        }
        return new RulePreparation(
            resolved,
            resolvedFactions,
            resolvedAll,
            acceptedChoices,
            rejectedChoices,
            equipmentType);
    }

    static ArmorRulePreparation resolveArmorRules(String[] configuredRules, EntityResolver entityResolver,
        ItemResolver itemResolver) {
        Map<EquipmentTarget, EnumMap<ArmorSlot, MutableItemRule>> rules = new LinkedHashMap<>();
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

            EquipmentTarget target = resolveTarget(fields[0], rule, "armor", entityResolver);
            if (target == null) {
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

            boolean emptyChoice = isEmptyChoice(fields[2]);
            Item item = emptyChoice ? null : itemResolver.resolve(fields[2]);
            if (!emptyChoice && item == null) {
                HotNCold.LOG.warn(
                    "Item '{}' in LOTR NPC armor rule '{}' was not found; names are exact and case-sensitive",
                    fields[2],
                    rule);
                rejectedChoices++;
                continue;
            }
            if (!emptyChoice && (!(item instanceof ItemArmor) || ((ItemArmor) item).armorType != slot.armorType)) {
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

            EnumMap<ArmorSlot, MutableItemRule> npcRules = rules.get(target);
            if (npcRules == null) {
                npcRules = new EnumMap<>(ArmorSlot.class);
                rules.put(target, npcRules);
            }
            MutableItemRule mutableRule = npcRules.get(slot);
            if (mutableRule == null) {
                mutableRule = new MutableItemRule(fields[0]);
                npcRules.put(slot, mutableRule);
            }
            String choiceKey = emptyChoice ? "empty" : fields[2];
            if (!mutableRule.itemNames.add(choiceKey)) {
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
        Map<LOTRFaction, ArmorRuleSet> resolvedFactions = new LinkedHashMap<>();
        ArmorRuleSet resolvedAll = null;
        int slotRuleCount = 0;
        for (Map.Entry<EquipmentTarget, EnumMap<ArmorSlot, MutableItemRule>> npcEntry : rules.entrySet()) {
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
                if (npcEntry.getKey().npcClass != null) {
                    resolved.put(npcEntry.getKey().npcClass, new ArmorRuleSet(slotRules));
                } else if (npcEntry.getKey().faction != null) {
                    resolvedFactions.put(npcEntry.getKey().faction, new ArmorRuleSet(slotRules));
                } else {
                    resolvedAll = new ArmorRuleSet(slotRules);
                }
                slotRuleCount += slotRules.size();
            }
        }
        return new ArmorRulePreparation(
            resolved,
            resolvedFactions,
            resolvedAll,
            acceptedChoices,
            rejectedChoices,
            slotRuleCount);
    }

    public static boolean applyConfiguredWeapon(LOTREntityNPC npc) {
        if (npc == null || npc.worldObj == null || npc.worldObj.isRemote || !shouldApplyConfiguredEquipment(npc)) {
            return false;
        }

        @SuppressWarnings("unchecked")
        WeightedItemRule rule = cachedWeaponRules.get((Class<? extends LOTREntityNPC>) npc.getClass());
        if (rule == null) {
            rule = cachedFactionWeaponRules.get(npc.getFaction());
        }
        if (rule == null) {
            rule = cachedAllWeaponRule;
        }
        if (rule == null) {
            return false;
        }
        if (!Config.replaceExistingLOTREquipment
            && (npc.npcItemsInv.getMeleeWeapon() != null || npc.getEquipmentInSlot(0) != null)) {
            return false;
        }

        Item chosenItem = rule.choose(npc.getRNG()).item;
        ItemStack weapon = chosenItem == null ? null : new ItemStack(chosenItem);
        npc.npcItemsInv.setMeleeWeapon(copyOrNull(weapon));
        npc.npcItemsInv.setMeleeWeaponMounted(copyOrNull(weapon));
        npc.npcItemsInv.setIdleItem(copyOrNull(weapon));
        npc.npcItemsInv.setIdleItemMounted(copyOrNull(weapon));
        npc.setCurrentItemOrArmor(0, copyOrNull(weapon));
        return true;
    }

    public static boolean applyConfiguredRangedWeapon(LOTREntityNPC npc) {
        if (npc == null || npc.worldObj == null || npc.worldObj.isRemote || !shouldApplyConfiguredEquipment(npc)) {
            return false;
        }

        @SuppressWarnings("unchecked")
        WeightedItemRule rule = cachedRangedWeaponRules.get((Class<? extends LOTREntityNPC>) npc.getClass());
        if (rule == null) {
            rule = cachedFactionRangedWeaponRules.get(npc.getFaction());
        }
        if (rule == null) {
            rule = cachedAllRangedWeaponRule;
        }
        if (rule == null || !Config.replaceExistingLOTREquipment && npc.npcItemsInv.getRangedWeapon() != null) {
            return false;
        }

        ItemStack previousRangedWeapon = npc.npcItemsInv.getRangedWeapon();
        boolean rangedWeaponWasIdle = previousRangedWeapon != null
            && ItemStack.areItemStacksEqual(previousRangedWeapon, npc.npcItemsInv.getIdleItem());
        boolean rangedWeaponWasHeld = previousRangedWeapon != null
            && ItemStack.areItemStacksEqual(previousRangedWeapon, npc.getEquipmentInSlot(0));
        Item chosenItem = rule.choose(npc.getRNG()).item;
        ItemStack rangedWeapon = chosenItem == null ? null : new ItemStack(chosenItem);
        npc.npcItemsInv.setRangedWeapon(copyOrNull(rangedWeapon));
        if (rangedWeaponWasIdle) {
            npc.npcItemsInv.setIdleItem(copyOrNull(rangedWeapon));
        }
        if (rangedWeaponWasHeld) {
            npc.setCurrentItemOrArmor(0, copyOrNull(rangedWeapon));
        }
        return true;
    }

    public static int applyConfiguredArmor(LOTREntityNPC npc) {
        if (npc == null || npc.worldObj == null || npc.worldObj.isRemote || !shouldApplyConfiguredEquipment(npc)) {
            return 0;
        }

        @SuppressWarnings("unchecked")
        ArmorRuleSet exactRuleSet = cachedArmorRules.get((Class<? extends LOTREntityNPC>) npc.getClass());
        ArmorRuleSet factionRuleSet = cachedFactionArmorRules.get(npc.getFaction());
        if (exactRuleSet == null && factionRuleSet == null && cachedAllArmorRules == null) {
            return 0;
        }

        int appliedSlots = 0;
        for (ArmorSlot slot : ArmorSlot.values()) {
            WeightedItemRule slotRule = exactRuleSet == null ? null : exactRuleSet.slotRules.get(slot);
            if (slotRule == null && factionRuleSet != null) {
                slotRule = factionRuleSet.slotRules.get(slot);
            }
            if (slotRule == null && cachedAllArmorRules != null) {
                slotRule = cachedAllArmorRules.slotRules.get(slot);
            }
            if (slotRule == null) {
                continue;
            }
            if (!Config.replaceExistingLOTREquipment && npc.getEquipmentInSlot(slot.equipmentSlot) != null) {
                continue;
            }
            Item chosenItem = slotRule.choose(npc.getRNG()).item;
            ItemStack armor = chosenItem == null ? null : new ItemStack(chosenItem);
            npc.setCurrentItemOrArmor(slot.equipmentSlot, armor);
            appliedSlots++;
        }
        return appliedSlots;
    }

    public static IEntityLivingData finishNaturalSpawn(EntityLiving entity, IEntityLivingData livingData) {
        IEntityLivingData result = entity.onSpawnWithEgg(livingData);
        if (entity instanceof LOTREntityNPC) {
            LOTREntityNPC npc = (LOTREntityNPC) entity;
            applyConfiguredWeapon(npc);
            applyConfiguredRangedWeapon(npc);
            applyConfiguredArmor(npc);
        }
        return result;
    }

    private static boolean shouldApplyConfiguredEquipment(LOTREntityNPC npc) {
        boolean hired = npc.hiredNPCInfo != null && npc.hiredNPCInfo.isActive;
        return shouldApplyConfiguredEquipment(hired, npc.hasCustomNameTag());
    }

    static boolean shouldApplyConfiguredEquipment(boolean hired, boolean customNamed) {
        return (Config.customizeHiredLOTREquipment || !hired) && (Config.customizeNamedLOTREquipment || !customNamed);
    }

    private static EquipmentTarget resolveTarget(String configuredTarget, String rule, String equipmentType,
        EntityResolver entityResolver) {
        if ("all".equalsIgnoreCase(configuredTarget)) {
            return EquipmentTarget.forAll();
        }
        if (configuredTarget.regionMatches(true, 0, "faction:", 0, "faction:".length())) {
            String factionName = configuredTarget.substring("faction:".length())
                .trim();
            LOTRFaction faction = resolveFaction(factionName);
            if (faction == null) {
                HotNCold.LOG.warn(
                    "LOTR NPC {} faction '{}' in rule '{}' was not found; use a faction code such as GONDOR or ROHAN",
                    equipmentType,
                    factionName,
                    rule);
                return null;
            }
            return EquipmentTarget.forFaction(faction);
        }

        Class entityClass = entityResolver.resolve(configuredTarget);
        if (entityClass == null) {
            HotNCold.LOG.warn(
                "LOTR NPC {} entity '{}' in rule '{}' was not found; names are exact and case-sensitive",
                equipmentType,
                configuredTarget,
                rule);
            return null;
        }
        if (!LOTREntityNPC.class.isAssignableFrom(entityClass)) {
            HotNCold.LOG.warn("Entity '{}' in {} rule '{}' is not a LOTR NPC", configuredTarget, equipmentType, rule);
            return null;
        }

        @SuppressWarnings("unchecked")
        Class<? extends LOTREntityNPC> npcClass = (Class<? extends LOTREntityNPC>) entityClass;
        return EquipmentTarget.forNPCClass(npcClass);
    }

    static LOTRFaction resolveFaction(String configuredName) {
        for (LOTRFaction faction : LOTRFaction.values()) {
            if (faction.codeName()
                .equalsIgnoreCase(configuredName)) {
                return faction;
            }
        }
        return null;
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

    private static boolean isEmptyChoice(String itemName) {
        return "empty".equalsIgnoreCase(itemName);
    }

    private static ItemStack copyOrNull(ItemStack itemStack) {
        return itemStack == null ? null : itemStack.copy();
    }

    interface EntityResolver {

        Class resolve(String entityName);
    }

    interface ItemResolver {

        Item resolve(String itemName);
    }

    public static final class RulePreparation {

        final Map<Class<? extends LOTREntityNPC>, WeightedItemRule> weaponRules;
        final Map<LOTRFaction, WeightedItemRule> factionWeaponRules;
        final WeightedItemRule allWeaponRule;
        private final int acceptedChoices;
        private final int rejectedChoices;
        private final String equipmentType;

        private RulePreparation(Map<Class<? extends LOTREntityNPC>, WeightedItemRule> weaponRules,
            Map<LOTRFaction, WeightedItemRule> factionWeaponRules, WeightedItemRule allWeaponRule, int acceptedChoices,
            int rejectedChoices, String equipmentType) {
            this.weaponRules = Collections.unmodifiableMap(new LinkedHashMap<>(weaponRules));
            this.factionWeaponRules = Collections.unmodifiableMap(new LinkedHashMap<>(factionWeaponRules));
            this.allWeaponRule = allWeaponRule;
            this.acceptedChoices = acceptedChoices;
            this.rejectedChoices = rejectedChoices;
            this.equipmentType = equipmentType;
        }

        public String describeStartup() {
            String summaryName = "ranged weapon".equals(equipmentType) ? "LOTR NPC ranged equipment summary"
                : "LOTR NPC equipment summary";
            return summaryName + ": prepared "
                + acceptedChoices
                + " "
                + equipmentType
                + " choice(s) for "
                + weaponRules.size()
                + " exact NPC type(s)"
                + describeFactionCount(factionWeaponRules.size())
                + describeAllTarget(allWeaponRule != null)
                + "; rejected "
                + rejectedChoices
                + " invalid or duplicate choice(s).";
        }
    }

    public static final class ArmorRulePreparation {

        final Map<Class<? extends LOTREntityNPC>, ArmorRuleSet> armorRules;
        final Map<LOTRFaction, ArmorRuleSet> factionArmorRules;
        final ArmorRuleSet allArmorRules;
        private final int acceptedChoices;
        private final int rejectedChoices;
        private final int slotRuleCount;

        private ArmorRulePreparation(Map<Class<? extends LOTREntityNPC>, ArmorRuleSet> armorRules,
            Map<LOTRFaction, ArmorRuleSet> factionArmorRules, ArmorRuleSet allArmorRules, int acceptedChoices,
            int rejectedChoices, int slotRuleCount) {
            this.armorRules = Collections.unmodifiableMap(new LinkedHashMap<>(armorRules));
            this.factionArmorRules = Collections.unmodifiableMap(new LinkedHashMap<>(factionArmorRules));
            this.allArmorRules = allArmorRules;
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
                + " exact NPC type(s)"
                + describeFactionCount(factionArmorRules.size())
                + describeAllTarget(allArmorRules != null)
                + "; rejected "
                + rejectedChoices
                + " invalid or duplicate choice(s).";
        }
    }

    public static final class EquipmentRuleReloadResult {

        private final RulePreparation weaponPreparation;
        private final RulePreparation rangedWeaponPreparation;
        private final ArmorRulePreparation armorPreparation;

        EquipmentRuleReloadResult(RulePreparation weaponPreparation, RulePreparation rangedWeaponPreparation,
            ArmorRulePreparation armorPreparation) {
            this.weaponPreparation = weaponPreparation;
            this.rangedWeaponPreparation = rangedWeaponPreparation;
            this.armorPreparation = armorPreparation;
        }

        public String describeReload() {
            return "Reloaded LOTR NPC equipment rules: prepared " + weaponPreparation.acceptedChoices
                + " weapon choice(s) for "
                + weaponPreparation.weaponRules.size()
                + " exact NPC type(s)"
                + describeFactionCount(weaponPreparation.factionWeaponRules.size())
                + describeAllTarget(weaponPreparation.allWeaponRule != null)
                + describeRangedReload(rangedWeaponPreparation)
                + ", and "
                + armorPreparation.acceptedChoices
                + " armor choice(s) across "
                + armorPreparation.slotRuleCount
                + " slot rule(s) for "
                + armorPreparation.armorRules.size()
                + " exact NPC type(s)"
                + describeFactionCount(armorPreparation.factionArmorRules.size())
                + describeAllTarget(armorPreparation.allArmorRules != null)
                + "; rejected "
                + (weaponPreparation.rejectedChoices + rejectedChoices(rangedWeaponPreparation)
                    + armorPreparation.rejectedChoices)
                + " invalid or duplicate choice(s). Existing NPCs were not changed.";
        }
    }

    private static String describeRangedReload(RulePreparation preparation) {
        if (preparation == null) {
            return "";
        }
        return ", " + preparation.acceptedChoices
            + " ranged weapon choice(s) for "
            + preparation.weaponRules.size()
            + " exact NPC type(s)"
            + describeFactionCount(preparation.factionWeaponRules.size())
            + describeAllTarget(preparation.allWeaponRule != null);
    }

    private static int rejectedChoices(RulePreparation preparation) {
        return preparation == null ? 0 : preparation.rejectedChoices;
    }

    private static String describeFactionCount(int factionCount) {
        return factionCount == 0 ? "" : " and " + factionCount + " faction(s)";
    }

    private static String describeAllTarget(boolean hasAllTarget) {
        return hasAllTarget ? " and an all-NPC fallback" : "";
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

    private static final class EquipmentTarget {

        private final Class<? extends LOTREntityNPC> npcClass;
        private final LOTRFaction faction;
        private final boolean all;

        private EquipmentTarget(Class<? extends LOTREntityNPC> npcClass, LOTRFaction faction, boolean all) {
            this.npcClass = npcClass;
            this.faction = faction;
            this.all = all;
        }

        private static EquipmentTarget forNPCClass(Class<? extends LOTREntityNPC> npcClass) {
            return new EquipmentTarget(npcClass, null, false);
        }

        private static EquipmentTarget forFaction(LOTRFaction faction) {
            return new EquipmentTarget(null, faction, false);
        }

        private static EquipmentTarget forAll() {
            return new EquipmentTarget(null, null, true);
        }

        @Override
        public boolean equals(Object value) {
            if (this == value) {
                return true;
            }
            if (!(value instanceof EquipmentTarget)) {
                return false;
            }
            EquipmentTarget other = (EquipmentTarget) value;
            return npcClass == other.npcClass && faction == other.faction && all == other.all;
        }

        @Override
        public int hashCode() {
            return npcClass != null ? npcClass.hashCode() : faction != null ? faction.hashCode() : 1;
        }
    }
}
