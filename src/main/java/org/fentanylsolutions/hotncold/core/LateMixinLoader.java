package org.fentanylsolutions.hotncold.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

import cpw.mods.fml.relauncher.FMLLaunchHandler;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

@SuppressWarnings("unused")
@LateMixin
@IFMLLoadingPlugin.MCVersion("1.7.10")
public class LateMixinLoader implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.hotncold.late.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        return new MixinBuilder().addMixin("MixinEventHelper", Side.BOTH, "lotr")
            .build();
    }

    public enum Side {
        CLIENT,
        SERVER,
        BOTH;
    }

    public static boolean isServer() {
        return FMLLaunchHandler.side()
            .isServer();
    }

    public static class MixinBuilder {

        private final List<String> mixins = new ArrayList<>();

        public MixinBuilder addMixin(String name, Side side, String modid) {
            if ((side == Side.CLIENT && isServer()) || (side == Side.SERVER && !isServer())) {
                return this;
            }

            mixins.add(modid + "." + name);
            return this;
        }

        public MixinBuilder addMixin(String name, Side side) {
            return addMixin(name, side, "minecraft");
        }

        public List<String> build() {
            return mixins;
        }
    }
}
