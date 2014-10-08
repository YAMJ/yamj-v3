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
package org.yamj.core.service.metadata.nfo;

import java.util.Date;
import org.apache.commons.lang.StringUtils;

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
    private int rating;

    public boolean isValid() {
        return ((this.season >= 0) && (this.episode >= 0)); 
    }
    
    public boolean isSameEpisode(int season, int episode) {
        if (this.season != season) return false;
        if (this.episode != episode) return false;
        return true;
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

    @Override
    public int hashCode() {
        final int prime = 7;
        int result = 1;
        result = prime * result + getSeason();
        result = prime * result + getEpisode();
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (!(other instanceof InfoEpisodeDTO)) {
            return false;
        }
        InfoEpisodeDTO castOther = (InfoEpisodeDTO) other;
        if (getSeason() != castOther.getSeason()) return false;
        if (getEpisode() != castOther.getEpisode()) return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[EpisodeDetail=");
        sb.append("[title=").append(title);
        sb.append("], [season=").append(season);
        sb.append("], [episode=").append(episode);
        sb.append("], [plot=").append(plot);
        if (firstAired != null) {
            sb.append("], [firstAired=").append(firstAired);
        }
        if (airsAfterSeason != null) {
            sb.append("], [airsAfterSeason=").append(airsAfterSeason);
        }
        if (airsBeforeSeason != null) {
            sb.append("], [airsBeforeSeason=").append(airsBeforeSeason);
        }
        if (airsBeforeEpisode != null) {
            sb.append("], [airsBeforeEpisode=").append(airsBeforeEpisode);
        }
        sb.append("], [rating=").append(rating);
        sb.append("]]");
        return sb.toString();
    }
}