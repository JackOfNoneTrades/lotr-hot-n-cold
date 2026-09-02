package org.fentanylsolutions.hotncold.compat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.world.World;

import org.fentanylsolutions.hotncold.Config;

import lotr.common.entity.npc.LOTREntityNPC;
import lotr.common.fac.LOTRFaction;

public final class LOTREquipmentReport {

    private LOTREquipmentReport() {}

    public static List<String> createEquipmentExplanation(String entityName) {
        if (isAllTarget(entityName)) {
            List<String> lines = createAllExplanation(
                LOTREquipmentControl.getPreparedAllWeaponRule(),
                LOTREquipmentControl.getPreparedAllRangedWeaponRule(),
                LOTREquipmentControl.getPreparedAllArmorRules(),
                Config.replaceExistingLOTREquipment,
                Config.customizeHiredLOTREquipment,
                Config.customizeNamedLOTREquipment);
            appendShieldExplanation(lines, null, null, true);
            return lines;
        }
        if (isFactionTarget(entityName)) {
            String factionName = entityName.substring("faction:".length())
                .trim();
            LOTRFaction faction = LOTREquipmentControl.resolveFaction(factionName);
            List<String> lines = createFactionExplanation(
                factionName,
                faction,
                LOTREquipmentControl.getPreparedFactionWeaponRules(),
                LOTREquipmentControl.getPreparedFactionArmorRules(),
                LOTREquipmentControl.getPreparedAllWeaponRule(),
                LOTREquipmentControl.getPreparedAllArmorRules(),
                LOTREquipmentControl.getPreparedFactionRangedWeaponRules(),
                LOTREquipmentControl.getPreparedAllRangedWeaponRule(),
                Config.replaceExistingLOTREquipment,
                Config.customizeHiredLOTREquipment,
                Config.customizeNamedLOTREquipment);
            appendShieldExplanation(lines, null, faction, false);
            return lines;
        }
        if (isGroupTarget(entityName)) {
            return createGroupExplanation(entityName.substring("group:".length()));
        }
        Object mappedEntityClass = EntityList.stringToClassMapping.get(entityName);
        List<String> lines = createEquipmentExplanation(
            entityName,
            mappedEntityClass,
            LOTREquipmentControl.getPreparedWeaponRules(),
            LOTREquipmentControl.getPreparedArmorRules(),
            null,
            Collections.<LOTRFaction, LOTREquipmentControl.WeightedItemRule>emptyMap(),
            Collections.<LOTRFaction, LOTREquipmentControl.ArmorRuleSet>emptyMap(),
            LOTREquipmentControl.getPreparedAllWeaponRule(),
            LOTREquipmentControl.getPreparedAllArmorRules(),
            LOTREquipmentControl.getPreparedRangedWeaponRules(),
            Collections.<LOTRFaction, LOTREquipmentControl.WeightedItemRule>emptyMap(),
            LOTREquipmentControl.getPreparedAllRangedWeaponRule(),
            Config.replaceExistingLOTREquipment,
            Config.customizeHiredLOTREquipment,
            Config.customizeNamedLOTREquipment);
        appendShieldExplanation(lines, asNPCClass(mappedEntityClass), null, false);
        return lines;
    }

    public static List<String> createEquipmentExplanation(String entityName, World world) {
        if (isFactionTarget(entityName) || isGroupTarget(entityName) || isAllTarget(entityName)) {
            return createEquipmentExplanation(entityName);
        }

        Object mappedEntityClass = EntityList.stringToClassMapping.get(entityName);
        LOTRFaction faction = null;
        if (mappedEntityClass instanceof Class && LOTREntityNPC.class.isAssignableFrom((Class) mappedEntityClass)) {
            Entity entity = EntityList.createEntityByName(entityName, world);
            if (entity instanceof LOTREntityNPC) {
                faction = ((LOTREntityNPC) entity).getFaction();
            }
        }
        List<String> lines = createEquipmentExplanation(
            entityName,
            mappedEntityClass,
            LOTREquipmentControl.getPreparedWeaponRules(),
            LOTREquipmentControl.getPreparedArmorRules(),
            faction,
            LOTREquipmentControl.getPreparedFactionWeaponRules(),
            LOTREquipmentControl.getPreparedFactionArmorRules(),
            LOTREquipmentControl.getPreparedAllWeaponRule(),
            LOTREquipmentControl.getPreparedAllArmorRules(),
            LOTREquipmentControl.getPreparedRangedWeaponRules(),
            LOTREquipmentControl.getPreparedFactionRangedWeaponRules(),
            LOTREquipmentControl.getPreparedAllRangedWeaponRule(),
            Config.replaceExistingLOTREquipment,
            Config.customizeHiredLOTREquipment,
            Config.customizeNamedLOTREquipment);
        appendShieldExplanation(lines, asNPCClass(mappedEntityClass), faction, false);
        return lines;
    }

