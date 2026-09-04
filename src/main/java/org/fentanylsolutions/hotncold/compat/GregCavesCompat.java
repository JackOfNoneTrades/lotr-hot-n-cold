package org.fentanylsolutions.hotncold.compat;

import net.minecraft.world.gen.MapGenBase;

import org.fentanylsolutions.hotncold.HotNCold;

public final class GregCavesCompat {

    private GregCavesCompat() {}

    public static MapGenBase createGenerator() {
        try {
            MapGenBase generator = Class.forName("mods.tesseract.gregcaves.world.MapGenGregCaves")
                .asSubclass(MapGenBase.class)
                .getConstructor()
                .newInstance();
            HotNCold.LOG.info("Enabled Greg Caves generation in Middle-earth");
            return generator;
        } catch (ReflectiveOperationException | LinkageError e) {
            HotNCold.LOG.error("Could not create the Greg Caves generator; retaining LOTR cave generation", e);
            return null;
        }
    }
}
