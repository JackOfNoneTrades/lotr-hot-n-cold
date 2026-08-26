package org.fentanylsolutions.hotncold.compat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class BlockedSpawnAttemptLog {

    private static final int MAXIMUM_DETAILS = 3;
    private final Map<String, Integer> attemptsByDescription = new LinkedHashMap<>();
    private long nextLogTimeMillis;

    synchronized String record(String entityName, String biomeDescription, String spawnPath, long nowMillis,
        long intervalMillis) {
        String description = entityName + " in " + biomeDescription + " [" + spawnPath + "]";
        Integer previousAttempts = attemptsByDescription.get(description);
        attemptsByDescription.put(description, previousAttempts == null ? 1 : previousAttempts + 1);

        if (nextLogTimeMillis != 0 && nowMillis < nextLogTimeMillis) {
            return null;
        }

        String summary = createSummary();
        attemptsByDescription.clear();
        nextLogTimeMillis = nowMillis + intervalMillis;
        return summary;
    }

    synchronized void reset() {
        attemptsByDescription.clear();
        nextLogTimeMillis = 0;
    }

    private String createSummary() {
        List<Map.Entry<String, Integer>> sortedAttempts = new ArrayList<>(attemptsByDescription.entrySet());
        sortedAttempts.sort(
            Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue)
                .reversed()
                .thenComparing(Map.Entry::getKey));

        int totalAttempts = 0;
        for (Map.Entry<String, Integer> attempts : sortedAttempts) {
            totalAttempts += attempts.getValue();
        }

        StringBuilder summary = new StringBuilder("Blocked natural spawn attempts: ");
        summary.append(totalAttempts)
            .append(" total across ")
            .append(sortedAttempts.size())
            .append(" entity/biome/path combination(s); top: ");
        int detailCount = Math.min(MAXIMUM_DETAILS, sortedAttempts.size());
        for (int index = 0; index < detailCount; index++) {
            if (index > 0) {
                summary.append(", ");
            }
            Map.Entry<String, Integer> attempts = sortedAttempts.get(index);
            summary.append(attempts.getKey())
                .append('=')
                .append(attempts.getValue());
        }
        if (sortedAttempts.size() > detailCount) {
            summary.append(", ...");
        }
        return summary.toString();
    }
}
