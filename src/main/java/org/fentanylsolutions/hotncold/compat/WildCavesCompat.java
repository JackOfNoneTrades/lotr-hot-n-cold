package org.fentanylsolutions.hotncold.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraftforge.common.config.Configuration;

import org.fentanylsolutions.hotncold.Config;
import org.fentanylsolutions.hotncold.HotNCold;

import lotr.common.LOTRDimension;
import lotr.common.LOTRMod;

public final class WildCavesCompat {

    private static Object generator;
    private static Method generateMethod;
    private static int maxGenerationHeight = 128;

    private WildCavesCompat() {}

    @SuppressWarnings("unchecked")
    public static void initialize() {
        try {
            Class<?> generatorClass = Class.forName("wildCaves.WorldGenWildCaves");
            Configuration configuration = (Configuration) Class.forName("wildCaves.WildCaves")
                .getField("config")
                .get(null);
            Object created = generatorClass.getConstructor(Configuration.class)
                .newInstance(configuration);
            Method method = generatorClass.getMethod("generate", Random.class, int.class, int.class, World.class);
            Field whitelistField = generatorClass.getDeclaredField("blockWhiteList");
            whitelistField.setAccessible(true);
            List<Block> whitelist = (List<Block>) whitelistField.get(null);
            int added = addLotrUndergroundBlocks(whitelist);
            List<Integer> blacklist = (List<Integer>) generatorClass.getField("dimensionBlacklist")
                .get(null);
            maxGenerationHeight = generatorClass.getField("maxGenHeight")
                .getInt(null);
            generator = created;
            generateMethod = method;
            HotNCold.LOG.info(
                "Enabled Wild Caves 3 decorations on LOTR underground blocks ({} blocks added to its whitelist)",
                added);
            if (blacklist.contains(LOTRDimension.MIDDLE_EARTH.dimensionID)) {
                HotNCold.LOG.warn(
                    "Wild Caves dimension blacklist contains Middle-earth ({}); decorations will not generate until it is removed from Wild Caves' own configuration",
                    LOTRDimension.MIDDLE_EARTH.dimensionID);
            }
        } catch (ReflectiveOperationException | ClassCastException | LinkageError e) {
            generator = null;
            generateMethod = null;
            HotNCold.LOG.error("Could not enable Wild Caves 3 decorations on LOTR underground blocks", e);
        }
    }

    public static void generate(World world, Random random, int blockX, int blockZ) {
        if (generator == null || generateMethod == null) {
            return;
        }
        try {
            int before = Config.logWorldgenCompatibility ? countDecorations(world, blockX + 8, blockZ + 8) : 0;
            // Match Wild Caves' normal decorator offset. Its generator honors its own blacklist/settings.
            generateMethod.invoke(generator, random, blockX + 8, blockZ + 8, world);
            if (Config.logWorldgenCompatibility) {
                HotNCold.LOG.info(
                    "[worldgen][WildCaves] origin=({},{}), dimension={}, decorationDelta={}",
                    blockX,
                    blockZ,
                    world.provider.dimensionId,
                    countDecorations(world, blockX + 8, blockZ + 8) - before);
            }
        } catch (ReflectiveOperationException | LinkageError e) {
            generator = null;
            generateMethod = null;
            HotNCold.LOG
                .error("Could not generate Wild Caves 3 decorations in Middle-earth; further attempts disabled", e);
        }
    }

    private static int countDecorations(World world, int minX, int minZ) {
        int count = 0;
        int height = Math.min(256, Math.max(1, maxGenerationHeight));
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                for (int y = 0; y < height; y++) {
                    if (world.getBlock(x, y, z)
                        .getClass()
                        .getName()
                        .startsWith("wildCaves.")) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static int addLotrUndergroundBlocks(List<Block> whitelist) {
        Block[] blocks = { LOTRMod.rock, LOTRMod.oreCopper, LOTRMod.oreTin, LOTRMod.oreSilver, LOTRMod.oreMithril,
            LOTRMod.oreNaurite, LOTRMod.oreMorgulIron, LOTRMod.oreQuendite, LOTRMod.oreGlowstone, LOTRMod.oreGulduril,
            LOTRMod.oreSulfur, LOTRMod.oreSaltpeter, LOTRMod.oreSalt, LOTRMod.oreGem, LOTRMod.scorchedStone,
            LOTRMod.redSandstone, LOTRMod.whiteSandstone, LOTRMod.mordorDirt };
        int added = 0;
        for (Block block : blocks) {
            if (block != null && !whitelist.contains(block)) {
                whitelist.add(block);
                added++;
            }
        }
        return added;
    }
}
