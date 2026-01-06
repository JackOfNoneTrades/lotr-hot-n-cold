package org.fentanylsolutions.hotncold.core;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.fentanylsolutions.hotncold.HotNCold;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

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
        return new MixinUtil.MixinBuilder(false).addMixin("MixinEventHelper", MixinUtil.Side.BOTH, "lotr")
            .addMixin("MixinEventHelper", MixinUtil.Side.BOTH, "wotrmc")
            .build();
    }
}