    static List<String> createEquipmentExplanation(String entityName, Object mappedEntityClass,
        Map<Class<? extends LOTREntityNPC>, LOTREquipmentControl.WeightedItemRule> weaponRules,
        Map<Class<? extends LOTREntityNPC>, LOTREquipmentControl.ArmorRuleSet> armorRules, boolean replaceExisting,
        boolean customizeHired, boolean customizeNamed) {
        return createEquipmentExplanation(
            entityName,
            mappedEntityClass,
            weaponRules,
            armorRules,
            null,
            Collections.<LOTRFaction, LOTREquipmentControl.WeightedItemRule>emptyMap(),
            Collections.<LOTRFaction, LOTREquipmentControl.ArmorRuleSet>emptyMap(),
            null,
            null,
            Collections.<Class<? extends LOTREntityNPC>, LOTREquipmentControl.WeightedItemRule>emptyMap(),
            Collections.<LOTRFaction, LOTREquipmentControl.WeightedItemRule>emptyMap(),
            null,
            replaceExisting,
            customizeHired,
            customizeNamed);
    }

    static List<String> createEquipmentExplanation(String entityName, Object mappedEntityClass,
        Map<Class<? extends LOTREntityNPC>, LOTREquipmentControl.WeightedItemRule> weaponRules,
        Map<Class<? extends LOTREntityNPC>, LOTREquipmentControl.ArmorRuleSet> armorRules, LOTRFaction faction,
        Map<LOTRFaction, LOTREquipmentControl.WeightedItemRule> factionWeaponRules,
        Map<LOTRFaction, LOTREquipmentControl.ArmorRuleSet> factionArmorRules, boolean replaceExisting,
        boolean customizeHired, boolean customizeNamed) {
        return createEquipmentExplanation(
            entityName,
            mappedEntityClass,
            weaponRules,
            armorRules,
            faction,
            factionWeaponRules,
            factionArmorRules,
            null,
            null,
            Collections.<Class<? extends LOTREntityNPC>, LOTREquipmentControl.WeightedItemRule>emptyMap(),
            Collections.<LOTRFaction, LOTREquipmentControl.WeightedItemRule>emptyMap(),
            null,
            replaceExisting,
            customizeHired,
            customizeNamed);
    }

    static List<String> createEquipmentExplanation(String entityName, Object mappedEntityClass,
        Map<Class<? extends LOTREntityNPC>, LOTREquipmentControl.WeightedItemRule> weaponRules,
        Map<Class<? extends LOTREntityNPC>, LOTREquipmentControl.ArmorRuleSet> armorRules, LOTRFaction faction,
        Map<LOTRFaction, LOTREquipmentControl.WeightedItemRule> factionWeaponRules,
        Map<LOTRFaction, LOTREquipmentControl.ArmorRuleSet> factionArmorRules,
        LOTREquipmentControl.WeightedItemRule allWeaponRule, LOTREquipmentControl.ArmorRuleSet allArmorRuleSet,
        boolean replaceExisting, boolean customizeHired, boolean customizeNamed) {
        return createEquipmentExplanation(
            entityName,
            mappedEntityClass,
            weaponRules,
            armorRules,
            faction,
            factionWeaponRules,
            factionArmorRules,
            allWeaponRule,
            allArmorRuleSet,
            Collections.<Class<? extends LOTREntityNPC>, LOTREquipmentControl.WeightedItemRule>emptyMap(),
            Collections.<LOTRFaction, LOTREquipmentControl.WeightedItemRule>emptyMap(),
            null,
            replaceExisting,
            customizeHired,
            customizeNamed);
    }

