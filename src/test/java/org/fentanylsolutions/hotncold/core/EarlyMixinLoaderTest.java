package org.fentanylsolutions.hotncold.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class EarlyMixinLoaderTest {

    @Test
    public void enablesOnlyWarOfTheRingTransformations() {
        Set<String> exclusions = new HashSet<>();
        exclusions.add("wotrmc");
        exclusions.add("another.coremod");

        assertTrue(EarlyMixinLoader.allowWarOfTheRingTransformations(exclusions));
        assertFalse(exclusions.contains("wotrmc"));
        assertTrue(exclusions.contains("another.coremod"));
        assertFalse(EarlyMixinLoader.allowWarOfTheRingTransformations(exclusions));
    }

    @Test
    public void enablesRestrictionBypassOnDedicatedServers() {
        assertTrue(EarlyMixinLoader.shouldEnableWarOfTheRingRestrictionBypass(true, false));
    }

    @Test
    public void enablesRestrictionBypassInDevelopmentClients() {
        assertTrue(EarlyMixinLoader.shouldEnableWarOfTheRingRestrictionBypass(false, true));
    }

    @Test
    public void leavesProductionSingleplayerWarOfTheRingClassesUntouched() {
        assertFalse(EarlyMixinLoader.shouldEnableWarOfTheRingRestrictionBypass(false, false));
    }
}
