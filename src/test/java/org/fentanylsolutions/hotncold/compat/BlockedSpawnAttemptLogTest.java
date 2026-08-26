package org.fentanylsolutions.hotncold.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BlockedSpawnAttemptLogTest {

    @Test
    public void emitsAtMostOneAggregatedLinePerInterval() {
        BlockedSpawnAttemptLog attempts = new BlockedSpawnAttemptLog();

        assertEquals(
            "Blocked natural spawn attempts: 1 total across 1 entity/biome/path combination(s); top: "
                + "MoCreatures.Elephant in shire (ID 3) [natural]=1",
            attempts.record("MoCreatures.Elephant", "shire (ID 3)", "natural", 1_000, 60_000));
        assertNull(attempts.record("MoCreatures.Elephant", "shire (ID 3)", "natural", 2_000, 60_000));
        assertNull(attempts.record("MoCreatures.Lion", "nearHarad (ID 82)", "LOTR world-gen", 3_000, 60_000));

        String nextSummary = attempts.record("MoCreatures.Elephant", "shire (ID 3)", "natural", 61_000, 60_000);
        assertTrue(nextSummary.contains("3 total across 2 entity/biome/path combination(s)"));
        assertTrue(nextSummary.contains("MoCreatures.Elephant in shire (ID 3) [natural]=2"));
        assertTrue(nextSummary.contains("MoCreatures.Lion in nearHarad (ID 82) [LOTR world-gen]=1"));
    }

    @Test
    public void resetStartsANewLoggingInterval() {
        BlockedSpawnAttemptLog attempts = new BlockedSpawnAttemptLog();
        attempts.record("Pig", "shire (ID 3)", "natural", 1_000, 60_000);
        assertNull(attempts.record("Pig", "shire (ID 3)", "natural", 2_000, 60_000));

        attempts.reset();

        assertTrue(
            attempts.record("Pig", "shire (ID 3)", "natural", 3_000, 60_000)
                .contains("1 total"));
    }
}