    static List<String> createEquipmentExplanation(String entityName, Object mappedEntityClass,
        Map<Class<? extends LOTREntityNPC>, LOTREquipmentControl.WeightedItemRule> weaponRules,
        Map<Class<? extends LOTREntityNPC>, LOTREquipmentControl.ArmorRuleSet> armorRules, LOTRFaction faction,
        Map<LOTRFaction, LOTREquipmentControl.WeightedItemRule> factionWeaponRules,
        Map<LOTRFaction, LOTREquipmentControl.ArmorRuleSet> factionArmorRules,
        LOTREquipmentControl.WeightedItemRule allWeaponRule, LOTREquipmentControl.ArmorRuleSet allArmorRuleSet,
        Map<Class<? extends LOTREntityNPC>, LOTREquipmentControl.WeightedItemRule> rangedWeaponRules,
        Map<LOTRFaction, LOTREquipmentControl.WeightedItemRule> factionRangedWeaponRules,
        LOTREquipmentControl.WeightedItemRule allRangedWeaponRule, boolean replaceExisting, boolean customizeHired,
        boolean customizeNamed) {
        if (!(mappedEntityClass instanceof Class)) {
            return Collections
                .singletonList("Entity '" + entityName + "' was not found. Names are exact and case-sensitive.");
        }

        Class entityClass = (Class) mappedEntityClass;
        if (!LOTREntityNPC.class.isAssignableFrom(entityClass)) {
            return Collections.singletonList("Entity '" + entityName + "' is not a LOTR NPC.");
        }

        @SuppressWarnings("unchecked")
        Class<? extends LOTREntityNPC> npcClass = (Class<? extends LOTREntityNPC>) entityClass;
        LOTREquipmentControl.WeightedItemRule weaponRule = weaponRules.get(npcClass);
        String weaponSource = null;
        if (weaponRule == null) {
            String group = LOTREquipmentControl
                .findGroupSource(npcClass, LOTREquipmentControl.getPreparedGroupWeaponRules());
            if (group != null) {
                weaponRule = LOTREquipmentControl.getPreparedGroupWeaponRules()
                    .get(group);
                weaponSource = "group:" + group;
            }
        }
        if (weaponRule == null && faction != null) {
            weaponRule = factionWeaponRules.get(faction);
            if (weaponRule != null) {
                weaponSource = "faction:" + faction.codeName();
            }
        }
        if (weaponRule == null) {
            weaponRule = allWeaponRule;
            if (weaponRule != null) {
                weaponSource = "all";
            }
        }
        LOTREquipmentControl.ArmorRuleSet exactArmorRuleSet = armorRules.get(npcClass);
        LOTREquipmentControl.ArmorRuleSet factionArmorRuleSet = faction == null ? null : factionArmorRules.get(faction);

        List<String> lines = new ArrayList<>();
        lines.add("Equipment rules for " + entityName + " (" + entityClass.getName() + "):");
        if (weaponRule == null) {
            lines.add("  Weapon: no configured rule.");
        } else {
            appendChoices(lines, "Weapon" + sourceSuffix(weaponSource), weaponRule);
        }

        LOTREquipmentControl.WeightedItemRule rangedWeaponRule = rangedWeaponRules.get(npcClass);
        String rangedWeaponSource = null;
        if (rangedWeaponRule == null) {
            String group = LOTREquipmentControl
                .findGroupSource(npcClass, LOTREquipmentControl.getPreparedGroupRangedWeaponRules());
            if (group != null) {
                rangedWeaponRule = LOTREquipmentControl.getPreparedGroupRangedWeaponRules()
                    .get(group);
                rangedWeaponSource = "group:" + group;
            }
        }
        if (rangedWeaponRule == null && faction != null) {
            rangedWeaponRule = factionRangedWeaponRules.get(faction);
            if (rangedWeaponRule != null) {
                rangedWeaponSource = "faction:" + faction.codeName();
            }
        }
        if (rangedWeaponRule == null) {
            rangedWeaponRule = allRangedWeaponRule;
            if (rangedWeaponRule != null) {
                rangedWeaponSource = "all";
            }
        }
        boolean rangedRulesConfigured = !rangedWeaponRules.isEmpty()
            || !LOTREquipmentControl.getPreparedGroupRangedWeaponRules()
                .isEmpty()
            || !factionRangedWeaponRules.isEmpty()
            || allRangedWeaponRule != null;
        if (rangedRulesConfigured) {
            if (rangedWeaponRule == null) {
                lines.add("  Ranged weapon: no configured rule.");
            } else {
                appendChoices(lines, "Ranged weapon" + sourceSuffix(rangedWeaponSource), rangedWeaponRule);
            }
        }

        boolean usedFallbackRule = weaponSource != null || rangedWeaponSource != null;
        if (exactArmorRuleSet == null && factionArmorRuleSet == null && allArmorRuleSet == null) {
            lines.add("  Armor: no configured rules.");
        } else {
            for (LOTREquipmentControl.ArmorSlot slot : LOTREquipmentControl.ArmorSlot.values()) {
                LOTREquipmentControl.WeightedItemRule slotRule = exactArmorRuleSet == null ? null
                    : exactArmorRuleSet.slotRules.get(slot);
                String slotSource = null;
                if (slotRule == null) {
                    String group = LOTREquipmentControl.findGroupArmorSlotSource(npcClass, slot);
                    if (group != null) {
                        slotRule = LOTREquipmentControl.getPreparedGroupArmorRules()
                            .get(group).slotRules.get(slot);
                        slotSource = "group:" + group;
                    }
                }
                if (slotRule == null && factionArmorRuleSet != null) {
                    slotRule = factionArmorRuleSet.slotRules.get(slot);
                    if (slotRule != null) {
                        slotSource = "faction:" + faction.codeName();
                    }
                }
                if (slotRule == null && allArmorRuleSet != null) {
                    slotRule = allArmorRuleSet.slotRules.get(slot);
                    if (slotRule != null) {
                        slotSource = "all";
                    }
                }
                if (slotRule != null) {
                    appendChoices(lines, capitalize(slot.configName) + sourceSuffix(slotSource), slotRule);
                    usedFallbackRule |= slotSource != null;
                }
            }
        }

        if (usedFallbackRule) {
            appendPriority(lines, allWeaponRule != null || allRangedWeaponRule != null || allArmorRuleSet != null);
        }
        appendSettings(lines, replaceExisting, customizeHired, customizeNamed);
        return lines;
    }

