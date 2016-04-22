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
import org.apache.commons.lang3.StringUtils;

public class SeriesDTO {

    private final Map<String, String> ids;
    private String title;
    private String originalTitle;
    private int startYear;
    private int endYear;
    private String plot;
    private String outline;
    private int rating = -1;
    private Collection<String> genres = new HashSet<>();
    private Collection<String> studios = new HashSet<>();
    private Collection<String> countries = new HashSet<>();
    private Map<String,String> certifications = new HashMap<>();
    private Set<AwardDTO> awards = new HashSet<>();
    private Collection<SeasonDTO> seasons = new ArrayList<>();
    
    public SeriesDTO(Map<String, String> ids) {
        this.ids = ids;
    }

    public Map<String, String> getIds() {
        return ids;
    }

    public SeriesDTO addId(String source, String id) {
        if (StringUtils.isNotBlank(id)) {
            this.ids.put(source, id);
        }
        return this;
    }

    public String getTitle() {
        return title;
    }

    public SeriesDTO setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public SeriesDTO setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
        return this;
    }

    public int getStartYear() {
        return startYear;
    }

    public SeriesDTO setStartYear(int startYear) {
        this.startYear = startYear;
        return this;
    }

    public int getEndYear() {
        return endYear;
    }

    public SeriesDTO setEndYear(int endYear) {
        this.endYear = endYear;
        return this;
    }

    public String getPlot() {
        return plot;
    }

    public SeriesDTO setPlot(String plot) {
        this.plot = plot;
        return this;
    }

    public String getOutline() {
        return outline;
    }

    public SeriesDTO setOutline(String outline) {
        this.outline = outline;
        return this;
    }

    public int getRating() {
        return rating;
    }

    public SeriesDTO setRating(int rating) {
        this.rating = rating;
        return this;
    }

    public Collection<String> getStudios() {
        return studios;
    }

    public SeriesDTO setStudios(Collection<String> studios) {
        this.studios = studios;
        return this;
    }

    public SeriesDTO addStudio(String studio) {
        if (studio != null) {
            this.studios.add(studio);
        }
        return this;
    }

    public Collection<String> getGenres() {
        return genres;
    }

    public SeriesDTO setGenres(Collection<String> genres) {
        this.genres = genres;
        return this;
    }
    
    public SeriesDTO addGenre(String genre) {
        if (genre != null) {
            this.genres.add(genre);
        }
        return this;
    }

    public Collection<String> getCountries() {
        return countries;
    }

    public SeriesDTO setCountries(Collection<String> countries) {
        this.countries = countries;
        return this;
    }

    public SeriesDTO addCountry(String country) {
        if (country != null) {
            this.countries.add(country);
        }
        return this;
    }

    public Map<String, String> getCertifications() {
        return certifications;
    }

    public SeriesDTO addCertification(String country, String certificate) {
        if (StringUtils.isNotBlank(country) && StringUtils.isNotBlank(certificate)) {
            this.certifications.put(country, certificate);
        }
        return this;
    }
    
    public Set<AwardDTO> getAwards() {
        return awards;
    }

    public SeriesDTO addAward(AwardDTO award) {
        if (award != null) {
            this.awards.add(award);
        }
        return this;
    }

    public Collection<SeasonDTO> getSeasons() {
        return seasons;
    }

    public SeriesDTO setSeasons(Collection<SeasonDTO> seasons) {
        this.seasons = seasons;
        return this;
    }
    
    public SeriesDTO addSeason(SeasonDTO season) {
        this.seasons.add(season);
        return this;
    }
    
    public SeasonDTO getSeason(int seasonNumber) {
        for (SeasonDTO season : this.seasons) {
            if (season.getSeasonNumber() == seasonNumber) {
                return season;
            }
        }
        return null;
    }
}