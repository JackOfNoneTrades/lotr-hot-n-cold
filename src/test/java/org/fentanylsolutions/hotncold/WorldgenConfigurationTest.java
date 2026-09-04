package org.fentanylsolutions.hotncold;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import net.minecraftforge.common.config.Configuration;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class WorldgenConfigurationTest {

    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    private java.lang.reflect.Field minecraftHome;
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
        minecraftHome.set(null, originalHome);
        Config.enableStreamsMiddleEarth = true;
        Config.enableGregCavesMiddleEarth = true;
        Config.enableWildCavesMiddleEarth = true;
        Config.logWorldgenCompatibility = false;
        Config.streamsMiddleEarthBiomes = new String[] { "shire", "breeland", "eriador", "rohan", "gondor",
            "anduinVale" };
    }

    private void read(Configuration configuration) throws Exception {
        Method method = Config.class.getDeclaredMethod("readWorldgenConfiguration", Configuration.class);
        method.setAccessible(true);
        method.invoke(null, configuration);
    }

    @Test
    public void defaultsMatchRecoveredBuildWithoutVerboseLogging() throws Exception {
        read(new Configuration(temporary.newFile()));
        assertTrue(Config.enableStreamsMiddleEarth);
        assertTrue(Config.enableGregCavesMiddleEarth);
        assertTrue(Config.enableWildCavesMiddleEarth);
        assertFalse(Config.logWorldgenCompatibility);
        assertArrayEquals(
            new String[] { "shire", "breeland", "eriador", "rohan", "gondor", "anduinVale" },
            Config.streamsMiddleEarthBiomes);
    }

    @Test
    public void restoresDisabledSettingsAndEmptyAllowlistFromDisk() throws Exception {
        java.io.File file = temporary.newFile();
        Configuration config = new Configuration(file);
        for (String key : new String[] { "enableStreamsMiddleEarth", "enableGregCavesMiddleEarth",
            "enableWildCavesMiddleEarth" }) {
            config.get("general", key, true)
                .set(false);
        }
        config.get("general", "streamsMiddleEarthBiomes", new String[0])
            .set(new String[0]);
        config.get("general", "logWorldgenCompatibility", false)
            .set(true);
        config.save();
        read(new Configuration(file));
        assertFalse(Config.enableStreamsMiddleEarth);
        assertFalse(Config.enableGregCavesMiddleEarth);
        assertFalse(Config.enableWildCavesMiddleEarth);
        assertTrue(Config.logWorldgenCompatibility);
        assertArrayEquals(new String[0], Config.streamsMiddleEarthBiomes);
    }

    @Test
    public void preservesUserBiomeNamesAndIds() throws Exception {
        Configuration config = new Configuration(temporary.newFile());
        String[] biomes = { " SHIRE ", "3", "missing-biome" };
        config.get("general", "streamsMiddleEarthBiomes", new String[0])
            .set(biomes);
        read(config);
        assertArrayEquals(biomes, Config.streamsMiddleEarthBiomes);
    }
}