    static List<String> createFactionExplanation(String configuredName, LOTRFaction faction,
        Map<LOTRFaction, LOTREquipmentControl.WeightedItemRule> weaponRules,
        Map<LOTRFaction, LOTREquipmentControl.ArmorRuleSet> armorRules, boolean replaceExisting, boolean customizeHired,
        boolean customizeNamed) {
        return createFactionExplanation(
            configuredName,
            faction,
            weaponRules,
            armorRules,
            null,
            null,
            Collections.<LOTRFaction, LOTREquipmentControl.WeightedItemRule>emptyMap(),
            null,
            replaceExisting,
            customizeHired,
            customizeNamed);
    }

    static List<String> createGroupExplanation(String configuredName) {
        String groupName = LOTREquipmentControl.normalizeGroupName(configuredName);
        Set<Class<? extends LOTREntityNPC>> members = LOTREquipmentControl.getPreparedNPCGroups()
            .get(groupName);
        if (members == null) {
            return Collections.singletonList(
                "LOTR NPC group '" + configuredName + "' was not found. Define it in lotrNPCGroupMembers.");
        }

        List<String> lines = new ArrayList<>();
        lines.add("Equipment rules for group:" + groupName + " (" + members.size() + " NPC type(s)):");
        LOTREquipmentControl.WeightedItemRule weaponRule = LOTREquipmentControl.getPreparedGroupWeaponRules()
            .get(groupName);
        if (weaponRule == null) {
            weaponRule = LOTREquipmentControl.getPreparedAllWeaponRule();
        }
        if (weaponRule == null) {
            lines.add("  Weapon: no configured rule.");
        } else {
            appendChoices(lines, "Weapon", weaponRule);
        }

        LOTREquipmentControl.WeightedItemRule rangedRule = LOTREquipmentControl.getPreparedGroupRangedWeaponRules()
            .get(groupName);
        if (rangedRule == null) {
            rangedRule = LOTREquipmentControl.getPreparedAllRangedWeaponRule();
        }
        if (rangedRule != null) {
            appendChoices(lines, "Ranged weapon", rangedRule);
        }

        LOTREquipmentControl.WeightedShieldRule shieldRule = LOTREquipmentControl.getPreparedGroupShieldRules()
            .get(groupName);
        if (shieldRule == null) {
            shieldRule = LOTREquipmentControl.getPreparedAllShieldRule();
        }
        if (shieldRule != null) {
            appendShieldChoices(lines, "Shield", shieldRule);
        }

        LOTREquipmentControl.ArmorRuleSet groupArmor = LOTREquipmentControl.getPreparedGroupArmorRules()
            .get(groupName);
        LOTREquipmentControl.ArmorRuleSet allArmor = LOTREquipmentControl.getPreparedAllArmorRules();
        if (groupArmor == null && allArmor == null) {
            lines.add("  Armor: no configured rules.");
        } else {
            for (LOTREquipmentControl.ArmorSlot slot : LOTREquipmentControl.ArmorSlot.values()) {
                LOTREquipmentControl.WeightedItemRule slotRule = groupArmor == null ? null
                    : groupArmor.slotRules.get(slot);
                if (slotRule == null && allArmor != null) {
                    slotRule = allArmor.slotRules.get(slot);
                }
                if (slotRule != null) {
                    appendChoices(lines, capitalize(slot.configName), slotRule);
                }
            }
        }
        appendPriority(lines, true);
        appendSettings(
            lines,
            Config.replaceExistingLOTREquipment,
            Config.customizeHiredLOTREquipment,
            Config.customizeNamedLOTREquipment);
        return lines;
    }

