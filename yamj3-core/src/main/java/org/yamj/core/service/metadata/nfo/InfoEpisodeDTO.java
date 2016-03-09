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
package org.yamj.core.service.metadata.nfo;

import java.util.Date;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.builder.*;

/**
 * Class to hold the episode information scraped from the XBMC style TV Episode
 * NFO file.
 */
public final class InfoEpisodeDTO {

    private String title;
    private int season = -1;
    private int episode = -1;
    private String plot;
    private Date firstAired;
    private String airsAfterSeason;
    private String airsBeforeSeason;
    private String airsBeforeEpisode;
    private int rating = -1;
    private boolean watched = false;
    private Date watchedDate; 

    public boolean isValid() {
        return ((this.season >= 0) && (this.episode >= 0));
    }

    public boolean isSameEpisode(int season, int episode) {
        if (this.season != season) {
            return false;
        }
        return this.episode == episode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (StringUtils.isNotBlank(title)) {
            this.title = title;
        }
    }

    public int getSeason() {
        return season;
    }

    public void setSeason(int season) {
        if (season >= 0) {
            this.season = season;
        }
    }

    public int getEpisode() {
        return episode;
    }

    public void setEpisode(int episode) {
        if (episode >= 0) {
            this.episode = episode;
        }
    }

    public String getPlot() {
        return plot;
    }

    public void setPlot(String plot) {
        if (StringUtils.isNotBlank(plot)) {
            this.plot = plot;
        }
    }

    public Date getFirstAired() {
        return firstAired;
    }

    public void setFirstAired(Date firstAired) {
        if (firstAired != null) {
            this.firstAired = firstAired;
        }
    }

    public String getAirsAfterSeason() {
        return airsAfterSeason;
    }

    public void setAirsAfterSeason(String airsAfterSeason) {
        if (StringUtils.isNotBlank(airsAfterSeason)) {
            this.airsAfterSeason = airsAfterSeason;
        }
    }

    public String getAirsBeforeSeason() {
        return airsBeforeSeason;
    }

    public void setAirsBeforeSeason(String airsBeforeSeason) {
        if (StringUtils.isNotBlank(airsBeforeSeason)) {
            this.airsBeforeSeason = airsBeforeSeason;
        }
    }

    public String getAirsBeforeEpisode() {
        return airsBeforeEpisode;
    }

    public void setAirsBeforeEpisode(String airsBeforeEpisode) {
        if (StringUtils.isNotBlank(airsBeforeEpisode)) {
            this.airsBeforeEpisode = airsBeforeEpisode;
        }
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        if (rating >= 0) {
            this.rating = rating;
        }
    }

    public boolean isWatched() {
        return watched;
    }

    public Date getWatchedDate() {
        return this.watchedDate;
    }
    
    public void setWatched(boolean watched, Date watchedDate) {
        this.watched = this.watched || watched;
        if (watchedDate != null && (this.watchedDate == null || this.watchedDate.before(watchedDate))) {
            // set last watched date
            this.watchedDate = watchedDate;
        }
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getSeason())
                .append(getEpisode())
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof InfoEpisodeDTO) {
            final InfoEpisodeDTO other = (InfoEpisodeDTO) obj;
            return new EqualsBuilder()
                    .append(getSeason(), other.getSeason())
                    .append(getEpisode(), other.getEpisode())
                    .isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
