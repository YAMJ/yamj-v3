/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      YAMJ is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      YAMJ is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.filescanner.model;

import static org.apache.commons.lang3.text.WordUtils.capitalizeFully;
import static org.yamj.common.tools.DateTimeTools.formatDurationColon;
import static org.yamj.common.tools.DateTimeTools.formatDurationText;
import static org.yamj.common.tools.DateTimeTools.getDuration;

import java.util.EnumMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Class to store any statistics about the jukebox
 *
 * @author stuart.boston
 */
public class Statistics {

    // Statistics
    private final Map<StatType, Integer> stats = new EnumMap<>(StatType.class);
    private final Map<TimeType, Long> times = new EnumMap<>(TimeType.class);

    public Statistics() {
        // Initialise the statistic values
        for (StatType stat : StatType.values()) {
            stats.put(stat, 0);
        }

        // Initialise the time values
        for (TimeType time : TimeType.values()) {
            times.put(time, 0L);
        }
    }

    /**
     * Get the current value of the required statistic
     *
     * @param stat
     * @return
     */
    public synchronized int getStatistic(StatType stat) {
        return stats.get(stat);
    }

    /**
     * Set the statistic to a specific value
     *
     * @param stat
     * @param value
     */
    public synchronized void setStatistic(StatType stat, Integer value) {
        stats.put(stat, value);
    }

    /**
     * Increment the statistic by 1
     *
     * @param stat
     */
    public synchronized void increment(StatType stat) {
        increment(stat, 1);
    }

    /**
     * Increment the statistic by the value
     *
     * @param stat
     * @param amount
     */
    public synchronized void increment(StatType stat, Integer amount) {
        Integer current;
        if (stats.containsKey(stat)) {
            current = stats.get(stat);
        } else {
            current = 0;
        }
        stats.put(stat, current + amount);
    }

    /**
     * Decrement the statistic by 1
     *
     * @param stat
     */
    public synchronized void decrement(StatType stat) {
        decrement(stat, 1);
    }

    /**
     * Decrement the statistic by the value
     *
     * @param stat
     * @param amount
     */
    public synchronized void decrement(StatType stat, Integer amount) {
        Integer current;
        if (stats.containsKey(stat)) {
            current = stats.get(stat);
        } else {
            // The end result will be zero
            current = amount;
        }
        stats.put(stat, current - amount);
    }

    /**
     * Set a time for the processing
     *
     * @param timeType
     * @param timeValue
     */
    public void setTime(TimeType timeType, long timeValue) {
        if (times.get(timeType) <= 0L) {
            times.put(timeType, timeValue);
        }
    }

    /**
     * Set the time for this type to the current time
     *
     * @param timeType
     */
    public void setTime(TimeType timeType) {
        if (times.get(timeType) <= 0L) {
            setTime(timeType, System.currentTimeMillis());
        }
    }

    /**
     * Calculate the difference between two jukebox times
     *
     * @param timeStart
     * @param timeEnd
     * @param useColon
     * @return
     */
    public String getProcessingTime(TimeType timeStart, TimeType timeEnd, boolean useColon) {
        String returnValue = "";
        if (times.containsKey(timeStart) && times.containsKey(timeEnd)) {
            Long duration = getDuration(times.get(timeStart), times.get(timeEnd));
            if (useColon) {
                returnValue = formatDurationColon(duration);
            } else {
                formatDurationText(duration);
            }
        }

        return returnValue;
    }

    /**
     * Calculate the processing time for the jukebox run. Uses the the START and END times.
     *
     * @return
     */
    public String getProcessingTime() {
        return getProcessingTime(TimeType.START, TimeType.END, true);
    }

    /**
     * Get a formatted string of the time type
     *
     * @param timeType
     * @param useColon
     * @return
     */
    public String getTime(TimeType timeType, boolean useColon) {
        String returnValue = "";
        if (times.containsKey(timeType)) {
            long timeToFormat = times.get(timeType);
            if (useColon) {
                returnValue = formatDurationColon(timeToFormat);
            } else {
                returnValue = formatDurationText(timeToFormat);
            }
        }
        return returnValue;
    }

    /**
     * Get the (long) time of the time type
     *
     * @param timeType
     * @return
     */
    public long getTime(TimeType timeType) {
        if (times.containsKey(timeType)) {
            return times.get(timeType);
        }
        return 0L;
    }

    /**
     * Output the jukebox statistics
     *
     * @param skipZero Skip zero values from the output
     * @return
     */
    public String generateStatistics(boolean skipZero) {
        final String lineFeed = String.format("%n");
        final StringBuilder statOutput = new StringBuilder("Jukebox Statistics:");
        
        // Build the counts
        int value;
        for (StatType stat : StatType.values()) {
            value = stats.get(stat);
            if (value > 0 || !skipZero) {
                statOutput.append(lineFeed);
                statOutput.append(capitalizeFully(stat.toString().replace("_", " ").toLowerCase()));
                statOutput.append(" = ");
                statOutput.append(value);
            }
        }

        // Add the processing time
        String processTime = getProcessingTime();
        if (StringUtils.isNotBlank(processTime)) {
            statOutput.append(lineFeed);
            statOutput.append("Scanning Time = ");
            statOutput.append(processTime);
        }
        processTime = getProcessingTime(TimeType.SENDING_START, TimeType.SENDING_END, true);
        if (StringUtils.isNotBlank(processTime)) {
            statOutput.append(lineFeed);
            statOutput.append("Sending Time = ");
            statOutput.append(processTime);
        }
        
        return statOutput.toString();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
