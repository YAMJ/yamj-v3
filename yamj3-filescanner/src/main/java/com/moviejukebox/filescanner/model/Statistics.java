package com.moviejukebox.filescanner.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

public class Statistics {

    private Map<StatType, Integer> statistics;

    public Statistics() {
        statistics = new EnumMap<StatType, Integer>(StatType.class);
    }

    public int inc(StatType stat) {
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

    public int dec(StatType stat) {
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

    public int set(StatType stat, int value) {
        statistics.put(stat, value);
        return value;
    }

    public String generateStats() {
        StringBuilder stats = new StringBuilder("Statistics: \n");
        for (Entry<StatType, Integer> entry : statistics.entrySet()) {
            stats.append("  ").append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
        }
        return stats.toString();
    }
}
