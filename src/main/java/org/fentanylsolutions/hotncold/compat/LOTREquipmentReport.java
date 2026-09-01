package org.fentanylsolutions.hotncold.compat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.entity.EntityList;

import org.fentanylsolutions.hotncold.Config;

import lotr.common.entity.npc.LOTREntityNPC;

public final class LOTREquipmentReport {

    private LOTREquipmentReport() {}

    public static List<String> createEquipmentExplanation(String entityName) {
        return createEquipmentExplanation(
            entityName,
            EntityList.stringToClassMapping.get(entityName),
            LOTREquipmentControl.getPreparedWeaponRules(),
            LOTREquipmentControl.getPreparedArmorRules(),
            Config.replaceExistingLOTREquipment,
            Config.customizeHiredLOTREquipment,
            Config.customizeNamedLOTREquipment);
    }

    static List<String> createEquipmentExplanation(String entityName, Object mappedEntityClass,
        Map<Class<? extends LOTREntityNPC>, LOTREquipmentControl.WeightedItemRule> weaponRules,
        Map<Class<? extends LOTREntityNPC>, LOTREquipmentControl.ArmorRuleSet> armorRules, boolean replaceExisting,
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
        LOTREquipmentControl.ArmorRuleSet armorRuleSet = armorRules.get(npcClass);

        List<String> lines = new ArrayList<>();
        lines.add("Equipment rules for " + entityName + " (" + entityClass.getName() + "):");
        if (weaponRule == null) {
            lines.add("  Weapon: no configured rule.");
        } else {
            appendChoices(lines, "Weapon", weaponRule);
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

        lines
            .add("  Mode: configured choices " + (replaceExisting ? "replace normal gear." : "fill only empty slots."));
        lines.add("  Hired NPCs: " + (customizeHired ? "included." : "protected."));
        lines.add("  NPCs with custom name tags: " + (customizeNamed ? "included." : "protected."));
        lines.add("  Existing NPCs are not changed; these rules apply to future natural spawns.");
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
        return names.toArray(new String[0]);
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
