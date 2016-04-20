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

import java.util.*;

public class EpisodeDTO {

    private final Map<String, String> ids;
    private final int episodeNumber;
    private String title;
    private String originalTitle;
    private String plot;
    private String outline;
    private String tagline;
    private String quote;
    private String releaseCountry;
    private Date releaseDate;
    private int rating = -1;
    private List<CreditDTO> credits = new ArrayList<>();
    private boolean valid = true;
    
    public EpisodeDTO(Map<String, String> ids, int episodeNumber) {
        this.ids = ids;
        this.episodeNumber = episodeNumber;
    }

    public Map<String, String> getIds() {
        return ids;
    }

    public EpisodeDTO addId(String source, String id) {
        if (id != null && id.length() > 0) {
            this.ids.put(source, id);
        }
        return this;
    }
    
    public int getEpisodeNumber() {
        return episodeNumber;
    }


    public String getTitle() {
        return title;
    }

    public EpisodeDTO setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public EpisodeDTO setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
        return this;
    }

    public String getPlot() {
        return plot;
    }

    public EpisodeDTO setPlot(String plot) {
        this.plot = plot;
        return this;
    }

    public String getOutline() {
        return outline;
    }

    public EpisodeDTO setOutline(String outline) {
        this.outline = outline;
        return this;
    }

    public String getTagline() {
        return tagline;
    }

    public EpisodeDTO setTagline(String tagline) {
        this.tagline = tagline;
        return this;
    }

    public String getQuote() {
        return quote;
    }

    public EpisodeDTO setQuote(String quote) {
        this.quote = quote;
        return this;
    }

    public String getReleaseCountry() {
        return releaseCountry;
    }

    public EpisodeDTO setReleaseCountry(String releaseCountry) {
        this.releaseCountry = releaseCountry;
        return this;
    }

    public Date getReleaseDate() {
        return releaseDate;
    }

    public EpisodeDTO setReleaseDate(Date releaseDate) {
        this.releaseDate = releaseDate;
        return this;
    }

    public int getRating() {
        return rating;
    }

    public EpisodeDTO setRating(int rating) {
        this.rating = rating;
        return this;
    }

    public List<CreditDTO> getCredits() {
        return credits;
    }

    public EpisodeDTO setCredits(List<CreditDTO> credits) {
        this.credits = credits;
        return this;
    }
    
    public EpisodeDTO addCredit(CreditDTO credit) {
        this.credits.add(credit);
        return this;
    }

    public EpisodeDTO addCredits(Collection<CreditDTO> credits) {
        if (credits != null && !credits.isEmpty()) {
            this.credits.addAll(credits);
        }
        return this;
    }

    public boolean isValid() {
        return valid;
    }

    public boolean isNotValid() {
        return !valid;
    }

    public EpisodeDTO setValid(boolean valid) {
        this.valid = valid;
        return this;
    }
}