package org.fentanylsolutions.hotncold.util;

import net.minecraft.entity.EntityList;

import org.fentanylsolutions.hotncold.HotNCold;

public class MobUtil {

    public static void printMobNames() {
        HotNCold.LOG.info("=========Mob List=========");
        HotNCold.LOG.info(
            "The printing of this list is for you to know which mob has which name. You can disable this print in the configs.");
        for (Object e : EntityList.stringToClassMapping.keySet()) {
            HotNCold.LOG.info(e + " (" + EntityList.stringToClassMapping.get(e) + ")");
        }
        HotNCold.LOG.info("=============================");
    }

    public static String getClassByName(String name) {
        Object res = EntityList.stringToClassMapping.get(name);
        if (res != null) {
            return ((Class) res).getCanonicalName();
        }
        return null;
    }
}
