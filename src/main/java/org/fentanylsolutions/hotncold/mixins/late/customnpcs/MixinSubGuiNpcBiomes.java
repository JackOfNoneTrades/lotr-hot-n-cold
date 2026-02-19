package org.fentanylsolutions.hotncold.mixins.late.customnpcs;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.world.biome.BiomeGenBase;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import lotr.common.LOTRDimension;
import lotr.common.world.biome.LOTRBiome;
import noppes.npcs.client.gui.SubGuiNpcBiomes;

@Mixin(SubGuiNpcBiomes.class)
public class MixinSubGuiNpcBiomes {

    @Redirect(
        method = "initGui()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/biome/BiomeGenBase;getBiomeGenArray()[Lnet/minecraft/world/biome/BiomeGenBase;"),
        require = 0)
    private BiomeGenBase[] hotncold$injectLOTRBiomesInit() {
        return hotncold$getMergedBiomeArray();
    }

    @Redirect(
        method = "actionPerformed(Lnet/minecraft/client/gui/GuiButton;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/biome/BiomeGenBase;getBiomeGenArray()[Lnet/minecraft/world/biome/BiomeGenBase;"),
        require = 0)
    private BiomeGenBase[] hotncold$injectLOTRBiomesAction(GuiButton button) {
        return hotncold$getMergedBiomeArray();
    }

    @Unique
    private BiomeGenBase[] hotncold$getMergedBiomeArray() {
        Map<Integer, BiomeGenBase> biomesById = new LinkedHashMap<>();

        hotncold$addBiomeArray(biomesById, BiomeGenBase.getBiomeGenArray());
        hotncold$addLOTRDimensionBiomes(biomesById, LOTRDimension.MIDDLE_EARTH);
        hotncold$addLOTRDimensionBiomes(biomesById, LOTRDimension.UTUMNO);

        List<BiomeGenBase> merged = new ArrayList<>(biomesById.values());
        return merged.toArray(new BiomeGenBase[0]);
    }

    @Unique
    private void hotncold$addLOTRDimensionBiomes(Map<Integer, BiomeGenBase> biomesById, LOTRDimension dimension) {
        if (dimension == null || dimension.biomeList == null) {
            return;
        }
        for (LOTRBiome biome : dimension.biomeList) {
            hotncold$addBiome(biomesById, biome);
        }
    }

    @Unique
    private void hotncold$addBiomeArray(Map<Integer, BiomeGenBase> biomesById, BiomeGenBase[] biomes) {
        if (biomes == null) {
            return;
        }
        for (BiomeGenBase biome : biomes) {
            hotncold$addBiome(biomesById, biome);
        }
    }

    @Unique
    private void hotncold$addBiome(Map<Integer, BiomeGenBase> biomesById, BiomeGenBase biome) {
        if (biome == null || biome.biomeName == null) {
            return;
        }
        biomesById.putIfAbsent(biome.biomeID, biome);
    }
}
