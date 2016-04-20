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
package org.yamj.plugin.api.metadata.dto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class SeasonDTO {

    private final Map<String, String> ids;
    private final int seasonNumber;
    private boolean scanNeeded;
    private String title;
    private String originalTitle;
    private int year;
    private String plot;
    private String outline;
    private int rating = -1;
    private Collection<EpisodeDTO> episodes = new ArrayList<>();
    private boolean valid = true;
    
    public SeasonDTO(Map<String, String> ids, int seasonNumber) {
        this.ids = ids;
        this.seasonNumber = seasonNumber;
    }
    
    public Map<String, String> getIds() {
        return ids;
    }

    public SeasonDTO addId(String source, String id) {
        if (id != null && id.length() > 0) {
            this.ids.put(source, id);
        }
        return this;
    }

    public int getSeasonNumber() {
        return seasonNumber;
    }

    public boolean isScanNeeded() {
        return scanNeeded;
    }

    public SeasonDTO setScanNeeded(boolean scanNeeded) {
        this.scanNeeded = scanNeeded;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public SeasonDTO setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public SeasonDTO setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
        return this;
    }

    public int getYear() {
        return year;
    }

    public SeasonDTO setYear(int year) {
        this.year = year;
        return this;
    }

    public String getPlot() {
        return plot;
    }

    public SeasonDTO setPlot(String plot) {
        this.plot = plot;
        return this;
    }

    public String getOutline() {
        return outline;
    }

    public SeasonDTO setOutline(String outline) {
        this.outline = outline;
        return this;
    }

    public int getRating() {
        return rating;
    }

    public SeasonDTO setRating(int rating) {
        this.rating = rating;
        return this;
    }

    public Collection<EpisodeDTO> getEpisodes() {
        return episodes;
    }

    public void setEpisodes(Collection<EpisodeDTO> episodes) {
        this.episodes = episodes;
    }
    
    public SeasonDTO addEpisode(EpisodeDTO episode) {
        this.episodes.add(episode);
        return this;
    }
    
    public EpisodeDTO getEpisode(int episodeNumber) {
        for (EpisodeDTO episode : this.episodes) {
            if (episode.getEpisodeNumber() == episodeNumber) {
                return episode;
            }
        }
        return null;
    }

    public boolean isValid() {
        return valid;
    }

    public boolean isNotValid() {
        return !valid;
    }

    public SeasonDTO setValid(boolean valid) {
        this.valid = valid;
        return this;
    }
}