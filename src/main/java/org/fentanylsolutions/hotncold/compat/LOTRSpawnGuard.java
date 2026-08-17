package org.fentanylsolutions.hotncold.compat;

import net.minecraft.util.MathHelper;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public final class LOTRSpawnGuard {

    public static final LOTRSpawnGuard INSTANCE = new LOTRSpawnGuard();

    private LOTRSpawnGuard() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onCheckSpawn(LivingSpawnEvent.CheckSpawn event) {
        if (event.world.isRemote) {
            return;
        }

        BiomeGenBase biome = event.world
            .getBiomeGenForCoords(MathHelper.floor_double(event.x), MathHelper.floor_double(event.z));
        if (LOTRSpawnControl.isSpawnBlocked(event.entityLiving.getClass(), biome)) {
            event.setResult(Event.Result.DENY);
        }
    }
}
