package org.fentanylsolutions.hotncold;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraftforge.common.config.Configuration;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class EquipmentSourceConfigurationTest {

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
        Config.customizeSpawnEggLOTREquipment = false;
        Config.customizeStructureLOTREquipment = false;
        Config.customizeInvasionLOTREquipment = false;
        minecraftHome.set(null, originalHome);
    }

    private void read(Configuration configuration) throws Exception {
        Method method = Config.class.getDeclaredMethod("readNPCEquipmentConfiguration", Configuration.class);
        method.setAccessible(true);
        method.invoke(null, configuration);
    }

    @Test
    public void missingSwitchesDefaultOffEvenAfterPreviousEnable() throws Exception {
        Config.customizeSpawnEggLOTREquipment = true;
        Config.customizeStructureLOTREquipment = true;
        Config.customizeInvasionLOTREquipment = true;
        read(new Configuration(temporary.newFile()));
        assertFalse(Config.customizeSpawnEggLOTREquipment);
        assertFalse(Config.customizeStructureLOTREquipment);
        assertFalse(Config.customizeInvasionLOTREquipment);
    }

    @Test
    public void everyCombinationPersistsAndReloadsIndependently() throws Exception {
        java.io.File file = temporary.newFile();
        for (int mask = 0; mask < 8; mask++) {
            Configuration configuration = new Configuration(file);
            configuration.get("general", "customizeSpawnEggLOTREquipment", false)
                .set((mask & 1) != 0);
            configuration.get("general", "customizeStructureLOTREquipment", false)
                .set((mask & 2) != 0);
            configuration.get("general", "customizeInvasionLOTREquipment", false)
                .set((mask & 4) != 0);
            configuration.save();
            read(new Configuration(file));
            assertEquals((mask & 1) != 0, Config.customizeSpawnEggLOTREquipment);
            assertEquals((mask & 2) != 0, Config.customizeStructureLOTREquipment);
            assertEquals((mask & 4) != 0, Config.customizeInvasionLOTREquipment);
        }
    }
}
