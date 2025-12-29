package org.fentanylsolutions.hotncold.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import net.minecraft.world.biome.BiomeGenBase;

import org.fentanylsolutions.hotncold.HotNCold;

import lotr.common.LOTRDimension;
import lotr.common.world.biome.LOTRBiome;

public class BiomeUtil {

    public static List<BiomeGenBase> getAllLOTRBiomes() {
        List<BiomeGenBase> biomes = new ArrayList<>();

        for (LOTRDimension dim : new LOTRDimension[] { LOTRDimension.MIDDLE_EARTH, LOTRDimension.UTUMNO }) {
            for (LOTRBiome b : dim.biomeList) {
                if (b != null) {
                    biomes.add(b);
                }
            }
        }

        return biomes;
    }

    public static List<BiomeGenBase> getBiomeList() {
        List<BiomeGenBase> res = new ArrayList<>();

        Arrays.stream(BiomeGenBase.getBiomeGenArray())
            .filter(Objects::nonNull)
            .forEach(res::add);
        res.addAll(getAllLOTRBiomes());

        return (res);
    }

    public static BiomeGenBase getBiomeGenBase(String name) {
        for (BiomeGenBase b : getBiomeList()) {
            if (b.biomeName.equals(name)) {
                return b;
            }
        }
        return null;
    }

    public static BiomeGenBase getBiomeGenBase(int id) {
        for (BiomeGenBase b : getBiomeList()) {
            if (b.biomeID == id) {
                return b;
            }
        }
        return null;
    }

    public static void printBiomeNames() {
        HotNCold.LOG.info("=========Biome List=========");
        for (BiomeGenBase b : getBiomeList()) {
            HotNCold.LOG.info("{} ({}) ({})", b.biomeName, b.biomeID, b.getClass());
        }
        HotNCold.LOG.info("=============================");
    }
}
