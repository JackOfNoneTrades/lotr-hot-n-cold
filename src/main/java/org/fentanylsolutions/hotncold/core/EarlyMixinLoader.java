package org.fentanylsolutions.hotncold.core;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fentanylsolutions.hotncold.HotNCold;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import cpw.mods.fml.relauncher.ReflectionHelper;

@SuppressWarnings("unused")
@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.SortingIndex(1002)
public class EarlyMixinLoader extends FentEarlyMixinLoader {

    private static final Logger LOG = LogManager.getLogger("Hot N Cold core");
    private static boolean warOfTheRingRestrictionBypassEnabled;

    @Override
    public String getMixinConfig() {
        return "mixins." + HotNCold.MODID + ".early.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        return new MixinUtil.MixinBuilder(true).build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void injectData(Map<String, Object> data) {
        Object runtimeDeobfuscationEnabled = data.get("runtimeDeobfuscationEnabled");
        boolean developmentEnvironment = Boolean.FALSE.equals(runtimeDeobfuscationEnabled)
            || Boolean.TRUE.equals(Launch.blackboard.get("fml.deobfuscatedEnvironment"));
        warOfTheRingRestrictionBypassEnabled = shouldEnableWarOfTheRingRestrictionBypass(
            MixinUtil.isServer(),
            developmentEnvironment);
        if (!warOfTheRingRestrictionBypassEnabled) {
            LOG.info("Leaving War of the Ring classes untransformed on the production client");
            return;
        }

        LaunchClassLoader classLoader = data.get("classLoader") instanceof LaunchClassLoader
            ? (LaunchClassLoader) data.get("classLoader")
            : Launch.classLoader;
        Set<String> transformerExclusions = ReflectionHelper
            .getPrivateValue(LaunchClassLoader.class, classLoader, "transformerExceptions");

        if (allowWarOfTheRingTransformations(transformerExclusions)) {
            LOG.info("Enabled transformations for War of the Ring classes so dedicated-server compatibility can apply");
        }
    }

    static boolean allowWarOfTheRingTransformations(Set<String> transformerExclusions) {
        return transformerExclusions.remove("wotrmc");
    }

    static boolean shouldEnableWarOfTheRingRestrictionBypass(boolean dedicatedServer, boolean developmentEnvironment) {
        return dedicatedServer || developmentEnvironment;
    }

    static boolean isWarOfTheRingRestrictionBypassEnabled() {
        return warOfTheRingRestrictionBypassEnabled;
    }
}
