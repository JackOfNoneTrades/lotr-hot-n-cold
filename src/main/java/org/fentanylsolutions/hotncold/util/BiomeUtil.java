package org.fentanylsolutions.hotncold.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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

    public static String[] getDefaultEnviromineBiomeTemperatures() {
        List<BiomeGenBase> biomes = new ArrayList<>(getAllLOTRBiomes());
        biomes.sort(Comparator.comparing(biome -> biome.biomeName));

        String[] defaults = new String[biomes.size()];
        for (int i = 0; i < biomes.size(); i++) {
            BiomeGenBase biome = biomes.get(i);
            defaults[i] = biome.biomeName + ":" + formatTemperature(getEnviromineDefaultAmbientTemperature(biome));
        }

        return defaults;
    }

    public static float getEnviromineDefaultAmbientTemperature(BiomeGenBase biome) {
        return getEnviromineDefaultAmbientTemperature(biome.temperature);
    }

    public static float getEnviromineDefaultAmbientTemperature(float minecraftTemperature) {
        double radians = Math.toRadians(minecraftTemperature * 45F);
        if (minecraftTemperature >= 0F) {
            return (float) (Math.sin(radians) * 45D);
        }
        return (float) (Math.sin(radians) * -15D);
    }

    private static String formatTemperature(float temperature) {
        return BigDecimal.valueOf(temperature)
            .setScale(1, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString();
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

    public static BiomeGenBase getLOTRBiome(String nameOrId) {
        if (Util.isNumeric(nameOrId)) {
            return getLOTRBiome(Integer.parseInt(nameOrId));
        }

        List<BiomeGenBase> lotrBiomes = getAllLOTRBiomes();
        for (BiomeGenBase biome : lotrBiomes) {
            if (biome.biomeName.equals(nameOrId)) {
                return biome;
            }
        }
        for (BiomeGenBase biome : lotrBiomes) {
            if (biome.biomeName.equalsIgnoreCase(nameOrId)) {
                return biome;
            }
        }

        return null;
    }

    public static BiomeGenBase getLOTRBiome(int id) {
        for (BiomeGenBase biome : getAllLOTRBiomes()) {
            if (biome.biomeID == id) {
                return biome;
            }
        }

        return null;
    }

    public static BiomeGenBase getBiomeArrayEntry(int id) {
        BiomeGenBase[] biomeArray = BiomeGenBase.getBiomeGenArray();
        if (id < 0 || id >= biomeArray.length) {
            return null;
        }

        return biomeArray[id];
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
