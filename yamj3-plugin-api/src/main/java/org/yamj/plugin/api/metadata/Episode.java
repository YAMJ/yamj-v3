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
package org.yamj.plugin.api.metadata;

import java.util.*;

public class Episode {

    private Map<String, String> ids = new HashMap<>();
    private int episodeNumber;
    private String title;
    private String originalTitle;
    private String plot;
    private String outline;
    private String tagline;
    private String quote;
    private String releaseCountry;
    private Date releaseDate;
    private int rating = -1;
    private List<Credit> credits = new ArrayList<>();
    private boolean found;
    
    public Map<String, String> getIds() {
        return ids;
    }

    public Episode setIds(Map<String, String> ids) {
        this.ids = ids;
        return this;
    }

    public Episode addId(String source, String id) {
        if (id != null && id.length() > 0) {
            this.ids.put(source, id);
        }
        return this;
    }
    
    public int getEpisodeNumber() {
        return episodeNumber;
    }

    public Episode setEpisodeNumber(int episodeNumber) {
        this.episodeNumber = episodeNumber;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public Episode setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public Episode setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
        return this;
    }

    public String getPlot() {
        return plot;
    }

    public Episode setPlot(String plot) {
        this.plot = plot;
        return this;
    }

    public String getOutline() {
        return outline;
    }

    public Episode setOutline(String outline) {
        this.outline = outline;
        return this;
    }

    public String getTagline() {
        return tagline;
    }

    public Episode setTagline(String tagline) {
        this.tagline = tagline;
        return this;
    }

    public String getQuote() {
        return quote;
    }

    public Episode setQuote(String quote) {
        this.quote = quote;
        return this;
    }

    public String getReleaseCountry() {
        return releaseCountry;
    }

    public Episode setReleaseCountry(String releaseCountry) {
        this.releaseCountry = releaseCountry;
        return this;
    }

    public Date getReleaseDate() {
        return releaseDate;
    }

    public Episode setReleaseDate(Date releaseDate) {
        this.releaseDate = releaseDate;
        return this;
    }

    public int getRating() {
        return rating;
    }

    public Episode setRating(int rating) {
        this.rating = rating;
        return this;
    }

    public List<Credit> getCredits() {
        return credits;
    }

    public Episode setCredits(List<Credit> credits) {
        this.credits = credits;
        return this;
    }
    
    public Episode addCredit(Credit credit) {
        this.credits.add(credit);
        return this;
    }

    public Episode addCredits(Collection<Credit> credits) {
        if (credits != null && !credits.isEmpty()) {
            this.credits.addAll(credits);
        }
        return this;
    }

    public boolean isFound() {
        return found;
    }

    public Episode setFound(boolean found) {
        this.found = found;
        return this;
    }    
}