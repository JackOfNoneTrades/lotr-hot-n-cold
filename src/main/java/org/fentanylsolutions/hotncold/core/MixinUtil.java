package org.fentanylsolutions.hotncold.core;

import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.FMLLaunchHandler;

public class MixinUtil {

    public static boolean isServer() {
        return FMLLaunchHandler.side()
            .isServer();
    }

    public enum Side {
        CLIENT,
        SERVER,
        BOTH;
    }

    public static class MixinBuilder {

        private final boolean early;

        public MixinBuilder(boolean early) {
            this.early = early;
        }

        private final List<String> mixins = new ArrayList<>();

        public MixinBuilder addMixin(String name, Side side, String modid) {
            if ((side == Side.CLIENT && isServer()) || (side == Side.SERVER && !isServer())) {
                return this;
            }

            if (!early && !Loader.isModLoaded(modid)) {
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
