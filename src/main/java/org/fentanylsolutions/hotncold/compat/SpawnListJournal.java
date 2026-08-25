package org.fentanylsolutions.hotncold.compat;

import java.util.ArrayList;
import java.util.List;

final class SpawnListJournal {

    private final List<Mutation> mutations = new ArrayList<>();

    void recordAdded(List spawnEntries, Object entry) {
        mutations.add(new AddedEntry(spawnEntries, entry));
    }

    void recordRemoved(List spawnEntries, Object entry, int originalIndex) {
        mutations.add(new RemovedEntry(spawnEntries, entry, originalIndex));
    }

    int undo() {
        int undoneMutations = 0;
        for (int index = mutations.size() - 1; index >= 0; index--) {
            if (mutations.get(index)
                .undo()) {
                undoneMutations++;
            }
        }
        mutations.clear();
        return undoneMutations;
    }

    private static int identityIndexOf(List values, Object target) {
        for (int index = 0; index < values.size(); index++) {
            if (values.get(index) == target) {
                return index;
            }
        }
        return -1;
    }

    private interface Mutation {

        boolean undo();
    }

    private static final class AddedEntry implements Mutation {

        private final List spawnEntries;
        private final Object entry;

        private AddedEntry(List spawnEntries, Object entry) {
            this.spawnEntries = spawnEntries;
            this.entry = entry;
        }

        @Override
        public boolean undo() {
            int currentIndex = identityIndexOf(spawnEntries, entry);
            if (currentIndex < 0) {
                return false;
            }
            spawnEntries.remove(currentIndex);
            return true;
        }
    }

    private static final class RemovedEntry implements Mutation {

        private final List spawnEntries;
        private final Object entry;
        private final int originalIndex;

        private RemovedEntry(List spawnEntries, Object entry, int originalIndex) {
            this.spawnEntries = spawnEntries;
            this.entry = entry;
            this.originalIndex = originalIndex;
        }

        @Override
        public boolean undo() {
            if (identityIndexOf(spawnEntries, entry) >= 0) {
                return false;
            }
            spawnEntries.add(Math.min(originalIndex, spawnEntries.size()), entry);
            return true;
        }
    }
}