    static List<String> createFactionExplanation(String configuredName, LOTRFaction faction,
        Map<LOTRFaction, LOTREquipmentControl.WeightedItemRule> weaponRules,
        Map<LOTRFaction, LOTREquipmentControl.ArmorRuleSet> armorRules,
        LOTREquipmentControl.WeightedItemRule allWeaponRule, LOTREquipmentControl.ArmorRuleSet allArmorRuleSet,
        boolean replaceExisting, boolean customizeHired, boolean customizeNamed) {
        return createFactionExplanation(
            configuredName,
            faction,
            weaponRules,
            armorRules,
            allWeaponRule,
            allArmorRuleSet,
            Collections.<LOTRFaction, LOTREquipmentControl.WeightedItemRule>emptyMap(),
            null,
            replaceExisting,
            customizeHired,
            customizeNamed);
    }

    static List<String> createFactionExplanation(String configuredName, LOTRFaction faction,
        Map<LOTRFaction, LOTREquipmentControl.WeightedItemRule> weaponRules,
        Map<LOTRFaction, LOTREquipmentControl.ArmorRuleSet> armorRules,
        LOTREquipmentControl.WeightedItemRule allWeaponRule, LOTREquipmentControl.ArmorRuleSet allArmorRuleSet,
        Map<LOTRFaction, LOTREquipmentControl.WeightedItemRule> rangedWeaponRules,
        LOTREquipmentControl.WeightedItemRule allRangedWeaponRule, boolean replaceExisting, boolean customizeHired,
        boolean customizeNamed) {
        if (faction == null) {
            return Collections.singletonList(
                "LOTR faction '" + configuredName + "' was not found. Use a faction code such as GONDOR or ROHAN.");
        }

        List<String> lines = new ArrayList<>();
        lines.add("Equipment rules for faction:" + faction.codeName() + ":");
        LOTREquipmentControl.WeightedItemRule weaponRule = weaponRules.get(faction);
        String weaponSource = null;
        if (weaponRule == null) {
            weaponRule = allWeaponRule;
            if (weaponRule != null) {
                weaponSource = "all";
            }
        }
        if (weaponRule == null) {
            lines.add("  Weapon: no configured rule.");
        } else {
            appendChoices(lines, "Weapon" + sourceSuffix(weaponSource), weaponRule);
        }

        LOTREquipmentControl.WeightedItemRule rangedWeaponRule = rangedWeaponRules.get(faction);
        String rangedWeaponSource = null;
        if (rangedWeaponRule == null) {
            rangedWeaponRule = allRangedWeaponRule;
            if (rangedWeaponRule != null) {
                rangedWeaponSource = "all";
            }
        }
        if (!rangedWeaponRules.isEmpty() || allRangedWeaponRule != null) {
            if (rangedWeaponRule == null) {
                lines.add("  Ranged weapon: no configured rule.");
            } else {
                appendChoices(lines, "Ranged weapon" + sourceSuffix(rangedWeaponSource), rangedWeaponRule);
            }
        }

        LOTREquipmentControl.ArmorRuleSet armorRuleSet = armorRules.get(faction);
        if (armorRuleSet == null && allArmorRuleSet == null) {
            lines.add("  Armor: no configured rules.");
        } else {
            for (LOTREquipmentControl.ArmorSlot slot : LOTREquipmentControl.ArmorSlot.values()) {
                LOTREquipmentControl.WeightedItemRule slotRule = armorRuleSet == null ? null
                    : armorRuleSet.slotRules.get(slot);
                String slotSource = null;
                if (slotRule == null && allArmorRuleSet != null) {
                    slotRule = allArmorRuleSet.slotRules.get(slot);
                    if (slotRule != null) {
                        slotSource = "all";
                    }
                }
                if (slotRule != null) {
                    appendChoices(lines, capitalize(slot.configName) + sourceSuffix(slotSource), slotRule);
                }
            }
        }

        appendPriority(lines, allWeaponRule != null || allRangedWeaponRule != null || allArmorRuleSet != null);
        appendSettings(lines, replaceExisting, customizeHired, customizeNamed);
        return lines;
    }

