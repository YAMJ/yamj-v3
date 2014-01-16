/*
 *      Copyright (c) 2004-2014 YAMJ Members
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

import org.yamj.common.tools.DateTimeTools;
import java.util.EnumMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.text.WordUtils;

/**
 * Class to store any statistics about the jukebox
 *
 * @author stuart.boston
 */
public class Statistics {

    // Statistics
    private final EnumMap<StatType, Integer> statistics = new EnumMap<StatType, Integer>(StatType.class);
    private final EnumMap<TimeType, Long> times = new EnumMap<TimeType, Long>(TimeType.class);

    public Statistics() {
        // Initialise the statistic values
        for (StatType stat : StatType.values()) {
            statistics.put(stat, 0);
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
        return statistics.get(stat);
    }

    /**
     * Set the statistic to a specific value
     *
     * @param stat
     * @param value
     */
    public synchronized void setStatistic(StatType stat, Integer value) {
        statistics.put(stat, value);
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
        if (statistics.containsKey(stat)) {
            current = statistics.get(stat);
        } else {
            current = 0;
        }
        statistics.put(stat, current + amount);
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
        if (statistics.containsKey(stat)) {
            current = statistics.get(stat);
        } else {
            // The end result will be 0;
            current = amount;
        }
        statistics.put(stat, current - amount);
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
            Long duration = DateTimeTools.getDuration(times.get(timeStart), times.get(timeEnd));
            if (useColon) {
                returnValue = DateTimeTools.formatDurationColon(duration);
            } else {
                DateTimeTools.formatDurationText(duration);
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
        return getProcessingTime(TimeType.START, TimeType.END, Boolean.TRUE);
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
                returnValue = DateTimeTools.formatDurationColon(timeToFormat);
            } else {
                returnValue = DateTimeTools.formatDurationText(timeToFormat);
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
        } else {
            return 0L;
        }
    }

    /**
     * Output the jukebox statistics
     *
     * @param skipZero Skip zero values from the output
     * @return
     */
    public String generateStatistics(Boolean skipZero) {
        StringBuilder statOutput = new StringBuilder("Jukebox Statistics:\n");

        // Build the counts
        int value;
        for (StatType stat : StatType.values()) {
            value = statistics.get(stat);
            if (value > 0 || !skipZero) {
                statOutput.append(WordUtils.capitalizeFully(stat.toString().replace("_", " ").toLowerCase()));
                statOutput.append(" = ").append(value).append("\n");
            }
        }

        // Add the processing time
        String processTime = getProcessingTime();
        if (StringUtils.isNotBlank(processTime)) {
            statOutput.append("Scanning Time = ").append(processTime).append("\n");
        }

        processTime = getProcessingTime(TimeType.SENDING_START, TimeType.SENDING_END, Boolean.TRUE);
        if (StringUtils.isNotBlank(processTime)) {
            statOutput.append("Sending Time = ").append(processTime).append("\n");
        }
        return statOutput.toString();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
