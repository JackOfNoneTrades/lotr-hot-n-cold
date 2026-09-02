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
        if (isFactionTarget(entityName)) {
            String factionName = entityName.substring("faction:".length())
                .trim();
            return createFactionExplanation(
                factionName,
                LOTREquipmentControl.resolveFaction(factionName),
                LOTREquipmentControl.getPreparedFactionWeaponRules(),
                LOTREquipmentControl.getPreparedFactionArmorRules(),
                Config.replaceExistingLOTREquipment,
                Config.customizeHiredLOTREquipment,
                Config.customizeNamedLOTREquipment);
        }
        return createEquipmentExplanation(
            entityName,
            EntityList.stringToClassMapping.get(entityName),
            LOTREquipmentControl.getPreparedWeaponRules(),
            LOTREquipmentControl.getPreparedArmorRules(),
            Config.replaceExistingLOTREquipment,
            Config.customizeHiredLOTREquipment,
            Config.customizeNamedLOTREquipment);
    }

    public static List<String> createEquipmentExplanation(String entityName, World world) {
        if (isFactionTarget(entityName)) {
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
        return createEquipmentExplanation(
            entityName,
            mappedEntityClass,
            LOTREquipmentControl.getPreparedWeaponRules(),
            LOTREquipmentControl.getPreparedArmorRules(),
            faction,
            LOTREquipmentControl.getPreparedFactionWeaponRules(),
            LOTREquipmentControl.getPreparedFactionArmorRules(),
            Config.replaceExistingLOTREquipment,
            Config.customizeHiredLOTREquipment,
            Config.customizeNamedLOTREquipment);
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
        boolean weaponFromFaction = false;
        if (weaponRule == null && faction != null) {
            weaponRule = factionWeaponRules.get(faction);
            weaponFromFaction = weaponRule != null;
        }
        LOTREquipmentControl.ArmorRuleSet exactArmorRuleSet = armorRules.get(npcClass);
        LOTREquipmentControl.ArmorRuleSet factionArmorRuleSet = faction == null ? null : factionArmorRules.get(faction);

        List<String> lines = new ArrayList<>();
        lines.add("Equipment rules for " + entityName + " (" + entityClass.getName() + "):");
        if (weaponRule == null) {
            lines.add("  Weapon: no configured rule.");
        } else {
            appendChoices(lines, "Weapon" + factionSourceSuffix(weaponFromFaction, faction), weaponRule);
        }

        boolean usedFactionArmorRule = false;
        if (exactArmorRuleSet == null && factionArmorRuleSet == null) {
            lines.add("  Armor: no configured rules.");
        } else {
            for (LOTREquipmentControl.ArmorSlot slot : LOTREquipmentControl.ArmorSlot.values()) {
                LOTREquipmentControl.WeightedItemRule slotRule = exactArmorRuleSet == null ? null
                    : exactArmorRuleSet.slotRules.get(slot);
                boolean slotFromFaction = false;
                if (slotRule == null && factionArmorRuleSet != null) {
                    slotRule = factionArmorRuleSet.slotRules.get(slot);
                    slotFromFaction = slotRule != null;
                    usedFactionArmorRule |= slotFromFaction;
                }
                if (slotRule != null) {
                    appendChoices(
                        lines,
                        capitalize(slot.configName) + factionSourceSuffix(slotFromFaction, faction),
                        slotRule);
                }
            }
        }

        if (weaponFromFaction || usedFactionArmorRule) {
            lines.add("  Exact NPC rules take priority over faction rules for the same equipment slot.");
        }
        appendSettings(lines, replaceExisting, customizeHired, customizeNamed);
        return lines;
    }

    static List<String> createFactionExplanation(String configuredName, LOTRFaction faction,
        Map<LOTRFaction, LOTREquipmentControl.WeightedItemRule> weaponRules,
        Map<LOTRFaction, LOTREquipmentControl.ArmorRuleSet> armorRules, boolean replaceExisting, boolean customizeHired,
        boolean customizeNamed) {
        if (faction == null) {
            return Collections.singletonList(
                "LOTR faction '" + configuredName + "' was not found. Use a faction code such as GONDOR or ROHAN.");
        }

        List<String> lines = new ArrayList<>();
        lines.add("Equipment rules for faction:" + faction.codeName() + ":");
        LOTREquipmentControl.WeightedItemRule weaponRule = weaponRules.get(faction);
        if (weaponRule == null) {
            lines.add("  Weapon: no configured rule.");
        } else {
            appendChoices(lines, "Weapon", weaponRule);
        }

        LOTREquipmentControl.ArmorRuleSet armorRuleSet = armorRules.get(faction);
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

        lines.add("  Exact NPC rules take priority over faction rules for the same equipment slot.");
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
        return names.toArray(new String[0]);
    }

    private static boolean isFactionTarget(String configuredTarget) {
        return configuredTarget != null && configuredTarget.regionMatches(true, 0, "faction:", 0, "faction:".length());
    }

    private static void appendSettings(List<String> lines, boolean replaceExisting, boolean customizeHired,
        boolean customizeNamed) {
        lines
            .add("  Mode: configured choices " + (replaceExisting ? "replace normal gear." : "fill only empty slots."));
        lines.add("  Hired NPCs: " + (customizeHired ? "included." : "protected."));
        lines.add("  NPCs with custom name tags: " + (customizeNamed ? "included." : "protected."));
        lines.add("  Existing NPCs are not changed; these rules apply to future natural spawns.");
    }

    private static String factionSourceSuffix(boolean fromFaction, LOTRFaction faction) {
        return fromFaction ? " (from faction:" + faction.codeName() + ")" : "";
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

    private static String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