    static List<String> createAllExplanation(LOTREquipmentControl.WeightedItemRule weaponRule,
        LOTREquipmentControl.ArmorRuleSet armorRuleSet, boolean replaceExisting, boolean customizeHired,
        boolean customizeNamed) {
        return createAllExplanation(weaponRule, null, armorRuleSet, replaceExisting, customizeHired, customizeNamed);
    }

    static List<String> createAllExplanation(LOTREquipmentControl.WeightedItemRule weaponRule,
        LOTREquipmentControl.WeightedItemRule rangedWeaponRule, LOTREquipmentControl.ArmorRuleSet armorRuleSet,
        boolean replaceExisting, boolean customizeHired, boolean customizeNamed) {
        List<String> lines = new ArrayList<>();
        lines.add("Equipment rules for all LOTR NPCs:");
        if (weaponRule == null) {
            lines.add("  Weapon: no configured rule.");
        } else {
            appendChoices(lines, "Weapon", weaponRule);
        }
        if (rangedWeaponRule != null) {
            appendChoices(lines, "Ranged weapon", rangedWeaponRule);
        }
        if (armorRuleSet == null) {
            lines.add("  Armor: no configured rules.");
        } else {
            for (LOTREquipmentControl.ArmorSlot slot : LOTREquipmentControl.ArmorSlot.values()) {
                LOTREquipmentControl.WeightedItemRule slotRule = armorRuleSet.slotRules.get(slot);
                if (slotRule != null) {
                    appendChoices(lines, capitalize(slot.configName), slotRule);
                }
            }
        }
        appendPriority(lines, true);
        appendSettings(lines, replaceExisting, customizeHired, customizeNamed);
        return lines;
    }

