package com.moviejukebox.filescanner.stats;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ScannerStatistics {

    private static Map<StatType, Integer> statistics = new HashMap<>();

    private ScannerStatistics() {
        throw new UnsupportedOperationException("Cannot instantiate this class");
    }

    public static int inc(StatType stat) {
        int value = 0;

        // Check to see if the value already exists and use that value
        if (statistics.containsKey(stat)) {
            value = statistics.get(stat);
        }

        // increment the value
        statistics.put(stat, ++value);

        // return the new value
        return value;
    }

    public static int dec(StatType stat) {
        int value = 0;

        // Check to see if the value already exists and use that value
        if (statistics.containsKey(stat)) {
            value = statistics.get(stat);
        }

        // increment the value
        statistics.put(stat, (value > 0 ? --value : 0));

        // return the new value
        return value;
    }

    public static int set(StatType stat, int value) {
        statistics.put(stat, value);
        return value;
    }

    public static String generateStats() {
        StringBuilder stats = new StringBuilder("Statistics: \n");
        for (Entry<StatType, Integer> entry : statistics.entrySet()) {
            stats.append("  ").append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
        }
        return stats.toString();
    }
}
