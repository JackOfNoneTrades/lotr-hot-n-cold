package org.fentanylsolutions.hotncold.compat;

import net.minecraft.item.ItemStack;

/** Implemented only when both Campfire Backport and MineFantasy 2 are present. */
public interface MineFantasyCampfire {

    String FUEL_TAG = "HotNColdFirepitFuel";
    String ENABLED_TAG = "HotNColdFirepitEnabled";
    int FUEL_CAPACITY = 12000;

    boolean hotncold$isFuelEnabled();

    int hotncold$getFuel();

    boolean hotncold$addFuel(ItemStack stack);
}