    public static String[] getLOTRNPCEntityNames() {
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Object value : EntityList.stringToClassMapping.entrySet()) {
            Map.Entry entry = (Map.Entry) value;
            if (entry.getKey() instanceof String && entry.getValue() instanceof Class
                && LOTREntityNPC.class.isAssignableFrom((Class) entry.getValue())) {
                names.add((String) entry.getKey());
            }
        }
        for (LOTRFaction faction : LOTRFaction.values()) {
            names.add("faction:" + faction.codeName());
        }
        for (String groupName : LOTREquipmentControl.getPreparedNPCGroups()
            .keySet()) {
            names.add("group:" + groupName);
        }
        names.add("all");
        return names.toArray(new String[0]);
    }

    private static boolean isFactionTarget(String configuredTarget) {
        return configuredTarget != null && configuredTarget.regionMatches(true, 0, "faction:", 0, "faction:".length());
    }

    private static boolean isGroupTarget(String configuredTarget) {
        return configuredTarget != null && configuredTarget.regionMatches(true, 0, "group:", 0, "group:".length());
    }

    private static boolean isAllTarget(String configuredTarget) {
        return configuredTarget != null && "all".equalsIgnoreCase(configuredTarget.trim());
    }

    private static void appendSettings(List<String> lines, boolean replaceExisting, boolean customizeHired,
        boolean customizeNamed) {
        lines
            .add("  Mode: configured choices " + (replaceExisting ? "replace normal gear." : "fill only empty slots."));
        lines.add("  Hired NPCs: " + (customizeHired ? "included." : "protected."));
        lines.add("  NPCs with custom name tags: " + (customizeNamed ? "included." : "protected."));
        lines.add("  Quest-linked NPCs: " + (Config.customizeQuestLOTREquipment ? "included." : "protected."));
        lines.add(
            "  Persistent or location-specific NPCs: "
                + (Config.customizePersistentLOTREquipment ? "included." : "protected."));
        lines.add("  Existing NPCs are not changed; these rules apply to future natural spawns.");
    }

    private static String sourceSuffix(String source) {
        return source == null ? "" : " (from " + source + ")";
    }

    private static void appendPriority(List<String> lines, boolean hasAllRules) {
        if (hasAllRules) {
            lines
                .add("  Priority for the same equipment slot is exact NPC, custom group, faction, then all-NPC rules.");
        } else {
            lines.add("  Exact NPC rules take priority over custom group and faction rules for the same slot.");
        }
    }

    private static void appendChoices(List<String> lines, String label, LOTREquipmentControl.WeightedItemRule rule) {
        lines.add("  " + label + " choices (total weight " + rule.totalWeight + "):");
        for (LOTREquipmentControl.WeightedItem choice : rule.choices) {
            double percentage = choice.weight * 100D / rule.totalWeight;
            lines.add(
                "    " + choice.itemName
                    + " - weight "
                    + choice.weight
                    + " ("
                    + String.format(Locale.ROOT, "%.1f", percentage)
                    + "%)");
        }
    }

    private static void appendShieldExplanation(List<String> lines, Class<? extends LOTREntityNPC> npcClass,
        LOTRFaction faction, boolean allTarget) {
        Map<Class<? extends LOTREntityNPC>, LOTREquipmentControl.WeightedShieldRule> exactRules = LOTREquipmentControl
            .getPreparedShieldRules();
        Map<LOTRFaction, LOTREquipmentControl.WeightedShieldRule> factionRules = LOTREquipmentControl
            .getPreparedFactionShieldRules();
        LOTREquipmentControl.WeightedShieldRule allRule = LOTREquipmentControl.getPreparedAllShieldRule();
        if (exactRules.isEmpty() && factionRules.isEmpty() && allRule == null) {
            return;
        }

        LOTREquipmentControl.WeightedShieldRule rule = allTarget || npcClass == null ? null : exactRules.get(npcClass);
        String source = null;
        if (!allTarget && rule == null && npcClass != null) {
            String group = LOTREquipmentControl
                .findGroupSource(npcClass, LOTREquipmentControl.getPreparedGroupShieldRules());
            if (group != null) {
                rule = LOTREquipmentControl.getPreparedGroupShieldRules()
                    .get(group);
                source = "group:" + group;
            }
        }
        if (!allTarget && rule == null && faction != null) {
            rule = factionRules.get(faction);
            if (rule != null && npcClass != null) {
                source = "faction:" + faction.codeName();
            }
        }
        if (rule == null) {
            rule = allRule;
            if (rule != null && !allTarget) {
                source = "all";
            }
        }

        List<String> shieldLines = new ArrayList<>();
        if (rule == null) {
            shieldLines.add("  Shield: no configured rule.");
        } else {
            appendShieldChoices(shieldLines, "Shield" + sourceSuffix(source), rule);
        }
        int insertionIndex = lines.size();
        for (int index = 0; index < lines.size(); index++) {
            if (lines.get(index)
                .startsWith("  Mode:")) {
                insertionIndex = index;
                break;
            }
        }
        lines.addAll(insertionIndex, shieldLines);
    }

    private static void appendShieldChoices(List<String> lines, String label,
        LOTREquipmentControl.WeightedShieldRule rule) {
        lines.add("  " + label + " choices (total weight " + rule.totalWeight + "):");
        for (LOTREquipmentControl.WeightedShield choice : rule.choices) {
            double percentage = choice.weight * 100D / rule.totalWeight;
            lines.add(
                "    " + choice.shieldName
                    + " - weight "
                    + choice.weight
                    + " ("
                    + String.format(Locale.ROOT, "%.1f", percentage)
                    + "%)");
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends LOTREntityNPC> asNPCClass(Object mappedEntityClass) {
        if (mappedEntityClass instanceof Class && LOTREntityNPC.class.isAssignableFrom((Class) mappedEntityClass)) {
            return (Class<? extends LOTREntityNPC>) mappedEntityClass;
        }
        return null;
    }

    private static String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
