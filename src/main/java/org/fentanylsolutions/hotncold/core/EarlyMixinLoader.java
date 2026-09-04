package org.fentanylsolutions.hotncold.core;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.launchwrapper.IClassTransformer;
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
        LaunchClassLoader classLoader = data.get("classLoader") instanceof LaunchClassLoader
            ? (LaunchClassLoader) data.get("classLoader")
            : Launch.classLoader;
        Set<String> transformerExclusions = ReflectionHelper
            .getPrivateValue(LaunchClassLoader.class, classLoader, "transformerExceptions");
        if (!allowWarOfTheRingTransformations(transformerExclusions)) {
            return;
        }

        List<IClassTransformer> transformers = ReflectionHelper
            .getPrivateValue(LaunchClassLoader.class, classLoader, "transformers");
        WarOfTheRingRestrictionTransformer restrictionTransformer = new WarOfTheRingRestrictionTransformer();
        try {
            ReflectionHelper.setPrivateValue(
                LaunchClassLoader.class,
                classLoader,
                Collections.<IClassTransformer>singletonList(restrictionTransformer),
                "transformers");
            for (String className : WarOfTheRingRestrictionTransformer.targetClassNames()) {
                Class.forName(className, false, classLoader);
            }
            restrictionTransformer.verifyEveryRestrictionWasRemoved();
            LOG.info(
                "Removed War of the Ring dedicated-server restriction without exposing its classes to transformers");
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Could not remove War of the Ring dedicated-server restriction", exception);
        } finally {
            ReflectionHelper.setPrivateValue(LaunchClassLoader.class, classLoader, transformers, "transformers");
            transformerExclusions.add("wotrmc");
        }
    }

    static boolean allowWarOfTheRingTransformations(Set<String> transformerExclusions) {
        return transformerExclusions.remove("wotrmc");
    }

}
