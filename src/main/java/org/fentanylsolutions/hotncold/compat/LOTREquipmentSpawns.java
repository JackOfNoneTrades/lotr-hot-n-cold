package org.fentanylsolutions.hotncold.compat;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;

import org.fentanylsolutions.hotncold.Config;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import lotr.common.entity.npc.LOTREntityNPC;

/** Applies once after the source finishes setting names, home positions, quests and gear. */
public final class LOTREquipmentSpawns {

    private static final String HANDLED = "hotncold:equipmentSpawnHandled";
    private static final Map<LOTREntityNPC, Source> PENDING = new IdentityHashMap<>();

    private LOTREquipmentSpawns() {}

    public static void init() {
        LOTREquipmentSpawns listener = new LOTREquipmentSpawns();
        FMLCommonHandler.instance()
            .bus()
            .register(listener);
        MinecraftForge.EVENT_BUS.register(listener);
    }

    public static boolean spawnFromEgg(World world, Entity entity) {
        return spawn(world, entity, Source.EGG);
    }

    public static boolean spawnFromStructure(World world, Entity entity) {
        return spawn(world, entity, Source.STRUCTURE);
    }

    public static boolean spawnFromInvasion(World world, Entity entity) {
        return spawn(world, entity, Source.INVASION);
    }

    private static boolean spawn(World world, Entity entity, Source source) {
        boolean spawned = world.spawnEntityInWorld(entity);
        if (spawned && !world.isRemote
            && source.enabled()
            && entity instanceof LOTREntityNPC
            && !entity.getEntityData()
                .getBoolean(HANDLED)) {
            synchronized (PENDING) {
                PENDING.put((LOTREntityNPC) entity, source);
            }
        }
        return spawned;
    }

    @SubscribeEvent
    public void tick(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.world.isRemote) {
            finish(event.world);
        }
    }

    @SubscribeEvent
    public void save(WorldEvent.Save event) {
        if (!event.world.isRemote) {
            finish(event.world);
        }
    }

    @SubscribeEvent
    public void unload(WorldEvent.Unload event) {
        synchronized (PENDING) {
            PENDING.keySet()
                .removeIf(npc -> npc.worldObj == event.world);
        }
    }

    private static void finish(World world) {
        Map<LOTREntityNPC, Source> ready = new IdentityHashMap<>();
        synchronized (PENDING) {
            Iterator<Map.Entry<LOTREntityNPC, Source>> iterator = PENDING.entrySet()
                .iterator();
            while (iterator.hasNext()) {
                Map.Entry<LOTREntityNPC, Source> entry = iterator.next();
                if (entry.getKey().worldObj == world) {
                    ready.put(entry.getKey(), entry.getValue());
                    iterator.remove();
                }
            }
        }
        for (Map.Entry<LOTREntityNPC, Source> entry : ready.entrySet()) {
            LOTREntityNPC npc = entry.getKey();
            npc.getEntityData()
                .setBoolean(HANDLED, true);
            if (!npc.isDead && entry.getValue()
                .enabled()) {
                LOTREquipmentControl.applySpawnEquipment(npc, entry.getValue().label);
            }
        }
    }

    private enum Source {

        EGG("spawn egg"),
        STRUCTURE("structure"),
        INVASION("invasion");

        private final String label;

        Source(String label) {
            this.label = label;
        }

        private boolean enabled() {
            switch (this) {
                case EGG:
                    return Config.customizeSpawnEggLOTREquipment;
                case STRUCTURE:
                    return Config.customizeStructureLOTREquipment;
                case INVASION:
                    return Config.customizeInvasionLOTREquipment;
                default:
                    throw new AssertionError(this);
            }
        }
    }
}
