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
package org.yamj.plugin.api.metadata.model;

import java.util.*;

public class Season {

    private Map<String, String> ids = new HashMap<>();
    private int seasonNumber;
    private String title;
    private String originalTitle;
    private int year;
    private String plot;
    private String outline;
    private int rating = -1;
    private Collection<Episode> episodes = new ArrayList<>();
    private boolean scanNeeded;
    private boolean valid = true;
    
    public Map<String, String> getIds() {
        return ids;
    }

    public Season setIds(Map<String, String> ids) {
        this.ids = ids;
        return this;
    }

    public Season addId(String source, String id) {
        if (id != null && id.length() > 0) {
            this.ids.put(source, id);
        }
        return this;
    }

    public int getSeasonNumber() {
        return seasonNumber;
    }

    public Season setSeasonNumber(int seasonNumber) {
        this.seasonNumber = seasonNumber;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public Season setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public Season setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
        return this;
    }

    public int getYear() {
        return year;
    }

    public Season setYear(int year) {
        this.year = year;
        return this;
    }

    public String getPlot() {
        return plot;
    }

    public Season setPlot(String plot) {
        this.plot = plot;
        return this;
    }

    public String getOutline() {
        return outline;
    }

    public Season setOutline(String outline) {
        this.outline = outline;
        return this;
    }

    public int getRating() {
        return rating;
    }

    public Season setRating(int rating) {
        this.rating = rating;
        return this;
    }

    public Collection<Episode> getEpisodes() {
        return episodes;
    }

    public void setEpisodes(Collection<Episode> episodes) {
        this.episodes = episodes;
    }
    
    public Season addEpisode(Episode episode) {
        this.episodes.add(episode);
        return this;
    }
    
    public Episode getEpisode(int episodeNumber) {
        for (Episode episode : this.episodes) {
            if (episode.getEpisodeNumber() == episodeNumber) {
                return episode;
            }
        }
        return null;
    }

    public boolean isScanNeeded() {
        return scanNeeded;
    }

    public Season setScanNeeded(boolean scanNeeded) {
        this.scanNeeded = scanNeeded;
        return this;
    }

    public boolean isValid() {
        return valid;
    }

    public boolean isNotValid() {
        return !valid;
    }

    public Season setValid(boolean valid) {
        this.valid = valid;
        return this;
    }
}