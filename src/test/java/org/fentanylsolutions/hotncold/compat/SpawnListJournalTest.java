package org.fentanylsolutions.hotncold.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class SpawnListJournalTest {

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void undoesOnlyRecordedChangesInReverseOrder() {
        Object firstOriginalEntry = new Object();
        Object secondOriginalEntry = new Object();
        Object unrelatedLateEntry = new Object();
        Object configuredAddition = new Object();
        List entries = new ArrayList();
        entries.add(firstOriginalEntry);
        entries.add(secondOriginalEntry);

        SpawnListJournal journal = new SpawnListJournal();
        entries.add(configuredAddition);
        journal.recordAdded(entries, configuredAddition);
        journal.recordRemoved(entries, firstOriginalEntry, 0);
        entries.remove(firstOriginalEntry);
        entries.add(unrelatedLateEntry);

        assertEquals(2, journal.undo());
        assertEquals(3, entries.size());
        assertSame(firstOriginalEntry, entries.get(0));
        assertSame(secondOriginalEntry, entries.get(1));
        assertSame(unrelatedLateEntry, entries.get(2));
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void undoIsSafeWhenAnotherModAlreadyChangedARecordedEntry() {
        Object removedEntry = new Object();
        Object addedEntry = new Object();
        List entries = new ArrayList();
        entries.add(removedEntry);
        entries.add(addedEntry);

        SpawnListJournal journal = new SpawnListJournal();
        journal.recordRemoved(entries, removedEntry, 0);
        journal.recordAdded(entries, addedEntry);
        entries.remove(addedEntry);

        assertEquals(0, journal.undo());
        assertEquals(1, entries.size());
        assertSame(removedEntry, entries.get(0));
    }
}
