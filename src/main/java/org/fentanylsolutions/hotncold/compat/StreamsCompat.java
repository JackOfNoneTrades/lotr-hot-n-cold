package org.fentanylsolutions.hotncold.compat;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialLiquid;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import org.fentanylsolutions.hotncold.Config;
import org.fentanylsolutions.hotncold.HotNCold;

import farseek.world.BlockAccess;
import lotr.common.LOTRDimension;
import lotr.common.world.biome.LOTRBiome;
import lotr.common.world.biome.LOTRBiomeGenLake;
import lotr.common.world.biome.LOTRBiomeGenOcean;
import lotr.common.world.biome.LOTRBiomeGenRiver;
import streams.world.gen.structure.RiverGenerator;

/** Middle-earth integration recovered from the tested streams-greg-wild-test4 build. */
public final class StreamsCompat {

    private static Set<LOTRBiome> allowedBiomes = Collections.emptySet();
    private static RiverGenerator middleEarthGenerator;

    private StreamsCompat() {}

    public static void initialize() {
        Set<LOTRBiome> resolved = new HashSet<>();
        for (String entry : Config.streamsMiddleEarthBiomes) {
            LOTRBiome biome = findMiddleEarthBiome(entry);
            if (biome == null) {
                HotNCold.LOG.warn("Middle-earth biome '{}' not found for Streams allowlist", entry);
            } else {
                resolved.add(biome);
            }
        }
        allowedBiomes = Collections.unmodifiableSet(resolved);
        if (!Config.enableStreamsMiddleEarth) {
            HotNCold.LOG.info("Streams generation in Middle-earth is disabled");
        } else if (allowedBiomes.isEmpty()) {
            HotNCold.LOG.warn("Streams Middle-earth biome allowlist is empty; no rivers will generate there");
        } else {
            if (middleEarthGenerator == null) {
                // Farseek registers this generator for world-unload events and clears its world cache there.
                middleEarthGenerator = new RiverGenerator(
                    (MaterialLiquid) Material.water,
                    LOTRDimension.MIDDLE_EARTH.dimensionID);
            }
            HotNCold.LOG.info(
                "Enabled Streams generation in {} Middle-earth biomes: {}",
                allowedBiomes.size(),
                Arrays.toString(Config.streamsMiddleEarthBiomes));
        }
    }

    private static LOTRBiome findMiddleEarthBiome(String nameOrId) {
        if (nameOrId == null || nameOrId.trim()
            .isEmpty()) {
            return null;
        }
        String token = nameOrId.trim();
        try {
            int id = Integer.parseInt(token);
            return id >= 0 && id < LOTRDimension.MIDDLE_EARTH.biomeList.length
                ? LOTRDimension.MIDDLE_EARTH.biomeList[id]
                : null;
        } catch (NumberFormatException ignored) {
            for (LOTRBiome biome : LOTRDimension.MIDDLE_EARTH.biomeList) {
                if (biome != null && biome.biomeName.equalsIgnoreCase(token)) {
                    return biome;
                }
            }
            return null;
        }
    }

    public static void generateTerrain(World world, IChunkProvider provider, int chunkX, int chunkZ, Block[] blocks,
        byte[] metadata) {
        if (middleEarthGenerator == null || !(world instanceof WorldServer)) {
            return;
        }
        // Farseek checks dimension and provider identity itself, excluding its terrain-preview provider.
        middleEarthGenerator.onChunkGeneration((WorldServer) world, provider, chunkX, chunkZ, blocks, metadata);
        if (Config.logWorldgenCompatibility) {
            HotNCold.LOG.info(
                "[worldgen][Streams] terrain chunk=({},{}), dimension={}, primaryProvider={}",
                chunkX,
                chunkZ,
                world.provider.dimensionId,
                provider == ((WorldServer) world).theChunkProviderServer.currentChunkProvider);
        }
    }

    public static void populate(World world, Random random, int chunkX, int chunkZ) {
        if (middleEarthGenerator == null || !(world instanceof WorldServer)) {
            return;
        }
        middleEarthGenerator.build(chunkX, chunkZ, (WorldServer) world, random);
        if (Config.logWorldgenCompatibility) {
            HotNCold.LOG.info(
                "[worldgen][Streams] populated chunk=({},{}), dimension={}",
                chunkX,
                chunkZ,
                world.provider.dimensionId);
        }
    }

    public static boolean isAllowedComponent(IBlockAccess access, StructureBoundingBox bounds, boolean mouth) {
        WorldProvider provider = access instanceof World ? ((World) access).provider
            : access instanceof BlockAccess ? ((BlockAccess) access).worldProvider() : null;
        if (provider == null || provider.dimensionId != LOTRDimension.MIDDLE_EARTH.dimensionID) {
            return true;
        }
        if (!Config.enableStreamsMiddleEarth || allowedBiomes.isEmpty()) {
            return false;
        }
        for (int x = bounds.minX; x <= bounds.maxX; x++) {
            for (int z = bounds.minZ; z <= bounds.maxZ; z++) {
                BiomeGenBase biome = provider.getBiomeGenForCoords(x, z);
                boolean connector = biome instanceof LOTRBiomeGenOcean || biome instanceof LOTRBiomeGenRiver
                    || biome instanceof LOTRBiomeGenLake;
                if (!allowedBiomes.contains(biome) && !(mouth && connector)) {
                    if (Config.logWorldgenCompatibility) {
                        HotNCold.LOG.info(
                            "[worldgen][Streams] rejected component at ({},{}), biome={}, mouth={}",
                            x,
                            z,
                            biome == null ? "null" : biome.biomeName,
                            mouth);
                    }
                    return false;
                }
            }
        }
        return true;
    }
}
