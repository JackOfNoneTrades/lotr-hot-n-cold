package org.fentanylsolutions.hotncold.core;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.fentanylsolutions.hotncold.HotNCold;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

@SuppressWarnings("unused")
@LateMixin
@IFMLLoadingPlugin.MCVersion("1.7.10")
public class LateMixinLoader implements ILateMixinLoader {

    private final List<String> specialIds = Arrays.asList("fml", "mcp", "minecraft", "minecraftforge");

    @Override
    public String getMixinConfig() {
        return "mixins." + HotNCold.MODID + ".late.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        MixinUtil.MixinBuilder builder = new MixinUtil.MixinBuilder(false);
        if (Loader.isModLoaded("minefantasy2") && Loader.isModLoaded("campfirebackport")) {
            builder.addMixin("MixinTileEntityCampfire", MixinUtil.Side.BOTH, "campfirebackport")
                .addMixin("MixinBlockCampfire", MixinUtil.Side.BOTH, "campfirebackport");
        }
        builder.addMixin("MixinEntityRendererProxyWeather2Mini", MixinUtil.Side.CLIENT, "weather2")
            .addMixin("MixinSceneEnhancer", MixinUtil.Side.CLIENT, "weather2")
            .addMixin("MixinClientTickHandler", MixinUtil.Side.CLIENT, "weather2");
        return builder.addMixin("MixinEventHelper", MixinUtil.Side.BOTH, "lotr")
            .addMixin("MixinLOTRSpawnerAnimals", MixinUtil.Side.BOTH, "lotr")
            .addMixin("MixinLOTRSpawnerNPCs", MixinUtil.Side.BOTH, "lotr")
            .addMixin("MixinLOTRSkyRenderer", MixinUtil.Side.CLIENT, "lotr")
            .addMixin("MixinEMConfigHandler", MixinUtil.Side.BOTH, "enviromine")
            .addMixin("MixinEMStatusManagerLOTR", MixinUtil.Side.BOTH, "enviromine")
            .addMixin("MixinEventHelper", MixinUtil.Side.BOTH, "wotrmc")
            .addMixin("MixinSubGuiNpcBiomes", MixinUtil.Side.CLIENT, "customnpcs")
            .addMixin("MixinLOTRChunkProvider", MixinUtil.Side.BOTH, "streams")
            .addMixin("MixinRiverMouthComponent", MixinUtil.Side.BOTH, "streams")
            .addMixin("MixinRiverUpstreamComponent", MixinUtil.Side.BOTH, "streams")
            .addMixin("MixinLOTRChunkProvider", MixinUtil.Side.BOTH, "gregcaves")
            .addMixin("MixinMapGenGregCaves", MixinUtil.Side.BOTH, "gregcaves")
            .addMixin("MixinLOTRChunkProvider", MixinUtil.Side.BOTH, "wildcaves3")
            .addMixin("MixinDecorationHelper", MixinUtil.Side.BOTH, "wildcaves3")
            .addMixin("MixinWorldGenWildCaves", MixinUtil.Side.BOTH, "wildcaves3")
            .build();
    }
}
