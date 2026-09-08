package org.fentanylsolutions.hotncold;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;

import net.minecraft.world.World;
import net.minecraftforge.common.config.Configuration;

import org.fentanylsolutions.hotncold.core.EarlyMixinLoader;
import org.fentanylsolutions.hotncold.mixins.early.minecraft.MixinEntityLightningBolt;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class LightningSoundTest {

    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    private Field minecraftHome;
    private Object originalHome;

    @Before
    public void initializeForgeConfigLocation() throws Exception {
        minecraftHome = cpw.mods.fml.relauncher.FMLInjectionData.class.getDeclaredField("minecraftHome");
        minecraftHome.setAccessible(true);
        originalHome = minecraftHome.get(null);
        minecraftHome.set(null, temporary.getRoot());
    }

    @After
    public void reset() throws Exception {
        Config.disableLightningExplosionSound = false;
        minecraftHome.set(null, originalHome);
    }

    private void read(Configuration configuration) throws Exception {
        Method method = Config.class.getDeclaredMethod("readSoundConfiguration", Configuration.class);
        method.setAccessible(true);
        method.invoke(null, configuration);
    }

    @Test
    public void missingSettingKeepsVanillaSoundEvenAfterPriorEnable() throws Exception {
        Config.disableLightningExplosionSound = true;
        read(new Configuration(temporary.newFile()));
        assertFalse(Config.disableLightningExplosionSound);
    }

    @Test
    public void settingPersistsBothWays() throws Exception {
        java.io.File file = temporary.newFile();
        for (boolean disabled : new boolean[] { true, false }) {
            Configuration configuration = new Configuration(file);
            configuration.get("general", "disableLightningExplosionSound", false)
                .set(disabled);
            configuration.save();
            read(new Configuration(file));
            assertEquals(disabled, Config.disableLightningExplosionSound);
        }
    }

    @Test
    public void onlyExplosionSoundIsFilteredWhenEnabled() throws Exception {
        Method method = MixinEntityLightningBolt.class.getDeclaredMethod(
            "hotncold$keepLightningSound",
            World.class,
            double.class,
            double.class,
            double.class,
            String.class,
            float.class,
            float.class);
        method.setAccessible(true);
        MixinEntityLightningBolt mixin = new MixinEntityLightningBolt() {};
        for (boolean disabled : new boolean[] { false, true }) {
            Config.disableLightningExplosionSound = disabled;
            for (String sound : new String[] { "random.explode", "ambient.weather.thunder",
                "lotr:ambient.weather.thunder", "another:sound" }) {
                assertEquals(
                    !disabled || !"random.explode".equals(sound),
                    method.invoke(mixin, null, 1D, 2D, 3D, sound, 2F, 0.5F));
            }
        }
    }

    @Test
    public void mixinIsRegisteredEvenWithNoOptionalCoremods() {
        assertTrue(
            new EarlyMixinLoader().getMixins(Collections.emptySet())
                .contains("minecraft.MixinEntityLightningBolt"));
    }
}
